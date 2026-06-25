package com.github.mpetkov.hiddengemsdeluxe.util;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.github.mpetkov.hiddengemsdeluxe.util.GameConstants;

/** Layout + viewport helpers for mobile web (touch) builds. */
public final class MobileWebLayout {

    public enum Mode {
        DESKTOP,
        MOBILE_LANDSCAPE,
        MOBILE_PORTRAIT
    }

    public static final class Layout {
        public int cellSize;
        public int gridOffsetX;
        public int gridOffsetY;
        public boolean compactHud;
        /** Baseline Y for compact HUD text (landscape horizontal row). */
        public float hudBaselineY;
        /** Portrait HUD rows — evenly spaced inside {@code topBand}, like controls in {@code bottomBand}. */
        public float hudTextX;
        public float hudScoreY;
        public float hudLevelY;
        public float hudSpeedY;
        public float hudNextRowY;
        public float hudRowSpacing;
        /** Left X of the horizontal next-gem preview row. */
        public float nextGemsStartX;
        /** Bottom Y of next-gem preview row. */
        public float nextGemsY;
        public int nextGemCellSize;
        /** Lowest Y the grid bottom may sit (phone portrait). */
        public float phoneControlsFloor;
        /** When true (portrait), HUD lines stack vertically instead of one row. */
        public boolean hudVertical;
        public float hudLineHeight;
        /** Extra gap between stacked HUD lines (portrait). */
        public float hudLineGap;
        public float controlsCenterX;
        public float controlsCenterY;
        public float controlsSpacing;
        public float controlRadius;
        public float pauseX;
        public float pauseY;
        public float pauseRadius;
    }

    private MobileWebLayout() {
    }

    public static boolean isWeb() {
        return Gdx.app.getType() == Application.ApplicationType.WebGL;
    }

    /** True when the browser viewport looks like a phone (not a desktop/laptop monitor). */
    public static boolean isMobileWeb() {
        if (!isWeb()) {
            return false;
        }
        int w = cssWidth();
        int h = cssHeight();
        int min = Math.min(w, h);
        int max = Math.max(w, h);
        // Desktop / laptop monitors – always use the classic 1280×720 layout.
        if (max >= 1024 || min >= 768) {
            return false;
        }
        // Small viewport (typical phone).
        return min <= 520 && max <= 960;
    }

    public static Mode resolveMode() {
        if (!isWeb()) {
            return Mode.DESKTOP;
        }
        if (isMobileWeb()) {
            return cssHeight() > cssWidth() ? Mode.MOBILE_PORTRAIT : Mode.MOBILE_LANDSCAPE;
        }
        // Desktop browser with a tall window — same stacked HUD as phone portrait.
        if (cssHeight() > cssWidth()) {
            return Mode.MOBILE_PORTRAIT;
        }
        return Mode.DESKTOP;
    }

    private static float pixelRatio = 1f;
    private static int cssWidth = 1280;
    private static int cssHeight = 720;

    public static void setPixelRatio(float ratio) {
        pixelRatio = Math.max(1f, Math.min(ratio, 3f));
    }

    public static void setCssSize(int width, int height) {
        cssWidth = Math.max(1, width);
        cssHeight = Math.max(1, height);
    }

    /** Browser devicePixelRatio (1 on desktop). Set by the html module each frame. */
    public static float getPixelRatio() {
        return pixelRatio;
    }

    public static int cssWidth() {
        return cssWidth;
    }

    public static int cssHeight() {
        return cssHeight;
    }

    public static float worldWidth(Mode mode) {
        return mode == Mode.MOBILE_PORTRAIT ? 1080f : GameConfig.WORLD_WIDTH;
    }

    public static float worldHeight(Mode mode) {
        return mode == Mode.MOBILE_PORTRAIT ? 1920f : GameConfig.WORLD_HEIGHT;
    }

    public static final class WelcomeLayout {
        public float worldWidth;
        public float worldHeight;
        public boolean fillViewport;
        public int padTop;
        public int titlePadBottom;
        public int rowPad;
        public int fontSize;
        public int btnWidth;
        public int btnHeight;
    }

    public static WelcomeLayout welcomeLayout(Mode mode) {
        WelcomeLayout layout = new WelcomeLayout();
        layout.worldWidth = worldWidth(mode);
        layout.worldHeight = worldHeight(mode);
        layout.fillViewport = useFillViewport(mode);
        if (mode == Mode.MOBILE_PORTRAIT) {
            float u = portraitLayoutUniform();
            layout.padTop = Math.round(130 * Math.max(0.88f, u));
            layout.titlePadBottom = Math.round(44 * Math.max(0.88f, u));
            layout.rowPad = Math.round(16 * Math.max(0.88f, u));
            layout.fontSize = Math.round(32 * Math.max(0.88f, u));
            layout.btnWidth = 340;
            layout.btnHeight = 62;
        } else if (mode == Mode.MOBILE_LANDSCAPE) {
            layout.padTop = 48;
            layout.titlePadBottom = 28;
            layout.rowPad = 12;
            layout.fontSize = 28;
            layout.btnWidth = 300;
            layout.btnHeight = 52;
        } else {
            layout.worldWidth = GameConfig.WORLD_WIDTH;
            layout.worldHeight = GameConfig.WORLD_HEIGHT;
            layout.fillViewport = false;
            layout.padTop = 80;
            layout.titlePadBottom = 40;
            layout.rowPad = 10;
            layout.fontSize = 32;
            layout.btnWidth = 320;
            layout.btnHeight = 56;
        }
        return layout;
    }

    /** Typical phone portrait CSS aspect (width / height). */
    private static final float PHONE_PORTRAIT_CSS_ASPECT = 9f / 19.5f;
    private static final float PORTRAIT_CELL_BOOST = 1.07f;

    /**
     * Desktop portrait windows are often wider than a phone; scale HUD/grid rhythm so both
     * look like the same layout profile.
     */
    private static float portraitLayoutUniform() {
        if (!isWeb() || cssHeight() <= cssWidth()) {
            return 1f;
        }
        float cssAspect = (float) cssWidth() / cssHeight();
        if (cssAspect <= PHONE_PORTRAIT_CSS_ASPECT) {
            return 1f;
        }
        return PHONE_PORTRAIT_CSS_ASPECT / cssAspect;
    }

    public static boolean useFillViewport(Mode mode) {
        return mode != Mode.DESKTOP;
    }

    public static Layout compute(Mode mode, float visibleW, float visibleH, float camX, float camY) {
        Layout layout = new Layout();
        if (mode == Mode.DESKTOP) {
            computeDesktop(layout);
            if (isWeb()) {
                applyWebDesktopControls(layout, visibleW, visibleH, camX, camY);
            }
            return layout;
        }

        layout.compactHud = true;
        layout.hudVertical = mode == Mode.MOBILE_PORTRAIT;
        boolean phonePortrait = mode == Mode.MOBILE_PORTRAIT && isMobileWeb();
        float uniform = layout.hudVertical ? portraitLayoutUniform() : 1f;
        float bottomBand = phonePortrait ? visibleH * 0.20f : (mode == Mode.MOBILE_PORTRAIT ? visibleH * 0.22f : visibleH * 0.31f);
        float topBand = phonePortrait ? visibleH * 0.12f : (mode == Mode.MOBILE_PORTRAIT ? visibleH * 0.22f : visibleH * 0.11f);
        float sidePad = phonePortrait ? visibleW * 0.018f : visibleW * 0.035f;

        applyMobileControls(layout, mode, visibleW, visibleH, camX, camY, bottomBand, topBand);

        float gridAreaW = visibleW - sidePad * 2f;
        if (phonePortrait) {
            // Grid, HUD, and font scale are filled by PhonePortraitHud.apply() in GameScreen.
            layout.cellSize = 48;
            layout.gridOffsetX = Math.round(camX - GameConstants.COLS * layout.cellSize / 2f);
            layout.gridOffsetY = Math.round(camY - visibleH * 0.15f);
            layout.phoneControlsFloor = layout.controlsCenterY
                    + layout.controlsSpacing * 0.88f
                    + layout.controlRadius;
        } else {
            float gridAreaH = visibleH - bottomBand - topBand;
            if (layout.hudVertical) {
                int heightCell = Math.max(24, (int) (gridAreaH / GameConstants.ROWS * PORTRAIT_CELL_BOOST));
                int widthCell = Math.max(24, (int) (gridAreaW / GameConstants.COLS));
                layout.cellSize = Math.min(heightCell, widthCell);
            } else {
                layout.cellSize = Math.max(24, (int) Math.min(gridAreaH / GameConstants.ROWS, gridAreaW / GameConstants.COLS));
            }
        }

        int gridW = GameConstants.COLS * layout.cellSize;
        int gridH = GameConstants.ROWS * layout.cellSize;
        if (!phonePortrait) {
            layout.gridOffsetX = Math.round(camX - gridW / 2f);
            layout.gridOffsetY = Math.round(camY - visibleH / 2f + bottomBand);
        }

        if (layout.hudVertical && !phonePortrait) {
            applyPortraitHudBand(layout, camY, visibleH, topBand, uniform);
        } else if (!layout.hudVertical) {
            layout.hudLineHeight = Math.max(30f, layout.cellSize * 0.34f);
            layout.hudLineGap = Math.max(12f, layout.cellSize * 0.12f);
            layout.nextGemCellSize = Math.max(28, (int) (layout.cellSize * 0.48f));
            float gemGap = layout.nextGemCellSize * 0.20f;
            float gemsRowW = 3f * layout.nextGemCellSize + 2f * gemGap;
            layout.hudBaselineY = camY + visibleH / 2f - topBand * 0.42f;
            layout.nextGemsStartX = layout.gridOffsetX + gridW - gemsRowW;
            layout.nextGemsY = layout.hudBaselineY - layout.nextGemCellSize * 0.55f;
            float gridTop = layout.gridOffsetY + gridH;
            float hudFloor = gridTop + layout.cellSize * 0.10f;
            if (layout.hudBaselineY < hudFloor) {
                layout.hudBaselineY = hudFloor + layout.hudLineHeight * 0.5f;
                layout.nextGemsY = layout.hudBaselineY - layout.nextGemCellSize * 0.55f;
            }
        }

        return layout;
    }

    /**
     * Mirrors {@link #applyMobileControls}: items sit in the top band using fixed fractions of
     * band height, independent of grid pixel position.
     */
    private static void applyPortraitHudBand(Layout layout, float camY, float visibleH,
                                             float topBand, float uniform) {
        float bandTop = camY + visibleH / 2f;
        float padTop = topBand * 0.10f * uniform;
        layout.hudRowSpacing = topBand * 0.145f * uniform;
        layout.hudScoreY = bandTop - padTop;
        layout.hudLevelY = layout.hudScoreY - layout.hudRowSpacing;
        layout.hudSpeedY = layout.hudLevelY - layout.hudRowSpacing;
        layout.hudNextRowY = layout.hudSpeedY - layout.hudRowSpacing;
        layout.hudTextX = layout.gridOffsetX;
        layout.hudLineHeight = layout.hudRowSpacing * 0.48f;
        layout.hudLineGap = 0f;
        layout.hudBaselineY = layout.hudScoreY;
        layout.nextGemCellSize = Math.max(34, Math.round(layout.hudLineHeight * 1.5f));
        // Gem rects bottom-aligned to the "Next" text baseline.
        layout.nextGemsY = layout.hudNextRowY;
        layout.nextGemsStartX = layout.hudTextX + 52f;
    }

    private static void applyWebDesktopControls(Layout layout, float visibleW, float visibleH,
                                                float camX, float camY) {
        layout.controlRadius = Math.max(40f, Math.min(visibleW, visibleH) * 0.042f);
        layout.controlsSpacing = layout.controlRadius * 2.2f;
        layout.controlsCenterX = camX - visibleW * 0.36f;
        layout.controlsCenterY = camY - visibleH / 2f + layout.controlRadius * 2.4f;
        layout.pauseRadius = layout.controlRadius * 0.9f;
        layout.pauseX = camX + visibleW * 0.40f;
        layout.pauseY = camY + visibleH / 2f - layout.controlRadius * 2.1f;
    }

    private static void applyMobileControls(Layout layout, Mode mode, float visibleW, float visibleH,
                                            float camX, float camY, float bottomBand, float topBand) {
        layout.controlRadius = Math.max(36f, Math.min(visibleW, visibleH) * 0.052f);
        layout.controlsSpacing = layout.controlRadius * 2.25f;
        layout.controlsCenterX = camX - visibleW * (mode == Mode.MOBILE_PORTRAIT ? 0f : 0.28f);
        if (mode == Mode.MOBILE_PORTRAIT && isMobileWeb()) {
            layout.controlsCenterY = camY - visibleH / 2f + bottomBand * 0.56f;
        } else {
            layout.controlsCenterY = camY - visibleH / 2f + bottomBand * 0.52f;
        }

        layout.pauseRadius = layout.controlRadius * 0.88f;
        layout.pauseX = camX + visibleW * 0.38f;
        if (mode == Mode.MOBILE_PORTRAIT && isMobileWeb()) {
            layout.pauseY = camY + visibleH / 2f - topBand * 0.50f;
        } else {
            layout.pauseY = camY + visibleH / 2f - topBand * 0.72f;
        }
    }

    private static void computeDesktop(Layout layout) {
        final int padding = 20;
        final int sideCols = 2;
        int heightCell = (GameConfig.WORLD_HEIGHT - 2 * padding) / GameConstants.ROWS;
        int widthCell = (GameConfig.WORLD_WIDTH - 2 * padding) / (GameConstants.COLS + sideCols);
        layout.cellSize = Math.min(heightCell, widthCell);

        int gridWidth = GameConstants.COLS * layout.cellSize;
        int gridHeight = GameConstants.ROWS * layout.cellSize;
        int totalGameWidth = gridWidth + sideCols * layout.cellSize;

        layout.gridOffsetY = (GameConfig.WORLD_HEIGHT - gridHeight) / 2;
        layout.gridOffsetX = Math.max(padding, (GameConfig.WORLD_WIDTH - totalGameWidth) / 2);
        layout.compactHud = false;
    }
}
