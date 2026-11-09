package com.cobblemon_ts.mod.pokemoneditor;

public class PokemonEditorDebug {

    public static boolean isDebugEnabled() { return false; }
    public static boolean isMovesDebugEnabled() { return false; }

    public static void logEditor(String msg, Object... args) {}
    public static void warnEditor(String msg, Object... args) {}
    public static void errorEditor(String msg, Object... args) {}
    public static void infoMoves(String msg, Object... args) {}
    public static void warnMoves(String msg, Object... args) {}
    public static void errorMoves(String msg, Object... args) {}
    public static void infoPP(String msg, Object... args) {}
    public static void warnPP(String msg, Object... args) {}
    public static void errorPP(String msg, Object... args) {}
    public static void infoStatus(String msg, Object... args) {}
    public static void warnStatus(String msg, Object... args) {}
    public static void errorStatus(String msg, Object... args) {}
    public static void logError(String msg, Object... args) {}
    public static void log(String msg) {}
    public static void log(String msg, Object... args) {}
}