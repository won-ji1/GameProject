package io.jbnu.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class Main extends ApplicationAdapter {

    private static final float W = 960f;
    private static final float H = 540f;

    // 캐릭터의 실제 충돌 크기
    private static final float PLAYER_W = 48f;
    private static final float PLAYER_H = 66f;

    // 화면에 표시할 캐릭터 크기
    private static final float DRAW_W = 72f;
    private static final float DRAW_H = 76f;
    // 퀸은 그림의 실제 몸통(투명한 여백을 잘라낸 부분)을 이 높이로 그립니다.
    private static final float QUINN_DRAW_H = 74f;
    private static final float QUINN_FLY_DRAW_H = 96f;

    private static final float MOVE_SPEED = 310f;
    private static final float JUMP_SPEED = 630f;
    private static final float GRAVITY = 1500f;
    private static final float MINION_GRAVITY = 1200f;

    // 조작감 보정: 발판 끝에서 살짝 늦게 눌러도 점프, 착지 직전에 눌러도 점프
    private static final float COYOTE_TIME = 0.10f;
    private static final float JUMP_BUFFER = 0.13f;

    // Q 평타 간격
    private static final float ATTACK_COOLDOWN = 0.22f;

    // ===== 맵 =====
    private static final float GROUND_BOTTOM = -300f;
    private static final float LEDGE_THICK = 36f;
    private static final float CELL = 48f;
    private static final float TERRAIN_BAND = 40f;
    private static final float CAMERA_MAX_Y = 900f;

    // true: bl1~bl3.png 이미지를 땅 윗부분에 덧그립니다.
    // false: 이끼 낀 돌벽돌 지형을 코드로 그립니다. (기본)
    private static final boolean USE_TERRAIN_TEXTURES = false;

    // true: 바론 팔에 맞으면 라운드 시작 지점으로 복귀 / false: 마지막 체크포인트
    private static final boolean PILLAR_SENDS_TO_ROUND_START = true;

    // 전령 이미지가 왼쪽을 보고 있다면 true로 바꾸세요. (달릴 때 오른쪽을 보게 뒤집습니다)
    private static final boolean FLIP_HERALD_IMAGE = false;

    // 발러(bal.png), 나는 퀸(qw.png) 이미지가 왼쪽을 보고 있다면 true로 바꾸세요.
    private static final boolean FLIP_VALOR_IMAGE = false;
    private static final boolean FLIP_QUINN_FLY_IMAGE = false;

    // ===== 챔피언 번호 =====
    private static final int ZAC = 0;
    private static final int OLAF = 1;
    private static final int LEE_SIN = 2;
    private static final int QUINN = 3;
    private static final int CHAMPION_COUNT = 4;

    // ===== 퀸 =====
    private static final float ARROW_SPEED = 1250f;
    private static final float ARROW_RANGE = 620f;
    private static final float QUINN_ATTACK_COOLDOWN = 0.32f;
    private static final float VALOR_FLIGHT_TIME = 7f;
    private static final float VALOR_ARRIVE_TIME = 0.75f;
    private static final float VALOR_SPIRAL_TIME = 0.95f;
    private static final float VALOR_LEAVE_TIME = 0.9f;
    private static final float FLY_VERTICAL_SPEED = 340f;

    // 발러 상태
    private static final int VALOR_NONE = 0;
    private static final int VALOR_ARRIVING = 1;
    private static final int VALOR_SPIRAL = 2;
    private static final int VALOR_CARRYING = 3;
    private static final int VALOR_LEAVING = 4;

    private static final int KIND_GROUND = 0;
    private static final int KIND_LEDGE = 1;
    private static final int KIND_MOVER = 2;
    // 전령이 부수고 지나가는 바위 (걸어갈 때는 뛰어넘어야 합니다)
    private static final int KIND_ROCK = 3;

    // ===== 용 / 전령 =====
    private static final int MON_WIND_DRAGON = 0;
    private static final int MON_EARTH_DRAGON = 1;
    private static final int MON_HERALD = 2;

    private static final float DRAGON_W = 124f;
    private static final float DRAGON_H = 98f;
    private static final float HERALD_W = 130f;
    private static final float HERALD_H = 104f;
    private static final float HERALD_SPEED = 470f;
    private static final int MONSTER_HP = 5;

    // ===== 파티클 =====
    private static final int P_DOT = 0;
    private static final int P_CHUNK = 1;
    private static final int P_ORB = 2;
    private static final int P_RING = 3;
    private static final int MAX_PARTICLES = 900;

    private static final int MAX_MINIONS = 24;

    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    // 한글 글꼴 (kr.fnt + kr.png). 기본 글꼴에는 한글이 없어서 따로 불러옵니다.
    private BitmapFont krFont;
    // kr.fnt는 32px로 만들어져 있어 기본 글꼴(약 15px)과 크기를 맞추는 비율
    private static final float KR_SCALE = 15f / 32f;
    private GlyphLayout layout;
    private OrthographicCamera camera;
    private FitViewport viewport;

    private Texture selectionBackground;
    private Texture[] portraits = new Texture[CHAMPION_COUNT];
    private Texture[] championSprites = new Texture[CHAMPION_COUNT];
    private Texture[] backgrounds = new Texture[3];
    private Texture blockTexture;
    private Texture[] groundTextures = new Texture[3];
    private Texture[] minionTextures = new Texture[3];
    private Texture nexusTexture;
    private Texture flagTexture;
    private Texture baronTexture;
    private Texture heraldTexture;
    private Texture earthDragonTexture;
    private Texture windDragonTexture;
    private Texture quinnFlyTexture;
    // 이미지에서 투명하지 않은 영역 {x, y, 너비, 높이} (y는 위에서부터). 없으면 그림 전체를 씁니다.
    private int[] quinnTrim;
    private int[] quinnFlyTrim;
    private Texture valorTexture;

    private Music acceptMusic;

    // ===== 사운드 =====
    // assets 폴더에 아래 이름의 파일을 넣으면 자동으로 재생됩니다. (.ogg / .wav / .mp3)
    // 파일이 없으면 그 소리만 조용히 건너뜁니다.
    private static final String[] SOUND_NAMES = {
        // 챔피언 평타
        "zac_attack", "olaf_attack", "lee_attack",
        // 궁극기 (챔피언 대사·효과음)
        "zac_r", "zac_leap", "zac_land",
        "olaf_r",
        "lee_r", "lee_r2", "lee_mark",
        "quinn_attack", "arrow_hit", "quinn_r",
        "valor_screech", "valor_flap", "valor_drop",
        "axe_throw", "axe_stick", "axe_pickup",
        // 공통
        "jump", "land", "hit", "hit_monster", "cs", "hurt",
        "checkpoint", "round_start",
        // 용·전령·바론
        "dragon_slain", "orb", "herald_tame", "herald_stomp", "smash",
        "baron_arm", "launch"
    };

    // 배경음악: a20 (모든 라운드 공통, 끊기지 않고 무한 반복)
    // 승리 화면 전용 음악을 쓰고 싶으면 victory 파일을 넣으세요. (없으면 a20이 계속 나옵니다)
    private static final float MUSIC_VOLUME = 0.45f;
    private static final float SFX_VOLUME = 0.85f;

    private final HashMap<String, Sound> sounds = new HashMap<>();
    private final HashMap<String, Float> soundLastPlayed = new HashMap<>();
    private Music gameMusic;
    private Music victoryMusic;
    private Music currentMusic;
    private boolean muted;
    private float audioClock;

    private boolean selectingChampion = true;
    private boolean paused;
    private boolean cleared;

    // 0 = 자크, 1 = 올라프, 2 = 리 신, 3 = 퀸
    private int champion = -1;
    private int round = 1;
    private String roundName = "";

    private final Rectangle player = new Rectangle();

    private float velocityX;
    private float velocityY;
    private boolean onGround;
    private boolean facingRight = true;

    private float walkTimer;
    private float landingTimer;
    private float coyoteTimer;
    private float jumpBufferTimer;
    // 연타해도 입력이 사라지지 않도록 Q, R 입력도 잠깐 기억합니다.
    private float attackBufferTimer;
    private float ultBufferTimer;

    // 찌부 / 늘어남 연출 (1 = 원래 크기)
    private float squashX = 1f;
    private float squashY = 1f;
    private float squashVX;
    private float squashVY;

    private int cs;
    private int playerHits;
    private float hitCooldown;
    private float attackCooldown;

    // 용 버프
    private boolean windBuff;
    private boolean earthBuff;
    private int dragonsSlain;

    // 올라프 궁극기: 7초 무적 + 지형 통과 + 바론 무시
    private boolean olafActive;
    private float olafTimer;
    // 올라프가 지형 속을 지나갈 때 유지할 높이 (마지막으로 밟은 바닥 높이)
    private float olafFloor;

    // 퀸: 화살과 발러
    private final Array<Arrow> arrows = new Array<>();
    private int valorState;
    private float valorTimer;
    private float flightTimer;
    private float valorX;
    private float valorY;
    private float valorFromX;
    private float valorFromY;
    private float valorAngle;
    private float valorPrevX;
    private float valorFlapTimer;

    // 올라프 Q: 던지는 도끼 (하나만 존재)
    private static final float AXE_SPEED_X = 900f;
    private static final float AXE_SPEED_Y = 200f;
    private static final float AXE_GRAVITY = 900f;
    private static final float OLAF_AXE_COOLDOWN = 1.0f;
    private static final int AXE_NONE = 0;
    private static final int AXE_FLYING = 1;
    private static final int AXE_STUCK = 2;
    private int axeState;
    private float axeX;
    private float axeY;
    private float axeVX;
    private float axeVY;
    private float axeSpin;
    private float axeTimer;
    private float axeStuckAngle;
    private Rectangle axePlatform;
    private final Array<Monster> axeHits = new Array<>();
    private final float[] axeBladePts = new float[8];
    private boolean valorBehind;
    private float olafGhostTimer;

    // 자크 궁극기
    private boolean zacActive;
    private float zacTimer;
    private float zacLeapX;
    private float zacWindup;
    private boolean zacAirborne;
    private float zacAirTime;
    private float zacSpin;

    // 리 신 궁극기
    private boolean sonicWaveActive;
    private boolean leeAimMode;
    private float leeUltTimer;

    private float waveX;
    private float waveY;
    private float waveDirection;
    private float waveVelocityX;
    private float waveVelocityY;
    private float waveTimer;
    private float waveRingTimer;

    private float aimWorldX;
    private float aimWorldY;

    // 음파 표식 → R을 다시 누르면 표식으로 날아갑니다.
    private boolean markActive;
    private float markTimer;
    private float markX;
    private float markY;
    private Monster markMonster;
    // 음파가 맞은 지형 (꼭대기 근처에 맞히면 그 위로 올라섭니다)
    private Rectangle markPlatform;
    private static final float LEE_LEDGE_GRAB = 90f;

    private boolean leeDashing;
    private float dashT;
    private float dashDur;
    private float dashFromX;
    private float dashFromY;
    private float dashToX;
    private float dashToY;
    private Monster dashMonster;
    private float ghostEmitTimer;

    // Q 평타 효과
    private float attackTimer;
    private float attackDirection = 1f;
    private float slashTimer;
    private float slashSide = 1f;

    private float worldWidth;
    private float spawnTimer;

    private final Array<Rectangle> platforms = new Array<>();
    private final Array<Integer> platformTextureIndices = new Array<>();
    private final Array<Integer> platformKinds = new Array<>();
    private final Array<Minion> minions = new Array<>();
    private final Rectangle nexus = new Rectangle();

    private final Array<Mover> movers = new Array<>();
    private final Array<PillarTrap> traps = new Array<>();
    private final Array<Checkpoint> checkpoints = new Array<>();
    private final Array<Deco> decorations = new Array<>();
    private final Array<float[]> pits = new Array<>();
    private final Array<Rectangle> spawnSurfaces = new Array<>();
    private final Array<Rectangle> groundSegments = new Array<>();
    private final Array<Monster> monsters = new Array<>();
    private final Array<Particle> particles = new Array<>();
    // 잔상 {x, y, 남은시간, 최대시간, 좌우반전(1/0), 종류(0 리신, 1 올라프)}
    private final Array<float[]> ghosts = new Array<>();
    // 전령이 지나간 땅 자국 {x, y, 너비, 남은시간}
    private final Array<float[]> scars = new Array<>();

    private Rectangle standingOn;

    private float roundStartX;
    private float roundStartY;
    private float respawnX;
    private float respawnY;

    // 바론 팔에 맞아 날아가는 상태
    private boolean launched;
    private float launchTimer;
    private float launchSpin;

    // 전령 탑승
    private boolean riding;
    private int rideState;
    private float rideTimer;
    private Monster rideHerald;
    private Rectangle rideSeg;
    private Rectangle finalSeg;
    private float rideStopX;
    private float rideDustTimer;
    private float mountFromX;
    private float mountFromY;
    private Rectangle leapTarget;
    private float leapT;
    private float leapDur;
    private float leapX0;
    private float leapY0;
    private float leapVX;
    private float leapVY;

    // 연출용 타이머
    private float animTime;
    private float fadeTimer;
    private float bannerTimer;
    private float shakeTimer;
    private float hitStop;
    private float trailTimer;

    private String toastTitle = "";
    private boolean toastKorean;
    private String toastSub = "";
    private float toastTimer;
    private final Color toastColor = new Color(1f, 0.85f, 0.45f, 1f);

    private float camX = W / 2f;
    private float camY = H / 2f;
    private boolean cameraSnap = true;

    private boolean debugDraw;

    private final Rectangle tmpRect = new Rectangle();
    private final Rectangle tmpRect2 = new Rectangle();
    private final Color tmpA = new Color();
    private final Color tmpB = new Color();
    private final Color tmpC = new Color();
    private final Vector3 tmpVec = new Vector3();

    private final Random random = new Random();

    // ===== 라운드별 색 테마 =====
    // 0: 탑 라인(초록 이끼), 1: 강(청록 이끼), 2: 적 기지(재와 불씨)
    private static final float[][] STONE = {
        {0.46f, 0.48f, 0.43f}, {0.40f, 0.46f, 0.52f}, {0.33f, 0.28f, 0.37f}
    };
    private static final float[][] MOSS = {
        {0.24f, 0.42f, 0.13f}, {0.13f, 0.40f, 0.34f}, {0.30f, 0.26f, 0.33f}
    };
    private static final float[][] MOSS_TOP = {
        {0.56f, 0.80f, 0.30f}, {0.40f, 0.84f, 0.70f}, {0.56f, 0.50f, 0.60f}
    };
    private static final float[][] BUSH_DARK = {
        {0.07f, 0.24f, 0.13f}, {0.05f, 0.21f, 0.21f}, {0.14f, 0.09f, 0.16f}
    };
    private static final float[][] BUSH_LIGHT = {
        {0.20f, 0.46f, 0.22f}, {0.13f, 0.42f, 0.38f}, {0.30f, 0.20f, 0.32f}
    };
    private static final float[][] MOTE = {
        {1.00f, 0.90f, 0.58f}, {0.60f, 0.92f, 1.00f}, {1.00f, 0.48f, 0.34f}
    };
    private static final float[][] ABYSS = {
        {0.02f, 0.04f, 0.04f}, {0.01f, 0.03f, 0.06f}, {0.03f, 0.01f, 0.04f}
    };
    private static final float[][] GRADE = {
        {0.95f, 0.88f, 0.62f}, {0.45f, 0.72f, 0.98f}, {0.60f, 0.25f, 0.50f}
    };
    private static final float[][] SHAFT = {
        {1.00f, 0.96f, 0.80f}, {0.82f, 0.95f, 1.00f}, {1.00f, 0.55f, 0.42f}
    };

    private static class Minion {
        Rectangle rect = new Rectangle();

        // 0 = 근거리, 1 = 원거리, 2 = 대포
        int type;

        float speed;
        float direction;
        float velocityY;

        float minX;
        float maxX;

        Minion(
            float x,
            float y,
            int type,
            float speed,
            float minX,
            float maxX
        ) {
            this.type = type;
            this.speed = speed;
            this.minX = minX;
            this.maxX = maxX;

            if (type == 2) {
                rect.set(x, y, 66f, 58f);
            } else {
                rect.set(x, y, 52f, 48f);
            }

            direction = 1f;
        }

        int getCS() {
            if (type == 0) return 1;
            if (type == 1) return 2;
            return 3;
        }
    }

    private static class Mover {
        Rectangle rect;
        float baseX;
        float baseTop;
        float rangeX;
        float rangeY;
        float period;
        float phase;
    }

    // 바론 함정: 가운데 칸(바론)을 기준으로 양옆 3칸씩, 총 6칸에서 바론 팔이 솟습니다.
    private static class PillarTrap {
        float centerX;
        float groundTop;
        float[] cellX = new float[6];
        boolean[] chosen = new boolean[6];

        // 0 대기, 1 경고, 2 상승, 3 유지, 4 하강
        int state;
        float timer;
        float height;
        float maxHeight;
    }

    private static class Checkpoint {
        float x;
        float y;
        boolean active;
        float flash;
    }

    private static class Deco {
        int kind;
        float x;
        float y;
        float size;
        float seed;
    }

    // 용과 전령
    private static class Monster {
        int kind;
        Rectangle rect = new Rectangle();
        int hp = MONSTER_HP;
        float shownHp = MONSTER_HP;
        float flash;
        float punch;
        float baseY;
        float minX;
        float maxX;
        float dir = 1f;
        float seed;
        boolean alive = true;
        float deathTimer;
    }

    // 퀸의 화살: 대상이 있으면 대상을 따라가고, 없으면 앞으로 곧게 날아갑니다.
    private static class Arrow {
        float x, y, vx, vy;
        float life;
        Minion targetMinion;
        Monster targetMonster;
        float trailTimer;
    }

    private static class Particle {
        int kind;
        float x, y, vx, vy;
        float life, maxLife;
        float size, grow;
        float r, g, b, a;
        float gravity, drag;
        float rot, vrot;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        layout = new GlyphLayout();

        // 글자를 키워도 덜 깨지게 합니다.
        font.getRegion().getTexture().setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );
        font.setUseIntegerPositions(false);

        if (Gdx.files.internal("kr.fnt").exists()) {
            try {
                krFont = new BitmapFont(Gdx.files.internal("kr.fnt"));
                krFont.getRegion().getTexture().setFilter(
                    Texture.TextureFilter.Linear,
                    Texture.TextureFilter.Linear
                );
                krFont.setUseIntegerPositions(false);
            } catch (Exception e) {
                Gdx.app.error("Main", "Cannot load Korean font kr.fnt", e);
                krFont = null;
            }
        }

        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        viewport.apply();

        loadAssets();

        camera.position.set(W / 2f, H / 2f, 0);
        camera.update();
    }

    private Texture loadTexture(String name) {
        if (!Gdx.files.internal(name).exists()) {
            Gdx.app.error(
                "Main",
                "Image not found: " + name
            );
            return null;
        }

        try {
            Texture t = new Texture(Gdx.files.internal(name));
            t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return t;
        } catch (Exception e) {
            Gdx.app.error(
                "Main",
                "Cannot load image: " + name,
                e
            );
            return null;
        }
    }

    // 그림 둘레의 투명한 여백을 찾아 실제로 보이는 영역만 돌려줍니다.
    // 여백이 있으면 캐릭터가 작아 보이고, 아래 여백 때문에 공중에 떠 보이기 때문입니다.
    // 알파값이 낮은 그림자·빛 번짐이나 몇 픽셀짜리 잡티는 무시합니다.
    private int[] findOpaqueBounds(String name, Texture texture) {
        if (texture == null || !Gdx.files.internal(name).exists()) return null;

        Pixmap pm = null;

        try {
            pm = new Pixmap(Gdx.files.internal(name));

            int w = pm.getWidth();
            int h = pm.getHeight();
            int step = Math.max(1, Math.max(w, h) / 400);
            int minRow = Math.max(2, w / step / 50);
            int minCol = Math.max(2, h / step / 50);

            int[] rowCount = new int[h];
            int[] colCount = new int[w];

            for (int y = 0; y < h; y += step) {
                for (int x = 0; x < w; x += step) {
                    if ((pm.getPixel(x, y) & 0xff) > 60) {
                        rowCount[y]++;
                        colCount[x]++;
                    }
                }
            }

            int top = -1, bottom = -1, left = -1, right = -1;

            for (int y = 0; y < h; y += step) {
                if (rowCount[y] >= minRow) {
                    if (top < 0) top = y;
                    bottom = y;
                }
            }

            for (int x = 0; x < w; x += step) {
                if (colCount[x] >= minCol) {
                    if (left < 0) left = x;
                    right = x;
                }
            }

            if (top < 0 || left < 0) return null;

            bottom = Math.min(h - 1, bottom + step - 1);
            right = Math.min(w - 1, right + step - 1);

            // 텍스처 크기가 다르면 비율로 맞춥니다.
            float kx = (float) texture.getWidth() / w;
            float ky = (float) texture.getHeight() / h;

            int[] r = {
                Math.round(left * kx), Math.round(top * ky),
                Math.max(1, Math.round((right - left + 1) * kx)),
                Math.max(1, Math.round((bottom - top + 1) * ky))
            };

            Gdx.app.log("Main", name + " visible area: x=" + r[0] + " y=" + r[1]
                + " w=" + r[2] + " h=" + r[3] + " (image " + w + "x" + h + ")");
            return r;
        } catch (Exception e) {
            Gdx.app.error("Main", "Cannot read image pixels: " + name, e);
            return null;
        } finally {
            if (pm != null) pm.dispose();
        }
    }

    private void loadAssets() {
        selectionBackground = loadTexture("start.png");

        portraits[0] = loadTexture("z.png");
        portraits[1] = loadTexture("o.png");
        portraits[2] = loadTexture("l.png");

        championSprites[0] = loadTexture("z2.png");
        championSprites[1] = loadTexture("o2.png");
        championSprites[2] = loadTexture("l2.png");

        backgrounds[0] = loadTexture("bc1.png");
        backgrounds[1] = loadTexture("bc2.png");
        backgrounds[2] = loadTexture("bc3.png");

        blockTexture = loadTexture("block.png");

        groundTextures[0] = loadTexture("bl1.png");
        groundTextures[1] = loadTexture("bl2.png");
        groundTextures[2] = loadTexture("bl3.png");

        minionTextures[0] = loadTexture("mg.png");
        minionTextures[1] = loadTexture("mw.png");
        minionTextures[2] = loadTexture("m.png");

        nexusTexture = loadTexture("nex.png");
        flagTexture = loadTexture("flag.png");

        baronTexture = loadTexture("ba.png");
        heraldTexture = loadTexture("zr.png");
        earthDragonTexture = loadTexture("ld.png");
        windDragonTexture = loadTexture("wd.png");

        portraits[QUINN] = loadTexture("q.png");
        championSprites[QUINN] = loadTexture("q2.png");
        quinnFlyTexture = loadTexture("qw.png");
        quinnTrim = findOpaqueBounds("q2.png", championSprites[QUINN]);
        quinnFlyTrim = findOpaqueBounds("qw.png", quinnFlyTexture);
        valorTexture = loadTexture("bal.png");

        loadAudio();

        if (Gdx.files.internal("accept.mp3").exists()) {
            try {
                acceptMusic = Gdx.audio.newMusic(
                    Gdx.files.internal("accept.mp3")
                );
                acceptMusic.setLooping(false);
                acceptMusic.play();
            } catch (Exception e) {
                Gdx.app.error(
                    "Main",
                    "Cannot load accept.mp3",
                    e
                );
            }
        }
    }

    // =====================================================================
    //  사운드
    // =====================================================================
    private void loadAudio() {
        for (String name : SOUND_NAMES) {
            Sound s = loadSound(name);

            if (s != null) sounds.put(name, s);
        }

        gameMusic = loadMusic("a20");

        victoryMusic = loadMusic("victory");
    }

    // ----- 소리 파일 불러오기 -----
    // 이름이 base인 파일을 .ogg → .wav → .mp3 순서로 찾아 열어 봅니다.
    // 하나가 실패하면 다음 확장자를 시도하고, 결과는 Run 창에 출력합니다.
    private static final String[] AUDIO_EXTS = {".ogg", ".wav", ".mp3"};

    private Sound loadSound(String base) {
        for (FileHandle f : findAudioCandidates(base)) {
            try {
                return Gdx.audio.newSound(playableAudioFile(f));
            } catch (Exception e) {
                Gdx.app.error("Sound", "Cannot open sound " + f.path() + " (" + e.getMessage() + ")");
            }
        }

        return null;
    }

    private Music loadMusic(String base) {
        Array<FileHandle> candidates = findAudioCandidates(base);

        for (FileHandle f : candidates) {
            try {
                Music m = Gdx.audio.newMusic(playableAudioFile(f));
                m.setLooping(true);
                Gdx.app.log("Sound", "Background music loaded: " + f.path());
                return m;
            } catch (Exception e) {
                Gdx.app.error("Sound", "Cannot open music " + f.path() + " (" + e.getMessage() + ")");
            }
        }

        // 배경음악 a20을 못 찾았을 때: 게임이 실제로 보고 있는 소리 파일 목록을 출력합니다.
        // (Windows 콘솔에서 한글이 깨지지 않도록 영어로 출력)
        if (candidates.size == 0 && base.equals("a20")) {
            String workDir = System.getProperty("user.dir");

            Gdx.app.error("Sound", "Background music 'a20' NOT FOUND.");
            Gdx.app.error("Sound", "  Put a20.ogg (or a20.mp3 / a20.wav) in the SAME folder as accept.mp3 and nex.png.");
            Gdx.app.error("Sound", "  Working directory: " + workDir);
            Gdx.app.error("Sound", "  Audio files the game can see: " + listVisibleAudioFiles());
        }

        return null;
    }

    // base 이름의 소리 파일 후보를 찾습니다.
    // 1) base.ogg / base.wav / base.mp3
    // 2) assets.txt(빌드할 때 자동으로 만들어지는 에셋 목록)에서 이름이 비슷한 파일
    //    예: A20.OGG, a20.ogg.mp3, "a20 .ogg", sound/a20.ogg 처럼 이름이 조금 달라도 찾아냅니다.
    private Array<FileHandle> findAudioCandidates(String base) {
        Array<FileHandle> out = new Array<>();
        Array<String> seen = new Array<>();

        for (String ext : AUDIO_EXTS) {
            FileHandle f = Gdx.files.internal(base + ext);

            if (f.exists()) {
                out.add(f);
                seen.add(f.path().toLowerCase());
            }
        }

        for (String path : readAssetList()) {
            String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase().replace(" ", "");

            if (!isAudioName(name)) continue;

            boolean match = name.startsWith(base.toLowerCase() + ".");

            if (!match || seen.contains(path.toLowerCase(), false)) continue;

            FileHandle f = Gdx.files.internal(path);

            if (f.exists()) {
                out.add(f);
                seen.add(path.toLowerCase());
            }
        }

        return out;
    }

    private boolean isAudioName(String lowerName) {
        return lowerName.endsWith(".ogg") || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav");
    }

    // assets.txt 읽기 (없으면 빈 목록)
    private Array<String> readAssetList() {
        Array<String> list = new Array<>();
        FileHandle f = Gdx.files.internal("assets.txt");

        if (!f.exists()) return list;

        try {
            for (String line : f.readString("UTF-8").split("\\r?\\n")) {
                String p = line.trim().replace('\\', '/');

                if (!p.isEmpty()) list.add(p);
            }
        } catch (Exception e) {
            // 목록을 못 읽으면 그냥 넘어갑니다.
        }

        return list;
    }

    private String listVisibleAudioFiles() {
        StringBuilder sb = new StringBuilder();

        for (String p : readAssetList()) {
            if (!isAudioName(p.toLowerCase())) continue;

            if (sb.length() > 0) sb.append(", ");

            sb.append(p);
        }

        return sb.length() == 0 ? "(none, or assets.txt missing)" : sb.toString();
    }

    // 파일 내용을 직접 읽어 실제 형식을 확인합니다.
    // 예: mp3 파일의 이름만 .ogg로 바꾼 경우, 올바른 확장자로 임시 복사본을 만들어 엽니다.
    private FileHandle playableAudioFile(FileHandle f) {
        String real = detectAudioType(f);
        String ext = f.extension().toLowerCase();

        if (real == null) {
            throw new RuntimeException("unsupported format - only real ogg, mp3, wav files can be played");
        }

        if (real.equals(ext)) return f;

        Gdx.app.log("Sound", f.name() + " is really a " + real + " file (named ." + ext
            + "). Opening it as " + real + ".");

        java.io.File tmp = new java.io.File(
            System.getProperty("java.io.tmpdir"),
            "jbnu_" + f.nameWithoutExtension() + "." + real
        );

        FileHandle fixed = Gdx.files.absolute(tmp.getAbsolutePath());
        fixed.writeBytes(f.readBytes(), false);
        return fixed;
    }

    // 파일 앞부분의 표식으로 형식을 판별합니다. (모르는 형식이면 null)
    private String detectAudioType(FileHandle f) {
        byte[] h = new byte[12];
        int n = 0;

        try (java.io.InputStream in = f.read()) {
            while (n < h.length) {
                int r = in.read(h, n, h.length - n);

                if (r < 0) break;

                n += r;
            }
        } catch (Exception e) {
            return null;
        }

        if (n >= 4 && h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S') return "ogg";

        if (n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
            && h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E') return "wav";

        if (n >= 3 && h[0] == 'I' && h[1] == 'D' && h[2] == '3') return "mp3";

        if (n >= 2 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0) return "mp3";

        return null;
    }

    // 효과음 (pitchVariation: 같은 소리가 반복돼도 덜 기계적으로 들리게 음높이를 살짝 바꿉니다)
    private void sfx(String name, float volume, float pitchVariation) {
        if (muted) return;

        Sound s = sounds.get(name);

        if (s == null) return;

        // 같은 소리가 한 순간에 여러 번 겹쳐 터지지 않게 합니다.
        Float last = soundLastPlayed.get(name);

        if (last != null && audioClock - last < 0.045f) return;

        soundLastPlayed.put(name, audioClock);

        float pitch = 1f + (random.nextFloat() * 2f - 1f) * pitchVariation;

        s.play(Math.min(1f, volume * SFX_VOLUME), pitch, 0f);
    }

    private void sfx(String name, float volume) {
        sfx(name, volume, 0.06f);
    }

    // 챔피언 대사는 음높이를 바꾸지 않습니다.
    private void voice(String name) {
        sfx(name, 1f, 0f);
    }

    private String championKey() {
        if (champion == ZAC) return "zac";
        if (champion == OLAF) return "olaf";
        if (champion == QUINN) return "quinn";
        return "lee";
    }

    private void playMusic(Music m) {
        if (currentMusic == m) {
            if (m != null && !m.isPlaying()) m.play();
            return;
        }

        if (currentMusic != null) currentMusic.stop();

        currentMusic = m;

        if (m != null) {
            m.setVolume(muted ? 0f : MUSIC_VOLUME);
            m.play();
        }
    }

    private void updateMusicVolume() {
        float v = muted ? 0f : (paused ? MUSIC_VOLUME * 0.35f : MUSIC_VOLUME);

        if (currentMusic != null) currentMusic.setVolume(v);
        if (acceptMusic != null) acceptMusic.setVolume(muted ? 0f : 1f);
    }

    @Override
    public void render() {
        float delta = Math.min(
            Gdx.graphics.getDeltaTime(),
            0.033f
        );

        audioClock += delta;

        // M: 소리 끄기 / 켜기 (어느 화면에서나)
        if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
            muted = !muted;
            updateMusicVolume();
        }

        ScreenUtils.clear(0.06f, 0.09f, 0.12f, 1f);

        if (selectingChampion) {
            handleChampionSelection();
            renderChampionSelection();
            return;
        }

        if (cleared) {
            handleClearInput();
            renderClearScreen();
            return;
        }

        if (paused) {
            handlePauseInput();
            updateCamera(0f);
            renderGame();
            renderPauseScreen();
            return;
        }

        // 타격 순간 아주 짧게 멈추는 연출 (점프 입력은 기억해 둡니다)
        if (hitStop > 0f) {
            hitStop -= delta;

            // 멈칫하는 순간에도 일시정지는 바로 받습니다.
            if (Gdx.input.isKeyJustPressed(Input.Keys.P)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                paused = true;
                updateMusicVolume();
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                jumpBufferTimer = JUMP_BUFFER;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
                attackBufferTimer = 0.25f;
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                ultBufferTimer = 0.25f;
            }

            updateCamera(0f);
            renderGame();
            return;
        }

        handleInput();
        updateGame(delta);
        updateCamera(delta);
        renderGame();
    }

    // 챔피언 선택 화면의 초상화 위치 (4명)
    private static final float SELECT_W = 170f;
    private static final float SELECT_H = 208f;
    private static final float SELECT_GAP = 40f;
    private static final float SELECT_Y = 150f;

    private float selectX(int i) {
        float total = SELECT_W * CHAMPION_COUNT + SELECT_GAP * (CHAMPION_COUNT - 1);
        return (W - total) / 2f + i * (SELECT_W + SELECT_GAP);
    }

    private void handleChampionSelection() {
        if (!Gdx.input.justTouched()) return;

        float mx =
            Gdx.input.getX() * W / Gdx.graphics.getWidth();

        float my =
            H - Gdx.input.getY() * H / Gdx.graphics.getHeight();

        for (int i = 0; i < CHAMPION_COUNT; i++) {
            Rectangle bounds = new Rectangle(
                selectX(i), SELECT_Y, SELECT_W, SELECT_H
            );

            if (bounds.contains(mx, my)) {
                champion = i;
                selectingChampion = false;
                startRound(1);
                return;
            }
        }
    }

    private void renderChampionSelection() {
        camera.position.set(W / 2f, H / 2f, 0);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (selectionBackground != null) {
            batch.draw(
                selectionBackground,
                0, 0, W, H
            );
        }

        for (int i = 0; i < CHAMPION_COUNT; i++) {
            if (portraits[i] != null) {
                batch.draw(
                    portraits[i],
                    selectX(i), SELECT_Y,
                    SELECT_W, SELECT_H
                );
            }
        }

        batch.end();
    }

    private void startRound(int newRound) {
        round = newRound;

        paused = false;
        cleared = false;

        cs = 0;
        playerHits = 0;
        hitCooldown = 0f;
        attackCooldown = 0f;

        velocityX = 0f;
        velocityY = 0f;
        onGround = false;
        standingOn = null;
        coyoteTimer = 0f;
        jumpBufferTimer = 0f;
        attackBufferTimer = 0f;
        ultBufferTimer = 0f;

        facingRight = true;
        walkTimer = 0f;
        landingTimer = 0f;

        squashX = 1f;
        squashY = 1f;
        squashVX = 0f;
        squashVY = 0f;

        windBuff = false;
        earthBuff = false;
        dragonsSlain = 0;

        olafActive = false;
        olafTimer = 0f;

        zacActive = false;
        zacTimer = 0f;
        zacLeapX = 0f;
        zacWindup = 0f;
        zacAirborne = false;
        zacSpin = 0f;

        sonicWaveActive = false;
        leeAimMode = false;
        leeUltTimer = 0f;
        waveTimer = 0f;
        markActive = false;
        markMonster = null;
        leeDashing = false;
        dashMonster = null;

        attackTimer = 0f;
        spawnTimer = 0f;
        slashTimer = 0f;
        slashSide = 1f;

        launched = false;
        launchTimer = 0f;
        launchSpin = 0f;

        riding = false;
        rideHerald = null;

        valorState = VALOR_NONE;
        flightTimer = 0f;
        arrows.clear();
        axeState = AXE_NONE;

        animTime = 0f;
        fadeTimer = 0.6f;
        bannerTimer = 3.2f;
        shakeTimer = 0f;
        hitStop = 0f;
        toastTimer = 0f;

        minions.clear();
        particles.clear();
        ghosts.clear();
        scars.clear();

        createLevel();

        player.set(roundStartX, roundStartY, PLAYER_W, PLAYER_H);
        olafFloor = roundStartY;

        // 배경음악: 모든 라운드에서 같은 곡을 이어서 재생합니다.
        // 이미 나오고 있으면 처음부터 다시 틀지 않으므로 라운드가 바뀌어도 끊기지 않습니다.
        // (수락 소리가 아직 나오고 있으면 멈춥니다)
        Music bgm = gameMusic;

        if (bgm != null && acceptMusic != null && acceptMusic.isPlaying()) {
            acceptMusic.stop();
        }

        playMusic(bgm);
        updateMusicVolume();
        sfx("round_start", 0.9f, 0f);

        for (int i = 0; i < 10; i++) {
            spawnMinion();
        }

        cameraSnap = true;
    }

    // =====================================================================
    //  맵 구성
    //  모든 점프 구간은 실제 이동/점프 수치로 시뮬레이션해서 통과 가능함을 확인했습니다.
    // =====================================================================
    private void createLevel() {
        platforms.clear();
        platformTextureIndices.clear();
        platformKinds.clear();
        movers.clear();
        traps.clear();
        checkpoints.clear();
        decorations.clear();
        pits.clear();
        spawnSurfaces.clear();
        groundSegments.clear();
        monsters.clear();

        if (round == 1) {
            // ===== 1라운드: TOP LANE (길이 8400, 바론 3마리, 높은 산 1개) =====
            worldWidth = 8400f;
            roundName = "TOP LANE";

            // 땅 (x, 너비, 윗면 높이) - 사이의 빈 공간이 낙하 구간입니다.
            addGround(0f, 700f, 80f);
            addGround(700f, 300f, 150f);
            addGround(1000f, 300f, 220f);
            addGround(1300f, 700f, 150f);
            addGround(2180f, 800f, 130f);
            addGround(2980f, 520f, 120f);
            addGround(3500f, 420f, 540f);   // 높은 산: 계단 발판으로 오르거나 스킬로 넘어가세요
            addGround(3920f, 600f, 150f);
            addGround(4870f, 900f, 150f);
            addGround(5880f, 500f, 220f);
            addGround(6480f, 300f, 140f);
            addGround(6900f, 1500f, 140f);

            // 떠 있는 섬 (x, 윗면 높이, 너비)
            addLedge(3270f, 225f, 100f);
            addLedge(3380f, 330f, 100f);
            addLedge(3270f, 435f, 100f);
            addLedge(6560f, 260f, 120f);
            addLedge(7050f, 260f, 140f);
            addLedge(7450f, 300f, 120f);

            // 움직이는 발판 (x, 윗면 높이, 너비, 좌우 이동폭, 상하 이동폭, 주기, 시작 위상)
            addMover(4570f, 160f, 110f, 180f, 0f, 3.2f, 0f);

            // 바론 함정 (가운데 칸의 x)
            addTrap(2560f);
            addTrap(4320f);
            addTrap(5600f);

            // 체크포인트 깃발
            addCheckpoint(2240f);
            addCheckpoint(3980f);
            addCheckpoint(4930f);
            addCheckpoint(6960f);

            // 넥서스
            setNexus(8150f);

            // 전령이 부수는 바위 (x, 너비, 높이)
            addRock(6100f, 60f, 70f);
            addRock(7300f, 60f, 80f);
            addRock(7700f, 50f, 60f);

            // 용 (MON_WIND_DRAGON: 바람용, MON_EARTH_DRAGON: 대지용)과 전령
            addDragon(MON_WIND_DRAGON, 1650f);
            addDragon(MON_EARTH_DRAGON, 3710f);
            addHerald(5300f);
        } else if (round == 2) {
            // ===== 2라운드: RIVER (길이 10200, 바론 5마리, 높은 산 1개) =====
            worldWidth = 10200f;
            roundName = "RIVER";

            // 땅 (x, 너비, 윗면 높이) - 사이의 빈 공간이 낙하 구간입니다.
            addGround(0f, 600f, 80f);
            addGround(600f, 300f, 170f);
            addGround(1100f, 700f, 170f);
            addGround(1800f, 150f, 270f);
            addGround(1950f, 450f, 360f);
            addGround(2870f, 580f, 240f);
            addGround(3900f, 450f, 300f);
            addGround(4350f, 700f, 110f);
            addGround(5050f, 700f, 120f);
            addGround(5750f, 400f, 645f);   // 높은 산: 계단 발판으로 오르거나 스킬로 넘어가세요
            addGround(6150f, 600f, 200f);
            addGround(6750f, 120f, 290f);
            addGround(7620f, 900f, 300f);
            addGround(8700f, 1500f, 260f);

            // 떠 있는 섬 (x, 윗면 높이, 너비)
            addLedge(2500f, 330f, 80f);
            addLedge(2690f, 290f, 80f);
            addLedge(4650f, 260f, 90f);
            addLedge(4870f, 300f, 90f);
            addLedge(5520f, 225f, 100f);
            addLedge(5630f, 330f, 100f);
            addLedge(5520f, 435f, 100f);
            addLedge(5630f, 540f, 100f);

            // 움직이는 발판 (x, 윗면 높이, 너비, 좌우 이동폭, 상하 이동폭, 주기, 시작 위상)
            addMover(3580f, 160f, 110f, 0f, 220f, 3f, 0f);
            addMover(6930f, 290f, 100f, 200f, 0f, 2.6f, 0f);
            addMover(7250f, 330f, 100f, 200f, 0f, 2.6f, 0.5f);

            // 바론 함정 (가운데 칸의 x)
            addTrap(1500f);
            addTrap(3250f);
            addTrap(4700f);
            addTrap(6560f);
            addTrap(8200f);

            // 체크포인트 깃발
            addCheckpoint(2950f);
            addCheckpoint(3970f);
            addCheckpoint(6220f);
            addCheckpoint(7680f);
            addCheckpoint(8760f);

            // 넥서스
            setNexus(9900f);

            // 전령이 부수는 바위 (x, 너비, 높이)
            addRock(8420f, 60f, 70f);
            addRock(9000f, 60f, 70f);
            addRock(9350f, 70f, 90f);

            // 용 (MON_WIND_DRAGON: 바람용, MON_EARTH_DRAGON: 대지용)과 전령
            addDragon(MON_WIND_DRAGON, 2200f);
            addDragon(MON_EARTH_DRAGON, 5950f);
            addHerald(7800f);
        } else {
            // ===== 3라운드: ENEMY BASE (길이 11800, 바론 7마리, 높은 산 2개) =====
            worldWidth = 11800f;
            roundName = "ENEMY BASE";

            // 땅 (x, 너비, 윗면 높이) - 사이의 빈 공간이 낙하 구간입니다.
            addGround(0f, 600f, 80f);
            addGround(820f, 260f, 80f);
            addGround(1680f, 400f, 420f);
            addGround(2080f, 900f, 200f);
            addGround(3680f, 400f, 380f);
            addGround(4980f, 600f, 300f);
            addGround(5580f, 680f, 160f);
            addGround(6260f, 500f, 685f);   // 높은 산: 계단 발판으로 오르거나 스킬로 넘어가세요
            addGround(6760f, 800f, 200f);
            addGround(8260f, 200f, 200f);
            addGround(8460f, 250f, 120f);
            addGround(8960f, 500f, 120f);
            addGround(9460f, 300f, 220f);
            addGround(9760f, 900f, 320f);
            addGround(10660f, 350f, 620f);   // 높은 산: 계단 발판으로 오르거나 스킬로 넘어가세요
            addGround(11010f, 790f, 300f);

            // 떠 있는 섬 (x, 윗면 높이, 너비)
            addLedge(1160f, 180f, 90f);
            addLedge(1340f, 280f, 90f);
            addLedge(1520f, 380f, 90f);
            addLedge(4210f, 400f, 70f);
            addLedge(4390f, 360f, 70f);
            addLedge(4570f, 420f, 70f);
            addLedge(4760f, 380f, 70f);
            addLedge(6030f, 265f, 100f);
            addLedge(6140f, 370f, 100f);
            addLedge(6030f, 475f, 100f);
            addLedge(6140f, 580f, 100f);
            addLedge(10430f, 425f, 100f);
            addLedge(10540f, 530f, 100f);

            // 움직이는 발판 (x, 윗면 높이, 너비, 좌우 이동폭, 상하 이동폭, 주기, 시작 위상)
            addMover(3080f, 120f, 100f, 0f, 220f, 2.8f, 0f);
            addMover(3360f, 200f, 100f, 0f, 220f, 2.4f, 0.35f);
            addMover(7610f, 300f, 90f, 340f, 0f, 3f, 0f);

            // 바론 함정 (가운데 칸의 x)
            addTrap(2400f);
            addTrap(2780f);
            addTrap(5300f);
            addTrap(7150f);
            addTrap(9180f);
            addTrap(10050f);
            addTrap(11250f);

            // 체크포인트 깃발
            addCheckpoint(1730f);
            addCheckpoint(3730f);
            addCheckpoint(5040f);
            addCheckpoint(6830f);
            addCheckpoint(8530f);
            addCheckpoint(9820f);
            addCheckpoint(10720f);

            // 넥서스
            setNexus(11600f);

            // 전령이 부수는 바위 (x, 너비, 높이)
            addRock(7400f, 60f, 70f);
            addRock(10260f, 50f, 60f);
            addRock(11450f, 60f, 70f);

            // 용 (MON_WIND_DRAGON: 바람용, MON_EARTH_DRAGON: 대지용)과 전령
            addDragon(MON_WIND_DRAGON, 420f);
            addDragon(MON_EARTH_DRAGON, 1880f);
            addHerald(6500f);
        }

        roundStartX = 100f;
        roundStartY = groundTopAt(roundStartX + PLAYER_W / 2f);
        respawnX = roundStartX;
        respawnY = roundStartY;

        buildPits();
        buildSpawnSurfaces();
        buildDecorations();
    }

    private void addGround(float x, float width, float top) {
        platforms.add(
            new Rectangle(x, GROUND_BOTTOM, width, top - GROUND_BOTTOM)
        );
        platformKinds.add(KIND_GROUND);
        platformTextureIndices.add(
            (round - 1) % groundTextures.length
        );
    }

    private void addLedge(float x, float top, float width) {
        platforms.add(
            new Rectangle(x, top - LEDGE_THICK, width, LEDGE_THICK)
        );
        platformKinds.add(KIND_LEDGE);
        platformTextureIndices.add(
            (round - 1) % groundTextures.length
        );
    }

    private void addMover(
        float x,
        float top,
        float width,
        float rangeX,
        float rangeY,
        float period,
        float phase
    ) {
        Mover m = new Mover();
        m.rect = new Rectangle(x, top - LEDGE_THICK, width, LEDGE_THICK);
        m.baseX = x;
        m.baseTop = top;
        m.rangeX = rangeX;
        m.rangeY = rangeY;
        m.period = period;
        m.phase = phase;
        movers.add(m);

        platforms.add(m.rect);
        platformKinds.add(KIND_MOVER);
        platformTextureIndices.add(
            (round - 1) % groundTextures.length
        );

        positionMover(m);
    }

    // 바위: 땅 위 x에 너비 width, 높이 height
    private void addRock(float x, float width, float height) {
        float top = groundTopAt(x + width / 2f);

        platforms.add(new Rectangle(x, top, width, height));
        platformKinds.add(KIND_ROCK);
        platformTextureIndices.add(
            (round - 1) % groundTextures.length
        );
    }

    private void addTrap(float centerX) {
        PillarTrap t = new PillarTrap();
        t.centerX = centerX;
        t.groundTop = groundTopAt(centerX);

        float half = CELL / 2f;

        t.cellX[0] = centerX - half - CELL * 3f;
        t.cellX[1] = centerX - half - CELL * 2f;
        t.cellX[2] = centerX - half - CELL;
        t.cellX[3] = centerX + half;
        t.cellX[4] = centerX + half + CELL;
        t.cellX[5] = centerX + half + CELL * 2f;

        t.state = 0;
        t.timer = 0.8f + random.nextFloat() * 1.6f;
        t.maxHeight = 1450f - t.groundTop;

        traps.add(t);
    }

    private void addCheckpoint(float x) {
        Checkpoint c = new Checkpoint();
        c.x = x;
        c.y = groundTopAt(x);
        checkpoints.add(c);
    }

    private void setNexus(float x) {
        float top = groundTopAt(x + 45f);
        nexus.set(x, top, 90f, 110f);
    }

    // 바람용(MON_WIND_DRAGON) 또는 대지용(MON_EARTH_DRAGON)
    private void addDragon(int kind, float x) {
        Monster m = new Monster();
        m.kind = kind;

        float top = groundTopAt(x);

        m.baseY = top + 26f;
        m.rect.set(x - DRAGON_W / 2f, m.baseY, DRAGON_W, DRAGON_H);
        m.seed = random.nextFloat() * 10f;
        monsters.add(m);
    }

    private void addHerald(float x) {
        Monster m = new Monster();
        m.kind = MON_HERALD;

        float top = groundTopAt(x);

        m.baseY = top;
        m.rect.set(x - HERALD_W / 2f, top, HERALD_W, HERALD_H);

        Rectangle seg = groundSegmentAt(x);

        float segMin = seg != null ? seg.x + 20f : x - 200f;
        float segMax = seg != null ? seg.x + seg.width - 20f - HERALD_W : x + 200f;

        m.minX = Math.max(segMin, m.rect.x - 90f);
        m.maxX = Math.min(segMax, m.rect.x + 90f);
        m.seed = random.nextFloat() * 10f;
        monsters.add(m);
    }

    private float groundTopAt(float x) {
        for (int i = 0; i < platforms.size; i++) {
            if (platformKinds.get(i) != KIND_GROUND) continue;

            Rectangle p = platforms.get(i);

            if (x >= p.x && x <= p.x + p.width) {
                return p.y + p.height;
            }
        }

        return 80f;
    }

    private Rectangle groundSegmentAt(float x) {
        for (int i = 0; i < platforms.size; i++) {
            if (platformKinds.get(i) != KIND_GROUND) continue;

            Rectangle p = platforms.get(i);

            if (x >= p.x && x <= p.x + p.width) {
                return p;
            }
        }

        return null;
    }

    private void buildPits() {
        for (int i = 0; i < platforms.size; i++) {
            if (platformKinds.get(i) == KIND_GROUND) {
                groundSegments.add(platforms.get(i));
            }
        }

        groundSegments.sort((a, b) -> Float.compare(a.x, b.x));

        for (int i = 0; i + 1 < groundSegments.size; i++) {
            Rectangle a = groundSegments.get(i);
            Rectangle b = groundSegments.get(i + 1);

            float end = a.x + a.width;

            if (b.x - end > 1f) {
                pits.add(new float[] {
                    end,
                    b.x,
                    a.y + a.height,
                    b.y + b.height
                });
            }
        }
    }

    private void buildSpawnSurfaces() {
        for (int i = 0; i < platforms.size; i++) {
            int kind = platformKinds.get(i);
            Rectangle p = platforms.get(i);

            if (kind == KIND_MOVER) continue;
            if (p.width < 130f) continue;

            spawnSurfaces.add(p);
        }
    }

    private void buildDecorations() {
        Random r = new Random(round * 7919L);

        for (int i = 0; i < platforms.size; i++) {
            if (platformKinds.get(i) != KIND_GROUND) continue;

            Rectangle g = platforms.get(i);

            if (g.width < 160f) continue;

            float top = g.y + g.height;
            int count = Math.max(1, (int) (g.width / 300f));

            for (int k = 0; k < count; k++) {
                float x = g.x + 40f + r.nextFloat() * (g.width - 80f);

                if (!isDecorationSpotFree(x)) continue;

                Deco d = new Deco();
                d.kind = r.nextFloat() < 0.72f ? 0 : 1;
                d.x = x;
                d.y = top;
                d.size = 0.8f + r.nextFloat() * 0.5f;
                d.seed = r.nextFloat() * 100f;
                decorations.add(d);
            }
        }
    }

    private boolean isDecorationSpotFree(float x) {
        if (Math.abs(x - roundStartX) < 160f) return false;

        for (PillarTrap t : traps) {
            if (Math.abs(x - t.centerX) < 210f) return false;
        }

        for (Checkpoint c : checkpoints) {
            if (Math.abs(x - c.x) < 70f) return false;
        }

        for (Monster m : monsters) {
            if (x > m.rect.x - 60f && x < m.rect.x + m.rect.width + 60f) {
                return false;
            }
        }

        if (x > nexus.x - 90f && x < nexus.x + nexus.width + 90f) {
            return false;
        }

        for (int i = 0; i < platforms.size; i++) {
            if (platformKinds.get(i) != KIND_ROCK) continue;

            Rectangle rk = platforms.get(i);

            if (x > rk.x - 60f && x < rk.x + rk.width + 60f) return false;
        }

        return true;
    }

    private void spawnMinion() {
        if (minions.size >= MAX_MINIONS) return;
        if (spawnSurfaces.size == 0) return;

        float totalWidth = 0f;

        for (Rectangle s : spawnSurfaces) {
            totalWidth += s.width;
        }

        for (int attempt = 0; attempt < 10; attempt++) {
            float pick = random.nextFloat() * totalWidth;
            Rectangle platform = spawnSurfaces.first();

            for (Rectangle s : spawnSurfaces) {
                pick -= s.width;

                if (pick <= 0f) {
                    platform = s;
                    break;
                }
            }

            int type = random.nextInt(3);

            float speed;

            if (type == 0) {
                speed = 100f;
            } else if (type == 1) {
                speed = 135f;
            } else {
                speed = 75f;
            }

            float width = type == 2 ? 66f : 52f;

            float minX = platform.x;
            float maxX = platform.x + platform.width;

            float available = Math.max(
                0f,
                maxX - minX - width
            );

            float x = minX + random.nextFloat() * available;
            float y = platform.y + platform.height;

            if (Math.abs(x - player.x) < 320f) continue;

            // 바위가 있으면 바위 사이에서만 돌아다닙니다.
            boolean blocked = false;

            for (int i = 0; i < platforms.size; i++) {
                if (platformKinds.get(i) != KIND_ROCK) continue;

                Rectangle rk = platforms.get(i);

                if (Math.abs(rk.y - y) > 1f) continue;
                if (rk.x + rk.width < minX || rk.x > maxX) continue;

                if (rk.x + rk.width <= x) {
                    minX = Math.max(minX, rk.x + rk.width);
                } else if (rk.x >= x + width) {
                    maxX = Math.min(maxX, rk.x);
                } else {
                    blocked = true;
                }
            }

            if (blocked) continue;

            Minion m = new Minion(
                x, y, type, speed, minX, maxX
            );

            m.direction = random.nextBoolean() ? 1f : -1f;
            minions.add(m);
            return;
        }
    }

    private void updateMinions(float delta) {
        spawnTimer += delta;

        if (spawnTimer >= 1.55f) {
            spawnTimer = 0f;
            spawnMinion();

            if (minions.size < MAX_MINIONS
                && random.nextFloat() < 0.35f) {
                spawnMinion();
            }
        }

        Iterator<Minion> iterator = minions.iterator();

        while (iterator.hasNext()) {
            Minion m = iterator.next();

            float oldY = m.rect.y;

            m.rect.x += m.direction * m.speed * delta;

            if (m.rect.x <= m.minX) {
                m.rect.x = m.minX;
                m.direction = 1f;
            }

            if (m.rect.x + m.rect.width >= m.maxX) {
                m.rect.x = m.maxX - m.rect.width;
                m.direction = -1f;
            }

            m.velocityY -= MINION_GRAVITY * delta;
            m.rect.y += m.velocityY * delta;

            for (Rectangle p : platforms) {
                boolean horizontal =
                    m.rect.x + m.rect.width > p.x
                        && m.rect.x < p.x + p.width;

                boolean crossedTop =
                    oldY >= p.y + p.height - 2f
                        && m.rect.y <= p.y + p.height;

                if (horizontal
                    && crossedTop
                    && m.velocityY <= 0f) {
                    m.rect.y = p.y + p.height;
                    m.velocityY = 0f;
                }
            }

            if (m.rect.y < -200f) {
                iterator.remove();
            }
        }
    }

    // =====================================================================
    //  입력
    // =====================================================================
    private void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = true;
            updateMusicVolume();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            debugDraw = !debugDraw;
        }

        velocityX = 0f;

        // 전령 탑승 중 Q: 지금 위치에서 내립니다. (올라타는 동작 중에는 무시)
        if (riding) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.Q) && rideState != 0) {
                dismountHerald();
            }
            return;
        }

        // 날아가는 중, 리 신 돌진 중, 자크 웅크리는 중에는 조작 불가
        if (launched || leeDashing || zacWindup > 0f) return;

        // 퀸이 발러를 타고 나는 중: 좌우 이동, 위(↑ 또는 스페이스)/아래(↓)로 고도 조절
        if (isFlying()) {
            handleFlightInput();
            return;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            velocityX = -moveSpeed();
            facingRight = false;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            velocityX = moveSpeed();
            facingRight = true;
        }

        if (velocityX != 0f) {
            walkTimer += Gdx.graphics.getDeltaTime();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            jumpBufferTimer = JUMP_BUFFER;
        }

        // 점프할 때 standingOn은 지우지 않습니다.
        // (이번 프레임에도 움직이는 발판과 함께 이동해야 발판에 밀려나지 않습니다)
        if (jumpBufferTimer > 0f && coyoteTimer > 0f) {
            velocityY = jumpSpeed();
            onGround = false;
            coyoteTimer = 0f;
            jumpBufferTimer = 0f;

            setSquash(0.8f, 1.22f);
            spawnDust(player.x + player.width / 2f, player.y, 5, 0.5f);
            sfx("jump", 0.45f, 0.1f);
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            attackBufferTimer = 0.25f;
        }

        if (attackBufferTimer > 0f && attackCooldown <= 0f) {
            attackBufferTimer = 0f;
            basicAttack();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            ultBufferTimer = 0.25f;
        }

        if (ultBufferTimer > 0f) {
            ultBufferTimer = 0f;
            useUltimate();
        }

        // 리 신 궁극기 조준 중 마우스 클릭으로 음파를 발사합니다.
        if (champion == 2
            && leeAimMode
            && Gdx.input.justTouched()) {

            Vector3 click = new Vector3(
                Gdx.input.getX(),
                Gdx.input.getY(),
                0f
            );

            camera.unproject(click);

            aimWorldX = Math.max(
                0f,
                Math.min(worldWidth, click.x)
            );

            aimWorldY = Math.max(0f, click.y);

            launchSonicWave(aimWorldX, aimWorldY);

            leeAimMode = false;
        }
    }

    private boolean isFlying() {
        return champion == QUINN && valorState == VALOR_CARRYING;
    }

    private void handleFlightInput() {
        float speed = moveSpeed() * 1.3f;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            velocityX = -speed;
            facingRight = false;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            velocityX = speed;
            facingRight = true;
        }

        boolean up = Gdx.input.isKeyPressed(Input.Keys.UP)
            || Gdx.input.isKeyPressed(Input.Keys.SPACE);
        boolean down = Gdx.input.isKeyPressed(Input.Keys.DOWN);

        if (up && !down) {
            velocityY = FLY_VERTICAL_SPEED;
        } else if (down && !up) {
            velocityY = -FLY_VERTICAL_SPEED;
        } else {
            velocityY = 0f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            attackBufferTimer = 0.25f;
        }

        if (attackBufferTimer > 0f && attackCooldown <= 0f) {
            attackBufferTimer = 0f;
            basicAttack();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            dropFromValor();
        }
    }

    private float moveSpeed() {
        return MOVE_SPEED * (windBuff ? 1.4f : 1f);
    }

    private float jumpSpeed() {
        return JUMP_SPEED * (windBuff ? 1.15f : 1f);
    }

    private int maxHits() {
        return earthBuff ? 5 : 3;
    }

    // =====================================================================
    //  전투
    // =====================================================================
    private void basicAttack() {
        // 올라프는 도끼를 던집니다.
        if (champion == OLAF) {
            throwAxe();
            return;
        }

        // 퀸은 원거리 공격: 화살을 쏩니다.
        if (champion == QUINN) {
            fireArrow();
            return;
        }

        attackTimer = 0.18f;
        slashTimer = 0.22f;
        attackCooldown = ATTACK_COOLDOWN;

        // 챔피언별 평타 소리
        sfx(championKey() + "_attack", 0.8f, 0.08f);

        slashSide = facingRight ? 1f : -1f;
        attackDirection = facingRight ? 1f : -1f;

        float range = 100f;

        Rectangle attackBox;

        if (facingRight) {
            attackBox = new Rectangle(
                player.x + player.width,
                player.y + 5f,
                range,
                player.height - 10f
            );
        } else {
            attackBox = new Rectangle(
                player.x - range,
                player.y + 5f,
                range,
                player.height - 10f
            );
        }

        Iterator<Minion> iterator = minions.iterator();

        while (iterator.hasNext()) {
            Minion m = iterator.next();

            if (attackBox.overlaps(m.rect)) {
                cs += m.getCS();
                spawnBurst(m.rect.x + m.rect.width / 2f, m.rect.y + m.rect.height / 2f,
                    8, 1f, 0.9f, 0.5f);
                sfx("hit", 0.7f);
                sfx("cs", 0.5f, 0.04f);
                iterator.remove();
            }
        }

        // 용과 전령은 평타 5대에 쓰러집니다.
        for (int i = 0; i < monsters.size; i++) {
            Monster m = monsters.get(i);

            if (!m.alive || m == rideHerald) continue;

            if (attackBox.overlaps(m.rect)) {
                damageMonster(m);
            }
        }
    }

    private void damageMonster(Monster m) {
        if (!m.alive || m.hp <= 0) return;

        m.hp--;
        m.flash = 0.14f;
        m.punch = 1f;

        hitStop = 0.055f;
        shakeTimer = Math.max(shakeTimer, 0.08f);
        sfx("hit_monster", 0.9f);

        float cx = m.rect.x + m.rect.width / 2f;
        float cy = m.rect.y + m.rect.height / 2f;

        spawnBurst(cx, cy, 10, 1f, 0.9f, 0.6f);
        spawnRing(cx, cy, 14f, 50f, 0.3f, 1f, 0.95f, 0.7f, 0.8f);

        if (m.hp <= 0) {
            onMonsterDeath(m);
        }
    }

    private void onMonsterDeath(Monster m) {
        float cx = m.rect.x + m.rect.width / 2f;
        float cy = m.rect.y + m.rect.height / 2f;

        if (m.kind == MON_HERALD) {
            startRide(m);
            return;
        }

        m.alive = false;
        m.deathTimer = 0.8f;
        dragonsSlain++;
        sfx("dragon_slain", 1f, 0f);
        shakeTimer = 0.3f;
        hitStop = 0.12f;

        // 용 색깔의 투명한 구슬이 챔피언에게 빨려 들어갑니다.
        float r, g, b;

        if (m.kind == MON_EARTH_DRAGON) {
            r = 0.62f; g = 0.42f; b = 0.22f;
            earthBuff = true;
            showToast("EARTH DRAGON SLAIN", "MAX HP  3 -> 5", r + 0.25f, g + 0.25f, b + 0.2f);
        } else {
            r = 0.78f; g = 0.80f; b = 0.84f;
            windBuff = true;
            showToast("WIND DRAGON SLAIN", "MOVE SPEED UP  /  JUMP UP", 0.9f, 0.93f, 1f);
        }

        for (int i = 0; i < 18; i++) {
            float ang = random.nextFloat() * (float) Math.PI * 2f;
            float sp = 160f + random.nextFloat() * 280f;

            Particle p = addParticle(P_ORB, cx, cy,
                (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp + 120f,
                2.6f, 7f + random.nextFloat() * 6f, r, g, b, 1f);

            if (p != null) p.drag = 2.5f;
        }

        spawnRing(cx, cy, 20f, 160f, 0.6f, r, g, b, 0.9f);
        spawnBurst(cx, cy, 20, r + 0.2f, g + 0.2f, b + 0.2f);
    }

    // 한글 문구: 한글 글꼴이 있으면 한글로, 없으면 영어로 표시합니다.
    private void showToastKr(String krTitle, String krSub, String enTitle, String enSub,
                             float r, float g, float b) {
        if (krFont != null) {
            showToast(krTitle, krSub, r, g, b);
            toastKorean = true;
        } else {
            showToast(enTitle, enSub, r, g, b);
        }
    }

    private void showToast(String title, String sub, float r, float g, float b) {
        toastKorean = false;
        toastTitle = title;
        toastSub = sub;
        toastTimer = 3f;
        bannerTimer = 0f;
        toastColor.set(Math.min(1f, r), Math.min(1f, g), Math.min(1f, b), 1f);
    }

    // =====================================================================
    //  궁극기
    // =====================================================================
    private void useUltimate() {
        // ----- 자크 -----
        if (champion == 0) {
            if (zacActive) {
                if (zacWindup > 0f) return;

                if (onGround) startZacWindup();
                else releaseZacLeap();
                return;
            }

            if (cs < 10) return;

            cs -= 10;
            zacActive = true;
            zacTimer = 7f;
            voice("zac_r");

            if (onGround) startZacWindup();
            else releaseZacLeap();
            return;
        }

        // ----- 리 신 -----
        // R → 조준 → 클릭으로 음파 발사 → 맞으면 표식 → R 다시 누르면 표식으로 날아감
        if (champion == 2) {
            if (leeDashing) return;

            if (markActive) {
                startLeeDash();
                return;
            }

            if (leeUltTimer > 0f) {
                leeAimMode = true;
                return;
            }

            if (cs < 10) return;

            cs -= 10;
            leeUltTimer = 7f;
            leeAimMode = true;
            sonicWaveActive = false;
            return;
        }

        // ----- 퀸 -----
        // R → 발러가 멀리서 날아와 소용돌이처럼 돌며 내려와 퀸을 태움 → 7초 비행
        // 비행 중 R → 그 자리에서 내림
        if (champion == QUINN) {
            if (valorState == VALOR_CARRYING) {
                dropFromValor();
                return;
            }

            if (valorState == VALOR_ARRIVING || valorState == VALOR_SPIRAL) return;

            if (cs < 10) return;

            cs -= 10;
            summonValor();
            return;
        }

        // ----- 올라프 -----
        if (cs < 10) return;

        cs -= 10;
        olafActive = true;
        olafTimer = 7f;
        voice("olaf_r");

        float cx = player.x + player.width / 2f;
        float cy = player.y + player.height / 2f;

        spawnRing(cx, cy, 20f, 120f, 0.5f, 1f, 0.45f, 0.15f, 0.9f);
        spawnBurst(cx, cy, 16, 1f, 0.55f, 0.2f);
        showToast("RAGNAROK", "INVULNERABLE  /  PHASE THROUGH TERRAIN", 1f, 0.6f, 0.3f);
    }

    // =====================================================================
    //  올라프 Q: 도끼 던지기
    //  회전하며 포물선으로 날아가 미니언을 관통하고, 땅이나 벽에 박힙니다.
    //  박힌 도끼를 주우면 바로 다시 던질 수 있습니다. (안 주우면 1초 뒤)
    // =====================================================================
    private void throwAxe() {
        // 이전에 던진 도끼가 남아 있으면 사라집니다. (도끼는 하나)
        if (axeState == AXE_STUCK) {
            spawnBurst(axeX, axeY, 6, 0.8f, 0.82f, 0.88f);
        }

        float dir = facingRight ? 1f : -1f;

        axeState = AXE_FLYING;
        axeX = player.x + player.width / 2f + dir * 20f;
        // 낮고 평평하게 던져 미니언 키 높이로 날아갑니다. (미니언을 넘어가지 않게)
        axeY = player.y + player.height * 0.4f;
        axeVX = dir * AXE_SPEED_X;
        axeVY = AXE_SPEED_Y;
        axeSpin = 0f;
        axeTimer = 0f;
        axePlatform = null;
        axeHits.clear();

        attackCooldown = OLAF_AXE_COOLDOWN;
        attackDirection = dir;

        setSquash(1.12f, 0.9f);
        sfx("axe_throw", 0.85f, 0.08f);
    }

    private void updateAxe(float delta) {
        if (axeState == AXE_NONE) return;

        axeTimer += delta;

        if (axeState == AXE_FLYING) {
            axeVY -= AXE_GRAVITY * delta;
            axeSpin += (axeVX > 0f ? -1f : 1f) * 1080f * delta;

            float dx = axeVX * delta;
            float dy = axeVY * delta;
            int steps = Math.max(1, (int) (Math.sqrt(dx * dx + dy * dy) / 4f));

            for (int k = 0; k < steps; k++) {
                float prevY = axeY;

                axeX += dx / steps;
                axeY += dy / steps;

                // 날 부분이 넓어서 미니언은 크게, 지형은 작게 판정합니다.
                tmpRect.set(axeX - 14f, axeY - 14f, 28f, 28f);

                Iterator<Minion> it = minions.iterator();

                while (it.hasNext()) {
                    Minion m = it.next();

                    if (tmpRect.overlaps(m.rect)) {
                        cs += m.getCS();
                        spawnBurst(m.rect.x + m.rect.width / 2f, m.rect.y + m.rect.height / 2f,
                            8, 1f, 0.6f, 0.35f);
                        sfx("hit", 0.7f);
                        sfx("cs", 0.5f, 0.04f);
                        it.remove();
                    }
                }

                for (int i = 0; i < monsters.size; i++) {
                    Monster m = monsters.get(i);

                    if (!m.alive || m == rideHerald || axeHits.contains(m, true)) continue;

                    if (tmpRect.overlaps(m.rect)) {
                        axeHits.add(m);
                        damageMonster(m);
                    }
                }

                if (axeState != AXE_FLYING) break;

                tmpRect2.set(axeX - 4f, axeY - 4f, 8f, 8f);

                for (int i = 0; i < platforms.size; i++) {
                    Rectangle p = platforms.get(i);

                    if (!tmpRect2.overlaps(p)) continue;

                    float top = p.y + p.height;

                    if (axeVY <= 0f && prevY - 4f >= top - 6f) {
                        // 위에서 떨어져 땅에 박힘: 날이 아래로
                        axeY = top + 8f;
                        axeStuckAngle = axeVX > 0f ? -70f : -110f;
                    } else {
                        // 벽에 박힘: 날이 벽 쪽으로
                        axeX = axeVX > 0f ? p.x - 6f : p.x + p.width + 6f;
                        axeStuckAngle = axeVX > 0f ? 0f : 180f;
                    }

                    axeState = AXE_STUCK;
                    axeTimer = 0f;
                    axePlatform = p;

                    spawnDust(axeX, axeY - 8f, 6, 0.6f);
                    spawnBurst(axeX, axeY, 5, 1f, 0.9f, 0.7f);
                    sfx("axe_stick", 0.8f, 0.06f);
                    shakeTimer = Math.max(shakeTimer, 0.06f);
                    break;
                }

                if (axeState != AXE_FLYING) break;

                if (axeY < -250f || axeX < -50f || axeX > worldWidth + 50f) {
                    // 구멍에 빠진 도끼는 사라집니다. (재사용 대기시간은 그대로)
                    axeState = AXE_NONE;
                    break;
                }
            }

            return;
        }

        // 박힌 도끼: 박힌 지형이 부서지면 사라집니다.
        if (axePlatform != null && !platforms.contains(axePlatform, true)) {
            spawnBurst(axeX, axeY, 6, 0.8f, 0.82f, 0.88f);
            axeState = AXE_NONE;
            return;
        }

        // 올라프가 닿으면 주워서 바로 다시 던질 수 있습니다.
        if (axeTimer > 0.15f) {
            tmpRect.set(player.x - 18f, player.y - 10f, player.width + 36f, player.height + 20f);

            if (tmpRect.contains(axeX, axeY)) {
                axeState = AXE_NONE;
                attackCooldown = 0f;

                spawnRing(axeX, axeY, 8f, 46f, 0.3f, 1f, 0.85f, 0.45f, 0.9f);
                spawnBurst(axeX, axeY, 8, 1f, 0.85f, 0.45f);
                sfx("axe_pickup", 0.8f, 0.04f);
            }
        }
    }

    // 도끼 그리기 (날 쪽이 +x 방향)
    private void drawOlafAxe() {
        if (axeState == AXE_NONE) return;

        float ang = axeState == AXE_FLYING ? axeSpin : axeStuckAngle;
        float rad = (float) Math.toRadians(ang);
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);

        // 날아가는 동안 회전 잔상
        if (axeState == AXE_FLYING) {
            shapes.setColor(0.9f, 0.92f, 1f, 0.18f);
            drawArc(axeX, axeY, 26f, rad, rad + 2.4f, 3f, 10);
        } else {
            // 주울 수 있다는 표시
            float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 6f);
            float bob = (float) Math.sin(animTime * 4f) * 4f;

            shapes.setColor(1f, 0.82f, 0.4f, 0.18f + 0.15f * pulse);
            shapes.circle(axeX, axeY, 24f, 20);
            shapes.setColor(1f, 0.85f, 0.45f, 0.9f);
            shapes.triangle(axeX - 6f, axeY + 40f + bob, axeX + 6f, axeY + 40f + bob, axeX, axeY + 31f + bob);
        }

        // 자루
        float hx0 = axeX + (-23f) * c;
        float hy0 = axeY + (-23f) * s;
        float hx1 = axeX + 16f * c;
        float hy1 = axeY + 16f * s;

        shapes.setColor(0.42f, 0.26f, 0.14f, 1f);
        shapes.rectLine(hx0, hy0, hx1, hy1, 5f);
        shapes.setColor(0.6f, 0.4f, 0.22f, 1f);
        shapes.rectLine(hx0, hy0, hx1, hy1, 2f);

        // 날 (로컬 좌표를 회전해서 그립니다)
        float[] blade = {8f, 4f, 26f, 20f, 30f, -17f, 8f, -6f};
        float[] w = axeBladePts;

        for (int i = 0; i < 8; i += 2) {
            w[i] = axeX + blade[i] * c - blade[i + 1] * s;
            w[i + 1] = axeY + blade[i] * s + blade[i + 1] * c;
        }

        shapes.setColor(0.70f, 0.74f, 0.80f, 1f);
        shapes.triangle(w[0], w[1], w[2], w[3], w[4], w[5]);
        shapes.triangle(w[0], w[1], w[4], w[5], w[6], w[7]);

        // 날 끝 하이라이트
        shapes.setColor(0.95f, 0.97f, 1f, 1f);
        shapes.rectLine(w[2], w[3], w[4], w[5], 2f);

        // 붉은 룬
        float rx = axeX + 12f * c;
        float ry = axeY + 12f * s;

        shapes.setColor(0.85f, 0.2f, 0.15f, 1f);
        shapes.circle(rx, ry, 2.6f, 8);
    }

    // =====================================================================
    //  퀸
    // =====================================================================

    // 앞쪽 사거리 안에서 가장 가까운 미니언 / 용 / 전령을 노려 화살을 쏩니다.
    private void fireArrow() {
        attackCooldown = QUINN_ATTACK_COOLDOWN;
        attackDirection = facingRight ? 1f : -1f;

        float dir = facingRight ? 1f : -1f;
        float sx = player.x + player.width / 2f + dir * 18f;
        float sy = player.y + player.height * 0.6f;

        Arrow a = new Arrow();
        a.x = sx;
        a.y = sy;

        float best = ARROW_RANGE;
        float tx = 0f;
        float ty = 0f;
        boolean found = false;

        for (Minion m : minions) {
            float cx = m.rect.x + m.rect.width / 2f;
            float cy = m.rect.y + m.rect.height / 2f;

            if (!inArrowCone(sx, sy, cx, cy, dir)) continue;

            float d = dist(sx, sy, cx, cy);

            if (d < best) {
                best = d;
                tx = cx;
                ty = cy;
                a.targetMinion = m;
                a.targetMonster = null;
                found = true;
            }
        }

        for (int i = 0; i < monsters.size; i++) {
            Monster m = monsters.get(i);

            if (!m.alive || m == rideHerald) continue;

            float cx = m.rect.x + m.rect.width / 2f;
            float cy = m.rect.y + m.rect.height / 2f;

            if (!inArrowCone(sx, sy, cx, cy, dir)) continue;

            float d = dist(sx, sy, cx, cy);

            if (d < best) {
                best = d;
                tx = cx;
                ty = cy;
                a.targetMonster = m;
                a.targetMinion = null;
                found = true;
            }
        }

        if (found) {
            float d = Math.max(1f, dist(sx, sy, tx, ty));
            a.vx = (tx - sx) / d * ARROW_SPEED;
            a.vy = (ty - sy) / d * ARROW_SPEED;
            a.life = 1.2f;
        } else {
            a.vx = dir * ARROW_SPEED;
            a.vy = 0f;
            a.life = ARROW_RANGE / ARROW_SPEED;
        }

        arrows.add(a);

        setSquash(1.08f, 0.94f);
        sfx("quinn_attack", 0.8f, 0.08f);
    }

    private boolean inArrowCone(float sx, float sy, float tx, float ty, float dir) {
        float dx = (tx - sx) * dir;
        float dy = Math.abs(ty - sy);

        return dx > -12f && dx < ARROW_RANGE && dy < 300f;
    }

    private float dist(float ax, float ay, float bx, float by) {
        float dx = bx - ax;
        float dy = by - ay;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void updateArrows(float delta) {
        for (int i = arrows.size - 1; i >= 0; i--) {
            Arrow a = arrows.get(i);

            a.life -= delta;

            if (a.life <= 0f) {
                arrows.removeIndex(i);
                continue;
            }

            // 대상이 살아 있으면 대상 쪽으로 방향을 맞춥니다. (유도 화살)
            float tx = Float.NaN;
            float ty = Float.NaN;

            if (a.targetMinion != null && minions.contains(a.targetMinion, true)) {
                tx = a.targetMinion.rect.x + a.targetMinion.rect.width / 2f;
                ty = a.targetMinion.rect.y + a.targetMinion.rect.height / 2f;
            } else if (a.targetMonster != null && a.targetMonster.alive
                && a.targetMonster != rideHerald) {
                tx = a.targetMonster.rect.x + a.targetMonster.rect.width / 2f;
                ty = a.targetMonster.rect.y + a.targetMonster.rect.height / 2f;
            }

            if (!Float.isNaN(tx)) {
                float d = Math.max(1f, dist(a.x, a.y, tx, ty));
                a.vx = (tx - a.x) / d * ARROW_SPEED;
                a.vy = (ty - a.y) / d * ARROW_SPEED;
            }

            a.trailTimer -= delta;

            if (a.trailTimer <= 0f) {
                a.trailTimer = 0.02f;
                Particle p = addParticle(P_DOT, a.x, a.y, 0f, 0f, 0.18f, 1.6f,
                    0.75f, 0.9f, 1f, 0.6f);

                if (p != null) p.grow = 1.5f;
            }

            if (moveArrow(a, delta)) {
                arrows.removeIndex(i);
            }
        }
    }

    // 짧은 간격으로 나눠 이동하며 충돌을 검사합니다. (빠른 화살이 대상을 건너뛰지 않게)
    // 무언가에 맞으면 true
    private boolean moveArrow(Arrow a, float delta) {
        float dx = a.vx * delta;
        float dy = a.vy * delta;
        int steps = Math.max(1, (int) (Math.sqrt(dx * dx + dy * dy) / 4f));

        for (int k = 0; k < steps; k++) {
            a.x += dx / steps;
            a.y += dy / steps;

            tmpRect.set(a.x - 3f, a.y - 3f, 6f, 6f);

            Iterator<Minion> it = minions.iterator();

            while (it.hasNext()) {
                Minion m = it.next();

                if (tmpRect.overlaps(m.rect)) {
                    cs += m.getCS();
                    spawnBurst(a.x, a.y, 8, 0.8f, 0.95f, 1f);
                    sfx("arrow_hit", 0.7f);
                    sfx("cs", 0.5f, 0.04f);
                    it.remove();
                    return true;
                }
            }

            for (int i = 0; i < monsters.size; i++) {
                Monster m = monsters.get(i);

                if (!m.alive || m == rideHerald) continue;

                if (tmpRect.overlaps(m.rect)) {
                    sfx("arrow_hit", 0.7f);
                    damageMonster(m);
                    return true;
                }
            }

            for (int i = 0; i < platforms.size; i++) {
                if (tmpRect.overlaps(platforms.get(i))) {
                    spawnBurst(a.x, a.y, 4, 0.9f, 0.85f, 0.7f);
                    sfx("arrow_hit", 0.35f, 0.15f);
                    return true;
                }
            }

            if (a.x < 0f || a.x > worldWidth || a.y < -200f || a.y > 1500f) return true;
        }

        return false;
    }

    // 발러 호출: 화면 밖 뒤쪽 하늘에서 날아옵니다.
    private void summonValor() {
        valorState = VALOR_ARRIVING;
        valorTimer = 0f;

        float cx = player.x + player.width / 2f;
        float dir = facingRight ? 1f : -1f;

        valorFromX = cx - dir * 760f;
        valorFromY = player.y + 520f;
        valorX = valorFromX;
        valorY = valorFromY;
        valorPrevX = valorX;
        valorFlapTimer = 0f;

        voice("quinn_r");
        sfx("valor_screech", 1f, 0f);
        showToastKr("발러!", "발러가 날아옵니다", "VALOR!", "INCOMING...", 0.6f, 0.85f, 1f);
    }

    private void updateValor(float delta) {
        if (valorState == VALOR_NONE) return;

        valorTimer += delta;
        valorPrevX = valorX;

        float cx = player.x + player.width / 2f;
        float headY = player.y + player.height;

        if (valorState == VALOR_ARRIVING) {
            // 멀리서 캐릭터 머리 위 높은 곳까지 활강
            float p = Math.min(1f, valorTimer / VALOR_ARRIVE_TIME);
            float e = p * p * (3f - 2f * p);

            float toX = cx + (facingRight ? -150f : 150f);
            float toY = headY + 230f;

            valorX = valorFromX + (toX - valorFromX) * e;
            valorY = valorFromY + (toY - valorFromY) * e
                + (float) Math.sin(p * Math.PI) * 60f;

            if (p >= 1f) {
                valorState = VALOR_SPIRAL;
                valorTimer = 0f;
                valorAngle = facingRight ? (float) Math.PI : 0f;
            }

        } else if (valorState == VALOR_SPIRAL) {
            // 캐릭터 위를 소용돌이처럼 두 바퀴 돌며 내려옵니다.
            float p = Math.min(1f, valorTimer / VALOR_SPIRAL_TIME);

            valorAngle += delta * (float) Math.PI * 4f / VALOR_SPIRAL_TIME;

            float radius = 150f * (1f - p) + 10f;
            float height = 230f * (1f - p) + 18f;

            valorX = cx + (float) Math.cos(valorAngle) * radius;
            valorY = headY + height + (float) Math.sin(valorAngle) * radius * 0.3f;
            valorBehind = Math.sin(valorAngle) > 0;

            if (random.nextFloat() < 0.6f) {
                Particle pt = addParticle(P_DOT, valorX, valorY,
                    (random.nextFloat() - 0.5f) * 40f, -40f - random.nextFloat() * 40f,
                    0.5f, 2f + random.nextFloat() * 2f, 0.8f, 0.92f, 1f, 0.7f);

                if (pt != null) pt.drag = 1.5f;
            }

            if (p >= 1f) {
                pickUpQuinn();
            }

        } else if (valorState == VALOR_CARRYING) {
            flightTimer -= delta;

            if (flightTimer <= 0f) {
                dropFromValor();
            }

        } else if (valorState == VALOR_LEAVING) {
            // 하늘 위로 날아가 사라집니다.
            float dir = facingRight ? 1f : -1f;
            valorX += dir * 420f * delta;
            valorY += 520f * delta;

            if (valorTimer >= VALOR_LEAVE_TIME) {
                valorState = VALOR_NONE;
            }
        }

        // 날갯짓 소리
        if (valorState != VALOR_NONE) {
            valorFlapTimer -= delta;

            if (valorFlapTimer <= 0f) {
                valorFlapTimer = valorState == VALOR_CARRYING ? 0.42f : 0.3f;
                sfx("valor_flap", valorState == VALOR_CARRYING ? 0.35f : 0.5f, 0.1f);
            }
        }
    }

    private void pickUpQuinn() {
        valorState = VALOR_CARRYING;
        valorTimer = 0f;
        flightTimer = VALOR_FLIGHT_TIME;

        velocityY = 260f;
        onGround = false;
        standingOn = null;
        coyoteTimer = 0f;
        zacLeapX = 0f;

        float cx = player.x + player.width / 2f;
        float cy = player.y + player.height / 2f;

        spawnRing(cx, cy, 18f, 130f, 0.45f, 0.7f, 0.9f, 1f, 0.9f);
        spawnBurst(cx, cy, 16, 0.85f, 0.95f, 1f);
        spawnDust(cx, player.y, 10, 1.2f);
        setSquash(0.85f, 1.15f);

        sfx("valor_screech", 0.8f, 0.05f);
        showToastKr("발러와 비행!", "↑ / 스페이스: 위로   ↓: 아래로   Q: 화살   R: 내리기",
            "TAG TEAM!", "UP/SPACE: RISE   DOWN: DIVE   Q: ARROW   R: DROP", 0.6f, 0.85f, 1f);
    }

    // 비행 끝: 퀸은 그 자리에서 떨어지고 발러는 하늘로 날아갑니다.
    private void dropFromValor() {
        if (valorState != VALOR_CARRYING) return;

        valorState = VALOR_LEAVING;
        valorTimer = 0f;
        flightTimer = 0f;

        valorX = player.x + player.width / 2f;
        valorY = player.y + player.height + 20f;
        velocityY = Math.min(velocityY, 0f);

        sfx("valor_drop", 0.8f, 0.05f);
        spawnBurst(valorX, valorY, 10, 0.85f, 0.95f, 1f);
    }

    // 다른 상태로 바뀌어 비행을 강제로 멈춰야 할 때 (바론, 전령, 리스폰)
    private void cancelValor(boolean flyAway) {
        if (valorState == VALOR_NONE) return;

        if (flyAway) {
            valorState = VALOR_LEAVING;
            valorTimer = 0f;
            valorX = player.x + player.width / 2f;
            valorY = player.y + player.height + 20f;
        } else {
            valorState = VALOR_NONE;
        }

        flightTimer = 0f;
    }

    // 자크: 잠깐 웅크렸다가 튀어 오릅니다.
    private void startZacWindup() {
        zacWindup = 0.16f;
        velocityX = 0f;
        setSquash(1.42f, 0.58f);
    }

    private void releaseZacLeap() {
        zacWindup = 0f;
        velocityY = JUMP_SPEED * 2.35f;
        zacLeapX = facingRight ? 430f : -430f;
        onGround = false;
        coyoteTimer = 0f;
        zacAirborne = true;
        zacAirTime = 0f;
        zacSpin = 0f;

        setSquash(0.68f, 1.38f);
        sfx("zac_leap", 0.9f, 0.05f);

        float cx = player.x + player.width / 2f;

        spawnDust(cx, player.y, 12, 1f);
        spawnJelly(cx, player.y + 10f, 8);
        shakeTimer = Math.max(shakeTimer, 0.1f);
    }

    private void launchSonicWave(
        float targetX,
        float targetY
    ) {
        sonicWaveActive = true;

        waveX = player.x + player.width / 2f;
        waveY = player.y + player.height * 0.55f;

        float dx = targetX - waveX;
        float dy = targetY - waveY;

        float length = (float) Math.sqrt(
            dx * dx + dy * dy
        );

        if (length < 1f) {
            dx = facingRight ? 1f : -1f;
            dy = 0f;
            length = 1f;
        }

        float speed = 1100f;

        waveVelocityX = dx / length * speed;
        waveVelocityY = dy / length * speed;

        waveDirection = (float) Math.toDegrees(
            Math.atan2(dy, dx)
        );

        waveTimer = 0f;
        waveRingTimer = 0f;

        voice("lee_r");

        spawnRing(waveX, waveY, 8f, 46f, 0.3f, 0.45f, 0.85f, 1f, 0.9f);
    }

    private void setMark(float x, float y, Monster m) {
        markPlatform = null;
        sonicWaveActive = false;
        markActive = true;
        markTimer = 3f;
        markX = x;
        markY = y;
        markMonster = m;

        sfx("lee_mark", 0.8f, 0f);

        spawnRing(x, y, 10f, 70f, 0.45f, 0.45f, 0.88f, 1f, 1f);
        spawnRing(x, y, 4f, 40f, 0.3f, 0.9f, 0.98f, 1f, 1f);
        spawnBurst(x, y, 10, 0.5f, 0.9f, 1f);
    }

    private void startLeeDash() {
        float tx = markX;
        float ty = markY;

        if (markMonster != null && markMonster.alive) {
            tx = markMonster.rect.x + markMonster.rect.width / 2f;
            ty = markMonster.rect.y + markMonster.rect.height / 2f;
        }

        dashMonster = markMonster;
        markActive = false;
        markMonster = null;
        leeAimMode = false;
        sonicWaveActive = false;
        leeUltTimer = 0f;

        dashFromX = player.x;
        dashFromY = player.y;
        dashToX = tx - PLAYER_W / 2f;
        dashToY = ty - PLAYER_H / 2f;

        // 절벽이나 발판의 꼭대기 근처(옆면 위쪽, 아랫면)에 맞혔다면 그 위로 올라섭니다.
        Rectangle grab = markPlatform;
        markPlatform = null;

        if (grab != null && platforms.contains(grab, true)) {
            float top = grab.y + grab.height;

            if (top - ty <= LEE_LEDGE_GRAB && ty <= top + 4f) {
                dashToX = Math.max(grab.x, Math.min(grab.x + grab.width - PLAYER_W, tx - PLAYER_W / 2f));
                dashToY = top;
            }
        }

        float dx = dashToX - dashFromX;
        float dy = dashToY - dashFromY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        dashDur = Math.max(0.12f, Math.min(0.5f, dist / 1700f));
        dashT = 0f;
        leeDashing = true;
        ghostEmitTimer = 0f;

        voice("lee_r2");

        velocityX = 0f;
        velocityY = 0f;
        zacLeapX = 0f;
        standingOn = null;

        facingRight = dx >= 0f;

        spawnRing(player.x + PLAYER_W / 2f, player.y + PLAYER_H / 2f,
            10f, 60f, 0.3f, 0.5f, 0.9f, 1f, 0.9f);
    }

    private void updateLeeDash(float delta) {
        dashT += delta;

        float p = Math.min(1f, dashT / dashDur);
        float e = 1f - (1f - p) * (1f - p) * (1f - p);

        player.x = dashFromX + (dashToX - dashFromX) * e;
        player.y = dashFromY + (dashToY - dashFromY) * e;

        ghostEmitTimer -= delta;

        if (ghostEmitTimer <= 0f) {
            ghostEmitTimer = 0.018f;
            ghosts.add(new float[] {player.x, player.y, 0.28f, 0.28f, facingRight ? 0f : 1f, 0f});
        }

        if (p >= 1f) {
            leeDashing = false;

            placeSafely(player.x, player.y);

            velocityY = 0f;
            onGround = false;

            float cx = player.x + PLAYER_W / 2f;
            float cy = player.y + PLAYER_H / 2f;

            spawnRing(cx, cy, 10f, 90f, 0.4f, 0.55f, 0.92f, 1f, 1f);
            spawnBurst(cx, cy, 12, 0.6f, 0.95f, 1f);
            shakeTimer = Math.max(shakeTimer, 0.15f);

            if (dashMonster != null && dashMonster.alive) {
                damageMonster(dashMonster);
            }

            dashMonster = null;
        }
    }

    // 지형과 겹치지 않는 가장 가까운 위치에 캐릭터를 놓습니다.
    private void placeSafely(float x, float y) {
        for (int r = 0; r <= 320; r += 6) {
            int steps = r == 0 ? 1 : 16;

            for (int k = 0; k < steps; k++) {
                double ang = Math.PI / 2.0 + k * Math.PI * 2.0 / steps;

                float cx = x + (float) Math.cos(ang) * r;
                float cy = y + (float) Math.sin(ang) * r;

                if (cx < 0f || cx > worldWidth - PLAYER_W) continue;
                if (cy < -100f) continue;

                tmpRect.set(cx, cy, PLAYER_W, PLAYER_H);

                if (!overlapsAnyPlatform(tmpRect)) {
                    player.set(cx, cy, PLAYER_W, PLAYER_H);
                    return;
                }
            }
        }

        player.set(x, y, PLAYER_W, PLAYER_H);
    }

    private boolean overlapsAnyPlatform(Rectangle r) {
        for (int i = 0; i < platforms.size; i++) {
            if (r.overlaps(platforms.get(i))) return true;
        }

        return false;
    }

    // 올라프 궁이 끝났을 때 지형 속에 있으면 위로 꺼내 줍니다.
    private void ejectFromTerrain() {
        for (int k = 0; k < 16; k++) {
            boolean moved = false;

            for (int i = 0; i < platforms.size; i++) {
                Rectangle p = platforms.get(i);

                if (player.overlaps(p)) {
                    player.y = p.y + p.height;
                    moved = true;
                }
            }

            if (!moved) break;
        }

        velocityY = Math.max(0f, velocityY);
    }

    private void updateSkills(float delta) {
        if (olafActive) {
            olafTimer -= delta;

            if (olafTimer <= 0f) {
                olafTimer = 0f;
                olafActive = false;
                ejectFromTerrain();
            }
        }

        if (zacActive) {
            zacTimer -= delta;

            if (zacTimer <= 0f) {
                zacTimer = 0f;
                zacActive = false;
            }
        }

        if (attackTimer > 0f) {
            attackTimer -= delta;
        }

        if (slashTimer > 0f) {
            slashTimer -= delta;
        }

        if (attackCooldown > 0f) {
            attackCooldown -= delta;
        }

        if (champion == 2 && leeUltTimer > 0f) {
            leeUltTimer -= delta;

            if (leeUltTimer <= 0f) {
                leeAimMode = false;
                leeUltTimer = 0f;
            }
        }

        if (markActive) {
            markTimer -= delta;

            if (markTimer <= 0f
                || (markMonster != null && !markMonster.alive)) {
                markActive = false;
                markMonster = null;
            }
        }
    }

    private void updateSonicWave(float delta) {
        if (!sonicWaveActive) return;

        waveTimer += delta;

        // 지나간 자리에 퍼지는 음파 고리
        waveRingTimer -= delta;

        if (waveRingTimer <= 0f) {
            waveRingTimer = 0.035f;
            spawnRing(waveX, waveY, 6f, 30f, 0.32f, 0.4f, 0.8f, 1f, 0.55f);
        }

        float nextX = waveX + waveVelocityX * delta;
        float nextY = waveY + waveVelocityY * delta;

        float dx = nextX - waveX;
        float dy = nextY - waveY;

        int steps = Math.max(
            1,
            (int) (
                Math.sqrt(dx * dx + dy * dy) / 3f
            )
        );

        float stepX = dx / steps;
        float stepY = dy / steps;

        for (int step = 0; step < steps; step++) {
            float testX = waveX + stepX;
            float testY = waveY + stepY;

            Rectangle waveBox = new Rectangle(
                testX - 5f,
                testY - 5f,
                10f,
                10f
            );

            // 땅·바위·움직이는 발판에 맞으면 그 자리에 표식이 남습니다.
            // 얇게 떠 있는 섬은 통과합니다. (계단 발판 너머 산꼭대기를 노릴 수 있게)
            for (int pi = 0; pi < platforms.size; pi++) {
                if (platformKinds.get(pi) == KIND_LEDGE) continue;

                Rectangle platform = platforms.get(pi);

                if (waveBox.overlaps(platform)) {
                    setMark(testX, testY, null);
                    markPlatform = platform;
                    return;
                }
            }

            Iterator<Minion> iterator = minions.iterator();

            while (iterator.hasNext()) {
                Minion m = iterator.next();

                if (waveBox.overlaps(m.rect)) {
                    cs += m.getCS();
                    iterator.remove();

                    setMark(
                        m.rect.x + m.rect.width / 2f,
                        m.rect.y + m.rect.height / 2f,
                        null
                    );
                    return;
                }
            }

            for (int i = 0; i < monsters.size; i++) {
                Monster m = monsters.get(i);

                if (!m.alive || m == rideHerald) continue;

                if (waveBox.overlaps(m.rect)) {
                    damageMonster(m);

                    if (m.alive && m != rideHerald) {
                        setMark(testX, testY, m);
                    } else {
                        setMark(testX, testY, null);
                    }
                    return;
                }
            }

            waveX = testX;
            waveY = testY;

            if (waveX < 0f
                || waveX > worldWidth
                || waveY < 0f
                || waveY > 1400f) {
                sonicWaveActive = false;
                return;
            }
        }

        if (waveTimer > 3f) {
            sonicWaveActive = false;
        }
    }

    // =====================================================================
    //  움직이는 발판
    // =====================================================================
    private void positionMover(Mover m) {
        float t = animTime / m.period + m.phase;
        float f = 0.5f - 0.5f * (float) Math.cos(t * Math.PI * 2.0);

        m.rect.x = m.baseX + m.rangeX * f;
        m.rect.y = m.baseTop - LEDGE_THICK + m.rangeY * f;
    }

    private void updateMovers() {
        for (Mover m : movers) {
            float prevX = m.rect.x;
            float prevTop = m.rect.y + m.rect.height;

            positionMover(m);

            float top = m.rect.y + m.rect.height;
            float dx = m.rect.x - prevX;

            if (launched || riding || leeDashing) continue;

            if (standingOn == m.rect) {
                // 올라탄 캐릭터를 함께 옮기고, 높이는 발판 윗면에 정확히 맞춥니다.
                // (소수점 오차로 발판에 살짝 파묻혀 옆으로 밀려나는 것을 막습니다)
                player.x += dx;
                player.y = top;

            } else if (player.overlaps(m.rect)
                && player.y >= prevTop - 8f) {
                // 올라오는 발판이 발밑을 받쳐 줍니다. 점프 중이어도 옆으로 밀지 않습니다.
                player.y = top;

                if (velocityY <= 0f) {
                    velocityY = 0f;
                    standingOn = m.rect;
                }
            }
        }
    }

    // =====================================================================
    //  바론 함정
    // =====================================================================
    private float trapWarnTime() {
        return round == 1 ? 0.95f : (round == 2 ? 0.8f : 0.65f);
    }

    private void updateTraps(float delta) {
        float warnTime = trapWarnTime();
        float idleMin = round == 1 ? 1.6f : (round == 2 ? 1.2f : 0.9f);
        float idleRange = round == 1 ? 1.4f : (round == 2 ? 1.2f : 1.0f);

        for (PillarTrap t : traps) {
            t.timer -= delta;

            if (t.state == 0) {
                if (t.timer <= 0f) {
                    for (int i = 0; i < 6; i++) t.chosen[i] = false;

                    int count;

                    if (round == 1) {
                        count = 2;
                    } else if (round == 2) {
                        count = 2 + random.nextInt(2);
                    } else {
                        count = 3 + random.nextInt(2);
                    }

                    int picked = 0;

                    while (picked < count) {
                        int c = random.nextInt(6);

                        if (!t.chosen[c]) {
                            t.chosen[c] = true;
                            picked++;
                        }
                    }

                    t.state = 1;
                    t.timer = warnTime;
                }

            } else if (t.state == 1) {
                // 경고 중 땅이 들썩입니다.
                if (random.nextFloat() < 0.35f) {
                    int c = random.nextInt(6);

                    if (t.chosen[c]) {
                        float px = t.cellX[c] + 6f + random.nextFloat() * (CELL - 12f);
                        spawnChunk(px, t.groundTop, (random.nextFloat() - 0.5f) * 60f,
                            120f + random.nextFloat() * 120f, 2.5f + random.nextFloat() * 2f,
                            0.35f, 0.3f, 0.25f);
                    }
                }

                if (t.timer <= 0f) {
                    t.state = 2;
                    t.timer = 0.16f;
                    t.height = 0f;

                    for (int i = 0; i < 6; i++) {
                        if (!t.chosen[i]) continue;

                        float cx = t.cellX[i] + CELL / 2f;

                        for (int k = 0; k < 6; k++) {
                            spawnChunk(cx + (random.nextFloat() - 0.5f) * CELL, t.groundTop,
                                (random.nextFloat() - 0.5f) * 260f, 260f + random.nextFloat() * 300f,
                                3f + random.nextFloat() * 4f, 0.32f, 0.3f, 0.3f);
                        }

                        spawnBurst(cx, t.groundTop + 6f, 5, 0.75f, 0.4f, 1f);
                    }

                    float dist = Math.abs(player.x - t.centerX);

                    if (dist < 650f) {
                        shakeTimer = Math.max(shakeTimer, 0.18f);
                    }

                    // 가까울수록 크게 들립니다.
                    if (dist < 900f) {
                        sfx("baron_arm", 1f - dist / 1000f, 0.1f);
                    }
                }

            } else if (t.state == 2) {
                float p = 1f - Math.max(0f, t.timer) / 0.16f;
                t.height = t.maxHeight * (1f - (1f - p) * (1f - p));

                if (t.timer <= 0f) {
                    t.height = t.maxHeight;
                    t.state = 3;
                    t.timer = 0.5f;
                }

            } else if (t.state == 3) {
                if (t.timer <= 0f) {
                    t.state = 4;
                    t.timer = 0.35f;
                }

            } else {
                t.height = t.maxHeight * Math.max(0f, t.timer) / 0.35f;

                if (t.timer <= 0f) {
                    t.height = 0f;
                    t.state = 0;
                    t.timer = idleMin + random.nextFloat() * idleRange;

                    for (int i = 0; i < 6; i++) t.chosen[i] = false;
                }
            }

            // 바론 팔에 맞으면 하늘로 날아갑니다. (올라프 궁 중에는 무시)
            boolean immune = launched || riding || leeDashing || olafActive;

            if (!immune && (t.state == 2 || t.state == 3)) {
                for (int i = 0; i < 6; i++) {
                    if (!t.chosen[i]) continue;

                    tmpRect.set(
                        t.cellX[i] + 5f,
                        t.groundTop,
                        CELL - 10f,
                        t.height
                    );

                    if (player.overlaps(tmpRect)) {
                        launchPlayer();
                        break;
                    }
                }
            }
        }
    }

    private void launchPlayer() {
        cancelValor(true);
        launched = true;
        launchTimer = 1.15f;
        launchSpin = 0f;

        velocityX = 0f;
        velocityY = 1650f;
        zacLeapX = 0f;
        zacAirborne = false;
        zacWindup = 0f;

        onGround = false;
        standingOn = null;
        shakeTimer = 0.35f;
        sfx("launch", 1f, 0f);

        setSquash(0.7f, 1.35f);
    }

    private void updateCheckpoints(float delta) {
        for (Checkpoint c : checkpoints) {
            if (c.flash > 0f) c.flash -= delta;

            if (c.active) continue;

            float centerX = player.x + player.width / 2f;

            boolean reached =
                onGround
                    && centerX >= c.x - 10f
                    && centerX < c.x + 600f
                    && Math.abs(player.y - c.y) < 4f;

            if (reached) {
                activateCheckpoint(c);
            }
        }
    }

    private void activateCheckpoint(Checkpoint c) {
        c.active = true;
        c.flash = 1f;
        sfx("checkpoint", 0.8f, 0f);
        respawnX = c.x - PLAYER_W / 2f;
        respawnY = c.y;
    }

    // =====================================================================
    //  용과 전령
    // =====================================================================
    private void updateMonsters(float delta) {
        for (int i = monsters.size - 1; i >= 0; i--) {
            Monster m = monsters.get(i);

            if (m.flash > 0f) m.flash -= delta;
            if (m.punch > 0f) m.punch = Math.max(0f, m.punch - delta * 6f);

            if (m.shownHp > m.hp) {
                m.shownHp = Math.max(m.hp, m.shownHp - delta * 2.5f);
            }

            if (!m.alive) {
                m.deathTimer -= delta;

                if (m.deathTimer <= 0f) {
                    monsters.removeIndex(i);
                }
                continue;
            }

            if (m == rideHerald) continue;

            if (m.kind == MON_HERALD) {
                // 제자리에서 천천히 서성입니다.
                m.rect.x += m.dir * 45f * delta;

                if (m.rect.x < m.minX) {
                    m.rect.x = m.minX;
                    m.dir = 1f;
                }

                if (m.rect.x > m.maxX) {
                    m.rect.x = m.maxX;
                    m.dir = -1f;
                }

                m.rect.y = m.baseY;
            } else {
                // 용은 공중에서 위아래로 떠다닙니다.
                m.rect.y = m.baseY
                    + (float) Math.sin(animTime * 2f + m.seed) * 7f;
            }
        }
    }

    // ----- 전령 탑승 -----
    private void startRide(Monster h) {
        cancelValor(true);
        riding = true;
        rideHerald = h;
        rideState = 0;
        rideTimer = 0f;

        mountFromX = player.x;
        mountFromY = player.y;

        velocityX = 0f;
        velocityY = 0f;
        zacLeapX = 0f;
        zacAirborne = false;
        zacWindup = 0f;
        leeAimMode = false;
        sonicWaveActive = false;
        markActive = false;
        standingOn = null;

        h.hp = 0;
        h.shownHp = 0f;

        rideSeg = groundSegmentAt(h.rect.x + h.rect.width / 2f);
        finalSeg = groundSegmentAt(nexus.x + nexus.width / 2f);
        rideStopX = nexus.x - 30f;

        if (rideSeg == null) rideSeg = finalSeg;

        hitStop = 0.1f;
        shakeTimer = 0.25f;
        sfx("herald_tame", 1f, 0f);

        spawnRing(h.rect.x + h.rect.width / 2f, h.rect.y + h.rect.height / 2f,
            20f, 150f, 0.5f, 0.75f, 0.55f, 1f, 0.9f);

        showToastKr("전령 탑승!", "넥서스까지 돌진합니다  ·  Q를 누르면 그 자리에서 내립니다",
            "RIFT HERALD TAMED", "CHARGING TO THE NEXUS!  (Q: GET OFF)", 0.85f, 0.7f, 1f);
    }

    private void updateRide(float delta) {
        Monster h = rideHerald;
        Rectangle hr = h.rect;

        rideTimer += delta;

        if (rideState == 0) {
            // 전령 등에 훌쩍 올라탑니다.
            float p = Math.min(1f, rideTimer / 0.4f);
            float sx = hr.x + hr.width / 2f - PLAYER_W / 2f - 6f;
            float sy = hr.y + hr.height - 26f;

            player.x = mountFromX + (sx - mountFromX) * p;
            player.y = mountFromY + (sy - mountFromY) * p
                + (float) Math.sin(p * Math.PI) * 70f;

            if (p >= 1f) {
                rideState = 1;
                rideTimer = 0f;
                setSquash(1.3f, 0.75f);
                spawnDust(hr.x + hr.width / 2f, hr.y, 10, 1f);
            }
            return;
        }

        if (rideState == 1) {
            float top = rideSeg.y + rideSeg.height;

            hr.x += HERALD_SPEED * delta;
            hr.y = top;

            // 지나가는 자리의 땅이 흩어집니다.
            rideDustTimer -= delta;

            if (rideDustTimer <= 0f) {
                rideDustTimer = 0.035f;

                float fx = hr.x + 24f + random.nextFloat() * 30f;

                spawnChunk(fx, top, -120f - random.nextFloat() * 180f,
                    140f + random.nextFloat() * 220f, 3f + random.nextFloat() * 4f,
                    0.36f, 0.28f, 0.2f);
                spawnDust(fx, top, 1, 0.7f);

                if (random.nextFloat() < 0.5f) {
                    scars.add(new float[] {fx - 14f, top, 26f + random.nextFloat() * 20f, 6f});
                }
            }

            boolean onFinal = rideSeg == finalSeg;

            if (onFinal && hr.x + hr.width >= rideStopX) {
                hr.x = rideStopX - hr.width;
                rideState = 3;
                rideTimer = 0f;

            } else if (!onFinal
                && hr.x + hr.width >= rideSeg.x + rideSeg.width - 70f) {
                planHeraldLeap();
            }

        } else if (rideState == 2) {
            leapT += delta;

            float t = Math.min(leapT, leapDur);

            hr.x = leapX0 + leapVX * t;
            hr.y = leapY0 + leapVY * t - 0.5f * GRAVITY * t * t;

            if (leapT >= leapDur) {
                rideSeg = leapTarget;
                hr.y = rideSeg.y + rideSeg.height;
                rideState = 1;

                // 착지 충격
                shakeTimer = Math.max(shakeTimer, 0.22f);
                sfx("herald_stomp", 0.9f, 0.08f);
                setSquash(1.25f, 0.8f);
                spawnDust(hr.x + hr.width / 2f, hr.y, 14, 1.3f);

                for (int k = 0; k < 8; k++) {
                    spawnChunk(hr.x + random.nextFloat() * hr.width, hr.y,
                        (random.nextFloat() - 0.5f) * 320f, 160f + random.nextFloat() * 260f,
                        3f + random.nextFloat() * 5f, 0.36f, 0.3f, 0.24f);
                }
            }

        } else if (rideState == 3) {
            // 넥서스 앞에서 내려 줍니다.
            float top = hr.y;

            player.x = hr.x + hr.width - PLAYER_W;
            player.y = top;
            velocityY = jumpSpeed() * 0.55f;
            onGround = false;
            facingRight = true;

            float cx = hr.x + hr.width / 2f;
            float cy = hr.y + hr.height / 2f;

            spawnBurst(cx, cy, 24, 0.8f, 0.6f, 1f);
            spawnRing(cx, cy, 20f, 170f, 0.55f, 0.8f, 0.6f, 1f, 0.9f);
            spawnDust(cx, top, 14, 1.2f);

            h.alive = false;
            h.deathTimer = 0.45f;

            riding = false;
            rideHerald = null;
            cameraSnap = false;
            return;
        }

        smashAround(hr);

        // 탑승 중 흔들림
        float wob = (float) Math.sin(animTime * 20f) * 2.2f;

        player.x = hr.x + hr.width / 2f - PLAYER_W / 2f - 6f;
        player.y = hr.y + hr.height - 26f + wob;
        facingRight = true;

        for (Checkpoint c : checkpoints) {
            if (!c.active && c.x < hr.x + hr.width / 2f) {
                activateCheckpoint(c);
            }
        }
    }

    // Q로 전령에서 내리기: 누른 순간의 자리에 내려 줍니다.
    // 공중에서 내리면 그대로 떨어지므로 구멍 위에서는 조심해야 합니다.
    private void dismountHerald() {
        Monster h = rideHerald;
        Rectangle hr = h.rect;

        float x = hr.x + hr.width / 2f - PLAYER_W / 2f;
        float y = hr.y;

        placeSafely(x, y);

        velocityX = 0f;
        velocityY = jumpSpeed() * 0.45f;
        onGround = false;
        standingOn = null;
        coyoteTimer = 0f;
        facingRight = true;
        hitCooldown = 0.5f;

        float cx = hr.x + hr.width / 2f;
        float cy = hr.y + hr.height / 2f;

        spawnBurst(cx, cy, 20, 0.8f, 0.6f, 1f);
        spawnRing(cx, cy, 20f, 140f, 0.5f, 0.8f, 0.6f, 1f, 0.9f);
        spawnDust(cx, hr.y, 10, 1f);
        setSquash(0.85f, 1.15f);

        h.alive = false;
        h.deathTimer = 0.45f;

        riding = false;
        rideHerald = null;

        showToastKr("전령에서 내렸습니다", "", "DISMOUNTED", "", 0.85f, 0.7f, 1f);
    }

    // 다음 땅까지 포물선으로 도약합니다. 중간 지형에 걸리지 않도록 높이를 계산합니다.
    private void planHeraldLeap() {
        Rectangle hr = rideHerald.rect;

        int idx = -1;

        for (int i = 0; i < groundSegments.size; i++) {
            if (groundSegments.get(i) == rideSeg) idx = i;
        }

        if (idx < 0 || idx + 1 >= groundSegments.size) {
            rideState = 3;
            rideTimer = 0f;
            return;
        }

        int ni = idx + 1;
        Rectangle next = groundSegments.get(ni);

        // 너무 좁은 계단 땅은 건너뛰고 그 다음 땅으로 바로 뜁니다.
        while (next != finalSeg
            && next.width < HERALD_W + 80f
            && ni + 1 < groundSegments.size
            && groundSegments.get(ni + 1).x - (next.x + next.width) < 1f) {
            ni++;
            next = groundSegments.get(ni);
        }

        float targetX = next.x + 30f;

        if (next == finalSeg) {
            targetX = Math.max(next.x + 10f, Math.min(targetX, rideStopX - hr.width - 10f));
        }

        float x0 = hr.x;
        float y0 = hr.y;
        float dh = next.y + next.height - y0;
        float dist = Math.max(40f, targetX - x0);

        float vy = 820f;
        float vx = 400f;
        float t = 1f;

        for (int k = 0; k < 40; k++) {
            float disc = vy * vy - 2f * GRAVITY * dh;

            if (disc < 0f) {
                vy += 90f;
                continue;
            }

            t = (vy + (float) Math.sqrt(disc)) / GRAVITY;
            vx = dist / t;

            if (vx > 760f) {
                t = dist / 760f;
                vx = 760f;
                vy = (dh + 0.5f * GRAVITY * t * t) / t;
            }

            if (isLeapClear(x0, y0, vx, vy, t)) break;

            vy += 90f;
        }

        leapTarget = next;
        leapT = 0f;
        leapDur = t;
        leapX0 = x0;
        leapY0 = y0;
        leapVX = vx;
        leapVY = vy;
        rideState = 2;

        setSquash(0.8f, 1.25f);
        spawnDust(hr.x + hr.width / 2f, y0, 12, 1.2f);
    }

    private boolean isLeapClear(float x0, float y0, float vx, float vy, float t) {
        for (float s = 0.02f; s < t - 0.02f; s += 0.02f) {
            float x = x0 + vx * s;
            float y = y0 + vy * s - 0.5f * GRAVITY * s * s;

            tmpRect2.set(x + 10f, y + 8f, HERALD_W - 20f, HERALD_H - 16f);

            for (int i = 0; i < groundSegments.size; i++) {
                if (tmpRect2.overlaps(groundSegments.get(i))) return false;
            }
        }

        return true;
    }

    // 전령이 부딪히는 발판, 미니언, 장식을 부숩니다.
    private void smashAround(Rectangle hr) {
        tmpRect2.set(hr.x + 6f, hr.y + 6f, hr.width - 12f, hr.height - 6f);

        for (int i = platforms.size - 1; i >= 0; i--) {
            if (platformKinds.get(i) == KIND_GROUND) continue;

            Rectangle p = platforms.get(i);

            if (p.overlaps(tmpRect2)) {
                destroyPlatform(i);
            }
        }

        Iterator<Minion> it = minions.iterator();

        while (it.hasNext()) {
            Minion m = it.next();

            if (m.rect.overlaps(tmpRect2)) {
                cs += m.getCS();
                spawnBurst(m.rect.x + m.rect.width / 2f, m.rect.y + m.rect.height / 2f,
                    10, 1f, 0.85f, 0.5f);
                it.remove();
            }
        }

        for (int i = decorations.size - 1; i >= 0; i--) {
            Deco d = decorations.get(i);

            if (d.x > hr.x && d.x < hr.x + hr.width && Math.abs(d.y - hr.y) < 24f) {
                decorations.removeIndex(i);

                float[] c = d.kind == 0 ? BUSH_LIGHT[(round - 1) % 3] : MOTE[(round - 1) % 3];

                for (int k = 0; k < 12; k++) {
                    spawnChunk(d.x + (random.nextFloat() - 0.5f) * 40f, d.y + 10f + random.nextFloat() * 20f,
                        (random.nextFloat() - 0.2f) * 300f, 120f + random.nextFloat() * 260f,
                        3f + random.nextFloat() * 3f, c[0], c[1], c[2]);
                }
            }
        }
    }

    private void destroyPlatform(int i) {
        Rectangle p = platforms.removeIndex(i);
        int kind = platformKinds.removeIndex(i);
        platformTextureIndices.removeIndex(i);

        if (kind == KIND_MOVER) {
            for (int k = movers.size - 1; k >= 0; k--) {
                if (movers.get(k).rect == p) movers.removeIndex(k);
            }
        }

        spawnSurfaces.removeValue(p, true);

        if (standingOn == p) standingOn = null;

        // 부서진 조각이 사방으로 튑니다.
        float[] st = STONE[(round - 1) % 3];
        float[] mo = MOSS_TOP[(round - 1) % 3];
        int pieces = (int) Math.min(40, p.width / 5f);

        for (int k = 0; k < pieces; k++) {
            boolean moss = random.nextFloat() < 0.3f;
            float[] c = moss ? mo : st;

            spawnChunk(
                p.x + random.nextFloat() * p.width,
                p.y + random.nextFloat() * p.height,
                (random.nextFloat() - 0.3f) * 520f,
                120f + random.nextFloat() * 420f,
                4f + random.nextFloat() * 8f,
                c[0] * (0.7f + random.nextFloat() * 0.4f),
                c[1] * (0.7f + random.nextFloat() * 0.4f),
                c[2] * (0.7f + random.nextFloat() * 0.4f)
            );
        }

        spawnDust(p.x + p.width / 2f, p.y + p.height / 2f, 10, 1.4f);
        shakeTimer = Math.max(shakeTimer, 0.2f);
        sfx("smash", 0.9f, 0.12f);
        hitStop = Math.max(hitStop, 0.035f);
    }

    // =====================================================================
    //  파티클
    // =====================================================================
    private Particle addParticle(
        int kind, float x, float y, float vx, float vy,
        float life, float size, float r, float g, float b, float a
    ) {
        if (particles.size >= MAX_PARTICLES) return null;

        Particle p = new Particle();
        p.kind = kind;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = life;
        p.maxLife = life;
        p.size = size;
        p.r = r;
        p.g = g;
        p.b = b;
        p.a = a;
        particles.add(p);
        return p;
    }

    private void spawnDust(float x, float y, int count, float power) {
        for (int i = 0; i < count; i++) {
            Particle p = addParticle(P_DOT,
                x + (random.nextFloat() - 0.5f) * 30f, y + 4f,
                (random.nextFloat() - 0.5f) * 220f * power,
                (20f + random.nextFloat() * 90f) * power,
                0.45f + random.nextFloat() * 0.35f,
                5f + random.nextFloat() * 7f,
                0.8f, 0.76f, 0.68f, 0.35f);

            if (p != null) {
                p.drag = 3f;
                p.grow = 14f;
            }
        }
    }

    private void spawnBurst(float x, float y, int count, float r, float g, float b) {
        for (int i = 0; i < count; i++) {
            float ang = random.nextFloat() * (float) Math.PI * 2f;
            float sp = 120f + random.nextFloat() * 320f;

            Particle p = addParticle(P_DOT, x, y,
                (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp,
                0.25f + random.nextFloat() * 0.25f,
                2f + random.nextFloat() * 2.5f, r, g, b, 1f);

            if (p != null) p.drag = 4f;
        }
    }

    private void spawnChunk(float x, float y, float vx, float vy, float size,
                            float r, float g, float b) {
        Particle p = addParticle(P_CHUNK, x, y, vx, vy,
            0.8f + random.nextFloat() * 0.6f, size, r, g, b, 1f);

        if (p != null) {
            p.gravity = 1300f;
            p.rot = random.nextFloat() * 360f;
            p.vrot = (random.nextFloat() - 0.5f) * 900f;
        }
    }

    private void spawnRing(float x, float y, float size, float grow, float life,
                           float r, float g, float b, float a) {
        Particle p = addParticle(P_RING, x, y, 0f, 0f, life, size, r, g, b, a);

        if (p != null) p.grow = grow;
    }

    // 자크 젤리 방울
    private void spawnJelly(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            float ang = (float) Math.PI * (0.1f + random.nextFloat() * 0.8f);
            float sp = 180f + random.nextFloat() * 260f;

            Particle p = addParticle(P_DOT, x, y,
                (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp,
                0.6f + random.nextFloat() * 0.4f,
                4f + random.nextFloat() * 5f,
                0.35f, 0.85f, 0.35f, 0.85f);

            if (p != null) p.gravity = 1100f;
        }
    }

    private void updateParticles(float delta) {
        float pcx = player.x + player.width / 2f;
        float pcy = player.y + player.height / 2f;

        for (int i = particles.size - 1; i >= 0; i--) {
            Particle p = particles.get(i);

            p.life -= delta;

            if (p.life <= 0f) {
                particles.removeIndex(i);
                continue;
            }

            if (p.kind == P_ORB) {
                // 처음엔 퍼졌다가 챔피언에게 빨려 들어갑니다.
                float age = p.maxLife - p.life;
                float dx = pcx - p.x;
                float dy = pcy - p.y;
                float d = (float) Math.sqrt(dx * dx + dy * dy);

                if (age > 0.35f && d > 1f) {
                    float acc = 2600f + age * 2000f;
                    p.vx += dx / d * acc * delta;
                    p.vy += dy / d * acc * delta;
                }

                float damp = Math.max(0f, 1f - p.drag * delta);
                p.vx *= damp;
                p.vy *= damp;

                if (age > 0.35f && d < 22f) {
                    spawnBurst(p.x, p.y, 3, p.r + 0.3f, p.g + 0.3f, p.b + 0.3f);
                    sfx("orb", 0.35f, 0.2f);
                    particles.removeIndex(i);
                    setSquash(1.1f, 0.92f);
                    continue;
                }
            } else {
                p.vy -= p.gravity * delta;

                if (p.drag > 0f) {
                    float damp = Math.max(0f, 1f - p.drag * delta);
                    p.vx *= damp;
                    p.vy *= damp;
                }
            }

            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.rot += p.vrot * delta;
        }

        for (int i = ghosts.size - 1; i >= 0; i--) {
            float[] g = ghosts.get(i);
            g[2] -= delta;

            if (g[2] <= 0f) ghosts.removeIndex(i);
        }

        for (int i = scars.size - 1; i >= 0; i--) {
            float[] s = scars.get(i);
            s[3] -= delta;

            if (s[3] <= 0f) scars.removeIndex(i);
        }
    }

    // =====================================================================
    //  찌부 / 늘어남 (스프링)
    // =====================================================================
    private void setSquash(float sx, float sy) {
        squashX = sx;
        squashY = sy;
        squashVX = 0f;
        squashVY = 0f;
    }

    private void updateSquash(float delta) {
        float tx = 1f;
        float ty = 1f;

        if (zacWindup > 0f) {
            tx = 1.42f;
            ty = 0.58f;
        }

        float k = 260f;
        float damp = 15f;

        squashVX += ((tx - squashX) * k - squashVX * damp) * delta;
        squashVY += ((ty - squashY) * k - squashVY * damp) * delta;

        squashX += squashVX * delta;
        squashY += squashVY * delta;
    }

    // =====================================================================
    //  게임 업데이트
    // =====================================================================
    private void updateGame(float delta) {
        animTime += delta;

        if (hitCooldown > 0f) hitCooldown -= delta;
        if (landingTimer > 0f) landingTimer -= delta;
        if (fadeTimer > 0f) fadeTimer -= delta;
        if (bannerTimer > 0f) bannerTimer -= delta;
        if (shakeTimer > 0f) shakeTimer -= delta;
        if (toastTimer > 0f) toastTimer -= delta;
        if (jumpBufferTimer > 0f) jumpBufferTimer -= delta;
        if (attackBufferTimer > 0f) attackBufferTimer -= delta;

        updateSquash(delta);
        updateSkills(delta);
        updateMovers();
        updateTraps(delta);
        updateMinions(delta);
        updateMonsters(delta);
        updateSonicWave(delta);
        updateArrows(delta);
        updateAxe(delta);
        updateValor(delta);
        updateParticles(delta);

        if (riding) {
            updateRide(delta);
            return;
        }

        // 바론 팔에 맞아 하늘로 날아가는 중
        if (launched) {
            launchTimer -= delta;
            launchSpin += 900f * delta;

            player.y += velocityY * delta;
            velocityY -= GRAVITY * 0.3f * delta;

            if (launchTimer <= 0f) {
                launched = false;

                if (PILLAR_SENDS_TO_ROUND_START) {
                    respawnAt(roundStartX, roundStartY);
                } else {
                    respawnAt(respawnX, respawnY);
                }
            }
            return;
        }

        if (leeDashing) {
            updateLeeDash(delta);
            return;
        }

        if (zacWindup > 0f) {
            zacWindup -= delta;

            if (zacWindup > 0f) return;

            releaseZacLeap();
        }

        float oldX = player.x;
        float oldY = player.y;
        boolean wasOnGround = onGround;

        player.x += velocityX * delta;
        player.x += zacLeapX * delta;

        zacLeapX *= Math.max(
            0f,
            1f - 2f * delta
        );

        player.x = Math.max(
            0f,
            Math.min(
                worldWidth - player.width,
                player.x
            )
        );

        resolveHorizontalCollision(oldX);

        // 발러를 타고 나는 중에는 중력이 없습니다.
        if (!isFlying()) {
            velocityY -= GRAVITY * delta;
        }

        player.y += velocityY * delta;

        if (isFlying()) {
            float ceiling = CAMERA_MAX_Y + H / 2f - PLAYER_H - 40f;

            if (player.y > ceiling) {
                player.y = ceiling;
                velocityY = Math.min(velocityY, 0f);
            }

            if (player.y < 30f) {
                player.y = 30f;
                velocityY = Math.max(velocityY, 0f);
            }
        }

        float fallSpeed = velocityY;

        resolveVerticalCollision(oldY);

        if (onGround) {
            coyoteTimer = COYOTE_TIME;
        } else {
            coyoteTimer -= delta;
        }

        if (zacAirborne) {
            zacAirTime += delta;
            zacSpin += (facingRight ? -1f : 1f) * 760f * delta;
        }

        if (onGround && !wasOnGround) {
            onLanded(fallSpeed);
        }

        emitTrails(delta);

        updateCheckpoints(delta);
        checkMinionCollisions();

        if (player.y < -150f) {
            respawnPlayer();
        }

        if (player.overlaps(nexus)) {
            if (round < 3) {
                startRound(round + 1);
            } else {
                cleared = true;
                if (victoryMusic != null) {
                    playMusic(victoryMusic);
                }
            }
        }
    }

    private void onLanded(float fallSpeed) {
        float cx = player.x + player.width / 2f;

        if (zacAirborne) {
            // 자크: 땅에 철퍼덕 찌부됩니다.
            zacAirborne = false;
            zacSpin = 0f;

            setSquash(1.62f, 0.48f);
            sfx("zac_land", 1f, 0.05f);
            spawnJelly(cx, player.y + 6f, 16);
            spawnDust(cx, player.y, 16, 1.4f);
            spawnRing(cx, player.y + 6f, 14f, 140f, 0.4f, 0.5f, 0.95f, 0.5f, 0.8f);
            shakeTimer = Math.max(shakeTimer, 0.25f);

            // 착지 충격으로 주변 미니언 처치, 용/전령 피해
            Iterator<Minion> it = minions.iterator();

            while (it.hasNext()) {
                Minion m = it.next();
                float mx = m.rect.x + m.rect.width / 2f;

                if (Math.abs(mx - cx) < 140f && Math.abs(m.rect.y - player.y) < 90f) {
                    cs += m.getCS();
                    spawnBurst(mx, m.rect.y + m.rect.height / 2f, 8, 0.5f, 1f, 0.5f);
                    it.remove();
                }
            }

            tmpRect.set(cx - 140f, player.y - 20f, 280f, 140f);

            for (int i = 0; i < monsters.size; i++) {
                Monster m = monsters.get(i);

                if (m.alive && m != rideHerald && tmpRect.overlaps(m.rect)) {
                    damageMonster(m);
                }
            }
            return;
        }

        float amount = Math.max(0.06f, Math.min(0.3f, -fallSpeed / 1500f));
        setSquash(1f + amount, 1f - amount);

        if (fallSpeed < -650f) {
            spawnDust(cx, player.y, 6, 0.7f);
            sfx("land", 0.5f, 0.1f);
        }
    }

    // 올라프 잔상, 바람용 버프 바람 자국
    private void emitTrails(float delta) {
        trailTimer -= delta;

        if (trailTimer > 0f) return;

        trailTimer = 0.06f;

        if (olafActive && (velocityX != 0f || !onGround)) {
            ghosts.add(new float[] {player.x, player.y, 0.3f, 0.3f, facingRight ? 0f : 1f, 1f});
        }

        if (windBuff && velocityX != 0f) {
            Particle p = addParticle(P_DOT,
                player.x + player.width / 2f - (facingRight ? 20f : -20f),
                player.y + 10f + random.nextFloat() * 46f,
                facingRight ? -160f : 160f, 0f,
                0.35f, 2f + random.nextFloat() * 2f,
                0.85f, 0.88f, 0.92f, 0.6f);

            if (p != null) p.drag = 2f;
        }

        if (olafActive) {
            Particle p = addParticle(P_DOT,
                player.x + random.nextFloat() * player.width,
                player.y + random.nextFloat() * player.height * 0.8f,
                (random.nextFloat() - 0.5f) * 30f, 60f + random.nextFloat() * 60f,
                0.5f, 2.5f + random.nextFloat() * 2.5f,
                1f, 0.45f + random.nextFloat() * 0.3f, 0.1f, 0.85f);

            if (p != null) p.drag = 1f;
        }
    }

    private void resolveHorizontalCollision(float oldX) {
        // 올라프 궁: 벽과 지형을 그대로 통과합니다.
        if (olafActive) return;

        float moved = player.x - oldX;

        for (Rectangle p : platforms) {
            if (!player.overlaps(p)) continue;

            // 발이 윗면에 거의 붙어 있으면 벽이 아니라 바닥입니다. (옆으로 밀지 않음)
            if (player.y >= p.y + p.height - 3f) continue;

            if (moved > 0f) {
                player.x = p.x - player.width;
            } else if (moved < 0f) {
                player.x = p.x + p.width;
            } else {
                float pushLeft = player.x + player.width - p.x;
                float pushRight = p.x + p.width - player.x;

                if (pushLeft < pushRight) {
                    player.x = p.x - player.width;
                } else {
                    player.x = p.x + p.width;
                }
            }

            velocityX = 0f;
            zacLeapX = 0f;
        }
    }

    private void resolveVerticalCollision(float oldY) {
        onGround = false;
        standingOn = null;

        for (Rectangle p : platforms) {
            boolean horizontalOverlap =
                player.x + player.width > p.x
                    && player.x < p.x + p.width;

            if (!horizontalOverlap) continue;

            float platformTop = p.y + p.height;

            float oldBottom = oldY;
            float newBottom = player.y;

            float oldTop = oldY + player.height;
            float newTop = player.y + player.height;

            boolean landing =
                velocityY <= 0f
                    && oldBottom >= platformTop - 3f
                    && newBottom <= platformTop;

            // 올라프 궁 중에는 머리를 박지 않고 그대로 뚫고 올라갑니다.
            boolean hittingBottom =
                !olafActive
                    && velocityY > 0f
                    && oldTop <= p.y + 2f
                    && newTop >= p.y;

            if (landing) {
                player.y = platformTop;
                velocityY = 0f;
                onGround = true;
                standingOn = p;
                landingTimer = 0.15f;

            } else if (hittingBottom) {
                player.y = p.y - player.height;
                velocityY = 0f;
            }
        }

        // 올라프 궁: 지형 속을 지나가는 동안에도 아래로 빠지지 않습니다.
        // (구멍에 빠져 땅 깊숙이 들어간 경우는 받쳐 주지 않습니다 → 땅속을 걷지 않음)
        // (지형 속에 있는 동안에도 점프할 수 있습니다)
        boolean phasing = false;

        if (olafActive && !onGround && velocityY <= 0f) {
            boolean deepInside = false;

            for (int i = 0; i < platforms.size; i++) {
                Rectangle p = platforms.get(i);

                if (!player.overlaps(p)) continue;

                float top = p.y + p.height;
                float depth = top - player.y;
                float snap = platformKinds.get(i) == KIND_GROUND ? 30f : 44f;

                if (depth <= snap) {
                    player.y = top;
                    standingOn = p;
                    velocityY = 0f;
                    onGround = true;
                    break;
                }

                if (depth <= 115f) {
                    // 낮은 벽 속에 들어가면 막히지 않고 스르륵 위로 빠져나옵니다.
                    float rise = 480f * Gdx.graphics.getDeltaTime();
                    player.y = Math.min(top, Math.max(player.y, oldY) + rise);
                    velocityY = 0f;
                    onGround = true;
                    phasing = true;
                    break;
                }

                deepInside = true;
            }

            // 높은 산 속: 걸어 들어간 높이를 그대로 유지하며 산을 뚫고 지나갑니다.
            // (구멍에 떨어지다 벽 속으로 들어간 경우는 받쳐 주지 않습니다)
            if (!onGround && deepInside && player.y >= olafFloor - 24f) {
                player.y = Math.max(player.y, olafFloor);
                velocityY = 0f;
                onGround = true;
                phasing = true;
            }
        }

        if (onGround && !phasing) {
            olafFloor = player.y;
        }
    }

    private void checkMinionCollisions() {
        Iterator<Minion> iterator = minions.iterator();

        while (iterator.hasNext()) {
            Minion m = iterator.next();

            if (!player.overlaps(m.rect)) continue;

            boolean stomping =
                velocityY < 0f
                    && player.y >= m.rect.y + m.rect.height - 12f;

            if (stomping) {
                cs += m.getCS();
                velocityY = JUMP_SPEED * 0.45f;
                onGround = false;
                standingOn = null;
                spawnBurst(m.rect.x + m.rect.width / 2f, m.rect.y + m.rect.height, 8, 1f, 0.9f, 0.5f);
                iterator.remove();

            } else if (hitCooldown <= 0f && !olafActive) {
                // 올라프 궁 중에는 맞아도 피격 횟수가 늘지 않습니다.
                playerHits++;
                hitCooldown = 0.65f;
                sfx("hurt", 0.7f, 0.05f);
                shakeTimer = Math.max(shakeTimer, 0.12f);

                if (playerHits >= maxHits()) {
                    respawnPlayer();
                    return;
                }
            }
        }
    }

    private void respawnPlayer() {
        respawnAt(respawnX, respawnY);
    }

    private void respawnAt(float x, float y) {
        player.set(x, y, PLAYER_W, PLAYER_H);

        velocityX = 0f;
        velocityY = 0f;
        zacLeapX = 0f;
        onGround = false;
        standingOn = null;
        coyoteTimer = 0f;

        playerHits = 0;
        hitCooldown = 0.65f;

        olafActive = false;
        olafTimer = 0f;

        zacActive = false;
        zacTimer = 0f;
        zacWindup = 0f;
        zacAirborne = false;

        sonicWaveActive = false;
        leeAimMode = false;
        leeDashing = false;
        markActive = false;
        markMonster = null;

        launched = false;
        cancelValor(false);
        arrows.clear();
        axeState = AXE_NONE;
        olafFloor = y;
        setSquash(1f, 1f);
        fadeTimer = 0.45f;
        cameraSnap = true;
    }

    private void updateCamera(float delta) {
        float targetX =
            player.x + player.width / 2f
                + (facingRight ? 70f : -70f);

        float targetY = player.y + player.height / 2f + 40f;

        targetX = Math.max(
            W / 2f,
            Math.min(worldWidth - W / 2f, targetX)
        );

        targetY = Math.max(
            H / 2f,
            Math.min(CAMERA_MAX_Y, targetY)
        );

        if (cameraSnap) {
            camX = targetX;
            camY = targetY;
            cameraSnap = false;
        } else if (delta > 0f) {
            float ax = 1f - (float) Math.exp(-7f * delta);
            float ay = 1f - (float) Math.exp(-4.5f * delta);

            camX += (targetX - camX) * ax;
            camY += (targetY - camY) * ay;
        }

        float sx = 0f;
        float sy = 0f;

        if (shakeTimer > 0f && delta > 0f) {
            float s = Math.min(1f, shakeTimer / 0.35f) * 7f;
            sx = (random.nextFloat() * 2f - 1f) * s;
            sy = (random.nextFloat() * 2f - 1f) * s;
        }

        camera.position.set(camX + sx, camY + sy, 0);
        camera.update();
    }

    // =====================================================================
    //  렌더링
    // =====================================================================

    private void beginShapes() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(
            GL20.GL_SRC_ALPHA,
            GL20.GL_ONE_MINUS_SRC_ALPHA
        );

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
    }

    private float hash(float n) {
        float s = (float) Math.sin(n * 12.9898f) * 43758.5453f;
        return s - (float) Math.floor(s);
    }

    private float viewLeft() {
        return camera.position.x - W / 2f;
    }

    private float viewBottom() {
        return camera.position.y - H / 2f;
    }

    private int ri() {
        return (round - 1) % 3;
    }

    private void gradientRect(
        float x,
        float y,
        float w,
        float h,
        Color bottom,
        Color top
    ) {
        shapes.rect(x, y, w, h, bottom, bottom, top, top);
    }

    // 굵은 선으로 그린 원 / 호
    private void drawArc(float cx, float cy, float r, float a0, float a1,
                         float width, int segs) {
        float step = (a1 - a0) / segs;
        float px = cx + (float) Math.cos(a0) * r;
        float py = cy + (float) Math.sin(a0) * r;

        for (int i = 1; i <= segs; i++) {
            float a = a0 + step * i;
            float nx = cx + (float) Math.cos(a) * r;
            float ny = cy + (float) Math.sin(a) * r;

            shapes.rectLine(px, py, nx, ny, width);
            px = nx;
            py = ny;
        }
    }

    private void renderGame() {
        // 1. 배경
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawBackground();
        batch.end();

        // 2. 하늘빛, 빛줄기, 안개, 낙하 구간, 돌 지형, 장식
        beginShapes();
        drawAtmosphere();
        drawLightShafts();
        drawMotes(false);
        drawMist();
        drawPits();
        drawTerrainBodies();
        drawDecorations();
        shapes.end();

        // 3. (선택) 지형 이미지
        if (USE_TERRAIN_TEXTURES) {
            batch.begin();
            drawTerrainTextures();
            batch.end();
        }

        // 4. 이끼 테두리, 땅 자국, 함정 바닥, 넥서스·깃발 빛, 그림자, 표식
        beginShapes();
        drawTerrainEdges();
        drawScars();
        drawTrapFloors();
        drawNexusGlow();
        drawCheckpointShapes();
        drawShadows();
        drawMonsterFallbacks();
        drawPlayerAuras();
        shapes.end();

        // 5. 바론, 넥서스, 깃발, 용, 전령, 미니언, 잔상, 챔피언
        batch.begin();
        drawBarons();
        drawNexusAndFlags();
        drawMonsters(false);

        for (Minion m : minions) {
            Texture minionTexture =
                minionTextures[m.type];

            if (minionTexture != null) {
                batch.draw(
                    minionTexture,
                    m.rect.x,
                    m.rect.y,
                    m.rect.width,
                    m.rect.height
                );
            }
        }

        drawGhosts();
        drawValor(true);
        drawPlayer();
        drawValor(false);

        // 탑승 중에는 전령을 챔피언 앞에 그려 등에 탄 것처럼 보이게 합니다.
        drawMonsters(true);
        batch.end();

        // 6. 바론 팔, 파티클, 스킬 효과, 체력바, 앞쪽 실루엣
        beginShapes();

        for (Minion m : minions) {
            if (minionTextures[m.type] == null) {
                if (m.type == 0) {
                    shapes.setColor(Color.GREEN);
                } else if (m.type == 1) {
                    shapes.setColor(Color.CYAN);
                } else {
                    shapes.setColor(Color.ORANGE);
                }

                shapes.rect(
                    m.rect.x,
                    m.rect.y,
                    m.rect.width,
                    m.rect.height
                );
            }
        }

        drawBaronArms();
        drawParticles();
        drawSlash();
        drawLeeEffects();
        drawQuinnEffects();
        drawOlafAxe();
        drawHealthBars();
        drawMotes(true);
        drawForeground();

        if (fadeTimer > 0f) {
            shapes.setColor(0f, 0f, 0f, Math.min(1f, fadeTimer / 0.45f));
            shapes.rect(viewLeft() - 20f, viewBottom() - 20f, W + 40f, H + 40f);
        }

        shapes.end();

        if (debugDraw) {
            drawDebug();
        }

        renderHUD();
    }

    private void drawBackground() {
        Texture bg = backgrounds[round - 1];

        if (bg == null) return;

        float bw = W * 1.35f;
        float bh = H * 1.2f;

        float px = worldWidth > W
            ? (camera.position.x - W / 2f) / (worldWidth - W)
            : 0f;

        float py =
            (camera.position.y - H / 2f) / (CAMERA_MAX_Y - H / 2f);

        px = Math.max(0f, Math.min(1f, px));
        py = Math.max(0f, Math.min(1f, py));

        float x = viewLeft() - (bw - W) * px;
        float y = viewBottom() - (bh - H) * py;

        batch.setColor(0.86f, 0.88f, 0.92f, 1f);
        batch.draw(bg, x, y, bw, bh);
        batch.setColor(Color.WHITE);
    }

    private void drawAtmosphere() {
        float l = viewLeft();
        float b = viewBottom();

        // 배경 이미지가 없을 때 대체 하늘 (위: 파랑, 아래: 옅은 하늘색)
        if (backgrounds[round - 1] == null) {
            float[] g = GRADE[ri()];
            tmpA.set(0.55f + g[0] * 0.2f, 0.70f + g[1] * 0.15f, 0.88f, 1f);
            tmpB.set(0.16f, 0.36f + g[1] * 0.1f, 0.70f, 1f);
            gradientRect(l, b, W, H, tmpA, tmpB);
        }

        float[] g = GRADE[ri()];

        shapes.setColor(g[0], g[1], g[2], 0.06f);
        shapes.rect(l, b, W, H);

        tmpA.set(0f, 0f, 0f, 0f);
        tmpB.set(0f, 0.02f, 0.06f, 0.30f);
        gradientRect(l, b + H - 120f, W, 120f, tmpA, tmpB);
    }

    // 하늘에서 비스듬히 내려오는 빛줄기
    private void drawLightShafts() {
        float l = viewLeft();
        float b = viewBottom();
        float[] c = SHAFT[ri()];
        float strength = round == 3 ? 0.5f : 1f;

        for (int i = 0; i < 4; i++) {
            float span = W + 400f;
            float sx = (i * 290f + 80f - camera.position.x * 0.12f) % span;

            if (sx < 0f) sx += span;

            sx = l + sx - 200f;

            float topY = b + H;
            float botY = b + H * 0.12f;
            float wTop = 50f + 40f * hash(i * 3.7f);
            float wBot = wTop * 2.6f;
            float slant = -200f;

            float a = (0.05f + 0.03f * (float) Math.sin(animTime * 0.6f + i * 1.7f)) * strength;

            tmpA.set(c[0], c[1], c[2], a);
            tmpB.set(c[0], c[1], c[2], 0f);

            shapes.triangle(
                sx, topY, sx + wTop, topY, sx + slant, botY,
                tmpA, tmpA, tmpB
            );
            shapes.triangle(
                sx + wTop, topY, sx + slant + wBot, botY, sx + slant, botY,
                tmpA, tmpB, tmpB
            );
        }
    }

    // 낮게 깔리는 안개
    private void drawMist() {
        float l = viewLeft();
        float b = viewBottom();

        for (int i = 0; i < 6; i++) {
            float span = W + 600f;
            float x = (hash(i * 4.1f) * 3000f + animTime * (8f + i * 3f) - camera.position.x * 0.35f) % span;

            if (x < 0f) x += span;

            x = l + x - 300f;

            float y = b + 30f + hash(i * 2.3f) * 120f - (camera.position.y - H / 2f) * 0.1f;

            shapes.setColor(0.85f, 0.92f, 1f, 0.045f);
            shapes.ellipse(x, y, 420f, 70f);
        }
    }

    private void drawMotes(boolean front) {
        float l = viewLeft();
        float b = viewBottom();
        float spanX = W + 80f;
        float spanY = H + 60f;

        float[] c = MOTE[ri()];

        for (int i = 0; i < 48; i++) {
            float depth = 0.3f + 0.7f * hash(i * 3.1f + 1f);

            if (front != (depth > 0.75f)) continue;

            float drift = 6f + 16f * hash(i * 5.7f + 2f);
            float rise = 5f + 12f * hash(i * 1.9f + 3f);

            float sx = hash(i * 7.3f + 4f) * 5000f
                - camera.position.x * depth
                + animTime * drift;

            float sy = hash(i * 2.2f + 5f) * 3000f
                - camera.position.y * depth * 0.6f
                + animTime * rise;

            sx = ((sx % spanX) + spanX) % spanX - 40f;
            sy = ((sy % spanY) + spanY) % spanY - 30f;

            float twinkle =
                0.5f + 0.5f * (float) Math.sin(animTime * 2.2f + i * 1.7f);

            float alpha = (0.15f + 0.45f * depth) * twinkle;
            float size = 1.1f + 2.2f * depth;

            shapes.setColor(c[0], c[1], c[2], alpha * 0.35f);
            shapes.circle(l + sx, b + sy, size * 2.6f, 10);

            shapes.setColor(c[0], c[1], c[2], alpha);
            shapes.circle(l + sx, b + sy, size, 8);
        }
    }

    private void drawPits() {
        float l = viewLeft();
        float r = l + W;
        float b = viewBottom();

        float[] c = MOTE[ri()];
        float[] ab = ABYSS[ri()];

        for (float[] pit : pits) {
            float x0 = pit[0];
            float x1 = pit[1];

            if (x1 < l - 20f || x0 > r + 20f) continue;

            float w = x1 - x0;
            float topY = Math.min(pit[2], pit[3]) - 10f;
            float bottomY = Math.max(GROUND_BOTTOM, b - 10f);

            if (topY <= bottomY) continue;

            tmpA.set(ab[0], ab[1], ab[2], 0.97f);
            tmpB.set(ab[0], ab[1], ab[2], 0f);
            gradientRect(x0, bottomY, w, topY - bottomY, tmpA, tmpB);

            shapes.setColor(c[0], c[1], c[2], 0.06f);
            shapes.ellipse(x0 + w * 0.1f, b - 30f, w * 0.8f, 70f);

            int count = Math.max(3, (int) (w / 45f));

            for (int i = 0; i < count; i++) {
                float seed = x0 * 0.013f + i * 3.7f;
                float px = x0 + 10f + hash(seed) * (w - 20f);
                float cycle = (animTime * (0.25f + 0.2f * hash(seed + 1f))
                    + hash(seed + 2f)) % 1f;
                float py = b + cycle * Math.max(60f, topY - b);
                float a = 0.55f * (1f - cycle);

                px += (float) Math.sin(animTime * 1.5f + i) * 5f;

                shapes.setColor(c[0], c[1], c[2], a * 0.3f);
                shapes.circle(px, py, 5f, 8);
                shapes.setColor(c[0], c[1], c[2], a);
                shapes.circle(px, py, 1.8f, 6);
            }
        }
    }

    // 돌벽돌로 쌓은 지형 (텍스처가 없어도 항상 보입니다)
    private void drawStoneBody(Rectangle p, float y0, float top) {
        float l = viewLeft() - 10f;
        float r = l + W + 20f;
        float[] st = STONE[ri()];

        // 벽돌 사이 틈 (어두운 바탕)
        tmpA.set(st[0] * 0.14f, st[1] * 0.14f, st[2] * 0.16f, 1f);
        tmpB.set(st[0] * 0.30f, st[1] * 0.30f, st[2] * 0.32f, 1f);
        gradientRect(p.x, y0, p.width, top - y0, tmpA, tmpB);

        float rowH = 24f;
        int row = 0;

        for (float ry = top - 9f; ry > y0 + 2f; ry -= rowH, row++) {
            float rb = Math.max(y0, ry - rowH);
            float depth = Math.max(0.32f, 1f - row * 0.08f);

            float off = (row % 2) * 23f + hash(row * 7.1f + p.x * 0.013f) * 12f;
            float bx = p.x - off;
            int k = 0;

            while (bx < p.x + p.width && k < 400) {
                float bw = 38f + hash(row * 13.7f + k * 3.3f + p.x * 0.007f) * 34f;
                float sx = Math.max(bx, p.x);
                float ex = Math.min(bx + bw, p.x + p.width);

                if (ex > l && sx < r && ex - sx > 4f && ry - rb > 4f) {
                    float tone = 0.80f + 0.32f * hash(row * 5.3f + k * 9.1f + p.x * 0.011f);
                    float c = tone * depth;

                    shapes.setColor(st[0] * c, st[1] * c, st[2] * c, 1f);
                    shapes.rect(sx + 1.5f, rb + 1.5f, ex - sx - 3f, ry - rb - 3f);

                    // 윗면 밝은 모서리, 아랫면 그림자
                    shapes.setColor(
                        Math.min(1f, st[0] * c * 1.3f),
                        Math.min(1f, st[1] * c * 1.3f),
                        Math.min(1f, st[2] * c * 1.3f), 0.9f);
                    shapes.rect(sx + 1.5f, ry - 3.5f, ex - sx - 3f, 2f);

                    shapes.setColor(0f, 0f, 0f, 0.18f);
                    shapes.rect(sx + 1.5f, rb + 1.5f, ex - sx - 3f, 2.5f);

                    // 위쪽 줄 벽돌에는 이끼 얼룩
                    if (row < 3 && round != 3
                        && hash(row * 2.9f + k * 6.1f + p.x * 0.003f) > 0.72f) {
                        float[] mo = MOSS[ri()];
                        shapes.setColor(mo[0], mo[1], mo[2], 0.55f);
                        shapes.ellipse(sx + 4f, ry - 11f, Math.min(26f, ex - sx - 6f), 9f);
                    }
                }

                bx += bw;
                k++;
            }
        }

        shapes.setColor(0f, 0f, 0f, 0.3f);
        shapes.rect(p.x, y0, 4f, top - y0);
        shapes.rect(p.x + p.width - 4f, y0, 4f, top - y0);
    }

    private void drawTerrainBodies() {
        float l = viewLeft() - 20f;
        float r = l + W + 40f;
        float b = viewBottom() - 10f;

        float[] st = STONE[ri()];

        for (int i = 0; i < platforms.size; i++) {
            Rectangle p = platforms.get(i);

            if (p.x + p.width < l || p.x > r) continue;

            int kind = platformKinds.get(i);
            float top = p.y + p.height;

            if (kind == KIND_GROUND) {
                float y0 = Math.max(p.y, b);

                if (top - y0 <= 0f) continue;

                drawStoneBody(p, y0, top);

            } else if (kind == KIND_ROCK) {
                // 무너진 돌기둥 바위
                shapes.setColor(0f, 0f, 0f, 0.3f);
                shapes.ellipse(p.x - 8f, p.y - 6f, p.width + 16f, 12f);

                drawStoneBody(p, p.y, top);

                // 금이 간 자국
                shapes.setColor(0f, 0f, 0f, 0.35f);
                shapes.rectLine(p.x + p.width * 0.3f, top - 12f, p.x + p.width * 0.55f, p.y + p.height * 0.45f, 1.5f);
                shapes.rectLine(p.x + p.width * 0.55f, p.y + p.height * 0.45f, p.x + p.width * 0.4f, p.y + 6f, 1.5f);

            } else if (kind == KIND_LEDGE) {
                // 떠 있는 섬: 아래 그림자 + 매달린 돌 + 뿌리
                shapes.setColor(0f, 0f, 0f, 0.20f);
                shapes.ellipse(p.x + p.width * 0.1f, p.y - 34f, p.width * 0.8f, 18f);

                for (float hx = p.x + 10f; hx < p.x + p.width - 10f; hx += 16f) {
                    float hl = 6f + hash(hx * 0.37f) * 16f;
                    shapes.setColor(st[0] * 0.32f, st[1] * 0.32f, st[2] * 0.34f, 1f);
                    shapes.triangle(hx - 9f, p.y + 2f, hx + 9f, p.y + 2f, hx + 1f, p.y - hl);
                }

                drawStoneBody(p, p.y, top);

                float[] mo = MOSS[ri()];

                if (round != 3) {
                    for (float hx = p.x + 14f; hx < p.x + p.width - 10f; hx += 23f) {
                        float len = 10f + hash(hx * 0.71f) * 22f;
                        float sway = (float) Math.sin(animTime * 1.6f + hx * 0.05f) * 3f;
                        shapes.setColor(mo[0] * 0.7f, mo[1] * 0.7f, mo[2] * 0.7f, 0.9f);
                        shapes.rectLine(hx, p.y + 4f, hx + sway, p.y - len, 1.6f);
                    }
                }

            } else {
                // 움직이는 발판: 마법 석판
                float glow =
                    0.5f + 0.5f * (float) Math.sin(animTime * 3f + p.x * 0.01f);

                shapes.setColor(0.3f, 0.85f, 1f, 0.10f + 0.10f * glow);
                shapes.ellipse(p.x - 10f, p.y - 30f, p.width + 20f, 26f);

                tmpA.set(0.07f, 0.08f, 0.12f, 1f);
                tmpB.set(0.22f, 0.24f, 0.31f, 1f);
                gradientRect(p.x, p.y, p.width, p.height, tmpA, tmpB);

                shapes.setColor(0.10f, 0.11f, 0.16f, 1f);
                shapes.triangle(
                    p.x + 6f, p.y,
                    p.x + p.width - 6f, p.y,
                    p.x + p.width / 2f, p.y - 16f
                );
            }
        }
    }

    private void drawTerrainTextures() {
        float l = viewLeft() - 20f;
        float r = l + W + 40f;

        for (int i = 0; i < platforms.size; i++) {
            Rectangle p = platforms.get(i);

            if (p.x + p.width < l || p.x > r) continue;

            int kind = platformKinds.get(i);

            if (kind == KIND_MOVER) continue;

            Texture terrain = groundTextures[platformTextureIndices.get(i)];

            if (terrain == null) {
                terrain = blockTexture;
            }

            if (terrain == null) continue;

            float top = p.y + p.height;
            float tileH = kind == KIND_GROUND ? TERRAIN_BAND : p.height;

            float tileW = tileH
                * ((float) terrain.getWidth() / terrain.getHeight());

            tileW = Math.max(16f, tileW);

            float startX = p.x;

            if (startX < l) {
                startX += (float) Math.floor((l - startX) / tileW) * tileW;
            }

            for (float tx = startX; tx < p.x + p.width && tx < r; tx += tileW) {
                float drawW = Math.min(tileW, p.x + p.width - tx);
                float u2 = drawW / tileW;

                batch.draw(
                    terrain,
                    tx,
                    top - tileH,
                    drawW,
                    tileH,
                    0f,
                    1f,
                    u2,
                    0f
                );
            }
        }
    }

    // 이끼 덮인 윗면 (밟을 수 있는 면이 분명하게 보이도록)
    private void drawTerrainEdges() {
        float l = viewLeft() - 20f;
        float r = l + W + 40f;

        float[] mo = MOSS[ri()];
        float[] mt = MOSS_TOP[ri()];

        for (int i = 0; i < platforms.size; i++) {
            Rectangle p = platforms.get(i);

            if (p.x + p.width < l || p.x > r) continue;

            int kind = platformKinds.get(i);
            float top = p.y + p.height;

            if (kind == KIND_MOVER) {
                float pulse =
                    0.55f + 0.45f * (float) Math.sin(animTime * 4f + p.x * 0.02f);

                shapes.setColor(0.35f, 0.9f, 1f, 0.85f * pulse);
                shapes.rect(p.x + 4f, top - 3f, p.width - 8f, 2f);

                for (int k = 0; k < 3; k++) {
                    float cx = p.x + p.width * (0.25f + 0.25f * k);
                    float cy = p.y + p.height * 0.45f;

                    shapes.setColor(0.45f, 0.95f, 1f, 0.6f * pulse);
                    shapes.triangle(cx - 4f, cy, cx + 4f, cy, cx, cy + 7f);
                    shapes.triangle(cx - 4f, cy, cx + 4f, cy, cx, cy - 7f);
                }
                continue;
            }

            float x0 = Math.max(p.x, l);
            float x1 = Math.min(p.x + p.width, r);

            // 이끼 띠
            tmpA.set(mo[0], mo[1], mo[2], 1f);
            tmpB.set(mt[0], mt[1], mt[2], 1f);
            gradientRect(x0, top - 10f, x1 - x0, 10f, tmpA, tmpB);

            // 아래로 흘러내린 이끼
            float dripChance = round == 3 ? 0.8f : 0.55f;
            float gx0 = (float) Math.floor(x0 / 9f) * 9f;

            for (float gx = gx0; gx < x1; gx += 9f) {
                float h = hash(gx * 0.71f + top * 0.13f);

                if (h < dripChance) continue;

                float jx = gx + (hash(gx * 1.37f) - 0.5f) * 6f;

                if (jx < p.x + 3f || jx > p.x + p.width - 3f) continue;

                float len = 3f + (float) Math.pow(hash(gx * 0.29f), 2.0) * (kind == KIND_GROUND ? 30f : 22f);
                float w = 4f + hash(gx * 0.33f) * 9f;

                shapes.setColor(mo[0] * 0.8f, mo[1] * 0.8f, mo[2] * 0.8f, 1f);
                shapes.ellipse(jx - w / 2f, top - 9f - len, w, len + 4f);

                // 덩어리 진 이끼
                if (h > 0.9f) {
                    shapes.setColor(mo[0], mo[1], mo[2], 1f);
                    shapes.ellipse(jx - w, top - 14f, w * 2f, 10f);
                }
            }

            shapes.setColor(
                Math.min(1f, mt[0] * 1.15f),
                Math.min(1f, mt[1] * 1.15f),
                Math.min(1f, mt[2] * 1.15f), 0.95f);
            shapes.rect(x0, top - 1.5f, x1 - x0, 1.5f);

            // 적 기지: 풀 대신 불씨
            if (round == 3) {
                for (float gx = gx0; gx < x1; gx += 11f) {
                    float h = hash(gx * 0.53f + top);

                    if (h < 0.9f) continue;

                    float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 3f + gx);
                    shapes.setColor(1f, 0.45f, 0.15f, 0.25f * pulse);
                    shapes.circle(gx, top - 4f, 6f, 10);
                    shapes.setColor(1f, 0.7f, 0.3f, 0.8f * pulse);
                    shapes.circle(gx, top - 4f, 1.6f, 6);
                }
                continue;
            }

            // 풀
            float tx0 = (float) Math.floor(x0 / 7f) * 7f;

            for (float gx = tx0; gx < x1; gx += 7f) {
                if (gx < p.x + 3f || gx > p.x + p.width - 3f) continue;

                float hsh = hash(gx * 0.173f);

                if (hsh < 0.4f) continue;

                float gh = 4f + hsh * 9f;
                float sway =
                    (float) Math.sin(animTime * 2f + gx * 0.05f) * 1.8f;

                float shade = 0.7f + 0.4f * hash(gx * 0.31f);

                shapes.setColor(
                    Math.min(1f, mt[0] * shade),
                    Math.min(1f, mt[1] * shade),
                    Math.min(1f, mt[2] * shade), 0.95f);
                shapes.triangle(gx - 2.5f, top - 1f, gx + 2.5f, top - 1f, gx + sway, top + gh);
            }

            // 작은 꽃
            for (float gx = gx0; gx < x1; gx += 11f) {
                if (hash(gx * 0.91f + 3f) < 0.94f) continue;

                float fy = top + 6f + hash(gx) * 4f;

                shapes.setColor(1f, 0.95f, 0.85f, 0.95f);
                shapes.circle(gx, fy, 2.2f, 6);
                shapes.setColor(1f, 0.8f, 0.3f, 1f);
                shapes.circle(gx, fy, 0.9f, 4);
            }
        }
    }

    private void drawScars() {
        for (float[] s : scars) {
            float a = Math.min(1f, s[3] / 2f);

            shapes.setColor(0.18f, 0.12f, 0.08f, 0.75f * a);
            shapes.rect(s[0], s[1] - 7f, s[2], 6f);
            shapes.setColor(0.32f, 0.22f, 0.14f, 0.6f * a);
            shapes.rect(s[0] + 3f, s[1] - 2f, s[2] - 6f, 2f);
        }
    }

    private void drawDecorations() {
        float l = viewLeft() - 80f;
        float r = l + W + 160f;

        float[] dark = BUSH_DARK[ri()];
        float[] light = BUSH_LIGHT[ri()];
        float[] mote = MOTE[ri()];

        for (Deco d : decorations) {
            if (d.x < l || d.x > r) continue;

            float s = d.size;

            if (d.kind == 0) {
                float sway = (float) Math.sin(animTime * 1.3f + d.seed) * 1.5f;

                shapes.setColor(dark[0], dark[1], dark[2], 1f);
                shapes.ellipse(d.x - 46f * s, d.y - 6f, 50f * s, 34f * s);
                shapes.ellipse(d.x - 20f * s + sway, d.y - 4f, 44f * s, 46f * s);
                shapes.ellipse(d.x + 4f * s, d.y - 6f, 46f * s, 36f * s);

                shapes.setColor(light[0], light[1], light[2], 1f);
                shapes.ellipse(d.x - 38f * s, d.y + 12f * s, 26f * s, 14f * s);
                shapes.ellipse(d.x - 12f * s + sway, d.y + 26f * s, 26f * s, 14f * s);
                shapes.ellipse(d.x + 14f * s, d.y + 14f * s, 22f * s, 12f * s);

            } else {
                float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 2f + d.seed);
                float h = 26f * s;
                float w = 9f * s;

                shapes.setColor(mote[0], mote[1], mote[2], 0.10f + 0.10f * pulse);
                shapes.circle(d.x, d.y + h * 0.5f, 26f * s, 18);

                shapes.setColor(mote[0] * 0.6f, mote[1] * 0.6f, mote[2] * 0.6f, 1f);
                shapes.triangle(d.x - w, d.y, d.x + w, d.y, d.x, d.y + h);

                shapes.setColor(mote[0], mote[1], mote[2], 0.9f);
                shapes.triangle(d.x - w * 0.15f, d.y + 2f, d.x + w * 0.7f, d.y + 2f, d.x, d.y + h);

                shapes.setColor(mote[0] * 0.5f, mote[1] * 0.5f, mote[2] * 0.5f, 1f);
                shapes.triangle(
                    d.x + w * 0.6f, d.y,
                    d.x + w * 2.0f, d.y,
                    d.x + w * 1.4f, d.y + h * 0.55f
                );
            }
        }
    }

    // 바론 함정 바닥: 균열과 경고 빛
    private void drawTrapFloors() {
        float l = viewLeft() - 300f;
        float r = l + W + 600f;

        for (PillarTrap t : traps) {
            if (t.centerX < l || t.centerX > r) continue;

            float top = t.groundTop;
            float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 3f + t.centerX);

            // 바론 둥지: 보라색 독웅덩이
            shapes.setColor(0.35f, 0.1f, 0.55f, 0.18f + 0.1f * pulse);
            shapes.ellipse(t.centerX - CELL * 3.6f, top - 12f, CELL * 7.2f, 22f);

            for (int i = 0; i < 6; i++) {
                float cx = t.cellX[i];

                shapes.setColor(0.16f, 0.05f, 0.24f, 0.85f);
                shapes.rect(cx + 2f, top - 8f, CELL - 4f, 8f);

                // 갈라진 틈
                shapes.setColor(0.6f, 0.28f, 0.9f, 0.35f + 0.25f * pulse);
                shapes.rectLine(cx + 8f, top - 3f, cx + CELL * 0.45f, top - 6f, 1.5f);
                shapes.rectLine(cx + CELL * 0.45f, top - 6f, cx + CELL - 8f, top - 2f, 1.5f);
            }

            shapes.setColor(0.45f, 0.12f, 0.75f, 0.95f);
            shapes.rect(t.centerX - CELL / 2f, top - 9f, CELL, 9f);

            if (t.state == 1) {
                float p = 1f - Math.max(0f, t.timer) / trapWarnTime();
                float flicker =
                    0.75f + 0.25f * (float) Math.sin(animTime * 40f);

                for (int i = 0; i < 6; i++) {
                    if (!t.chosen[i]) continue;

                    float cx = t.cellX[i];

                    shapes.setColor(0.8f, 0.4f, 1f, (0.35f + 0.6f * p) * flicker);
                    shapes.rect(cx + 2f, top - 8f, CELL - 4f, 8f);

                    shapes.setColor(0.75f, 0.35f, 1f, 0.25f * p);
                    shapes.ellipse(cx - 6f, top - 10f, CELL + 12f, 20f);

                    tmpA.set(0.65f, 0.30f, 1f, 0.26f * p);
                    tmpB.set(0.65f, 0.30f, 1f, 0f);
                    gradientRect(cx + 6f, top, CELL - 12f, 200f, tmpA, tmpB);
                }
            }
        }
    }

    // 바론 팔 (촉수)
    private void drawBaronArms() {
        float l = viewLeft() - 100f;
        float r = l + W + 200f;
        float vb = viewBottom() - 20f;
        float vt = viewBottom() + H + 40f;

        for (PillarTrap t : traps) {
            if (t.height <= 0f) continue;
            if (t.centerX < l - 200f || t.centerX > r + 200f) continue;

            float alpha = t.state == 4
                ? Math.max(0f, t.timer) / 0.35f
                : 1f;

            for (int i = 0; i < 6; i++) {
                if (!t.chosen[i]) continue;

                drawTentacle(t.cellX[i] + CELL / 2f, t.groundTop, t.height,
                    i * 1.7f + t.centerX * 0.01f, alpha, vb, vt);
            }
        }
    }

    private void drawTentacle(float baseX, float baseY, float h, float seed,
                              float alpha, float viewB, float viewT) {
        float seg = 14f;
        float baseW = 21f;

        float prevY = baseY;
        float prevCx = baseX;
        float prevHw = baseW;

        int n = (int) (h / seg) + 1;

        for (int s = 1; s <= n; s++) {
            float y = Math.min(baseY + h, baseY + s * seg);
            float f = (y - baseY) / Math.max(1f, h);

            float hw = baseW * (1f - 0.88f * (float) Math.pow(f, 1.25));
            float cx = baseX + (float) Math.sin(animTime * 3.2f + (y - baseY) * 0.012f + seed)
                * 15f * f;

            if (y >= viewB && prevY <= viewT) {
                // 바깥 보라빛
                shapes.setColor(0.6f, 0.25f, 0.95f, 0.16f * alpha);
                shapes.triangle(prevCx - prevHw - 6f, prevY, prevCx + prevHw + 6f, prevY, cx + hw + 6f, y);
                shapes.triangle(prevCx - prevHw - 6f, prevY, cx + hw + 6f, y, cx - hw - 6f, y);

                // 원통처럼 보이는 명암
                tmpA.set(0.18f, 0.04f, 0.30f, alpha);
                tmpB.set(0.52f, 0.20f, 0.72f, alpha);
                tmpC.set(0.30f, 0.07f, 0.45f, alpha);

                shapes.triangle(prevCx - prevHw, prevY, prevCx, prevY, cx, y, tmpA, tmpB, tmpB);
                shapes.triangle(prevCx - prevHw, prevY, cx, y, cx - hw, y, tmpA, tmpB, tmpA);
                shapes.triangle(prevCx, prevY, prevCx + prevHw, prevY, cx + hw, y, tmpB, tmpC, tmpC);
                shapes.triangle(prevCx, prevY, cx + hw, y, cx, y, tmpB, tmpC, tmpB);

                // 빨판
                if (s % 3 == 0 && hw > 5f) {
                    shapes.setColor(0.92f, 0.55f, 0.85f, 0.85f * alpha);
                    shapes.circle(cx + hw * 0.55f, y, Math.max(1.8f, hw * 0.28f), 8);
                    shapes.setColor(0.45f, 0.12f, 0.45f, 0.9f * alpha);
                    shapes.circle(cx + hw * 0.55f, y, Math.max(0.8f, hw * 0.12f), 6);
                }

                // 가시
                if (s % 4 == 1 && hw > 6f) {
                    shapes.setColor(0.85f, 0.75f, 0.95f, 0.9f * alpha);
                    shapes.triangle(cx - hw, y - 4f, cx - hw, y + 4f, cx - hw - 9f, y + 9f);
                }
            }

            prevY = y;
            prevCx = cx;
            prevHw = hw;
        }

        // 끝의 발톱
        float tipY = baseY + h;

        if (tipY >= viewB && tipY <= viewT + 30f) {
            shapes.setColor(0.9f, 0.8f, 1f, alpha);
            shapes.triangle(prevCx - 4f, tipY, prevCx + 4f, tipY, prevCx - 6f, tipY + 16f);
        }

        // 땅을 뚫고 나온 자리
        shapes.setColor(0.8f, 0.55f, 1f, 0.5f * alpha);
        shapes.ellipse(baseX - 30f, baseY - 9f, 60f, 18f);
        shapes.setColor(0.2f, 0.12f, 0.12f, 0.9f * alpha);
        shapes.ellipse(baseX - 24f, baseY - 6f, 48f, 10f);
    }

    private void drawNexusGlow() {
        float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 2.4f);
        float cx = nexus.x + nexus.width / 2f;

        shapes.setColor(0.45f, 0.75f, 1f, 0.10f + 0.10f * pulse);
        shapes.ellipse(cx - 110f, nexus.y - 20f, 220f, 46f);

        shapes.setColor(0.65f, 0.88f, 1f, 0.25f + 0.2f * pulse);
        shapes.ellipse(cx - 60f, nexus.y - 8f, 120f, 18f);

        tmpA.set(0.55f, 0.85f, 1f, 0.20f * pulse);
        tmpB.set(0.55f, 0.85f, 1f, 0f);
        gradientRect(cx - 18f, nexus.y, 36f, 520f, tmpA, tmpB);

        if (nexusTexture == null) {
            shapes.setColor(0.2f, 0.35f, 0.6f, 1f);
            shapes.rect(nexus.x + 10f, nexus.y, nexus.width - 20f, 30f);
            shapes.setColor(0.55f, 0.85f, 1f, 1f);
            shapes.triangle(
                cx - 24f, nexus.y + 30f,
                cx + 24f, nexus.y + 30f,
                cx, nexus.y + nexus.height
            );
        }
    }

    private void drawCheckpointShapes() {
        for (Checkpoint c : checkpoints) {
            if (c.active) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 3f + c.x);

                shapes.setColor(1f, 0.85f, 0.45f, 0.10f + 0.08f * pulse);
                shapes.circle(c.x, c.y + 40f, 46f, 24);
            }

            if (c.flash > 0f) {
                float k = 1f - c.flash;

                shapes.setColor(1f, 0.9f, 0.55f, 0.35f * c.flash);
                shapes.circle(c.x, c.y + 40f, 30f + 120f * k, 32);
            }

            if (flagTexture == null) {
                shapes.setColor(0.35f, 0.32f, 0.30f, 1f);
                shapes.rect(c.x - 2f, c.y, 4f, 76f);

                if (c.active) {
                    shapes.setColor(0.95f, 0.75f, 0.30f, 1f);
                } else {
                    shapes.setColor(0.45f, 0.45f, 0.50f, 1f);
                }

                float wave = (float) Math.sin(animTime * 4f + c.x) * 3f;

                shapes.triangle(
                    c.x + 2f, c.y + 74f,
                    c.x + 2f, c.y + 48f,
                    c.x + 34f, c.y + 61f + wave
                );
            }
        }
    }

    private void drawNexusAndFlags() {
        if (nexusTexture != null) {
            batch.draw(
                nexusTexture,
                nexus.x,
                nexus.y,
                nexus.width,
                nexus.height
            );
        }

        if (flagTexture != null) {
            float fh = 78f;
            float fw = fh
                * ((float) flagTexture.getWidth() / flagTexture.getHeight());

            fw = Math.max(28f, Math.min(70f, fw));

            for (Checkpoint c : checkpoints) {
                if (c.active) {
                    batch.setColor(Color.WHITE);
                } else {
                    batch.setColor(0.5f, 0.5f, 0.55f, 0.9f);
                }

                batch.draw(flagTexture, c.x - fw / 2f, c.y, fw, fh);
            }

            batch.setColor(Color.WHITE);
        }
    }

    // 함정 가운데의 바론
    private void drawBarons() {
        if (baronTexture == null) return;

        float l = viewLeft() - 200f;
        float r = l + W + 400f;

        float bh = 128f;
        float bw = bh * ((float) baronTexture.getWidth() / baronTexture.getHeight());

        bw = Math.max(80f, Math.min(240f, bw));

        for (PillarTrap t : traps) {
            if (t.centerX < l || t.centerX > r) continue;

            float bob = (float) Math.sin(animTime * 1.8f + t.centerX) * 3f;
            float scale = 1f;
            float shake = 0f;

            if (t.state == 1) {
                scale = 1.05f;
                shake = (random.nextFloat() - 0.5f) * 4f;
                batch.setColor(1f, 0.85f, 1f, 1f);
            } else {
                batch.setColor(Color.WHITE);
            }

            float w = bw * scale;
            float h = bh * scale;

            batch.draw(baronTexture, t.centerX - w / 2f + shake, t.groundTop - 6f + bob, w, h);
        }

        batch.setColor(Color.WHITE);
    }

    private Texture monsterTexture(Monster m) {
        if (m.kind == MON_WIND_DRAGON) return windDragonTexture;
        if (m.kind == MON_EARTH_DRAGON) return earthDragonTexture;
        return heraldTexture;
    }

    // ridden = true 이면 탑승 중인 전령만, false 이면 나머지만 그립니다.
    private void drawMonsters(boolean ridden) {
        for (int i = 0; i < monsters.size; i++) {
            Monster m = monsters.get(i);

            if ((m == rideHerald) != ridden) continue;

            Texture t = monsterTexture(m);

            if (t == null) continue;

            float punch = 1f + 0.1f * m.punch;
            float dh = m.rect.height * 1.12f * punch;
            float dw = dh * ((float) t.getWidth() / t.getHeight());

            dw = Math.max(m.rect.width * 0.8f, Math.min(m.rect.width * 1.7f, dw));

            float cx = m.rect.x + m.rect.width / 2f;
            float x = cx - dw / 2f;
            float y = m.rect.y;
            float rot = 0f;

            float alpha = m.alive ? 1f : Math.max(0f, m.deathTimer / 0.8f);

            if (m.kind == MON_HERALD) {
                // 전령은 미세하게 흔들거립니다. 탑승 중에는 더 크게.
                float amp = m == rideHerald ? 1f : 0.35f;
                y += (float) Math.sin(animTime * 18f + m.seed) * 2.5f * amp;
                rot = (float) Math.sin(animTime * 9f + m.seed) * 3f * amp;
            }

            if (m.flash > 0f) {
                batch.setColor(1f, 0.55f, 0.55f, alpha);
            } else {
                batch.setColor(1f, 1f, 1f, alpha);
            }

            boolean flip = m.kind == MON_HERALD && FLIP_HERALD_IMAGE;

            batch.draw(t, x, y, dw / 2f, 0f, dw, dh, 1f, 1f, rot,
                0, 0, t.getWidth(), t.getHeight(), flip, false);
        }

        batch.setColor(Color.WHITE);
    }

    // 이미지가 없을 때의 용/전령/바론 대체 모양과 발밑 빛
    private void drawMonsterFallbacks() {
        for (int i = 0; i < monsters.size; i++) {
            Monster m = monsters.get(i);
            float cx = m.rect.x + m.rect.width / 2f;

            float r, g, b;

            if (m.kind == MON_WIND_DRAGON) {
                r = 0.8f; g = 0.85f; b = 0.9f;
            } else if (m.kind == MON_EARTH_DRAGON) {
                r = 0.65f; g = 0.45f; b = 0.25f;
            } else {
                r = 0.6f; g = 0.35f; b = 0.85f;
            }

            float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 2.5f + m.seed);

            if (m.kind != MON_HERALD) {
                float ground = m.baseY - 26f;
                shapes.setColor(r, g, b, 0.12f + 0.08f * pulse);
                shapes.ellipse(cx - 80f, ground - 12f, 160f, 26f);
            } else if (m != rideHerald) {
                shapes.setColor(0f, 0f, 0f, 0.3f);
                shapes.ellipse(cx - 60f, m.rect.y - 6f, 120f, 12f);
            }

            if (monsterTexture(m) != null || !m.alive) continue;

            shapes.setColor(r * 0.6f, g * 0.6f, b * 0.6f, 1f);
            shapes.ellipse(m.rect.x, m.rect.y, m.rect.width, m.rect.height * 0.7f);
            shapes.setColor(r, g, b, 1f);
            shapes.ellipse(m.rect.x + 10f, m.rect.y + 10f, m.rect.width - 20f, m.rect.height * 0.5f);
            shapes.setColor(1f, 0.95f, 0.6f, 1f);
            shapes.circle(m.rect.x + m.rect.width * 0.72f, m.rect.y + m.rect.height * 0.5f, 4f, 8);
        }

        if (baronTexture == null) {
            for (PillarTrap t : traps) {
                float glow = 0.5f + 0.5f * (float) Math.sin(animTime * 2f + t.centerX);
                shapes.setColor(0.22f, 0.07f, 0.32f, 1f);
                shapes.ellipse(t.centerX - 50f, t.groundTop - 10f, 100f, 90f);
                shapes.setColor(1f, 0.35f, 0.9f, 0.7f + 0.3f * glow);
                shapes.circle(t.centerX - 14f, t.groundTop + 50f, 4f, 8);
                shapes.circle(t.centerX + 14f, t.groundTop + 50f, 4f, 8);
            }
        }
    }

    private void drawPlayerAuras() {
        float cx = player.x + player.width / 2f;

        if (earthBuff && !launched) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 3f);
            shapes.setColor(0.62f, 0.42f, 0.22f, 0.25f + 0.15f * pulse);
            shapes.ellipse(cx - 34f, player.y - 6f, 68f, 12f);
        }

        if (olafActive) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 10f);
            shapes.setColor(1f, 0.4f, 0.1f, 0.12f + 0.08f * pulse);
            shapes.ellipse(cx - 44f, player.y - 6f, 88f, 90f);
        }

        // 리 신 표식
        if (markActive) {
            float mx = markX;
            float my = markY;

            if (markMonster != null && markMonster.alive) {
                mx = markMonster.rect.x + markMonster.rect.width / 2f;
                my = markMonster.rect.y + markMonster.rect.height / 2f;
            }

            float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 8f);

            shapes.setColor(0.4f, 0.85f, 1f, 0.15f + 0.1f * pulse);
            shapes.circle(mx, my, 30f, 24);
        }
    }

    private void drawShadows() {
        if (!launched && !riding) {
            float cx = player.x + player.width / 2f;
            float best = -9999f;

            for (Rectangle p : platforms) {
                if (cx < p.x || cx > p.x + p.width) continue;

                float top = p.y + p.height;

                if (top <= player.y + 1f && top > best) {
                    best = top;
                }
            }

            if (best > -9000f) {
                float dist = player.y - best;
                float k = Math.max(0.3f, Math.min(1f, 1f - dist / 320f));

                shapes.setColor(0f, 0f, 0f, 0.38f * k);
                shapes.ellipse(cx - 28f * k, best - 5f * k, 56f * k, 10f * k);
            }
        }

        for (Minion m : minions) {
            shapes.setColor(0f, 0f, 0f, 0.3f);
            shapes.ellipse(
                m.rect.x + 4f,
                m.rect.y - 4f,
                m.rect.width - 8f,
                8f
            );
        }
    }

    private void drawParticles() {
        for (int i = 0; i < particles.size; i++) {
            Particle p = particles.get(i);
            float k = p.life / p.maxLife;

            if (p.kind == P_RING) {
                float rad = p.size + (1f - k) * p.grow;
                shapes.setColor(p.r, p.g, p.b, p.a * k);
                drawArc(p.x, p.y, rad, 0f, (float) Math.PI * 2f, 2.5f * k + 0.5f, 24);

            } else if (p.kind == P_CHUNK) {
                shapes.setColor(p.r, p.g, p.b, Math.min(1f, k * 2f));
                shapes.rect(p.x - p.size / 2f, p.y - p.size / 2f, p.size / 2f, p.size / 2f,
                    p.size, p.size, 1f, 1f, p.rot);

            } else if (p.kind == P_ORB) {
                // 투명한 구슬
                float s = p.size;
                shapes.setColor(p.r, p.g, p.b, 0.18f);
                shapes.circle(p.x, p.y, s * 1.9f, 14);
                shapes.setColor(p.r, p.g, p.b, 0.45f);
                shapes.circle(p.x, p.y, s, 14);
                shapes.setColor(1f, 1f, 1f, 0.7f);
                shapes.circle(p.x - s * 0.35f, p.y + s * 0.35f, s * 0.3f, 8);

            } else {
                float s = p.size + (1f - k) * p.grow;
                shapes.setColor(p.r, p.g, p.b, p.a * k);
                shapes.circle(p.x, p.y, s, 10);
            }
        }
    }

    private void drawSlash() {
        if (slashTimer <= 0f) return;

        float px =
            player.x + player.width / 2f
                + slashSide * 42f;

        float py =
            player.y + player.height * 0.62f;

        float fade = Math.min(
            1f,
            slashTimer / 0.22f
        );

        shapes.setColor(
            1f, 0.88f, 0.42f, 0.28f * fade
        );

        shapes.ellipse(
            px - 31f,
            py - 15f,
            66f,
            30f
        );

        shapes.setColor(
            1f, 0.97f, 0.78f, 0.85f * fade
        );

        shapes.ellipse(
            px - 25f,
            py - 5f,
            52f,
            10f
        );
    }

    // 리 신 조준선, 음파, 표식
    private void drawLeeEffects() {
        if (champion == 2 && leeAimMode) {
            float sx = player.x + player.width / 2f;
            float sy = player.y + player.height * 0.55f;

            shapes.setColor(0.4f, 0.9f, 1f, 0.5f);
            shapes.circle(sx, sy, 9f, 16);

            tmpVec.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
            camera.unproject(tmpVec);

            float dx = tmpVec.x - sx;
            float dy = tmpVec.y - sy;
            float len = (float) Math.sqrt(dx * dx + dy * dy);

            if (len > 1f) {
                dx /= len;
                dy /= len;

                for (float d = 24f; d < Math.min(len, 520f); d += 22f) {
                    float a = 0.65f * (1f - d / 560f);
                    shapes.setColor(0.55f, 0.95f, 1f, a);
                    shapes.circle(sx + dx * d, sy + dy * d, 2.6f, 8);
                }
            }
        }

        if (sonicWaveActive) {
            float dir = (float) Math.toRadians(waveDirection);
            float ux = (float) Math.cos(dir);
            float uy = (float) Math.sin(dir);

            // 꼬리
            for (int k = 0; k < 6; k++) {
                float d0 = k * 12f;
                float d1 = d0 + 12f;
                float a = 0.35f * (1f - k / 6f);

                shapes.setColor(0.35f, 0.75f, 1f, a);
                shapes.rectLine(waveX - ux * d0, waveY - uy * d0,
                    waveX - ux * d1, waveY - uy * d1, 7f * (1f - k / 7f));
            }

            // 앞으로 휘어진 음파 3겹
            for (int layer = 0; layer < 3; layer++) {
                float back = layer * 15f;
                float rad = 26f - layer * 3f;
                float fade = 1f - layer * 0.3f;

                float cx = waveX - ux * (rad + back);
                float cy = waveY - uy * (rad + back);
                float a0 = dir - 1.1f;
                float a1 = dir + 1.1f;

                shapes.setColor(0.15f, 0.55f, 1f, 0.16f * fade);
                drawArc(cx, cy, rad, a0, a1, 13f, 12);

                shapes.setColor(0.45f, 0.86f, 1f, 0.5f * fade);
                drawArc(cx, cy, rad, a0, a1, 6f, 12);

                shapes.setColor(0.9f, 0.98f, 1f, 0.95f * fade);
                drawArc(cx, cy, rad, a0 + 0.15f, a1 - 0.15f, 2.2f, 12);
            }

            shapes.setColor(0.4f, 0.85f, 1f, 0.3f);
            shapes.circle(waveX, waveY, 12f, 16);
            shapes.setColor(0.95f, 1f, 1f, 0.95f);
            shapes.circle(waveX, waveY, 4.5f, 12);
        }

        if (markActive) {
            float mx = markX;
            float my = markY;

            if (markMonster != null && markMonster.alive) {
                mx = markMonster.rect.x + markMonster.rect.width / 2f;
                my = markMonster.rect.y + markMonster.rect.height / 2f;
            }

            float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 8f);
            float spin = animTime * 3f;

            shapes.setColor(0.45f, 0.9f, 1f, 0.85f);
            drawArc(mx, my, 20f + pulse * 3f, 0f, (float) Math.PI * 2f, 2.5f, 28);

            for (int k = 0; k < 3; k++) {
                float a = spin + k * 2.094f;
                shapes.setColor(0.8f, 0.97f, 1f, 0.9f);
                drawArc(mx, my, 31f, a, a + 0.8f, 2.5f, 6);
            }

            shapes.setColor(0.9f, 1f, 1f, 1f);
            shapes.circle(mx, my, 3.5f, 10);
        }
    }

    private void drawHealthBars() {
        float l = viewLeft() - 100f;
        float r = l + W + 200f;

        for (int i = 0; i < monsters.size; i++) {
            Monster m = monsters.get(i);

            if (!m.alive || m == rideHerald) continue;

            float cx = m.rect.x + m.rect.width / 2f;

            if (cx < l || cx > r) continue;

            float bw = 112f;
            float bh = 9f;
            float x = cx - bw / 2f;
            float y = m.rect.y + m.rect.height * 1.12f + 18f;

            shapes.setColor(0.62f, 0.5f, 0.28f, 0.95f);
            shapes.rect(x - 2f, y - 2f, bw + 4f, bh + 4f);
            shapes.setColor(0.05f, 0.05f, 0.08f, 0.95f);
            shapes.rect(x, y, bw, bh);

            // 줄어드는 체력의 잔상
            shapes.setColor(1f, 0.85f, 0.55f, 0.9f);
            shapes.rect(x, y, bw * m.shownHp / MONSTER_HP, bh);

            tmpA.set(0.62f, 0.08f, 0.08f, 1f);
            tmpB.set(0.95f, 0.30f, 0.25f, 1f);
            gradientRect(x, y, bw * Math.max(0, m.hp) / MONSTER_HP, bh, tmpA, tmpB);

            for (int k = 1; k < MONSTER_HP; k++) {
                shapes.setColor(0f, 0f, 0f, 0.8f);
                shapes.rect(x + bw * k / MONSTER_HP - 1f, y, 2f, bh);
            }

            shapes.setColor(1f, 1f, 1f, 0.25f);
            shapes.rect(x, y + bh - 2f, bw, 1.5f);
        }
    }

    // 화면 맨 앞을 지나가는 어두운 풀숲 실루엣 (깊이감)
    private void drawForeground() {
        float l = viewLeft();
        float b = viewBottom();
        float span = W + 500f;

        for (int i = 0; i < 7; i++) {
            float x = (hash(i * 3.3f + 7f) * 4000f - camera.position.x * 1.35f) % span;

            if (x < 0f) x += span;

            x = l + x - 250f;

            float w = 140f + hash(i * 1.7f) * 140f;
            float h = 22f + hash(i * 5.1f) * 22f;

            shapes.setColor(0.02f, 0.035f, 0.035f, 0.85f);
            shapes.ellipse(x, b - h * 0.6f, w, h);
            shapes.ellipse(x + w * 0.35f, b - h * 0.5f, w * 0.55f, h * 1.15f);
        }
    }

    // 발러 (behind = true 이면 캐릭터 뒤쪽을 지날 때만 그립니다)
    private void drawValor(boolean behind) {
        if (valorState == VALOR_NONE || valorState == VALOR_CARRYING) return;

        boolean isBehind = valorState == VALOR_SPIRAL && valorBehind;

        if (isBehind != behind) return;

        Texture t = valorTexture;

        if (t == null) return;

        float h = 64f;
        float w = h * ((float) t.getWidth() / t.getHeight());

        w = Math.max(60f, Math.min(150f, w));

        boolean right = valorX >= valorPrevX;
        boolean flip = right == FLIP_VALOR_IMAGE;
        float flap = (float) Math.sin(animTime * 22f) * 0.08f;
        float alpha = valorState == VALOR_LEAVING
            ? Math.max(0f, 1f - valorTimer / VALOR_LEAVE_TIME)
            : 1f;

        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(t, valorX - w / 2f, valorY - h / 2f, w / 2f, h / 2f, w, h,
            1f, 1f + flap, right ? -8f : 8f,
            0, 0, t.getWidth(), t.getHeight(), flip, false);
        batch.setColor(Color.WHITE);
    }

    // 화살, 소용돌이, 비행 시간 링, 발러 이미지가 없을 때의 대체 모양
    private void drawQuinnEffects() {
        for (int i = 0; i < arrows.size; i++) {
            Arrow a = arrows.get(i);

            float sp = Math.max(1f, (float) Math.sqrt(a.vx * a.vx + a.vy * a.vy));
            float ux = a.vx / sp;
            float uy = a.vy / sp;
            float nx = -uy;
            float ny = ux;

            float tailX = a.x - ux * 20f;
            float tailY = a.y - uy * 20f;

            shapes.setColor(0.55f, 0.85f, 1f, 0.25f);
            shapes.rectLine(tailX - ux * 10f, tailY - uy * 10f, a.x, a.y, 5f);

            shapes.setColor(0.85f, 0.72f, 0.5f, 1f);
            shapes.rectLine(tailX, tailY, a.x, a.y, 1.8f);

            shapes.setColor(0.92f, 0.96f, 1f, 1f);
            shapes.triangle(
                a.x + ux * 7f, a.y + uy * 7f,
                a.x + nx * 3.2f, a.y + ny * 3.2f,
                a.x - nx * 3.2f, a.y - ny * 3.2f
            );

            shapes.setColor(0.45f, 0.7f, 1f, 1f);
            shapes.triangle(
                tailX, tailY,
                tailX - ux * 6f + nx * 3.5f, tailY - uy * 6f + ny * 3.5f,
                tailX + ux * 2f, tailY + uy * 2f
            );
            shapes.triangle(
                tailX, tailY,
                tailX - ux * 6f - nx * 3.5f, tailY - uy * 6f - ny * 3.5f,
                tailX + ux * 2f, tailY + uy * 2f
            );
        }

        float cx = player.x + player.width / 2f;

        // 소용돌이: 발러가 돌며 내려오는 동안 공기가 휘감깁니다.
        if (valorState == VALOR_SPIRAL) {
            float p = Math.min(1f, valorTimer / VALOR_SPIRAL_TIME);
            float baseY = player.y + player.height;

            for (int k = 0; k < 3; k++) {
                float r = (150f * (1f - p) + 30f) * (1f - k * 0.22f);
                float a0 = valorAngle - 1.6f - k * 0.9f;

                shapes.setColor(0.75f, 0.9f, 1f, 0.35f * (1f - k * 0.25f));
                drawArc(cx, baseY + 40f + k * 22f, r, a0, a0 + 2.2f, 2.5f, 14);
            }
        }

        // bal.png가 없을 때: 파란 새 모양으로 대신 그립니다.
        if (valorTexture == null
            && (valorState == VALOR_ARRIVING || valorState == VALOR_SPIRAL
            || valorState == VALOR_LEAVING || (isFlying() && quinnFlyTexture == null))) {
            float bx = isFlying() ? cx : valorX;
            float by = isFlying() ? player.y + QUINN_DRAW_H + 10f : valorY;
            float wing = (float) Math.sin(animTime * 20f) * 14f;

            shapes.setColor(0.25f, 0.45f, 0.85f, 1f);
            shapes.triangle(bx - 34f, by + wing, bx, by + 4f, bx, by - 6f);
            shapes.triangle(bx + 34f, by + wing, bx, by + 4f, bx, by - 6f);
            shapes.setColor(0.9f, 0.95f, 1f, 1f);
            shapes.ellipse(bx - 12f, by - 8f, 24f, 14f);
            shapes.setColor(1f, 0.8f, 0.3f, 1f);
            shapes.triangle(bx + 12f, by, bx + 12f, by - 5f, bx + 19f, by - 3f);
        }

        // 비행 중: 남은 시간 표시 링
        if (isFlying()) {
            float k = flightTimer / VALOR_FLIGHT_TIME;
            float cy = player.y + QUINN_DRAW_H + 30f;

            shapes.setColor(0f, 0f, 0f, 0.35f);
            drawArc(cx, cy, 13f, 0f, (float) Math.PI * 2f, 4f, 24);

            shapes.setColor(0.55f, 0.85f, 1f, 0.95f);
            drawArc(cx, cy, 13f, (float) Math.PI / 2f,
                (float) Math.PI / 2f + (float) Math.PI * 2f * k, 3f, 24);

            // 날갯짓 바람
            if (random.nextFloat() < 0.3f) {
                Particle pt = addParticle(P_DOT, cx + (random.nextFloat() - 0.5f) * 60f,
                    player.y + player.height * 0.8f, (facingRight ? -1f : 1f) * 80f, -30f,
                    0.4f, 2f, 0.85f, 0.95f, 1f, 0.6f);

                if (pt != null) pt.drag = 1.5f;
            }
        }
    }

    private void drawGhosts() {
        Texture texture = championSprites[champion];

        if (texture == null) return;

        float drawW = Math.max(54f, Math.min(DRAW_H * ((float) texture.getWidth() / texture.getHeight()), 88f));

        for (int i = 0; i < ghosts.size; i++) {
            float[] g = ghosts.get(i);
            float a = g[2] / g[3];

            if (g[5] == 0f) {
                batch.setColor(0.45f, 0.85f, 1f, 0.45f * a);
            } else {
                batch.setColor(1f, 0.45f, 0.15f, 0.4f * a);
            }

            float x = g[0] + PLAYER_W / 2f - drawW / 2f;

            batch.draw(texture, x, g[1] - 1f, drawW / 2f, 0f, drawW, DRAW_H, 1f, 1f, 0f,
                0, 0, texture.getWidth(), texture.getHeight(), g[4] == 1f, false);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawPlayer() {
        Texture texture = championSprites[champion];

        float drawW = DRAW_W;
        float drawH = DRAW_H;

        // 퀸이 발러를 타고 나는 중: qw.png (없으면 bal.png를 머리 위에 그리고 q2.png)
        if (isFlying()) {
            drawFlyingQuinn();
            return;
        }

        if (texture != null) {
            float aspect =
                (float) texture.getWidth()
                    / texture.getHeight();

            drawW = DRAW_H * aspect;
            drawW = Math.max(
                54f,
                Math.min(drawW, 88f)
            );

            drawH = DRAW_H;
        }

        // 퀸: 여백을 잘라낸 영역만 더 크게 그리고, 발이 땅에 닿게 합니다.
        int srcX = 0, srcY = 0;
        int srcW = texture != null ? texture.getWidth() : 1;
        int srcH = texture != null ? texture.getHeight() : 1;
        float footY = -1f;

        if (champion == QUINN && texture != null) {
            if (quinnTrim != null) {
                srcX = quinnTrim[0];
                srcY = quinnTrim[1];
                srcW = quinnTrim[2];
                srcH = quinnTrim[3];
            }

            drawH = QUINN_DRAW_H;
            drawW = Math.max(30f, Math.min(drawH * ((float) srcW / srcH), 130f));
            footY = -3f;
        }

        float sx = squashX;
        float sy = squashY;
        float rotation = 0f;
        boolean centered = false;

        if (launched) {
            rotation = facingRight ? -launchSpin : launchSpin;
            centered = true;
        }

        // 자크 궁: 공처럼 웅크린 채 굴러 날아갑니다.
        if (champion == 0 && zacAirborne && zacAirTime > 0.1f) {
            sx *= 0.9f;
            sy *= 0.78f;
            rotation = zacSpin;
            centered = true;
        }

        if (riding) {
            rotation = (float) Math.sin(animTime * 9f) * 3f;
        }

        float dw = drawW * sx;
        float dh = drawH * sy;

        float drawX = player.x + player.width / 2f - dw / 2f;
        float drawY = centered
            ? player.y + player.height / 2f - dh / 2f
            : player.y + footY;

        float originY = centered ? dh / 2f : 0f;

        float alpha = 1f;

        if (hitCooldown > 0f && ((int) (animTime * 18f)) % 2 == 0) {
            alpha = 0.45f;
        }

        float cr = 1f, cg = 1f, cb = 1f;

        if (champion == 1 && olafActive) {
            // 올라프 궁: 유령처럼 반투명한 분노 상태
            cr = 1f; cg = 0.6f; cb = 0.35f;
            alpha *= 0.8f;
        }

        if (leeDashing) {
            cr = 0.7f; cg = 0.95f; cb = 1f;
        }

        batch.setColor(cr, cg, cb, alpha);

        if (texture != null) {
            batch.draw(
                texture,
                drawX,
                drawY,
                dw / 2f,
                originY,
                dw,
                dh,
                1f,
                1f,
                rotation,
                srcX,
                srcY,
                srcW,
                srcH,
                !facingRight,
                false
            );
        } else {
            batch.end();

            beginShapes();

            if (champion == 0) {
                shapes.setColor(0.35f, 0.85f, 0.35f, alpha);
            } else if (champion == 1) {
                shapes.setColor(olafActive ? 1f : 0.9f, olafActive ? 0.55f : 0.2f, 0.15f, alpha);
            } else if (champion == QUINN) {
                shapes.setColor(0.35f, 0.55f, 0.9f, alpha);
            } else {
                shapes.setColor(0.25f, 0.4f, 0.95f, alpha);
            }

            shapes.rect(
                drawX,
                drawY,
                dw / 2f,
                originY,
                dw,
                dh,
                1f,
                1f,
                rotation
            );

            shapes.end();
            batch.begin();
        }

        batch.setColor(Color.WHITE);
    }

    private void drawFlyingQuinn() {
        float cx = player.x + player.width / 2f;
        float bob = (float) Math.sin(animTime * 7f) * 4f;
        float tilt = velocityX == 0f ? 0f : (facingRight ? -6f : 6f);

        if (velocityY > 0f) tilt += facingRight ? 4f : -4f;
        if (velocityY < 0f) tilt += facingRight ? -4f : 4f;

        float alpha = (hitCooldown > 0f && ((int) (animTime * 18f)) % 2 == 0) ? 0.45f : 1f;

        if (quinnFlyTexture != null) {
            Texture t = quinnFlyTexture;
            int[] tr = quinnFlyTrim != null
                ? quinnFlyTrim
                : new int[] {0, 0, t.getWidth(), t.getHeight()};
            float h = QUINN_FLY_DRAW_H;
            float w = h * ((float) tr[2] / tr[3]);

            w = Math.max(90f, Math.min(200f, w));

            boolean flip = facingRight == FLIP_QUINN_FLY_IMAGE;

            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(t, cx - w / 2f, player.y - 18f + bob, w / 2f, h / 2f, w, h,
                1f, 1f, tilt, tr[0], tr[1], tr[2], tr[3], flip, false);
            batch.setColor(Color.WHITE);
            return;
        }

        // qw.png가 없을 때: 발러가 퀸을 붙잡고 날아갑니다.
        Texture q = championSprites[QUINN];

        if (q != null) {
            int[] tr = quinnTrim != null
                ? quinnTrim
                : new int[] {0, 0, q.getWidth(), q.getHeight()};
            float qw = Math.max(30f, Math.min(QUINN_DRAW_H * ((float) tr[2] / tr[3]), 130f));

            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(q, cx - qw / 2f, player.y - 3f + bob, qw / 2f, 0f, qw, QUINN_DRAW_H,
                1f, 1f, tilt, tr[0], tr[1], tr[2], tr[3], !facingRight, false);
        }

        if (valorTexture != null) {
            Texture t = valorTexture;
            float h = 60f;
            float w = Math.max(60f, Math.min(150f, h * ((float) t.getWidth() / t.getHeight())));
            float flap = (float) Math.sin(animTime * 14f) * 0.1f;

            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(t, cx - w / 2f, player.y + QUINN_DRAW_H - 8f + bob, w / 2f, h / 2f, w, h,
                1f, 1f + flap, tilt, 0, 0, t.getWidth(), t.getHeight(),
                facingRight == FLIP_VALOR_IMAGE, false);
        }

        batch.setColor(Color.WHITE);
    }

    private void drawDebug() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);

        shapes.setColor(Color.LIME);
        for (Rectangle p : platforms) {
            shapes.rect(p.x, p.y, p.width, p.height);
        }

        shapes.setColor(Color.YELLOW);
        shapes.rect(player.x, player.y, player.width, player.height);

        shapes.setColor(Color.RED);
        for (Minion m : minions) {
            shapes.rect(m.rect.x, m.rect.y, m.rect.width, m.rect.height);
        }

        for (int i = 0; i < monsters.size; i++) {
            Rectangle r = monsters.get(i).rect;
            shapes.rect(r.x, r.y, r.width, r.height);
        }

        shapes.setColor(Color.MAGENTA);
        for (PillarTrap t : traps) {
            for (int i = 0; i < 6; i++) {
                shapes.rect(t.cellX[i] + 5f, t.groundTop, CELL - 10f, Math.max(4f, t.height));
            }
        }

        shapes.setColor(Color.SKY);
        shapes.rect(nexus.x, nexus.y, nexus.width, nexus.height);

        shapes.end();
    }

    // =====================================================================
    //  HUD
    // =====================================================================
    private void text(String s, float x, float y, float scale,
                      float r, float g, float b, float a, boolean center) {
        drawText(font, s, x, y, scale, r, g, b, a, center);
    }

    // 한글 문구 (한글 글꼴이 없으면 그리지 않습니다)
    private void textKr(String s, float x, float y, float scale,
                        float r, float g, float b, float a, boolean center) {
        if (krFont == null) return;

        drawText(krFont, s, x, y, scale * KR_SCALE, r, g, b, a, center);
    }

    private void drawText(BitmapFont f, String s, float x, float y, float scale,
                          float r, float g, float b, float a, boolean center) {
        f.getData().setScale(scale);
        layout.setText(f, s);

        float px = center ? x - layout.width / 2f : x;

        f.setColor(0f, 0f, 0f, a * 0.75f);
        f.draw(batch, s, px + 1.3f, y - 1.3f);
        f.setColor(r, g, b, a);
        f.draw(batch, s, px, y);

        f.getData().setScale(1f);
    }

    private void textRight(String s, float x, float y, float scale,
                           float r, float g, float b, float a) {
        font.getData().setScale(scale);
        layout.setText(font, s);
        float w = layout.width;
        font.getData().setScale(1f);

        text(s, x - w, y, scale, r, g, b, a, false);
    }

    // 금색 테두리 패널
    private void plate(float x, float y, float w, float h) {
        shapes.setColor(0.62f, 0.5f, 0.28f, 0.95f);
        shapes.rect(x - 2f, y - 2f, w + 4f, h + 4f);

        tmpA.set(0.03f, 0.05f, 0.09f, 0.92f);
        tmpB.set(0.09f, 0.12f, 0.20f, 0.92f);
        gradientRect(x, y, w, h, tmpA, tmpB);

        shapes.setColor(0.35f, 0.42f, 0.6f, 0.5f);
        shapes.rect(x, y + h - 1.5f, w, 1f);

        // 양쪽 끝 장식
        shapes.setColor(0.85f, 0.7f, 0.4f, 1f);
        shapes.triangle(x - 2f, y + h / 2f - 5f, x - 2f, y + h / 2f + 5f, x - 9f, y + h / 2f);
        shapes.triangle(x + w + 2f, y + h / 2f - 5f, x + w + 2f, y + h / 2f + 5f, x + w + 9f, y + h / 2f);
    }

    private String championName() {
        if (champion == ZAC) return "ZAC";
        if (champion == OLAF) return "OLAF";
        if (champion == QUINN) return "QUINN";
        return "LEE SIN";
    }

    private void renderHUD() {
        float L = camera.position.x - W / 2f;
        float R = camera.position.x + W / 2f;
        float T = camera.position.y + H / 2f;
        float B = camera.position.y - H / 2f;
        float cx = camera.position.x;

        float pcx = L + 54f;
        float pcy = T - 54f;

        float barX = L + 104f;
        float barW = 230f;

        int hpMax = maxHits();
        int hpLeft = Math.max(0, hpMax - playerHits);
        float ult = Math.min(1f, cs / 10f);
        float pulse = 0.5f + 0.5f * (float) Math.sin(animTime * 6f);

        beginShapes();

        // ----- 초상화 테두리 -----
        shapes.setColor(0f, 0f, 0f, 0.35f);
        shapes.circle(pcx + 2f, pcy - 3f, 44f, 40);
        shapes.setColor(0.55f, 0.42f, 0.22f, 1f);
        shapes.circle(pcx, pcy, 43f, 40);
        shapes.setColor(0.92f, 0.78f, 0.46f, 1f);
        shapes.circle(pcx, pcy, 40f, 40);
        shapes.setColor(0.06f, 0.09f, 0.15f, 1f);
        shapes.circle(pcx, pcy, 36f, 40);

        // ----- 체력/궁극기 패널 -----
        plate(barX - 8f, T - 78f, barW + 22f, 54f);

        float segGap = 3f;
        float segW = (barW - segGap * (hpMax - 1)) / hpMax;

        for (int i = 0; i < hpMax; i++) {
            float sx = barX + i * (segW + segGap);

            if (i < hpLeft) {
                tmpA.set(0.62f, 0.06f, 0.08f, 1f);
                tmpB.set(0.98f, 0.30f, 0.30f, 1f);
                gradientRect(sx, T - 48f, segW, 13f, tmpA, tmpB);
                shapes.setColor(1f, 0.75f, 0.75f, 0.5f);
                shapes.rect(sx, T - 38f, segW, 1.5f);
            } else {
                shapes.setColor(0.2f, 0.06f, 0.07f, 1f);
                shapes.rect(sx, T - 48f, segW, 13f);
            }
        }

        shapes.setColor(0.05f, 0.08f, 0.16f, 1f);
        shapes.rect(barX, T - 66f, barW, 9f);

        if (ult >= 1f) {
            tmpA.set(0.2f, 0.55f + 0.2f * pulse, 1f, 1f);
            tmpB.set(0.6f, 0.95f, 1f, 1f);
        } else {
            tmpA.set(0.10f, 0.30f, 0.80f, 1f);
            tmpB.set(0.30f, 0.65f, 1f, 1f);
        }

        gradientRect(barX, T - 66f, barW * ult, 9f, tmpA, tmpB);

        // ----- 오른쪽 위: 라운드 / CS -----
        plate(R - 446f, T - 50f, 206f, 32f);
        plate(R - 212f, T - 50f, 190f, 32f);

        shapes.setColor(1f, 0.82f, 0.3f, 1f);
        shapes.circle(R - 192f, T - 34f, 8f, 20);
        shapes.setColor(1f, 0.95f, 0.6f, 1f);
        shapes.circle(R - 194f, T - 32f, 3f, 10);

        shapes.setColor(0.4f, 0.75f, 1f, 1f);
        shapes.triangle(R - 104f, T - 34f, R - 96f, T - 34f, R - 100f, T - 24f);
        shapes.triangle(R - 104f, T - 34f, R - 96f, T - 34f, R - 100f, T - 44f);

        // ----- 아래: 진행도 -----
        float pw = 300f;
        float px0 = cx - pw / 2f;
        float py0 = B + 22f;

        shapes.setColor(0f, 0f, 0f, 0.5f);
        shapes.rect(px0 - 4f, py0 - 4f, pw + 8f, 12f);

        float goal = Math.max(1f, nexus.x);
        float prog = Math.max(0f, Math.min(1f, player.x / goal));

        tmpA.set(0.85f, 0.65f, 0.25f, 1f);
        tmpB.set(1f, 0.88f, 0.5f, 1f);
        shapes.rect(px0, py0, pw * prog, 4f, tmpA, tmpB, tmpB, tmpA);

        for (Checkpoint c : checkpoints) {
            float tx = px0 + pw * Math.min(1f, c.x / goal);

            if (c.active) {
                shapes.setColor(1f, 0.85f, 0.4f, 1f);
            } else {
                shapes.setColor(0.6f, 0.6f, 0.65f, 1f);
            }

            shapes.rect(tx - 1.5f, py0 - 4f, 3f, 12f);
        }

        for (int i = 0; i < monsters.size; i++) {
            Monster m = monsters.get(i);

            if (!m.alive || m.kind != MON_HERALD) continue;

            float tx = px0 + pw * Math.min(1f, m.rect.x / goal);
            shapes.setColor(0.75f, 0.5f, 1f, 1f);
            shapes.circle(tx, py0 + 2f, 4f, 10);
        }

        shapes.setColor(0.55f, 0.85f, 1f, 1f);
        shapes.rect(px0 + pw - 4f, py0 - 5f, 8f, 14f);

        shapes.setColor(1f, 1f, 1f, 1f);
        shapes.circle(px0 + pw * prog, py0 + 2f, 5f, 12);

        // ----- 알림 띠 -----
        if (bannerTimer > 0f) {
            float a = bannerAlpha();
            tmpA.set(0f, 0f, 0f, 0f);
            tmpB.set(0f, 0f, 0f, 0.5f * a);
            shapes.rect(L, cy() + 40f, W / 2f, 90f, tmpA, tmpB, tmpB, tmpA);
            shapes.rect(cx, cy() + 40f, W / 2f, 90f, tmpB, tmpA, tmpA, tmpB);
        }

        if (toastTimer > 0f) {
            float a = toastAlpha();
            tmpA.set(0f, 0f, 0f, 0f);
            tmpB.set(0f, 0f, 0f, 0.55f * a);
            shapes.rect(L, T - 190f, W / 2f, 62f, tmpA, tmpB, tmpB, tmpA);
            shapes.rect(cx, T - 190f, W / 2f, 62f, tmpB, tmpA, tmpA, tmpB);
        }

        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 초상화
        Texture portrait = portraits[champion];

        if (portrait == null) portrait = championSprites[champion];

        if (portrait != null) {
            batch.draw(portrait, pcx - 25f, pcy - 25f, 50f, 50f);
        }

        text(championName(), barX, T - 14f, 1.05f, 1f, 0.95f, 0.85f, 1f, false);
        textRight("CS " + cs, barX + barW, T - 14f, 1.05f, 1f, 0.85f, 0.4f, 1f);

        // 궁극기 상태
        String status;
        float sr = 0.85f, sg = 0.9f, sb = 1f;

        if (champion == QUINN && isFlying()) {
            status = "VALOR  " + (int) Math.ceil(flightTimer) + "s  -  UP/DOWN: FLY   R: DROP";
            sr = 0.6f; sg = 0.88f; sb = 1f;
        } else if (champion == QUINN
            && (valorState == VALOR_ARRIVING || valorState == VALOR_SPIRAL)) {
            status = "VALOR INCOMING...";
            sr = 0.6f; sg = 0.88f; sb = 1f;
        } else if (champion == 1 && olafActive) {
            status = "RAGNAROK  " + (int) Math.ceil(olafTimer) + "s  -  INVULNERABLE";
            sr = 1f; sg = 0.65f; sb = 0.35f;
        } else if (champion == 0 && zacActive) {
            status = "LET'S BOUNCE  " + (int) Math.ceil(zacTimer) + "s  -  PRESS R TO LEAP";
            sr = 0.6f; sg = 1f; sb = 0.6f;
        } else if (champion == 2 && markActive) {
            status = "PRESS R : RESONATING STRIKE  " + (int) Math.ceil(markTimer) + "s";
            sr = 0.6f; sg = 0.95f; sb = 1f;
        } else if (champion == 2 && sonicWaveActive) {
            status = "SONIC WAVE  ...";
            sr = 0.6f; sg = 0.95f; sb = 1f;
        } else if (champion == 2 && leeUltTimer > 0f) {
            status = leeAimMode
                ? "SONIC WAVE : CLICK TO FIRE  " + (int) Math.ceil(leeUltTimer) + "s"
                : "SONIC WAVE : PRESS R TO AIM  " + (int) Math.ceil(leeUltTimer) + "s";
            sr = 0.6f; sg = 0.95f; sb = 1f;
        } else if (cs >= 10) {
            status = "R  READY";
            sr = 0.6f + 0.4f * pulse; sg = 0.9f; sb = 1f;
        } else {
            status = "R  NEEDS " + (10 - cs) + " CS";
            sr = 0.65f; sg = 0.7f; sb = 0.8f;
        }

        if (riding && krFont != null) {
            textKr("전령 탑승 중  ·  Q: 내리기", barX, T - 84f, 0.95f, 0.85f, 0.7f, 1f, 1f, false);
        } else if (riding) {
            text("RIDING HERALD  -  Q: GET OFF", barX, T - 86f, 0.85f, 0.85f, 0.7f, 1f, 1f, false);
        } else {
            text(status, barX, T - 86f, 0.85f, sr, sg, sb, 1f, false);
        }

        text("ROUND " + round + "  " + roundName, R - 343f, T - 27f, 0.95f,
            0.95f, 0.92f, 0.85f, 1f, true);

        text("" + cs, R - 178f, T - 27f, 1f, 1f, 0.95f, 0.85f, 1f, false);
        text("" + dragonsSlain, R - 86f, T - 27f, 1f, 1f, 0.95f, 0.85f, 1f, false);

        // 용과 전령 이름
        for (int i = 0; i < monsters.size; i++) {
            Monster m = monsters.get(i);

            if (!m.alive || m == rideHerald) continue;

            String name = m.kind == MON_WIND_DRAGON ? "WIND DRAGON"
                : (m.kind == MON_EARTH_DRAGON ? "EARTH DRAGON" : "RIFT HERALD");

            float nx = m.rect.x + m.rect.width / 2f;
            float ny = m.rect.y + m.rect.height * 1.12f + 44f;

            text(name, nx, ny, 0.8f, 1f, 0.92f, 0.75f, 1f, true);
        }

        text("<- ->  MOVE     SPACE  JUMP     Q  ATTACK     R  ULTIMATE     P  PAUSE     M  SOUND     F1  HITBOX"
                + (champion == QUINN ? "     (FLYING: UP / DOWN)" : ""),
            L + 16f, B + 50f, 0.72f, 0.85f, 0.88f, 0.95f, 0.55f, false);

        if (bannerTimer > 0f) {
            float a = bannerAlpha();

            text("ROUND " + round, cx, cy() + 112f, 2.4f, 1f, 0.88f, 0.55f, a, true);
            text(roundName, cx, cy() + 66f, 1.3f, 0.9f, 0.92f, 1f, a, true);
        }

        if (toastTimer > 0f) {
            float a = toastAlpha();

            if (toastKorean) {
                textKr(toastTitle, cx, T - 134f, 1.6f, toastColor.r, toastColor.g, toastColor.b, a, true);
                textKr(toastSub, cx, T - 163f, 0.95f, 1f, 1f, 1f, a, true);
            } else {
                text(toastTitle, cx, T - 136f, 1.6f, toastColor.r, toastColor.g, toastColor.b, a, true);
                text(toastSub, cx, T - 164f, 0.95f, 1f, 1f, 1f, a, true);
            }
        }

        batch.end();
    }

    private float cy() {
        return camera.position.y;
    }

    private float bannerAlpha() {
        float a = Math.min(1f, bannerTimer / 0.7f);
        float fadeIn = Math.min(1f, (3.2f - bannerTimer) / 0.35f);
        return Math.max(0f, Math.min(a, fadeIn));
    }

    private float toastAlpha() {
        float a = Math.min(1f, toastTimer / 0.5f);
        float fadeIn = Math.min(1f, (3f - toastTimer) / 0.25f);
        return Math.max(0f, Math.min(a, fadeIn));
    }

    private void handlePauseInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)
            || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = false;
            updateMusicVolume();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            startRound(1);
        }
    }

    private void renderPauseScreen() {
        beginShapes();
        shapes.setColor(0f, 0f, 0f, 0.55f);
        shapes.rect(viewLeft(), viewBottom(), W, H);
        plate(camera.position.x - 120f, camera.position.y - 60f, 240f, 120f);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float x = camera.position.x;
        float y = camera.position.y;

        text("PAUSED", x, y + 38f, 1.6f, 1f, 0.88f, 0.55f, 1f, true);
        text("P / ESC : RESUME", x, y, 1f, 1f, 1f, 1f, 1f, true);
        text("T : RESTART", x, y - 26f, 1f, 1f, 1f, 1f, 1f, true);

        batch.end();
    }

    private void handleClearInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            champion = -1;
            selectingChampion = true;
            cleared = false;
            paused = false;
            // 챔피언 선택 화면에서도 배경음악은 계속 이어집니다.
            playMusic(gameMusic);
        }
    }

    private void renderClearScreen() {
        ScreenUtils.clear(
            0.02f, 0.06f, 0.1f, 1f
        );

        camera.position.set(W / 2f, H / 2f, 0);
        camera.update();

        beginShapes();
        plate(W / 2f - 200f, H / 2f - 70f, 400f, 150f);
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        text("VICTORY", W / 2f, H / 2f + 52f, 2.4f, 1f, 0.84f, 0.4f, 1f, true);
        text("All rounds cleared!", W / 2f, H / 2f, 1.1f, 1f, 1f, 1f, 1f, true);
        text("Press R to play again", W / 2f, H / 2f - 32f, 1f, 0.8f, 0.85f, 0.95f, 1f, true);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (font != null) font.dispose();
        if (krFont != null) krFont.dispose();

        disposeTexture(selectionBackground);
        disposeTexture(blockTexture);

        for (Texture t : groundTextures) {
            disposeTexture(t);
        }

        for (Texture t : minionTextures) {
            disposeTexture(t);
        }

        disposeTexture(nexusTexture);
        disposeTexture(flagTexture);
        disposeTexture(baronTexture);
        disposeTexture(heraldTexture);
        disposeTexture(earthDragonTexture);
        disposeTexture(windDragonTexture);
        disposeTexture(quinnFlyTexture);
        disposeTexture(valorTexture);

        for (Texture t : portraits) {
            disposeTexture(t);
        }

        for (Texture t : championSprites) {
            disposeTexture(t);
        }

        for (Texture t : backgrounds) {
            disposeTexture(t);
        }

        if (acceptMusic != null) {
            acceptMusic.dispose();
        }

        for (Sound s : sounds.values()) {
            s.dispose();
        }

        if (gameMusic != null) {
            gameMusic.dispose();
        }

        if (victoryMusic != null) {
            victoryMusic.dispose();
        }
    }

    private void disposeTexture(Texture texture) {
        if (texture != null) {
            texture.dispose();
        }
    }
}
