package bumbumapps.a2048game;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TapOnBoardTest {
    // Board like on a 1080x2400 phone: x 30..1050, y 840..1840.
    @Test
    public void tapInTheMiddleOfTheBoardContinues() {
        assertTrue(InputListener.tapOnBoard(540f, 1450f, 30f, 1050f, 840f, 1840f));
    }

    @Test
    public void tapBelowTheBoardDoesNot() {
        assertFalse(InputListener.tapOnBoard(540f, 2100f, 30f, 1050f, 840f, 1840f));
    }

    @Test
    public void xIsNotComparedWithTheVerticalRange() {
        // Regression: x=540 is outside 840..1840 but the tap is on the board, so it must count.
        assertTrue(InputListener.tapOnBoard(540f, 1000f, 30f, 1050f, 840f, 1840f));
    }
}
