// [线性-R02] 精灵雷达客户端状态管理：持有所有过滤条件、模式开关、匹配逻辑
package com.cobblemon_ts.mod.pokemonradar;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon_ts.mod.pokemoneditor.PokemonNameMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PokemonRadarState {

    private static PokemonRadarState INSTANCE;

    // [线性-R06g] RCT模组是否已加载（静态检测，仅影响初始默认值）
    private static final boolean RCT_MOD_PRESENT;
    static {
        boolean p = false;
        try { Class.forName("com.gitlab.srcmc.rctmod.world.entities.TrainerMob"); p = true; }
        catch (ClassNotFoundException ignored) {}
        RCT_MOD_PRESENT = p;
    }

    // [线性-R03h-cache] IV反射缓存：避免每tick重复class.forName/反射
    private static Class<?> statClass = null;
    private static java.lang.reflect.Method ivGetMethod = null;
    private static boolean ivCacheFailed = false;

    // [线性-R02a] 模式开关：神兽/闪光/精灵/NPC（红=false关, 绿=true开）
    private boolean legendaryMode;
    private boolean shinyMode;
    private boolean pokemonMode;
    private boolean npcMode;                    // [线性-R06a] NPC训练师追踪模式开关
    private String npcSubMode = "npc";        // [线性-R06g] 子模式: "rtc"(蓝)=跟随卡片 / "npc"(紫)=手动追踪

    // [线性-R02a2] 追踪总开关：NUMPAD8切换，关闭时完全不追踪
    private boolean radarTrackingEnabled = false;

    // [线性-R02b] 过滤输入：精灵名批量(;分隔)、精灵名单个、性别(012;分隔)、特性、性格(;分隔)
    // [线性-R02c] 等级+个体值：等级(单值或1-100范围)，个体HP/Atk/Def/SpA/SpD/Spe(单值或0-31范围)
    private String batchSpeciesInput = "";
    private String singleSpeciesInput = "";
    private String genderInput = "";
    private String abilityInput = "";
    private String natureInput = "";
    private String levelInput = "";
    private String hpIvInput = "";
    private String atkIvInput = "";
    private String defIvInput = "";
    private String spaIvInput = "";
    private String spdIvInput = "";
    private String speIvInput = "";
    private String npcNameInput = "";           // [线性-R06b] NPC训练师名字过滤（空=追踪所有；填名字=只追踪该名）
    private Vec3 cardTargetPos = null;           // [线性-R06f] RCT图鉴追踪目标坐标（每tick由渲染器更新）
    private UUID cardTargetUuid = null;           // [线性-R06f] 离卡片目标最近的TrainerMob UUID

    // [线性-R07] 雷达范围（方块距离）：默认16，最大64
    private int radarRange = 16;

    private PokemonRadarState() {
        // [线性-R02a-init] 初始默认三个模式都开启，追踪总开关默认开
        this.legendaryMode = true;
        this.shinyMode = true;
        this.pokemonMode = true;
        // [线性-R06g] RCT模组存在→默认RTC开启; 无RCT→NPC关闭
        this.npcMode = RCT_MOD_PRESENT;
        this.npcSubMode = RCT_MOD_PRESENT ? "rtc" : "npc";
        this.radarTrackingEnabled = true;
    }

    public static PokemonRadarState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PokemonRadarState();
            INSTANCE.loadFromFile(); // [线性-R08a] 首次创建时从文件加载
        }
        return INSTANCE;
    }

    // ==== 模式开关 ====

    public boolean isLegendaryMode() { return legendaryMode; }
    public void setLegendaryMode(boolean v) { this.legendaryMode = v; }

    public boolean isShinyMode() { return shinyMode; }
    public void setShinyMode(boolean v) { this.shinyMode = v; }

    public boolean isPokemonMode() { return pokemonMode; }
    public void setPokemonMode(boolean v) { this.pokemonMode = v; }

    // [线性-R06a] NPC训练师追踪模式
    public boolean isNpcMode() { return npcMode; }
    public void setNpcMode(boolean v) { this.npcMode = v; }

    // [线性-R06g] NPC子模式: "rtc"(蓝) / "npc"(紫)
    public String getNpcSubMode() { return npcSubMode; }
    public void setNpcSubMode(String v) { this.npcSubMode = v != null ? v : "npc"; }

    /** [线性-R02a2] 追踪总开关 */
    public boolean isRadarTrackingEnabled() { return radarTrackingEnabled; }
    public void setRadarTrackingEnabled(boolean v) { this.radarTrackingEnabled = v; }

    /** 是否有任意模式开启（含NPC） */
    public boolean isAnyModeActive() {
        return legendaryMode || shinyMode || pokemonMode || npcMode;
    }

    /** 是否有精灵模式开启（不含NPC） */
    public boolean isPokemonModesActive() {
        return legendaryMode || shinyMode || pokemonMode;
    }

    /** 精灵模式激活数量（不含NPC） */
    public int activeModeCount() {
        int c = 0;
        if (legendaryMode) c++;
        if (shinyMode) c++;
        if (pokemonMode) c++;
        return c;
    }

    // [线性-R06a] NPC模式活跃检查
    public boolean isNpcModeActive() { return npcMode && radarTrackingEnabled; }

    // ==== 过滤输入 ====

    public String getBatchSpeciesInput() { return batchSpeciesInput; }
    public void setBatchSpeciesInput(String v) { this.batchSpeciesInput = v != null ? v : ""; }

    public String getSingleSpeciesInput() { return singleSpeciesInput; }
    public void setSingleSpeciesInput(String v) { this.singleSpeciesInput = v != null ? v : ""; }

    public String getGenderInput() { return genderInput; }
    public void setGenderInput(String v) { this.genderInput = v != null ? v : ""; }

    public String getAbilityInput() { return abilityInput; }
    public void setAbilityInput(String v) { this.abilityInput = v != null ? v : ""; }

    public String getNatureInput() { return natureInput; }
    public void setNatureInput(String v) { this.natureInput = v != null ? v : ""; }

    // ==== 等级 + 个体值 ====
    public String getLevelInput() { return levelInput; }
    public void setLevelInput(String v) { this.levelInput = v != null ? v : ""; }
    public String getHpIvInput() { return hpIvInput; }
    public void setHpIvInput(String v) { this.hpIvInput = v != null ? v : ""; }
    public String getAtkIvInput() { return atkIvInput; }
    public void setAtkIvInput(String v) { this.atkIvInput = v != null ? v : ""; }
    public String getDefIvInput() { return defIvInput; }
    public void setDefIvInput(String v) { this.defIvInput = v != null ? v : ""; }
    public String getSpaIvInput() { return spaIvInput; }
    public void setSpaIvInput(String v) { this.spaIvInput = v != null ? v : ""; }
    public String getSpdIvInput() { return spdIvInput; }
    public void setSpdIvInput(String v) { this.spdIvInput = v != null ? v : ""; }
    public String getSpeIvInput() { return speIvInput; }
    public void setSpeIvInput(String v) { this.speIvInput = v != null ? v : ""; }

    // [线性-R06b] NPC训练师名字过滤
    public String getNpcNameInput() { return npcNameInput; }
    public void setNpcNameInput(String v) { this.npcNameInput = v != null ? v : ""; }

    // [线性-R07] 雷达范围（方块距离，1-64）
    public int getRadarRange() { return radarRange; }
    public void setRadarRange(int v) {
        this.radarRange = Math.max(1, Math.min(64, v));
    }

    // ==== 匹配逻辑 ====

    /**
     * [线性-R03] 判断精灵实体是否匹配雷达过滤条件
     * 多模式情况：任一模式匹配即通过，不检查子过滤
     * 单模式情况：模式匹配 + 子过滤（物种/性别/特性/性格）也匹配才通过
     */
    public boolean matches(PokemonEntity entity) {
        // [线性-R02a2] 追踪总开关未启用时不匹配
        if (!radarTrackingEnabled) return false;
        if (!isPokemonModesActive()) return false;

        // [线性-R02a3] 排除有主人的精灵 — 用getOwnerUUID()读同步数据,O(1)零开销
        // entity.getOwnerUUID() 直接取synchedEntityData里的OWNER_UUID,不做Player查询
        if (entity.getOwnerUUID() != null) return false;
        Pokemon pokemon = entity.getPokemon();
        Species species = pokemon.getSpecies();
        if (species == null) return false;

        String speciesId = species.showdownId().toLowerCase();
        boolean isLegendary = isLegendarySpecies(species);
        boolean isShiny = pokemon.getShiny();

        int activeCount = activeModeCount();

        if (activeCount > 1) {
            // [线性-R03a] 多模式：任一匹配即通过
            boolean matched = false;
            if (legendaryMode && isLegendary) matched = true;
            if (shinyMode && isShiny) matched = true;
            if (pokemonMode && matchesSpeciesFilter(speciesId)) matched = true;
            return matched;
        } else {
            // [线性-R03b] 单模式：必须模式匹配 + 子过滤也匹配
            boolean modeMatch = false;
            if (legendaryMode && isLegendary) modeMatch = true;
            if (shinyMode && isShiny) modeMatch = true;
            if (pokemonMode && matchesSpeciesFilter(speciesId)) modeMatch = true;
            if (!modeMatch) return false;

            // 子过滤检查
            if (!matchesGenderFilter(pokemon)) return false;
            if (!matchesAbilityFilter(pokemon)) return false;
            if (!matchesNatureFilter(pokemon)) return false;
            if (!matchesLevelFilter(pokemon)) return false;         // [线性-R03g]
            if (!matchesIvFilter(pokemon)) return false;            // [线性-R03h]
            return true;
        }
    }

    /** [线性-R03c] 单模式下的物种过滤：中英文/拼音→showdownId比对 */
    private boolean matchesSpeciesFilter(String speciesId) {
        if (singleSpeciesInput.isEmpty()) return true;
        // [线性-R03c-fix] 用PokemonNameMap翻译中文→showdownId (小火龙→charmander)
        String realInput = PokemonNameMap.toEnglish(singleSpeciesInput.trim());
        if (realInput != null && !realInput.isEmpty()) {
            return speciesId.equalsIgnoreCase(realInput);
        }
        // Fallback：直接字符串匹配
        String input = singleSpeciesInput.toLowerCase().trim();
        return speciesId.contains(input) || input.contains(speciesId);
    }

    /** [线性-R03d] 性别过滤：输入0/1/2，;分隔多个 */
    private boolean matchesGenderFilter(Pokemon pokemon) {
        if (genderInput.isEmpty()) return true;
        Object genderObj = pokemon.getGender();
        int pokemonGender = 2; // 默认无性别
        if (genderObj != null) {
            String gn = genderObj.toString().toUpperCase();
            if (gn.contains("MALE") && !gn.contains("FE")) pokemonGender = 0;
            else if (gn.contains("FEMALE")) pokemonGender = 1;
        }
        String[] parts = genderInput.split(";");
        for (String p : parts) {
            try {
                int val = Integer.parseInt(p.trim());
                if (val == pokemonGender) return true;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    /** [线性-R03e] 特性过滤：支持中英文 */
    private boolean matchesAbilityFilter(Pokemon pokemon) {
        if (abilityInput.isEmpty()) return true;
        Object abilityObj = pokemon.getAbility();
        if (abilityObj == null) return false;
        String abilityId = extractStringId(abilityObj).toLowerCase();
        String input = abilityInput.toLowerCase().trim();
        return abilityId.contains(input) || input.contains(abilityId);
    }

    /** [线性-R03f] 性格过滤：;分隔多个 */
    private boolean matchesNatureFilter(Pokemon pokemon) {
        if (natureInput.isEmpty()) return true;
        Object natureObj = pokemon.getNature();
        if (natureObj == null) return false;
        String natureId = extractStringId(natureObj).toLowerCase();
        String[] parts = natureInput.split(";");
        for (String p : parts) {
            String trimmed = p.trim().toLowerCase();
            if (!trimmed.isEmpty() && natureId.contains(trimmed)) return true;
        }
        return false;
    }

    /** [线性-R03g] 等级过滤：单值如"50" 或 范围如"1-100"，空=不过滤 */
    private boolean matchesLevelFilter(Pokemon pokemon) {
        if (levelInput.isEmpty()) return true;
        int level = pokemon.getLevel();
        return matchesRange(levelInput.trim(), level);
    }

    /** [线性-R03h] 个体值过滤：HP/Atk/Def/SpA/SpD/Spe各自单值或0-31范围，空=不过滤
     *  使用缓存反射调IVs.get(Stat)获取各IV值 */
    private boolean matchesIvFilter(Pokemon pokemon) {
        if (hpIvInput.isEmpty() && atkIvInput.isEmpty() && defIvInput.isEmpty()
            && spaIvInput.isEmpty() && spdIvInput.isEmpty() && speIvInput.isEmpty()) {
            return true;
        }
        if (ivCacheFailed) return true;
        try {
            if (ivGetMethod == null) {
                // 用forName避免编译期接触Kotlin类型(KMappedMarker)
                statClass = Class.forName("com.cobblemon.mod.common.api.pokemon.stats.Stat");
                Class<?> ivsClass = Class.forName("com.cobblemon.mod.common.api.pokemon.ivs.Ivs");
                ivGetMethod = ivsClass.getMethod("get", statClass);
            }
            Object ivs = pokemon.getIvs();
            if (!checkIvField(ivs, "HP",                hpIvInput))   return false;
            if (!checkIvField(ivs, "ATTACK",            atkIvInput))  return false;
            if (!checkIvField(ivs, "DEFENCE",           defIvInput))  return false;
            if (!checkIvField(ivs, "SPECIAL_ATTACK",    spaIvInput))  return false;
            if (!checkIvField(ivs, "SPECIAL_DEFENCE",   spdIvInput))  return false;
            if (!checkIvField(ivs, "SPEED",             speIvInput))  return false;
        } catch (Throwable t) {
            ivCacheFailed = true;
        }
        return true;
    }

    /** 单个IV字段检查：反射取值→区间匹配 */
    @SuppressWarnings("unchecked")
    private boolean checkIvField(Object ivs, String statName, String filter) throws Exception {
        if (filter.isEmpty()) return true;
        Enum<?> stat = Enum.valueOf((Class<Enum>) statClass, statName);
        Integer val = (Integer) ivGetMethod.invoke(ivs, stat);
        int iv = val != null ? val : 0;
        return matchesRange(filter.trim(), iv);
    }

    /** 通用区间匹配：输入"50"→精确匹配,"1-100"→闭区间,""→全通过 */
    private boolean matchesRange(String input, int value) {
        if (input.isEmpty()) return true;
        int dashIdx = input.indexOf('-');
        if (dashIdx >= 0) {
            try {
                int lo = Integer.parseInt(input.substring(0, dashIdx).trim());
                int hi = Integer.parseInt(input.substring(dashIdx + 1).trim());
                return value >= lo && value <= hi;
            } catch (NumberFormatException e) { return false; }
        } else {
            try {
                return value == Integer.parseInt(input);
            } catch (NumberFormatException e) { return false; }
        }
    }

    // [线性-R06d] NPC训练师过滤：反射检查TrainerMob+名字匹配
    // [线性-R06g] RTC子模式=仅跟卡片目标(无卡片时不渲染); NPC子模式=手动追踪(名过滤/全NPC)
    public boolean matchesNpcFilter(Entity entity) {
        if (!npcMode || !radarTrackingEnabled) return false;
        try {
            Class<?> trainerMobClass = Class.forName("com.gitlab.srcmc.rctmod.world.entities.TrainerMob");
            if (!trainerMobClass.isInstance(entity)) return false;
        } catch (ClassNotFoundException e) { return false; }

        if (!npcNameInput.isEmpty()) {
            // 填了名 → 名字包含匹配（忽略子模式）
            String npcName = entity.getName().getString();
            return npcName.toLowerCase().contains(npcNameInput.toLowerCase());
        }
        // [线性-R06g] 空名分支：RTC模式=只跟卡片目标; NPC模式=全追踪
        if ("rtc".equals(npcSubMode)) {
            return cardTargetUuid != null && entity.getUUID().equals(cardTargetUuid);
        }
        // NPC子模式 → 追踪所有TrainerMob
        return true;
    }

    // [线性-R06f] 更新RCT图鉴追踪目标+最近TrainerMob（由渲染器每帧调用）
    public void updateCardTarget(net.minecraft.client.multiplayer.ClientLevel level) {
        cardTargetPos = null;
        cardTargetUuid = null;
        if (level == null) return;
        try {
            Class<?> tarClass = Class.forName("com.gitlab.srcmc.rctmod.client.renderer.TargetArrowRenderer");
            Object tar = tarClass.getMethod("getInstance").invoke(null);
            if (!(boolean) tarClass.getMethod("hasTarget").invoke(tar)) return;
            java.lang.reflect.Field f = tarClass.getDeclaredField("target");
            f.setAccessible(true);
            cardTargetPos = (Vec3) f.get(tar);
            if (cardTargetPos == null) return;

            // [线性-R06f] 找离卡片目标最近的TrainerMob (16方块内)
            Class<?> tmClass = Class.forName("com.gitlab.srcmc.rctmod.world.entities.TrainerMob");
            double bestDist = 256.0; // 16²
            for (Entity e : level.entitiesForRendering()) {
                if (!tmClass.isInstance(e)) continue;
                double d = e.position().distanceToSqr(cardTargetPos);
                if (d < bestDist) { bestDist = d; cardTargetUuid = e.getUUID(); }
            }
        } catch (Throwable ignored) {}
    }

    // ==== 工具方法 ====

    /** 从对象提取字符串ID */
    private static String extractStringId(Object obj) {
        if (obj == null) return "";
        try {
            Object nameObj = obj.getClass().getMethod("getName").invoke(obj);
            if (nameObj instanceof net.minecraft.resources.ResourceLocation rl) {
                return rl.getPath();
            }
            if (nameObj instanceof String s && !s.isEmpty()) {
                return s;
            }
        } catch (Throwable ignored) {}
        try {
            Object nameObj = obj.getClass().getMethod("name").invoke(obj);
            if (nameObj instanceof String s && !s.isEmpty()) {
                return s;
            }
        } catch (Throwable ignored) {}
        String s = obj.toString();
        int at = s.lastIndexOf('@');
        if (at > 0) {
            String afterAt = s.substring(at + 1);
            if (afterAt.matches("[0-9a-fA-F]+")) {
                int dot = s.lastIndexOf('.', at);
                if (dot > 0) return s.substring(dot + 1, at);
                return s.substring(0, at);
            }
        }
        int colon = s.lastIndexOf(':');
        if (colon > 0 && colon < s.length() - 1) {
            return s.substring(colon + 1);
        }
        return s;
    }

    /** [线性-R03g] 判断是否为神兽（Species.labels含"legendary"或"ultra_beast"） */
    public static boolean isLegendarySpecies(Species species) {
        try {
            // 优先用公开getter getLabels(),不用setAccessible更安全
            Object labelsObj = species.getClass().getMethod("getLabels").invoke(species);
            if (labelsObj instanceof Set<?> labels) {
                for (Object label : labels) {
                    String labelStr = label.toString().toLowerCase();
                    if (labelStr.contains("legendary") || labelStr.contains("ultra_beast")) {
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            // getMethod失败备用:直读labels字段
            try {
                java.lang.reflect.Field f = species.getClass().getDeclaredField("labels");
                f.setAccessible(true);
                Object labelsObj = f.get(species);
                if (labelsObj instanceof Set<?> labels) {
                    for (Object label : labels) {
                        String labelStr = label.toString().toLowerCase();
                        if (labelStr.contains("legendary") || labelStr.contains("ultra_beast")) {
                            return true;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return false;
    }

    /** [线性-R03h] 获取模式对应的渲染颜色（RGB int） */
    public int getColorForMode(String mode) {
        return switch (mode) {
            case "legendary" -> 0xFFFFD700; // 金色
            case "shiny" -> 0xFF4488FF;     // 蓝色
            case "pokemon" -> 0xFF00FF00;   // 绿色
            case "npc" -> 0xFFAA00FF;       // 紫色 [线性-R06c]
            default -> 0xFFFFFFFF;
        };
    }

    /** [线性-R03i] 获取当前激活模式对应的颜色（多模式时返回null表示混合渲染） */
    public List<String> getActiveModesForEntity(PokemonEntity entity) {
        List<String> modes = new ArrayList<>();
        // [线性-R02a2] 追踪总开关未启用时返回空
        if (!radarTrackingEnabled) return modes;

        // [线性-R02a3] 排除有主人的精灵 — 用getOwnerUUID(),O(1)
        if (entity.getOwnerUUID() != null) return modes;
        Pokemon pokemon = entity.getPokemon();

        Species species = pokemon.getSpecies();
        if (species == null) return modes;

        int activeCount = activeModeCount();

        if (activeCount > 1) {
            // 多模式：每个激活模式独立判断
            if (legendaryMode && isLegendarySpecies(species)) modes.add("legendary");
            if (shinyMode && pokemon.getShiny()) modes.add("shiny");
            // [线性-fix] pokemon让路：传说/闪光已匹配时不叠加绿色，避免覆盖金/蓝色
            if (pokemonMode && !modes.contains("legendary") && !modes.contains("shiny")
                    && matchesSpeciesFilter(species.showdownId().toLowerCase())) modes.add("pokemon");
        } else {
            // 单模式：完整匹配检查
            if (matches(entity)) {
                if (legendaryMode) modes.add("legendary");
                if (shinyMode) modes.add("shiny");
                if (pokemonMode) modes.add("pokemon");
            }
        }
        return modes;
    }

    // [线性-R03j] 获取指定物种的可能特性列表（用于UI下拉栏过滤）
    public static List<String> getAbilitiesForSpecies(String speciesEnglishName) {
        List<String> enList = new ArrayList<>();
        if (speciesEnglishName == null || speciesEnglishName.isEmpty()) return enList;
        try {
            Class<?> pokemonSpeciesClass = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies");
            java.lang.reflect.Method getByName = pokemonSpeciesClass.getMethod("getByName", String.class);
            Object speciesObj = getByName.invoke(null, speciesEnglishName.toLowerCase());
            if (speciesObj == null) return enList;

            // Species.getAbilities() → Set<AbilityTemplate>
            Object abilitiesObj = speciesObj.getClass().getMethod("getAbilities").invoke(speciesObj);
            if (abilitiesObj instanceof Iterable<?> iter) {
                for (Object ab : iter) {
                    // AbilityTemplate.getRealName() 或 ResourceLocation
                    String id = extractStringId(ab);
                    if (id != null && !id.isEmpty() && !enList.contains(id)) {
                        enList.add(id.toLowerCase());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return enList;
    }

    // ==== 持久化：JSON文件存取 ====

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getSavePath() {
        // [线性-R08b] 保存路径：.minecraft/config/cobblemon_ts/radar_settings.json
        return Paths.get("config", "cobblemon_ts", "radar_settings.json");
    }

    /** [线性-R08a] 从文件加载设置 */
    public void loadFromFile() {
        try {
            Path path = getSavePath();
            if (!Files.exists(path)) return;
            String json = Files.readString(path);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("legendaryMode")) legendaryMode = root.get("legendaryMode").getAsBoolean();
            if (root.has("shinyMode")) shinyMode = root.get("shinyMode").getAsBoolean();
            if (root.has("pokemonMode")) pokemonMode = root.get("pokemonMode").getAsBoolean();
            if (root.has("npcMode")) npcMode = root.get("npcMode").getAsBoolean();
            if (root.has("npcSubMode")) npcSubMode = root.get("npcSubMode").getAsString();
            if (root.has("radarTrackingEnabled")) radarTrackingEnabled = root.get("radarTrackingEnabled").getAsBoolean();
            if (root.has("batchSpeciesInput")) batchSpeciesInput = root.get("batchSpeciesInput").getAsString();
            if (root.has("singleSpeciesInput")) singleSpeciesInput = root.get("singleSpeciesInput").getAsString();
            if (root.has("genderInput")) genderInput = root.get("genderInput").getAsString();
            if (root.has("abilityInput")) abilityInput = root.get("abilityInput").getAsString();
            if (root.has("natureInput")) natureInput = root.get("natureInput").getAsString();
            if (root.has("levelInput")) levelInput = root.get("levelInput").getAsString();
            if (root.has("hpIvInput")) hpIvInput = root.get("hpIvInput").getAsString();
            if (root.has("atkIvInput")) atkIvInput = root.get("atkIvInput").getAsString();
            if (root.has("defIvInput")) defIvInput = root.get("defIvInput").getAsString();
            if (root.has("spaIvInput")) spaIvInput = root.get("spaIvInput").getAsString();
            if (root.has("spdIvInput")) spdIvInput = root.get("spdIvInput").getAsString();
            if (root.has("speIvInput")) speIvInput = root.get("speIvInput").getAsString();
            if (root.has("npcNameInput")) npcNameInput = root.get("npcNameInput").getAsString();
            if (root.has("radarRange")) radarRange = root.get("radarRange").getAsInt();
        } catch (Throwable ignored) {}
    }

    /** [线性-R08c] 保存设置到文件（关闭编辑器或切换追踪时调用） */
    public void saveToFile() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("legendaryMode", legendaryMode);
            root.addProperty("shinyMode", shinyMode);
            root.addProperty("pokemonMode", pokemonMode);
            root.addProperty("npcMode", npcMode);
            root.addProperty("npcSubMode", npcSubMode);
            root.addProperty("radarTrackingEnabled", radarTrackingEnabled);
            root.addProperty("batchSpeciesInput", batchSpeciesInput);
            root.addProperty("singleSpeciesInput", singleSpeciesInput);
            root.addProperty("genderInput", genderInput);
            root.addProperty("abilityInput", abilityInput);
            root.addProperty("natureInput", natureInput);
            root.addProperty("levelInput", levelInput);
            root.addProperty("hpIvInput", hpIvInput);
            root.addProperty("atkIvInput", atkIvInput);
            root.addProperty("defIvInput", defIvInput);
            root.addProperty("spaIvInput", spaIvInput);
            root.addProperty("spdIvInput", spdIvInput);
            root.addProperty("speIvInput", speIvInput);
            root.addProperty("npcNameInput", npcNameInput);
            root.addProperty("radarRange", radarRange);

            Path path = getSavePath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(root));
        } catch (Throwable ignored) {}
    }
}