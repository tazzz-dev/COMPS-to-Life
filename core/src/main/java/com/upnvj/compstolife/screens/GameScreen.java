package com.upnvj.compstolife.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.upnvj.compstolife.CompsGame;
import com.upnvj.compstolife.database.DatabaseManager;
import com.upnvj.compstolife.entities.Player;

import com.badlogic.gdx.audio.Sound;

public class GameScreen implements Screen {
    private final CompsGame game;
    private final Player player;
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private Vector2 playerPos;
    private Vector2 targetPos;
    private float moveSpeed = 120f;
    private final float TILE_SIZE = 16f;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;

    private Texture walkSheet;
    private Texture idleSheet;
    private Animation<TextureRegion> walkDown, walkUp, walkLeft, walkRight;
    private Animation<TextureRegion> idleDown, idleUp, idleLeft, idleRight;
    private Animation<TextureRegion> currentAnimation;
    private float stateTime;
    private boolean isMoving = false;

    private enum Direction { DOWN, UP, LEFT, RIGHT }
    private Direction lastDirection = Direction.DOWN;

    private Stage uiStage;
    private Skin skin;
    private boolean quizActive = false;
    private boolean showCustomDialog = false;
    private DatabaseManager dbManager;

    // Transition variables
    private float fadeAlpha = 1.0f;
    private float fadeSpeed = 0.5f;

    // NPC and Sound properties
    private Vector2 npcPos;
    private Vector2 npcAlmetPos;
    private Texture npcAlmetSheet;
    private Animation<TextureRegion> almetDown, almetUp, almetLeft, almetRight;
    private Direction almetDirection = Direction.DOWN;
    private float almetTimer = 0;
    private Texture dialogBoxTexture;
    private Sound runSound;
    private long runSoundId = -1;

    public GameScreen(CompsGame game, String username) {
        this.game = game;
        this.player = new Player(username);
        this.camera = new OrthographicCamera();
        this.shapeRenderer = new ShapeRenderer();

        map = new TmxMapLoader().load("map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, game.batch);

        this.playerPos = new Vector2(46 * TILE_SIZE, 4 * TILE_SIZE);
        this.targetPos = new Vector2(playerPos);

        // NPC Adam at (47,64)
        this.npcPos = new Vector2(47 * TILE_SIZE, 64 * TILE_SIZE);
        // NPC Almet 2 tiles above Adam (47,66)
        this.npcAlmetPos = new Vector2(47 * TILE_SIZE, 66 * TILE_SIZE);
        this.dialogBoxTexture = new Texture(Gdx.files.internal("dialog-box-example.png"));

        // Load Sound
        runSound = Gdx.audio.newSound(Gdx.files.internal("Run.ogg"));

        walkSheet = new Texture(Gdx.files.internal("bob_run.png"));
        idleSheet = new Texture(Gdx.files.internal("bob_idle.png"));
        npcAlmetSheet = new Texture(Gdx.files.internal("almet-stop.png"));

        walkSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        idleSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        npcAlmetSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        int frameWidth = 16;
        int frameHeight = 32;

        TextureRegion[][] walkTmp = TextureRegion.split(walkSheet, frameWidth, frameHeight);
        walkDown = createHorizontalAnimation(walkTmp, 18);
        walkUp = createHorizontalAnimation(walkTmp, 6);
        walkLeft = createHorizontalAnimation(walkTmp, 12);
        walkRight = createHorizontalAnimation(walkTmp, 0);

        TextureRegion[][] idleTmp = TextureRegion.split(idleSheet, frameWidth, frameHeight);
        idleDown = createHorizontalAnimation(idleTmp, 18);
        idleUp = createHorizontalAnimation(idleTmp, 6);
        idleLeft = createHorizontalAnimation(idleTmp, 12);
        idleRight = createHorizontalAnimation(idleTmp, 0);

        // Almet Animations (assuming 4 frames: DOWN, UP, LEFT, RIGHT)
        int almetFrameWidth = npcAlmetSheet.getWidth() / 4;
        int almetFrameHeight = npcAlmetSheet.getHeight();

        if (almetFrameWidth > 0 && almetFrameHeight > 0) {
            TextureRegion[][] almetTmp = TextureRegion.split(npcAlmetSheet, almetFrameWidth, almetFrameHeight);
            if (almetTmp.length > 0 && almetTmp[0].length >= 4) {
                almetDown = new Animation<>(0.1f, almetTmp[0][0]);
                almetUp = new Animation<>(0.1f, almetTmp[0][1]);
                almetLeft = new Animation<>(0.1f, almetTmp[0][2]);
                almetRight = new Animation<>(0.1f, almetTmp[0][3]);
            } else {
                // Fallback if not enough frames
                TextureRegion fullRegion = new TextureRegion(npcAlmetSheet);
                almetDown = almetUp = almetLeft = almetRight = new Animation<>(0.1f, fullRegion);
            }
        } else {
            // Fallback if texture is too small to split into 4
            TextureRegion fullRegion = new TextureRegion(npcAlmetSheet);
            almetDown = almetUp = almetLeft = almetRight = new Animation<>(0.1f, fullRegion);
        }

        currentAnimation = idleDown;
        stateTime = 0f;

        this.uiStage = new Stage(new ScreenViewport());
        this.skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        this.dbManager = new DatabaseManager();
    }

    private Animation<TextureRegion> createHorizontalAnimation(TextureRegion[][] tmp, int startCol) {
        TextureRegion[] frames = new TextureRegion[6];
        System.arraycopy(tmp[0], startCol, frames, 0, 6);
        return new Animation<>(0.1f, frames);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(uiStage);
    }

    @Override
    public void render(float delta) {
        if (!quizActive && !showCustomDialog) {
            handleInput(delta);
            update(delta);
        } else if (showCustomDialog) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                showCustomDialog = false;
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();

        drawGrid();

        game.batch.begin();
        stateTime += delta;

        // Rotation logic for Almet
        almetTimer += delta;
        if (almetTimer >= 1.0f) {
            almetTimer = 0;
            switch (almetDirection) {
                case DOWN: almetDirection = Direction.RIGHT; break;
                case RIGHT: almetDirection = Direction.UP; break;
                case UP: almetDirection = Direction.LEFT; break;
                case LEFT: almetDirection = Direction.DOWN; break;
            }
        }

        TextureRegion npcFrame = idleDown.getKeyFrame(stateTime, true);
        game.batch.draw(npcFrame, npcPos.x, npcPos.y, 16, 32);

        Animation<TextureRegion> almetAnim;
        switch (almetDirection) {
            case UP: almetAnim = almetUp; break;
            case LEFT: almetAnim = almetLeft; break;
            case RIGHT: almetAnim = almetRight; break;
            default: almetAnim = almetDown; break;
        }
        TextureRegion almetFrame = almetAnim.getKeyFrame(stateTime, true);
        game.batch.draw(almetFrame, npcAlmetPos.x, npcAlmetPos.y, 16, 32);

        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);
        game.batch.draw(currentFrame, playerPos.x, playerPos.y, 16, 32);
        game.batch.end();

        uiStage.act(delta);
        uiStage.draw();

        if (showCustomDialog) {
            game.batch.setProjectionMatrix(uiStage.getCamera().combined);
            game.batch.begin();
            float screenWidth = Gdx.graphics.getWidth();
            float screenHeight = Gdx.graphics.getHeight();
            float dialogWidth = screenWidth * 0.9f;
            float dialogHeight = (dialogWidth / dialogBoxTexture.getWidth()) * dialogBoxTexture.getHeight();
            float x = (screenWidth - dialogWidth) / 2;
            float y = 20;
            game.batch.draw(dialogBoxTexture, x, y, dialogWidth, dialogHeight);
            game.batch.end();
        }

        if (fadeAlpha > 0) {
            fadeAlpha -= delta * fadeSpeed;
            if (fadeAlpha < 0) fadeAlpha = 0;

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, fadeAlpha);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    private void drawGrid() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(new Color(1, 1, 1, 0.2f));

        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("Tile Layer 1");
        if (layer != null) {
            int width = layer.getWidth();
            int height = layer.getHeight();
            for (float x = 0; x <= width * TILE_SIZE; x += TILE_SIZE) {
                shapeRenderer.line(x, 0, x, height * TILE_SIZE);
            }
            for (float y = 0; y <= height * TILE_SIZE; y += TILE_SIZE) {
                shapeRenderer.line(0, y, width * TILE_SIZE, y);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void handleInput(float delta) {
        if (isMoving) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            checkInteraction();
        }

        float nextX = playerPos.x;
        float nextY = playerPos.y;
        boolean attemptingMove = false;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            nextX -= TILE_SIZE;
            lastDirection = Direction.LEFT;
            currentAnimation = walkLeft;
            attemptingMove = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            nextX += TILE_SIZE;
            lastDirection = Direction.RIGHT;
            currentAnimation = walkRight;
            attemptingMove = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            nextY += TILE_SIZE;
            lastDirection = Direction.UP;
            currentAnimation = walkUp;
            attemptingMove = true;
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            nextY -= TILE_SIZE;
            lastDirection = Direction.DOWN;
            currentAnimation = walkDown;
            attemptingMove = true;
        }

        if (attemptingMove) {
            if (isCellPassable(nextX, nextY)) {
                targetPos.set(nextX, nextY);
                isMoving = true;
            }
        } else {
            switch (lastDirection) {
                case DOWN: currentAnimation = idleDown; break;
                case UP: currentAnimation = idleUp; break;
                case LEFT: currentAnimation = idleLeft; break;
                case RIGHT: currentAnimation = idleRight; break;
            }
        }
    }

    private void checkInteraction() {
        float distance = playerPos.dst(npcPos);
        if (distance <= TILE_SIZE * 1.5f) {
            showCustomDialog = true;
        }
    }

    private boolean isCellPassable(float x, float y) {
        int cellX = (int) (x / TILE_SIZE);
        int cellY = (int) (y / TILE_SIZE);
        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("Tile Layer 1");
        if (layer == null) return true;
        if (cellX < 0 || cellX >= layer.getWidth() || cellY < 0 || cellY >= layer.getHeight()) return false;
        Cell cell = layer.getCell(cellX, cellY);
        if (cell == null) return false;
        Object blocked = cell.getTile().getProperties().get("blocked");
        if (blocked != null) return false;
        return true;
    }

    private void update(float delta) {
        if (isMoving) {
            // Play running sound if not already playing
            if (runSoundId == -1) {
                runSoundId = runSound.loop(1.0f);
            }

            float moveAmount = moveSpeed * delta;
            if (playerPos.x < targetPos.x) playerPos.x = Math.min(playerPos.x + moveAmount, targetPos.x);
            else if (playerPos.x > targetPos.x) playerPos.x = Math.max(playerPos.x - moveAmount, targetPos.x);
            else if (playerPos.y < targetPos.y) playerPos.y = Math.min(playerPos.y + moveAmount, targetPos.y);
            else if (playerPos.y > targetPos.y) playerPos.y = Math.max(playerPos.y - moveAmount, targetPos.y);

            if (playerPos.epsilonEquals(targetPos, 1f)) {
                playerPos.set(targetPos);
                isMoving = false;
                // Stop sound when target reached
                runSound.stop(runSoundId);
                runSoundId = -1;
            }
        }
        camera.position.set(playerPos.x + 16, playerPos.y + 16, 0);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = 400;
        camera.viewportHeight = 400f * height / width;
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
        if (runSoundId != -1) {
            runSound.stop(runSoundId);
            runSoundId = -1;
        }
    }

    @Override
    public void dispose() {
        if (walkSheet != null) walkSheet.dispose();
        if (idleSheet != null) idleSheet.dispose();
        if (npcAlmetSheet != null) npcAlmetSheet.dispose();
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (dialogBoxTexture != null) dialogBoxTexture.dispose();
        if (runSound != null) runSound.dispose();
        uiStage.dispose();
        skin.dispose();
    }
}
