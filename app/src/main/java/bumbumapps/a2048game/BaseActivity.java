package bumbumapps.a2048game;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Applies the chosen skin to the window and re-creates the screen when the skin changes elsewhere. */
public abstract class BaseActivity extends AppCompatActivity {
    protected Skin skin;
    protected Store store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        store = Store.get(this);
        skin = store.skin();
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(skin.bg);
        getWindow().setNavigationBarColor(skin.bg);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(!skin.dark);
        c.setAppearanceLightNavigationBars(!skin.dark);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!store.skin().key.equals(skin.key)) {
            recreate();
        }
    }

    /** Adds the system-bar padding and sets the content view. */
    protected void show(View content) {
        setContentView(content);
        EdgeToEdgeInsets.apply(this);
    }

    protected View backButton() {
        android.widget.FrameLayout b = new android.widget.FrameLayout(this);
        b.setBackground(Ui.card(skin.surface, 14, skin, this));
        Ui.elevate(b, skin);
        android.widget.ImageView i = Ui.icon(this, R.drawable.ic_back, skin.text, 24);
        b.addView(i, new android.widget.FrameLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24), android.view.Gravity.CENTER));
        b.setContentDescription(getString(R.string.back));
        b.setOnClickListener(v -> finish());
        b.setLayoutParams(new android.widget.LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        return b;
    }
}
