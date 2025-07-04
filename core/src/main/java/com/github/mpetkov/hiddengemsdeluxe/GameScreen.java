package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Timer;

import java.util.*;

public class GameScreen implements Screen, InputProcessor {

    private static class MatchMarker {
        int col, row;
        float timer = 0.8f; // беше 0.5f
        Color color;

        MatchMarker(int col, int row, Color color) {
            this.col = col;
            this.row = row;
            this.color = color;
        }

        void update(float delta) {
            timer -= delta;
        }

        boolean isExpired() {
            return timer <= 0;
        }
    }

    private float getDropIntervalForLevel(int level) {
        return Math.max(MIN_DROP_INTERVAL, 1.5f - (level - 1) * 0.05f);
    }

    private boolean isGameOver = false;
    private float gameOverTimer = 0f;

    private final Main game;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private SpriteBatch batch;

    private static final int ROWS = 12;
    private static final int COLS = 6;
    private int CELL_SIZE;

    private int gridOffsetX;
    private int gridOffsetY;

    private int fallingRow = ROWS - 1;
    private int fallingCol = 2;
    private int[] fallingColors = new int[3];
    private int[] nextColors = new int[3];

    private float visualFallingY = fallingRow;
    private float animationProgress = 0;
    private boolean isAnimating = false;

    private Timer.Task dropTask;
    private Random random;
    private int[][] grid;

    private List<Particle> particles = new ArrayList<>();
    private List<MatchMarker> matchMarkers = new ArrayList<>();

    private boolean isProcessingMatches = false;

    private int score = 0;

    private float currentDropInterval = 1.5f;

    private int level = 1;
    private float levelUpTimer = 0f;
    private static final float MIN_DROP_INTERVAL = 0.05f;

    private boolean wasInitialized = false;

    private AnimatedBackground background;

    public GameScreen(Main game) {
        this.game = game;
    }

    private void checkLevelUp() {
        int newLevel = score / 20 + 1;
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
        Gdx.input.setInputProcessor(this); // <-- винаги!

        if (wasInitialized) return;
        wasInitialized = true;
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(this);

        background = new AnimatedBackground(100); // Брой падащи линии

        CELL_SIZE = Gdx.graphics.getHeight() / ROWS;
        int sidePanelWidth = CELL_SIZE * 2;
        gridOffsetX = (Gdx.graphics.getWidth() - COLS * CELL_SIZE - sidePanelWidth) / 2;
        gridOffsetY = 0;

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
        for (int i = 0; i < 3; i++) nextColors[i] = random.nextInt(4);

        grid = new int[ROWS][COLS];
        for (int[] row : grid) Arrays.fill(row, -1);

        generateNewFallingColumn();



        scheduleDrop(1.5f); // забавено тройно
    }

    private void scheduleDrop(float interval) {
        currentDropInterval = interval;

        if (dropTask != null) dropTask.cancel();

        dropTask = new Timer.Task() {
            @Override
            public void run() {
                if (isAnimating || isProcessingMatches) return;

                if (canRise()) {
                    fallingRow--;
                    isAnimating = true;
                    animationProgress = 0f;
                } else {
                    for (int i = 0; i < 3; i++) {
                        int row = fallingRow - i;
                        if (row >= 0 && row < ROWS) {
                            grid[row][fallingCol] = fallingColors[i];
                            addParticles(fallingCol, row, getColor(fallingColors[i]));
                        }
                    }
                    generateNewFallingColumn();
                    processMatches();
                }
            }
        };

        Timer.schedule(dropTask, 0, interval);
    }

    private boolean checkAndRemoveMatches() {
        boolean[][] toRemove = new boolean[ROWS][COLS];
        boolean anyRemoved = false;

        // Хоризонтално
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col <= COLS - 3; col++) {
                int color = grid[row][col];
                if (color != -1 &&
                    color == grid[row][col + 1] &&
                    color == grid[row][col + 2]) {
                    toRemove[row][col] = true;
                    toRemove[row][col + 1] = true;
                    toRemove[row][col + 2] = true;
                }
            }
        }

        // Вертикално
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row <= ROWS - 3; row++) {
                int color = grid[row][col];
                if (color != -1 &&
                    color == grid[row + 1][col] &&
                    color == grid[row + 2][col]) {
                    toRemove[row][col] = true;
                    toRemove[row + 1][col] = true;
                    toRemove[row + 2][col] = true;
                }
            }
        }

        // Диагонали ↘
        for (int row = 0; row <= ROWS - 3; row++) {
            for (int col = 0; col <= COLS - 3; col++) {
                int color = grid[row][col];
                if (color != -1 &&
                    color == grid[row + 1][col + 1] &&
                    color == grid[row + 2][col + 2]) {
                    toRemove[row][col] = true;
                    toRemove[row + 1][col + 1] = true;
                    toRemove[row + 2][col + 2] = true;
                }
            }
        }

        // Диагонали ↙
        for (int row = 0; row <= ROWS - 3; row++) {
            for (int col = 2; col < COLS; col++) {
                int color = grid[row][col];
                if (color != -1 &&
                    color == grid[row + 1][col - 1] &&
                    color == grid[row + 2][col - 2]) {
                    toRemove[row][col] = true;
                    toRemove[row + 1][col - 1] = true;
                    toRemove[row + 2][col - 2] = true;
                }
            }
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (toRemove[row][col]) {
                    Color c = getColor(grid[row][col]);
                    matchMarkers.add(new MatchMarker(col, row, c));
                    anyRemoved = true;
                    score++; // ⬅ Добавяне на 1 точка за всеки премахнат блок
                    checkLevelUp();
                }
            }
        }

        if (anyRemoved) {
            isProcessingMatches = true;

            final boolean[][] finalToRemove = new boolean[ROWS][COLS];
            for (int row = 0; row < ROWS; row++) {
                System.arraycopy(toRemove[row], 0, finalToRemove[row], 0, COLS);
            }

            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    for (int row = 0; row < ROWS; row++) {
                        for (int col = 0; col < COLS; col++) {
                            if (finalToRemove[row][col]) {
                                Color c = getColor(grid[row][col]);
                                grid[row][col] = -1;
                                addParticles(col, row, c);
                            }
                        }
                    }

                    matchMarkers.clear();
                    applyGravity();

                    // Ново изчакване преди следваща проверка
                    Timer.schedule(new Timer.Task() {
                        @Override
                        public void run() {
                            isProcessingMatches = false;
                            processMatches();
                        }
                    }, 0.8f); // беше 0.5f
                }
            }, 0.8f); // беше 0.5f
        }

        return anyRemoved;
    }

    private void applyGravity() {
        for (int col = 0; col < COLS; col++) {
            for (int row = 1; row < ROWS; row++) {
                if (grid[row][col] != -1 && grid[row - 1][col] == -1) {
                    int currentRow = row;
                    while (currentRow > 0 && grid[currentRow - 1][col] == -1) {
                        grid[currentRow - 1][col] = grid[currentRow][col];
                        grid[currentRow][col] = -1;
                        currentRow--;
                    }
                }
            }
        }
    }

    private void processMatches() {
        if (isProcessingMatches) return;

        isProcessingMatches = true;

        boolean found = checkAndRemoveMatches(); // Търсим нови съвпадения

        if (!found) {
            isProcessingMatches = false;
        }
        // Ако има, checkAndRemoveMatches ще се погрижи да извика пак processMatches() след 0.5 секунди
    }

    private void triggerGameOver() {
        isGameOver = true;
        gameOverTimer = 3f;
        if (dropTask != null) dropTask.cancel(); // спри падащия таймер
    }




    private void generateNewFallingColumn() {
        System.arraycopy(nextColors, 0, fallingColors, 0, 3);
        fallingRow = ROWS - 1;
        fallingCol = 2 + random.nextInt(2);
        animationProgress = 0f;
        visualFallingY = fallingRow;

        for (int i = 0; i < 3; i++) {
            nextColors[i] = random.nextInt(4);
        }

        // Проверка дали мястото за новата колона е заето (Game Over)
        for (int i = 0; i < 3; i++) {
            int row = fallingRow - i;
            if (row >= 0 && grid[row][fallingCol] != -1) {
                triggerGameOver();
                return;
            }
        }
    }

    private boolean canRise() {
        return fallingRow >= 3 &&
            grid[fallingRow - 1][fallingCol] == -1 &&
            grid[fallingRow - 2][fallingCol] == -1 &&
            grid[fallingRow - 3][fallingCol] == -1;
    }

    private boolean canMove(int dir) {
        int newCol = fallingCol + dir;
        if (newCol < 0 || newCol >= COLS) return false;
        for (int i = 0; i < 3; i++) {
            int row = fallingRow - i;
            if (row >= 0 && grid[row][newCol] != -1) return false;
        }
        return true;
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        background.update(delta);
        background.render(shapeRenderer);

        if (isAnimating) {
            animationProgress += delta * 5f;
            if (animationProgress >= 1f) {
                animationProgress = 1f;
                isAnimating = false;
            }
            visualFallingY = fallingRow + 1 - animationProgress;
        } else {
            visualFallingY = fallingRow;
        }

        particles.removeIf(p -> {
            p.update(delta);
            return p.life <= 0;
        });

        matchMarkers.removeIf(m -> {
            m.update(delta);
            return m.isExpired();
        });

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 1);
                shapeRenderer.rect(gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int color = grid[row][col];
                if (color != -1) {
                    draw3DBlock(gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, getColor(color));
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            float drawY = (visualFallingY - i) * CELL_SIZE;
            if (fallingRow - i >= 0) {
                draw3DBlock(gridOffsetX + fallingCol * CELL_SIZE, gridOffsetY + drawY, getColor(fallingColors[i]));
            }
        }

        for (Particle p : particles) {
            shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, p.life / p.initialLife);
            shapeRenderer.circle(p.x, p.y, p.size);
        }

        float previewX = gridOffsetX + COLS * CELL_SIZE + 40;
        float nextBlockY = gridOffsetY + (ROWS - 2) * CELL_SIZE;
        String nextText = "Next:";
        GlyphLayout layout = new GlyphLayout(font, nextText);
        float textX = previewX + CELL_SIZE / 2f - layout.width / 2f;
        float topRowY = gridOffsetY + (ROWS - 1) * CELL_SIZE + CELL_SIZE / 2f;
        float textY = topRowY + layout.height / 2f;

        String scoreText = "Score: " + score;
        GlyphLayout scoreLayout = new GlyphLayout(font, scoreText);
        float scoreX = gridOffsetX - scoreLayout.width - 40;
        float scoreY = topRowY + scoreLayout.height / 2f;

        float currentDropInterval = this.currentDropInterval;
        String speedText = String.format("Speed: %.2f s", currentDropInterval);
        GlyphLayout speedLayout = new GlyphLayout(font, speedText);
        float speedX = scoreX + scoreLayout.width - speedLayout.width;
        float speedY = scoreY - scoreLayout.height - 10;

        for (int i = 0; i < 3; i++) {
            float y = nextBlockY - i * CELL_SIZE;
            draw3DBlock(previewX, y, getColor(nextColors[i]));
        }

        for (MatchMarker m : matchMarkers) {
            float x = gridOffsetX + m.col * CELL_SIZE;
            float y = gridOffsetY + m.row * CELL_SIZE;
            float alpha = 1f - m.timer / 0.8f;
            float pulse = 0.5f + 0.5f * MathUtils.sin(alpha * MathUtils.PI * 2);

            Color glowTarget = new Color(1f, 0.85f, 0.6f, 1f);
            Color glowColor = m.color.cpy().lerp(glowTarget, pulse);
            glowColor.a = 0.7f + 0.3f * pulse;

            shapeRenderer.setColor(glowColor);
            shapeRenderer.rect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
        }

        shapeRenderer.end();

        batch.begin();

        // Сянка
        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, nextText, textX + 1, textY - 1);
        font.draw(batch, scoreText, scoreX + 1, scoreY - 1);
        font.draw(batch, speedText, speedX + 1, speedY - 1);

        // Основен текст
        font.setColor(Color.ORANGE);
        font.draw(batch, nextText, textX, textY);
        font.draw(batch, scoreText, scoreX, scoreY);
        font.draw(batch, speedText, speedX, speedY);

        String levelDisplayText = "Level: " + level;
        GlyphLayout levelLayout = new GlyphLayout(font, levelDisplayText);
        float levelX = speedX + speedLayout.width - levelLayout.width;
        float levelY = speedY - speedLayout.height - 10;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, levelDisplayText, levelX + 1, levelY - 1);
        font.setColor(Color.ORANGE);
        font.draw(batch, levelDisplayText, levelX, levelY);

        if (levelUpTimer > 0f) {
            levelUpTimer -= delta;

            String levelText = "LEVEL UP!";

            float alpha = Math.min(1f, levelUpTimer); // Избледняване
            float scale = 1f + 0.3f * (float)Math.sin((2f - levelUpTimer) * Math.PI); // Пулсиране

            font.getData().setScale(scale);
            GlyphLayout levelUpLayout = new GlyphLayout(font, levelText);

            float gridCenterX = gridOffsetX + (COLS * CELL_SIZE) / 2f;
            float gridCenterY = gridOffsetY + (ROWS * CELL_SIZE) / 2f;

            float levelTextX = gridCenterX - levelUpLayout.width / 2f;
            float levelTextY = gridCenterY + levelUpLayout.height / 2f;

            font.setColor(1f, 0.8f, 0.2f, alpha);
            font.draw(batch, levelText, levelTextX, levelTextY);

            font.getData().setScale(1f); // възстанови нормалния мащаб
        }

        if (isGameOver) {
            gameOverTimer -= delta;

            String gameOverText = "GAME OVER!";
            float alpha = Math.min(1f, gameOverTimer);
            float scale = 1f + 0.3f * (float)Math.sin((3f - gameOverTimer) * Math.PI);

            font.getData().setScale(scale);
            GlyphLayout gameOverLayout = new GlyphLayout(font, gameOverText);

            float centerX = gridOffsetX + (COLS * CELL_SIZE - gameOverLayout.width) / 2f;
            float centerY = gridOffsetY + (ROWS * CELL_SIZE + gameOverLayout.height) / 2f;

            font.setColor(1f, 0.4f, 0.4f, alpha);
            font.draw(batch, gameOverText, centerX, centerY);

            font.getData().setScale(1f);

            if (gameOverTimer <= 0f) {
                game.setScreen(new WelcomeScreen(game));

                // Отложи dispose() с 0.1 секунди, за да избегнеш конфликт с текущия render()
                Timer.schedule(new Timer.Task() {
                    @Override
                    public void run() {
                        dispose();
                    }
                }, 0.1f);
            }

            batch.end();
            return; // спри допълнително рендериране
        }

        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 1);
        for (int row = 0; row <= ROWS; row++) {
            shapeRenderer.line(gridOffsetX, gridOffsetY + row * CELL_SIZE, gridOffsetX + COLS * CELL_SIZE, gridOffsetY + row * CELL_SIZE);
        }
        for (int col = 0; col <= COLS; col++) {
            shapeRenderer.line(gridOffsetX + col * CELL_SIZE, gridOffsetY, gridOffsetX + col * CELL_SIZE, gridOffsetY + ROWS * CELL_SIZE);
        }
        shapeRenderer.end();
    }

    private void draw3DBlock(float x, float y, Color baseColor) {
        float padding = 6f;
        float size = CELL_SIZE - 2 * padding;
        float radius = size * 0.1f;
        float left = x + padding;
        float bottom = y + padding;

        shapeRenderer.setColor(baseColor);
        shapeRenderer.rect(left + radius, bottom + radius, size - 2 * radius, size - 2 * radius);

        shapeRenderer.circle(left + radius, bottom + radius, radius);
        shapeRenderer.circle(left + size - radius, bottom + radius, radius);
        shapeRenderer.circle(left + radius, bottom + size - radius, radius);
        shapeRenderer.circle(left + size - radius, bottom + size - radius, radius);

        shapeRenderer.setColor(baseColor.cpy().lerp(Color.BLACK, 0.25f));
        shapeRenderer.rect(left + size * 0.6f, bottom, size * 0.4f, size);
        shapeRenderer.rect(left, bottom, size, size * 0.2f);

        shapeRenderer.setColor(baseColor.cpy().lerp(Color.WHITE, 0.2f));
        shapeRenderer.rect(left, bottom + size * 0.8f, size, size * 0.2f);
        shapeRenderer.rect(left, bottom, size * 0.2f, size);

        shapeRenderer.setColor(1, 1, 1, 0.12f);
        shapeRenderer.ellipse(left + size * 0.15f, bottom + size * 0.65f, size * 0.35f, size * 0.25f);
    }

    private Color getColor(int index) {
        switch (index) {
            case 0: return Color.RED;
            case 1: return Color.BLUE;
            case 2: return Color.GREEN;
            case 3: return Color.YELLOW;
            default: return Color.WHITE;
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
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.LEFT && canMove(-1)) {
            fallingCol--;
        } else if (keycode == Input.Keys.RIGHT && canMove(1)) {
            fallingCol++;
        } else if (keycode == Input.Keys.DOWN) {
            scheduleDrop(0.05f); // ускорено падане
        } else if (keycode == Input.Keys.UP) {
            // Завъртане на камъните
            int top = fallingColors[0];
            fallingColors[0] = fallingColors[1];
            fallingColors[1] = fallingColors[2];
            fallingColors[2] = top;
        } else if (keycode == Input.Keys.ENTER) {
            game.pauseGame(this); // Вместо директно да сетваме WelcomeScreen
        }


        return true;
    }


    @Override
    public boolean keyUp(int keycode) {
        if (keycode == Input.Keys.DOWN) {
            scheduleDrop(getDropIntervalForLevel(level)); // връщаме скоростта за текущото ниво
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

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        shapeRenderer.dispose();
        font.dispose();
        batch.dispose();
        if (dropTask != null) dropTask.cancel();
    }

    private static class Particle {
        float x, y, dx, dy;
        float size = 6f;
        float life = 0.6f;
        float initialLife = 0.6f;
        Color color;

        Particle(float x, float y, float dx, float dy, Color color) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.color = new Color(color);
        }

        void update(float delta) {
            x += dx * delta;
            y += dy * delta;
            life -= delta;
        }
    }
}
