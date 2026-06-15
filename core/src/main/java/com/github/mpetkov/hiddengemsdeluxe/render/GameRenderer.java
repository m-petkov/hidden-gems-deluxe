package com.github.mpetkov.hiddengemsdeluxe.render;

import com.github.mpetkov.hiddengemsdeluxe.model.FallingBlock;
import com.github.mpetkov.hiddengemsdeluxe.model.MatchMarker;
import com.github.mpetkov.hiddengemsdeluxe.model.Particle;
import com.github.mpetkov.hiddengemsdeluxe.util.ColorMapper;
import com.github.mpetkov.hiddengemsdeluxe.util.GameConstants;


import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.List;

public class GameRenderer {

    public static final String GITHUB_URL = "https://github.com/m-petkov";
    public static final String GITHUB_LABEL = "github.com/m-petkov";
    private static final float GITHUB_LINK_SCALE = 0.72f;
    private static final float GITHUB_LINK_GAP = 10f;

    private static float gitHubLinkX;
    private static float gitHubLinkY;
    private static float gitHubLinkW;
    private static float gitHubLinkH;
    private static boolean gitHubLinkVisible;

    // Toggle to enable the 3D gem rendering instead of the flat block texture.
    private static final boolean USE_3D_GEMS = true;
    // Runtime flag that falls back to 2D if 3D init fails or the platform cannot handle it (e.g. TeaVM/WebGL).
    private static boolean use3DGemsRuntime = false;

    private static boolean is3DGemsSupported() {
        return Gdx.app.getType() != Application.ApplicationType.WebGL;
    }

    public static boolean uses3DGems() {
        return use3DGemsRuntime;
    }

    public static boolean isGitHubLinkVisible() {
        return gitHubLinkVisible;
    }

    public static boolean containsGitHubLink(float worldX, float worldY) {
        return gitHubLinkVisible
            && worldX >= gitHubLinkX && worldX <= gitHubLinkX + gitHubLinkW
            && worldY >= gitHubLinkY && worldY <= gitHubLinkY + gitHubLinkH;
    }

    private static void drawGitHubLink(BitmapFont font, SpriteBatch batch, float statsRightX, float levelY, float levelTextHeight) {
        float scaleX = font.getData().scaleX;
        float scaleY = font.getData().scaleY;
        font.getData().setScale(scaleX * GITHUB_LINK_SCALE, scaleY * GITHUB_LINK_SCALE);

        GlyphLayout linkLayout = new GlyphLayout(font, GITHUB_LABEL);
        float linkX = statsRightX - linkLayout.width;
        float linkY = levelY - levelTextHeight - GITHUB_LINK_GAP;

        font.setColor(0f, 0f, 0f, 0.45f);
        font.draw(batch, GITHUB_LABEL, linkX + 1, linkY - 1);
        font.setColor(Color.CYAN);
        font.draw(batch, GITHUB_LABEL, linkX, linkY);

        gitHubLinkX = linkX;
        gitHubLinkY = linkY - linkLayout.height;
        gitHubLinkW = linkLayout.width;
        gitHubLinkH = linkLayout.height;
        gitHubLinkVisible = true;

        font.getData().setScale(scaleX, scaleY);
    }

    // 🔹 За анимацията на неоновия контур
    private static float neonTime = 0f;
    
    // 💎 За анимацията на 3D ефектите на камъните
    private static float gemAnimationTime = 0f;

    // 💡 Текстура за блоковете
    private static Texture blockTexture;

    private GameRenderer() {
        // Приватен конструктор за статичен клас
    }

    // 💡 МЕТОД: Зареждане на ресурса
    public static void initialize() {
        try {
            if (blockTexture == null) {
                blockTexture = new Texture(Gdx.files.internal("block.png"));
            }
        } catch (Exception e) {
            Gdx.app.error("GameRenderer", "Грешка при зареждане на block.png. Уверете се, че файлът е в 'assets/': " + e.getMessage());
        }

        // Инициализация на 3D рендера за камъните (отделно, за да не скриваме грешки)
        use3DGemsRuntime = false;
        if (USE_3D_GEMS && is3DGemsSupported()) {
            Gem3DRenderer.initialize();
            if (Gem3DRenderer.isInitialized()) {
                use3DGemsRuntime = true;
            } else {
                Gdx.app.log("GameRenderer", "3D gems not initialized, falling back to 2D sprites.");
            }
        } else if (USE_3D_GEMS) {
            Gdx.app.log("GameRenderer", "3D gems disabled on WebGL, using 2D sprites.");
        }
    }

    // 💡 МЕТОД: Освобождаване на ресурса
    public static void dispose() {
        if (blockTexture != null) {
            blockTexture.dispose();
            blockTexture = null;
        }
        if (use3DGemsRuntime) {
            Gem3DRenderer.dispose();
        }
    }

    // 💎 Рисува блока с текстура и 3D ефекти (запазва оригиналния дизайн на block.png)
    private static void drawBlock(SpriteBatch batch, float x, float y, int CELL_SIZE, Color baseColor, int row, int col, boolean isFalling) {
        if (use3DGemsRuntime) {
            // Уникално време за анимация базирано на позицията
            float uniqueTime = gemAnimationTime + (row * 0.3f) + (col * 0.2f);

            // Леко пулсиране на скала за "жив" ефект
            float pulseScale = 1f + 0.04f * MathUtils.sin(uniqueTime * 2f);
            float size = CELL_SIZE * pulseScale;

            // Центърът на клетката
            float centerX = x + CELL_SIZE / 2f;
            float centerY = y + CELL_SIZE / 2f;

            Gem3DRenderer.addGem(centerX, centerY, size, baseColor);
        } else {
            if (blockTexture == null) return;

            // Уникално време за анимация базирано на позицията
            float uniqueTime = gemAnimationTime + (row * 0.3f) + (col * 0.2f);

            // Пулсиращ ефект (много субтилен)
            float pulseScale = 1f + 0.03f * MathUtils.sin(uniqueTime * 2f);
            float size = CELL_SIZE * pulseScale;
            float offsetX = (CELL_SIZE - size) / 2f;
            float offsetY = (CELL_SIZE - size) / 2f;

            float gemX = x + offsetX;
            float gemY = y + offsetY;

            // === Основна текстура с цветен филтър ===
            batch.setColor(baseColor);
            batch.draw(blockTexture, gemX, gemY, size, size);
            batch.setColor(Color.WHITE);
        }
    }

    public static void renderGame(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont font,
                                  int gridOffsetX, int gridOffsetY, int CELL_SIZE,
                                  int[][] grid, FallingBlock fallingBlock,
                                  List<Particle> particles, List<MatchMarker> matchMarkers,
                                  int score, int level, float currentDropInterval,
                                  float levelUpTimer, boolean isGameOver, float gameOverTimer,
                                  float visualFallingY) {

        // === Обновяване на неоновите настройки ===
        neonTime += Gdx.graphics.getDeltaTime() * 1.5f;
        
        // === Обновяване на анимацията на камъните ===
        gemAnimationTime += Gdx.graphics.getDeltaTime() * 2f;

        float hueCenter = 0.65f;
        float hueAmplitude = 0.3f;
        float newHue = (hueCenter + hueAmplitude * MathUtils.sin(neonTime * 0.5f)) % 1f;
        if (newHue < 0) newHue += 1f;

        Color neonBaseColor = hsvToColor(newHue, 1.0f, 1.0f);

        // ----------------------------------------------------------------------------------
        // === I. ShapeRenderer (За фон, частици, маркери) ===
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Полупрозрачна дъска – достатъчно видима, за да не се слива с фона (без разсейване)
        for (int row = 0; row < GameConstants.ROWS; row++) {
            for (int col = 0; col < GameConstants.COLS; col++) {
                shapeRenderer.setColor(0.1f, 0.08f, 0.16f, 0.65f);
                shapeRenderer.rect(gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
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

        shapeRenderer.end();

        // ----------------------------------------------------------------------------------
        // === II. Рисуване на блоковете / 3D камъните ===
        if (!use3DGemsRuntime) {
            // 2D текстурирани блокове
            batch.begin();

            for (int row = 0; row < GameConstants.ROWS; row++) {
                for (int col = 0; col < GameConstants.COLS; col++) {
                    int colorCode = grid[row][col];
                    if (colorCode != -1) {
                        drawBlock(batch, gridOffsetX + col * CELL_SIZE, gridOffsetY + row * CELL_SIZE,
                            CELL_SIZE, ColorMapper.getColor(colorCode), row, col, false);
                    }
                }
            }

            for (int i = 0; i < 3; i++) {
                float visualY = visualFallingY - i;
                if (visualY >= 0) {
                    drawBlock(batch, gridOffsetX + fallingBlock.getFallingCol() * CELL_SIZE,
                        gridOffsetY + visualFallingY * CELL_SIZE - i * CELL_SIZE,
                        CELL_SIZE, ColorMapper.getColor(fallingBlock.getFallingColors()[i]), (int)visualY, fallingBlock.getFallingCol(), true);
                }
            }
        } else {
            // Когато използваме 3D камъни, тук само подаваме координатите към Gem3DRenderer чрез drawBlock().
            for (int row = 0; row < GameConstants.ROWS; row++) {
                for (int col = 0; col < GameConstants.COLS; col++) {
                    int colorCode = grid[row][col];
                    if (colorCode != -1) {
                        float x = gridOffsetX + col * CELL_SIZE;
                        float y = gridOffsetY + row * CELL_SIZE;
                        drawBlock(null, x, y, CELL_SIZE, ColorMapper.getColor(colorCode), row, col, false);
                    }
                }
            }

            for (int i = 0; i < 3; i++) {
                float visualY = visualFallingY - i;
                if (visualY >= 0) {
                    float x = gridOffsetX + fallingBlock.getFallingCol() * CELL_SIZE;
                    float y = gridOffsetY + visualY * CELL_SIZE;
                    drawBlock(null, x, y, CELL_SIZE, ColorMapper.getColor(fallingBlock.getFallingColors()[i]),
                        (int) visualY, fallingBlock.getFallingCol(), true);
                }
            }
        }

        // "Next:" блок (и при 2D, и при 3D – само позициите се подават)
        float previewX = gridOffsetX + GameConstants.COLS * CELL_SIZE + 40;
        float nextBlockY = gridOffsetY + (GameConstants.ROWS - 2) * CELL_SIZE;
        for (int i = 0; i < 3; i++) {
            float y = nextBlockY - i * CELL_SIZE;
            drawBlock(batch, previewX, y, CELL_SIZE, ColorMapper.getColor(fallingBlock.getNextColors()[i]), 
                GameConstants.ROWS + i, GameConstants.COLS + 1, false);
        }

        if (!use3DGemsRuntime) {
            batch.end();
        }

        // === III. ShapeRenderer (Линии на мрежата и Неонова рамка) ===
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.22f, 0.18f, 0.32f, 0.72f);
        for (int row = 0; row <= GameConstants.ROWS; row++) {
            shapeRenderer.line(gridOffsetX, gridOffsetY + row * CELL_SIZE, gridOffsetX + GameConstants.COLS * CELL_SIZE, gridOffsetY + row * CELL_SIZE);
        }
        for (int col = 0; col <= GameConstants.COLS; col++) {
            shapeRenderer.line(gridOffsetX + col * CELL_SIZE, gridOffsetY, gridOffsetX + col * CELL_SIZE, gridOffsetY + GameConstants.ROWS * CELL_SIZE);
        }
        shapeRenderer.end();

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
                x = minX + j * dx;
                y = maxY - borderThickness;
                width = dx;
                height = borderThickness;
            } else if (sectionIndex == 1) {
                x = maxX - borderThickness;
                y = maxY - (j + 1) * dy;
                width = borderThickness;
                height = dy;
            } else if (sectionIndex == 2) {
                x = minX + (SEGMENTS_PER_SIDE - 1 - j) * dx;
                y = minY;
                width = dx;
                height = borderThickness;
            } else {
                x = minX;
                y = minY + j * dy;
                width = borderThickness;
                height = dy;
            }

            shapeRenderer.rect(x, y, width, height);
        }

        shapeRenderer.end();
    }

    public static void renderHud(SpriteBatch batch, BitmapFont font,
                                   int gridOffsetX, int gridOffsetY, int CELL_SIZE,
                                   int score, int level, float currentDropInterval) {
        float previewX = gridOffsetX + GameConstants.COLS * CELL_SIZE + 40;
        float topRowY = gridOffsetY + (GameConstants.ROWS - 1) * CELL_SIZE + CELL_SIZE / 2f;

        gitHubLinkVisible = false;
        batch.begin();

        final Color LIME_COLOR = Color.LIME;

        String nextText = "Next:";
        GlyphLayout layout = new GlyphLayout(font, nextText);
        float textX = previewX + CELL_SIZE / 2f - layout.width / 2f;
        float textY = topRowY + layout.height / 2f;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, nextText, textX + 1, textY - 1);
        font.setColor(LIME_COLOR);
        font.draw(batch, nextText, textX, textY);

        String scoreText = "Score: " + score;
        GlyphLayout scoreLayout = new GlyphLayout(font, scoreText);
        float scoreX = gridOffsetX - scoreLayout.width - 40;
        float scoreY = topRowY + scoreLayout.height / 2f;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, scoreText, scoreX + 1, scoreY - 1);
        font.setColor(LIME_COLOR);
        font.draw(batch, scoreText, scoreX, scoreY);

        String speedText = String.format("Speed: %.2f s", currentDropInterval);
        GlyphLayout speedLayout = new GlyphLayout(font, speedText);
        float speedX = scoreX + scoreLayout.width - speedLayout.width;
        float speedY = scoreY - scoreLayout.height - 10;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, speedText, speedX + 1, speedY - 1);
        font.setColor(LIME_COLOR);
        font.draw(batch, speedText, speedX, speedY);

        String levelDisplayText = "Level: " + level;
        GlyphLayout levelLayout = new GlyphLayout(font, levelDisplayText);
        float levelY = speedY - speedLayout.height - 10;

        font.setColor(0, 0, 0, 0.5f);
        font.draw(batch, levelDisplayText, speedX + speedLayout.width - levelLayout.width + 1, levelY - 1);
        font.setColor(LIME_COLOR);
        font.draw(batch, levelDisplayText, speedX + speedLayout.width - levelLayout.width, levelY);

        drawGitHubLink(font, batch, scoreX + scoreLayout.width, levelY, levelLayout.height);

        batch.end();
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
