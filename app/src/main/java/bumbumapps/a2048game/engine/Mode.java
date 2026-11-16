package bumbumapps.a2048game.engine;

/** The playable game modes and their rules. */
public enum Mode {
    //            key       size  walls timeSec moves undo
    CLASSIC("classic", 4, false, 0, 0, 5),
    TIME("time", 4, false, 240, 0, 3),
    MOVES("moves", 4, false, 0, 100, 3),
    DAILY("daily", 4, false, 0, 0, 1),
    BLOCKS("blocks", 5, true, 0, 0, 5),
    ZEN("zen", 4, false, 0, 0, -1);

    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 8;

    public final String key;
    public final int defaultSize;
    public final boolean walls;
    /** Time limit in seconds, 0 = none. */
    public final int timeLimitSec;
    /** Move limit, 0 = none. */
    public final int moveLimit;
    /** Undo steps per game, -1 = unlimited. */
    public final int undoLimit;

    Mode(String key, int defaultSize, boolean walls, int timeLimitSec, int moveLimit, int undoLimit) {
        this.key = key;
        this.defaultSize = defaultSize;
        this.walls = walls;
        this.timeLimitSec = timeLimitSec;
        this.moveLimit = moveLimit;
        this.undoLimit = undoLimit;
    }

    /** Only Classic and Blocks let the player choose the board size. */
    public boolean sizeSelectable() {
        return this == CLASSIC || this == BLOCKS;
    }

    /** Time Attack and Limited Moves end as soon as the target tile is made. */
    public boolean endsOnWin() {
        return this == TIME || this == MOVES;
    }

    public int sizeOrDefault(int size) {
        return sizeSelectable() ? Math.max(MIN_SIZE, Math.min(MAX_SIZE, size)) : defaultSize;
    }

    /** Target tile for this mode on a board of the given size. */
    public int target(int size) {
        switch (this) {
            case TIME:
                return 256;
            case MOVES:
                return 128;
            case BLOCKS:
                return 1024;
            default:
                return classicTarget(size);
        }
    }

    public static int classicTarget(int size) {
        switch (size) {
            case 3:
                return 256;
            case 4:
                return 2048;
            case 5:
                return 4096;
            case 6:
                return 8192;
            case 7:
                return 16384;
            default:
                return 32768;
        }
    }

    /** Storage key for stats and saves: classic and blocks are tracked per board size. */
    public String storeKey(int size) {
        return sizeSelectable() ? key + "_" + sizeOrDefault(size) : key;
    }

    public static Mode fromKey(String key) {
        for (Mode m : values()) {
            if (m.key.equals(key)) {
                return m;
            }
        }
        return CLASSIC;
    }
}
