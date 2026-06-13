package com.cobblemon_ts.mod.pokemoneditor;

import java.util.ArrayList;
import java.util.List;

/**
 * 精灵栏队伍缓存持有者
 *
 * ★★★ 为什么不放在 mixin 包下？★★★
 * Mixin 框架会严格检查 mixin 包（com.cobblemon_ts.mod.mixin.*），
 * 这个包下的所有类都被认为是 Mixin 类，**不能被普通代码直接引用**！
 * 否则会抛出：
 *   IllegalClassLoadError: xxx is in a defined mixin package and cannot be referenced directly
 *
 * ★★★ Mixin 类中的方法必须是 private！★★★
 * 如果 Mixin 类中有 public 方法，游戏启动时会抛出：
 *   InvalidMixinException: contains non-private static method
 *
 * 所以缓存逻辑必须放在这个独立的普通 Java 类中，
 * 这样 InventoryScreenMixin 和 PokemonEditorScreen 都可以安全地访问它。
 */
public class PartyCacheHolder {

    // 缓存的队伍列表（6 个槽位，空槽为 null）
    private static List<Object> cachedParty;

    // 缓存生成的时间戳（毫秒）
    private static long cachedPartyTime;

    // ---- 获取缓存 ----

    public static List<Object> getCachedParty() {
        return cachedParty;
    }

    public static long getCachedPartyTime() {
        return cachedPartyTime;
    }

    // ---- 更新缓存 ----

    public static void setCachedParty(List<Object> party) {
        cachedParty = party;
    }

    public static void setCachedPartyTime(long time) {
        cachedPartyTime = time;
    }

    /**
     * 重置缓存：清空队伍数据，时间戳归零
     *
     * 任何修改了宝可梦队伍的操作后都应该调用此方法，
     * 否则 getPartyList() 在 2 秒内会返回旧的缓存数据。
     *
     * 调用场景：
     *   - 右键菜单 → 放生
     *   - 右键菜单 → 放入 PC
     *   - 右键菜单 → 交换位置
     *   - 精灵编辑器 → 修改属性后保存
     *   - 精灵编辑器 → 删除宝可梦
     */
    public static void resetPartyCache() {
        cachedParty = null;
        cachedPartyTime = 0;
    }

    /**
     * 便捷方法：保证 cachedParty 不为 null（空列表）
     */
    public static List<Object> ensurePartyList() {
        if (cachedParty == null) {
            cachedParty = new ArrayList<>();
        }
        return cachedParty;
    }
}