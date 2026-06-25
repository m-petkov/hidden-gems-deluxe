package com.github.mpetkov.hiddengemsdeluxe.screen;
import com.github.mpetkov.hiddengemsdeluxe.GameApp;
import com.github.mpetkov.hiddengemsdeluxe.model.GridManager;

import com.github.mpetkov.hiddengemsdeluxe.model.FallingBlock;
import com.github.mpetkov.hiddengemsdeluxe.model.MatchMarker;
import com.github.mpetkov.hiddengemsdeluxe.model.Particle;
import com.github.mpetkov.hiddengemsdeluxe.render.AnimatedBackground;
import com.github.mpetkov.hiddengemsdeluxe.render.GameRenderer;
import com.github.mpetkov.hiddengemsdeluxe.render.Gem3DRenderer;
import com.github.mpetkov.hiddengemsdeluxe.render.TouchControlsOverlay;
import com.github.mpetkov.hiddengemsdeluxe.util.ColorMapper;
import com.github.mpetkov.hiddengemsdeluxe.util.GameConstants;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.mpetkov.hiddengemsdeluxe.util.MobileWebLayout;
import com.github.mpetkov.hiddengemsdeluxe.util.PhonePortraitHud;
import com.github.mpetkov.hiddengemsdeluxe.util.SaveManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameScreen implements Screen, InputProcessor {

    private final GameApp game;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    /** По-голям шрифт за надписите (Game Over, Level Up, Level 12) – по-малко пикселизация. */
    private BitmapFont overlayFont;
    private SpriteBatch batch;
    private Viewport viewport;
    private OrthographicCamera camera;
    private Viewport backgroundViewport;
    private OrthographicCamera backgroundCamera;

    private int CELL_SIZE;
    private int gridOffsetX;
    private int gridOffsetY;

    private FallingBlock fallingBlock;
    private GridManager gridManager;

    private float visualFallingY;
    private float animationProgress;
    private boolean isAnimating;

    private Timer.Task dropTask;
    private Random random;

    private List<Particle> particles = new ArrayList<>();
    private List<MatchMarker> matchMarkers = new ArrayList<>();

    private boolean isProcessingMatches;

    private int score;
    private float currentDropInterval;
    private int level;
    private float levelUpTimer;
    /** Показва съобщението "Level 12 is reached, pink gems enabled" при достигане на ниво 12. */
    private float pinkEnabledMessageTimer;

    private boolean isGameOver;
    private float gameOverTimer;

    private boolean wasInitialized = false;

    private AnimatedBackground background;

    private boolean isHardDropping = false;

    /** Само едно ускорение надолу на натискане; след отпускане отново се приема. */
    private boolean downKeyReleased = true;

    private TouchControlsOverlay touchControls;
    private final Vector3 touchWorld = new Vector3();

    private MobileWebLayout.Mode mobileLayoutMode = MobileWebLayout.Mode.DESKTOP;
    private boolean compactHud;
    private boolean hudVertical;
    private float hudBaselineY;
    private float hudLineHeight;
    private float hudLineGap;
    private float hudTextX;
    private float hudScoreY;
    private float hudLevelY;
    private float hudSpeedY;
    private float hudNextRowY;
    private float hudRowSpacing;
    private float nextGemsStartX;
    private float nextGemsY;
    private int nextGemCellSize;
    private PhonePortraitHud.Metrics phoneHud;
    private int fontReferenceCellSize = -1;

    public GameScreen(GameApp game) {
        this.game = game;
    }

    private float getDropIntervalForLevel(int level) {
        int effectiveLevel = Math.min(level, 12);
        float calculatedInterval = 1.5f - (effectiveLevel - 1) * 0.10f;
        return Math.max(GameConstants.MIN_DROP_INTERVAL, calculatedInterval);
    }

    private void checkLevelUp() {
        int newLevel = (score / 10) + 1;
        if (newLevel > level) {
            level = newLevel;
            if (level == 12) {
                pinkEnabledMessageTimer = 2.5f;
            }
            float interval = getDropIntervalForLevel(level);
            if (interval < currentDropInterval) {
                currentDropInterval = interval;
                scheduleDrop(currentDropInterval);
            }
            levelUpTimer = 2f;
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(this);
        downKeyReleased = true; // винаги готови за натискане при показване на екрана

        if (wasInitialized) {
            setupGameViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            updateLayout();
            updateFontScale();
            initTouchControls();
            updateLayout();
            return;
        }
        wasInitialized = true;

        camera = new OrthographicCamera();
        backgroundCamera = new OrthographicCamera();
        setupGameViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        // ✅ КОРЕКЦИЯ 1: Инициализация на GameRenderer (зареждане на текстурата / 3D моделите)
        GameRenderer.initialize();

        background = new AnimatedBackground();

        updateLayout();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Play-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = Math.max(20, (int) (CELL_SIZE * 0.5f * Math.min(MobileWebLayout.getPixelRatio(), 2.5f)));
        fontReferenceCellSize = CELL_SIZE;
        parameter.color = Color.WHITE;
        parameter.shadowOffsetX = 2;
        parameter.shadowOffsetY = 2;
        parameter.shadowColor = new Color(0, 0, 0, 0.5f);
        parameter.borderWidth = 1f;
        parameter.borderColor = Color.BLACK;
        font = generator.generateFont(parameter);
        if (font.getRegion() != null && font.getRegion().getTexture() != null) {
            font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        FreeTypeFontGenerator.FreeTypeFontParameter overlayParam = new FreeTypeFontGenerator.FreeTypeFontParameter();
        overlayParam.size = 52;
        overlayParam.color = Color.WHITE;
        overlayFont = generator.generateFont(overlayParam);
        if (overlayFont.getRegion() != null && overlayFont.getRegion().getTexture() != null) {
            overlayFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        generator.dispose();

        random = new Random();
        gridManager = new GridManager(GameConstants.ROWS, GameConstants.COLS);

        int[] initialNextColors = new int[3];
        for (int i = 0; i < 3; i++) initialNextColors[i] = random.nextInt(ColorMapper.BASE_COLOR_COUNT);
        fallingBlock = new FallingBlock(random, gridManager, initialNextColors, ColorMapper.BASE_COLOR_COUNT);

        generateNewFallingColumn();

        level = 1;
        currentDropInterval = getDropIntervalForLevel(level);
        scheduleDrop(currentDropInterval);
        initTouchControls();
        updateLayout();
    }

    private void initTouchControls() {
        if (!TouchControlsOverlay.isEnabled()) {
            touchControls = null;
            return;
        }
        touchControls = new TouchControlsOverlay(new TouchControlsOverlay.Actions() {
            @Override public void onMoveLeft() { handleMoveLeft(); }
            @Override public void onMoveRight() { handleMoveRight(); }
            @Override public void onRotate() { handleRotate(); }
            @Override public void onDownPressed() { handleDownPressed(); }
            @Override public void onDownReleased() { handleDownReleased(); }
            @Override public void onPause() { handlePause(); }
        });
    }

    private void renderTouchControls() {
        if (touchControls == null) {
            return;
        }
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        touchControls.render(shapeRenderer);
    }

    private void unprojectTouch(int screenX, int screenY) {
        touchWorld.set(screenX, screenY, 0f);
        viewport.unproject(touchWorld);
    }

    private void setupGameViewport(int screenW, int screenH) {
        MobileWebLayout.Mode newMode = MobileWebLayout.resolveMode();
        float worldW = MobileWebLayout.worldWidth(newMode);
        float worldH = MobileWebLayout.worldHeight(newMode);

        if (viewport == null || newMode != mobileLayoutMode) {
            mobileLayoutMode = newMode;
            if (MobileWebLayout.useFillViewport(mobileLayoutMode)) {
                viewport = new FillViewport(worldW, worldH, camera);
            } else {
                viewport = new FitViewport(worldW, worldH, camera);
            }
            backgroundViewport = new FillViewport(worldW, worldH, backgroundCamera);
        } else {
            viewport.setWorldWidth(worldW);
            viewport.setWorldHeight(worldH);
            backgroundViewport.setWorldWidth(worldW);
            backgroundViewport.setWorldHeight(worldH);
        }

        viewport.update(screenW, screenH, true);
        backgroundViewport.update(screenW, screenH, true);

        if (GameRenderer.uses3DGems()) {
            Gem3DRenderer.resize((int) worldW, (int) worldH);
        }
    }

    private void updateFontScale() {
        if (font == null || fontReferenceCellSize <= 0) {
            return;
        }
        if (phoneHud != null) {
            return;
        }
        if (compactHud && hudVertical) {
            if (MobileWebLayout.isMobileWeb()) {
                float targetLine = Math.max(20f, hudLineHeight);
                float scale = targetLine / Math.max(1f, font.getLineHeight());
                font.getData().setScale(Math.max(0.52f, Math.min(scale, 0.88f)));
            } else {
                float targetLine = Math.max(1f, font.getLineHeight());
                float scale = (hudLineHeight / targetLine) * 0.82f;
                font.getData().setScale(Math.max(0.62f, Math.min(scale, 1.05f)));
            }
        } else {
            font.getData().setScale((float) CELL_SIZE / fontReferenceCellSize);
        }
    }

    private void updateLayout() {
        OrthographicCamera cam = (OrthographicCamera) viewport.getCamera();
        MobileWebLayout.Layout layout = MobileWebLayout.compute(
                mobileLayoutMode,
                cam.viewportWidth,
                cam.viewportHeight,
                cam.position.x,
                cam.position.y);

        CELL_SIZE = layout.cellSize;
        gridOffsetX = layout.gridOffsetX;
        gridOffsetY = layout.gridOffsetY;
        compactHud = layout.compactHud;
        hudVertical = layout.hudVertical;
        hudBaselineY = layout.hudBaselineY;
        hudLineHeight = layout.hudLineHeight;
        hudLineGap = layout.hudLineGap;
        hudTextX = layout.hudTextX;
        hudScoreY = layout.hudScoreY;
        hudLevelY = layout.hudLevelY;
        hudSpeedY = layout.hudSpeedY;
        hudNextRowY = layout.hudNextRowY;
        hudRowSpacing = layout.hudRowSpacing;
        nextGemsStartX = layout.nextGemsStartX;
        nextGemsY = layout.nextGemsY;
        nextGemCellSize = layout.nextGemCellSize;

        phoneHud = null;
        if (mobileLayoutMode == MobileWebLayout.Mode.MOBILE_PORTRAIT
                && MobileWebLayout.isMobileWeb()
                && font != null) {
            phoneHud = PhonePortraitHud.apply(
                    layout,
                    font,
                    cam.viewportWidth,
                    cam.viewportHeight,
                    cam.position.x,
                    cam.position.y);
            CELL_SIZE = layout.cellSize;
            gridOffsetX = layout.gridOffsetX;
            gridOffsetY = layout.gridOffsetY;
            hudTextX = layout.hudTextX;
            hudScoreY = layout.hudScoreY;
            hudLevelY = layout.hudLevelY;
            hudSpeedY = layout.hudSpeedY;
            hudNextRowY = layout.hudNextRowY;
            hudRowSpacing = layout.hudRowSpacing;
            hudLineHeight = layout.hudLineHeight;
            hudLineGap = layout.hudLineGap;
            nextGemsStartX = layout.nextGemsStartX;
            nextGemsY = layout.nextGemsY;
            nextGemCellSize = layout.nextGemCellSize;
        }

        updateFontScale();

        if (touchControls != null) {
            touchControls.applyLayout(layout);
        }
    }

    private void applyViewport() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        viewport.update(screenW, screenH, true);
        viewport.apply();
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);
    }

    private void renderBackground(float delta) {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        backgroundViewport.update(screenW, screenH, true);
        backgroundViewport.apply();
        // 2D ShapeRenderer изисква near=0 – 3D pass не трябва да оставя near=1 върху тази камера.
        backgroundCamera.near = 0f;
        backgroundCamera.far = 100f;
        backgroundCamera.up.set(0, 1, 0);
        backgroundCamera.direction.set(0, 0, -1);
        backgroundCamera.update();
        shapeRenderer.setProjectionMatrix(backgroundCamera.combined);
        background.update(delta);
        background.render(shapeRenderer, backgroundCamera);
    }

    private void renderHudOverlay() {
        batch.setProjectionMatrix(viewport.getCamera().combined);
        if (phoneHud != null) {
            PhonePortraitHud.render(batch, font, phoneHud, score, level, currentDropInterval);
            return;
        }
        GameRenderer.renderHud(batch, font, gridOffsetX, gridOffsetY, CELL_SIZE, score, level, currentDropInterval,
                compactHud, hudBaselineY, nextGemsStartX, nextGemCellSize, hudVertical, hudLineHeight, hudLineGap,
                nextGemsY, hudTextX, hudScoreY, hudLevelY, hudSpeedY, hudNextRowY);
    }

    /** 3D фонови орби (Fill) + камъни/ефекти (Fit) с актуален viewport при resize. */
    private void render3DScene() {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        backgroundViewport.update(screenW, screenH, true);
        backgroundViewport.apply();
        Gem3DRenderer.renderBackgroundOrbs(
                backgroundCamera.viewportWidth,
                backgroundCamera.viewportHeight,
                backgroundCamera.position.x,
                backgroundCamera.position.y);
        applyViewport();
        Gem3DRenderer.renderAll();
    }

    private void scheduleDrop(float interval) {
        currentDropInterval = interval;
        if (dropTask != null) dropTask.cancel();

        dropTask = new Timer.Task() {
            @Override
            public void run() {
                if (isAnimating || isProcessingMatches || isGameOver) return;

                if (fallingBlock.canRise()) {
                    fallingBlock.moveDown();
                    isAnimating = true;
                    animationProgress = 0f;
                } else {
                    for (int i = 0; i < 3; i++) {
                        int row = fallingBlock.getFallingRow() - i;
                        int col = fallingBlock.getFallingCol();
                        if (row >= 0 && row < GameConstants.ROWS) {
                            gridManager.setGridCell(row, col, fallingBlock.getFallingColors()[i]);
                            addParticles(col, row, ColorMapper.getColor(fallingBlock.getFallingColors()[i]));
                        }
                    }
                    generateNewFallingColumn();
                    processMatches();
                }
            }
        };
        Timer.schedule(dropTask, 0, interval);
    }

    private void processMatches() {
        if (isProcessingMatches) return;
        isProcessingMatches = true;

        boolean[][] toRemove = gridManager.findMatches();
        boolean anyRemoved = false;

        for (int row = 0; row < GameConstants.ROWS; row++) {
            for (int col = 0; col < GameConstants.COLS; col++) {
                if (toRemove[row][col]) {
                    Color c = ColorMapper.getColor(gridManager.getGridCell(row, col));
                    matchMarkers.add(new MatchMarker(col, row, c));
                    anyRemoved = true;
                    score++;
                    checkLevelUp();
                }
            }
        }

        if (anyRemoved) {
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    for (int row = 0; row < GameConstants.ROWS; row++) {
                        for (int col = 0; col < GameConstants.COLS; col++) {
                            if (toRemove[row][col]) {
                                Color c = ColorMapper.getColor(gridManager.getGridCell(row, col));
                                gridManager.setGridCell(row, col, -1);
                                float cx = gridOffsetX + col * CELL_SIZE + CELL_SIZE / 2f;
                                float cy = gridOffsetY + row * CELL_SIZE + CELL_SIZE / 2f;
                                Gem3DRenderer.addBurstEffect(cx, cy, c);
                                addParticles(col, row, c);
                            }
                        }
                    }

                    matchMarkers.clear();
                    gridManager.applyGravity();

                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            isProcessingMatches = false;
                            processMatches();
                        }
                    }, GameConstants.MATCH_PROCESS_DELAY);
                }
            }, GameConstants.MATCH_PROCESS_DELAY);
        } else {
            isProcessingMatches = false;
        }
    }

    private void triggerGameOver() {
        isGameOver = true;
        gameOverTimer = 3f;
        if (dropTask != null) dropTask.cancel();
    }

    private void generateNewFallingColumn() {
        int colorCount = level >= 12 ? ColorMapper.FULL_COLOR_COUNT : ColorMapper.BASE_COLOR_COUNT;
        fallingBlock.generateNewBlock(random, colorCount);
        for (int i = 0; i < 3; i++) {
            int row = fallingBlock.getFallingRow() - i;
            if (row >= 0 && row < GameConstants.ROWS && gridManager.getGridCell(row, fallingBlock.getFallingCol()) != -1) {
                triggerGameOver();
                return;
            }
        }
        animationProgress = 0f;
        visualFallingY = fallingBlock.getFallingRow();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        // Clear both color and depth so 3D gems render correctly.
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        renderBackground(delta);
        applyViewport();

        // Check for game over early and skip all game logic
        if (isGameOver) {
            if (GameRenderer.uses3DGems()) {
                Gem3DRenderer.beginFrame();
            }

            // Render the last frame of the game
            GameRenderer.renderGame(shapeRenderer, batch, font,
                gridOffsetX, gridOffsetY, CELL_SIZE,
                gridManager.getGrid(), fallingBlock,
                particles, matchMarkers,
                score, level, currentDropInterval,
                levelUpTimer, isGameOver, gameOverTimer,
                visualFallingY, compactHud, nextGemsStartX, nextGemsY, nextGemCellSize);

            if (GameRenderer.uses3DGems()) {
                render3DScene();
            }
            renderHudOverlay();

            gameOverTimer -= delta;

            float gridW = GameConstants.COLS * CELL_SIZE;
            float gridH = GameConstants.ROWS * CELL_SIZE;
            float overlayAlpha = 0.42f * Math.min(1f, (3f - gameOverTimer) / 0.35f);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.05f, 0.04f, 0.12f, overlayAlpha);
            shapeRenderer.rect(gridOffsetX, gridOffsetY, gridW, gridH);
            shapeRenderer.end();

            String gameOverText = "GAME OVER";
            float alpha = Math.min(1f, gameOverTimer);
            float titleScale = 1.05f;

            overlayFont.getData().setScale(titleScale);
            GlyphLayout gameOverLayout = new GlyphLayout(overlayFont, gameOverText);
            overlayFont.getData().setScale(1f);

            float gridCenterX = gridOffsetX + gridW / 2f;
            float gridCenterY = gridOffsetY + gridH / 2f;
            float centerX = gridCenterX - gameOverLayout.width / 2f;
            float centerY = gridCenterY + gameOverLayout.height / 2f;

            batch.begin();
            Color gameOverNeon = new Color(0.95f, 0.25f, 0.75f, 1f);
            drawNeonText3D(batch, overlayFont, gameOverText, centerX, centerY, titleScale, gameOverNeon, alpha);

            String scoreLine = "Score: " + score;
            float scoreScale = 0.62f;
            overlayFont.getData().setScale(scoreScale);
            GlyphLayout scoreLayout = new GlyphLayout(overlayFont, scoreLine);
            float sx = gridCenterX - scoreLayout.width / 2f;
            float sy = gridCenterY - gameOverLayout.height - 22;
            Color scoreNeon = new Color(0.4f, 0.95f, 1f, alpha * 0.95f);
            drawNeonText3D(batch, overlayFont, scoreLine, sx, sy, scoreScale, scoreNeon, alpha * 0.95f);
            overlayFont.getData().setScale(1f);
            batch.end();

            if (gameOverTimer <= 0f) {
                SaveManager.saveScore(score, level);
                dispose();
                game.setScreen(new WelcomeScreen(game));
            }
            return;
        }

        // Normal game logic continues here
        if (touchControls != null) {
            touchControls.update(delta);
        }

        // Подготовка на 3D рендера за този кадър (desktop only)
        if (GameRenderer.uses3DGems()) {
            Gem3DRenderer.beginFrame();
        }

        if (isAnimating) {
            float speed = isHardDropping ? 20f : 5f;
            animationProgress += delta * speed;

            if (animationProgress >= 1f) {
                animationProgress = 1f;
                isAnimating = false;

                if (isHardDropping) {
                    if (fallingBlock.canRise()) {
                        fallingBlock.moveDown();
                        animationProgress = 0f;
                        isAnimating = true;
                    } else {
                        isHardDropping = false;
                        downKeyReleased = true; // готови за следващо натискане (в случай че keyUp не е пристигнал)
                        for (int i = 0; i < 3; i++) {
                            int row = fallingBlock.getFallingRow() - i;
                            int col = fallingBlock.getFallingCol();
                            if (row >= 0 && row < GameConstants.ROWS) {
                                gridManager.setGridCell(row, col, fallingBlock.getFallingColors()[i]);
                                addParticles(col, row, ColorMapper.getColor(fallingBlock.getFallingColors()[i]));
                            }
                        }

                        generateNewFallingColumn();
                        processMatches();
                    }
                }
            }

            visualFallingY = fallingBlock.getFallingRow() + 1 - animationProgress;
        } else {
            visualFallingY = fallingBlock.getFallingRow();
            // Когато не анимираме и не сме в hard drop, винаги сме готови за ново натискане „надолу“
            if (!isHardDropping) {
                downKeyReleased = true;
            }
        }

        particles.removeIf(p -> {
            p.update(delta);
            return p.life <= 0;
        });

        if (GameRenderer.uses3DGems()) {
            Gem3DRenderer.updateEffects(delta);
        }

        matchMarkers.removeIf(m -> {
            m.update(delta);
            return m.isExpired();
        });

        GameRenderer.renderGame(shapeRenderer, batch, font,
            gridOffsetX, gridOffsetY, CELL_SIZE,
            gridManager.getGrid(), fallingBlock,
            particles, matchMarkers,
            score, level, currentDropInterval,
            levelUpTimer, isGameOver, gameOverTimer,
            visualFallingY, compactHud, nextGemsStartX, nextGemsY, nextGemCellSize);

        // Рисуване на всички 3D камъни върху фона (desktop only)
        if (GameRenderer.uses3DGems()) {
            render3DScene();
        }

        renderHudOverlay();
        renderTouchControls();

        // Level Up – само неонов надпис с 3D (без кутия)
        if (levelUpTimer > 0f) {
            String levelText = "LEVEL UP";
            float alpha = Math.min(1f, levelUpTimer);
            float scale = 1.08f;

            overlayFont.getData().setScale(scale);
            GlyphLayout levelUpLayout = new GlyphLayout(overlayFont, levelText);
            overlayFont.getData().setScale(1f);

            float gridCenterX = gridOffsetX + (GameConstants.COLS * CELL_SIZE) / 2f;
            float gridCenterY = gridOffsetY + (GameConstants.ROWS * CELL_SIZE) / 2f;
            float levelTextX = gridCenterX - levelUpLayout.width / 2f;
            float levelTextY = gridCenterY + levelUpLayout.height / 2f;

            batch.begin();
            Color levelUpNeon = new Color(0.2f, 0.98f, 0.9f, 1f);
            drawNeonText3D(batch, overlayFont, levelText, levelTextX, levelTextY, scale, levelUpNeon, alpha);
            batch.end();
        }

        // Ниво 12 – само неонов надпис с 3D (без кутия)
        if (pinkEnabledMessageTimer > 0f) {
            String pinkText = "Level 12 — pink gems enabled";
            float alpha = Math.min(1f, pinkEnabledMessageTimer);
            float scale = 0.7f;

            overlayFont.getData().setScale(scale);
            GlyphLayout pinkLayout = new GlyphLayout(overlayFont, pinkText);
            overlayFont.getData().setScale(1f);

            float gridCenterX = gridOffsetX + (GameConstants.COLS * CELL_SIZE) / 2f;
            float gridCenterY = gridOffsetY + (GameConstants.ROWS * CELL_SIZE) / 2f - 50f;
            float textX = gridCenterX - pinkLayout.width / 2f;
            float textY = gridCenterY + pinkLayout.height / 2f;

            batch.begin();
            Color pinkNeon = new Color(0.98f, 0.4f, 0.88f, 1f);
            drawNeonText3D(batch, overlayFont, pinkText, textX, textY, scale, pinkNeon, alpha);
            batch.end();
        }

        if (levelUpTimer > 0) {
            levelUpTimer -= delta;
            if (levelUpTimer < 0) levelUpTimer = 0;
        }
        if (pinkEnabledMessageTimer > 0) {
            pinkEnabledMessageTimer -= delta;
            if (pinkEnabledMessageTimer < 0) pinkEnabledMessageTimer = 0;
        }
    }

    private void addParticles(int col, int row, Color color) {
        float cx = gridOffsetX + col * CELL_SIZE + CELL_SIZE / 2f;
        float cy = gridOffsetY + row * CELL_SIZE + CELL_SIZE / 2f;

        for (int i = 0; i < 15; i++) {
            float angle = random.nextFloat() * MathUtils.PI2;
            float speed = 40 + random.nextFloat() * 60;
            float dx = MathUtils.cos(angle) * speed;
            float dy = MathUtils.sin(angle) * speed;
            particles.add(new Particle(cx, cy, dx, dy, color));
        }
        Gem3DRenderer.addShardExplosion(cx, cy, color);
    }

    /** Модерен неонов надпис: дълбока 3D сянка + многослоен ореол + ярък core (като неонова табела). */
    private void drawNeonText3D(SpriteBatch batch, BitmapFont font, String text, float x, float y,
            float scale, Color neonColor, float alpha) {
        font.getData().setScale(scale);
        float r = neonColor.r, g = neonColor.g, b = neonColor.b;

        // Дълбока 3D сянка – два слоя за обем
        font.setColor(0.02f, 0.02f, 0.08f, alpha * 0.85f);
        font.draw(batch, text, x + 6f, y - 6f);
        font.setColor(0.04f, 0.03f, 0.12f, alpha * 0.7f);
        font.draw(batch, text, x + 3f, y - 3f);

        // Външен мек ореол (неонова дифузия) – много посоки, ниска алфа
        int[] ox = { -5, -4, -3, -2, 0, 2, 3, 4, 5, -4, -3, 4, 3, -5, 5, -2, 2, 0, 0, -3, 3, -4, 4 };
        int[] oy = { 0, -2, -3, -4, -5, -4, -3, -2, 0, 2, 3, 2, 3, -2, -2, 4, 4, 5, -5, 4, 4, -3, -3 };
        for (int i = 0; i < ox.length; i++) {
            float dist = (float) Math.sqrt(ox[i] * ox[i] + oy[i] * oy[i]);
            float layerAlpha = alpha * (0.12f - dist * 0.018f);
            if (layerAlpha > 0.01f) {
                font.setColor(r, g, b, layerAlpha);
                font.draw(batch, text, x + ox[i], y + oy[i]);
            }
        }
        font.setColor(r, g, b, alpha * 0.28f);
        for (int d = -2; d <= 2; d++) {
            for (int e = -2; e <= 2; e++) {
                if (d == 0 && e == 0) continue;
                font.draw(batch, text, x + d, y + e);
            }
        }

        // Вътрешен ореол (близо до текста)
        font.setColor(r, g, b, alpha * 0.5f);
        font.draw(batch, text, x + 1f, y);
        font.draw(batch, text, x - 1f, y);
        font.draw(batch, text, x, y + 1f);
        font.draw(batch, text, x, y - 1f);

        // Ярък core (неонова тръба)
        font.setColor(r, g, b, alpha);
        font.draw(batch, text, x, y);

        font.setColor(1f, 1f, 1f, 1f);
        font.getData().setScale(1f);
    }

    private void handlePause() {
        game.pauseGame();
    }

    private void handleMoveLeft() {
        if (isGameOver) {
            return;
        }
        if (fallingBlock.canMove(-1)) {
            fallingBlock.moveHorizontal(-1);
        }
    }

    private void handleMoveRight() {
        if (isGameOver) {
            return;
        }
        if (fallingBlock.canMove(1)) {
            fallingBlock.moveHorizontal(1);
        }
    }

    private void handleRotate() {
        if (isGameOver) {
            return;
        }
        fallingBlock.rotateBlock();
    }

    private void handleDownPressed() {
        if (isGameOver) {
            return;
        }
        if (downKeyReleased && !isProcessingMatches && fallingBlock.canRise()
                && (!isAnimating || !isHardDropping)) {
            downKeyReleased = false;
            isHardDropping = true;
            if (!isAnimating) {
                fallingBlock.moveDown();
                animationProgress = 0f;
                isAnimating = true;
            }
        }
    }

    private void handleDownReleased() {
        if (downKeyReleased) {
            return;
        }
        downKeyReleased = true;
        scheduleDrop(getDropIntervalForLevel(level));
    }

    @Override
    public boolean keyDown(int keycode) {

        if (keycode == Input.Keys.ESCAPE) {
            handlePause();
            return true;
        }

        if (isGameOver) return false;

        if (keycode == Input.Keys.LEFT) {
            handleMoveLeft();
        } else if (keycode == Input.Keys.RIGHT) {
            handleMoveRight();
        } else if (keycode == Input.Keys.UP) {
            handleRotate();
        } else if (keycode == Input.Keys.DOWN) {
            handleDownPressed();
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.DOWN) {
            handleDownReleased();
        }
        return true;
    }

    @Override public boolean keyTyped(char character) { return false; }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (touchControls == null || viewport == null) {
            return false;
        }
        unprojectTouch(screenX, screenY);
        return touchControls.touchDown(touchWorld.x, touchWorld.y, pointer);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (touchControls == null || viewport == null) {
            return false;
        }
        unprojectTouch(screenX, screenY);
        return touchControls.touchUp(touchWorld.x, touchWorld.y, pointer);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (touchControls == null || viewport == null) {
            return false;
        }
        unprojectTouch(screenX, screenY);
        return touchControls.touchDragged(touchWorld.x, touchWorld.y, pointer);
    }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }

    @Override
    public void resize(int width, int height) {
        if (viewport == null) {
            return;
        }
        setupGameViewport(width, height);
        updateLayout();
        updateFontScale();
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
        if (overlayFont != null) overlayFont.dispose();
        batch.dispose();
        if (dropTask != null) dropTask.cancel();

        // ✅ КОРЕКЦИЯ 3: Освобождаване на ресурса на GameRenderer (текстурата)
        GameRenderer.dispose();
    }
}
