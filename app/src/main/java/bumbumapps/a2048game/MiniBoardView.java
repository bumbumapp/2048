package bumbumapps.a2048game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Static little board used for previews (home hero, theme cards). */
public class MiniBoardView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final int[] values;
    private final int n;
    private Skin skin;

    public MiniBoardView(Context c, int n, int[] values, Skin skin) {
        super(c);
        this.n = n;
        this.values = values;
        this.skin = skin;
        text.setTypeface(Fonts.extra(c));
        text.setTextAlign(Paint.Align.CENTER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onMeasure(int w, int h) {
        int side = Math.min(MeasureSpec.getSize(w), MeasureSpec.getSize(h));
        if (MeasureSpec.getMode(h) == MeasureSpec.UNSPECIFIED) {
            side = MeasureSpec.getSize(w);
        }
        setMeasuredDimension(side, side);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float gap = w * 0.05f;
        float cell = (w - gap * (n + 1)) / n;
        fill.setColor(skin.board);
        r.set(0, 0, w, w);
        canvas.drawRoundRect(r, w * 0.07f, w * 0.07f, fill);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                float x = gap + j * (cell + gap);
                float y = gap + i * (cell + gap);
                int v = values[i * n + j];
                fill.setColor(v == 0 ? skin.empty : skin.tileColor(v));
                r.set(x, y, x + cell, y + cell);
                canvas.drawRoundRect(r, cell * 0.16f, cell * 0.16f, fill);
                if (v > 0) {
                    String s = String.valueOf(v);
                    text.setColor(skin.tileTextColor(v));
                    text.setTextSize(cell * (s.length() <= 2 ? 0.46f : 0.36f));
                    canvas.drawText(s, x + cell / 2f, y + cell / 2f - (text.descent() + text.ascent()) / 2f, text);
                }
            }
        }
    }
}
