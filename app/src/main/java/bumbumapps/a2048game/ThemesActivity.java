package bumbumapps.a2048game;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;

/** Skin picker with live previews, the haptics switch, and the privacy options row where required. */
public class ThemesActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(backButton());
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = Ui.text(this, getString(R.string.title_themes), 24, skin.text, 800);
        title.setLetterSpacing(-0.02f);
        titles.addView(title);
        TextView sub = Ui.text(this, getString(R.string.themes_sub), 12, skin.textSecondary, 500);
        sub.setPadding(0, Ui.dp(this, 2), 0, 0);
        titles.addView(sub);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(Ui.dp(this, 12));
        bar.addView(titles, tp);
        root.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        scroll.addView(col, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        String[] keys = Skin.ALL;
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 2; c++) {
                row.addView(themeCard(keys[r * 2 + c]), Ui.margins(Ui.weight(1, -1), this, c == 0 ? 0 : 4, 0, c == 0 ? 4 : 0, 0));
            }
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.bottomMargin = Ui.dp(this, 8);
            col.addView(row, rp);
        }

        // Haptics
        LinearLayout hap = new LinearLayout(this);
        hap.setOrientation(LinearLayout.HORIZONTAL);
        hap.setGravity(Gravity.CENTER_VERTICAL);
        hap.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        hap.setBackground(Ui.round(skin.surface, 20, this));
        Ui.elevate(hap, skin);
        LinearLayout ht = new LinearLayout(this);
        ht.setOrientation(LinearLayout.VERTICAL);
        ht.addView(Ui.text(this, getString(R.string.haptics), 16, skin.text, 800));
        TextView hd = Ui.text(this, getString(R.string.haptics_d), 12, skin.textSecondary, 500);
        hd.setPadding(0, Ui.dp(this, 2), 0, 0);
        ht.addView(hd);
        hap.addView(ht, new LinearLayout.LayoutParams(0, -2, 1f));
        SwitchCompat sw = new SwitchCompat(this);
        sw.setChecked(store.haptics());
        sw.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));
        sw.setTrackTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{skin.accent, skin.dark ? 0xFF4A5068 : 0xFFC9CCDA}));
        sw.setOnCheckedChangeListener((b, on) -> store.setHaptics(on));
        sw.setContentDescription(getString(R.string.haptics));
        hap.addView(sw);
        hap.setOnClickListener(v -> sw.toggle());
        col.addView(hap, new LinearLayout.LayoutParams(-1, -2));

        if (AdsHelper.isPrivacyOptionsRequired(this)) {
            TextView p = Ui.text(this, getString(R.string.privacy_options), 15, skin.text, 800);
            p.setGravity(Gravity.CENTER_VERTICAL);
            p.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
            p.setBackground(Ui.card(skin.surface, 20, skin, this));
            p.setOnClickListener(v -> AdsHelper.showPrivacyOptions(this));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 56));
            pp.topMargin = Ui.dp(this, 8);
            col.addView(p, pp);
        }
        show(root);
    }

    private View themeCard(String key) {
        Skin s = Skin.get(key);
        boolean sel = store.skin().key.equals(key);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        card.setBackground(Ui.roundStroke(skin.surface, sel ? skin.accent : skin.surface, 2, 20, this));
        Ui.elevate(card, skin);
        int[] vals = {2, 4, 8, 16, 32, 0, 64, 0, 2};
        FrameLayout prev = new FrameLayout(this);
        prev.setBackground(Ui.round(s.bg, 14, this));
        prev.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        prev.addView(new MiniBoardView(this, 3, vals, s));
        card.addView(prev, new LinearLayout.LayoutParams(-1, -2));
        TextView name = Ui.text(this, skinName(key), 15, skin.text, 800);
        name.setPadding(0, Ui.dp(this, 12), 0, 0);
        card.addView(name);
        TextView d = Ui.text(this, skinDesc(key), 12, skin.textSecondary, 500);
        d.setPadding(0, Ui.dp(this, 2), 0, 0);
        card.addView(d);
        card.setOnClickListener(v -> {
            store.setSkin(key);
            recreate();
        });
        card.setContentDescription(skinName(key) + (sel ? ", " + getString(R.string.selected) : ""));
        return card;
    }

    private String skinName(String key) {
        switch (key) {
            case Skin.NIGHT:
                return getString(R.string.skin_night);
            case Skin.PASTEL:
                return getString(R.string.skin_pastel);
            case Skin.CONTRAST:
                return getString(R.string.skin_contrast);
            default:
                return getString(R.string.skin_bento);
        }
    }

    private String skinDesc(String key) {
        switch (key) {
            case Skin.NIGHT:
                return getString(R.string.skin_night_d);
            case Skin.PASTEL:
                return getString(R.string.skin_pastel_d);
            case Skin.CONTRAST:
                return getString(R.string.skin_contrast_d);
            default:
                return getString(R.string.skin_bento_d);
        }
    }
}
