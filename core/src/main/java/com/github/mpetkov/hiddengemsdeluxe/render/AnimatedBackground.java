package com.github.mpetkov.hiddengemsdeluxe.render;

import com.github.mpetkov.hiddengemsdeluxe.util.GameConfig;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimatedBackground {

    /** Плаващи неонови „мехури“ – движат се и пулсират. */
    private static final int NUM_ORBS = 36;
    private static final float[] ORB_HUES = { 0.55f, 0.78f, 0.88f, 0.62f, 0.45f, 0.72f };
    private static final float ORB_SPEED_MIN = 8f;
    private static final float ORB_SPEED_MAX = 22f;
    private static final float ORB_RADIUS_MIN = 18f;
    private static final float ORB_RADIUS_MAX = 52f;

    private static class Orb {
        float x, y;
        float dx, dy;
        float phase;
        int hueIndex;
        float baseRadius;

        Orb(float x, float y, float dx, float dy, float phase, int hueIndex, float baseRadius) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.phase = phase;
            this.hueIndex = hueIndex;
            this.baseRadius = baseRadius;
        }
    }

    private final List<Orb> orbs = new ArrayList<>();
    private float globalTime = 0f;
    private float worldWidth = GameConfig.WORLD_WIDTH;
    private float worldHeight = GameConfig.WORLD_HEIGHT;

    public AnimatedBackground() {
    }

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

    private void ensureOrbsInitialized(float width, float height) {
        if (width <= 0f || height <= 0f) {
            return;
        }
        if (!orbs.isEmpty() && MathUtils.isEqual(worldWidth, width, 0.5f) && MathUtils.isEqual(worldHeight, height, 0.5f)) {
            return;
        }
        worldWidth = width;
        worldHeight = height;
        orbs.clear();

        Random r = new Random();
        for (int i = 0; i < NUM_ORBS; i++) {
            float x = r.nextFloat() * worldWidth;
            float y = r.nextFloat() * worldHeight;
            float angle = r.nextFloat() * MathUtils.PI2;
            float speed = ORB_SPEED_MIN + r.nextFloat() * (ORB_SPEED_MAX - ORB_SPEED_MIN);
            float dx = MathUtils.cos(angle) * speed;
            float dy = MathUtils.sin(angle) * speed;
            float phase = r.nextFloat() * MathUtils.PI2;
            int hueIndex = r.nextInt(ORB_HUES.length);
            float baseRadius = ORB_RADIUS_MIN + r.nextFloat() * (ORB_RADIUS_MAX - ORB_RADIUS_MIN);
            orbs.add(new Orb(x, y, dx, dy, phase, hueIndex, baseRadius));
        }
    }

    public void update(float delta) {
        globalTime += delta;
        ensureOrbsInitialized(worldWidth, worldHeight);
        for (Orb o : orbs) {
            o.x += o.dx * delta;
            o.y += o.dy * delta;
            if (o.x < -o.baseRadius * 2) o.x = worldWidth + o.baseRadius;
            if (o.x > worldWidth + o.baseRadius * 2) o.x = -o.baseRadius;
            if (o.y < -o.baseRadius * 2) o.y = worldHeight + o.baseRadius;
            if (o.y > worldHeight + o.baseRadius * 2) o.y = -o.baseRadius;
        }
    }

    /** Рисува фона в координатите на подадената камера (Fill/Fit viewport). */
    public void render(ShapeRenderer renderer, OrthographicCamera camera) {
        float left = camera.position.x - camera.viewportWidth / 2f;
        float bottom = camera.position.y - camera.viewportHeight / 2f;
        float W = camera.viewportWidth;
        float H = camera.viewportHeight;

        ensureOrbsInitialized(W, H);

        renderer.begin(ShapeRenderer.ShapeType.Filled);

        final int GRADIENT_STRIPS = 28;
        final float r0 = 0.07f, g0 = 0.02f, b0 = 0.15f;
        final float r1 = 0.01f, g1 = 0.01f, b1 = 0.05f;
        float wave = 0.5f + 0.5f * MathUtils.sin(globalTime * 0.8f);
        float wave2 = 0.5f + 0.5f * MathUtils.sin(globalTime * 0.5f + 1.3f);
        for (int i = 0; i < GRADIENT_STRIPS; i++) {
            float t0 = (float) i / GRADIENT_STRIPS;
            float t1 = (float) (i + 1) / GRADIENT_STRIPS;
            float y0 = bottom + H * (1f - t0);
            float y1 = bottom + H * (1f - t1);
            float r = r0 + (r1 - r0) * t0;
            float g = g0 + (g1 - g0) * t0;
            float b = b0 + (b1 - b0) * t0;
            float wobble = 0.04f * (wave * MathUtils.sin(t0 * MathUtils.PI) + wave2 * 0.5f);
            renderer.setColor(r + wobble, g + wobble * 0.8f, b + wobble * 1.2f, 1f);
            renderer.rect(left, y1, W, y0 - y1);
        }

        renderer.end();

        renderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Orb o : orbs) {
            float pulse = 0.85f + 0.25f * MathUtils.sin(globalTime * 1.4f + o.phase);
            float radius = o.baseRadius * pulse;
            float hue = ORB_HUES[o.hueIndex] + 0.03f * MathUtils.sin(globalTime * 0.7f + o.phase * 2f);
            Color c = hsvToColor(hue % 1f, 0.7f, 0.5f + 0.35f * MathUtils.sin(globalTime * 1.1f + o.phase * 1.5f));
            c.a = 0.08f + 0.18f * (0.5f + 0.5f * MathUtils.sin(globalTime * 1.2f + o.phase));
            float drawX = left + o.x;
            float drawY = bottom + o.y;
            renderer.setColor(c);
            renderer.circle(drawX, drawY, radius);
            float glowRadius = radius * 1.35f;
            c.a *= 0.4f;
            renderer.setColor(c);
            renderer.circle(drawX, drawY, glowRadius);
        }
        renderer.end();
    }
}
