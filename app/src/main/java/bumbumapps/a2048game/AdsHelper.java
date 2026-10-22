package bumbumapps.a2048game;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Central AdMob handling: UMP consent, SDK init only after consent, anchored adaptive banners and
 * a frequency-capped, preloaded interstitial.
 */
public final class AdsHelper {

    /** Minimum time between two interstitials (and between app start and the first one). */
    public static final long INTERSTITIAL_MIN_INTERVAL_MS = 60_000L;

    private static final AtomicBoolean mobileAdsStarted = new AtomicBoolean(false);
    private static InterstitialAd interstitialAd;
    private static boolean interstitialLoading;
    private static long lastInterstitialAt = -1L;

    public interface OnAdsReady {
        void onAdsReady();
    }

    private AdsHelper() {
    }

    /**
     * Requests a consent update, shows the consent form if required and calls {@code onReady}
     * at most once, and only when ads may be requested.
     */
    public static void gatherConsent(@NonNull Activity activity, @NonNull OnAdsReady onReady) {
        if (lastInterstitialAt < 0) {
            lastInterstitialAt = SystemClock.elapsedRealtime();
        }
        final AtomicBoolean delivered = new AtomicBoolean(false);
        final ConsentInformation info = UserMessagingPlatform.getConsentInformation(activity);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        info.requestConsentInfoUpdate(activity, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity,
                        formError -> startAdsIfAllowed(activity, delivered, onReady)),
                requestError -> startAdsIfAllowed(activity, delivered, onReady));
        // Consent from a previous session lets ads start without waiting for the network.
        startAdsIfAllowed(activity, delivered, onReady);
    }

    private static void startAdsIfAllowed(Activity activity, AtomicBoolean delivered, OnAdsReady onReady) {
        if (activity.isFinishing() || !canRequestAds(activity)) {
            return;
        }
        if (mobileAdsStarted.compareAndSet(false, true)) {
            final Context app = activity.getApplicationContext();
            new Thread(() -> MobileAds.initialize(app, status -> { })).start();
        }
        if (delivered.compareAndSet(false, true)) {
            onReady.onAdsReady();
        }
    }

    public static boolean canRequestAds(@NonNull Context context) {
        return UserMessagingPlatform.getConsentInformation(context).canRequestAds();
    }

    public static boolean isPrivacyOptionsRequired(@NonNull Context context) {
        return UserMessagingPlatform.getConsentInformation(context).getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public static void showPrivacyOptions(@NonNull Activity activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> { });
    }

    /** Adds an anchored adaptive banner to {@code container}; returns null if ads are not allowed. */
    @Nullable
    public static AdView loadBanner(@NonNull Activity activity, @NonNull ViewGroup container, @NonNull String adUnitId) {
        if (!canRequestAds(activity)) {
            return null;
        }
        AdView adView = new AdView(activity);
        adView.setAdUnitId(adUnitId);
        adView.setAdSize(anchoredAdaptiveSize(activity));
        container.removeAllViews();
        container.addView(adView);
        adView.loadAd(new AdRequest.Builder().build());
        return adView;
    }

    private static AdSize anchoredAdaptiveSize(Activity activity) {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthDp = (int) (metrics.widthPixels / metrics.density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp);
    }

    /** Loads one interstitial in the background if none is cached or loading. */
    public static void preloadInterstitial(@NonNull Context context, @NonNull String adUnitId) {
        if (interstitialAd != null || interstitialLoading || !canRequestAds(context)) {
            return;
        }
        interstitialLoading = true;
        InterstitialAd.load(context.getApplicationContext(), adUnitId, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        interstitialLoading = false;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        interstitialAd = null;
                        interstitialLoading = false;
                    }
                });
    }

    /**
     * Shows the cached interstitial at a natural break point if the frequency cap allows it.
     * Returns true if an ad is being shown.
     */
    public static boolean showInterstitialIfReady(@NonNull Activity activity, @NonNull String adUnitId) {
        long now = SystemClock.elapsedRealtime();
        if (interstitialAd == null) {
            preloadInterstitial(activity, adUnitId);
            return false;
        }
        if (!isIntervalElapsed(lastInterstitialAt, now, INTERSTITIAL_MIN_INTERVAL_MS) || activity.isFinishing()) {
            return false;
        }
        final Context app = activity.getApplicationContext();
        InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                preloadInterstitial(app, adUnitId);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                preloadInterstitial(app, adUnitId);
            }
        });
        lastInterstitialAt = now;
        ad.show(activity);
        return true;
    }

    /** Frequency cap: true when at least {@code minIntervalMs} passed since {@code lastAt} (negative = never). */
    static boolean isIntervalElapsed(long lastAt, long now, long minIntervalMs) {
        return lastAt < 0 || now - lastAt >= minIntervalMs;
    }
}
