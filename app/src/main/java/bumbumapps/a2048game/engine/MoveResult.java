package bumbumapps.a2048game.engine;

import java.util.ArrayList;
import java.util.List;

/** What a move did, for animation and feedback. {@code moved == false} means the board is unchanged. */
public final class MoveResult {
    public static final MoveResult NONE = new MoveResult(false, new ArrayList<TileMove>(), new ArrayList<int[]>(), -1, -1, 0);

    public final boolean moved;
    public final List<TileMove> moves;
    /** Cells {r, c, newValue} where two tiles merged. */
    public final List<int[]> merges;
    public final int spawnR, spawnC;
    public final int gained;

    public MoveResult(boolean moved, List<TileMove> moves, List<int[]> merges, int spawnR, int spawnC, int gained) {
        this.moved = moved;
        this.moves = moves;
        this.merges = merges;
        this.spawnR = spawnR;
        this.spawnC = spawnC;
        this.gained = gained;
    }
}
