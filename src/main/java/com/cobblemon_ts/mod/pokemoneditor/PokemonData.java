package com.cobblemon_ts.mod.pokemoneditor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 宝可梦编辑数据容器。所有默认值统一在 {@link #initializeDefaults()} 中设置。
 * 编辑新宝可梦时（从空槽位开始）：
 *   - 默认物种：""（空，表示待用户设置）
 *   - 默认等级：1
 *   - 默认 HP：当前HP=最大HP（满血）
 *   - 默认性格：""（空，待用户选择）
 *   - 默认特性：""（空，待用户选择）
 *   - 默认技能 PP：当前PP=最大PP（技能全满）
 *   - 默认闪光：false
 *   - 默认状态：""（无状态）
 *   - 默认性别：0（雄性）
 */
public class PokemonData {
    // ==================== 默认值常量（集中管理）====================
    public static final boolean DEFAULT_EXISTS = false;
    public static final String DEFAULT_SPECIES = "";
    public static final String DEFAULT_DISPLAY_NAME = "";
    public static final String DEFAULT_FORM = "";
    public static final int DEFAULT_LEVEL = 1;          // 新建宝可梦默认等级 1
    public static final int DEFAULT_EXPERIENCE = 0;
    public static final int DEFAULT_FRIENDSHIP = 0;
    public static final boolean DEFAULT_SHINY = false;
    public static final int DEFAULT_IV = 0;             // 新宝可梦 IV=0
    public static final int DEFAULT_EV = 0;             // 新宝可梦 EV=0
    public static final String DEFAULT_NATURE = "";     // 默认性格：下拉栏首位
    public static final String DEFAULT_ABILITY = "";    // 默认特性：下拉栏首位
    public static final String DEFAULT_MOVE = "";       // 技能默认空
    public static final String DEFAULT_HELD_ITEM = "";  // 携带物默认空
    public static final String DEFAULT_STATUS = "";     // 状态默认：无
    public static final int DEFAULT_GENDER = 0;         // 默认雄性

    // ==================== 数据字段 ====================
    public boolean exists = DEFAULT_EXISTS;
    public UUID pokemonUUID = null;
    public String speciesName = DEFAULT_SPECIES;
    public String displayName = DEFAULT_DISPLAY_NAME;
    public String formName = DEFAULT_FORM;
    public int level = DEFAULT_LEVEL;
    public int experience = DEFAULT_EXPERIENCE;
    public int friendship = DEFAULT_FRIENDSHIP;
    public boolean shiny = DEFAULT_SHINY;
    public int ivHp = DEFAULT_IV, ivAtk = DEFAULT_IV, ivDef = DEFAULT_IV, ivSpA = DEFAULT_IV, ivSpD = DEFAULT_IV, ivSpe = DEFAULT_IV;
    public int evHp = DEFAULT_EV, evAtk = DEFAULT_EV, evDef = DEFAULT_EV, evSpA = DEFAULT_EV, evSpD = DEFAULT_EV, evSpe = DEFAULT_EV;
    public String nature = DEFAULT_NATURE;
    public String ability = DEFAULT_ABILITY;
    public String move1 = DEFAULT_MOVE, move2 = DEFAULT_MOVE, move3 = DEFAULT_MOVE, move4 = DEFAULT_MOVE;
    public int move1PP = 0, move2PP = 0, move3PP = 0, move4PP = 0; // 当前PP
    public int move1MaxPP = 0, move2MaxPP = 0, move3MaxPP = 0, move4MaxPP = 0; // PP最大值(受pp_max影响)
    public int move1Raised = 0, move2Raised = 0, move3Raised = 0, move4Raised = 0; // pp_max提升等级 (0=未提升,1=已用pp_max)
    public int currentHp = 1, maxHp = 1;
    public String heldItem = DEFAULT_HELD_ITEM;
    public String status = DEFAULT_STATUS; // 状态名称：""/"poison"/"paralysis"/"burn"/"freeze"/"sleep"/"bad_poison"
    public int gender = DEFAULT_GENDER; // 0=雄性 1=雌性 2=无性别
    public List<String> possibleAbilities = new ArrayList<>(); // 该物种可能拥有的特性ID

    // ★ 追踪本次编辑器打开期间，用户手动修改了哪些字段
    // 用于：1. 服务端只修改这些字段；2. 物种/等级变化后的自动恢复逻辑按需跳过
    // 保存后、切换槽位、关闭编辑器时都会清空
    public java.util.Set<String> modifiedFields = new java.util.HashSet<>();

    public void markModified(String field) {
        if (modifiedFields == null) modifiedFields = new java.util.HashSet<>();
        modifiedFields.add(field);
    }

    public boolean wasModified(String field) {
        return modifiedFields != null && modifiedFields.contains(field);
    }

    public void clearModified() {
        if (modifiedFields == null) modifiedFields = new java.util.HashSet<>();
        else modifiedFields.clear();
    }

    /**
     * 构造一个宝可梦数据对象，所有字段按默认值初始化。
     * 从空槽位编辑时，所有字段都是默认值。
     */
    public PokemonData() {
        initializeDefaults();
    }

    /**
     * 初始化所有字段为默认值（集中管理，便于维护）
     */
    public void initializeDefaults() {
        this.exists = DEFAULT_EXISTS;
        this.pokemonUUID = null;
        this.speciesName = DEFAULT_SPECIES;
        this.displayName = DEFAULT_DISPLAY_NAME;
        this.formName = DEFAULT_FORM;
        this.level = DEFAULT_LEVEL;
        this.experience = DEFAULT_EXPERIENCE;
        this.friendship = DEFAULT_FRIENDSHIP;
        this.shiny = DEFAULT_SHINY;
        this.ivHp = DEFAULT_IV; this.ivAtk = DEFAULT_IV; this.ivDef = DEFAULT_IV;
        this.ivSpA = DEFAULT_IV; this.ivSpD = DEFAULT_IV; this.ivSpe = DEFAULT_IV;
        this.evHp = DEFAULT_EV; this.evAtk = DEFAULT_EV; this.evDef = DEFAULT_EV;
        this.evSpA = DEFAULT_EV; this.evSpD = DEFAULT_EV; this.evSpe = DEFAULT_EV;
        this.nature = DEFAULT_NATURE;
        this.ability = DEFAULT_ABILITY;
        this.move1 = DEFAULT_MOVE; this.move2 = DEFAULT_MOVE;
        this.move3 = DEFAULT_MOVE; this.move4 = DEFAULT_MOVE;
        this.move1PP = 0; this.move2PP = 0; this.move3PP = 0; this.move4PP = 0;
        this.move1MaxPP = 0; this.move2MaxPP = 0; this.move3MaxPP = 0; this.move4MaxPP = 0;
        this.currentHp = 1; this.maxHp = 1;
        this.heldItem = DEFAULT_HELD_ITEM;
        this.status = DEFAULT_STATUS;
        this.gender = DEFAULT_GENDER;
        if (this.possibleAbilities == null) this.possibleAbilities = new ArrayList<>();
        else this.possibleAbilities.clear();
    }

    /**
     * 辅助：自动把所有技能的当前 PP 设置为最大 PP（技能全满）
     * 用于修改物种/等级后自动补满 PP
     */
    public void refillAllPP() {
        if (this.move1MaxPP > 0) this.move1PP = this.move1MaxPP;
        if (this.move2MaxPP > 0) this.move2PP = this.move2MaxPP;
        if (this.move3MaxPP > 0) this.move3PP = this.move3MaxPP;
        if (this.move4MaxPP > 0) this.move4PP = this.move4MaxPP;
    }

    /**
     * 辅助：将当前 HP 设置为最大 HP（满血）
     * 用于修改物种/等级后自动满血
     */
    public void fullHealHP() {
        if (this.maxHp > 0) this.currentHp = this.maxHp;
    }

    /**
     * 克隆数据（用于保存原始值快照，检测用户修改了哪些字段）
     */
    public PokemonData snapshot() {
        PokemonData c = new PokemonData();
        c.exists = this.exists;
        c.pokemonUUID = this.pokemonUUID;
        c.speciesName = this.speciesName;
        c.displayName = this.displayName;
        c.formName = this.formName;
        c.level = this.level;
        c.experience = this.experience;
        c.friendship = this.friendship;
        c.shiny = this.shiny;
        c.ivHp = this.ivHp; c.ivAtk = this.ivAtk; c.ivDef = this.ivDef;
        c.ivSpA = this.ivSpA; c.ivSpD = this.ivSpD; c.ivSpe = this.ivSpe;
        c.evHp = this.evHp; c.evAtk = this.evAtk; c.evDef = this.evDef;
        c.evSpA = this.evSpA; c.evSpD = this.evSpD; c.evSpe = this.evSpe;
        c.nature = this.nature;
        c.ability = this.ability;
        c.move1 = this.move1; c.move2 = this.move2; c.move3 = this.move3; c.move4 = this.move4;
        c.move1PP = this.move1PP; c.move2PP = this.move2PP; c.move3PP = this.move3PP; c.move4PP = this.move4PP;
        c.move1MaxPP = this.move1MaxPP; c.move2MaxPP = this.move2MaxPP;
        c.move3MaxPP = this.move3MaxPP; c.move4MaxPP = this.move4MaxPP;
        c.currentHp = this.currentHp; c.maxHp = this.maxHp;
        c.heldItem = this.heldItem;
        c.status = this.status;
        c.gender = this.gender;
        c.possibleAbilities = new ArrayList<>(this.possibleAbilities);
        return c;
    }

    /**
     * 比较两个 PokemonData 的字段差异，返回被修改的字段名集合
     *（用于生成 modifiedFields）
     */
    public java.util.Set<String> diffFrom(PokemonData base) {
        java.util.Set<String> diff = new java.util.HashSet<>();
        if (base == null) return diff;
        if (!safeEquals(speciesName, base.speciesName)) diff.add("speciesName");
        if (!safeEquals(formName, base.formName)) diff.add("formName");
        if (level != base.level) diff.add("level");
        if (experience != base.experience) diff.add("experience");
        if (!safeEquals(displayName, base.displayName)) diff.add("displayName");
        if (shiny != base.shiny) diff.add("shiny");
        if (friendship != base.friendship) diff.add("friendship");
        if (ivHp != base.ivHp) diff.add("ivHp");
        if (ivAtk != base.ivAtk) diff.add("ivAtk");
        if (ivDef != base.ivDef) diff.add("ivDef");
        if (ivSpA != base.ivSpA) diff.add("ivSpA");
        if (ivSpD != base.ivSpD) diff.add("ivSpD");
        if (ivSpe != base.ivSpe) diff.add("ivSpe");
        if (evHp != base.evHp) diff.add("evHp");
        if (evAtk != base.evAtk) diff.add("evAtk");
        if (evDef != base.evDef) diff.add("evDef");
        if (evSpA != base.evSpA) diff.add("evSpA");
        if (evSpD != base.evSpD) diff.add("evSpD");
        if (evSpe != base.evSpe) diff.add("evSpe");
        if (!safeEquals(nature, base.nature)) diff.add("nature");
        if (!safeEquals(ability, base.ability)) diff.add("ability");
        if (!safeEquals(move1, base.move1)) diff.add("move1");
        if (!safeEquals(move2, base.move2)) diff.add("move2");
        if (!safeEquals(move3, base.move3)) diff.add("move3");
        if (!safeEquals(move4, base.move4)) diff.add("move4");
        if (move1PP != base.move1PP) diff.add("move1PP");
        if (move2PP != base.move2PP) diff.add("move2PP");
        if (move3PP != base.move3PP) diff.add("move3PP");
        if (move4PP != base.move4PP) diff.add("move4PP");
        if (move1MaxPP != base.move1MaxPP) diff.add("move1MaxPP");
        if (move2MaxPP != base.move2MaxPP) diff.add("move2MaxPP");
        if (move3MaxPP != base.move3MaxPP) diff.add("move3MaxPP");
        if (move4MaxPP != base.move4MaxPP) diff.add("move4MaxPP");
        if (currentHp != base.currentHp) diff.add("currentHp");
        if (maxHp != base.maxHp) diff.add("maxHp");
        if (!safeEquals(heldItem, base.heldItem)) diff.add("heldItem");
        if (gender != base.gender) diff.add("gender");
        if (!safeEquals(status, base.status)) diff.add("status");
        return diff;
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.equalsIgnoreCase(b);
    }
}