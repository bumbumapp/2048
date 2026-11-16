package bumbumapps.a2048game;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import bumbumapps.a2048game.engine.Achievements;
import bumbumapps.a2048game.engine.GameEngine;
import bumbumapps.a2048game.engine.Mode;
import bumbumapps.a2048game.engine.StatsBook;

/** The mode hub: Classic with board sizes, four more game modes, statistics, achievements and themes. */
public class HomeActivity extends BaseActivity {
    private FrameLayout adContainer;
    private View adView;
    private boolean adsStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adContainer = new FrameLayout(this);
        build();
        startAds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isFinishing()) {
            build();
        }
    }

    private void startAds() {
        if (adsStarted) {
            return;
        }
        adsStarted = true;
        AdsHelper.gatherConsent(this, () -> {
            adView = AdsHelper.loadBanner(this, adContainer, getString(R.string.app_banner));
            AdsHelper.preloadInterstitial(this, getString(R.string.interstial_id));
        });
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        scroll.addView(col, new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        if (adContainer.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) adContainer.getParent()).removeView(adContainer);
        }
        root.addView(adContainer, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        StatsBook stats = store.stats();
        long today = System.currentTimeMillis() / 86_400_000L;

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(Ui.text(this, getString(R.string.home_greeting), 13, skin.textSecondary, 500));
        TextView title = Ui.text(this, getString(R.string.header), 32, skin.text, 800);
        title.setLetterSpacing(-0.03f);
        titles.addView(title);
        header.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        long best = stats.bestScore();
        if (best > 0) {
            LinearLayout pill = new LinearLayout(this);
            pill.setOrientation(LinearLayout.VERTICAL);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
            pill.setBackground(Ui.round(skin.surface, 16, this));
            Ui.elevate(pill, skin);
            pill.addView(Ui.text(this, getString(R.string.label_best), 11, skin.textSecondary, 700));
            pill.addView(Ui.text(this, Ui.number(best), 18, skin.text, 800));
            header.addView(pill);
        }
        col.addView(header);
        space(col, 16);

        // Hero: Classic
        col.addView(classicHero());
        space(col, 8);

        // Mode grid
        col.addView(row(modeTile(Mode.TIME, stats, today), modeTile(Mode.MOVES, stats, today)));
        space(col, 8);
        col.addView(row(modeTile(Mode.DAILY, stats, today), modeTile(Mode.BLOCKS, stats, today)));
        space(col, 8);
        col.addView(modeTile(Mode.ZEN, stats, today));
        space(col, 8);

        // Utility row
        List<Achievements.Item> ach = Achievements.compute(stats);
        View a = utilTile(R.drawable.ic_stats, getString(R.string.home_stats), getString(R.string.home_stats_sub), 0xFF4E7FA8, StatsActivity.class, false);
        View b = utilTile(R.drawable.ic_trophy, getString(R.string.home_ach),
                getString(R.string.home_ach_sub, Achievements.unlockedCount(ach), ach.size()), 0xFFE2A93B, StatsActivity.class, true);
        View c = utilTile(R.drawable.ic_theme, getString(R.string.home_themes), getString(R.string.home_themes_sub), 0xFFA855F7, ThemesActivity.class, false);
        LinearLayout util = new LinearLayout(this);
        util.setOrientation(LinearLayout.HORIZONTAL);
        util.addView(a, Ui.margins(Ui.weight(1, LinearLayout.LayoutParams.WRAP_CONTENT), this, 0, 0, 4, 0));
        util.addView(b, Ui.margins(Ui.weight(1, LinearLayout.LayoutParams.WRAP_CONTENT), this, 4, 0, 4, 0));
        util.addView(c, Ui.margins(Ui.weight(1, LinearLayout.LayoutParams.WRAP_CONTENT), this, 4, 0, 0, 0));
        col.addView(util);

        show(root);
    }

    private void space(LinearLayout col, int dp) {
        col.addView(new View(this), new LinearLayout.LayoutParams(1, Ui.dp(this, dp)));
    }

    private View row(View left, View right) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.addView(left, Ui.margins(Ui.weight(1, LinearLayout.LayoutParams.MATCH_PARENT), this, 0, 0, 4, 0));
        r.addView(right, Ui.margins(Ui.weight(1, LinearLayout.LayoutParams.MATCH_PARENT), this, 4, 0, 0, 0));
        return r;
    }

    private View classicHero() {
        int size = store.classicSize();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        card.setBackground(Ui.round(skin.surface, 24, this));
        Ui.elevate(card, skin);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(Ui.text(this, getString(R.string.mode_classic), 22, skin.text, 800));
        TextView sub = Ui.text(this, getString(R.string.classic_sub), 13, skin.textSecondary, 500);
        sub.setPadding(0, Ui.dp(this, 4), 0, 0);
        t.addView(sub);
        top.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        int[] vals = {2, 4, 0, 0, 8, 16, 4, 0, 32, 8, 2, 0, 64, 16, 4, 2};
        MiniBoardView mini = new MiniBoardView(this, 4, vals, skin);
        mini.setBackground(null);
        top.addView(mini, new LinearLayout.LayoutParams(Ui.dp(this, 64), Ui.dp(this, 64)));
        card.addView(top);
        space(card, 16);

        // Size chips
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (int s = Mode.MIN_SIZE; s <= Mode.MAX_SIZE; s++) {
            final int sz = s;
            boolean sel = s == size;
            TextView chip = Ui.text(this, Modes.sizeLabel(s), 13, sel ? skin.surface : skin.text, 800);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(Ui.card(sel ? skin.text : skin.surface2, 14, skin, this));
            chip.setOnClickListener(v -> {
                store.setClassicSize(sz);
                build();
            });
            chip.setContentDescription(Modes.sizeLabel(s));
            chips.addView(chip, Ui.margins(Ui.weight(1, Ui.dp(this, 48)), this, s == Mode.MIN_SIZE ? 0 : 2, 0, s == Mode.MAX_SIZE ? 0 : 2, 0));
        }
        card.addView(chips);
        space(card, 8);

        // Play / Continue
        GameEngine saved = store.loadGame(Store.slot(Mode.CLASSIC, size, 0));
        String label = saved != null
                ? getString(R.string.tag_continue, Ui.number(saved.getScore()))
                : getString(R.string.btn_play_size, Modes.sizeLabel(size));
        TextView play = Ui.text(this, label, 16, skin.onAccent, 800);
        play.setGravity(Gravity.CENTER);
        play.setBackground(Ui.card(skin.accent, 16, skin, this));
        play.setOnClickListener(v -> startGame(Mode.CLASSIC, size, false));
        card.addView(play, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 56)));
        if (saved != null) {
            TextView fresh = Ui.text(this, getString(R.string.btn_new_game), 13, skin.textSecondary, 700);
            fresh.setGravity(Gravity.CENTER);
            fresh.setBackground(Ui.card(0x00000000, 12, skin, this));
            fresh.setOnClickListener(v -> startGame(Mode.CLASSIC, size, true));
            card.addView(fresh, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 40)));
        }
        return card;
    }

    private View modeTile(Mode mode, StatsBook stats, long today) {
        int size = mode == Mode.BLOCKS ? store.blocksSize() : mode.defaultSize;
        GameEngine saved = store.loadGame(Store.slot(mode, size, today));
        String sub;
        boolean accentSub = false;
        switch (mode) {
            case TIME:
                sub = getString(R.string.rule_time, Ui.clock(mode.timeLimitSec * 1000L), mode.target(size));
                break;
            case MOVES:
                sub = getString(R.string.rule_moves, mode.target(size), mode.moveLimit);
                break;
            case DAILY:
                int streak = stats.currentStreak(today);
                if (stats.lastDailyDay == today) {
                    sub = getString(R.string.daily_done);
                } else if (streak > 0) {
                    sub = getString(R.string.daily_streak, streak);
                } else {
                    sub = getString(R.string.daily_new);
                }
                break;
            case BLOCKS:
                sub = getString(R.string.rule_blocks);
                break;
            default:
                sub = getString(R.string.rule_zen);
        }
        if (saved != null) {
            sub = getString(R.string.tag_continue, Ui.number(saved.getScore()));
            accentSub = true;
        }
        final String subText = sub;
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        tile.setBackground(Ui.card(skin.surface, 20, skin, this));
        Ui.elevate(tile, skin);
        tile.setMinimumHeight(Ui.dp(this, 80));
        tile.addView(Ui.iconSquare(this, Modes.icon(mode), Modes.color(mode), 40, 24));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(Ui.text(this, Modes.name(this, mode), 15, skin.text, 800));
        TextView s = Ui.text(this, subText, 12, accentSub ? skin.accent : skin.textSecondary, accentSub ? 700 : 500);
        s.setPadding(0, Ui.dp(this, 2), 0, 0);
        t.addView(s);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tp.setMarginStart(Ui.dp(this, 12));
        tile.addView(t, tp);
        tile.setOnClickListener(v -> startGame(mode, size, false));
        tile.setContentDescription(Modes.name(this, mode) + ", " + subText);
        return tile;
    }

    private View utilTile(int icon, String title, String sub, int color, Class<?> target, boolean achievements) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setPadding(Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16));
        tile.setBackground(Ui.card(skin.surface, 20, skin, this));
        Ui.elevate(tile, skin);
        tile.addView(Ui.icon(this, icon, skin.dark ? color : darker(color), 24));
        TextView t = Ui.text(this, title, 13, skin.text, 800);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 8), 0, 0);
        tile.addView(t);
        TextView s = Ui.text(this, sub, 11, skin.textSecondary, 500);
        s.setGravity(Gravity.CENTER);
        s.setPadding(0, Ui.dp(this, 2), 0, 0);
        tile.addView(s);
        tile.setOnClickListener(v -> {
            Intent i = new Intent(this, target);
            if (achievements) {
                i.putExtra(StatsActivity.EXTRA_TAB, 1);
            }
            startActivity(i);
        });
        return tile;
    }

    private static int darker(int c) {
        float[] hsv = new float[3];
        android.graphics.Color.colorToHSV(c, hsv);
        hsv[2] *= 0.85f;
        return android.graphics.Color.HSVToColor(hsv);
    }

    private void startGame(Mode mode, int size, boolean fresh) {
        Intent i = new Intent(this, GameActivity.class);
        i.putExtra(GameActivity.EXTRA_MODE, mode.key);
        i.putExtra(GameActivity.EXTRA_SIZE, size);
        i.putExtra(GameActivity.EXTRA_NEW, fresh);
        startActivity(i);
    }
}
