package com.upnvj.compstolife.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.upnvj.compstolife.CompsGame;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

public class MainMenuScreen implements Screen {
    private final CompsGame game;
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private Texture startNormal, startHover;
    private Texture quitNormal, quitHover;

    public MainMenuScreen(CompsGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        
        // Load textures
        backgroundTexture = new Texture(Gdx.files.internal("background-screen.png"));
        startNormal = new Texture(Gdx.files.internal("button-start-normal.png"));
        startHover = new Texture(Gdx.files.internal("button-start-hover.png"));
        quitNormal = new Texture(Gdx.files.internal("button-quit-normal.png"));
        quitHover = new Texture(Gdx.files.internal("button-quit-hover.png"));
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Background Image
        Image background = new Image(backgroundTexture);
        background.setFillParent(true);
        stage.addActor(background);
        background.toBack();

        // Start Button
        TextureRegionDrawable startDrawable = new TextureRegionDrawable(new TextureRegion(startNormal));
        TextureRegionDrawable startHoverDrawable = new TextureRegionDrawable(new TextureRegion(startHover));
        ImageButton.ImageButtonStyle startStyle = new ImageButton.ImageButtonStyle();
        startStyle.imageUp = startDrawable;
        startStyle.imageOver = startHoverDrawable;
        
        ImageButton startButton = new ImageButton(startStyle);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game, "Player"));
            }
        });

        // Quit Button
        TextureRegionDrawable quitDrawable = new TextureRegionDrawable(new TextureRegion(quitNormal));
        TextureRegionDrawable quitHoverDrawable = new TextureRegionDrawable(new TextureRegion(quitHover));
        ImageButton.ImageButtonStyle quitStyle = new ImageButton.ImageButtonStyle();
        quitStyle.imageUp = quitDrawable;
        quitStyle.imageOver = quitHoverDrawable;

        ImageButton quitButton = new ImageButton(quitStyle);
        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        // Dialog Example Button (TextButton for simplicity)
        TextButton dialogButton = new TextButton("Show Dialog", skin);
        dialogButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showExampleDialog();
            }
        });

        // Layout
        table.bottom().right().pad(40); // Move to bottom right with 40px padding
        table.add(startButton)
            .size(startNormal.getWidth() * 0.5f, startNormal.getHeight() * 0.5f)
            .padBottom(10)
            .row();
        table.add(quitButton)
            .size(quitNormal.getWidth() * 0.5f, quitNormal.getHeight() * 0.5f);
    }

    private void showExampleDialog() {
        // Method can be kept but not called, or removed. I'll remove it for cleanliness.
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        backgroundTexture.dispose();
        startNormal.dispose();
        startHover.dispose();
        quitNormal.dispose();
        quitHover.dispose();
    }
}
