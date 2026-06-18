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

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import com.badlogic.gdx.audio.Music;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public class MainMenuScreen implements Screen {
    private final CompsGame game;
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private Texture startNormal, startHover;
    private Texture quitNormal, quitHover;
    private Music backgroundMusic;
    private Sound hoverSound, clickSound;

    // Transition variables
    private ShapeRenderer shapeRenderer;
    private float fadeAlpha = 0;
    private boolean isExiting = false;
    private float fadeSpeed = 0.5f;

    public MainMenuScreen(CompsGame game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport());
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        this.shapeRenderer = new ShapeRenderer();

        // Load textures
        backgroundTexture = new Texture(Gdx.files.internal("background-screen.png"));
        startNormal = new Texture(Gdx.files.internal("button-start-normal.png"));
        startHover = new Texture(Gdx.files.internal("button-start-hover.png"));
        quitNormal = new Texture(Gdx.files.internal("button-quit-normal.png"));
        quitHover = new Texture(Gdx.files.internal("button-quit-hover.png"));

        // Load sounds
        hoverSound = Gdx.audio.newSound(Gdx.files.internal("hover.ogg"));
        clickSound = Gdx.audio.newSound(Gdx.files.internal("click.ogg"));

        // Load and play music
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("mainmenu-music.ogg"));
        backgroundMusic.setLooping(true);
        backgroundMusic.setVolume(1.0f);
        backgroundMusic.play();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);

        // Debug and Load Music
        if (backgroundMusic == null) {
            boolean exists = Gdx.files.internal("mainmenu-music.ogg").exists();
            System.out.println("Checking mainmenu-music.ogg: " + (exists ? "FOUND" : "NOT FOUND"));
            
            if (exists) {
                try {
                    backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("mainmenu-music.ogg"));
                    backgroundMusic.setLooping(true);
                    backgroundMusic.setVolume(1.0f);
                    backgroundMusic.play();
                    System.out.println("Music (.ogg) play() called successfully.");
                } catch (Exception e) {
                    System.err.println("Error playing music: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else if (!backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }

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
                // Play sound 3 times to simulate 3x volume
                for(int i = 0; i < 3; i++) clickSound.play(1.0f);
                isExiting = true;
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) { // Only play when mouse enters
                    for(int i = 0; i < 3; i++) hoverSound.play(1.0f);
                }
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
                for(int i = 0; i < 3; i++) clickSound.play(1.0f);
                Gdx.app.exit();
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    for(int i = 0; i < 3; i++) hoverSound.play(1.0f);
                }
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

        // Handle Fade Out
        if (isExiting) {
            fadeAlpha += delta * fadeSpeed;
            if (fadeAlpha >= 1) {
                fadeAlpha = 1;
                game.setScreen(new GameScreen(game, "Player"));
            }
        }

        if (fadeAlpha > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, fadeAlpha);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
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
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        shapeRenderer.dispose();
        backgroundTexture.dispose();
        startNormal.dispose();
        startHover.dispose();
        quitNormal.dispose();
        quitHover.dispose();
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
        if (hoverSound != null) {
            hoverSound.dispose();
        }
        if (clickSound != null) {
            clickSound.dispose();
        }
    }
}
