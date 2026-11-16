package bumbumapps.a2048game.engine;

/** Lifetime numbers for one mode (and board size). */
public final class ModeStat {
    /** Games started (first move made). */
    public int games;
    /** Games that were finished or abandoned (their score is in totalScore). */
    public int finished;
    public int wins;
    public long bestScore;
    public int bestTile;
    public long totalScore;

    public long averageScore() {
        return finished == 0 ? 0 : totalScore / finished;
    }

    public String serialize() {
        return games + "," + wins + "," + bestScore + "," + bestTile + "," + totalScore + "," + finished;
    }

    public static ModeStat parse(String s) {
        ModeStat m = new ModeStat();
        if (s == null) {
            return m;
        }
        try {
            String[] p = s.split(",");
            m.games = Integer.parseInt(p[0]);
            m.wins = Integer.parseInt(p[1]);
            m.bestScore = Long.parseLong(p[2]);
            m.bestTile = Integer.parseInt(p[3]);
            m.totalScore = Long.parseLong(p[4]);
            m.finished = p.length > 5 ? Integer.parseInt(p[5]) : m.games;
        } catch (RuntimeException e) {
            return new ModeStat();
        }
        return m;
    }
}
