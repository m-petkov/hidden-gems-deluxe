package com.github.mpetkov.hiddengemsdeluxe.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class SaveManager {

    private static final String PREFS = "hidden_gems_save";
    private static final String HIGH_SCORE = "high_score";
    private static final String HIGH_LEVEL = "high_level";

    private static Preferences prefs() {
        return Gdx.app.getPreferences(PREFS);
    }

    public static int getHighScore() {
        return prefs().getInteger(HIGH_SCORE, 0);
    }

    public static int getHighLevel() {
        return prefs().getInteger(HIGH_LEVEL, 1);
    }

    public static void saveScore(int score, int level) {
        boolean changed = false;

        if (score > getHighScore()) {
            prefs().putInteger(HIGH_SCORE, score);
            changed = true;
        }

        if (level > getHighLevel()) {
            prefs().putInteger(HIGH_LEVEL, level);
            changed = true;
        }

        if (changed) {
            prefs().flush();
        }
    }
}
