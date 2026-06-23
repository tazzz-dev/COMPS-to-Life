package com.upnvj.compstolife.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.upnvj.compstolife.CompsGame;

public class HasilSnbtScreen implements Screen {
    private final CompsGame game;
    private Texture texture;
    private ShapeRenderer shapeRenderer;
    private final Matrix4 screenProjection = new Matrix4();

    private enum State {
        FADE_IN,
        DISPLAY,
        FADE_OUT,
        FINISHED
    }

    private State currentState = State.FADE_IN;
    private float fadeAlpha = 1.0f;
    private float fadeSpeed = 0.5f; // Fade in/out takes 2 seconds
    private float displayTimer = 0.0f;
    private static final float DISPLAY_DURATION = 2.0f; // Show for 2 seconds

    private final String[] scenePaths = {"scene/buka-web-snbt.png", "scene/hasil-snbt.png"};
    private int currentSceneIndex = 0;

    public HasilSnbtScreen(CompsGame game) {
        this.game = game;
        this.texture = new Texture(Gdx.files.internal(scenePaths[currentSceneIndex]));
        this.shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        switch (currentState) {
            case FADE_IN:
                fadeAlpha -= delta * fadeSpeed;
                if (fadeAlpha <= 0) {
                    fadeAlpha = 0;
                    currentState = State.DISPLAY;
                    displayTimer = 0;
                }
                break;
            case DISPLAY:
                displayTimer += delta;
                if (displayTimer >= DISPLAY_DURATION) {
                    currentState = State.FADE_OUT;
                }
                break;
            case FADE_OUT:
                fadeAlpha += delta * fadeSpeed;
                if (fadeAlpha >= 1) {
                    fadeAlpha = 1;
                    currentSceneIndex++;
                    if (currentSceneIndex < scenePaths.length) {
                        if (texture != null) {
                            texture.dispose();
                        }
                        texture = new Texture(Gdx.files.internal(scenePaths[currentSceneIndex]));
                        currentState = State.FADE_IN;
                        displayTimer = 0;
                    } else {
                        currentState = State.FINISHED;
                        game.setScreen(new GameScreen(game, "Player"));
                        return;
                    }
                }
                break;
            case FINISHED:
                return;
        }

        if (texture == null || shapeRenderer == null) {
            return;
        }

        screenProjection.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Draw background texture stretched to fill screen
        game.batch.setProjectionMatrix(screenProjection);
        game.batch.begin();
        game.batch.draw(texture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.batch.end();

        // Draw black overlay for fade transition
        if (fadeAlpha > 0) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(screenProjection);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, fadeAlpha);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    @Override
    public void resize(int width, int height) {
        screenProjection.setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (texture != null) {
            texture.dispose();
            texture = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
    }
}
