package com.github.mpetkov.hiddengemsdeluxe.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class SaveManager {

    private static final Preferences prefs =
        Gdx.app.getPreferences("HiddenGemsDeluxe");

    public static void saveScore(int score, int level) {
        if (score > getHighScore()) {
            prefs.putInteger("highScore", score);
        }
        if (level > getHighLevel()) {
            prefs.putInteger("highLevel", level);
        }
        prefs.flush();
    }

    public static int getHighScore() {
        return prefs.getInteger("highScore", 0);
    }

    public static int getHighLevel() {
        return prefs.getInteger("highLevel", 1);
    }

    // ✅ НОВО
    public static void reset() {
        prefs.putInteger("highScore", 0);
        prefs.putInteger("highLevel", 1);
        prefs.flush();
    }
}
