package com.upnvj.compstolife.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.upnvj.compstolife.CompsGame;
import com.upnvj.compstolife.database.DatabaseManager;
import com.upnvj.compstolife.entities.Player;

public class GameScreen implements Screen {
    private final CompsGame game;
    private final Player player;
    private OrthographicCamera camera;
    private Vector2 playerPos;
    private float moveSpeed = 200f;

    private Texture walkSheet;
    private Texture idleSheet;
    private Animation<TextureRegion> walkDown, walkUp, walkLeft, walkRight;
    private Animation<TextureRegion> idleDown, idleUp, idleLeft, idleRight;
    private Animation<TextureRegion> currentAnimation;
    private float stateTime;
    private boolean isMoving;
    
    private enum Direction { DOWN, UP, LEFT, RIGHT }
    private Direction lastDirection = Direction.DOWN;

    private Stage uiStage;
    private Skin skin;
    private boolean quizActive = false;
    private DatabaseManager dbManager;

    // Map constants
    private final int TILE_SIZE = 32;
    private final int MAP_WIDTH = 20;  // 20 tiles wide
    private final int MAP_HEIGHT = 20; // 20 tiles high
    private Texture whitePixel;
    private Texture blackPixel;

    public GameScreen(CompsGame game, String username) {
        this.game = game;
        this.player = new Player(username);
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, 800, 480);
        
        // Start in the middle of the white area
        this.playerPos = new Vector2(MAP_WIDTH * TILE_SIZE / 2f, MAP_HEIGHT * TILE_SIZE / 2f);

        // Load Spritesheets
        walkSheet = new Texture(Gdx.files.internal("bob_run.png"));
        idleSheet = new Texture(Gdx.files.internal("bob_idle.png"));
        
        walkSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        idleSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        int frameWidth = 16;
        int frameHeight = 32;
        
        // Split Walk Animations
        TextureRegion[][] walkTmp = TextureRegion.split(walkSheet, frameWidth, frameHeight);
        walkDown = createHorizontalAnimation(walkTmp, 18); // Swapped: 18-23
        walkUp = createHorizontalAnimation(walkTmp, 6);   // 6-11
        walkLeft = createHorizontalAnimation(walkTmp, 12); // 12-17
        walkRight = createHorizontalAnimation(walkTmp, 0);  // Swapped: 0-5

        // Split Idle Animations
        TextureRegion[][] idleTmp = TextureRegion.split(idleSheet, frameWidth, frameHeight);
        idleDown = createHorizontalAnimation(idleTmp, 18); // Swapped: 18-23
        idleUp = createHorizontalAnimation(idleTmp, 6);   // 6-11
        idleLeft = createHorizontalAnimation(idleTmp, 12); // 12-17
        idleRight = createHorizontalAnimation(idleTmp, 0);  // Swapped: 0-5

        currentAnimation = idleDown;
        stateTime = 0f;

        // Map textures
        whitePixel = createPixelTexture(1, 1, 1, 1);
        blackPixel = createPixelTexture(0, 0, 0, 1);

        this.uiStage = new Stage(new ScreenViewport());
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        this.dbManager = new DatabaseManager();
    }

    private Animation<TextureRegion> createHorizontalAnimation(TextureRegion[][] tmp, int startCol) {
        TextureRegion[] frames = new TextureRegion[6];
        System.arraycopy(tmp[0], startCol, frames, 0, 6);
        return new Animation<>(0.1f, frames);
    }

    private Texture createPixelTexture(float r, float g, float b, float a) {
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
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

        Gdx.gl.glClearColor(0, 0, 0, 1); // Background black
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Draw Map
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (x == 0 || x == MAP_WIDTH - 1 || y == 0 || y == MAP_HEIGHT - 1) {
                    game.batch.draw(blackPixel, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                } else {
                    game.batch.draw(whitePixel, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                }
            }
        }

        stateTime += delta;
        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);
        
        // Draw character
        game.batch.draw(currentFrame, playerPos.x, playerPos.y, 32, 64);
        
        game.batch.end();

        uiStage.act(delta);
        uiStage.draw();
    }

    private void handleInput(float delta) {
        isMoving = false;
        float nextX = playerPos.x;
        float nextY = playerPos.y;
        float moveAmount = moveSpeed * delta;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            nextX -= moveAmount;
            currentAnimation = walkLeft;
            lastDirection = Direction.LEFT;
            isMoving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            nextX += moveAmount;
            currentAnimation = walkRight;
            lastDirection = Direction.RIGHT;
            isMoving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            nextY += moveAmount;
            currentAnimation = walkUp;
            lastDirection = Direction.UP;
            isMoving = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            nextY -= moveAmount;
            currentAnimation = walkDown;
            lastDirection = Direction.DOWN;
            isMoving = true;
        }

        if (!isMoving) {
            switch (lastDirection) {
                case DOWN: currentAnimation = idleDown; break;
                case UP: currentAnimation = idleUp; break;
                case LEFT: currentAnimation = idleLeft; break;
                case RIGHT: currentAnimation = idleRight; break;
            }
        }

        // Collision detection with borders
        float playerWidth = 24;
        float playerHeight = 24; 
        float offsetX = (32 - playerWidth) / 2;

        if (isPassable(nextX + offsetX, nextY) && 
            isPassable(nextX + offsetX + playerWidth, nextY) &&
            isPassable(nextX + offsetX, nextY + playerHeight) &&
            isPassable(nextX + offsetX + playerWidth, nextY + playerHeight)) {
            playerPos.x = nextX;
            playerPos.y = nextY;
        }
    }

    private boolean isPassable(float x, float y) {
        int tileX = (int) (x / TILE_SIZE);
        int tileY = (int) (y / TILE_SIZE);
        
        if (tileX <= 0 || tileX >= MAP_WIDTH - 1 || tileY <= 0 || tileY >= MAP_HEIGHT - 1) {
            return false;
        }
        return true;
    }

    private void update(float delta) {
        camera.position.set(playerPos.x + 16, playerPos.y + 32, 0);
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
        if (walkSheet != null) walkSheet.dispose();
        if (idleSheet != null) idleSheet.dispose();
        if (whitePixel != null) whitePixel.dispose();
        if (blackPixel != null) blackPixel.dispose();
        uiStage.dispose();
        skin.dispose();
    }
}
