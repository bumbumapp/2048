package bumbumapps.a2048game.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/** All statistics, keyed by {@link Mode#storeKey(int)}, plus the Daily Challenge streak. */
public final class StatsBook {
    private final Map<String, ModeStat> stats = new LinkedHashMap<>();
    public long lastDailyDay = Long.MIN_VALUE;
    public int dailyStreak;
    public int bestDailyStreak;

    public ModeStat get(String key) {
        ModeStat m = stats.get(key);
        if (m == null) {
            m = new ModeStat();
            stats.put(key, m);
        }
        return m;
    }

    public Map<String, ModeStat> all() {
        return stats;
    }

    /** Raises the best score / best tile live while playing (no game count change). */
    public void noteProgress(String key, long score, int maxTile) {
        ModeStat m = get(key);
        m.bestScore = Math.max(m.bestScore, score);
        m.bestTile = Math.max(m.bestTile, maxTile);
    }

    /** A game was started (its first move was made). */
    public void recordStart(String key) {
        get(key).games++;
    }

    /** A started game ended or was abandoned: adds its score to the average. */
    public void recordFinish(String key, long score, int maxTile) {
        ModeStat m = get(key);
        m.finished++;
        m.totalScore += score;
        noteProgress(key, score, maxTile);
    }

    /** Start and finish in one call. */
    public void recordGame(String key, long score, int maxTile) {
        recordStart(key);
        recordFinish(key, score, maxTile);
    }

    public void recordWin(String key) {
        get(key).wins++;
    }

    /** Daily Challenge finished on {@code epochDay}: extends or restarts the streak (once per day). */
    public void recordDailyDone(long epochDay) {
        if (epochDay == lastDailyDay) {
            return;
        }
        dailyStreak = (lastDailyDay != Long.MIN_VALUE && epochDay == lastDailyDay + 1) ? dailyStreak + 1 : 1;
        lastDailyDay = epochDay;
        bestDailyStreak = Math.max(bestDailyStreak, dailyStreak);
    }

    /** Streak shown to the player: broken if yesterday was missed. */
    public int currentStreak(long today) {
        if (lastDailyDay == Long.MIN_VALUE) {
            return 0;
        }
        return (today - lastDailyDay <= 1) ? dailyStreak : 0;
    }

    // ---- aggregates ----

    public int totalGames() {
        int n = 0;
        for (ModeStat m : stats.values()) {
            n += m.games;
        }
        return n;
    }

    public int bestTile() {
        int n = 0;
        for (ModeStat m : stats.values()) {
            n = Math.max(n, m.bestTile);
        }
        return n;
    }

    public long bestScore() {
        long n = 0;
        for (ModeStat m : stats.values()) {
            n = Math.max(n, m.bestScore);
        }
        return n;
    }

    /** Sum of stats whose key starts with the mode key (classic_3 ... classic_8 all count as Classic). */
    public ModeStat forMode(Mode mode) {
        ModeStat sum = new ModeStat();
        for (Map.Entry<String, ModeStat> e : stats.entrySet()) {
            String k = e.getKey();
            if (k.equals(mode.key) || k.startsWith(mode.key + "_")) {
                ModeStat m = e.getValue();
                sum.games += m.games;
                sum.finished += m.finished;
                sum.wins += m.wins;
                sum.totalScore += m.totalScore;
                sum.bestScore = Math.max(sum.bestScore, m.bestScore);
                sum.bestTile = Math.max(sum.bestTile, m.bestTile);
            }
        }
        return sum;
    }

    public int modesPlayed() {
        int n = 0;
        for (Mode mode : Mode.values()) {
            if (forMode(mode).games > 0) {
                n++;
            }
        }
        return n;
    }

    // ---- text form ----

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("daily=").append(lastDailyDay).append(',').append(dailyStreak).append(',').append(bestDailyStreak);
        for (Map.Entry<String, ModeStat> e : stats.entrySet()) {
            sb.append('\n').append(e.getKey()).append('=').append(e.getValue().serialize());
        }
        return sb.toString();
    }

    public static StatsBook parse(String text) {
        StatsBook b = new StatsBook();
        if (text == null) {
            return b;
        }
        for (String line : text.split("\n")) {
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq);
            String val = line.substring(eq + 1);
            try {
                if (key.equals("daily")) {
                    String[] p = val.split(",");
                    b.lastDailyDay = Long.parseLong(p[0]);
                    b.dailyStreak = Integer.parseInt(p[1]);
                    b.bestDailyStreak = Integer.parseInt(p[2]);
                } else {
                    b.stats.put(key, ModeStat.parse(val));
                }
            } catch (RuntimeException ignored) {
                // skip a damaged line
            }
        }
        return b;
    }
}
