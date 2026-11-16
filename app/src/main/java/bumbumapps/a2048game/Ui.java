package bumbumapps.a2048game;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.NumberFormat;
import java.util.Locale;

/** Small view-building helpers so every screen shares the same sizes, radii and type scale (8dp grid). */
public final class Ui {
    private Ui() {
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable round(int color, float radiusDp, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    public static GradientDrawable roundStroke(int fill, int stroke, int strokeDp, float radiusDp, Context c) {
        GradientDrawable d = round(fill, radiusDp, c);
        d.setStroke(dp(c, strokeDp), stroke);
        return d;
    }

    /** Rounded surface with a ripple, used for every tappable card. */
    public static Drawable card(int color, float radiusDp, Skin skin, Context c) {
        GradientDrawable shape = round(color, radiusDp, c);
        int ripple = skin.dark ? 0x22FFFFFF : 0x14000000;
        GradientDrawable mask = round(Color.WHITE, radiusDp, c);
        return new RippleDrawable(ColorStateList.valueOf(ripple), shape, mask);
    }

    public static void elevate(View v, Skin skin) {
        v.setElevation(skin.dark ? 0 : dp(v.getContext(), 2));
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, int weight) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(weight >= 800 ? Fonts.extra(c) : weight >= 700 ? Fonts.bold(c) : Fonts.medium(c));
        t.setIncludeFontPadding(false);
        return t;
    }

    public static ImageView icon(Context c, int res, int tint, int sizeDp) {
        ImageView v = new ImageView(c);
        v.setImageDrawable(ContextCompat.getDrawable(c, res));
        v.setColorFilter(tint);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return v;
    }

    /** Tinted square holding an icon (the pastel icon squares of the Bento look). */
    public static View iconSquare(Context c, int res, int tint, int sizeDp, int iconDp) {
        android.widget.FrameLayout f = new android.widget.FrameLayout(c);
        f.setBackground(round(withAlpha(tint, 0x2A), 12, c));
        ImageView i = icon(c, res, tint, iconDp);
        android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(dp(c, iconDp), dp(c, iconDp), Gravity.CENTER);
        f.addView(i, lp);
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return f;
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams weight(float w, int heightPx) {
        return new LinearLayout.LayoutParams(0, heightPx, w);
    }

    public static ViewGroup.MarginLayoutParams margins(LinearLayout.LayoutParams p, Context c, int l, int t, int r, int b) {
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        p.setMarginStart(dp(c, l));
        p.setMarginEnd(dp(c, r));
        return p;
    }

    public static String number(long v) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(v);
    }

    public static String clock(long ms) {
        long s = Math.max(0, (ms + 999) / 1000);
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    public static String clockElapsed(long ms) {
        long s = Math.max(0, ms / 1000);
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    public static Drawable none() {
        return new ColorDrawable(Color.TRANSPARENT);
    }
}
