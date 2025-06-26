package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Game;
import com.github.mpetkov.hiddengemsdeluxe.WelcomeScreen;

public class Main extends Game {

    @Override
    public void create() {
        setScreen(new WelcomeScreen(this));
    }
}
