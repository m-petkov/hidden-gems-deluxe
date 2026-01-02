package com.github.mpetkov.hiddengemsdeluxe.model;


// Няма декларация за пакет

import com.badlogic.gdx.graphics.Color;

public class MatchMarker {
    public int col, row;
    public float timer;
    public Color color;

    public MatchMarker(int col, int row, Color color) {
        this.col = col;
        this.row = row;
        this.timer = 0.8f;
        this.color = color;
    }

    public void update(float delta) {
        timer -= delta;
    }

    public boolean isExpired() {
        return timer <= 0;
    }
}