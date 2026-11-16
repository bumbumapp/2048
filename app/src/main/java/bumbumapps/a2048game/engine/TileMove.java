package bumbumapps.a2048game.engine;

/** One tile sliding during a move; {@code merged} means it ends on a tile of equal value and both become one. */
public final class TileMove {
    public final int fromR, fromC, toR, toC, value;
    public final boolean merged;

    public TileMove(int fromR, int fromC, int toR, int toC, int value, boolean merged) {
        this.fromR = fromR;
        this.fromC = fromC;
        this.toR = toR;
        this.toC = toC;
        this.value = value;
        this.merged = merged;
    }
}
