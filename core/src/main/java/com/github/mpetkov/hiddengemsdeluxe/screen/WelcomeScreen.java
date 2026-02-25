package com.github.mpetkov.hiddengemsdeluxe.screen;

import com.github.mpetkov.hiddengemsdeluxe.GameApp;
import com.github.mpetkov.hiddengemsdeluxe.render.AnimatedBackground;
import com.github.mpetkov.hiddengemsdeluxe.util.SaveManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class WelcomeScreen implements Screen {

    private final GameApp game;
    private Stage stage;
    private Skin skin;
    private ShapeRenderer shapeRenderer;
    private AnimatedBackground background;

    private Label highScoreLabel;
    private Label highLevelLabel;

    public WelcomeScreen(GameApp game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        shapeRenderer = new ShapeRenderer();
        background = new AnimatedBackground();

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Play-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 32;
        p.color = Color.CYAN;
        BitmapFont font = gen.generateFont(p);
        gen.dispose();

        skin = new Skin();
        skin.add("default-font", font);

        // Плътни lime бутони – форма „пилюла“, 3D ефект + hover
        int btnW = 320;
        int btnH = 56;
        Texture btnUpTex = createLime3DButton(btnW, btnH, true, false);   // издигнат
        Texture btnDownTex = createLime3DButton(btnW, btnH, false, false); // натиснат
        Texture btnOverTex = createLime3DButton(btnW, btnH, true, true);  // hover – по-ярък
        skin.add("btn-up", btnUpTex);
        skin.add("btn-down", btnDownTex);
        skin.add("btn-over", btnOverTex);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = new Color(0.15f, 0.35f, 0.05f, 1f);
        style.downFontColor = new Color(0.12f, 0.3f, 0.02f, 1f);
        style.overFontColor = new Color(0.1f, 0.4f, 0.02f, 1f);  // леко по-ярък текст при hover
        style.up = new TextureRegionDrawable(new TextureRegion(btnUpTex));
        style.down = new TextureRegionDrawable(new TextureRegion(btnDownTex));
        style.over = new TextureRegionDrawable(new TextureRegion(btnOverTex));
        skin.add("default", style);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("Hidden Gems Deluxe", new Label.LabelStyle(font, Color.CYAN));

        highScoreLabel = new Label("Highest Score: " + SaveManager.getHighScore(),
            new Label.LabelStyle(font, Color.WHITE));

        highLevelLabel = new Label("Highest Level: " + SaveManager.getHighLevel(),
            new Label.LabelStyle(font, Color.WHITE));

        TextButton start = new TextButton("Start Game", skin);
        start.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.startNewGame();
            }
        });

        TextButton resume = new TextButton("Resume Game", skin);
        resume.setVisible(game.isPaused);
        resume.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.resumeGame();
            }
        });

        TextButton reset = new TextButton("Reset High Score", skin);
        reset.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                SaveManager.reset();
                highScoreLabel.setText("Highest Score: " + SaveManager.getHighScore());
                highLevelLabel.setText("Highest Level: " + SaveManager.getHighLevel());
            }
        });

        TextButton exit = new TextButton("Exit", skin);
        exit.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        table.top().padTop(80);
        table.add(title).padBottom(40).row();
        table.add(highScoreLabel).padBottom(10).row();
        table.add(highLevelLabel).padBottom(30).row();

        if (game.isPaused) {
            table.add(resume).padBottom(20).row();
        }

        table.add(start).padBottom(15).row();
        table.add(reset).padBottom(15).row();
        table.add(exit);
    }

    /** Плътен lime бутон – форма „пилюла“ (напълно закръглени краища), 3D ефект. */
    private static final float BUTTON_ALPHA = 0.96f;

    private Texture createLime3DButton(int w, int h, boolean raised, boolean hover) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        int r = h / 2;

        if (raised && !hover) {
            // Издигнат (нормален)
            fillPill(p, 0, 0, w, h, r, 0.45f, 1f, 0.12f, BUTTON_ALPHA);
            p.setColor(new Color(0.72f, 1f, 0.4f, BUTTON_ALPHA));
            p.fillRectangle(r, 0, w - 2 * r, 4);
            p.setColor(new Color(0.18f, 0.6f, 0f, BUTTON_ALPHA));
            p.fillRectangle(r, h - 5, w - 2 * r, 5);
        } else if (raised && hover) {
            // Hover – по-ярък lime, по-силен highlight отгоре
            fillPill(p, 0, 0, w, h, r, 0.52f, 1f, 0.2f, BUTTON_ALPHA);
            p.setColor(new Color(0.82f, 1f, 0.5f, BUTTON_ALPHA));
            p.fillRectangle(r, 0, w - 2 * r, 5);
            p.setColor(new Color(0.22f, 0.7f, 0.05f, BUTTON_ALPHA));
            p.fillRectangle(r, h - 5, w - 2 * r, 5);
        } else {
            // Натиснат
            fillPill(p, 0, 0, w, h, r, 0.28f, 0.82f, 0.05f, BUTTON_ALPHA);
            p.setColor(new Color(0.12f, 0.5f, 0f, BUTTON_ALPHA));
            p.fillRectangle(r, 0, w - 2 * r, 5);
            p.setColor(new Color(0.38f, 0.9f, 0.15f, BUTTON_ALPHA));
            p.fillRectangle(r, h - 4, w - 2 * r, 4);
        }

        Texture tex = new Texture(p);
        p.dispose();
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return tex;
    }

    /** Запълва форма „пилюла“ (капсула): правоъгълник + два полукръга в краищата. */
    private void fillPill(Pixmap p, int x, int y, int w, int h, int r,
            float cr, float cg, float cb, float ca) {
        int ir = (int) (cr * 255);
        int ig = (int) (cg * 255);
        int ib = (int) (cb * 255);
        int ia = (int) (ca * 255);
        p.setColor(ir, ig, ib, ia);
        p.fillRectangle(x + r, y, w - 2 * r, h);
        p.fillCircle(x + r, y + r, r);
        p.fillCircle(x + w - r - 1, y + r, r);
    }

    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        background.update(delta);
        background.render(shapeRenderer);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        stage.dispose();
        skin.dispose();
    }
}
