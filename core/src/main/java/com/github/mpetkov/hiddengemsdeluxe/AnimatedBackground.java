// AnimatedBackground.java
package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimatedBackground {

    // Класът Drop вече е статичен nested клас
    private static class Drop { // Добавено 'static'
        float x, y, speed, length;
        Color color;
        private Random random; // Добавяме Random като поле в Drop

        // Конструкторът приема Random инстанция
        Drop(float x, float y, float speed, float length, Color color, Random random) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = length;
            this.color = color;
            this.random = random; // Запазваме Random инстанцията
        }

        void update(float delta) {
            y -= speed * delta;
            // Когато една капка излезе от екрана, я рестартираме в горната част
            if (y + length < 0) {
                reset();
            }
        }

        void reset() {
            // Рестартираме позицията и свойствата на капката
            y = Gdx.graphics.getHeight(); // Започва от горния край
            // x = random.nextFloat() * Gdx.graphics.getWidth(); // Можеш да го разкоментираш, ако искаш x да се променя
            speed = 60 + random.nextFloat() * 100; // По-бързи капки
            length = 20 + random.nextFloat() * 60; // По-дълги капки
            color = new Color(0, 1f, 0.6f, 0.3f + random.nextFloat() * 0.5f); // Зеленикав цвят
        }
    }

    private final List<Drop> drops = new ArrayList<>();
    private final Random random = new Random(); // Тази инстанция на Random ще се подава на Drop

    public AnimatedBackground(int count) {
        for (int i = 0; i < count; i++) {
            float x = random.nextFloat() * Gdx.graphics.getWidth();
            float y = random.nextFloat() * Gdx.graphics.getHeight();
            float speed = 60 + random.nextFloat() * 100;
            float length = 20 + random.nextFloat() * 60;
            Color color = new Color(0, 1f, 0.6f, 0.3f + random.nextFloat() * 0.5f);
            // Подаваме инстанцията 'random' на конструктора на Drop
            drops.add(new Drop(x, y, speed, length, color, random));
        }
    }

    public void update(float delta) {
        // Използваме Iterator, за да можем безопасно да премахваме елементи, ако се наложи
        // Въпреки че тук не премахваме, а само рестартираме
        for (Drop d : drops) {
            d.update(delta);
        }
    }

    public void render(ShapeRenderer renderer) {
        renderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Drop d : drops) {
            renderer.setColor(d.color);
            // Използваме rectLine за да нарисуваме линия с дебелина 2 пиксела
            renderer.rectLine(d.x, d.y, d.x, d.y + d.length, 2);
        }
        renderer.end();
    }
}
