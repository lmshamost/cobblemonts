package com.cobblemon_ts.mod.pokemoneditor;

import com.cobblemon_ts.mod.CobblemonTS;
import com.cobblemon_ts.mod.DebugConfig;

public class PokemonEditorDebug {

    // ================ 开关读取（集中在 DebugConfig 独立配置文件中） ================

    /** debug_editor：宝可梦编辑器调试（保存流程、UI刷新、队伍同步等） */
    public static boolean isDebugEnabled() {
        try {
            return Boolean.TRUE.equals(DebugConfig.DEBUG_EDITOR.get());
        } catch (Exception e) {
            return false;
        }
    }

    /** debug_moves：技能读写调试（MoveSet探测、按索引读写、PP/MaxPP、独立实例等） */
    public static boolean isMovesDebugEnabled() {
        try {
            return Boolean.TRUE.equals(DebugConfig.DEBUG_MOVES.get());
        } catch (Exception e) {
            return false;
        }
    }

    // ================ 工具方法：支持 SLF4J 风格的 {} 占位符 ================
    private static String fmt(String format, Object... args) {
        if (args == null || args.length == 0) return format;
        StringBuilder sb = new StringBuilder();
        int argIdx = 0;
        int i = 0;
        while (i < format.length()) {
            if (i < format.length() - 1 && format.charAt(i) == '{' && format.charAt(i + 1) == '}') {
                if (argIdx < args.length) {
                    sb.append(args[argIdx]);
                    argIdx++;
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(format.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    // ================ 编辑器流程日志（受 debug.debug_editor 控制） ================

    public static void log(String message) {
        if (isDebugEnabled()) {
            CobblemonTS.LOGGER.info("[PokemonEditor] " + message);
        }
    }

    public static void logEditor(String format, Object... args) {
        if (isDebugEnabled()) {
            CobblemonTS.LOGGER.info("[Bridge] " + fmt(format, args));
        }
    }

    public static void warnEditor(String format, Object... args) {
        if (isDebugEnabled()) {
            CobblemonTS.LOGGER.warn("[Bridge] " + fmt(format, args));
        }
    }

    public static void errorEditor(String format, Object... args) {
        if (isDebugEnabled()) {
            CobblemonTS.LOGGER.error("[Bridge] " + fmt(format, args));
        }
    }

    public static void errorEditor(String message, Throwable throwable) {
        if (isDebugEnabled()) {
            CobblemonTS.LOGGER.error("[Bridge] " + message, throwable);
        }
    }

    // ================ 技能读写日志（受 debug.debug_moves 控制，或 debug_editor 全开时也输出） ================

    public static void logMoves(String message) {
        if (isMovesDebugEnabled() || isDebugEnabled()) {
            CobblemonTS.LOGGER.info("[PokemonEditor][Moves] " + message);
        }
    }

    public static void infoMoves(String format, Object... args) {
        if (isMovesDebugEnabled() || isDebugEnabled()) {
            CobblemonTS.LOGGER.info("[Bridge]" + fmt(format, args));
        }
    }

    public static void warnMoves(String format, Object... args) {
        if (isMovesDebugEnabled() || isDebugEnabled()) {
            CobblemonTS.LOGGER.warn("[Bridge]" + fmt(format, args));
        }
    }

    public static void errorMoves(String format, Object... args) {
        if (isMovesDebugEnabled() || isDebugEnabled()) {
            CobblemonTS.LOGGER.error("[Bridge]" + fmt(format, args));
        }
    }

    public static void errorMoves(String message, Throwable throwable) {
        if (isMovesDebugEnabled() || isDebugEnabled()) {
            CobblemonTS.LOGGER.error("[Bridge] " + message, throwable);
        }
    }

    // ================ 错误级别（始终输出，不受开关控制） ================

    public static void logError(String message, Throwable throwable) {
        CobblemonTS.LOGGER.error("[PokemonEditor] " + message, throwable);
    }

    public static void logError(String message) {
        CobblemonTS.LOGGER.error("[PokemonEditor] " + message);
    }
}