package bumbumapps.a2048game;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import bumbumapps.a2048game.engine.Dir;
import bumbumapps.a2048game.engine.GameEngine;
import bumbumapps.a2048game.engine.MoveResult;
import bumbumapps.a2048game.engine.Mode;
import bumbumapps.a2048game.engine.StatsBook;

/** One game of any mode: HUD, board, undo / restart / pause, and the win / game over / pause overlays. */
public class GameActivity extends BaseActivity {
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_SIZE = "size";
    public static final String EXTRA_NEW = "new";

    private static final int OVERLAY_NONE = 0;
    private static final int OVERLAY_PAUSE = 1;
    private static final int OVERLAY_CONFIRM = 2;
    private static final int OVERLAY_RESULT = 3;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Mode mode;
    private int size;
    private long today;
    private String slot;
    private String statsKey;
    private GameEngine engine;
    private boolean recorded;
    private boolean startCounted;
    private int overlayKind = OVERLAY_NONE;
    private boolean resumed;
    private long lastTick;

    private BoardView board;
    private TextView scoreV, bestV, thirdV, thirdLabel, targetValue;
    private LinearLayout barFill, barRest;
    private LinearLayout undoBtn, pauseBtn;
    private TextView undoCount, pauseLabel;
    private android.widget.ImageView pauseIcon;
    private FrameLayout overlay;
    private FrameLayout adContainer;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!resumed) {
                return;
            }
            long now = SystemClock.elapsedRealtime();
            if (overlayKind == OVERLAY_NONE && !engine.isOver()) {
                engine.tick(now - lastTick);
                updateHud();
                if (engine.isOver()) {
                    finishGame();
                }
            }
            lastTick = now;
            handler.postDelayed(this, 250);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = Mode.fromKey(getIntent().getStringExtra(EXTRA_MODE));
        size = mode.sizeOrDefault(getIntent().getIntExtra(EXTRA_SIZE, mode.defaultSize));
        today = System.currentTimeMillis() / 86_400_000L;
        slot = Store.slot(mode, size, today);
        statsKey = mode.storeKey(size);

        boolean fresh = savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_NEW, false);
        if (fresh) {
            abandonSaved();
        }
        engine = store.loadGame(slot);
        if (engine == null) {
            engine = newEngine();
        }
        startCounted = engine.getMoveCount() > 0;
        buildUi();
        board.setEngine(engine);
        updateHud();
        AdsHelper.gatherConsent(this, () -> {
            AdsHelper.loadBanner(this, adContainer, getString(R.string.app_banner));
            AdsHelper.preloadInterstitial(this, getString(R.string.interstial_id));
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (overlayKind == OVERLAY_PAUSE || overlayKind == OVERLAY_CONFIRM) {
                    hideOverlay();
                } else {
                    finish();
                }
            }
        });
        if (engine.isOver()) {
            showResult();
        }
    }

    private GameEngine newEngine() {
        long seed = mode == Mode.DAILY ? GameEngine.dailySeed(today)
                : System.nanoTime() * 31L + System.currentTimeMillis();
        recorded = false;
        return new GameEngine(mode, size, seed);
    }

    /** A game thrown away from "New game" still counts as a played game. */
    private void abandonSaved() {
        GameEngine old = store.loadGame(slot);
        if (old != null && old.getMoveCount() > 0) {
            store.stats().recordFinish(statsKey, old.getScore(), old.maxTile());
            store.saveStats();
        }
        store.clearGame(slot);
    }

    // ---- UI ----

    private void buildUi() {
        FrameLayout frame = new FrameLayout(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), 0);
        frame.addView(col, new FrameLayout.LayoutParams(-1, -1));

        // Top bar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(backButton());
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        String name = Modes.name(this, mode);
        if (mode.sizeSelectable()) {
            name += " " + Modes.sizeLabel(size);
        }
        TextView title = Ui.text(this, name, 22, skin.text, 800);
        title.setLetterSpacing(-0.02f);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titles.addView(title);
        TextView sub = Ui.text(this, subtitle(), 12, skin.textSecondary, 500);
        sub.setPadding(0, Ui.dp(this, 2), 0, 0);
        titles.addView(sub);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1f);
        tp.setMarginStart(Ui.dp(this, 12));
        bar.addView(titles, tp);
        col.addView(bar, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        col.addView(spacer(16));

        // HUD tiles
        LinearLayout hud = new LinearLayout(this);
        hud.setOrientation(LinearLayout.HORIZONTAL);
        scoreV = new TextView(this);
        bestV = new TextView(this);
        thirdV = new TextView(this);
        hud.addView(statTile(getString(R.string.label_score), scoreV, null), Ui.margins(Ui.weight(1, -2), this, 0, 0, 4, 0));
        hud.addView(statTile(getString(R.string.label_best), bestV, null), Ui.margins(Ui.weight(1, -2), this, 4, 0, 4, 0));
        thirdLabel = new TextView(this);
        hud.addView(statTile(null, thirdV, thirdLabel), Ui.margins(Ui.weight(1, -2), this, 4, 0, 0, 0));
        col.addView(hud);

        // Board + target progress: one flexible group, extra height is shared in even gaps
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        board = new BoardView(this);
        board.setSkin(skin);
        board.setSwipeListener(this::onSwipe);
        board.setReserveHeight(Ui.dp(this, 60));
        group.addView(flex(), new LinearLayout.LayoutParams(1, 0, 1f));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.gravity = Gravity.CENTER_HORIZONTAL;
        bp.topMargin = Ui.dp(this, 8);
        bp.bottomMargin = Ui.dp(this, 8);
        group.addView(board, bp);
        group.addView(flex(), new LinearLayout.LayoutParams(1, 0, 1f));

        LinearLayout tl = new LinearLayout(this);
        tl.setOrientation(LinearLayout.HORIZONTAL);
        TextView tlabel = Ui.text(this, getString(R.string.label_target), 12, skin.textSecondary, 700);
        tl.addView(tlabel, new LinearLayout.LayoutParams(0, -2, 1f));
        targetValue = Ui.text(this, "", 12, skin.text, 800);
        tl.addView(targetValue);
        group.addView(tl);
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(Ui.round(skin.surface2, 4, this));
        barFill = new LinearLayout(this);
        barFill.setBackground(Ui.round(skin.accent, 4, this));
        barRest = new LinearLayout(this);
        track.addView(barFill, new LinearLayout.LayoutParams(0, -1, 0f));
        track.addView(barRest, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams trp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 8));
        trp.topMargin = Ui.dp(this, 8);
        group.addView(track, trp);
        group.addView(flex(), new LinearLayout.LayoutParams(1, 0, 1f));
        col.addView(group, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Actions
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        undoCount = new TextView(this);
        undoBtn = actionButton(R.drawable.ic_undo, getString(R.string.btn_undo), undoCount, v -> onUndo());
        LinearLayout restartBtn = actionButton(R.drawable.ic_restart, getString(R.string.btn_restart), null, v -> askRestart());
        pauseLabel = new TextView(this);
        pauseBtn = actionButton(R.drawable.ic_pause, getString(R.string.btn_pause), null, v -> showPause());
        pauseIcon = (android.widget.ImageView) pauseBtn.getChildAt(0);
        actions.addView(undoBtn, Ui.margins(Ui.weight(1, Ui.dp(this, 72)), this, 0, 0, 4, 0));
        actions.addView(restartBtn, Ui.margins(Ui.weight(1, Ui.dp(this, 72)), this, 4, 0, 4, 0));
        actions.addView(pauseBtn, Ui.margins(Ui.weight(1, Ui.dp(this, 72)), this, 4, 0, 0, 0));
        col.addView(actions);

        adContainer = new FrameLayout(this);
        adContainer.setMinimumHeight(Ui.dp(this, 50));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = Ui.dp(this, 8);
        col.addView(adContainer, ap);

        overlay = new FrameLayout(this);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setBackgroundColor(Ui.withAlpha(skin.bg, 0xE6));
        frame.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        show(frame);
    }

    private View flex() {
        return new View(this);
    }

    private View spacer(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, Ui.dp(this, dp)));
        return v;
    }

    private String subtitle() {
        switch (mode) {
            case TIME:
                return getString(R.string.rule_time, Ui.clock(mode.timeLimitSec * 1000L), mode.target(size));
            case MOVES:
                return getString(R.string.rule_moves, mode.target(size), mode.moveLimit);
            case DAILY:
                return getString(R.string.daily_sub);
            case ZEN:
                return getString(R.string.rule_zen);
            default:
                return Modes.goal(this, mode, size);
        }
    }

    private View statTile(String label, TextView value, TextView labelView) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12));
        t.setBackground(Ui.round(skin.surface, 20, this));
        Ui.elevate(t, skin);
        TextView l = labelView != null ? labelView : new TextView(this);
        l.setText(label);
        l.setTextSize(11);
        l.setTextColor(skin.textSecondary);
        l.setTypeface(Fonts.bold(this));
        l.setIncludeFontPadding(false);
        l.setSingleLine(true);
        l.setEllipsize(android.text.TextUtils.TruncateAt.END);
        l.setGravity(Gravity.CENTER);
        value.setTextSize(22);
        value.setTextColor(skin.text);
        value.setTypeface(Fonts.extra(this));
        value.setIncludeFontPadding(false);
        value.setSingleLine(true);
        value.setGravity(Gravity.CENTER);
        value.setPadding(0, Ui.dp(this, 4), 0, 0);
        t.addView(l);
        t.addView(value);
        return t;
    }

    private LinearLayout actionButton(int icon, String label, TextView extra, View.OnClickListener click) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.card(skin.surface, 20, skin, this));
        Ui.elevate(b, skin);
        b.addView(Ui.icon(this, icon, skin.text, 24));
        TextView l = Ui.text(this, label, 12, skin.text, 800);
        l.setPadding(0, Ui.dp(this, 4), 0, 0);
        l.setGravity(Gravity.CENTER);
        l.setSingleLine(true);
        b.addView(l);
        if (extra != null) {
            extra.setTextSize(11);
            extra.setTextColor(skin.textSecondary);
            extra.setTypeface(Fonts.medium(this));
            extra.setIncludeFontPadding(false);
            extra.setGravity(Gravity.CENTER);
            b.addView(extra);
        } else {
            // Invisible second line keeps icons and labels aligned across the three buttons.
            TextView ghost = Ui.text(this, "0", 11, skin.textSecondary, 500);
            ghost.setVisibility(View.INVISIBLE);
            ghost.setGravity(Gravity.CENTER);
            b.addView(ghost);
        }
        b.setOnClickListener(click);
        b.setContentDescription(label);
        return b;
    }

    private void updateHud() {
        StatsBook st = store.stats();
        scoreV.setText(Ui.number(engine.getScore()));
        bestV.setText(Ui.number(Math.max(engine.getScore(), st.get(statsKey).bestScore)));
        if (mode == Mode.TIME) {
            thirdLabel.setText(R.string.label_time);
            long left = engine.timeLeftMs();
            thirdV.setText(Ui.clock(left));
            thirdV.setTextColor(left <= 30_000 ? 0xFFE5484D : skin.accent);
        } else if (mode == Mode.MOVES) {
            thirdLabel.setText(R.string.label_moves_left);
            thirdV.setText(String.valueOf(engine.movesLeft()));
            thirdV.setTextColor(engine.movesLeft() <= 10 ? 0xFFE5484D : skin.accent);
        } else {
            thirdLabel.setText(R.string.label_moves);
            thirdV.setText(String.valueOf(engine.getMoveCount()));
            thirdV.setTextColor(skin.text);
        }
        undoCount.setText(engine.getUndosLeft() < 0 ? getString(R.string.undo_unlimited)
                : getString(R.string.undo_left, engine.getUndosLeft()));
        undoBtn.setAlpha(engine.canUndo() ? 1f : 0.4f);

        int target = engine.getTarget();
        int max = engine.maxTile();
        targetValue.setText(Ui.number(max) + " / " + Ui.number(target));
        float p = max <= 1 ? 0f : (float) (Math.log(max) / Math.log(target));
        p = Math.max(0f, Math.min(1f, p));
        ((LinearLayout.LayoutParams) barFill.getLayoutParams()).weight = p * 1000f;
        ((LinearLayout.LayoutParams) barRest.getLayoutParams()).weight = (1f - p) * 1000f + 0.001f;
        barFill.requestLayout();
    }

    // ---- gameplay ----

    private void onSwipe(Dir dir) {
        if (overlayKind != OVERLAY_NONE || engine.isOver()) {
            return;
        }
        board.finishAnimation();
        applyMove(dir);
    }

    private void applyMove(Dir dir) {
        MoveResult r = engine.move(dir);
        if (!r.moved) {
            return;
        }
        board.animate(r);
        StatsBook st = store.stats();
        if (!startCounted) {
            startCounted = true;
            st.recordStart(statsKey);
        }
        st.noteProgress(statsKey, engine.getScore(), engine.maxTile());
        store.saveStats();
        store.saveGame(slot, engine);
        updateHud();
        if (!r.merges.isEmpty() && store.haptics()) {
            board.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        if (engine.consumeJustWon()) {
            st.recordWin(statsKey);
            if (mode == Mode.DAILY) {
                st.recordDailyDone(today);
            }
            store.saveStats();
            if (engine.isOver()) {
                finishGame();
            } else {
                overlayKind = OVERLAY_RESULT;
                handler.postDelayed(this::showWinContinue, 350);
            }
        } else if (engine.isOver()) {
            finishGame();
        }
    }

    /** The game ended (win in Time/Moves, no moves, time or moves up): record it and show the result. */
    private void finishGame() {
        if (!recorded && mode != Mode.ZEN) {
            recorded = true;
            StatsBook st = store.stats();
            if (!startCounted) {
                startCounted = true;
                st.recordStart(statsKey);
            }
            st.recordFinish(statsKey, engine.getScore(), engine.maxTile());
            if (mode == Mode.DAILY) {
                st.recordDailyDone(today);
            }
            store.saveStats();
            store.clearGame(slot);
        } else if (mode == Mode.ZEN) {
            store.saveGame(slot, engine);
        }
        overlayKind = OVERLAY_RESULT;
        if (store.haptics() && engine.getStatus() == GameEngine.LOST) {
            board.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        handler.postDelayed(() -> {
            if (!isFinishing()) {
                showResult();
                AdsHelper.showInterstitialIfReady(this, getString(R.string.interstial_id));
            }
        }, 350);
    }

    private void onUndo() {
        if (overlayKind != OVERLAY_NONE && !(overlayKind == OVERLAY_RESULT && mode == Mode.ZEN)) {
            return;
        }
        board.finishAnimation();
        if (engine.undo()) {
            hideOverlay();
            board.setEngine(engine);
            store.saveGame(slot, engine);
            updateHud();
        }
    }

    private void askRestart() {
        if (overlayKind != OVERLAY_NONE) {
            return;
        }
        if (engine.getMoveCount() == 0) {
            doRestart();
            return;
        }
        overlayKind = OVERLAY_CONFIRM;
        showCard(getString(R.string.dialog_restart_title), getString(R.string.dialog_restart_msg),
                getString(R.string.btn_restart_ok), v -> doRestart(), getString(R.string.btn_cancel), v -> hideOverlay(), null, null);
    }

    private void doRestart() {
        board.finishAnimation();
        if (!recorded && engine.getMoveCount() > 0) {
            store.stats().recordFinish(statsKey, engine.getScore(), engine.maxTile());
            store.saveStats();
        }
        store.clearGame(slot);
        engine = newEngine();
        startCounted = false;
        board.setEngine(engine);
        hideOverlay();
        updateHud();
        store.saveGame(slot, engine);
    }

    private void showPause() {
        if (overlayKind != OVERLAY_NONE || engine.isOver()) {
            return;
        }
        overlayKind = OVERLAY_PAUSE;
        showCard(getString(R.string.title_paused), null, getString(R.string.btn_resume), v -> hideOverlay(),
                getString(R.string.btn_home), v -> finish(), null, null);
    }

    private void showWinContinue() {
        if (isFinishing()) {
            return;
        }
        showCard(getString(R.string.title_win, Ui.number(engine.getTarget())), getString(R.string.msg_win_continue),
                getString(R.string.btn_keep), v -> hideOverlay(), getString(R.string.btn_play_again),
                v -> doRestart(), null, null);
    }

    private void showResult() {
        overlayKind = OVERLAY_RESULT;
        boolean won = engine.getStatus() == GameEngine.WON;
        String title;
        if (won) {
            title = getString(R.string.title_win, Ui.number(engine.getTarget()));
        } else if (engine.getReason() == GameEngine.REASON_TIME) {
            title = getString(R.string.title_timeup);
        } else if (engine.getReason() == GameEngine.REASON_MOVES) {
            title = getString(R.string.title_outmoves);
        } else {
            title = getString(R.string.title_nomoves);
        }
        String msg = getString(R.string.msg_result, Ui.number(engine.getScore()), Ui.number(engine.maxTile()));
        String third = null;
        View.OnClickListener thirdAction = null;
        if (mode == Mode.ZEN && engine.canUndo()) {
            third = getString(R.string.btn_undo_last);
            thirdAction = v -> onUndo();
        }
        showCard(title, msg, getString(won ? R.string.btn_play_again : R.string.btn_try_again), v -> doRestart(),
                getString(R.string.btn_home), v -> finish(), third, thirdAction);
    }

    private void hideOverlay() {
        overlayKind = OVERLAY_NONE;
        overlay.setVisibility(View.GONE);
        overlay.removeAllViews();
        lastTick = SystemClock.elapsedRealtime();
    }

    private void showCard(String title, String message, String primary, View.OnClickListener onPrimary,
                          String secondary, View.OnClickListener onSecondary, String third, View.OnClickListener onThird) {
        overlay.removeAllViews();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 24), Ui.dp(this, 24));
        card.setBackground(Ui.round(skin.surface, 28, this));
        card.setElevation(Ui.dp(this, 6));
        TextView t = Ui.text(this, title, 26, skin.text, 800);
        t.setGravity(Gravity.CENTER);
        t.setLetterSpacing(-0.02f);
        card.addView(t);
        if (message != null) {
            TextView m = Ui.text(this, message, 14, skin.textSecondary, 500);
            m.setGravity(Gravity.CENTER);
            m.setPadding(0, Ui.dp(this, 8), 0, 0);
            card.addView(m);
        }
        card.addView(spacer(24));
        card.addView(cardButton(primary, true, onPrimary));
        if (third != null) {
            card.addView(spacer(8));
            card.addView(cardButton(third, false, onThird));
        }
        card.addView(spacer(8));
        card.addView(cardButton(secondary, false, onSecondary));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        lp.setMargins(Ui.dp(this, 24), 0, Ui.dp(this, 24), 0);
        overlay.addView(card, lp);
        overlay.setVisibility(View.VISIBLE);
    }

    private View cardButton(String label, boolean primary, View.OnClickListener click) {
        TextView b = Ui.text(this, label, 16, primary ? skin.onAccent : skin.text, 800);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.card(primary ? skin.accent : skin.surface2, 16, skin, this));
        b.setOnClickListener(click);
        b.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));
        return b;
    }

    // ---- lifecycle ----

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        lastTick = SystemClock.elapsedRealtime();
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, 250);
    }

    @Override
    protected void onPause() {
        resumed = false;
        handler.removeCallbacks(ticker);
        board.finishAnimation();
        if (engine != null && (!engine.isOver() || mode == Mode.ZEN) && (engine.getMoveCount() > 0 || store.hasGame(slot))) {
            store.saveGame(slot, engine);
        }
        if (overlayKind == OVERLAY_NONE && !engine.isOver()) {
            // Leaving the app pauses the game; the clock is only advanced while the screen is in front.
            overlayKind = OVERLAY_PAUSE;
            showCard(getString(R.string.title_paused), null, getString(R.string.btn_resume), v -> hideOverlay(),
                    getString(R.string.btn_home), v -> finish(), null, null);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Dir d = null;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                d = Dir.UP;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                d = Dir.DOWN;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                d = Dir.LEFT;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                d = Dir.RIGHT;
                break;
            default:
                break;
        }
        if (d != null) {
            onSwipe(d);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
