package bumbumapps.a2048game;

import android.content.Context;
import android.graphics.Typeface;

/** Plus Jakarta Sans from assets (the Bento type family); falls back to the system font if loading fails. */
public final class Fonts {
    private static Typeface medium, bold, extra;

    private Fonts() {
    }

    private static Typeface load(Context c, String file, Typeface fallback) {
        try {
            return Typeface.createFromAsset(c.getApplicationContext().getAssets(), file);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public static synchronized Typeface medium(Context c) {
        if (medium == null) {
            medium = load(c, "PlusJakartaSans-Medium.ttf", Typeface.DEFAULT);
        }
        return medium;
    }

    public static synchronized Typeface bold(Context c) {
        if (bold == null) {
            bold = load(c, "PlusJakartaSans-Bold.ttf", Typeface.DEFAULT_BOLD);
        }
        return bold;
    }

    public static synchronized Typeface extra(Context c) {
        if (extra == null) {
            extra = load(c, "PlusJakartaSans-ExtraBold.ttf", Typeface.DEFAULT_BOLD);
        }
        return extra;
    }
}
