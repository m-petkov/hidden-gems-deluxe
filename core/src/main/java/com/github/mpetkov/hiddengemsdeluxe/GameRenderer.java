package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;

public class GameRenderer {

    // 🔹 За анимацията на неоновия контур
    private static float neonTime = 0f;

    // 💡 Текстура за блоковете
    private static Texture blockTexture;

    private GameRenderer() {
        // Приватен конструктор за статичен клас
    }

    // 💡 МЕТОД: Зареждане на ресурса
    public static void initialize() {
        try {
            if (blockTexture == null) {
                // Търси block.png директно в assets/
                blockTexture = new Texture(Gdx.files.internal("block.png"));
                Gdx.app.log("GameRenderer", "block.png заредена успешно.");
            }
        } catch (Exception e) {
            Gdx.app.error("GameRenderer", "Грешка при зареждане на block.png. Уверете се, че файлът е в 'assets/': " + e.getMessage());
        }
    }

    // 💡 МЕТОД: Освобождаване на ресурса
    public static void dispose() {
        if (blockTexture != null) {
            blockTexture.dispose();
        }
    }

    // 💡 АКТУАЛИЗИРАН МЕТОД: Рисува блока с текстура, прилагайки цветен филтър (tint)
    // Този метод отново се казва drawBlock, както в началото.
    private static void drawBlock(SpriteBatch batch, float x, float y, int CELL_SIZE, Color baseColor) {
        if (blockTexture == null) return;

        // Прилагаме филтър (tint) с базовия цвят
        batch.setColor(baseColor);

        // Рисуваме текстурата
        batch.draw(blockTexture, x, y, CELL_SIZE, CELL_SIZE);

        // ВАЖНО: Връщаме цвета на batch към бяло
        batch.setColor(Color.WHITE);
    }

    public static void renderGame(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
                                  int gridOffsetX, int gridOffsetY, int CELL_SIZE,
                                  int[][] grid, FallingBlock fallingBlock,
                                  List<Particle> particles, List<MatchMarker> matchMarkers,
                                  int score, int level, float currentDropInterval,
                                  float levelUpTimer, boolean isGameOver, float gameOverTimer) {

        // === Обновяване на неоновите настройки ===
        neonTime += Gdx.graphics.getDeltaTime() * 1.5f;

        float hueCenter = 0.65f;
        float hueAmplitude = 0.3f;
        float newHue = (hueCenter + hueAmplitude * MathUtils.sin(neonTime * 0.5f)) % 1f;
        if (newHue < 0) newHue += 1f;

        Color neonBaseColor = hsvToColor(newHue, 1.0f, 1.0f);

        // ----------------------------------------------------------------------------------
        // === I. ShapeRenderer (За фон, частици, маркери) ===
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Рисуване на фон на мрежата
        for (int row = 0; row < GameConstants.ROWS; row++) {
            for (int col = 0; col < GameConstants.COLS; col++) {
                shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 1);
                shapeRenderer.rect(gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // **ПРЕМАХНАТО:** Тук преди бяха извиквани drawBlockBackground методите.

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

        shapeRenderer.end();

        // ----------------------------------------------------------------------------------
        // === II. SpriteBatch (За БЛОКОВЕ и ТЕКСТ) ===
        batch.begin();

        // **АКТУАЛИЗИРАНО:** Извикваме drawBlock (с текстурата и tint)
        // Рисуване на блоковете в мрежата
        for (int row = 0; row < GameConstants.ROWS; row++) {
            for (int col = 0; col < GameConstants.COLS; col++) {
                int colorCode = grid[row][col];
                if (colorCode != -1) {
                    drawBlock(batch, gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE,
                        CELL_SIZE, ColorMapper.getColor(colorCode));
                }
            }
        }

        // **АКТУАЛИЗИРАНО:** Рисуване на падащия блок
        for (int i = 0; i < 3; i++) {
            if (fallingBlock.getFallingRow() - i >= 0) {
                drawBlock(batch, gridOffsetX + fallingBlock.getFallingCol() * CELL_SIZE,
                    gridOffsetY + (fallingBlock.getFallingRow() - i) * CELL_SIZE,
                    CELL_SIZE, ColorMapper.getColor(fallingBlock.getFallingColors()[i]));
            }
        }

        // **АКТУАЛИЗИРАНО:** "Next:" блок
        float previewX = gridOffsetX + GameConstants.COLS * CELL_SIZE + 40;
        float nextBlockY = gridOffsetY + (GameConstants.ROWS - 2) * CELL_SIZE;
        for (int i = 0; i < 3; i++) {
            float y = nextBlockY - i * CELL_SIZE;
            drawBlock(batch, previewX, y, CELL_SIZE, ColorMapper.getColor(fallingBlock.getNextColors()[i]));
        }


        // === Текстова част ===
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

        // ----------------------------------------------------------------------------------
        // === III. ShapeRenderer (Линии на мрежата и Неонова рамка) ===

        // Рисуване на линии на мрежата (ТЪНКИ ЛИНИИ) - Line ShapeType
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

        // === БЛОК ЗА НЕОНОВА РАМКА ===
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float borderThickness = 8f;
        float gridWidth = GameConstants.COLS * CELL_SIZE;
        float gridHeight = GameConstants.ROWS * CELL_SIZE;

        float minX = gridOffsetX;
        float minY = gridOffsetY;
        float maxX = gridOffsetX + gridWidth;
        float maxY = gridOffsetY + gridHeight;

        final int SEGMENTS_PER_SIDE = 40;
        final int TOTAL_SEGMENTS = 4 * SEGMENTS_PER_SIDE;

        float phaseStep = 1f / TOTAL_SEGMENTS;
        float totalPhaseOffset = neonTime * 0.5f;

        float dx = gridWidth / SEGMENTS_PER_SIDE;
        float dy = gridHeight / SEGMENTS_PER_SIDE;


        for (int i = 0; i < TOTAL_SEGMENTS; i++) {

            float phase = (totalPhaseOffset + i * phaseStep) % 1f;
            Color segColor = calculateColor(phase, neonBaseColor);
            shapeRenderer.setColor(segColor);

            int sectionIndex = i / SEGMENTS_PER_SIDE;
            int j = i % SEGMENTS_PER_SIDE;

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

            shapeRenderer.rect(x, y, width, height);
        }

        shapeRenderer.end();
        // ----------------------------------------------------------------------------------
    }

    // 🔹 ПОМОЩЕН МЕТОД за изчисляване на цвета на база фазата
    private static Color calculateColor(float phase, Color neonBaseColor) {
        float intensity = 0.4f + 0.6f * MathUtils.sin(phase * MathUtils.PI * 2);
        return neonBaseColor.cpy().lerp(Color.WHITE, 0.4f + 0.3f * intensity).mul(intensity);
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
