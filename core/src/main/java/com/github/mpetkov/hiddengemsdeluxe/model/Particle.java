package com.github.mpetkov.hiddengemsdeluxe.model;


// Няма декларация за пакет

import com.badlogic.gdx.graphics.Color;

public class Particle {
    public float x, y, dx, dy;
    public float size = 6f;
    public float life;
    public float initialLife;
    public Color color;

    public Particle(float x, float y, float dx, float dy, Color color) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.life = 0.6f;
        this.initialLife = 0.6f;
        this.color = new Color(color);
    }

    public void update(float delta) {
        x += dx * delta;
        y += dy * delta;
        life -= delta;
    }
}