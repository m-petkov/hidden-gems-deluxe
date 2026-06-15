package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.github.mpetkov.hiddengemsdeluxe.screen.GameScreen;
import com.github.mpetkov.hiddengemsdeluxe.screen.WelcomeScreen;

public class GameApp extends Game {

    public GameScreen currentGame;
    public boolean isPaused = false;

    private int lastWidth = -1;
    private int lastHeight = -1;

    @Override
    public void create() {
        lastWidth = Gdx.graphics.getWidth();
        lastHeight = Gdx.graphics.getHeight();
        setScreen(new WelcomeScreen(this));
    }

    @Override
    public void resize(int width, int height) {
        lastWidth = width;
        lastHeight = height;
        super.resize(width, height);
    }

    @Override
    public void render() {
        int width = Gdx.graphics.getWidth();
        int height = Gdx.graphics.getHeight();
        if (getScreen() != null && (width != lastWidth || height != lastHeight)) {
            lastWidth = width;
            lastHeight = height;
            getScreen().resize(width, height);
        }
        super.render();
    }

    public void startNewGame() {
        isPaused = false;
        currentGame = new GameScreen(this);
        setScreen(currentGame);
    }

    public void pauseGame() {
        isPaused = true;
        setScreen(new WelcomeScreen(this));
    }

    public void resumeGame() {
        if (currentGame != null) {
            isPaused = false;
            setScreen(currentGame);
        }
    }
}
