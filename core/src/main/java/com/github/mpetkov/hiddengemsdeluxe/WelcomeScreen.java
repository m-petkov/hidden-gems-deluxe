package com.github.mpetkov.hiddengemsdeluxe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class WelcomeScreen implements Screen {

    private final Main game;
    private Stage stage;
    private Skin skin;
    private TextButton resumeButton;

    public WelcomeScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // --- КОРЕКЦИЯ: Смяна на Color.GOLD с Color.CYAN за шрифта ---
        // Шрифт
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Play-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter fontParameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        fontParameter.size = 32;
        fontParameter.color = Color.CYAN; // <--- Смяна: Gold -> Cyan
        BitmapFont customFont = generator.generateFont(fontParameter);
        generator.dispose();
        // -----------------------------------------------------------

        // Skin
        skin = new Skin();
        skin.add("default-font", customFont);

        // Стил за бутон (Цветът на текста на бутоните вече е LIME)
        TextButton.TextButtonStyle orangeButtonStyle = new TextButton.TextButtonStyle();
        orangeButtonStyle.font = customFont;
        orangeButtonStyle.fontColor = Color.LIME;
        orangeButtonStyle.up = createRoundedRectDrawable(300, 60, 12, Color.DARK_GRAY);
        orangeButtonStyle.down = createRoundedRectDrawable(300, 60, 12, Color.GRAY);
        skin.add("orange", orangeButtonStyle);

        // Таблица за подреждане
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Заглавие
        // КОРЕКЦИЯ: Използва Color.CYAN за стила на заглавието
        Label.LabelStyle titleStyle = new Label.LabelStyle(customFont, Color.CYAN); // <--- Смяна: Gold -> Cyan
        Label title = new Label("Hidden Gems Deluxe", titleStyle);

        // Resume Game
        resumeButton = new TextButton("Resume Game", skin, "orange");
        resumeButton.setDisabled(!game.isPaused);
        resumeButton.setVisible(game.isPaused);
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!resumeButton.isDisabled()) {
                    game.resumeGame();
                }
            }
        });

        // Start Game
        TextButton startButton = new TextButton("Start Game", skin, "orange");
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.isPaused = false;
                dispose();
                game.setScreen(new GameScreen(game));
            }
        });

        // Exit
        TextButton exitButton = new TextButton("Exit", skin, "orange");
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Подреждане в таблицата
        table.top().padTop(100);
        table.add(title).padBottom(60).row();

        if (game.isPaused) {
            table.add(resumeButton).width(300).height(60).padBottom(20).row();
        }

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

    @Override public void resize(int width, int height) {
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
