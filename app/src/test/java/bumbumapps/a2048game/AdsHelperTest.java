package bumbumapps.a2048game;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AdsHelperTest {
    @Test
    public void interstitialCapBlocksWithinInterval() {
        assertFalse(AdsHelper.isIntervalElapsed(1_000L, 1_000L + 59 * 60_000L, AdsHelper.INTERSTITIAL_MIN_INTERVAL_MS));
    }

    @Test
    public void interstitialCapAllowsAfterInterval() {
        assertTrue(AdsHelper.isIntervalElapsed(1_000L, 1_000L + 60 * 60_000L, AdsHelper.INTERSTITIAL_MIN_INTERVAL_MS));
    }

    @Test
    public void interstitialCapAllowsWhenNeverShown() {
        assertTrue(AdsHelper.isIntervalElapsed(-1L, 0L, AdsHelper.INTERSTITIAL_MIN_INTERVAL_MS));
    }
}
