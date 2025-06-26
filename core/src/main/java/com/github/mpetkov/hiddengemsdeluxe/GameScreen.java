package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Timer;
import java.util.Random;

public class GameScreen implements Screen {

    private final Main game;
    private ShapeRenderer shapeRenderer;

    private static final int ROWS = 20;
    private static final int COLS = 10;
    private static final int CELL_SIZE = 32;

    private int fallingRow = 0;
    private int fallingCol = 4; // Центрирано
    private int fallingColor;
    private Random random;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        random = new Random();
        fallingColor = random.nextInt(5);

        // Таймер: местим надолу всяка 0.5 сек
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (fallingRow < ROWS - 1) {
                    fallingRow++;
                } else {
                    fallingRow = 0;
                    fallingColor = random.nextInt(5);
                }
            }
        }, 0.5f, 0.5f); // delay, interval
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Рисуване на мрежата
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                shapeRenderer.setColor(0.2f, 0.2f, 0.25f, 1);
                shapeRenderer.rect(col * CELL_SIZE, row * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);
            }
        }

        // Рисуване на падащия камък
        shapeRenderer.setColor(getColor(fallingColor));
        shapeRenderer.rect(fallingCol * CELL_SIZE, fallingRow * CELL_SIZE, CELL_SIZE - 1, CELL_SIZE - 1);

        shapeRenderer.end();
    }

    private com.badlogic.gdx.graphics.Color getColor(int index) {
        switch (index) {
            case 0: return com.badlogic.gdx.graphics.Color.RED;
            case 1: return com.badlogic.gdx.graphics.Color.BLUE;
            case 2: return com.badlogic.gdx.graphics.Color.GREEN;
            case 3: return com.badlogic.gdx.graphics.Color.YELLOW;
            case 4: return com.badlogic.gdx.graphics.Color.PURPLE;
            default: return com.badlogic.gdx.graphics.Color.WHITE;
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        shapeRenderer.dispose();
    }
}
