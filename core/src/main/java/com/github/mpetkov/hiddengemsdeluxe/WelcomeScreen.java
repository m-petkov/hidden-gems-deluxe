package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class WelcomeScreen implements Screen {

    private final Main game;
    private Stage stage;
    private Skin skin;
    private Texture background;

    public WelcomeScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Зареждане на шрифт от файл
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Play-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter fontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        fontParameter.size = 32;
        fontParameter.color = Color.GOLD;
        BitmapFont customFont = generator.generateFont(fontParameter);
        generator.dispose();

        // Skin
        skin = new Skin();
        skin.add("default-font", customFont);

        // Бутони стил
        TextButton.TextButtonStyle orangeButtonStyle = new TextButton.TextButtonStyle();
        orangeButtonStyle.font = customFont;
        orangeButtonStyle.fontColor = Color.ORANGE;
        orangeButtonStyle.up = createRoundedRectDrawable(300, 60, 12, Color.DARK_GRAY);
        orangeButtonStyle.down = createRoundedRectDrawable(300, 60, 12, Color.GRAY);
        skin.add("orange", orangeButtonStyle);

        // Таблица
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Заглавие
        Label.LabelStyle titleStyle = new Label.LabelStyle(customFont, Color.GOLD);
        Label title = new Label("Hidden Gems Deluxe", titleStyle);

        // Start бутон
        TextButton startButton = new TextButton("Start Game", skin, "orange");
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
            }
        });

        // Exit бутон
        TextButton exitButton = new TextButton("Exit", skin, "orange");
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit(); // Затваря играта
            }
        });

        // Добавяне към таблицата
        table.top().padTop(100);
        table.add(title).padBottom(60).row();
        table.add(startButton).width(300).height(60).padBottom(20).row();
        table.add(exitButton).width(300).height(60);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    private Drawable createRoundedRectDrawable(int width, int height, int radius, Color color) {
        Pixmap pixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fillRectangle(0, 0, width, height);
        return new Image(new Texture(pixmap)).getDrawable();
    }
}
