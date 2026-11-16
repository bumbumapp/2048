package bumbumapps.a2048game.engine;

import java.util.ArrayList;
import java.util.List;

/** The achievement list; progress is derived from {@link StatsBook}, so it can never be lost or double counted. */
public final class Achievements {
    public static final class Item {
        public final String id;
        public final long current;
        public final long target;

        Item(String id, long current, long target) {
            this.id = id;
            this.current = Math.min(current, target);
            this.target = target;
        }

        public boolean unlocked() {
            return current >= target;
        }

        public int percent() {
            return (int) (current * 100 / target);
        }
    }

    private Achievements() {
    }

    public static List<Item> compute(StatsBook s) {
        List<Item> l = new ArrayList<>();
        int best = s.bestTile();
        l.add(new Item("tile128", best, 128));
        l.add(new Item("tile512", best, 512));
        l.add(new Item("tile1024", best, 1024));
        l.add(new Item("tile2048", best, 2048));
        l.add(new Item("tile4096", best, 4096));
        l.add(new Item("games10", s.totalGames(), 10));
        l.add(new Item("games50", s.totalGames(), 50));
        l.add(new Item("score10000", s.bestScore(), 10000));
        l.add(new Item("timewin", s.forMode(Mode.TIME).wins, 1));
        l.add(new Item("moveswin", s.forMode(Mode.MOVES).wins, 1));
        l.add(new Item("streak3", s.bestDailyStreak, 3));
        l.add(new Item("allmodes", s.modesPlayed(), Mode.values().length));
        return l;
    }

    public static int unlockedCount(List<Item> items) {
        int n = 0;
        for (Item i : items) {
            if (i.unlocked()) {
                n++;
            }
        }
        return n;
    }
}
