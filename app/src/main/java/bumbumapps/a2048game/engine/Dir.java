package bumbumapps.a2048game.engine;

public enum Dir {
    UP(-1, 0), RIGHT(0, 1), DOWN(1, 0), LEFT(0, -1);

    public final int dr;
    public final int dc;

    Dir(int dr, int dc) {
        this.dr = dr;
        this.dc = dc;
    }
}
