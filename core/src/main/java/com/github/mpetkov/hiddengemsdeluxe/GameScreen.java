package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Timer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameScreen implements Screen {

    private final Main game;
    private ShapeRenderer shapeRenderer;

    private static final int ROWS = 12;
    private static final int COLS = 6;
    private static final int CELL_SIZE = 60;

    private int gridOffsetX;
    private int gridOffsetY;

    private int fallingRow = ROWS - 1;
    private int fallingCol = 2;
    private int fallingColor;
    private float visualFallingY = fallingRow;
    private float animationProgress = 0;
    private boolean isAnimating = false;

    private Random random;
    private int[][] grid;

    private List<Particle> particles = new ArrayList<>();

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        random = new Random();
        fallingColor = random.nextInt(4);

        grid = new int[ROWS][COLS];
        for (int row = 0; row < ROWS; row++)
            for (int col = 0; col < COLS; col++)
                grid[row][col] = -1;

        gridOffsetX = (Gdx.graphics.getWidth() - COLS * CELL_SIZE) / 2;
        gridOffsetY = (Gdx.graphics.getHeight() - ROWS * CELL_SIZE) / 2;

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (isAnimating) return;

                if (canRise()) {
                    fallingRow--;
                    isAnimating = true;
                    animationProgress = 0f;
                } else {
                    grid[fallingRow][fallingCol] = fallingColor;
                    addParticles(fallingCol, fallingRow, getColor(fallingColor));
                    fallingRow = ROWS - 1;
                    fallingCol = 2 + random.nextInt(2);
                    fallingColor = random.nextInt(4);
                    animationProgress = 0f;
                    visualFallingY = fallingRow;
                }
            }
        }, 0.5f, 0.5f);
    }

    private boolean canRise() {
        return fallingRow > 0 && grid[fallingRow - 1][fallingCol] == -1;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Плавна анимация на изкачване
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

        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.update(delta);
            if (p.life <= 0) iterator.remove();
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Фон на решетката
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 1);
                shapeRenderer.rect(gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // Камъни
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int color = grid[row][col];
                if (color != -1) {
                    draw3DBlock(gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, getColor(color));
                }
            }
        }

        // Падащ (вдигащ се) камък
        draw3DBlock(gridOffsetX + fallingCol * CELL_SIZE, gridOffsetY + visualFallingY * CELL_SIZE, getColor(fallingColor));

        // Частици
        for (Particle p : particles) {
            shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, p.life / p.initialLife);
            shapeRenderer.circle(p.x, p.y, p.size);
        }

        shapeRenderer.end();

        // Рамки на решетката
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

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        shapeRenderer.dispose();
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
