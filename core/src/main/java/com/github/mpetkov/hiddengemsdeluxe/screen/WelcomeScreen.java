package com.github.mpetkov.hiddengemsdeluxe.screen;

import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.github.mpetkov.hiddengemsdeluxe.GameApp;
import com.github.mpetkov.hiddengemsdeluxe.util.SaveManager;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class WelcomeScreen implements Screen {

    private final GameApp game;
    private Stage stage;
    private Skin skin;

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
        Label score = new Label("High Score: " + SaveManager.getHighScore(), new Label.LabelStyle(font, Color.WHITE));
        Label level = new Label("Highest Level: " + SaveManager.getHighLevel(), new Label.LabelStyle(font, Color.WHITE));

        TextButton start = new TextButton("Start Game", skin);
        start.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.startNewGame();
            }
        });

        TextButton resume = new TextButton("Resume Game", skin);
        resume.setVisible(game.isPaused);
        resume.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                game.resumeGame();
            }
        });

        TextButton exit = new TextButton("Exit", skin);
        exit.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        table.add(title).padBottom(40).row();
        table.add(score).padBottom(10).row();
        table.add(level).padBottom(40).row();
        if (game.isPaused) table.add(resume).padBottom(20).row();
        table.add(start).padBottom(20).row();
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

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }
}
