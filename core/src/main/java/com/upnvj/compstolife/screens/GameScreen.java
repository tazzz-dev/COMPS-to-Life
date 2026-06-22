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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class GameScreen implements Screen {
    private final CompsGame game;
    private final Player player;
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private Vector2 playerPos;
    private Vector2 targetPos;
    private float moveSpeed = 120f;
    private final float TILE_SIZE = 16f;
    private float playerOffsetX = 0f; // Gunakan ini untuk menggeser posisi horizontal karakter jika kurang pas di tengah tile (misal: 4f, -4f, 8f, -8f)

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
    private String currentMapName = "map/map-upnvj.tmx";
    private boolean isTransitioning = false;
    private boolean fadingOut = false;
    private String nextMapName = null;
    private Vector2 nextPlayerPos = new Vector2();

    // NPC and Sound properties
    private Vector2 npcPos;
    private Vector2 npcAlmetPos;
    private Texture npcAlmetSheet;
    private Animation<TextureRegion> almetDown, almetUp, almetLeft, almetRight;
    private Direction almetDirection = Direction.DOWN;
    private float almetTimer = 0;

    // Arka and Arya NPCs
    private Vector2 npcArkaPos;
    private Vector2 npcAryaPos;
    private Texture npcArkaTexture;
    private Texture npcAryaTexture;

    // Extra NPCs
    private List<GameNPC> extraNpcs;

    // Custom NPC Dialogue Flow
    private Map<String, NpcDialogueFlow> npcDialogueFlows;
    private DialogueState activeNpcDialog = null;
    private boolean npcDialogActive = false;

    // UI elements for NPC dialogue
    private Image npcDialogImage;
    private Table npcQuizButtonsTable;
    private ImageButton npcQzButtonA;
    private ImageButton npcQzButtonB;
    private ImageButton npcQzButtonC;

    private Texture dialogBoxTexture;
    private Label dialogLabel;
    private String currentDialogText = "";
    private Sound runSound;
    private long runSoundId = -1;

    // Pause components
    private boolean isPaused = false;
    private ImageButton pauseButton;
    private Table pauseMenuTable;
    private ImageButton resumeButton;
    private ImageButton settingButton;
    private ImageButton backMenuButton;
    private Texture pauseNormalTex, pauseHoverTex;
    private Texture resumeNormalTex, resumeHoverTex;
    private Texture settingNormalTex, settingHoverTex;
    private Texture backMenuNormalTex, backMenuHoverTex;
    private Texture pauseTitleTex;
    private Sound hoverSound;
    private Sound clickSound;

    // Quiz components
    private Table quizContainerTable;
    private Table quizTable;
    private Texture quizBgTex;
    private Texture qzA_NormalTex, qzA_HoverTex;
    private Texture qzB_NormalTex, qzB_HoverTex;
    private Texture qzC_NormalTex, qzC_HoverTex;
    private ImageButton qzButtonA, qzButtonB, qzButtonC;
    private Label quizQuestionLabel;
    private Label quizOptionALabel;
    private Label quizOptionBLabel;
    private Label quizOptionCLabel;
    private boolean quizAnswered = false;
    private boolean quizSuccess = false;
    private Label coordLabel;

    public GameScreen(CompsGame game, String username) {
        this.game = game;
        this.player = new Player(username);
        this.camera = new OrthographicCamera();
        this.shapeRenderer = new ShapeRenderer();

        map = new TmxMapLoader().load("map/map-upnvj.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map, game.batch);


        this.playerPos = new Vector2(79 * TILE_SIZE, 133 * TILE_SIZE);
        this.targetPos = new Vector2(playerPos);

        // NPC Adam at (48,63) - adjusted to passable tile
        this.npcPos = new Vector2(48 * TILE_SIZE, 63 * TILE_SIZE);
        // NPC Almet at (48,65) - adjusted to passable tile
        this.npcAlmetPos = new Vector2(48 * TILE_SIZE, 65 * TILE_SIZE);

        // NPC Arka at (90, 114)
        this.npcArkaPos = new Vector2(90 * TILE_SIZE, 114 * TILE_SIZE);
        // NPC Arya at (80, 132)
        this.npcAryaPos = new Vector2(80 * TILE_SIZE, 132 * TILE_SIZE);

        // Initialize extra NPCs
        this.extraNpcs = new ArrayList<>();
        this.extraNpcs.add(new GameNPC("Ayu", 80f, 115f, "sprite/ayu.png", "Ayu: Halo! Aku Ayu. Jangan lupa untuk mengerjakan tugas kuliahmu tepat waktu ya!", TILE_SIZE, false));
        this.extraNpcs.add(new GameNPC("Nadhifa", 95f, 121f, "sprite/nadhifa.png", "Nadhifa: Hai! Aku Nadhifa. Selamat datang di Fakultas Ilmu Komputer!", TILE_SIZE, false));
        this.extraNpcs.add(new GameNPC("Nadia", 14f, 17f, "sprite/nadia.png", "Nadia: Halo! Aku Nadia. Senang sekali melihatmu bersemangat menjelajahi kampus ini!", TILE_SIZE, true));
        this.extraNpcs.add(new GameNPC("Pak Hendra", 45f, 40f, "sprite/pak-hendra.png", "Pak Hendra: Selamat pagi mahasiswa sekalian. Ingat, kegagalan hari ini adalah awal dari kesuksesan!", TILE_SIZE, true));
        this.extraNpcs.add(new GameNPC("Reyhan", 76f, 104f, "sprite/reyhan.png", "Reyhan: Hei! Aku Reyhan. Sudahkah kamu memeriksa jadwal kuliah hari ini?", TILE_SIZE, false));
        this.extraNpcs.add(new GameNPC("Rizky", 40f, 17f, "sprite/rizky.png", "Rizky: Halo bro! Aku Rizky. Jangan lupa minum air putih yang cukup ya kalau sedang coding.", TILE_SIZE, true));
        this.extraNpcs.add(new GameNPC("Salsa", 42f, 44f, "sprite/salsa.png", "Salsa: Hai! Aku Salsa. Semoga harimu menyenangkan dan perkuliahanmu berjalan lancar!", TILE_SIZE, true));
        this.extraNpcs.add(new GameNPC("Tasya", 75f, 70f, "sprite/tasya.png", "Tasya: Halo! Aku Tasya. Perpustakaan ada di dekat sini, belajarlah dengan rajin!", TILE_SIZE, false));
        this.extraNpcs.add(new GameNPC("Zaki", 107f, 106f, "sprite/zaki.png", "Zaki: Yo! Aku Zaki. Main game boleh saja, tapi jangan sampai melupakan tugas utama kita sebagai mahasiswa.", TILE_SIZE, false));

        this.dialogBoxTexture = new Texture(Gdx.files.internal("dialog/dialog-box.png"));

        // Load Sound
        runSound = Gdx.audio.newSound(Gdx.files.internal("Run.ogg"));

        walkSheet = new Texture(Gdx.files.internal("sprite/bob_run.png"));
        idleSheet = new Texture(Gdx.files.internal("sprite/bob_idle.png"));
        npcAlmetSheet = new Texture(Gdx.files.internal("sprite/almet-stop.png"));
        npcArkaTexture = new Texture(Gdx.files.internal("sprite/arka.png"));
        npcAryaTexture = new Texture(Gdx.files.internal("sprite/arya.png"));

        walkSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        idleSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        npcAlmetSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        npcArkaTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        npcAryaTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

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

        this.dialogLabel = new Label("", skin, "white");
        this.dialogLabel.setWrap(true);
        this.dialogLabel.setAlignment(Align.topLeft);
        this.dialogLabel.setVisible(false);
        this.uiStage.addActor(dialogLabel);

        // Initialize textures for buttons
        pauseNormalTex = new Texture(Gdx.files.internal("btn/button-pause-normal.png"));
        pauseHoverTex = new Texture(Gdx.files.internal("btn/button-pause-hover.png"));
        resumeNormalTex = new Texture(Gdx.files.internal("btn/button-resume-normal.png"));
        resumeHoverTex = new Texture(Gdx.files.internal("btn/button-resume-hover.png"));
        settingNormalTex = new Texture(Gdx.files.internal("btn/button-setting-normal.png"));
        settingHoverTex = new Texture(Gdx.files.internal("btn/button-setting-hover.png"));
        backMenuNormalTex = new Texture(Gdx.files.internal("btn/button-backmenu-normal.png"));
        backMenuHoverTex = new Texture(Gdx.files.internal("btn/button-backmenu-hover.png"));

        // Set filters
        pauseNormalTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pauseHoverTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        resumeNormalTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        resumeHoverTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        settingNormalTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        settingHoverTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        backMenuNormalTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        backMenuHoverTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Load UI Sounds
        hoverSound = Gdx.audio.newSound(Gdx.files.internal("hover.ogg"));
        clickSound = Gdx.audio.newSound(Gdx.files.internal("click.ogg"));

        // Setup Pause Button (Top-Right)
        TextureRegionDrawable pauseNormalDrawable = new TextureRegionDrawable(new TextureRegion(pauseNormalTex));
        TextureRegionDrawable pauseHoverDrawable = new TextureRegionDrawable(new TextureRegion(pauseHoverTex));
        ImageButton.ImageButtonStyle pauseStyle = new ImageButton.ImageButtonStyle();
        pauseStyle.imageUp = pauseNormalDrawable;
        pauseStyle.imageOver = pauseHoverDrawable;

        pauseButton = new ImageButton(pauseStyle);
        pauseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                clickSound.play(1.0f);
                isPaused = true;
                pauseButton.setVisible(false);
                pauseMenuTable.setVisible(true);

                // Stop running sound immediately if playing
                if (runSoundId != -1) {
                    runSound.stop(runSoundId);
                    runSoundId = -1;
                }
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    hoverSound.play(1.0f);
                }
            }
        });

        Table pauseButtonTable = new Table();
        pauseButtonTable.setFillParent(true);
        pauseButtonTable.top().right().pad(10);
        pauseButtonTable.add(pauseButton).size(80, 80);
        uiStage.addActor(pauseButtonTable);

        // Setup Coordinate Indicator (Top-Left)
        Table coordTable = new Table();
        coordTable.setFillParent(true);
        coordTable.top().left().pad(20);
        coordLabel = new Label("X: 0, Y: 0", skin, "white");
        coordTable.add(coordLabel);
        uiStage.addActor(coordTable);

        // Setup Pause Menu (Center)
        TextureRegionDrawable resumeNormalDrawable = new TextureRegionDrawable(new TextureRegion(resumeNormalTex));
        TextureRegionDrawable resumeHoverDrawable = new TextureRegionDrawable(new TextureRegion(resumeHoverTex));
        ImageButton.ImageButtonStyle resumeStyle = new ImageButton.ImageButtonStyle();
        resumeStyle.imageUp = resumeNormalDrawable;
        resumeStyle.imageOver = resumeHoverDrawable;

        resumeButton = new ImageButton(resumeStyle);
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                clickSound.play(1.0f);
                isPaused = false;
                pauseButton.setVisible(true);
                pauseMenuTable.setVisible(false);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    hoverSound.play(1.0f);
                }
            }
        });

        TextureRegionDrawable settingNormalDrawable = new TextureRegionDrawable(new TextureRegion(settingNormalTex));
        TextureRegionDrawable settingHoverDrawable = new TextureRegionDrawable(new TextureRegion(settingHoverTex));
        ImageButton.ImageButtonStyle settingStyle = new ImageButton.ImageButtonStyle();
        settingStyle.imageUp = settingNormalDrawable;
        settingStyle.imageOver = settingHoverDrawable;

        settingButton = new ImageButton(settingStyle);
        settingButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                clickSound.play(1.0f);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    hoverSound.play(1.0f);
                }
            }
        });

        TextureRegionDrawable backMenuNormalDrawable = new TextureRegionDrawable(new TextureRegion(backMenuNormalTex));
        TextureRegionDrawable backMenuHoverDrawable = new TextureRegionDrawable(new TextureRegion(backMenuHoverTex));
        ImageButton.ImageButtonStyle backMenuStyle = new ImageButton.ImageButtonStyle();
        backMenuStyle.imageUp = backMenuNormalDrawable;
        backMenuStyle.imageOver = backMenuHoverDrawable;

        backMenuButton = new ImageButton(backMenuStyle);
        backMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                clickSound.play(1.0f);
                isPaused = false;
                game.setScreen(new MainMenuScreen(game));
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    hoverSound.play(1.0f);
                }
            }
        });

        pauseTitleTex = new Texture(Gdx.files.internal("art-title.png"));
        pauseTitleTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        Image pauseTitleImage = new Image(pauseTitleTex);

        pauseMenuTable = new Table();
        pauseMenuTable.setFillParent(true);
        pauseMenuTable.center();
        pauseMenuTable.add(pauseTitleImage).size(450, 300).padBottom(15).row();
        pauseMenuTable.add(resumeButton).size(270, 104).padBottom(15).row();
        pauseMenuTable.add(settingButton).size(270, 104).padBottom(15).row();
        pauseMenuTable.add(backMenuButton).size(270, 104);
        pauseMenuTable.setVisible(false); // Hidden by default
        uiStage.addActor(pauseMenuTable);

        // Initialize Quiz Textures
        quizBgTex = new Texture(Gdx.files.internal("quiz/qz-example.png"));
        qzA_NormalTex = new Texture(Gdx.files.internal("quiz/qz-button-A-normal.png"));
        qzA_HoverTex = new Texture(Gdx.files.internal("quiz/qz-button-A-hover.png"));
        qzB_NormalTex = new Texture(Gdx.files.internal("quiz/qz-button-B-normal.png"));
        qzB_HoverTex = new Texture(Gdx.files.internal("quiz/qz-button-B-hover.png"));
        qzC_NormalTex = new Texture(Gdx.files.internal("quiz/qz-button-C-normal.png"));
        qzC_HoverTex = new Texture(Gdx.files.internal("quiz/qz-button-C-hover.png"));

        quizBgTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        qzA_NormalTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        qzA_HoverTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        qzB_NormalTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        qzB_HoverTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        qzC_NormalTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        qzC_HoverTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // Initialize Quiz Labels
        quizQuestionLabel = new Label("Manakah yang merupakan komponen utama CPU?", skin, "white");
        quizQuestionLabel.setWrap(true);
        quizQuestionLabel.setAlignment(Align.center);

        quizOptionALabel = new Label("Register, ALU, dan Control Unit", skin, "white");
        quizOptionALabel.setWrap(true);
        quizOptionALabel.setAlignment(Align.left);

        quizOptionBLabel = new Label("RAM, ROM, dan Harddisk", skin, "white");
        quizOptionBLabel.setWrap(true);
        quizOptionBLabel.setAlignment(Align.left);

        quizOptionCLabel = new Label("Monitor, Keyboard, dan Mouse", skin, "white");
        quizOptionCLabel.setWrap(true);
        quizOptionCLabel.setAlignment(Align.left);

        // Initialize Quiz Buttons
        TextureRegionDrawable drawableANormal = new TextureRegionDrawable(new TextureRegion(qzA_NormalTex));
        TextureRegionDrawable drawableAHover = new TextureRegionDrawable(new TextureRegion(qzA_HoverTex));
        ImageButton.ImageButtonStyle styleA = new ImageButton.ImageButtonStyle();
        styleA.imageUp = drawableANormal;
        styleA.imageOver = drawableAHover;
        qzButtonA = new ImageButton(styleA);

        TextureRegionDrawable drawableBNormal = new TextureRegionDrawable(new TextureRegion(qzB_NormalTex));
        TextureRegionDrawable drawableBHover = new TextureRegionDrawable(new TextureRegion(qzB_HoverTex));
        ImageButton.ImageButtonStyle styleB = new ImageButton.ImageButtonStyle();
        styleB.imageUp = drawableBNormal;
        styleB.imageOver = drawableBHover;
        qzButtonB = new ImageButton(styleB);

        TextureRegionDrawable drawableCNormal = new TextureRegionDrawable(new TextureRegion(qzC_NormalTex));
        TextureRegionDrawable drawableCHover = new TextureRegionDrawable(new TextureRegion(qzC_HoverTex));
        ImageButton.ImageButtonStyle styleC = new ImageButton.ImageButtonStyle();
        styleC.imageUp = drawableCNormal;
        styleC.imageOver = drawableCHover;
        qzButtonC = new ImageButton(styleC);

        // Add Listeners to Buttons
        qzButtonA.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleQuizAnswer('A');
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    hoverSound.play(1.0f);
                }
            }
        });

        qzButtonB.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleQuizAnswer('B');
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    hoverSound.play(1.0f);
                }
            }
        });

        qzButtonC.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleQuizAnswer('C');
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) {
                    hoverSound.play(1.0f);
                }
            }
        });

        // Initialize Table Containers
        quizContainerTable = new Table();
        quizContainerTable.setFillParent(true);
        quizContainerTable.center();
        quizContainerTable.setVisible(false);

        quizTable = new Table();
        quizContainerTable.add(quizTable);
        uiStage.addActor(quizContainerTable);

        // Build the table layout initially
        rebuildQuizTable(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Initialize NPC Dialogue Flow
        initNpcDialogueFlows();

        // NPC dialogue UI components
        npcDialogImage = new Image();

        TextureRegionDrawable npcDrawableANormal = new TextureRegionDrawable(new TextureRegion(qzA_NormalTex));
        TextureRegionDrawable npcDrawableAHover = new TextureRegionDrawable(new TextureRegion(qzA_HoverTex));
        ImageButton.ImageButtonStyle npcStyleA = new ImageButton.ImageButtonStyle();
        npcStyleA.imageUp = npcDrawableANormal;
        npcStyleA.imageOver = npcDrawableAHover;
        npcQzButtonA = new ImageButton(npcStyleA);

        TextureRegionDrawable npcDrawableBNormal = new TextureRegionDrawable(new TextureRegion(qzB_NormalTex));
        TextureRegionDrawable npcDrawableBHover = new TextureRegionDrawable(new TextureRegion(qzB_HoverTex));
        ImageButton.ImageButtonStyle npcStyleB = new ImageButton.ImageButtonStyle();
        npcStyleB.imageUp = npcDrawableBNormal;
        npcStyleB.imageOver = npcDrawableBHover;
        npcQzButtonB = new ImageButton(npcStyleB);

        TextureRegionDrawable npcDrawableCNormal = new TextureRegionDrawable(new TextureRegion(qzC_NormalTex));
        TextureRegionDrawable npcDrawableCHover = new TextureRegionDrawable(new TextureRegion(qzC_HoverTex));
        ImageButton.ImageButtonStyle npcStyleC = new ImageButton.ImageButtonStyle();
        npcStyleC.imageUp = npcDrawableCNormal;
        npcStyleC.imageOver = npcDrawableCHover;
        npcQzButtonC = new ImageButton(npcStyleC);

        // Add Listeners to NPC Quiz Buttons
        npcQzButtonA.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleNpcQuizAnswer('A');
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) hoverSound.play(1.0f);
            }
        });

        npcQzButtonB.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleNpcQuizAnswer('B');
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) hoverSound.play(1.0f);
            }
        });

        npcQzButtonC.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleNpcQuizAnswer('C');
            }
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                if (pointer == -1) hoverSound.play(1.0f);
            }
        });

        npcQuizButtonsTable = new Table();

        npcDialogImage.setVisible(false);
        npcQuizButtonsTable.setVisible(false);
        uiStage.addActor(npcDialogImage);
        uiStage.addActor(npcQuizButtonsTable);

        rebuildNpcDialogLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
        if (coordLabel != null) {
            int tileX = (int) (playerPos.x / TILE_SIZE);
            int tileY = (int) (playerPos.y / TILE_SIZE);
            coordLabel.setText("X: " + tileX + ", Y: " + tileY);
        }

        if (!quizActive && !showCustomDialog && !npcDialogActive && !isPaused && !isTransitioning) {
            handleInput(delta);
            update(delta);
        } else if (showCustomDialog && !isPaused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                showCustomDialog = false;
            }
        } else if (npcDialogActive && !isPaused) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                advanceNpcDialogueFlow();
            }
        } else if (quizActive && !isPaused) {
            if (quizAnswered) {
                if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    if (quizSuccess) {
                        closeQuiz();
                    } else {
                        resetQuiz();
                    }
                }
            }
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        mapRenderer.setView(camera);
        mapRenderer.render();
        drawMapIndicators();
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

        // Render NPCs based on current map
        boolean isSelasar = "map/SelasarFIK.tmx".equals(currentMapName);
        if (!isSelasar) {
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

            // Render Arka and Arya NPCs
            game.batch.draw(npcArkaTexture, npcArkaPos.x, npcArkaPos.y, 16, 32);
            game.batch.draw(npcAryaTexture, npcAryaPos.x, npcAryaPos.y, 16, 32);

            // Render extra NPCs
            if (extraNpcs != null) {
                for (GameNPC npc : extraNpcs) {
                    if (!npc.isOnSelasar()) {
                        npc.draw(game.batch);
                    }
                }
            }
        } else {
            // Render extra Selasar NPCs
            if (extraNpcs != null) {
                for (GameNPC npc : extraNpcs) {
                    if (npc.isOnSelasar()) {
                        npc.draw(game.batch);
                    }
                }
            }
        }

        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);
        game.batch.draw(currentFrame, playerPos.x + playerOffsetX, playerPos.y, 16, 32);
        game.batch.end();

        if (showCustomDialog) {
            float screenWidth = Gdx.graphics.getWidth();
            float screenHeight = Gdx.graphics.getHeight();
            float dialogWidth = screenWidth * 0.9f;
            float dialogHeight = (dialogWidth / dialogBoxTexture.getWidth()) * dialogBoxTexture.getHeight();
            float x = (screenWidth - dialogWidth) / 2;
            float y = 20;

            // Render dialog box texture using the UI projection matrix
            game.batch.setProjectionMatrix(uiStage.getCamera().combined);
            game.batch.begin();
            game.batch.draw(dialogBoxTexture, x, y, dialogWidth, dialogHeight);
            game.batch.end();

            // Align the dialog label text dynamically
            dialogLabel.setPosition(x + 40, y + 25);
            dialogLabel.setSize(dialogWidth - 80, dialogHeight - 50);
            dialogLabel.setVisible(true);
        } else {
            dialogLabel.setVisible(false);
        }



        // Draw pause dim overlay
        if (isPaused || quizActive || npcDialogActive) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(uiStage.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 0.5f); // 50% opacity black
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        uiStage.act(delta);
        uiStage.draw();

        if (isTransitioning) {
            if (fadingOut) {
                fadeAlpha += delta * fadeSpeed;
                if (fadeAlpha >= 1.0f) {
                    fadeAlpha = 1.0f;
                    performMapChange();
                    fadingOut = false;
                }
            } else {
                fadeAlpha -= delta * fadeSpeed;
                if (fadeAlpha <= 0) {
                    fadeAlpha = 0;
                    isTransitioning = false;
                }
            }

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(uiStage.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, fadeAlpha);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        } else if (fadeAlpha > 0) {
            fadeAlpha -= delta * fadeSpeed;
            if (fadeAlpha < 0) fadeAlpha = 0;

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(uiStage.getCamera().combined);
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
        if (isPaused) return;
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
        boolean isSelasar = "map/SelasarFIK.tmx".equals(currentMapName);
        if (!isSelasar) {
            if (playerPos.dst(npcPos) <= TILE_SIZE * 1.5f) {
                currentDialogText = "Adam: Halo! Selamat datang di COMPS to Life. Jelajahi area sekitar untuk menemukan NPC lainnya!";
                dialogLabel.setText(currentDialogText);
                showCustomDialog = true;
            } else if (playerPos.dst(npcAlmetPos) <= TILE_SIZE * 1.5f) {
                openQuiz();
            } else if (playerPos.dst(npcArkaPos) <= TILE_SIZE * 1.5f) {
                startNpcDialogueFlow("Arka");
            } else if (playerPos.dst(npcAryaPos) <= TILE_SIZE * 1.5f) {
                startNpcDialogueFlow("Arya");
            } else if (extraNpcs != null) {
                for (GameNPC npc : extraNpcs) {
                    if (!npc.isOnSelasar() && npc.isNearPlayer(playerPos, TILE_SIZE)) {
                        String key = npc.getName().toLowerCase().replace(" ", "");
                        if (npcDialogueFlows.containsKey(key)) {
                            startNpcDialogueFlow(npc.getName());
                        } else {
                            currentDialogText = npc.getDialogText();
                            dialogLabel.setText(currentDialogText);
                            showCustomDialog = true;
                        }
                        break;
                    }
                }
            }
        } else {
            if (extraNpcs != null) {
                for (GameNPC npc : extraNpcs) {
                    if (npc.isOnSelasar() && npc.isNearPlayer(playerPos, TILE_SIZE)) {
                        String key = npc.getName().toLowerCase().replace(" ", "");
                        if (npcDialogueFlows.containsKey(key)) {
                            startNpcDialogueFlow(npc.getName());
                        } else {
                            currentDialogText = npc.getDialogText();
                            dialogLabel.setText(currentDialogText);
                            showCustomDialog = true;
                        }
                        break;
                    }
                }
            }
        }
    }

    private boolean isCellPassable(float x, float y) {
        int cellX = (int) (x / TILE_SIZE);
        int cellY = (int) (y / TILE_SIZE);

        if ("map/map-upnvj.tmx".equals(currentMapName)) {
            // Override transition portal coordinates to be passable
            if (cellX == 80 && (cellY >= 136 && cellY <= 138)) {
                return true;
            }
            TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("Ground");
            TiledMapTileLayer blockLayer = (TiledMapTileLayer) map.getLayers().get("Block");
            if (layer == null) return true;
            if (cellX < 0 || cellX >= layer.getWidth() || cellY < 0 || cellY >= layer.getHeight()) return false;

            if (blockLayer != null && blockLayer.getCell(cellX, cellY) != null) return false;
            if (layer.getCell(cellX, cellY) == null) return false;
            return true;
        } else if ("map/SelasarFIK.tmx".equals(currentMapName)) {
            // Override door/transition coordinates to be passable
            if (cellX <= 13 && (cellY >= 19 && cellY <= 21)) {
                return true;
            }
            TiledMapTileLayer layer1 = (TiledMapTileLayer) map.getLayers().get("Tile Layer 1");
            if (layer1 == null) return true;
            if (cellX < 0 || cellX >= layer1.getWidth() || cellY < 0 || cellY >= layer1.getHeight()) return false;

            // Ground floor cell must not be empty
            if (layer1.getCell(cellX, cellY) == null) return false;

            // Check Layer 2 (furniture, etc.) and Layer 4 (walls, doors) for blocks
            TiledMapTileLayer layer2 = (TiledMapTileLayer) map.getLayers().get("Tile Layer 2");
            if (layer2 != null && layer2.getCell(cellX, cellY) != null) return false;

            TiledMapTileLayer layer4 = (TiledMapTileLayer) map.getLayers().get("Tile Layer 4");
            if (layer4 != null && layer4.getCell(cellX, cellY) != null) return false;

            return true;
        }
        return true;
    }

    private void checkMapTransition() {
        int tileX = (int) (playerPos.x / TILE_SIZE);
        int tileY = (int) (playerPos.y / TILE_SIZE);
        if ("map/map-upnvj.tmx".equals(currentMapName)) {
            if (tileX == 80) {
                if (tileY == 136) {
                    // Spawn at (11, 21)
                    startMapTransition("map/SelasarFIK.tmx", new Vector2(11 * TILE_SIZE, 21 * TILE_SIZE));
                } else if (tileY == 137) {
                    // Spawn at (11, 20)
                    startMapTransition("map/SelasarFIK.tmx", new Vector2(11 * TILE_SIZE, 20 * TILE_SIZE));
                } else if (tileY == 138) {
                    // Spawn at (11, 19)
                    startMapTransition("map/SelasarFIK.tmx", new Vector2(11 * TILE_SIZE, 19 * TILE_SIZE));
                }
            }
        } else if ("map/SelasarFIK.tmx".equals(currentMapName)) {
            // Exit back to UPNVJ map when stepping on the door/entrance tiles (x <= 10, y = 19..21)
            if (tileX <= 10 && (tileY >= 19 && tileY <= 21)) {
                if (tileY == 21) {
                    startMapTransition("map/map-upnvj.tmx", new Vector2(80 * TILE_SIZE, 136 * TILE_SIZE));
                } else if (tileY == 20) {
                    startMapTransition("map/map-upnvj.tmx", new Vector2(80 * TILE_SIZE, 137 * TILE_SIZE));
                } else {
                    startMapTransition("map/map-upnvj.tmx", new Vector2(80 * TILE_SIZE, 138 * TILE_SIZE));
                }
            }
        }
    }

    private void startMapTransition(String mapName, Vector2 spawnPos) {
        isTransitioning = true;
        fadingOut = true;
        nextMapName = mapName;
        nextPlayerPos.set(spawnPos);
    }

    private void performMapChange() {
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();

        currentMapName = nextMapName;
        map = new TmxMapLoader().load(currentMapName);
        mapRenderer = new OrthogonalTiledMapRenderer(map, game.batch);

        playerPos.set(nextPlayerPos);
        targetPos.set(nextPlayerPos);
        isMoving = false;
    }

    private void drawMapIndicators() {
        if ("map/map-upnvj.tmx".equals(currentMapName)) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            // Black color with 50% opacity for marker
            shapeRenderer.setColor(new Color(0f, 0f, 0f, 0.5f));
            shapeRenderer.rect(80 * TILE_SIZE, 136 * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            shapeRenderer.rect(80 * TILE_SIZE, 137 * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            shapeRenderer.rect(80 * TILE_SIZE, 138 * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    private void update(float delta) {
        if (isPaused) {
            if (runSoundId != -1) {
                runSound.stop(runSoundId);
                runSoundId = -1;
            }
            return;
        }
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
                checkMapTransition();
            }
        }
        camera.position.set(playerPos.x + 8 + playerOffsetX, playerPos.y + 16, 0);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = 400;
        camera.viewportHeight = 400f * height / width;
        camera.update();
        uiStage.getViewport().update(width, height, true);
        rebuildQuizTable(width, height);
        rebuildNpcDialogLayout(width, height);
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
        if (npcArkaTexture != null) npcArkaTexture.dispose();
        if (npcAryaTexture != null) npcAryaTexture.dispose();
        if (extraNpcs != null) {
            for (GameNPC npc : extraNpcs) {
                npc.dispose();
            }
        }
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (dialogBoxTexture != null) dialogBoxTexture.dispose();
        if (runSound != null) runSound.dispose();

        // Dispose pause menu resources
        if (pauseNormalTex != null) pauseNormalTex.dispose();
        if (pauseHoverTex != null) pauseHoverTex.dispose();
        if (resumeNormalTex != null) resumeNormalTex.dispose();
        if (resumeHoverTex != null) resumeHoverTex.dispose();
        if (settingNormalTex != null) settingNormalTex.dispose();
        if (settingHoverTex != null) settingHoverTex.dispose();
        if (backMenuNormalTex != null) backMenuNormalTex.dispose();
        if (backMenuHoverTex != null) backMenuHoverTex.dispose();
        if (pauseTitleTex != null) pauseTitleTex.dispose();
        if (hoverSound != null) hoverSound.dispose();
        if (clickSound != null) clickSound.dispose();

        // Dispose quiz resources
        if (quizBgTex != null) quizBgTex.dispose();
        if (qzA_NormalTex != null) qzA_NormalTex.dispose();
        if (qzA_HoverTex != null) qzA_HoverTex.dispose();
        if (qzB_NormalTex != null) qzB_NormalTex.dispose();
        if (qzB_HoverTex != null) qzB_HoverTex.dispose();
        if (qzC_NormalTex != null) qzC_NormalTex.dispose();
        if (qzC_HoverTex != null) qzC_HoverTex.dispose();

        if (activeNpcDialog != null && activeNpcDialog.currentTexture != null) {
            activeNpcDialog.currentTexture.dispose();
        }

        uiStage.dispose();
        skin.dispose();
    }

    private void openQuiz() {
        quizActive = true;
        quizContainerTable.setVisible(true);
        pauseButton.setVisible(false);
        resetQuiz();
    }

    private void closeQuiz() {
        quizActive = false;
        quizContainerTable.setVisible(false);
        pauseButton.setVisible(true);
    }

    private void handleQuizAnswer(char answer) {
        quizAnswered = true;
        clickSound.play(1.0f);
        if (answer == 'A') {
            quizSuccess = true;
            quizQuestionLabel.setText("Jawaban Benar! Register, ALU, dan Control Unit\nadalah komponen utama dari CPU.\n\n(Tekan ENTER untuk kembali)");
        } else {
            quizSuccess = false;
            quizQuestionLabel.setText("Jawaban Salah! Pilihan " + answer + " bukan komponen utama CPU.\n\n(Tekan ENTER untuk mencoba lagi)");
        }
        rebuildQuizTable(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void resetQuiz() {
        quizQuestionLabel.setText("Manakah yang merupakan komponen utama CPU?");
        quizAnswered = false;
        quizSuccess = false;
        rebuildQuizTable(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void rebuildQuizTable(float width, float height) {
        if (quizTable == null || quizBgTex == null) return;

        quizTable.clear();
        quizTable.setBackground(new TextureRegionDrawable(new TextureRegion(quizBgTex)));

        // Calculate dynamic dimensions
        float quizWidth = width * 0.75f;
        float quizHeight = (quizWidth / quizBgTex.getWidth()) * quizBgTex.getHeight();
        if (quizHeight > height * 0.75f) {
            quizHeight = height * 0.75f;
            quizWidth = (quizHeight / quizBgTex.getHeight()) * quizBgTex.getWidth();
        }

        quizContainerTable.clear();
        quizContainerTable.add(quizTable).size(quizWidth, quizHeight);

        // Adjust padding inside the quiz box so text/buttons don't overlap borders
        quizTable.pad(quizHeight * 0.15f, quizWidth * 0.12f, quizHeight * 0.15f, quizWidth * 0.12f);

        // Add Question
        quizTable.add(quizQuestionLabel).colspan(2).fillX().expandX().padBottom(quizHeight * 0.06f).row();

        if (quizAnswered) {
            return;
        }

        float btnWidth = quizWidth * 0.09f;
        float btnHeight = (btnWidth / qzA_NormalTex.getWidth()) * qzA_NormalTex.getHeight();

        // Option A
        quizTable.add(qzButtonA).size(btnWidth, btnHeight).padBottom(quizHeight * 0.04f).padRight(quizWidth * 0.03f).left();
        quizTable.add(quizOptionALabel).padBottom(quizHeight * 0.04f).left().expandX().fillX().row();

        // Option B
        quizTable.add(qzButtonB).size(btnWidth, btnHeight).padBottom(quizHeight * 0.04f).padRight(quizWidth * 0.03f).left();
        quizTable.add(quizOptionBLabel).padBottom(quizHeight * 0.04f).left().expandX().fillX().row();

        // Option C
        quizTable.add(qzButtonC).size(btnWidth, btnHeight).padRight(quizWidth * 0.03f).left();
        quizTable.add(quizOptionCLabel).left().expandX().fillX();
    }

    private static class GameNPC {
        private final String name;
        private final Vector2 position;
        private final Texture texture;
        private final String dialogText;
        private final boolean onSelasar;

        public GameNPC(String name, float tileX, float tileY, String texturePath, String dialogText, float tileSize, boolean onSelasar) {
            this.name = name;
            this.position = new Vector2(tileX * tileSize, tileY * tileSize);
            this.texture = new Texture(Gdx.files.internal(texturePath));
            this.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            this.dialogText = dialogText;
            this.onSelasar = onSelasar;
        }

        public String getName() {
            return name;
        }

        public boolean isOnSelasar() {
            return onSelasar;
        }

        public void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch) {
            batch.draw(texture, position.x, position.y, 16, 32);
        }

        public boolean isNearPlayer(Vector2 playerPos, float tileSize) {
            return playerPos.dst(position) <= tileSize * 1.5f;
        }

        public String getDialogText() {
            return dialogText;
        }

        public void dispose() {
            if (texture != null) texture.dispose();
        }
    }

    private static class NpcDialogueFlow {
        String name;
        List<String> normals = new ArrayList<>();
        String quiz;
        char answer;
        String right;
        String wrong;
        List<String> posts = new ArrayList<>();

        public NpcDialogueFlow(String name, String[] normals, String quiz, char answer, String right, String wrong, String[] posts) {
            this.name = name;
            if (normals != null) {
                for (String s : normals) this.normals.add(s);
            }
            this.quiz = quiz;
            this.answer = answer;
            this.right = right;
            this.wrong = wrong;
            if (posts != null) {
                for (String s : posts) this.posts.add(s);
            }
        }
    }

    private static class DialogueState {
        NpcDialogueFlow flow;
        int currentStep = 0;
        boolean inQuiz = false;
        boolean quizAnswered = false;
        boolean quizCorrect = false;
        boolean inPostQuiz = false;
        int postQuizStep = 0;
        Texture currentTexture = null;
    }

    private void initNpcDialogueFlows() {
        npcDialogueFlows = new HashMap<>();

        npcDialogueFlows.put("arka", new NpcDialogueFlow("Arka",
            new String[]{"dialog/arka/Arka-1.png", "dialog/arka/Arka-2.png"},
            "dialog/arka/Arka-3-QzJwb=A.png", 'A',
            "dialog/arka/Arka4-right.png", "dialog/arka/Arka-4-wrong.png",
            new String[]{}
        ));

        npcDialogueFlows.put("arya", new NpcDialogueFlow("Arya",
            new String[]{
                "dialog/arya/Arya-1.png", "dialog/arya/Arya-2.png", "dialog/arya/Arya-3.png",
                "dialog/arya/Arya-4.png", "dialog/arya/Arya-5.png", "dialog/arya/Arya-6.png",
                "dialog/arya/Arya-7.png", "dialog/arya/Arya-8.png"
            },
            "dialog/arya/Arya-9-QzJwb=B.png", 'B',
            "dialog/arya/Arya-10-right.png", "dialog/arya/Arya-10-wrong.png",
            new String[]{}
        ));

        npcDialogueFlows.put("ayu", new NpcDialogueFlow("Ayu",
            new String[]{"dialog/ayu/Ayu-1.png", "dialog/ayu/Ayu-2.png"},
            "dialog/ayu/Ayu-3-QzJwb=B.png", 'B',
            "dialog/ayu/Ayu-4-right.png", "dialog/ayu/Ayu-4-wrong.png",
            new String[]{}
        ));

        npcDialogueFlows.put("nadhifa", new NpcDialogueFlow("Nadhifa",
            new String[]{"dialog/nadhifa/Nadhifa-1.png", "dialog/nadhifa/Nadhifa-2.png"},
            "dialog/nadhifa/Nadhifa-3-QzJwb=A.png", 'A',
            "dialog/nadhifa/Nadhifa-4-right.png", "dialog/nadhifa/Nadhifa-4-wrong.png",
            new String[]{}
        ));

        npcDialogueFlows.put("nadia", new NpcDialogueFlow("Nadia",
            new String[]{"dialog/nadia/Nadia-1.png", "dialog/nadia/Nadia-2.png"},
            "dialog/nadia/Nadia-3-QzJwb=B.png", 'B',
            "dialog/nadia/Nadia-4-right.png", "dialog/nadia/Nadia-4-wrong.png",
            new String[]{"dialog/nadia/Nadia-5.png"}
        ));

        npcDialogueFlows.put("pakhendra", new NpcDialogueFlow("Pak Hendra",
            new String[]{"dialog/pakhendra/Pak_Hendra-1.png", "dialog/pakhendra/Pak_Hendra-2.png"},
            "dialog/pakhendra/Pak_Hendra -3-QzJwb=C.png", 'C',
            "dialog/pakhendra/Pak_Hendra-4-right.png", "dialog/pakhendra/Pak_Hendra-4-wrong.png",
            new String[]{"dialog/pakhendra/Pak_Hendra 6.png"}
        ));

        npcDialogueFlows.put("reyhan", new NpcDialogueFlow("Reyhan",
            new String[]{"dialog/reyhan/Reyhan-1.png"},
            "dialog/reyhan/Reyhan-2.png", 'A',
            "dialog/reyhan/Reyhan-4-right.png", "dialog/reyhan/Reyhan-4-wrong.png",
            new String[]{}
        ));

        npcDialogueFlows.put("rizky", new NpcDialogueFlow("Rizky",
            new String[]{"dialog/rizky/Rizky-1.png", "dialog/rizky/Rizky-2.png"},
            "dialog/rizky/Rizky-3-QzJwb=A.png", 'A',
            "dialog/rizky/Rizky-4-right.png", "dialog/rizky/Rizky-4-wrong.png",
            new String[]{}
        ));

        npcDialogueFlows.put("salsa", new NpcDialogueFlow("Salsa",
            new String[]{"dialog/salsa/Salsa-1.png", "dialog/salsa/Salsa-2.png"},
            "dialog/salsa/Salsa-3-QzJwb=A.png", 'A',
            "dialog/salsa/Salsa-4-right.png", "dialog/salsa/Salsa-4-wrong.png",
            new String[]{"dialog/salsa/Salsa-5.png"}
        ));

        npcDialogueFlows.put("tasya", new NpcDialogueFlow("Tasya",
            new String[]{"dialog/tasya/Tasya-1.png", "dialog/tasya/Tasya-2.png"},
            "dialog/tasya/Tasya-3-QzJwb=B.png", 'B',
            "dialog/tasya/Tasya-4-right.png", "dialog/tasya/Tasya-4-wrong.png",
            new String[]{}
        ));

        npcDialogueFlows.put("zaki", new NpcDialogueFlow("Zaki",
            new String[]{"dialog/zaki/Zaki-1.png", "dialog/zaki/Zaki-2.png"},
            "dialog/zaki/Zaki-3-QzJwb=B.png", 'B',
            "dialog/zaki/Zaki-4-right.png", "dialog/zaki/Zaki-4-wrong.png",
            new String[]{}
        ));
    }

    private void startNpcDialogueFlow(String npcName) {
        String key = npcName.toLowerCase().replace(" ", "");
        NpcDialogueFlow flow = npcDialogueFlows.get(key);
        if (flow == null) return;

        activeNpcDialog = new DialogueState();
        activeNpcDialog.flow = flow;
        activeNpcDialog.currentStep = 0;
        activeNpcDialog.inQuiz = false;
        activeNpcDialog.quizAnswered = false;
        activeNpcDialog.inPostQuiz = false;

        if (flow.normals.size() > 0) {
            activeNpcDialog.currentTexture = new Texture(Gdx.files.internal(flow.normals.get(0)));
            activeNpcDialog.currentTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            npcDialogImage.setDrawable(new TextureRegionDrawable(new TextureRegion(activeNpcDialog.currentTexture)));
            npcDialogImage.setVisible(true);
            npcQuizButtonsTable.setVisible(false);
            npcDialogActive = true;
            pauseButton.setVisible(false);

            rebuildNpcDialogLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    }

    private void advanceNpcDialogueFlow() {
        if (activeNpcDialog == null) return;

        NpcDialogueFlow flow = activeNpcDialog.flow;

        if (activeNpcDialog.inQuiz) {
            return;
        }

        if (activeNpcDialog.quizAnswered) {
            if (activeNpcDialog.quizCorrect) {
                if (flow.posts.size() > 0) {
                    activeNpcDialog.quizAnswered = false;
                    activeNpcDialog.inPostQuiz = true;
                    activeNpcDialog.postQuizStep = 0;

                    if (activeNpcDialog.currentTexture != null) {
                        activeNpcDialog.currentTexture.dispose();
                    }
                    activeNpcDialog.currentTexture = new Texture(Gdx.files.internal(flow.posts.get(0)));
                    activeNpcDialog.currentTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    npcDialogImage.setDrawable(new TextureRegionDrawable(new TextureRegion(activeNpcDialog.currentTexture)));
                    rebuildNpcDialogLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                } else {
                    closeNpcDialogueFlow();
                }
            } else {
                closeNpcDialogueFlow();
            }
            return;
        }

        if (activeNpcDialog.inPostQuiz) {
            activeNpcDialog.postQuizStep++;
            if (activeNpcDialog.postQuizStep < flow.posts.size()) {
                if (activeNpcDialog.currentTexture != null) {
                    activeNpcDialog.currentTexture.dispose();
                }
                activeNpcDialog.currentTexture = new Texture(Gdx.files.internal(flow.posts.get(activeNpcDialog.postQuizStep)));
                activeNpcDialog.currentTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                npcDialogImage.setDrawable(new TextureRegionDrawable(new TextureRegion(activeNpcDialog.currentTexture)));
                rebuildNpcDialogLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            } else {
                closeNpcDialogueFlow();
            }
            return;
        }

        activeNpcDialog.currentStep++;
        if (activeNpcDialog.currentStep < flow.normals.size()) {
            if (activeNpcDialog.currentTexture != null) {
                activeNpcDialog.currentTexture.dispose();
            }
            activeNpcDialog.currentTexture = new Texture(Gdx.files.internal(flow.normals.get(activeNpcDialog.currentStep)));
            activeNpcDialog.currentTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            npcDialogImage.setDrawable(new TextureRegionDrawable(new TextureRegion(activeNpcDialog.currentTexture)));
            rebuildNpcDialogLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } else if (flow.quiz != null) {
            activeNpcDialog.inQuiz = true;
            if (activeNpcDialog.currentTexture != null) {
                activeNpcDialog.currentTexture.dispose();
            }
            activeNpcDialog.currentTexture = new Texture(Gdx.files.internal(flow.quiz));
            activeNpcDialog.currentTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            npcDialogImage.setDrawable(new TextureRegionDrawable(new TextureRegion(activeNpcDialog.currentTexture)));
            rebuildNpcDialogLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } else {
            closeNpcDialogueFlow();
        }
    }

    private void handleNpcQuizAnswer(char answer) {
        if (activeNpcDialog == null || !activeNpcDialog.inQuiz) return;

        clickSound.play(1.0f);
        NpcDialogueFlow flow = activeNpcDialog.flow;
        activeNpcDialog.inQuiz = false;
        activeNpcDialog.quizAnswered = true;
        npcQuizButtonsTable.setVisible(false);

        boolean isCorrect = (answer == 'A');
        activeNpcDialog.quizCorrect = isCorrect;

        if (activeNpcDialog.currentTexture != null) {
            activeNpcDialog.currentTexture.dispose();
        }

        if (isCorrect) {
            activeNpcDialog.currentTexture = new Texture(Gdx.files.internal(flow.right));
        } else {
            activeNpcDialog.currentTexture = new Texture(Gdx.files.internal(flow.wrong));
        }
        activeNpcDialog.currentTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        npcDialogImage.setDrawable(new TextureRegionDrawable(new TextureRegion(activeNpcDialog.currentTexture)));
        rebuildNpcDialogLayout(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void closeNpcDialogueFlow() {
        npcDialogActive = false;
        npcDialogImage.setVisible(false);
        npcQuizButtonsTable.setVisible(false);
        pauseButton.setVisible(true);

        if (activeNpcDialog != null) {
            if (activeNpcDialog.currentTexture != null) {
                activeNpcDialog.currentTexture.dispose();
            }
            activeNpcDialog = null;
        }
    }

    private void rebuildNpcDialogLayout(float width, float height) {
        if (activeNpcDialog == null || activeNpcDialog.currentTexture == null) return;

        float imgWidth = activeNpcDialog.currentTexture.getWidth();
        float imgHeight = activeNpcDialog.currentTexture.getHeight();
        float targetRatio = imgWidth / imgHeight;

        float dialogWidth;
        float dialogHeight;

        if (targetRatio < 2.0f) {
            // Quiz (centered, large size - full scale rendering)
            dialogWidth = width * 0.85f;
            dialogHeight = dialogWidth / targetRatio;

            // If it takes up too much height, scale down
            if (dialogHeight > height * 0.70f) {
                dialogHeight = height * 0.70f;
                dialogWidth = dialogHeight * targetRatio;
            }

            npcDialogImage.setSize(dialogWidth, dialogHeight);

            float imgX = (width - dialogWidth) / 2f;
            float imgY = (height - dialogHeight) / 2f + height * 0.08f;
            npcDialogImage.setPosition(imgX, imgY);

            // Size buttons
            float btnWidth = dialogWidth * 0.09f;
            if (btnWidth > 120f) btnWidth = 120f; // cap button width
            float btnHeight = (btnWidth / 372f) * 338f;

            npcQuizButtonsTable.clear();
            npcQuizButtonsTable.add(npcQzButtonA).size(btnWidth, btnHeight).padRight(dialogWidth * 0.03f);
            npcQuizButtonsTable.add(npcQzButtonB).size(btnWidth, btnHeight).padRight(dialogWidth * 0.03f);
            npcQuizButtonsTable.add(npcQzButtonC).size(btnWidth, btnHeight);
            
            npcQuizButtonsTable.pack();

            float tableWidth = npcQuizButtonsTable.getWidth();
            float tableHeight = npcQuizButtonsTable.getHeight();
            float tableX = (width - tableWidth) / 2f;
            float tableY = imgY - tableHeight - height * 0.02f;
            npcQuizButtonsTable.setPosition(tableX, tableY);
            
            if (activeNpcDialog.inQuiz) {
                npcQuizButtonsTable.setVisible(true);
            } else {
                npcQuizButtonsTable.setVisible(false);
            }
        } else {
            // Normal Dialogue (bottom aligned banner)
            dialogWidth = width * 0.9f;
            dialogHeight = dialogWidth / targetRatio;

            if (dialogHeight > height * 0.4f) {
                dialogHeight = height * 0.4f;
                dialogWidth = dialogHeight * targetRatio;
            }

            npcDialogImage.setSize(dialogWidth, dialogHeight);

            float imgX = (width - dialogWidth) / 2f;
            float imgY = 20f;
            npcDialogImage.setPosition(imgX, imgY);
            npcQuizButtonsTable.setVisible(false);
        }
    }
}
