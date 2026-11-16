package bumbumapps.a2048game;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import bumbumapps.a2048game.engine.Dir;
import bumbumapps.a2048game.engine.GameEngine;
import bumbumapps.a2048game.engine.MoveResult;
import bumbumapps.a2048game.engine.TileMove;

/**
 * Draws the board (any size, walls) and turns touches into exactly one swipe per gesture.
 * A move is animated as slide (first 60%) then pop of merged/spawned tiles.
 */
public class BoardView extends View {
    public interface SwipeListener {
        void onSwipe(Dir dir);
    }

    private static final long ANIM_MS = 170;
    private static final float SLIDE_END = 0.6f;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private GameEngine engine;
    private Skin skin = Skin.get(Skin.BENTO);
    private SwipeListener listener;

    private MoveResult anim;
    private float progress = 1f;
    private ValueAnimator animator;

    private float downX, downY;
    private boolean consumed;
    private final float swipeThreshold;

    public BoardView(Context context) {
        this(context, null);
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        textPaint.setTypeface(Fonts.extra(context));
        textPaint.setTextAlign(Paint.Align.CENTER);
        markPaint.setStyle(Paint.Style.STROKE);
        markPaint.setStrokeCap(Paint.Cap.ROUND);
        swipeThreshold = 22 * getResources().getDisplayMetrics().density;
        setContentDescription(context.getString(R.string.board_desc));
    }

    public void setEngine(GameEngine e) {
        finishAnimation();
        engine = e;
        invalidate();
    }

    public void setSkin(Skin s) {
        skin = s;
        invalidate();
    }

    public void setSwipeListener(SwipeListener l) {
        listener = l;
    }

    /** Plays the slide/pop animation for a move that has already been applied to the engine. */
    public void animate(MoveResult result) {
        finishAnimation();
        if (!result.moved) {
            return;
        }
        anim = result;
        progress = 0f;
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIM_MS);
        animator.setInterpolator(new DecelerateInterpolator(1.2f));
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                anim = null;
                progress = 1f;
                animator = null;
                invalidate();
            }
        });
        animator.start();
    }

    /** Ends a running animation immediately so the next move can start from a settled board. */
    public void finishAnimation() {
        if (animator != null) {
            ValueAnimator a = animator;
            animator = null;
            a.removeAllListeners();
            a.cancel();
        }
        anim = null;
        progress = 1f;
    }

    @Override
    protected void onDetachedFromWindow() {
        finishAnimation();
        super.onDetachedFromWindow();
    }

    private int reserveHeight;

    public void setReserveHeight(int px) {
        reserveHeight = px;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        int side = MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED ? w : Math.min(w, Math.max(0, h - reserveHeight));
        setMeasuredDimension(side, side);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                consumed = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!consumed) {
                    float dx = e.getX() - downX;
                    float dy = e.getY() - downY;
                    if (Math.max(Math.abs(dx), Math.abs(dy)) >= swipeThreshold) {
                        consumed = true; // one move per gesture, no double moves
                        Dir d = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? Dir.RIGHT : Dir.LEFT)
                                : (dy > 0 ? Dir.DOWN : Dir.UP);
                        if (listener != null) {
                            listener.onSwipe(d);
                        }
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                consumed = true;
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        GameEngine g = engine;
        float w = getWidth();
        if (g == null || w <= 0) {
            return;
        }
        int n = g.size;
        float density = getResources().getDisplayMetrics().density;
        float gap = Math.max(2 * density, w * 0.11f / (n + 1));
        float cell = (w - gap * (n + 1)) / n;
        float radius = cell * 0.14f;

        fill.setColor(skin.board);
        rect.set(0, 0, w, w);
        canvas.drawRoundRect(rect, w * 0.05f, w * 0.05f, fill);

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                float x = gap + c * (cell + gap);
                float y = gap + r * (cell + gap);
                if (g.get(r, c) == GameEngine.WALL) {
                    drawWall(canvas, x, y, cell, radius);
                } else {
                    fill.setColor(skin.empty);
                    rect.set(x, y, x + cell, y + cell);
                    canvas.drawRoundRect(rect, radius, radius, fill);
                }
            }
        }

        if (anim != null && progress < SLIDE_END) {
            float t = progress / SLIDE_END;
            for (TileMove m : anim.moves) {
                float fx = gap + m.fromC * (cell + gap);
                float fy = gap + m.fromR * (cell + gap);
                float tx = gap + m.toC * (cell + gap);
                float ty = gap + m.toR * (cell + gap);
                drawTile(canvas, fx + (tx - fx) * t, fy + (ty - fy) * t, cell, radius, m.value, 1f);
            }
            return;
        }
        float pop = anim == null ? 1f : (progress - SLIDE_END) / (1f - SLIDE_END);
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int v = g.get(r, c);
                if (v <= 0) {
                    continue;
                }
                float scale = 1f;
                if (anim != null) {
                    if (r == anim.spawnR && c == anim.spawnC) {
                        scale = pop;
                    } else if (isMerge(r, c)) {
                        scale = 1f + 0.10f * (float) Math.sin(Math.PI * pop);
                    }
                }
                drawTile(canvas, gap + c * (cell + gap), gap + r * (cell + gap), cell, radius, v, scale);
            }
        }
    }

    private boolean isMerge(int r, int c) {
        for (int[] m : anim.merges) {
            if (m[0] == r && m[1] == c) {
                return true;
            }
        }
        return false;
    }

    private void drawWall(Canvas canvas, float x, float y, float cell, float radius) {
        fill.setColor(skin.wall);
        rect.set(x, y, x + cell, y + cell);
        canvas.drawRoundRect(rect, radius, radius, fill);
        markPaint.setColor(skin.wallMark);
        markPaint.setStrokeWidth(Math.max(2f, cell * 0.07f));
        float inset = cell * 0.3f;
        canvas.drawLine(x + inset, y + inset, x + cell - inset, y + cell - inset, markPaint);
        canvas.drawLine(x + cell - inset, y + inset, x + inset, y + cell - inset, markPaint);
    }

    private void drawTile(Canvas canvas, float x, float y, float cell, float radius, int value, float scale) {
        if (scale <= 0.01f) {
            return;
        }
        float size = cell * scale;
        float off = (cell - size) / 2f;
        fill.setColor(skin.tileColor(value));
        rect.set(x + off, y + off, x + off + size, y + off + size);
        canvas.drawRoundRect(rect, radius * scale, radius * scale, fill);

        String s = String.valueOf(value);
        float base = cell * (s.length() <= 2 ? 0.46f : s.length() == 3 ? 0.40f : s.length() == 4 ? 0.33f : 0.27f);
        textPaint.setTextSize(base * scale);
        float maxW = size * 0.86f;
        float tw = textPaint.measureText(s);
        if (tw > maxW) {
            textPaint.setTextSize(base * scale * maxW / tw);
        }
        textPaint.setColor(skin.tileTextColor(value));
        float cy = y + cell / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(s, x + cell / 2f, cy, textPaint);
    }
}
