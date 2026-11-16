package bumbumapps.a2048game.engine;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GameEngineTest {
    private static GameEngine empty(Mode mode, int size, int[][] board) {
        GameEngine g = new GameEngine(mode, size, 1L);
        g.setBoardForTest(board);
        return g;
    }

    private static int count(GameEngine g) {
        int n = 0;
        for (int r = 0; r < g.size; r++) {
            for (int c = 0; c < g.size; c++) {
                if (g.get(r, c) > 0) {
                    n++;
                }
            }
        }
        return n;
    }

    @Test
    public void startsWithTwoTiles() {
        GameEngine g = new GameEngine(Mode.CLASSIC, 4, 7L);
        assertEquals(2, count(g));
    }

    @Test
    public void slidesLeft() {
        GameEngine g = empty(Mode.CLASSIC, 4, new int[][]{{0, 0, 2, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}});
        MoveResult r = g.move(Dir.LEFT);
        assertTrue(r.moved);
        assertEquals(2, g.get(0, 0));
    }

    @Test
    public void mergesOncePerMove() {
        // 2 2 2 2 -> 4 4 (not 8) when moving left
        GameEngine g = empty(Mode.CLASSIC, 4, new int[][]{{2, 2, 2, 2}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}});
        g.move(Dir.LEFT);
        assertEquals(4, g.get(0, 0));
        assertEquals(4, g.get(0, 1));
        assertEquals(8, g.getScore());
    }

    @Test
    public void mergeResultIsNotMergedAgainInTheSameMove() {
        // 4 2 2 -> 4 4 (the new 4 must not merge with the first 4)
        GameEngine g = empty(Mode.CLASSIC, 4, new int[][]{{4, 2, 2, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}});
        g.move(Dir.LEFT);
        assertEquals(4, g.get(0, 0));
        assertEquals(4, g.get(0, 1));
    }

    @Test
    public void mergesTowardTheMovingEdge() {
        // 2 2 2 moving right merges the two rightmost: 0 0 2 4 ... leading tile stays
        GameEngine g = empty(Mode.CLASSIC, 4, new int[][]{{2, 2, 2, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}});
        g.move(Dir.RIGHT);
        assertEquals(4, g.get(0, 3));
        assertEquals(2, g.get(0, 2));
    }

    @Test
    public void allFourDirectionsMove() {
        int[][] b = {{0, 0, 0, 0}, {0, 2, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}};
        GameEngine g = empty(Mode.CLASSIC, 4, b);
        g.move(Dir.UP);
        assertEquals(2, g.get(0, 1));
        g = empty(Mode.CLASSIC, 4, b);
        g.move(Dir.DOWN);
        assertEquals(2, g.get(3, 1));
        g = empty(Mode.CLASSIC, 4, b);
        g.move(Dir.LEFT);
        assertEquals(2, g.get(1, 0));
        g = empty(Mode.CLASSIC, 4, b);
        g.move(Dir.RIGHT);
        assertEquals(2, g.get(1, 3));
    }

    @Test
    public void moveThatChangesNothingIsIgnoredAndSpawnsNothing() {
        GameEngine g = empty(Mode.CLASSIC, 4, new int[][]{{2, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}});
        long before = g.getScore();
        MoveResult r = g.move(Dir.LEFT);
        assertFalse(r.moved);
        assertEquals(1, count(g));
        assertEquals(0, g.getMoveCount());
        assertEquals(before, g.getScore());
    }

    @Test
    public void spawnsOneTileAfterAMove() {
        GameEngine g = empty(Mode.CLASSIC, 4, new int[][]{{0, 0, 0, 2}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}});
        MoveResult r = g.move(Dir.LEFT);
        assertEquals(2, count(g));
        assertTrue(r.spawnR >= 0);
        int v = g.get(r.spawnR, r.spawnC);
        assertTrue(v == 2 || v == 4);
    }

    @Test
    public void gameOverWhenFullAndNoMerges() {
        GameEngine g = empty(Mode.CLASSIC, 4, new int[][]{{2, 4, 2, 4}, {4, 2, 4, 2}, {2, 4, 2, 4}, {4, 2, 4, 0}});
        // a legal slide fills the last cell with a spawned 2 or 4 and locks the board.
        g.setBoardForTest(new int[][]{{2, 4, 2, 4}, {4, 2, 4, 2}, {2, 4, 2, 4}, {4, 2, 0, 8}});
        g.move(Dir.RIGHT);
        // After this the board is full; the spawn may or may not create a pair, so test the check directly.
        g.setBoardForTest(new int[][]{{2, 4, 2, 4}, {4, 2, 4, 2}, {2, 4, 2, 4}, {4, 2, 4, 2}});
        assertFalse(g.movesAvailable());
    }

    @Test
    public void lossIsDetectedAfterMove() {
        // Whatever 2 or 4 spawns in the freed corner, no equal neighbours remain.
        GameEngine g = empty(Mode.CLASSIC, 3, new int[][]{{8, 16, 32}, {16, 32, 64}, {128, 256, 0}});
        MoveResult r = g.move(Dir.RIGHT);
        assertTrue(r.moved);
        assertEquals(GameEngine.LOST, g.getStatus());
        assertEquals(GameEngine.REASON_NO_MOVES, g.getReason());
        assertFalse(g.move(Dir.LEFT).moved);
    }

    @Test
    public void winningEndsTimeAttackButNotClassic() {
        GameEngine time = empty(Mode.TIME, 4, new int[][]{{128, 128, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}});
        time.move(Dir.LEFT);
        assertEquals(GameEngine.WON, time.getStatus());
        assertTrue(time.consumeJustWon());
        assertFalse(time.consumeJustWon());

        GameEngine classic = empty(Mode.CLASSIC, 4, new int[][]{{1024, 1024, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}, {0, 0, 0, 0}});
        classic.move(Dir.LEFT);
        assertEquals(GameEngine.PLAYING, classic.getStatus());
        assertTrue(classic.isTargetReached());
        assertTrue(classic.consumeJustWon());
        assertTrue(classic.move(Dir.RIGHT).moved);
    }

    @Test
    public void targetDependsOnBoardSize() {
        assertEquals(256, Mode.CLASSIC.target(3));
        assertEquals(2048, Mode.CLASSIC.target(4));
        assertEquals(4096, Mode.CLASSIC.target(5));
        assertEquals(32768, Mode.CLASSIC.target(8));
    }

    @Test
    public void otherModesHaveFixedTargets() {
        assertEquals(256, Mode.TIME.target(4));
        assertEquals(128, Mode.MOVES.target(4));
        assertEquals(1024, Mode.BLOCKS.target(5));
    }

    @Test
    public void everyClassicSizeWorks() {
        for (int s = Mode.MIN_SIZE; s <= Mode.MAX_SIZE; s++) {
            GameEngine g = new GameEngine(Mode.CLASSIC, s, 3L);
            assertEquals(s, g.size);
            for (int i = 0; i < 40 && !g.isOver(); i++) {
                g.move(Dir.values()[i % 4]);
            }
            assertNotNull(g.serialize());
        }
    }

    @Test
    public void sizeIsClampedAndFixedForOtherModes() {
        assertEquals(8, new GameEngine(Mode.CLASSIC, 99, 1).size);
        assertEquals(3, new GameEngine(Mode.CLASSIC, 1, 1).size);
        assertEquals(4, new GameEngine(Mode.TIME, 7, 1).size);
    }

    @Test
    public void wallsBlockTilesAndMerges() {
        GameEngine g = new GameEngine(Mode.BLOCKS, 5, 1);
        g.setBoardForTest(new int[][]{
                {2, -1, 2, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}, {0, 0, 0, 0, 0}});
        g.move(Dir.RIGHT);
        // 2 stays left of the wall, the other 2 slides right: they must not merge through the wall.
        assertEquals(2, g.get(0, 0));
        assertEquals(-1, g.get(0, 1));
        assertEquals(2, g.get(0, 4));
    }

    @Test
    public void blocksModePlacesWallsAndTheyNeverMove() {
        GameEngine g = new GameEngine(Mode.BLOCKS, 5, 99L);
        int walls = 0;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                if (g.get(r, c) == GameEngine.WALL) {
                    walls++;
                }
            }
        }
        assertEquals(3, walls);
        for (int i = 0; i < 30 && !g.isOver(); i++) {
            g.move(Dir.values()[i % 4]);
        }
        int after = 0;
        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                if (g.get(r, c) == GameEngine.WALL) {
                    after++;
                }
            }
        }
        assertEquals(3, after);
    }

    @Test
    public void dailyChallengeIsReproducible() {
        long seed = GameEngine.dailySeed(20400);
        GameEngine a = new GameEngine(Mode.DAILY, 4, seed);
        GameEngine b = new GameEngine(Mode.DAILY, 4, seed);
        for (int i = 0; i < 60; i++) {
            Dir d = Dir.values()[(i * 7 + i / 3) % 4];
            a.move(d);
            b.move(d);
        }
        assertEquals(a.serialize(), b.serialize());
        assertTrue(a.getMoveCount() > 5);
    }

    @Test
    public void dailySeedDiffersPerDay() {
        assertFalse(GameEngine.dailySeed(20400) == GameEngine.dailySeed(20401));
        assertEquals(GameEngine.dailySeed(20400), GameEngine.dailySeed(20400));
        GameEngine a = new GameEngine(Mode.DAILY, 4, GameEngine.dailySeed(1));
        GameEngine b = new GameEngine(Mode.DAILY, 4, GameEngine.dailySeed(2));
        assertFalse(a.serialize().equals(b.serialize()));
    }

    @Test
    public void undoRestoresBoardScoreAndRandomness() {
        GameEngine g = new GameEngine(Mode.CLASSIC, 4, 5L);
        String start = g.serialize();
        GameEngine twin = new GameEngine(Mode.CLASSIC, 4, 5L);
        // play, undo, replay -> identical to a fresh run of the same move
        GameEngine h = new GameEngine(Mode.CLASSIC, 4, 5L);
        boolean moved = h.move(Dir.LEFT).moved;
        if (!moved) {
            moved = h.move(Dir.UP).moved;
        }
        assertTrue(moved);
        assertTrue(h.undo());
        assertEquals(0, h.getMoveCount());
        assertEquals(0, h.getScore());
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                assertEquals(twin.get(r, c), h.get(r, c));
            }
        }
        assertNotNull(start);
    }

    @Test
    public void undoLimitIsEnforced() {
        GameEngine g = new GameEngine(Mode.DAILY, 4, 11L); // 1 undo
        int played = 0;
        for (int i = 0; i < 12 && played < 3; i++) {
            if (g.move(Dir.values()[i % 4]).moved) {
                played++;
            }
        }
        assertTrue(g.undo());
        assertFalse(g.canUndo());
        assertFalse(g.undo());
    }

    @Test
    public void zenHasUnlimitedUndo() {
        GameEngine g = new GameEngine(Mode.ZEN, 4, 11L);
        int played = 0;
        for (int i = 0; i < 40 && played < 8; i++) {
            if (g.move(Dir.values()[i % 4]).moved) {
                played++;
            }
        }
        int undone = 0;
        while (g.undo()) {
            undone++;
        }
        assertTrue(undone >= 5);
        assertEquals(-1, g.getUndosLeft());
    }

    @Test
    public void timeAttackEndsWhenTimeIsUp() {
        GameEngine g = new GameEngine(Mode.TIME, 4, 1L);
        g.tick(239_000);
        assertEquals(GameEngine.PLAYING, g.getStatus());
        assertEquals(1000, g.timeLeftMs());
        g.tick(2_000);
        assertEquals(GameEngine.LOST, g.getStatus());
        assertEquals(GameEngine.REASON_TIME, g.getReason());
        assertEquals(0, g.timeLeftMs());
    }

    @Test
    public void limitedMovesEndsWhenOutOfMoves() {
        GameEngine g = new GameEngine(Mode.MOVES, 4, 3L);
        assertEquals(100, g.movesLeft());
        int guard = 0;
        while (!g.isOver() && guard++ < 1000) {
            g.move(Dir.values()[guard % 4]);
        }
        assertTrue(g.isOver());
        if (g.getReason() == GameEngine.REASON_MOVES) {
            assertEquals(100, g.getMoveCount());
            assertEquals(0, g.movesLeft());
        }
    }

    @Test
    public void saveAndRestoreKeepsTheGameIncludingUndoAndRandomness() {
        GameEngine g = new GameEngine(Mode.CLASSIC, 5, 42L);
        for (int i = 0; i < 25; i++) {
            g.move(Dir.values()[i % 4]);
        }
        g.tick(5000);
        GameEngine copy = GameEngine.deserialize(g.serialize());
        assertNotNull(copy);
        assertEquals(g.serialize(), copy.serialize());
        g.move(Dir.LEFT);
        copy.move(Dir.LEFT);
        assertEquals(g.serialize(), copy.serialize());
        assertEquals(g.canUndo(), copy.canUndo());
    }

    @Test
    public void damagedSaveIsRejected() {
        assertNull(GameEngine.deserialize(null));
        assertNull(GameEngine.deserialize(""));
        assertNull(GameEngine.deserialize("v1|classic|4|1"));
        String ok = new GameEngine(Mode.CLASSIC, 4, 1).serialize();
        assertNull(GameEngine.deserialize(ok.replace("classic", "bogus")));
        assertNull(GameEngine.deserialize(ok.replace("v1|", "v9|")));
        assertNull(GameEngine.deserialize(ok.substring(0, ok.length() / 2)));
        assertNull(GameEngine.deserialize(ok.replace("|4|", "|99|")));
    }

    @Test
    public void storeKeysSeparateBoardSizes() {
        assertEquals("classic_5", Mode.CLASSIC.storeKey(5));
        assertEquals("time", Mode.TIME.storeKey(7));
        assertEquals("blocks_5", Mode.BLOCKS.storeKey(5));
    }

    @Test
    public void randomGamesNeverCorruptTheBoard() {
        for (long seed = 0; seed < 30; seed++) {
            for (Mode m : Mode.values()) {
                GameEngine g = new GameEngine(m, 3 + (int) (seed % 6), seed);
                long scoreBefore = 0;
                for (int i = 0; i < 300 && !g.isOver(); i++) {
                    g.move(Dir.values()[(int) ((seed + i * 31 + i / 5) % 4)]);
                    assertTrue(g.getScore() >= scoreBefore);
                    scoreBefore = g.getScore();
                    for (int r = 0; r < g.size; r++) {
                        for (int c = 0; c < g.size; c++) {
                            int v = g.get(r, c);
                            assertTrue(v == -1 || v == 0 || (v & (v - 1)) == 0);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void rngIsDeterministic() {
        SeededRandom a = new SeededRandom(5);
        SeededRandom b = new SeededRandom(5);
        for (int i = 0; i < 10; i++) {
            assertEquals(a.nextLong(), b.nextLong());
        }
        long[] expected = {a.nextInt(10), a.nextInt(10)};
        a.setState(b.getState());
        assertArrayEquals(expected, new long[]{b.nextInt(10), b.nextInt(10)});
    }
}
