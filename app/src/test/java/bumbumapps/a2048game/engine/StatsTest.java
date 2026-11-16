package bumbumapps.a2048game.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class StatsTest {
    @Test
    public void recordsGamesBestsAndAverage() {
        StatsBook b = new StatsBook();
        b.recordGame("classic_4", 1000, 128);
        b.recordGame("classic_4", 3000, 256);
        b.recordWin("classic_4");
        ModeStat m = b.get("classic_4");
        assertEquals(2, m.games);
        assertEquals(1, m.wins);
        assertEquals(3000, m.bestScore);
        assertEquals(256, m.bestTile);
        assertEquals(2000, m.averageScore());
    }

    @Test
    public void unfinishedGamesCountButDoNotChangeTheAverage() {
        StatsBook b = new StatsBook();
        b.recordGame("zen", 1000, 64);
        b.recordStart("zen");
        ModeStat m = b.get("zen");
        assertEquals(2, m.games);
        assertEquals(1, m.finished);
        assertEquals(1000, m.averageScore());
        assertEquals(2, StatsBook.parse(b.serialize()).get("zen").games);
        assertEquals(1, StatsBook.parse(b.serialize()).get("zen").finished);
    }

    @Test
    public void classicSizesAreSummedForTheMode() {
        StatsBook b = new StatsBook();
        b.recordGame("classic_3", 100, 32);
        b.recordGame("classic_6", 900, 512);
        b.recordGame("time", 50, 16);
        ModeStat c = b.forMode(Mode.CLASSIC);
        assertEquals(2, c.games);
        assertEquals(900, c.bestScore);
        assertEquals(512, c.bestTile);
        assertEquals(2, b.modesPlayed());
        assertEquals(3, b.totalGames());
    }

    @Test
    public void dailyStreakExtendsOnConsecutiveDaysAndResets() {
        StatsBook b = new StatsBook();
        b.recordDailyDone(100);
        b.recordDailyDone(100); // same day does not count twice
        assertEquals(1, b.dailyStreak);
        b.recordDailyDone(101);
        b.recordDailyDone(102);
        assertEquals(3, b.dailyStreak);
        assertEquals(3, b.currentStreak(102));
        assertEquals(3, b.currentStreak(103));
        assertEquals(0, b.currentStreak(105));
        b.recordDailyDone(110);
        assertEquals(1, b.dailyStreak);
        assertEquals(3, b.bestDailyStreak);
    }

    @Test
    public void serializeRoundTrip() {
        StatsBook b = new StatsBook();
        b.recordGame("classic_4", 1234, 256);
        b.recordWin("classic_4");
        b.recordDailyDone(50);
        StatsBook c = StatsBook.parse(b.serialize());
        assertEquals(b.serialize(), c.serialize());
        assertEquals(1, c.dailyStreak);
        assertEquals(1234, c.get("classic_4").bestScore);
    }

    @Test
    public void parseToleratesGarbage() {
        StatsBook b = StatsBook.parse("junk\nclassic_4=a,b\nzen=1,0,10,4,10\n=\n");
        assertEquals(1, b.get("zen").games);
        assertEquals(0, b.get("classic_4").games);
        assertEquals(0, StatsBook.parse(null).totalGames());
    }

    @Test
    public void achievementsFollowStats() {
        StatsBook b = new StatsBook();
        List<Achievements.Item> items = Achievements.compute(b);
        assertEquals(0, Achievements.unlockedCount(items));
        b.recordGame("classic_4", 12000, 1024);
        items = Achievements.compute(b);
        assertTrue(find(items, "tile1024").unlocked());
        assertTrue(find(items, "tile128").unlocked());
        assertFalse(find(items, "tile2048").unlocked());
        assertEquals(50, find(items, "tile2048").percent());
        assertTrue(find(items, "score10000").unlocked());
        b.recordWin("time");
        assertEquals(12, items.size());
        assertTrue(find(Achievements.compute(b), "timewin").unlocked());
    }

    private static Achievements.Item find(List<Achievements.Item> l, String id) {
        for (Achievements.Item i : l) {
            if (i.id.equals(id)) {
                return i;
            }
        }
        throw new AssertionError(id);
    }
}
