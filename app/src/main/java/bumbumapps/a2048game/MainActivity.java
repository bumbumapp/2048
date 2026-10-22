package bumbumapps.a2048game;


import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;


public class MainActivity extends AppCompatActivity {

    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SCORE = "score";
    private static final String HIGH_SCORE = "high score temp";
    private static final String UNDO_SCORE = "undo score";
    private static final String CAN_UNDO = "can undo";
    private static final String UNDO_GRID = "undo";
    private static final String GAME_STATE = "game state";
    private static final String UNDO_GAME_STATE = "undo game state";
    private static final String MAX_TILE = "max tile";
    private static final String TIMER = "timer";
    private AdView adView;


    private MainView view;
    Thread t;
    volatile public static boolean running = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        EdgeToEdgeInsets.apply(this);
        view = (MainView) findViewById(R.id.view);
        final android.view.ViewGroup adContainer = findViewById(R.id.ad_container);
        AdsHelper.gatherConsent(this, () -> {
            adView = AdsHelper.loadBanner(this, adContainer, getString(R.string.app_banner));
            AdsHelper.preloadInterstitial(this, getString(R.string.interstial_id));
            updatePrivacyButton();
        });

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        view.hasSaveState = settings.getBoolean("save_state", false);

        if (savedInstanceState != null) {

            if (savedInstanceState.getBoolean("hasState")) {
                load();
            }
        }

        // Elapsed Time Counter
        t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (running) {
                                    view.setElTime();

                                }
                            }
                        });
                    }
                } catch (InterruptedException ignored) {
                }
            }
        };
        t.start();

    }

    private void updatePrivacyButton() {
        android.view.View privacy = findViewById(R.id.privacy_options);
        if (AdsHelper.isPrivacyOptionsRequired(this)) {
            privacy.setVisibility(android.view.View.VISIBLE);
            privacy.setOnClickListener(v -> AdsHelper.showPrivacyOptions(this));
        } else {
            privacy.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            //Do nothing
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            view.game.move(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            view.game.move(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            view.game.move(3);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            view.game.move(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("hasState", true);
        save();


    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
        running = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
        running = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        save();
        running = false;
    }

    private void save() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = view.game.grid.field;
        Tile[][] undoField = view.game.grid.undoField;
        editor.putInt(WIDTH, field.length);
        editor.putInt(HEIGHT, field.length);
        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null) {
                    editor.putInt(xx + " " + yy, field[xx][yy].getValue());
                } else {
                    editor.putInt(xx + " " + yy, 0);
                }

                if (undoField[xx][yy] != null) {
                    editor.putInt(UNDO_GRID + xx + " " + yy, undoField[xx][yy].getValue());
                } else {
                    editor.putInt(UNDO_GRID + xx + " " + yy, 0);
                }
            }
        }
        editor.putLong(SCORE, view.game.score);
        editor.putLong(HIGH_SCORE, view.game.highScore);
        editor.putLong(UNDO_SCORE, view.game.lastScore);
        editor.putBoolean(CAN_UNDO, view.game.canUndo);
        editor.putInt(GAME_STATE, view.game.gameState);
        editor.putInt(UNDO_GAME_STATE, view.game.lastGameState);
        editor.putLong(MAX_TILE, view.game.maxTile);
        editor.putInt(TIMER, view.elTime);
        editor.commit();
    }

    /**
     * Restores a tile from its saved value: &gt;0 creates a tile, 0 means empty, -1 (missing) keeps the current tile.
     */
    static Tile restoreTile(int x, int y, int savedValue, Tile current) {
        if (savedValue > 0) {
            return new Tile(x, y, savedValue);
        } else if (savedValue == 0) {
            return null;
        }
        return current;
    }

    @Override
    protected void onDestroy() {
        if (t != null) {
            t.interrupt();
        }
        super.onDestroy();
    }

    private void load() {
        //Stopping all animations

        view.game.aGrid.cancelAnimations();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        for (int xx = 0; xx < view.game.grid.field.length; xx++) {
            for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
                int value = settings.getInt(xx + " " + yy, -1);
                view.game.grid.field[xx][yy] = restoreTile(xx, yy, value, view.game.grid.field[xx][yy]);

                int undoValue = settings.getInt(UNDO_GRID + xx + " " + yy, -1);
                view.game.grid.undoField[xx][yy] = restoreTile(xx, yy, undoValue, view.game.grid.undoField[xx][yy]);
            }
        }

        view.game.score = settings.getLong(SCORE, view.game.score);
        view.game.highScore = settings.getLong(HIGH_SCORE, view.game.highScore);
        view.game.lastScore = settings.getLong(UNDO_SCORE, view.game.lastScore);
        view.game.canUndo = settings.getBoolean(CAN_UNDO, view.game.canUndo);
        view.game.gameState = settings.getInt(GAME_STATE, view.game.gameState);
        view.game.lastGameState = settings.getInt(UNDO_GAME_STATE, view.game.lastGameState);
        view.game.maxTile = settings.getLong(MAX_TILE, view.game.maxTile);
        view.elTime = settings.getInt(TIMER, view.elTime);

    }









}
