package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AnimatedBackground {
    private class Drop {
        float x, y, speed, length;
        Color color;

        Drop(float x, float y, float speed, float length, Color color) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = length;
            this.color = color;
        }

        void update(float delta) {
            y -= speed * delta;
            if (y + length < 0) {
                reset();
            }
        }

        void reset() {
            y = Gdx.graphics.getHeight() + length;
            speed = 60 + random.nextFloat() * 100;
            length = 20 + random.nextFloat() * 60;
            color = new Color(0, 1f, 0.6f, 0.3f + random.nextFloat() * 0.5f);
        }
    }

    private final List<Drop> drops = new ArrayList<>();
    private final Random random = new Random();

    public AnimatedBackground(int count) {
        for (int i = 0; i < count; i++) {
            float x = random.nextFloat() * Gdx.graphics.getWidth();
            float y = random.nextFloat() * Gdx.graphics.getHeight();
            float speed = 60 + random.nextFloat() * 100;
            float length = 20 + random.nextFloat() * 60;
            Color color = new Color(0, 1f, 0.6f, 0.3f + random.nextFloat() * 0.5f);
            drops.add(new Drop(x, y, speed, length, color));
        }
    }

    public void update(float delta) {
        for (Drop d : drops) {
            d.update(delta);
        }
    }

    public void render(ShapeRenderer renderer) {
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Drop d : drops) {
            renderer.setColor(d.color);
            renderer.rectLine(d.x, d.y, d.x, d.y + d.length, 2);
        }
        renderer.end();
    }
}
