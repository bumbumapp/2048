package bumbumapps.a2048game;

import android.content.Context;
import android.graphics.Color;

import bumbumapps.a2048game.engine.Mode;

/** Display data (name, icon, colour) for each mode. */
public final class Modes {
    private Modes() {
    }

    public static String name(Context c, Mode m) {
        switch (m) {
            case TIME:
                return c.getString(R.string.mode_time);
            case MOVES:
                return c.getString(R.string.mode_moves);
            case DAILY:
                return c.getString(R.string.mode_daily);
            case BLOCKS:
                return c.getString(R.string.mode_blocks);
            case ZEN:
                return c.getString(R.string.mode_zen);
            default:
                return c.getString(R.string.mode_classic);
        }
    }

    public static String sizeLabel(int size) {
        return size + "×" + size;
    }

    /** Name for a store key such as "classic_5" or "time". */
    public static String nameForKey(Context c, String key) {
        int us = key.indexOf('_');
        Mode m = Mode.fromKey(us > 0 ? key.substring(0, us) : key);
        String n = name(c, m);
        if (us > 0) {
            try {
                n += " " + sizeLabel(Integer.parseInt(key.substring(us + 1)));
            } catch (NumberFormatException ignored) {
                // plain name
            }
        }
        return n;
    }

    public static int icon(Mode m) {
        switch (m) {
            case TIME:
                return R.drawable.ic_timer;
            case MOVES:
                return R.drawable.ic_moves;
            case DAILY:
                return R.drawable.ic_flame;
            case BLOCKS:
                return R.drawable.ic_blocks;
            case ZEN:
                return R.drawable.ic_zen;
            default:
                return R.drawable.ic_grid;
        }
    }

    public static int color(Mode m) {
        switch (m) {
            case TIME:
                return Color.parseColor("#4E7FA8");
            case MOVES:
                return Color.parseColor("#E5773A");
            case DAILY:
                return Color.parseColor("#E5484D");
            case BLOCKS:
                return Color.parseColor("#6366F1");
            case ZEN:
                return Color.parseColor("#3BB273");
            default:
                return Color.parseColor("#F08A24");
        }
    }

    public static String goal(Context c, Mode m, int size) {
        return c.getString(R.string.goal_reach, Ui.number(m.target(size)));
    }
}
