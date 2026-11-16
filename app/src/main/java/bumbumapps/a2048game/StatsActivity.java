package bumbumapps.a2048game;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bumbumapps.a2048game.engine.Achievements;
import bumbumapps.a2048game.engine.Mode;
import bumbumapps.a2048game.engine.ModeStat;
import bumbumapps.a2048game.engine.StatsBook;

/** Statistics per mode and the achievements list, in two tabs. */
public class StatsActivity extends BaseActivity {
    public static final String EXTRA_TAB = "tab";

    private int tab;
    private LinearLayout content;
    private TextView tabStats, tabAch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tab = savedInstanceState != null ? savedInstanceState.getInt(EXTRA_TAB) : getIntent().getIntExtra(EXTRA_TAB, 0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(backButton());
        TextView title = Ui.text(this, getString(R.string.title_progress), 24, skin.text, 800);
        title.setLetterSpacing(-0.02f);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(Ui.dp(this, 12));
        bar.addView(title, tp);
        root.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));

        LinearLayout seg = new LinearLayout(this);
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        seg.setBackground(Ui.round(skin.surface2, 18, this));
        tabStats = segment(getString(R.string.tab_stats), 0);
        tabAch = segment(getString(R.string.tab_ach), 1);
        seg.addView(tabStats, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f));
        seg.addView(tabAch, new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = Ui.dp(this, 16);
        root.addView(seg, sp);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        show(root);
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(EXTRA_TAB, tab);
    }

    private TextView segment(String label, int index) {
        TextView t = Ui.text(this, label, 14, skin.text, 800);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setOnClickListener(v -> {
            tab = index;
            render();
        });
        return t;
    }

    private void styleSegment(TextView t, boolean selected) {
        t.setBackground(selected ? Ui.round(skin.surface, 14, this) : Ui.none());
        t.setTextColor(selected ? skin.text : skin.textSecondary);
        if (selected) {
            Ui.elevate(t, skin);
        } else {
            t.setElevation(0);
        }
    }

    private void render() {
        styleSegment(tabStats, tab == 0);
        styleSegment(tabAch, tab == 1);
        content.removeAllViews();
        if (tab == 0) {
            renderStats();
        } else {
            renderAchievements();
        }
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.round(skin.surface, 24, this));
        Ui.elevate(c, skin);
        return c;
    }

    private void renderStats() {
        StatsBook s = store.stats();
        if (s.totalGames() == 0 && s.bestScore() == 0) {
            LinearLayout c = card();
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.setPadding(Ui.dp(this, 24), Ui.dp(this, 32), Ui.dp(this, 24), Ui.dp(this, 24));
            c.addView(Ui.iconSquare(this, R.drawable.ic_stats, 0xFF4E7FA8, 56, 32));
            TextView t = Ui.text(this, getString(R.string.empty_stats_title), 20, skin.text, 800);
            t.setPadding(0, Ui.dp(this, 16), 0, 0);
            t.setGravity(Gravity.CENTER);
            c.addView(t);
            TextView m = Ui.text(this, getString(R.string.empty_stats_msg), 14, skin.textSecondary, 500);
            m.setGravity(Gravity.CENTER);
            m.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 24));
            c.addView(m);
            TextView b = Ui.text(this, getString(R.string.btn_play_now), 16, skin.onAccent, 800);
            b.setGravity(Gravity.CENTER);
            b.setBackground(Ui.card(skin.accent, 16, skin, this));
            b.setOnClickListener(v -> finish());
            c.addView(b, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));
            content.addView(c);
            return;
        }
        long today = System.currentTimeMillis() / 86_400_000L;
        LinearLayout r1 = new LinearLayout(this);
        r1.addView(summary(getString(R.string.stat_games), Ui.number(s.totalGames())), Ui.margins(Ui.weight(1, -2), this, 0, 0, 4, 0));
        r1.addView(summary(getString(R.string.stat_best_tile), Ui.number(s.bestTile())), Ui.margins(Ui.weight(1, -2), this, 4, 0, 0, 0));
        content.addView(r1);
        LinearLayout r2 = new LinearLayout(this);
        r2.addView(summary(getString(R.string.stat_best_score), Ui.number(s.bestScore())), Ui.margins(Ui.weight(1, -2), this, 0, 0, 4, 0));
        r2.addView(summary(getString(R.string.stat_streak), String.valueOf(s.currentStreak(today))), Ui.margins(Ui.weight(1, -2), this, 4, 0, 0, 0));
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(-1, -2);
        p2.topMargin = Ui.dp(this, 8);
        content.addView(r2, p2);

        LinearLayout list = card();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Ui.dp(this, 16);
        content.addView(list, lp);
        List<String> keys = new ArrayList<>();
        for (Mode m : Mode.values()) {
            for (Map.Entry<String, ModeStat> e : s.all().entrySet()) {
                String k = e.getKey();
                if ((k.equals(m.key) || k.startsWith(m.key + "_")) && e.getValue().games > 0) {
                    keys.add(k);
                }
            }
        }
        boolean first = true;
        for (String k : keys) {
            if (!first) {
                list.addView(divider());
            }
            first = false;
            list.addView(statRow(k, s.get(k)));
        }
    }

    private View summary(String label, String value) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        t.setBackground(Ui.round(skin.surface, 20, this));
        Ui.elevate(t, skin);
        TextView l = Ui.text(this, label.toUpperCase(java.util.Locale.getDefault()), 11, skin.textSecondary, 700);
        l.setSingleLine(true);
        l.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.addView(l);
        TextView v = Ui.text(this, value, 24, skin.text, 800);
        v.setSingleLine(true);
        v.setPadding(0, Ui.dp(this, 4), 0, 0);
        t.addView(v);
        return t;
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(skin.divider);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, Ui.dp(this, 1));
        p.setMarginStart(Ui.dp(this, 16));
        p.setMarginEnd(Ui.dp(this, 16));
        d.setLayoutParams(p);
        return d;
    }

    private View statRow(String key, ModeStat m) {
        int us = key.indexOf('_');
        Mode mode = Mode.fromKey(us > 0 ? key.substring(0, us) : key);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        row.addView(Ui.iconSquare(this, Modes.icon(mode), Modes.color(mode), 40, 24));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(Ui.text(this, Modes.nameForKey(this, key), 15, skin.text, 800));
        TextView sub = Ui.text(this, getString(R.string.stat_row_games, m.games, m.wins) + " · "
                + getString(R.string.stat_row_avg, Ui.number(m.averageScore())), 12, skin.textSecondary, 500);
        sub.setPadding(0, Ui.dp(this, 2), 0, 0);
        t.addView(sub);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(Ui.dp(this, 12));
        row.addView(t, tp);
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setGravity(Gravity.END);
        TextView best = Ui.text(this, Ui.number(m.bestScore), 16, skin.text, 800);
        best.setGravity(Gravity.END);
        r.addView(best);
        TextView tile = Ui.text(this, "■ " + Ui.number(m.bestTile), 12, skin.accent, 700);
        tile.setGravity(Gravity.END);
        tile.setPadding(0, Ui.dp(this, 2), 0, 0);
        r.addView(tile);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2);
        rp.setMarginStart(Ui.dp(this, 8));
        row.addView(r, rp);
        return row;
    }

    private void renderAchievements() {
        List<Achievements.Item> items = Achievements.compute(store.stats());
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(Ui.text(this, getString(R.string.ach_unlocked_n, Achievements.unlockedCount(items), items.size()), 20, skin.text, 800));
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(Ui.iconSquare(this, R.drawable.ic_trophy, 0xFFE2A93B, 48, 28));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.bottomMargin = Ui.dp(this, 16);
        content.addView(head, hp);

        LinearLayout list = card();
        content.addView(list);
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                list.addView(divider());
            }
            list.addView(achRow(items.get(i)));
        }
    }

    private View achRow(Achievements.Item it) {
        String[] txt = achText(it);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16));
        int green = 0xFF3BB273;
        row.addView(Ui.iconSquare(this, it.unlocked() ? R.drawable.ic_check : R.drawable.ic_trophy,
                it.unlocked() ? green : 0xFFE2A93B, 40, 24));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(Ui.text(this, txt[0], 15, skin.text, 800));
        TextView d = Ui.text(this, txt[1], 12, skin.textSecondary, 500);
        d.setPadding(0, Ui.dp(this, 2), 0, Ui.dp(this, 8));
        col.addView(d);
        LinearLayout track = new LinearLayout(this);
        track.setBackground(Ui.round(skin.surface2, 3, this));
        LinearLayout fill = new LinearLayout(this);
        fill.setBackground(Ui.round(it.unlocked() ? green : skin.accent, 3, this));
        track.addView(fill, new LinearLayout.LayoutParams(0, -1, Math.max(0.001f, it.percent())));
        track.addView(new View(this), new LinearLayout.LayoutParams(0, -1, Math.max(0.001f, 100 - it.percent())));
        col.addView(track, new LinearLayout.LayoutParams(-1, Ui.dp(this, 6)));
        LinearLayout foot = new LinearLayout(this);
        foot.setPadding(0, Ui.dp(this, 6), 0, 0);
        foot.addView(Ui.text(this, getString(it.unlocked() ? R.string.ach_status_unlocked : R.string.ach_status_progress),
                11, it.unlocked() ? green : skin.textSecondary, 700), new LinearLayout.LayoutParams(0, -2, 1f));
        foot.addView(Ui.text(this, Ui.number(it.current) + " / " + Ui.number(it.target), 11, skin.textSecondary, 700));
        col.addView(foot);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -2, 1f);
        cp.setMarginStart(Ui.dp(this, 12));
        row.addView(col, cp);
        return row;
    }

    private String[] achText(Achievements.Item it) {
        String id = it.id;
        if (id.startsWith("tile")) {
            String n = Ui.number(it.target);
            return new String[]{getString(R.string.ach_tile_t, n), getString(R.string.ach_tile_d, n)};
        }
        switch (id) {
            case "games10":
            case "games50":
                String n = Ui.number(it.target);
                return new String[]{getString(R.string.ach_games_t, n), getString(R.string.ach_games_d, n)};
            case "score10000":
                return new String[]{getString(R.string.ach_score_t, Ui.number(it.target)), getString(R.string.ach_score_d, Ui.number(it.target))};
            case "timewin":
                return new String[]{getString(R.string.ach_time_t), getString(R.string.ach_time_d)};
            case "moveswin":
                return new String[]{getString(R.string.ach_moves_t), getString(R.string.ach_moves_d)};
            case "streak3":
                return new String[]{getString(R.string.ach_streak_t), getString(R.string.ach_streak_d, (int) it.target)};
            default:
                return new String[]{getString(R.string.ach_all_t), getString(R.string.ach_all_d, (int) it.target)};
        }
    }
}
