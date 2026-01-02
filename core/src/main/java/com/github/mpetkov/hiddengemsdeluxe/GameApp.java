package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Game;
import com.github.mpetkov.hiddengemsdeluxe.screen.GameScreen;
import com.github.mpetkov.hiddengemsdeluxe.screen.WelcomeScreen;

public class GameApp extends Game {

    public GameScreen currentGame;
    public boolean isPaused = false;

    @Override
    public void create() {
        setScreen(new WelcomeScreen(this));
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
