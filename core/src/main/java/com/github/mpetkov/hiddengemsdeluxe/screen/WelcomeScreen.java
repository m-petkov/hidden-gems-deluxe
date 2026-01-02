package com.github.mpetkov.hiddengemsdeluxe.screen;

import com.github.mpetkov.hiddengemsdeluxe.GameApp;
import com.github.mpetkov.hiddengemsdeluxe.util.SaveManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class WelcomeScreen implements Screen {

    private final GameApp game;
    private Stage stage;
    private Skin skin;

    private Label highScoreLabel;
    private Label highLevelLabel;

    public WelcomeScreen(GameApp game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Play-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = 32;
        p.color = Color.CYAN;
        BitmapFont font = gen.generateFont(p);
        gen.dispose();

        skin = new Skin();
        skin.add("default-font", font);

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.LIME;
        style.up = rect(300, 60, Color.DARK_GRAY);
        style.down = rect(300, 60, Color.GRAY);
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

    private Drawable rect(int w, int h, Color c) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        return new Image(new Texture(p)).getDrawable();
    }

    @Override public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
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
        stage.dispose();
        skin.dispose();
    }
}
