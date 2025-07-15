package com.github.mpetkov.hiddengemsdeluxe;// GameRenderer.java
// Няма декларация за пакет

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;



public class GameRenderer {

    private GameRenderer() {
        // Приватен конструктор за статичен клас
    }

    public static void renderGame(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
                                  int gridOffsetX, int gridOffsetY, int CELL_SIZE,
                                  int[][] grid, FallingBlock fallingBlock,
                                  List<Particle> particles, List<MatchMarker> matchMarkers,
                                  int score, int level, float currentDropInterval,
                                  float levelUpTimer, boolean isGameOver, float gameOverTimer) {

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled); // <- Единствен begin за filled форми
        for (int row = 0; row < GameConstants.ROWS; row++) {
            for (int col = 0; col < GameConstants.COLS; col++) {
                shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 1);
                shapeRenderer.rect(gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        for (int row = 0; row < GameConstants.ROWS; row++) {
            for (int col = 0; col < GameConstants.COLS; col++) {
                int color = grid[row][col];
                if (color != -1) {
                    draw3DBlock(shapeRenderer, gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, CELL_SIZE, ColorMapper.getColor(color));
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            // float drawY = (fallingBlock.getFallingRow() + 1 - (GameConstants.ROWS - fallingBlock.getFallingRow())) * CELL_SIZE; // Тази променлива не се използва
            // float drawYCorrected = gridOffsetY + (fallingBlock.getFallingRow() - i) * CELL_SIZE; // Тази променлива не се използва

            if (fallingBlock.getFallingRow() - i >= 0) {
                draw3DBlock(shapeRenderer, gridOffsetX + fallingBlock.getFallingCol() * CELL_SIZE,
                    gridOffsetY + (fallingBlock.getFallingRow() - i) * CELL_SIZE,
                    CELL_SIZE, ColorMapper.getColor(fallingBlock.getFallingColors()[i]));
            }
        }

        for (Particle p : particles) {
            shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, p.life / p.initialLife);
            shapeRenderer.circle(p.x, p.y, p.size);
        }

        for (MatchMarker m : matchMarkers) {
            float x = gridOffsetX + m.col * CELL_SIZE;
            float y = gridOffsetY + m.row * CELL_SIZE;
            float alpha = 1f - m.timer / GameConstants.MATCH_PROCESS_DELAY;
            float pulse = 0.5f + 0.5f * MathUtils.sin(alpha * MathUtils.PI * 2);

            Color glowTarget = new Color(1f, 0.85f, 0.6f, 1f);
            Color glowColor = m.color.cpy().lerp(glowTarget, pulse);
            glowColor.a = 0.7f + 0.3f * pulse;

            shapeRenderer.setColor(glowColor);
            shapeRenderer.rect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
        }

        // ТУК ПРЕМЕСТИХ КОДА ЗА РИСУВАНЕ НА "NEXT:" БЛОКА
        float previewX = gridOffsetX + GameConstants.COLS * CELL_SIZE + 40;
        float nextBlockY = gridOffsetY + (GameConstants.ROWS - 2) * CELL_SIZE;
        for (int i = 0; i < 3; i++) {
            float y = nextBlockY - i * CELL_SIZE;
            draw3DBlock(shapeRenderer, previewX, y, CELL_SIZE, ColorMapper.getColor(fallingBlock.getNextColors()[i]));
        }

        shapeRenderer.end(); // <- Единствен end за filled форми


        batch.begin();
        // Текстът "Next:" вече е тук, както преди.
        String nextText = "Next:";
        GlyphLayout layout = new GlyphLayout(font, nextText);
        float textX = previewX + CELL_SIZE / 2f - layout.width / 2f;
        float topRowY = gridOffsetY + (GameConstants.ROWS - 1) * CELL_SIZE + CELL_SIZE / 2f;
        float textY = topRowY + layout.height / 2f;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, nextText, textX + 1, textY - 1);
        font.setColor(Color.ORANGE);
        font.draw(batch, nextText, textX, textY);

        // ... останалият код за текст (score, speed, level)

        String scoreText = "Score: " + score;
        GlyphLayout scoreLayout = new GlyphLayout(font, scoreText);
        float scoreX = gridOffsetX - scoreLayout.width - 40;
        float scoreY = topRowY + scoreLayout.height / 2f;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, scoreText, scoreX + 1, scoreY - 1);
        font.setColor(Color.ORANGE);
        font.draw(batch, scoreText, scoreX, scoreY);

        // currentDropInterval вече е параметър, не го декларирайте отново.
        String speedText = String.format("Speed: %.2f s", currentDropInterval);
        GlyphLayout speedLayout = new GlyphLayout(font, speedText);
        float speedX = scoreX + scoreLayout.width - speedLayout.width;
        float speedY = scoreY - scoreLayout.height - 10;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, speedText, speedX + 1, speedY - 1);
        font.setColor(Color.ORANGE);
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
            String levelText = "LEVEL UP!";
            float alpha = Math.min(1f, levelUpTimer);
            float scale = 1f + 0.3f * (float)Math.sin((2f - levelUpTimer) * Math.PI);

            font.getData().setScale(scale);
            GlyphLayout levelUpLayout = new GlyphLayout(font, levelText);

            float gridCenterX = gridOffsetX + (GameConstants.COLS * CELL_SIZE) / 2f;
            float gridCenterY = gridOffsetY + (GameConstants.ROWS * CELL_SIZE) / 2f;

            float levelTextX = gridCenterX - levelUpLayout.width / 2f;
            float levelTextY = gridCenterY + levelUpLayout.height / 2f;

            font.setColor(1f, 0.8f, 0.2f, alpha);
            font.draw(batch, levelText, levelTextX, levelTextY);

            font.getData().setScale(1f);
        }
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line); // <- Нов begin за line форми
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 1);
        for (int row = 0; row <= GameConstants.ROWS; row++) {
            shapeRenderer.line(gridOffsetX, gridOffsetY + row * CELL_SIZE, gridOffsetX + GameConstants.COLS * CELL_SIZE, gridOffsetY + row * CELL_SIZE);
        }
        for (int col = 0; col <= GameConstants.COLS; col++) {
            shapeRenderer.line(gridOffsetX + col * CELL_SIZE, gridOffsetY, gridOffsetX + col * CELL_SIZE, gridOffsetY + GameConstants.ROWS * CELL_SIZE);
        }
        shapeRenderer.end(); // <- Нов end за line форми
    }

    private static void draw3DBlock(ShapeRenderer shapeRenderer, float x, float y, int CELL_SIZE, Color baseColor) {
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
}
