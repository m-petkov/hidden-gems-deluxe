// AnimatedBackground.java
package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import java.lang.Math;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimatedBackground {

    // Клас за симулиране на бързо летяща звезда (запазен)
    private static class Star {
        float x, y, speed;
        float hueOffset;

        private final Random random;

        Star(Random random) {
            this.random = random;
            reset();
        }

        void update(float delta) {
            // Движение само по Y, симулирайки полет "напред"
            y -= speed * delta;

            // Рестарт, когато излезе извън екрана
            if (y < 0) {
                reset();
            }
        }

        void reset() {
            x = random.nextFloat() * Gdx.graphics.getWidth();
            y = Gdx.graphics.getHeight() + random.nextFloat() * 50f; // Започва малко над екрана
            speed = 300 + random.nextFloat() * 500; // Много висока скорост
            hueOffset = random.nextFloat();
        }

        Color getColor(float globalTime) {
            float hueCenter = 0.65f;
            float hueAmplitude = 0.3f;

            float baseHue = (hueCenter + hueAmplitude * MathUtils.sin(globalTime * 0.5f)) % 1f;
            float finalHue = (baseHue + hueOffset * 0.5f) % 1f;

            // Звездите са винаги ярки, но с различна интензивност
            return hsvToColor(finalHue, 1.0f, 0.8f + 0.2f * random.nextFloat());
        }
    }

    private final List<Star> stars = new ArrayList<>();
    private final Random random = new Random();
    private float globalTime = 0f;

    // Параметри за звездите
    private final int NUM_STARS;

    // Премахваме всички променливи за фон/линии

    private static Color hsvToColor(float h, float s, float v) {
        float r = 0, g = 0, b = 0;
        int i = (int) Math.floor(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: r = v; g = p; b = q; break;
        }
        return new Color(r, g, b, 1f);
    }

    public AnimatedBackground(int count) {
        NUM_STARS = count;
        // Инициализираме бързо движещите се звезди
        for (int i = 0; i < NUM_STARS; i++) {
            stars.add(new Star(random));
        }
    }

    public void update(float delta) {
        globalTime += delta;
        for (Star s : stars) {
            s.update(delta);
        }
    }

    public void render(ShapeRenderer renderer) {
        renderer.begin(ShapeRenderer.ShapeType.Filled);

        // === Обновяване на общите настройки ===
        final float SCREEN_HEIGHT = Gdx.graphics.getHeight();

        // ----------------------------------------------------------------------------------
        // 1. Рисуване на Бързи Неонови Звезди (Warp Speed)
        // ----------------------------------------------------------------------------------
        for (Star star : stars) {
            Color starColor = star.getColor(globalTime);

            // Алфа каналът е зависим от позицията (по-ярки в средата на екрана)
            float brightness = 0.5f + 0.5f * (star.y / SCREEN_HEIGHT);
            starColor.a = MathUtils.clamp(brightness * 0.8f, 0.2f, 1.0f);

            renderer.setColor(starColor);

            // Рисуваме звездата като линия, за да симулираме движение (като hyperdrive)
            float trailLength = star.speed / 200f; // По-бързите звезди имат по-дълги следи
            renderer.rectLine(star.x, star.y, star.x, star.y + trailLength, 2f);
        }

        renderer.end();
    }
}
