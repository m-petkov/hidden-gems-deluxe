package com.github.mpetkov.hiddengemsdeluxe.util;

import com.badlogic.gdx.graphics.Color;

public class ColorMapper {
    private ColorMapper() {
        // Приватен конструктор, за да предотврати инстанциране
    }

    /** Брой основни цветове (без розов). Розовият се добавя от ниво 12. */
    public static final int BASE_COLOR_COUNT = 4;
    /** Общ брой цветове при ниво >= 12 (с розов). */
    public static final int FULL_COLOR_COUNT = 5;

    /** Светлорозов цвят за камъните от ниво 12. */
    public static final Color PINK = new Color(1f, 0.75f, 0.85f, 1f);

    public static Color getColor(int index) {
        switch (index) {
            case 0: return Color.RED;
            case 1: return Color.BLUE;
            case 2: return Color.GREEN;
            case 3: return Color.YELLOW;
            case 4: return PINK;
            default: return Color.WHITE;
        }
    }
}
