package bumbumapps.a2048game;

import android.graphics.Color;

/** A colour scheme (theme + tile skin). All screens are drawn from these values. */
public final class Skin {
    public static final String BENTO = "bento";
    public static final String NIGHT = "night";
    public static final String PASTEL = "pastel";
    public static final String CONTRAST = "contrast";
    public static final String[] ALL = {BENTO, NIGHT, PASTEL, CONTRAST};

    public final String key;
    public final boolean dark;
    public final int bg, surface, surface2, board, empty, wall, wallMark;
    public final int text, textSecondary, accent, onAccent, divider;
    private final int[] tiles;
    private final int tileTextLight, tileTextDark;
    private final boolean darkTextOnTiles;
    /** Tile values from which the number is drawn in the "light" colour (used by High Contrast). */
    private final int lightTextFrom;

    private Skin(String key, boolean dark, int bg, int surface, int surface2, int board, int empty, int wall,
                 int wallMark, int text, int textSecondary, int accent, int onAccent, int divider,
                 int[] tiles, int tileTextLight, int tileTextDark, boolean darkTextOnTiles, int lightTextFrom) {
        this.key = key;
        this.dark = dark;
        this.bg = bg;
        this.surface = surface;
        this.surface2 = surface2;
        this.board = board;
        this.empty = empty;
        this.wall = wall;
        this.wallMark = wallMark;
        this.text = text;
        this.textSecondary = textSecondary;
        this.accent = accent;
        this.onAccent = onAccent;
        this.divider = divider;
        this.tiles = tiles;
        this.tileTextLight = tileTextLight;
        this.tileTextDark = tileTextDark;
        this.darkTextOnTiles = darkTextOnTiles;
        this.lightTextFrom = lightTextFrom;
    }

    private static int c(String hex) {
        return Color.parseColor(hex);
    }

    private static int[] palette(String... hex) {
        int[] a = new int[hex.length];
        for (int i = 0; i < hex.length; i++) {
            a[i] = c(hex[i]);
        }
        return a;
    }

    private static final Skin BENTO_SKIN = new Skin(BENTO, false, c("#F3F4F8"), c("#FFFFFF"), c("#ECEDF3"),
            c("#FFFFFF"), c("#ECEDF3"), c("#C9CCDA"), c("#A9ADC0"), c("#0E1222"), c("#5B6275"), c("#F08A24"),
            c("#FFFFFF"), c("#E6E8F0"),
            palette("#5B6B8C", "#4E7FA8", "#2E9C8A", "#3BB273", "#E2A93B", "#E5773A", "#E5484D", "#D6336C",
                    "#A855F7", "#6366F1", "#F5B83D", "#FF6B70", "#26A69A", "#7C4DFF", "#111827"),
            c("#FFFFFF"), c("#0E1222"), false, 0);

    private static final Skin NIGHT_SKIN = new Skin(NIGHT, true, c("#0D0F16"), c("#181B26"), c("#232736"),
            c("#181B26"), c("#232736"), c("#3A4056"), c("#565D78"), c("#F2F4FA"), c("#9AA1B5"), c("#FFA24A"),
            c("#1A1204"), c("#262B3B"),
            palette("#6E7FA3", "#5B93C4", "#35B29E", "#44C484", "#E8B64A", "#F08848", "#F0585C", "#E34A84",
                    "#B66BFA", "#7C7FF5", "#F7C253", "#FF7E82", "#3BC4B8", "#9168FF", "#F3F4F8"),
            c("#FFFFFF"), c("#0E1222"), false, 0);

    private static final Skin PASTEL_SKIN = new Skin(PASTEL, false, c("#FFF7F2"), c("#FFFFFF"), c("#F6E9E2"),
            c("#F6E9E2"), c("#EBDAD1"), c("#D9C5BA"), c("#BCA597"), c("#3A3347"), c("#7A6F86"), c("#E8826B"),
            c("#FFFFFF"), c("#F0E2DA"),
            palette("#BFD7EA", "#B5E2D0", "#CDEAC0", "#F6E6A6", "#F9CFA0", "#F7B6B0", "#F2B5D4", "#D5B8EA",
                    "#B9BDF2", "#A8D5F2", "#FFD76A", "#FFB3A7", "#A7E3E0", "#C9B6F5", "#E5D8CF"),
            c("#3A3347"), c("#3A3347"), true, 0);

    private static final Skin CONTRAST_SKIN = new Skin(CONTRAST, true, c("#000000"), c("#121212"), c("#262626"),
            c("#000000"), c("#2B2B2B"), c("#555555"), c("#FFFFFF"), c("#FFFFFF"), c("#CFCFCF"), c("#FFEB3B"),
            c("#000000"), c("#3A3A3A"),
            palette("#FFFFFF", "#FFEB3B", "#FF9800", "#FF5722", "#00E5FF", "#00B0FF", "#76FF03", "#E040FB",
                    "#F50057", "#2962FF", "#FFD600", "#FF1744", "#00E676", "#651FFF", "#FFFFFF"),
            c("#FFFFFF"), c("#000000"), true, 512);

    public static Skin get(String key) {
        if (NIGHT.equals(key)) {
            return NIGHT_SKIN;
        }
        if (PASTEL.equals(key)) {
            return PASTEL_SKIN;
        }
        if (CONTRAST.equals(key)) {
            return CONTRAST_SKIN;
        }
        return BENTO_SKIN;
    }

    private static int exponent(int value) {
        int e = 0;
        int v = Math.max(2, value);
        while (v > 2) {
            v >>= 1;
            e++;
        }
        return e;
    }

    public int tileColor(int value) {
        return tiles[Math.min(tiles.length - 1, exponent(value))];
    }

    public int tileTextColor(int value) {
        if (!darkTextOnTiles) {
            return tileTextLight;
        }
        if (lightTextFrom > 0 && value >= lightTextFrom && value != 2048) {
            return tileTextLight;
        }
        return tileTextDark;
    }
}
