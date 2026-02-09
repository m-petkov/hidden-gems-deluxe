package com.github.mpetkov.hiddengemsdeluxe.screen;
import com.github.mpetkov.hiddengemsdeluxe.GameApp;
import com.github.mpetkov.hiddengemsdeluxe.model.GridManager;

import com.github.mpetkov.hiddengemsdeluxe.model.FallingBlock;
import com.github.mpetkov.hiddengemsdeluxe.model.MatchMarker;
import com.github.mpetkov.hiddengemsdeluxe.model.Particle;
import com.github.mpetkov.hiddengemsdeluxe.render.AnimatedBackground;
import com.github.mpetkov.hiddengemsdeluxe.render.GameRenderer;
import com.github.mpetkov.hiddengemsdeluxe.render.Gem3DRenderer;
import com.github.mpetkov.hiddengemsdeluxe.util.ColorMapper;
import com.github.mpetkov.hiddengemsdeluxe.util.GameConstants;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Timer;
import com.github.mpetkov.hiddengemsdeluxe.util.SaveManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameScreen implements Screen, InputProcessor {

    private final GameApp game;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private SpriteBatch batch;

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

    private boolean isGameOver;
    private float gameOverTimer;

    private boolean wasInitialized = false;

    private AnimatedBackground background;

    private boolean isHardDropping = false;

    /** Само едно ускорение надолу на натискане; след отпускане отново се приема. */
    private boolean downKeyReleased = true;

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

        if (wasInitialized) return;
        wasInitialized = true;

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        // ✅ КОРЕКЦИЯ 1: Инициализация на GameRenderer (зареждане на текстурата / 3D моделите)
        GameRenderer.initialize();

        background = new AnimatedBackground(100);

        // === ИЗЧИСЛЯВАНЕ НА ПОЗИЦИЯТА ===
        final int PADDING = 20;

        CELL_SIZE = (Gdx.graphics.getHeight() - 2 * PADDING) / GameConstants.ROWS;

        int sidePanelWidth = CELL_SIZE * 2;
        int gridWidth = GameConstants.COLS * CELL_SIZE;
        int gridHeight = GameConstants.ROWS * CELL_SIZE;
        int totalGameWidth = gridWidth + sidePanelWidth;

        gridOffsetY = (Gdx.graphics.getHeight() - gridHeight) / 2;
        gridOffsetX = (Gdx.graphics.getWidth() - totalGameWidth) / 2;

        // ===========================================

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Play-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = (int)(CELL_SIZE * 0.5f);
        parameter.color = Color.WHITE;
        parameter.shadowOffsetX = 2;
        parameter.shadowOffsetY = 2;
        parameter.shadowColor = new Color(0, 0, 0, 0.5f);
        parameter.borderWidth = 1f;
        parameter.borderColor = Color.BLACK;
        font = generator.generateFont(parameter);
        generator.dispose();

        random = new Random();
        gridManager = new GridManager(GameConstants.ROWS, GameConstants.COLS);

        int[] initialNextColors = new int[3];
        for (int i = 0; i < 3; i++) initialNextColors[i] = random.nextInt(4);
        fallingBlock = new FallingBlock(random, gridManager, initialNextColors);

        generateNewFallingColumn();

        level = 1;
        currentDropInterval = getDropIntervalForLevel(level);
        scheduleDrop(currentDropInterval);
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
        fallingBlock.generateNewBlock(random);
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

        // Check for game over early and skip all game logic
        if (isGameOver) {
            // Only update background and render game over text
            background.update(delta);
            background.render(shapeRenderer);
            
            // Render the last frame of the game
            GameRenderer.renderGame(shapeRenderer, batch, font,
                gridOffsetX, gridOffsetY, CELL_SIZE,
                gridManager.getGrid(), fallingBlock,
                particles, matchMarkers,
                score, level, currentDropInterval,
                levelUpTimer, isGameOver, gameOverTimer,
                visualFallingY);

            gameOverTimer -= delta;

            float gridW = GameConstants.COLS * CELL_SIZE;
            float gridH = GameConstants.ROWS * CELL_SIZE;
            float overlayAlpha = 0.72f * Math.min(1f, (3f - gameOverTimer) / 0.35f);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.06f, 0.02f, 0.02f, overlayAlpha);
            shapeRenderer.rect(gridOffsetX, gridOffsetY, gridW, gridH);
            shapeRenderer.end();

            String gameOverText = "GAME OVER!";
            float alpha = Math.min(1f, gameOverTimer);
            float scale = 1.15f;

            font.getData().setScale(scale);
            GlyphLayout gameOverLayout = new GlyphLayout(font, gameOverText);
            font.getData().setScale(1f);

            float gridCenterX = gridOffsetX + gridW / 2f;
            float gridCenterY = gridOffsetY + gridH / 2f;
            float centerX = gridCenterX - gameOverLayout.width / 2f;
            float centerY = gridCenterY + gameOverLayout.height / 2f;

            batch.begin();
            font.getData().setScale(scale);
            font.setColor(0.1f, 0.15f, 0.05f, alpha);
            font.draw(batch, gameOverText, centerX + 2, centerY - 2);
            font.setColor(Color.LIME.r, Color.LIME.g, Color.LIME.b, alpha);
            font.draw(batch, gameOverText, centerX, centerY);

            String scoreLine = "Score: " + score;
            font.getData().setScale(0.7f);
            GlyphLayout scoreLayout = new GlyphLayout(font, scoreLine);
            float sx = gridCenterX - scoreLayout.width / 2f;
            float sy = gridCenterY - gameOverLayout.height - 18;
            font.setColor(Color.LIME.r * 0.7f, Color.LIME.g * 0.7f, Color.LIME.b * 0.7f, alpha * 0.9f);
            font.draw(batch, scoreLine, sx, sy);

            font.getData().setScale(1f);
            batch.end();

            if (gameOverTimer <= 0f) {
                SaveManager.saveScore(score, level);
                dispose();
                game.setScreen(new WelcomeScreen(game));
            }
            return;
        }

        // Normal game logic continues here
        background.update(delta);
        background.render(shapeRenderer);

        // Подготовка на 3D рендера за този кадър
        Gem3DRenderer.beginFrame();

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
        }

        particles.removeIf(p -> {
            p.update(delta);
            return p.life <= 0;
        });

        Gem3DRenderer.updateEffects(delta);

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
            visualFallingY);

        // Рисуване на всички 3D камъни върху фона
        Gem3DRenderer.renderAll();

        // Level Up над всичко (вкл. 3D камъните)
        if (levelUpTimer > 0f) {
            String levelText = "LEVEL UP!";
            float alpha = Math.min(1f, levelUpTimer);
            float scale = 1.15f;

            font.getData().setScale(scale);
            GlyphLayout levelUpLayout = new GlyphLayout(font, levelText);
            font.getData().setScale(1f);

            float gridCenterX = gridOffsetX + (GameConstants.COLS * CELL_SIZE) / 2f;
            float gridCenterY = gridOffsetY + (GameConstants.ROWS * CELL_SIZE) / 2f;

            float pad = 28f;
            float boxW = levelUpLayout.width + pad * 2f;
            float boxH = levelUpLayout.height + pad * 2f;
            float boxX = gridCenterX - boxW / 2f;
            float boxY = gridCenterY - boxH / 2f;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.08f, 0.08f, 0.12f, 0.92f * alpha);
            shapeRenderer.rect(boxX, boxY, boxW, boxH);
            shapeRenderer.end();

            batch.begin();
            font.getData().setScale(scale);
            float levelTextX = gridCenterX - levelUpLayout.width / 2f;
            float levelTextY = gridCenterY + levelUpLayout.height / 2f;

            font.setColor(0.1f, 0.15f, 0.05f, alpha);
            font.draw(batch, levelText, levelTextX + 2, levelTextY - 2);
            font.setColor(Color.LIME.r, Color.LIME.g, Color.LIME.b, alpha);
            font.draw(batch, levelText, levelTextX, levelTextY);
            font.getData().setScale(1f);
            batch.end();
        }

        if (levelUpTimer > 0) {
            levelUpTimer -= delta;
            if (levelUpTimer < 0) levelUpTimer = 0;
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

    @Override
    public boolean keyDown(int keycode) {

        if (keycode == Input.Keys.ESCAPE) {
            game.pauseGame();
            return true;
        }

        if (isGameOver) return false;

        if (keycode == Input.Keys.LEFT && fallingBlock.canMove(-1)) {
            fallingBlock.moveHorizontal(-1);
        } else if (keycode == Input.Keys.RIGHT && fallingBlock.canMove(1)) {
            fallingBlock.moveHorizontal(1);
        } else if (keycode == Input.Keys.UP) {
            fallingBlock.rotateBlock();
        } else if (keycode == Input.Keys.DOWN && downKeyReleased && !isAnimating && !isProcessingMatches
            && fallingBlock.canRise()) {
            downKeyReleased = false;
            fallingBlock.moveDown();
            isHardDropping = true;
            animationProgress = 0f;
            isAnimating = true;
        }

        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.DOWN) {
            downKeyReleased = true;
            scheduleDrop(getDropIntervalForLevel(level));
        }
        return true;
    }

    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }

    @Override
    public void resize(int width, int height) {
        Gem3DRenderer.resize(width, height);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
        batch.dispose();
        if (dropTask != null) dropTask.cancel();

        // ✅ КОРЕКЦИЯ 3: Освобождаване на ресурса на GameRenderer (текстурата)
        GameRenderer.dispose();
    }
}
