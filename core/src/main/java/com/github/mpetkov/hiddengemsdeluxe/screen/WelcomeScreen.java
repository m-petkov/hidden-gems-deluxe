package com.github.mpetkov.hiddengemsdeluxe.screen;

import com.github.mpetkov.hiddengemsdeluxe.GameApp;
import com.github.mpetkov.hiddengemsdeluxe.render.AnimatedBackground;
import com.github.mpetkov.hiddengemsdeluxe.util.MobileWebLayout;
import com.github.mpetkov.hiddengemsdeluxe.util.SaveManager;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import com.badlogic.gdx.utils.viewport.FillViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class WelcomeScreen implements Screen {

    private final GameApp game;
    private Stage stage;
    private Skin skin;
    private ShapeRenderer shapeRenderer;
    private AnimatedBackground background;
    private Viewport backgroundViewport;
    private OrthographicCamera backgroundCamera;

    private Label highScoreLabel;
    private Label highLevelLabel;
    private MobileWebLayout.Mode layoutMode = MobileWebLayout.Mode.DESKTOP;

    public WelcomeScreen(GameApp game) {
        this.game = game;
    }

    @Override
    public void show() {
        layoutMode = MobileWebLayout.resolveMode();
        buildScreen(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void buildScreen(int screenW, int screenH) {
        disposeUi();

        shapeRenderer = new ShapeRenderer();
        background = new AnimatedBackground();
        backgroundCamera = new OrthographicCamera();

        MobileWebLayout.WelcomeLayout wl = MobileWebLayout.welcomeLayout(layoutMode);
        backgroundViewport = new FillViewport(wl.worldWidth, wl.worldHeight, backgroundCamera);

        Viewport stageViewport = wl.fillViewport
                ? new FillViewport(wl.worldWidth, wl.worldHeight, new OrthographicCamera())
                : new FitViewport(wl.worldWidth, wl.worldHeight, new OrthographicCamera());
        stage = new Stage(stageViewport);
        Gdx.input.setInputProcessor(stage);

        float fontScale = MobileWebLayout.isWeb() ? Math.min(MobileWebLayout.getPixelRatio(), 2f) : 1f;
        int fontSize = Math.round(wl.fontSize * fontScale);

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Play-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = fontSize;
        p.color = Color.CYAN;
        BitmapFont font = gen.generateFont(p);
        if (font.getRegion() != null && font.getRegion().getTexture() != null) {
            font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        gen.dispose();

        skin = new Skin();
        skin.add("default-font", font);

        int btnW = wl.btnWidth;
        int btnH = wl.btnHeight;
        Texture btnUpTex = createLime3DButton(btnW, btnH, true, false);
        Texture btnDownTex = createLime3DButton(btnW, btnH, false, false);
        Texture btnOverTex = createLime3DButton(btnW, btnH, true, true);
        skin.add("btn-up", btnUpTex);
        skin.add("btn-down", btnDownTex);
        skin.add("btn-over", btnOverTex);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = new Color(0.15f, 0.35f, 0.05f, 1f);
        style.downFontColor = new Color(0.12f, 0.3f, 0.02f, 1f);
        style.overFontColor = new Color(0.1f, 0.4f, 0.02f, 1f);
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

        table.top().padTop(wl.padTop);
        table.add(title).padBottom(wl.titlePadBottom).row();
        table.add(highScoreLabel).padBottom(wl.rowPad).row();
        table.add(highLevelLabel).padBottom(wl.rowPad * 2).row();

        if (game.isPaused) {
            table.add(resume).width(btnW).height(btnH).padBottom(wl.rowPad).row();
        }

        table.add(start).width(btnW).height(btnH).padBottom(wl.rowPad).row();
        table.add(reset).width(btnW).height(btnH).padBottom(wl.rowPad).row();

        if (Gdx.app.getType() != Application.ApplicationType.WebGL) {
            TextButton exit = new TextButton("Exit", skin);
            exit.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.exit();
                }
            });
            table.add(exit).width(btnW).height(btnH);
        }

        applyViewport(screenW, screenH);
    }

    private void applyViewport(int screenW, int screenH) {
        if (backgroundViewport != null) {
            backgroundViewport.update(screenW, screenH, true);
        }
        if (stage != null) {
            stage.getViewport().update(screenW, screenH, true);
        }
    }

    /** Плътен lime бутон – форма „пилюла“ (напълно закръглени краища), 3D ефект. */
    private static final float BUTTON_ALPHA = 0.96f;

    private Texture createLime3DButton(int w, int h, boolean raised, boolean hover) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        int r = h / 2;

        if (raised && !hover) {
            fillPill(p, 0, 0, w, h, r, 0.45f, 1f, 0.12f, BUTTON_ALPHA);
            p.setColor(new Color(0.72f, 1f, 0.4f, BUTTON_ALPHA));
            p.fillRectangle(r, 0, w - 2 * r, 4);
            p.setColor(new Color(0.18f, 0.6f, 0f, BUTTON_ALPHA));
            p.fillRectangle(r, h - 5, w - 2 * r, 5);
        } else if (raised && hover) {
            fillPill(p, 0, 0, w, h, r, 0.52f, 1f, 0.2f, BUTTON_ALPHA);
            p.setColor(new Color(0.82f, 1f, 0.5f, BUTTON_ALPHA));
            p.fillRectangle(r, 0, w - 2 * r, 5);
            p.setColor(new Color(0.22f, 0.7f, 0.05f, BUTTON_ALPHA));
            p.fillRectangle(r, h - 5, w - 2 * r, 5);
        } else {
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

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        backgroundViewport.update(screenW, screenH, true);
        backgroundViewport.apply();
        shapeRenderer.setProjectionMatrix(backgroundCamera.combined);
        background.update(delta);
        background.render(shapeRenderer, backgroundCamera);

        stage.getViewport().update(screenW, screenH, true);
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) {
        MobileWebLayout.Mode newMode = MobileWebLayout.resolveMode();
        if (newMode != layoutMode) {
            layoutMode = newMode;
            buildScreen(w, h);
        } else {
            applyViewport(w, h);
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    private void disposeUi() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
    }

    @Override
    public void dispose() {
        disposeUi();
    }
}
