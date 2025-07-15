package com.github.mpetkov.hiddengemsdeluxe;// GameConstants.java
// Няма декларация за пакет

public final class GameConstants {
    private GameConstants() {
        // Приватен конструктор, за да предотврати инстанциране
    }

    public static final int ROWS = 12;
    public static final int COLS = 6;

    public static final float MIN_DROP_INTERVAL = 0.05f;
    public static final float FAST_DROP_INTERVAL = 0.05f;
    public static final float MATCH_PROCESS_DELAY = 0.8f;
}
