package com.github.mpetkov.hiddengemsdeluxe.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Self-contained phone portrait HUD: font scale, row layout, next-gem slot, and grid ceiling
 * are computed together from screen fractions — never from grid position.
 */
public final class PhonePortraitHud {

    private static final float FONT_SCALE = 0.72f;
    private static final float TOP_INSET = 0.026f;
    private static final float ROW_GAP = 0.0075f;
    private static final float SIDE_PAD = 0.018f;
    private static final float HUD_GRID_GAP = 0.010f;
    private static final float CONTROLS_LIFT = 0.030f;
    private static final float GRID_CELL_BOOST = 1.08f;

    public static final class Metrics {
        public float textX;
        public float scoreBaseline;
        public float levelBaseline;
        public float speedBaseline;
        public float nextBaseline;
        public float nextGemsX;
        public float nextGemsBottom;
        public int nextGemSize;
        /** Lowest Y of the HUD block; grid top sits below this. */
        public float hudFloorY;
        public float fontScale;
        public float rowStep;
    }

    private PhonePortraitHud() {
    }

    public static float fontScaleFor(float visibleH) {
        return visibleH / 1920f * FONT_SCALE;
    }

    /** Measures rows after applying font scale; fills {@code layout} grid + HUD fields. */
    public static Metrics apply(MobileWebLayout.Layout layout, BitmapFont font,
                                float visibleW, float visibleH, float camX, float camY) {
        float scale = fontScaleFor(visibleH);
        font.getData().setScale(scale);

        float lineH = font.getLineHeight() * scale;
        float rowStep = lineH + visibleH * ROW_GAP;
        int gemSize = Math.max(28, Math.round(lineH * 1.28f));

        float screenTop = camY + visibleH / 2f;

        float scoreY = screenTop - visibleH * TOP_INSET;
        float levelY = scoreY - rowStep;
        float speedY = levelY - rowStep;
        float nextY = speedY - rowStep;

        GlyphLayout nextLabel = new GlyphLayout(font, "Next");
        float gemGap = gemSize * 0.24f;
        float blockWidth = measureHudBlockWidth(font, gemSize, visibleW, gemGap);
        float textX = camX - blockWidth / 2f;
        float gemsX = textX + nextLabel.width + visibleW * 0.014f;
        float nextTextMid = nextY - lineH * 0.38f;
        float gemsBottom = nextTextMid - gemSize * 0.48f;

        float hudFloorY = Math.min(nextY - lineH * 0.12f, gemsBottom - visibleH * 0.003f);

        Metrics m = new Metrics();
        m.textX = textX;
        m.scoreBaseline = scoreY;
        m.levelBaseline = levelY;
        m.speedBaseline = speedY;
        m.nextBaseline = nextY;
        m.nextGemsX = gemsX;
        m.nextGemsBottom = gemsBottom;
        m.nextGemSize = gemSize;
        m.hudFloorY = hudFloorY;
        m.fontScale = scale;
        m.rowStep = rowStep;

        layout.controlsCenterY += visibleH * CONTROLS_LIFT;
        layout.pauseY = scoreY - lineH * 0.38f;

        float controlsFloor = layout.controlsCenterY
                + layout.controlsSpacing * 0.88f
                + layout.controlRadius
                + visibleH * 0.008f;

        float gridTop = hudFloorY - visibleH * HUD_GRID_GAP;
        float gridAreaW = visibleW - visibleW * SIDE_PAD * 2f;
        float availH = gridTop - controlsFloor;
        int heightCell = Math.max(24, (int) (availH / GameConstants.ROWS * GRID_CELL_BOOST));
        int widthCell = Math.max(24, (int) (gridAreaW / GameConstants.COLS * GRID_CELL_BOOST));
        layout.cellSize = Math.min(heightCell, widthCell);

        int gridH = GameConstants.ROWS * layout.cellSize;
        layout.gridOffsetY = Math.round(gridTop - gridH);
        if (layout.gridOffsetY < controlsFloor) {
            layout.cellSize = Math.max(24, (int) ((gridTop - controlsFloor) / GameConstants.ROWS));
            gridH = GameConstants.ROWS * layout.cellSize;
            layout.gridOffsetY = Math.round(gridTop - gridH);
        }

        int gridW = GameConstants.COLS * layout.cellSize;
        layout.gridOffsetX = Math.round(camX - gridW / 2f);
        layout.phoneControlsFloor = controlsFloor;

        layout.hudTextX = textX;
        layout.hudScoreY = scoreY;
        layout.hudLevelY = levelY;
        layout.hudSpeedY = speedY;
        layout.hudNextRowY = nextY;
        layout.hudRowSpacing = rowStep;
        layout.hudLineHeight = lineH;
        layout.hudLineGap = visibleH * ROW_GAP;
        layout.nextGemsStartX = gemsX;
        layout.nextGemsY = gemsBottom;
        layout.nextGemCellSize = gemSize;

        return m;
    }

    public static void render(SpriteBatch batch, BitmapFont font, Metrics m,
                              int score, int level, float dropInterval) {
        batch.begin();
        Color lime = Color.LIME;
        drawShadowedText(batch, font, "Score: " + score, m.textX, m.scoreBaseline, lime);
        drawShadowedText(batch, font, "Level: " + level, m.textX, m.levelBaseline, lime);
        drawShadowedText(batch, font, String.format("Speed: %.2f s", dropInterval),
                m.textX, m.speedBaseline, lime);
        drawShadowedText(batch, font, "Next", m.textX, m.nextBaseline, lime);
        batch.end();
    }

    private static void drawShadowedText(SpriteBatch batch, BitmapFont font, String text,
                                         float x, float y, Color color) {
        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, text, x + 1, y - 1);
        font.setColor(color);
        font.draw(batch, text, x, y);
    }

    /** Widest HUD row (text lines + next-gem preview) for horizontal centering. */
    private static float measureHudBlockWidth(BitmapFont font, int gemSize, float visibleW, float gemGap) {
        GlyphLayout layout = new GlyphLayout();
        float max = 0f;
        layout.setText(font, "Score: 9999");
        max = Math.max(max, layout.width);
        layout.setText(font, "Level: 99");
        max = Math.max(max, layout.width);
        layout.setText(font, "Speed: 9.99 s");
        max = Math.max(max, layout.width);
        layout.setText(font, "Next");
        float nextRow = layout.width + visibleW * 0.014f + 3f * gemSize + 2f * gemGap;
        return Math.max(max, nextRow);
    }
}
