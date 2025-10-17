package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;

public class GameRenderer {

    // 🔹 За анимацията на неоновия контур
    private static float neonTime = 0f;

    private GameRenderer() {
        // Приватен конструктор за статичен клас
    }

    public static void renderGame(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
                                  int gridOffsetX, int gridOffsetY, int CELL_SIZE,
                                  int[][] grid, FallingBlock fallingBlock,
                                  List<Particle> particles, List<MatchMarker> matchMarkers,
                                  int score, int level, float currentDropInterval,
                                  float levelUpTimer, boolean isGameOver, float gameOverTimer) {

        // === Обновяване на неоновите настройки ===
        neonTime += Gdx.graphics.getDeltaTime() * 1.5f; // Скорост на въртене
        float pulse = 0.5f + 0.5f * MathUtils.sin(neonTime * 2f); // пулсация на яркостта

        // Променяме оттенъка за по-ярък, класически неонов ефект (Синьо-Лилаво-Розово)
        // Hue се движи между 0.5 (Cyan/Blue), 0.8 (Purple/Magenta), 0.0 (Pink/Red)
        float hueCenter = 0.65f; // Център на Синьо-Лилаво
        float hueAmplitude = 0.3f; // Голяма амплитуда, за да обхване Розово

        // Бавната промяна на Hue за преливане между базовите цветове
        float newHue = (hueCenter + hueAmplitude * MathUtils.sin(neonTime * 0.5f)) % 1f;
        if (newHue < 0) newHue += 1f;

        // Hue Shift Base все още контролира въртящата се светлинна точка
        float hueShiftBase = (MathUtils.sin(neonTime * 1.5f) + 1f) * 0.08f;
        Color neonBaseColor = hsvToColor(newHue, 1.0f, 1.0f); // S=1.0, V=1.0 за максимална яркост

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Рисуване на фон на мрежата
        for (int row = 0; row < GameConstants.ROWS; row++) {
            for (int col = 0; col < GameConstants.COLS; col++) {
                shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 1);
                shapeRenderer.rect(gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // Рисуване на блоковете в мрежата
        for (int row = 0; row < GameConstants.ROWS; row++) {
            for (int col = 0; col < GameConstants.COLS; col++) {
                int color = grid[row][col];
                if (color != -1) {
                    draw3DBlock(shapeRenderer, gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, CELL_SIZE, ColorMapper.getColor(color));
                }
            }
        }

        // Рисуване на падащия блок
        for (int i = 0; i < 3; i++) {
            if (fallingBlock.getFallingRow() - i >= 0) {
                draw3DBlock(shapeRenderer, gridOffsetX + fallingBlock.getFallingCol() * CELL_SIZE,
                    gridOffsetY + (fallingBlock.getFallingRow() - i) * CELL_SIZE,
                    CELL_SIZE, ColorMapper.getColor(fallingBlock.getFallingColors()[i]));
            }
        }

        // Рисуване на частиците
        for (Particle p : particles) {
            shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, p.life / p.initialLife);
            shapeRenderer.circle(p.x, p.y, p.size);
        }

        // Рисуване на маркерите за съвпадение
        for (MatchMarker m : matchMarkers) {
            float x = gridOffsetX + m.col * CELL_SIZE;
            float y = gridOffsetY + m.row * CELL_SIZE;
            float alpha = 1f - m.timer / GameConstants.MATCH_PROCESS_DELAY;
            float pulseM = 0.5f + 0.5f * MathUtils.sin(alpha * MathUtils.PI * 2);

            Color glowTarget = new Color(1f, 0.85f, 0.6f, 1f);
            Color glowColor = m.color.cpy().lerp(glowTarget, pulseM);
            glowColor.a = 0.7f + 0.3f * pulseM;

            shapeRenderer.setColor(glowColor);
            shapeRenderer.rect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
        }

        // "Next:" блок
        float previewX = gridOffsetX + GameConstants.COLS * CELL_SIZE + 40;
        float nextBlockY = gridOffsetY + (GameConstants.ROWS - 2) * CELL_SIZE;
        for (int i = 0; i < 3; i++) {
            float y = nextBlockY - i * CELL_SIZE;
            draw3DBlock(shapeRenderer, previewX, y, CELL_SIZE, ColorMapper.getColor(fallingBlock.getNextColors()[i]));
        }

        shapeRenderer.end();

        // === Текстова част ===
        batch.begin();
        String nextText = "Next:";
        GlyphLayout layout = new GlyphLayout(font, nextText);
        float textX = previewX + CELL_SIZE / 2f - layout.width / 2f;
        float topRowY = gridOffsetY + (GameConstants.ROWS - 1) * CELL_SIZE + CELL_SIZE / 2f;
        float textY = topRowY + layout.height / 2f;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, nextText, textX + 1, textY - 1);
        font.setColor(Color.ORANGE);
        font.draw(batch, nextText, textX, textY);

        String scoreText = "Score: " + score;
        GlyphLayout scoreLayout = new GlyphLayout(font, scoreText);
        float scoreX = gridOffsetX - scoreLayout.width - 40;
        float scoreY = topRowY + scoreLayout.height / 2f;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, scoreText, scoreX + 1, scoreY - 1);
        font.setColor(Color.ORANGE);
        font.draw(batch, scoreText, scoreX, scoreY);

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
        float levelY = speedY - levelLayout.height - 10;

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

        // === Рисуване на линии на мрежата (ТЪНКИ ЛИНИИ) ===
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 1);
        for (int row = 0; row <= GameConstants.ROWS; row++) {
            shapeRenderer.line(gridOffsetX, gridOffsetY + row * CELL_SIZE, gridOffsetX + GameConstants.COLS * CELL_SIZE, gridOffsetY + row * CELL_SIZE);
        }
        for (int col = 0; col <= GameConstants.COLS; col++) {
            shapeRenderer.line(gridOffsetX + col * CELL_SIZE, gridOffsetY, gridOffsetX + col * CELL_SIZE, gridOffsetY + GameConstants.ROWS * CELL_SIZE);
        }
        shapeRenderer.end();

        // ----------------------------------------------------------------------------------

        // === БЛОК ЗА НЕОНОВА РАМКА: СТРОГО ПРАВОЪГЪЛЕН ШНУР С RGB ПРЕЛИВАНЕ ===
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float borderThickness = 8f;
        float halfThickness = borderThickness / 2f;
        float gridWidth = GameConstants.COLS * CELL_SIZE;
        float gridHeight = GameConstants.ROWS * CELL_SIZE;

        float minX = gridOffsetX;
        float minY = gridOffsetY;
        float maxX = gridOffsetX + gridWidth;
        float maxY = gridOffsetY + gridHeight;

        // Брой сегменти
        final int SEGMENTS_PER_SIDE = 40;
        final int TOTAL_SEGMENTS = 4 * SEGMENTS_PER_SIDE;

        // 1/160 от пълната фаза
        float phaseStep = 1f / TOTAL_SEGMENTS;
        float totalPhaseOffset = neonTime * 0.5f;

        // Дължините на промяна на координатите за един сегмент (права линия)
        float dx = gridWidth / SEGMENTS_PER_SIDE;
        float dy = gridHeight / SEGMENTS_PER_SIDE;


        // Започваме цикъла, който рисува целия правоъгълен контур
        for (int i = 0; i < TOTAL_SEGMENTS; i++) {

            float phase = (totalPhaseOffset + i * phaseStep) % 1f;
            Color segColor = calculateColor(phase, neonBaseColor);
            shapeRenderer.setColor(segColor);

            int sectionIndex = i / SEGMENTS_PER_SIDE; // 0: Top, 1: Right, 2: Bottom, 3: Left
            int j = i % SEGMENTS_PER_SIDE; // Индекс в рамките на страната

            // 1. ПРАВА ЛИНИЯ (Рисува се като малък правоъгълник)
            float x, y, width, height;

            if (sectionIndex == 0) {
                // TOP (от TL към TR)
                x = minX + j * dx;
                y = maxY - borderThickness;
                width = dx;
                height = borderThickness;
            } else if (sectionIndex == 1) {
                // RIGHT (от TR към BR)
                x = maxX - borderThickness;
                y = maxY - (j + 1) * dy;
                width = borderThickness;
                height = dy;
            } else if (sectionIndex == 2) {
                // BOTTOM (от BR към BL)
                // Движим се надясно, за да съвпадне с посоката на анимацията
                x = minX + (SEGMENTS_PER_SIDE - 1 - j) * dx;
                y = minY;
                width = dx;
                height = borderThickness;
            } else {
                // LEFT (от BL към TL)
                x = minX;
                y = minY + j * dy;
                width = borderThickness;
                height = dy;
            }

            // Рисуваме правоъгълника.
            shapeRenderer.rect(x, y, width, height);
        }

        shapeRenderer.end();
        // ----------------------------------------------------------------------------------
    }

    // 🔹 ПОМОЩЕН МЕТОД за изчисляване на цвета на база фазата
    private static Color calculateColor(float phase, Color neonBaseColor) {
        // Интензитет: синусоида с отместване, за да не е напълно черно
        float intensity = 0.4f + 0.6f * MathUtils.sin(phase * MathUtils.PI * 2);

        // Цвят: Базовият цвят се смесва с бяло за ефект на "гореща точка" и се затъмнява/изсветлява според интензитета
        return neonBaseColor.cpy().lerp(Color.WHITE, 0.4f + 0.3f * intensity).mul(intensity);
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

    // 🔹 Помощен метод за HSV към Color (без java.awt)
    private static Color hsvToColor(float h, float s, float v) {
        float r = 0, g = 0, b = 0;
        int i = (int) Math.floor(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: r = v; g = p; b = q; break;
        }
        return new Color(r, g, b, 1f);
    }
}
