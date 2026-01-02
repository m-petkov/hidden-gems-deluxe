package com.github.mpetkov.hiddengemsdeluxe;

import com.github.mpetkov.hiddengemsdeluxe.screen.GameScreen;
import com.github.mpetkov.hiddengemsdeluxe.screen.WelcomeScreen;


import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;

public class GameApp extends Game {
    public boolean isPaused = false;
    public Screen pausedGameScreen = null;

    public void pauseGame(Screen currentScreen) {
        isPaused = true;
        pausedGameScreen = currentScreen;
        setScreen(new WelcomeScreen(this));
    }

    public void resumeGame() {
        if (isPaused && pausedGameScreen != null) {
            setScreen(pausedGameScreen);
            isPaused = false;
            pausedGameScreen = null;
        }
    }

    @Override
    public void create() {
        setScreen(new WelcomeScreen(this));
    }
}