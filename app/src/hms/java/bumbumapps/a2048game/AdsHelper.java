package bumbumapps.a2048game;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.ads.InterstitialAd;
import com.huawei.hms.ads.NonPersonalizedAd;
import com.huawei.hms.ads.RequestOptions;
import com.huawei.hms.ads.banner.BannerView;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Huawei AppGallery build: same API and interstitial cap as the Play build, but served by Huawei Ads
 * (Petal) instead of AdMob. Only non-personalized ads are requested, so no consent form is needed.
 * The AdMob unit IDs passed in by callers are ignored; the Huawei slot IDs come from resources.
 */
public final class AdsHelper {
    private static final String TAG = "AdsHelper";

    /**
     * Interstitials only at game over, never mid-game. Stored in SharedPreferences so the cap survives
     * restarts: at most one per 3 minutes, none in the first 3 minutes after install.
     */
    public static final long INTERSTITIAL_MIN_INTERVAL_MS = 3 * 60_000L;
    public static final long INTERSTITIAL_FIRST_GRACE_MS = 3 * 60_000L;
    private static final String CAP_PREFS = "interstitial_cap";
    private static final String KEY_LAST = "last_shown_ms";
    private static final String KEY_FIRST_SEEN = "first_seen_ms";

    private static final AtomicBoolean sdkReady = new AtomicBoolean(false);
    private static InterstitialAd interstitialAd;

    public interface OnAdsReady {
        void onAdsReady();
    }

    private AdsHelper() {
    }

    /** Initializes Huawei Ads (non-personalized only) and calls {@code onReady} once it is ready. */
    public static void gatherConsent(@NonNull Activity activity, @NonNull OnAdsReady onReady) {
        firstSeen(capPrefs(activity), System.currentTimeMillis());
        if (activity.isFinishing()) {
            return;
        }
        if (!sdkReady.get()) {
            try {
                Context app = activity.getApplicationContext();
                HwAds.init(app);
                RequestOptions options = HwAds.getRequestOptions().toBuilder()
                        .setNonPersonalizedAd(NonPersonalizedAd.ALLOW_NON_PERSONALIZED)
                        .build();
                HwAds.setRequestOptions(options);
                sdkReady.set(true);
            } catch (RuntimeException e) {
                Log.w(TAG, "Huawei Ads init failed", e);
                return;
            }
        }
        onReady.onAdsReady();
    }

    public static boolean canRequestAds(@NonNull Context context) {
        return sdkReady.get();
    }

    public static boolean isPrivacyOptionsRequired(@NonNull Context context) {
        return false;
    }

    public static void showPrivacyOptions(@NonNull Activity activity) {
    }

    private static String slot(Context context, int real, int test) {
        return context.getString(BuildConfig.DEBUG ? test : real);
    }

    /** Adds a Huawei banner to {@code container}; returns null if ads are not ready. */
    @Nullable
    public static View loadBanner(@NonNull Activity activity, @NonNull ViewGroup container, @NonNull String adUnitId) {
        if (!canRequestAds(activity)) {
            return null;
        }
        BannerView banner = new BannerView(activity);
        banner.setAdId(slot(activity, R.string.hms_banner_id, R.string.hms_test_banner_id));
        banner.setBannerAdSize(bannerSize(activity));
        banner.setAdListener(new AdListener() {
            @Override
            public void onAdFailed(int code) {
                Log.d(TAG, "Banner failed: " + code);
            }
        });
        container.removeAllViews();
        container.addView(banner);
        banner.loadAd(new AdParam.Builder().build());
        return banner;
    }

    private static BannerAdSize bannerSize(Activity activity) {
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthDp = (int) (metrics.widthPixels / metrics.density);
        return BannerAdSize.getCurrentDirectionBannerSize(activity, widthDp);
    }

    /** Loads one interstitial in the background if none is cached or loading. */
    public static void preloadInterstitial(@NonNull Context context, @NonNull String adUnitId) {
        if (interstitialAd != null || !canRequestAds(context)) {
            return;
        }
        Context app = context.getApplicationContext();
        InterstitialAd ad = new InterstitialAd(app);
        ad.setAdId(slot(app, R.string.hms_interstitial_id, R.string.hms_test_interstitial_id));
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdFailed(int code) {
                if (interstitialAd == ad) {
                    interstitialAd = null;
                }
                Log.d(TAG, "Interstitial failed: " + code);
            }
        });
        interstitialAd = ad;
        ad.loadAd(new AdParam.Builder().build());
    }

    /**
     * Shows the cached interstitial at a natural break point if the frequency cap allows it.
     * Returns true if an ad is being shown.
     */
    public static boolean showInterstitialIfReady(@NonNull Activity activity, @NonNull String adUnitId) {
        long now = System.currentTimeMillis();
        InterstitialAd ad = interstitialAd;
        if (ad == null) {
            preloadInterstitial(activity, adUnitId);
            return false;
        }
        SharedPreferences prefs = capPrefs(activity);
        if (!ad.isLoaded()
                || !isIntervalElapsed(firstSeen(prefs, now), now, INTERSTITIAL_FIRST_GRACE_MS)
                || !isIntervalElapsed(prefs.getLong(KEY_LAST, -1L), now, INTERSTITIAL_MIN_INTERVAL_MS)
                || activity.isFinishing()) {
            return false;
        }
        final Context app = activity.getApplicationContext();
        interstitialAd = null;
        ad.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                preloadInterstitial(app, adUnitId);
            }

            @Override
            public void onAdFailed(int code) {
                preloadInterstitial(app, adUnitId);
            }
        });
        prefs.edit().putLong(KEY_LAST, now).apply();
        ad.show(activity);
        return true;
    }

    private static SharedPreferences capPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(CAP_PREFS, Context.MODE_PRIVATE);
    }

    private static long firstSeen(SharedPreferences prefs, long now) {
        long first = prefs.getLong(KEY_FIRST_SEEN, 0L);
        if (first == 0L) {
            first = now;
            prefs.edit().putLong(KEY_FIRST_SEEN, first).apply();
        }
        return first;
    }

    /** Frequency cap: true when at least {@code minIntervalMs} passed since {@code lastAt} (negative = never). */
    static boolean isIntervalElapsed(long lastAt, long now, long minIntervalMs) {
        return lastAt < 0 || now - lastAt >= minIntervalMs;
    }
}
