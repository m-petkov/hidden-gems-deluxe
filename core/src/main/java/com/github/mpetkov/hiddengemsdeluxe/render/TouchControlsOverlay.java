package com.github.mpetkov.hiddengemsdeluxe.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.IntMap;
import com.github.mpetkov.hiddengemsdeluxe.util.MobileWebLayout;

/** On-screen D-pad + pause for web (WebGL) builds. */
public final class TouchControlsOverlay {

    public interface Actions {
        void onMoveLeft();

        void onMoveRight();

        void onRotate();

        void onDownPressed();

        void onDownReleased();

        void onPause();
    }

    private enum ButtonKind {
        LEFT, RIGHT, UP, DOWN, PAUSE
    }

    private static final float INITIAL_REPEAT_SEC = 0.28f;
    private static final float REPEAT_SEC = 0.11f;

    private final Actions actions;
    private final IntMap<ButtonKind> activePointers = new IntMap<>(4);

    private ButtonKind repeatButton;
    private boolean repeatActive;
    private float repeatTimer;

    private final ControlButton left = new ControlButton(ButtonKind.LEFT);
    private final ControlButton right = new ControlButton(ButtonKind.RIGHT);
    private final ControlButton up = new ControlButton(ButtonKind.UP);
    private final ControlButton down = new ControlButton(ButtonKind.DOWN);
    private final ControlButton pause = new ControlButton(ButtonKind.PAUSE);

    public TouchControlsOverlay(Actions actions) {
        this.actions = actions;
    }

    public static boolean isEnabled() {
        return MobileWebLayout.isWeb();
    }

    public void applyLayout(MobileWebLayout.Layout layout) {
        float cx = layout.controlsCenterX;
        float cy = layout.controlsCenterY;
        float spacing = layout.controlsSpacing;
        left.set(cx - spacing, cy, layout.controlRadius);
        right.set(cx + spacing, cy, layout.controlRadius);
        up.set(cx, cy + spacing * 0.88f, layout.controlRadius);
        down.set(cx, cy - spacing * 0.88f, layout.controlRadius);
        pause.set(layout.pauseX, layout.pauseY, layout.pauseRadius);
    }

    public void update(float delta) {
        if (!repeatActive) {
            return;
        }
        repeatTimer -= delta;
        if (repeatTimer > 0f) {
            return;
        }
        repeatTimer = REPEAT_SEC;
        if (repeatButton == ButtonKind.LEFT) {
            actions.onMoveLeft();
        } else if (repeatButton == ButtonKind.RIGHT) {
            actions.onMoveRight();
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        left.render(shapeRenderer);
        right.render(shapeRenderer);
        up.render(shapeRenderer);
        down.render(shapeRenderer);
        pause.render(shapeRenderer);
    }

    public boolean touchDown(float worldX, float worldY, int pointer) {
        ButtonKind kind = hitTest(worldX, worldY);
        if (kind == null) {
            return false;
        }
        activePointers.put(pointer, kind);
        activate(kind);
        return true;
    }

    public boolean touchUp(float worldX, float worldY, int pointer) {
        ButtonKind kind = activePointers.remove(pointer);
        if (kind == null) {
            return false;
        }
        if (kind == ButtonKind.DOWN) {
            actions.onDownReleased();
        }
        clearPressed(kind);
        if (!hasPointerFor(kind)) {
            stopRepeat(kind);
        }
        return true;
    }

    public boolean touchDragged(float worldX, float worldY, int pointer) {
        ButtonKind active = activePointers.get(pointer, null);
        if (active == null) {
            return false;
        }
        ButtonKind under = hitTest(worldX, worldY);
        if (under == active) {
            return true;
        }
        if (active == ButtonKind.DOWN) {
            actions.onDownReleased();
        }
        clearPressed(active);
        activePointers.remove(pointer);
        stopRepeat(active);
        return true;
    }

    private void activate(ButtonKind kind) {
        switch (kind) {
            case LEFT -> {
                left.pressed = true;
                actions.onMoveLeft();
                startRepeat(ButtonKind.LEFT);
            }
            case RIGHT -> {
                right.pressed = true;
                actions.onMoveRight();
                startRepeat(ButtonKind.RIGHT);
            }
            case UP -> {
                up.pressed = true;
                actions.onRotate();
            }
            case DOWN -> {
                down.pressed = true;
                actions.onDownPressed();
            }
            case PAUSE -> {
                pause.pressed = true;
                actions.onPause();
            }
            default -> {
            }
        }
    }

    private void clearPressed(ButtonKind kind) {
        switch (kind) {
            case LEFT -> left.pressed = false;
            case RIGHT -> right.pressed = false;
            case UP -> up.pressed = false;
            case DOWN -> down.pressed = false;
            case PAUSE -> pause.pressed = false;
            default -> {
            }
        }
    }

    private void startRepeat(ButtonKind kind) {
        repeatButton = kind;
        repeatActive = true;
        repeatTimer = INITIAL_REPEAT_SEC;
    }

    private void stopRepeat(ButtonKind kind) {
        if (repeatActive && repeatButton == kind) {
            repeatActive = false;
        }
    }

    private boolean hasPointerFor(ButtonKind kind) {
        for (ButtonKind value : activePointers.values()) {
            if (value == kind) {
                return true;
            }
        }
        return false;
    }

    private ButtonKind hitTest(float worldX, float worldY) {
        if (pause.contains(worldX, worldY)) {
            return ButtonKind.PAUSE;
        }
        if (left.contains(worldX, worldY)) {
            return ButtonKind.LEFT;
        }
        if (right.contains(worldX, worldY)) {
            return ButtonKind.RIGHT;
        }
        if (up.contains(worldX, worldY)) {
            return ButtonKind.UP;
        }
        if (down.contains(worldX, worldY)) {
            return ButtonKind.DOWN;
        }
        return null;
    }

    private static final class ControlButton {
        float x;
        float y;
        float radius;
        final ButtonKind kind;
        boolean pressed;

        ControlButton(ButtonKind kind) {
            this.kind = kind;
        }

        void set(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        boolean contains(float wx, float wy) {
            float dx = wx - x;
            float dy = wy - y;
            return dx * dx + dy * dy <= radius * radius;
        }

        void render(ShapeRenderer sr) {
            float alpha = pressed ? 0.72f : 0.42f;
            float border = pressed ? 0.95f : 0.65f;

            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.08f, 0.06f, 0.14f, alpha);
            sr.circle(x, y, radius);
            sr.end();

            sr.begin(ShapeRenderer.ShapeType.Line);
            Gdx.gl.glLineWidth(2.5f);
            sr.setColor(0.45f * border, 1f * border, 0.15f * border, 0.9f);
            sr.circle(x, y, radius);
            sr.end();
            Gdx.gl.glLineWidth(1f);

            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(0.55f, 1f, 0.25f, pressed ? 0.95f : 0.75f);
            drawIcon(sr);
            sr.end();
        }

        private void drawIcon(ShapeRenderer sr) {
            float s = radius * 0.38f;
            switch (kind) {
                case LEFT -> drawTriangle(sr, x - s * 0.35f, y, s, 180f);
                case RIGHT -> drawTriangle(sr, x + s * 0.35f, y, s, 0f);
                case UP -> drawTriangle(sr, x, y + s * 0.35f, s, 90f);
                case DOWN -> drawTriangle(sr, x, y - s * 0.35f, s, -90f);
                case PAUSE -> {
                    float barW = s * 0.35f;
                    float barH = s * 1.1f;
                    float gap = s * 0.28f;
                    sr.rect(x - gap - barW, y - barH / 2f, barW, barH);
                    sr.rect(x + gap, y - barH / 2f, barW, barH);
                }
                default -> {
                }
            }
        }

        private static void drawTriangle(ShapeRenderer sr, float cx, float cy, float size, float directionDeg) {
            double rad = Math.toRadians(directionDeg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            float tipX = cx + cos * size;
            float tipY = cy + sin * size;
            float baseX = cx - cos * size * 0.55f;
            float baseY = cy - sin * size * 0.55f;
            float px = -sin * size * 0.55f;
            float py = cos * size * 0.55f;
            sr.triangle(tipX, tipY, baseX + px, baseY + py, baseX - px, baseY - py);
        }
    }
}
