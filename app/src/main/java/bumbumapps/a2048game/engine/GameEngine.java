package bumbumapps.a2048game.engine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Pure game logic for every mode (no Android classes): slide and merge (each tile merges at most once per move),
 * spawning from a seeded generator, walls, win/lose rules, limited undo and text save/restore.
 */
public final class GameEngine {
    public static final int WALL = -1;
    public static final int PLAYING = 0;
    public static final int WON = 1;
    public static final int LOST = 2;

    public static final int REASON_NONE = 0;
    public static final int REASON_NO_MOVES = 1;
    public static final int REASON_TIME = 2;
    public static final int REASON_MOVES = 3;

    private static final int MAX_HISTORY = 20;

    public final Mode mode;
    public final int size;
    public final long seed;
    private final int[][] cells;
    private final SeededRandom rng;
    private final ArrayDeque<Snapshot> history = new ArrayDeque<>();

    private long score;
    private int moveCount;
    private long elapsedMs;
    private int undosLeft;
    private int status = PLAYING;
    private int reason = REASON_NONE;
    private boolean targetReached;
    private boolean justWon;

    private static final class Snapshot {
        final int[][] cells;
        final long score;
        final int moves;
        final long rngState;

        Snapshot(int[][] cells, long score, int moves, long rngState) {
            this.cells = cells;
            this.score = score;
            this.moves = moves;
            this.rngState = rngState;
        }
    }

    public GameEngine(Mode mode, int size, long seed) {
        this.mode = mode;
        this.size = mode.sizeOrDefault(size);
        this.seed = seed;
        this.cells = new int[this.size][this.size];
        this.rng = new SeededRandom(seed);
        this.undosLeft = mode.undoLimit;
        if (mode.walls) {
            int count = Math.max(1, this.size - 2);
            while (count > 0) {
                int r = rng.nextInt(this.size);
                int c = rng.nextInt(this.size);
                if (cells[r][c] != WALL) {
                    cells[r][c] = WALL;
                    count--;
                }
            }
        }
        spawn();
        spawn();
    }

    /** Seed for the Daily Challenge: the same for everybody on a given (UTC) day. */
    public static long dailySeed(long epochDay) {
        return new SeededRandom(epochDay * 1_000_003L + 2048L).nextLong();
    }

    // ---- read access ----

    public int get(int r, int c) {
        return cells[r][c];
    }

    public long getScore() {
        return score;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public int getStatus() {
        return status;
    }

    public int getReason() {
        return reason;
    }

    public boolean isOver() {
        return status != PLAYING;
    }

    public boolean isTargetReached() {
        return targetReached;
    }

    /** True once, right after the move that first made the target tile; reading clears it. */
    public boolean consumeJustWon() {
        boolean v = justWon;
        justWon = false;
        return v;
    }

    public int getTarget() {
        return mode.target(size);
    }

    public int getUndosLeft() {
        return undosLeft;
    }

    public boolean canUndo() {
        if (history.isEmpty() || undosLeft == 0) {
            return false;
        }
        return status == PLAYING || (mode == Mode.ZEN && status == LOST);
    }

    public int maxTile() {
        int max = 0;
        for (int[] row : cells) {
            for (int v : row) {
                max = Math.max(max, v);
            }
        }
        return max;
    }

    public int movesLeft() {
        return mode.moveLimit > 0 ? Math.max(0, mode.moveLimit - moveCount) : -1;
    }

    public long timeLeftMs() {
        return mode.timeLimitSec > 0 ? Math.max(0, mode.timeLimitSec * 1000L - elapsedMs) : -1;
    }

    public int emptyCount() {
        int n = 0;
        for (int[] row : cells) {
            for (int v : row) {
                if (v == 0) {
                    n++;
                }
            }
        }
        return n;
    }

    // ---- actions ----

    /** Advances the clock; ends a Time Attack game when the time is up. Ignored when the game is over. */
    public void tick(long deltaMs) {
        if (status != PLAYING || deltaMs <= 0) {
            return;
        }
        elapsedMs += deltaMs;
        if (mode.timeLimitSec > 0 && elapsedMs >= mode.timeLimitSec * 1000L && !targetReached) {
            elapsedMs = mode.timeLimitSec * 1000L;
            status = LOST;
            reason = REASON_TIME;
        }
    }

    public MoveResult move(Dir dir) {
        if (status != PLAYING) {
            return MoveResult.NONE;
        }
        int[][] before = copy(cells);
        long rngBefore = rng.getState();
        long scoreBefore = score;
        int movesBefore = moveCount;

        int[][] out = new int[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (cells[r][c] == WALL) {
                    out[r][c] = WALL;
                }
            }
        }
        List<TileMove> moves = new ArrayList<>();
        List<int[]> merges = new ArrayList<>();
        long gained = 0;
        boolean changed = false;

        for (int line = 0; line < size; line++) {
            // Positions of this line ordered from the edge the tiles slide toward.
            int[][] pos = new int[size][2];
            for (int k = 0; k < size; k++) {
                int r, c;
                if (dir.dr != 0) {
                    c = line;
                    r = dir.dr < 0 ? k : size - 1 - k;
                } else {
                    r = line;
                    c = dir.dc < 0 ? k : size - 1 - k;
                }
                pos[k][0] = r;
                pos[k][1] = c;
            }
            int dest = 0;
            int prev = -1;
            boolean prevMerged = false;
            for (int k = 0; k < size; k++) {
                int r = pos[k][0], c = pos[k][1];
                int v = cells[r][c];
                if (v == WALL) {
                    dest = k + 1;
                    prev = -1;
                    prevMerged = false;
                    continue;
                }
                if (v == 0) {
                    continue;
                }
                if (prev >= 0 && !prevMerged && out[pos[prev][0]][pos[prev][1]] == v) {
                    int nv = v * 2;
                    out[pos[prev][0]][pos[prev][1]] = nv;
                    prevMerged = true;
                    gained += nv;
                    merges.add(new int[]{pos[prev][0], pos[prev][1], nv});
                    moves.add(new TileMove(r, c, pos[prev][0], pos[prev][1], v, true));
                    changed = true;
                } else {
                    out[pos[dest][0]][pos[dest][1]] = v;
                    if (dest != k) {
                        changed = true;
                    }
                    moves.add(new TileMove(r, c, pos[dest][0], pos[dest][1], v, false));
                    prev = dest;
                    prevMerged = false;
                    dest++;
                }
            }
        }
        if (!changed) {
            return MoveResult.NONE;
        }

        history.addLast(new Snapshot(before, scoreBefore, movesBefore, rngBefore));
        int cap = undosLeft < 0 ? MAX_HISTORY : Math.min(MAX_HISTORY, Math.max(1, undosLeft));
        while (history.size() > cap) {
            history.removeFirst();
        }
        for (int r = 0; r < size; r++) {
            System.arraycopy(out[r], 0, cells[r], 0, size);
        }
        score += gained;
        moveCount++;
        int[] spawned = spawn();

        if (!targetReached && maxTile() >= getTarget()) {
            targetReached = true;
            justWon = true;
            if (mode.endsOnWin()) {
                status = WON;
            }
        }
        if (status == PLAYING) {
            if (mode.moveLimit > 0 && moveCount >= mode.moveLimit) {
                status = LOST;
                reason = REASON_MOVES;
            } else if (!movesAvailable()) {
                status = LOST;
                reason = REASON_NO_MOVES;
            }
        }
        return new MoveResult(true, moves, merges, spawned == null ? -1 : spawned[0],
                spawned == null ? -1 : spawned[1], (int) gained);
    }

    /** Takes one step back (score, moves and the random state included). Returns false if not possible. */
    public boolean undo() {
        if (!canUndo()) {
            return false;
        }
        Snapshot s = history.removeLast();
        for (int r = 0; r < size; r++) {
            System.arraycopy(s.cells[r], 0, cells[r], 0, size);
        }
        score = s.score;
        moveCount = s.moves;
        rng.setState(s.rngState);
        if (undosLeft > 0) {
            undosLeft--;
        }
        status = PLAYING;
        reason = REASON_NONE;
        return true;
    }

    public boolean movesAvailable() {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int v = cells[r][c];
                if (v == 0) {
                    return true;
                }
                if (v > 0) {
                    if (r + 1 < size && cells[r + 1][c] == v) {
                        return true;
                    }
                    if (c + 1 < size && cells[r][c + 1] == v) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private int[] spawn() {
        List<int[]> empty = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (cells[r][c] == 0) {
                    empty.add(new int[]{r, c});
                }
            }
        }
        if (empty.isEmpty()) {
            return null;
        }
        int[] cell = empty.get(rng.nextInt(empty.size()));
        cells[cell[0]][cell[1]] = rng.nextDouble() < 0.9 ? 2 : 4;
        return cell;
    }

    private static int[][] copy(int[][] src) {
        int[][] dst = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            dst[i] = src[i].clone();
        }
        return dst;
    }

    // ---- save / restore ----

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("v1|").append(mode.key).append('|').append(size).append('|').append(seed).append('|')
                .append(score).append('|').append(moveCount).append('|').append(elapsedMs).append('|')
                .append(undosLeft).append('|').append(rng.getState()).append('|').append(status).append('|')
                .append(reason).append('|').append(targetReached ? 1 : 0).append('|');
        appendCells(sb, cells);
        sb.append('|').append(history.size());
        for (Snapshot s : history) {
            sb.append('|');
            appendCells(sb, s.cells);
            sb.append(';').append(s.score).append(';').append(s.moves).append(';').append(s.rngState);
        }
        return sb.toString();
    }

    private static void appendCells(StringBuilder sb, int[][] g) {
        boolean first = true;
        for (int[] row : g) {
            for (int v : row) {
                if (!first) {
                    sb.append(',');
                }
                sb.append(v);
                first = false;
            }
        }
    }

    private static int[][] parseCells(String s, int size) {
        String[] parts = s.split(",");
        if (parts.length != size * size) {
            throw new IllegalArgumentException("cells");
        }
        int[][] g = new int[size][size];
        for (int i = 0; i < parts.length; i++) {
            int v = Integer.parseInt(parts[i]);
            if (v < WALL || (v > 0 && (v & (v - 1)) != 0)) {
                throw new IllegalArgumentException("value");
            }
            g[i / size][i % size] = v;
        }
        return g;
    }

    /** Restores a game written by {@link #serialize()}; returns null if the text is damaged. */
    public static GameEngine deserialize(String text) {
        if (text == null) {
            return null;
        }
        try {
            String[] p = text.split("\\|");
            if (p.length < 13 || !"v1".equals(p[0])) {
                return null;
            }
            Mode mode = Mode.fromKey(p[1]);
            if (!mode.key.equals(p[1])) {
                return null;
            }
            int size = Integer.parseInt(p[2]);
            if (size < Mode.MIN_SIZE || size > Mode.MAX_SIZE || mode.sizeOrDefault(size) != size) {
                return null;
            }
            GameEngine g = new GameEngine(mode, size, Long.parseLong(p[3]));
            g.score = Long.parseLong(p[4]);
            g.moveCount = Integer.parseInt(p[5]);
            g.elapsedMs = Long.parseLong(p[6]);
            g.undosLeft = Integer.parseInt(p[7]);
            g.rng.setState(Long.parseLong(p[8]));
            g.status = Integer.parseInt(p[9]);
            g.reason = Integer.parseInt(p[10]);
            g.targetReached = "1".equals(p[11]);
            int[][] board = parseCells(p[12], size);
            for (int r = 0; r < size; r++) {
                System.arraycopy(board[r], 0, g.cells[r], 0, size);
            }
            g.history.clear();
            int n = p.length > 13 ? Integer.parseInt(p[13]) : 0;
            for (int i = 0; i < n; i++) {
                String[] sp = p[14 + i].split(";");
                g.history.addLast(new Snapshot(parseCells(sp[0], size), Long.parseLong(sp[1]),
                        Integer.parseInt(sp[2]), Long.parseLong(sp[3])));
            }
            return g;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Debug/test helper: replaces the board (rows of values, -1 = wall) and re-evaluates nothing else. */
    public void setBoardForTest(int[][] board) {
        for (int r = 0; r < size; r++) {
            System.arraycopy(board[r], 0, cells[r], 0, size);
        }
    }

    /** Iterates history snapshots count (tests). */
    public int historySize() {
        int n = 0;
        for (Iterator<Snapshot> it = history.iterator(); it.hasNext(); it.next()) {
            n++;
        }
        return n;
    }
}
