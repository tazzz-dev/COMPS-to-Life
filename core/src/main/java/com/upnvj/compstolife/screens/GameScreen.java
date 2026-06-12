package com.upnvj.compstolife.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.upnvj.compstolife.CompsGame;
import com.upnvj.compstolife.database.DatabaseManager;
import com.upnvj.compstolife.entities.Player;

public class GameScreen implements Screen {
    private final CompsGame game;
    private final Player player;
    private OrthographicCamera camera;
    private Texture playerTexture;
    private Vector2 playerPos;
    private float moveSpeed = 200f;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private Stage uiStage;
    private Skin skin;
    private boolean quizActive = false;
    private DatabaseManager dbManager;

    public GameScreen(CompsGame game, String username) {
        this.game = game;
        this.player = new Player(username);
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, 800, 480);
        this.playerTexture = new Texture(Gdx.files.internal("libgdx.png"));
        this.playerPos = new Vector2(100, 100);

        this.map = new TmxMapLoader().load("map.tmx");
        this.mapRenderer = new OrthogonalTiledMapRenderer(map);
        
        this.uiStage = new Stage(new ScreenViewport());
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        this.dbManager = new DatabaseManager();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(uiStage);
    }

    @Override
    public void render(float delta) {
        if (!quizActive) {
            handleInput(delta);
            update(delta);
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        game.batch.draw(playerTexture, playerPos.x, playerPos.y, 32, 32);
        game.batch.end();

        uiStage.act(delta);
        uiStage.draw();
    }

    private void handleInput(float delta) {
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) playerPos.x -= moveSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) playerPos.x += moveSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) playerPos.y += moveSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) playerPos.y -= moveSpeed * delta;
    }

    private void update(float delta) {
        camera.position.set(playerPos.x + 16, playerPos.y + 16, 0);
        checkCollisions();
    }

    private void checkCollisions() {
        Rectangle playerRect = new Rectangle(playerPos.x, playerPos.y, 32, 32);
        for (MapObject object : map.getLayers().get("Quizzes").getObjects()) {
            if (object instanceof RectangleMapObject) {
                Rectangle objRect = ((RectangleMapObject) object).getRectangle();
                if (playerRect.overlaps(objRect)) {
                    showQuizDialog(object);
                    // Move player back slightly to avoid re-triggering immediately
                    playerPos.sub(10, 10); 
                    break;
                }
            }
        }
    }

    private void showQuizDialog(MapObject quizObject) {
        quizActive = true;
        String question = quizObject.getProperties().get("question", String.class);
        String answer = quizObject.getProperties().get("answer", String.class);
        String optionsStr = quizObject.getProperties().get("options", String.class);
        String[] options = optionsStr.split(",");

        Dialog dialog = new Dialog("Kampus Quiz", skin) {
            @Override
            protected void result(Object object) {
                if (object.equals(answer)) {
                    player.addScore(10);
                    dbManager.updateScore(player.getUsername(), player.getTotalScore());
                    System.out.println("Correct! Score: " + player.getTotalScore());
                } else {
                    System.out.println("Wrong! Correct answer was: " + answer);
                }
                quizActive = false;
            }
        };

        dialog.text(new Label(question, skin));
        for (String option : options) {
            TextButton btn = new TextButton(option, skin);
            dialog.button(btn, option);
        }
        dialog.show(uiStage);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = 800f;
        camera.viewportHeight = 800f * height / width;
        camera.update();
        uiStage.getViewport().update(width, height, true);
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
        playerTexture.dispose();
        map.dispose();
        mapRenderer.dispose();
        uiStage.dispose();
        skin.dispose();
    }
}
