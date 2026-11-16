package bumbumapps.a2048game;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import bumbumapps.a2048game.engine.GameEngine;
import bumbumapps.a2048game.engine.Mode;
import bumbumapps.a2048game.engine.StatsBook;

/** SharedPreferences-backed storage: one saved game per mode, statistics, and settings. */
public final class Store {
    private static final String PREFS = "game_2048_v2";
    private static Store instance;

    private final SharedPreferences prefs;
    private final Context app;
    private StatsBook stats;

    private Store(Context c) {
        app = c.getApplicationContext();
        prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized Store get(Context c) {
        if (instance == null) {
            instance = new Store(c);
        }
        return instance;
    }

    // ---- settings ----

    public Skin skin() {
        String saved = prefs.getString("skin", null);
        if (saved == null) {
            boolean night = (app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES;
            return Skin.get(night ? Skin.NIGHT : Skin.BENTO);
        }
        return Skin.get(saved);
    }

    public void setSkin(String key) {
        prefs.edit().putString("skin", key).apply();
    }

    public boolean haptics() {
        return prefs.getBoolean("haptics", true);
    }

    public void setHaptics(boolean on) {
        prefs.edit().putBoolean("haptics", on).apply();
    }

    public int classicSize() {
        return Mode.CLASSIC.sizeOrDefault(prefs.getInt("classic_size", 4));
    }

    public void setClassicSize(int size) {
        prefs.edit().putInt("classic_size", Mode.CLASSIC.sizeOrDefault(size)).apply();
    }

    public int blocksSize() {
        return Mode.BLOCKS.sizeOrDefault(prefs.getInt("blocks_size", 5));
    }

    public void setBlocksSize(int size) {
        prefs.edit().putInt("blocks_size", Mode.BLOCKS.sizeOrDefault(size)).apply();
    }

    // ---- saved games ----

    /** Save slot name; the Daily Challenge gets a slot per day so yesterday's board is never resumed. */
    public static String slot(Mode mode, int size, long epochDay) {
        String k = mode.storeKey(size);
        return mode == Mode.DAILY ? k + "_" + epochDay : k;
    }

    public void saveGame(String slot, GameEngine g) {
        prefs.edit().putString("save_" + slot, g.serialize()).putString("last_slot", slot).apply();
    }

    /** Returns the saved, unfinished game or null. */
    public GameEngine loadGame(String slot) {
        GameEngine g = GameEngine.deserialize(prefs.getString("save_" + slot, null));
        if (g == null || (g.isOver() && g.mode != Mode.ZEN)) {
            return null;
        }
        return g;
    }

    public void clearGame(String slot) {
        prefs.edit().remove("save_" + slot).apply();
    }

    public boolean hasGame(String slot) {
        return loadGame(slot) != null;
    }

    public String lastSlot() {
        return prefs.getString("last_slot", null);
    }

    // ---- stats ----

    public synchronized StatsBook stats() {
        if (stats == null) {
            stats = StatsBook.parse(prefs.getString("stats", null));
        }
        return stats;
    }

    public synchronized void saveStats() {
        if (stats != null) {
            prefs.edit().putString("stats", stats.serialize()).apply();
        }
    }
}
