package com.github.mpetkov.hiddengemsdeluxe.util;


// Няма декларация за пакет

import com.badlogic.gdx.graphics.Color;

public class ColorMapper {
    private ColorMapper() {
        // Приватен конструктор, за да предотврати инстанциране
    }

    public static Color getColor(int index) {
        switch (index) {
            case 0: return Color.RED;
            case 1: return Color.BLUE;
            case 2: return Color.GREEN;
            case 3: return Color.YELLOW;
            default: return Color.WHITE;
        }
    }
}