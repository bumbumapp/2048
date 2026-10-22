package bumbumapps.a2048game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class RestoreTileTest {
    @Test
    public void positiveValueCreatesTile() {
        Tile t = MainActivity.restoreTile(1, 2, 8, null);
        assertEquals(8, t.getValue());
    }

    @Test
    public void zeroClearsCell() {
        // Regression: undo grid used the main grid's value to decide whether to clear.
        assertNull(MainActivity.restoreTile(0, 0, 0, new Tile(0, 0, 4)));
    }

    @Test
    public void missingValueKeepsCurrent() {
        Tile current = new Tile(0, 0, 2);
        assertSame(current, MainActivity.restoreTile(0, 0, -1, current));
    }
}
