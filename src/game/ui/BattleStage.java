package game.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Draws the player and monster in a Pokémon-style diagonal camera: the player is seen
 * from behind, up close in the bottom-left foreground; the monster faces the viewer,
 * smaller and further away in the top-right background. Each stands on its own platform.
 */
public class BattleStage extends JComponent {
    private static final int LUNGE = 40;
    private static final Color PLAYER_PLATFORM = new Color(50, 68, 48);
    private static final Color MONSTER_PLATFORM = new Color(54, 58, 68);
    private static final double PLAYER_SCALE = 1.15;
    private static final double MONSTER_SCALE = 0.72;

    private static final long DAMAGE_TEXT_DURATION = 750;
    private static final long LEVEL_UP_DURATION = 2400;
    private static final int LEVEL_UP_STREAKS = 14;
    private static final int LEVEL_UP_TWINKLES = 12;
    private static final String LEVEL_UP_TEXT = "LEVEL UP!";
    private static final long LETTER_STAGGER = 55;   // ms between each letter dropping in
    private static final long LETTER_DROP = 320;     // ms for one letter's drop + bounce
    private static final long COIN_FLIGHT = 560;
    private static final long REWARD_LABEL_DELAY = COIN_FLIGHT;   // pop up as the first coin lands
    private static final long REWARD_LABEL_DURATION = 1400;
    private static final Color COIN_LIGHT = new Color(255, 236, 130);
    private static final Color COIN_DARK = new Color(214, 150, 30);
    private static final Color COIN_RIM = new Color(120, 72, 10);
    private static final Color EXP_BADGE = new Color(70, 150, 230);
    private static final Color EXP_TEXT = new Color(150, 210, 255);

    // ---------- dungeon art: backdrops and monster sprites (anything without art falls back to the vector drawings) ----------
    /** Backdrop per dungeon index, in World.buildDungeons() order. */
    private static final String[] BACKDROP_PATHS = {"images/dungeon1/bg.png"};
    private static final BufferedImage[] BACKDROPS = new BufferedImage[BACKDROP_PATHS.length];
    private static final float BACKDROP_FOCUS_Y = 0.62f;     // which band of a tall backdrop survives the crop (0 = top)

    /** A bitmap monster, sized as a fraction of the far platform's ground height and optionally mirrored to face the player. */
    private static final class MonsterSprite {
        final BufferedImage image;
        final float heightFrac;
        final boolean flip;
        BufferedImage cached;      // downscaled copy for the current stage size
        int cachedH;

        MonsterSprite(BufferedImage image, float heightFrac, boolean flip) {
            this.image = image;
            this.heightFrac = heightFrac;
            this.flip = flip;
        }
    }

    /** Keyed by exact monster name, so e.g. the boss wolf doesn't also replace other dungeons' wolves. */
    private static final java.util.Map<String, MonsterSprite> MONSTER_SPRITES = new java.util.HashMap<>();
    static {
        addSprite("들쥐", "images/dungeon1/rat.png", 0.56f, true);
        addSprite("멧돼지", "images/dungeon1/boar.png", 0.72f, false);
        addSprite("들개", "images/dungeon1/dog.png", 0.70f, false);
        addSprite("너구리", "images/dungeon1/raccoon.png", 0.78f, false);
        addSprite("초원의 우두머리 늑대", "images/dungeon1/boss_wolf.png", 0.92f, false);
    }

    private static void addSprite(String name, String path, float heightFrac, boolean flip) {
        BufferedImage img = loadImage(path);
        if (img != null) MONSTER_SPRITES.put(name, new MonsterSprite(img, heightFrac, flip));
    }

    private BufferedImage backdrop;
    private final BattleHud hud = new BattleHud(this::repaint);
    /** On-screen height of the current monster sprite (0 for vector monsters); set every paint. */
    private int monsterSpritePx = 0;

    private String monsterName = "";
    private int playerOffsetX = 0;
    private int monsterOffsetX = 0;
    private float playerHitFlash = 0f;
    private float monsterHitFlash = 0f;
    private long startTime = System.currentTimeMillis();
    private Timer idleTimer;
    private final List<DamageText> damageTexts = new ArrayList<>();
    private LevelUpFx levelUpFx;
    private final List<RewardFx> rewardFxs = new ArrayList<>();
    private final List<SlashFx> slashFxs = new ArrayList<>();

    private static final long SLASH_DURATION = 420;
    private static final int SLASH_SPARKS = 14;

    // ---------- impact feedback: screen shake, hit-stop, knockback, flashes ----------
    // Shake is driven by a decaying "trauma" value (0..1); the visible shake is trauma^2, so light
    // hits barely move the stage while crits and kills really punch. It only moves the drawing.
    private static final float TRAUMA_DECAY = 1.6f;          // trauma lost per second
    private static final float SHAKE_MAX_X = 14f;
    private static final float SHAKE_MAX_Y = 10f;
    private static final double SHAKE_MAX_ROLL = 0.02;       // radians
    private static final float HIT_FLASH_TIME = 0.16f;       // seconds for a hit tint to fade out
    private static final float KNOCK_TIME = 0.28f;           // seconds for a knocked-back body to settle
    private static final int KNOCK_DIST = 14;
    private static final float SQUASH = 0.14f;               // how much a struck body widens/flattens
    private static final float SCREEN_FLASH_TIME = 0.12f;
    private static final long DEATH_DURATION = 520;
    private static final long DAMAGE_POP = 160;              // ms for a damage number's overshoot pop
    private static final Color PLAYER_HURT_TINT = new Color(255, 70, 60);

    private float trauma = 0f;
    private float shakeClock = 0f;
    private float playerKnock = 0f;
    private float monsterKnock = 0f;
    private float screenFlash = 0f;
    private long lastFrameNanos = 0;
    private long monsterDeathStart = 0;
    /** True only while an attack's onImpact callback runs, so showDamage knows the number is a real hit. */
    private boolean inImpact = false;
    /** Hit-stop requested by the hits of the current impact; the lunge freezes this long before returning. */
    private int pendingHitStopMs = 0;

    /** A crescent sword trail swept across the monster, plus sparks at the point of impact. */
    private static final class SlashFx {
        final long start = System.currentTimeMillis();
        final Color color;
        final boolean mirrored;        // alternate hits slash the other way
        final double[] sparkAngles = new double[SLASH_SPARKS];
        final double[] sparkSpeeds = new double[SLASH_SPARKS];

        SlashFx(Color color, boolean mirrored) {
            this.color = color;
            this.mirrored = mirrored;
            java.util.Random r = new java.util.Random();
            for (int i = 0; i < SLASH_SPARKS; i++) {
                sparkAngles[i] = r.nextDouble() * Math.PI * 2;
                sparkSpeeds[i] = 0.6 + r.nextDouble() * 0.6;
            }
        }
    }

    /** One coin flying from the defeated monster into the player, on its own arc. */
    private static final class Coin {
        final long delay;
        final double spreadX;
        final double arcHeight;
        final double spinPhase;

        Coin(long delay, double spreadX, double arcHeight, double spinPhase) {
            this.delay = delay;
            this.spreadX = spreadX;
            this.arcHeight = arcHeight;
            this.spinPhase = spinPhase;
        }
    }

    /** Kill reward: coins fly from the monster to the player, then "+gold" / "+EXP" pop up beside the player. */
    private static final class RewardFx {
        final long start = System.currentTimeMillis();
        final int exp;
        final int gold;
        final Coin[] coins;

        RewardFx(int exp, int gold) {
            this.exp = exp;
            this.gold = gold;
            java.util.Random r = new java.util.Random();
            int count = gold > 0 ? Math.min(10, 3 + gold / 15) : 0;
            coins = new Coin[count];
            for (int i = 0; i < count; i++) {
                coins[i] = new Coin(i * 45L + r.nextInt(30), (r.nextDouble() - 0.5) * 50,
                        50 + r.nextDouble() * 50, r.nextDouble() * Math.PI * 2);
            }
        }
    }

    private static final class DamageText {
        final String text;
        final boolean onMonster;
        final boolean crit;
        final long start = System.currentTimeMillis();
        final int jitterX;     // small sideways offset so multi-hit numbers don't stack on one spot

        DamageText(String text, boolean onMonster, boolean crit) {
            this.text = text;
            this.onMonster = onMonster;
            this.crit = crit;
            this.jitterX = new java.util.Random().nextInt(29) - 14;
        }
    }

    /** A thin golden light streak shooting up from the player's feet. */
    private static final class Streak {
        final double xOff;     // horizontal offset from the player's center, as a fraction of body width
        final long delay;      // ms after the effect starts before this streak launches
        final double speed;    // px per ms
        final int length;

        Streak(double xOff, long delay, double speed, int length) {
            this.xOff = xOff;
            this.delay = delay;
            this.speed = speed;
            this.length = length;
        }
    }

    /** A four-pointed twinkle star that drifts up around the player's body. */
    private static final class Twinkle {
        final double xOff;     // fraction of body width from center
        final double yFrac;    // 0 = feet, 1 = head
        final double phase;
        final float size;

        Twinkle(double xOff, double yFrac, double phase, float size) {
            this.xOff = xOff;
            this.yFrac = yFrac;
            this.phase = phase;
            this.size = size;
        }
    }

    /** Old-MapleStory-style level-up: light pillar, rising streaks, twinkles and bouncing "LEVEL UP!" letters. Randomness is rolled once so playback is stable. */
    private static final class LevelUpFx {
        final long start = System.currentTimeMillis();
        final Streak[] streaks = new Streak[LEVEL_UP_STREAKS];
        final Twinkle[] twinkles = new Twinkle[LEVEL_UP_TWINKLES];

        LevelUpFx() {
            java.util.Random r = new java.util.Random();
            for (int i = 0; i < streaks.length; i++) {
                streaks[i] = new Streak((r.nextDouble() - 0.5) * 1.1, r.nextInt(900),
                        0.35 + r.nextDouble() * 0.3, 28 + r.nextInt(40));
            }
            for (int i = 0; i < twinkles.length; i++) {
                twinkles[i] = new Twinkle((r.nextDouble() - 0.5) * 1.5, r.nextDouble(),
                        r.nextDouble() * Math.PI * 2, 5 + r.nextFloat() * 6);
            }
        }
    }

    public BattleStage() {
        setOpaque(false);
        setPreferredSize(new Dimension(600, 220));
    }

    public void setMonsterName(String name) {
        this.monsterName = name;
        playerOffsetX = 0;
        monsterOffsetX = 0;
        playerHitFlash = 0f;
        monsterHitFlash = 0f;
        playerKnock = 0f;
        monsterKnock = 0f;
        monsterDeathStart = 0;
        repaint();
    }

    /** The status boxes painted over this stage. */
    public BattleHud getHud() {
        return hud;
    }

    /** Picks the battle backdrop for a dungeon (World.buildDungeons() index); dungeons without one keep the plain stage. */
    public void setDungeon(int index) {
        if (index >= 0 && index < BACKDROP_PATHS.length) {
            if (BACKDROPS[index] == null) BACKDROPS[index] = loadImage(BACKDROP_PATHS[index]);
            backdrop = BACKDROPS[index];
        } else {
            backdrop = null;
        }
        repaint();
    }

    public void startIdle() {
        if (idleTimer != null) idleTimer.stop();
        startTime = System.currentTimeMillis();
        idleTimer = new Timer(60, e -> repaint());
        idleTimer.start();
    }

    public void stopIdle() {
        if (idleTimer != null) {
            idleTimer.stop();
            idleTimer = null;
        }
    }

    /** Spawns a floating damage number over the player (onMonster=false) or monster (onMonster=true). */
    public void showDamage(int amount, boolean onMonster, boolean crit) {
        damageTexts.add(new DamageText(String.valueOf(amount), onMonster, crit));
        if (inImpact) {
            // Feedback scaled to the hit: a normal hit nudges, a crit punches, getting hit stings.
            if (onMonster) {
                addTrauma(crit ? 0.6f : 0.35f);
                pendingHitStopMs = Math.max(pendingHitStopMs, crit ? 110 : 50);
                if (crit) screenFlash = 1f;
            } else {
                addTrauma(0.5f);
                pendingHitStopMs = Math.max(pendingHitStopMs, 70);
            }
        }
        repaint();
    }

    /** Kill effect: the monster flashes white, then stretches thin and fades away. */
    public void playMonsterDeath() {
        monsterDeathStart = System.currentTimeMillis();
        monsterHitFlash = 1f;
        addTrauma(0.45f);
        repaint();
    }

    private void addTrauma(float amount) {
        trauma = Math.min(1f, trauma + amount);   // hits add up rather than reset
    }

    /** Plays a skill's hit effect on the monster; hitIndex alternates the direction for multi-hit skills. */
    public void playSkillFx(game.model.Skill.Fx fx, int rgb, int hitIndex) {
        if (fx == game.model.Skill.Fx.SLASH) {
            slashFxs.add(new SlashFx(new Color(rgb), hitIndex % 2 == 1));
            repaint();
        }
    }

    /** Plays the kill reward: coins fly into the player, then gained gold and EXP pop up beside them. */
    public void showRewards(int exp, int gold) {
        rewardFxs.add(new RewardFx(exp, gold));
        repaint();
    }

    /** Plays the old-MapleStory-style level-up effect over the player. */
    public void playLevelUp() {
        levelUpFx = new LevelUpFx();
        repaint();
    }

    /** Animates a lunge attack. onImpact fires at the peak of the lunge (apply damage there); onDone fires after the return. */
    public void animateAttack(boolean playerAttacking, Runnable onImpact, Runnable onDone) {
        final int frames = 12;
        final int half = frames / 2;
        final int[] frame = {0};
        final Timer[] holder = new Timer[1];
        Timer timer = new Timer(18, null);
        timer.addActionListener(e -> {
            frame[0]++;
            int f = frame[0];
            // Wind up slightly, accelerate into the hit, then ease back to rest.
            float lunge = f <= half
                    ? easeInBack(f / (float) half)
                    : 1 - easeOutCubic((f - half) / (float) half);
            int off = Math.round(lunge * LUNGE);
            if (playerAttacking) playerOffsetX = off;
            else monsterOffsetX = off;

            if (f == half) {
                if (playerAttacking) {
                    monsterHitFlash = 1f;
                    monsterKnock = 1f;
                } else {
                    playerHitFlash = 1f;
                    playerKnock = 1f;
                }
                pendingHitStopMs = 0;
                inImpact = true;
                try {
                    if (onImpact != null) onImpact.run();
                } finally {
                    inImpact = false;
                }
                // Hit-stop: hold the lunge at the point of impact for a beat before it returns.
                if (pendingHitStopMs > 0) {
                    holder[0].stop();
                    Timer resume = new Timer(pendingHitStopMs, ev -> holder[0].start());
                    resume.setRepeats(false);
                    resume.start();
                }
            }
            repaint();
            if (f >= frames) {
                holder[0].stop();
                playerOffsetX = 0;
                monsterOffsetX = 0;
                if (onDone != null) onDone.run();
            }
        });
        holder[0] = timer;
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        // Time-based decay so feedback lasts the same no matter how often we repaint.
        long nowNanos = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0f : Math.min(0.05f, (nowNanos - lastFrameNanos) / 1e9f);
        lastFrameNanos = nowNanos;
        trauma = Math.max(0f, trauma - TRAUMA_DECAY * dt);
        playerHitFlash = Math.max(0f, playerHitFlash - dt / HIT_FLASH_TIME);
        monsterHitFlash = Math.max(0f, monsterHitFlash - dt / HIT_FLASH_TIME);
        playerKnock = Math.max(0f, playerKnock - dt / KNOCK_TIME);
        monsterKnock = Math.max(0f, monsterKnock - dt / KNOCK_TIME);
        screenFlash = Math.max(0f, screenFlash - dt / SCREEN_FLASH_TIME);
        shakeClock += dt * 30f;

        if (backdrop != null) drawBackdrop(g2, w, h);

        if (trauma > 0) {
            // Smooth sine-based shake (not per-frame random, which buzzes like static).
            float s = trauma * trauma;
            g2.translate(SHAKE_MAX_X * s * Math.sin(shakeClock * 1.7), SHAKE_MAX_Y * s * Math.sin(shakeClock * 2.3));
            g2.rotate(SHAKE_MAX_ROLL * s * Math.sin(shakeClock * 1.1), w / 2.0, h / 2.0);
        }

        // Player near the bottom-left, monster on the far ground at the right; both follow the stage width.
        int playerGroundY = h - 26;
        int playerX = Math.max(100, Math.round(w * 0.2f));
        int monsterGroundY = (int) Math.round(h * 0.42);
        int monsterX = Math.max(playerX + 180, w - Math.max(95, Math.round(w * 0.2f)));

        MonsterSprite sprite = MONSTER_SPRITES.get(monsterName);
        monsterSpritePx = sprite == null ? 0 : Math.round(sprite.heightFrac * monsterGroundY);

        if (backdrop != null) {
            // The painted scenery already has ground; just a soft shadow patch where each one stands.
            drawGroundShadow(g2, playerX, playerGroundY, 190, 34);
            drawGroundShadow(g2, monsterX, monsterGroundY, sprite == null ? 120 : Math.max(120, monsterSpritePx + 30), 22);
        } else {
            drawPlatform(g2, playerX, playerGroundY, 200, 40, PLAYER_PLATFORM);
            drawPlatform(g2, monsterX, monsterGroundY, 130, 26, MONSTER_PLATFORM);
        }

        double t = (System.currentTimeMillis() - startTime) / 1000.0;
        int bob = (int) Math.round(Math.sin(t * 2.4) * 3);

        // Lunges travel along the diagonal between the two platforms; knockback pushes the
        // struck body back the other way and eases it home (knock^2 = fast push, soft settle).
        float pk = playerKnock * playerKnock;
        float mk = monsterKnock * monsterKnock;
        int px = playerX + playerOffsetX - Math.round(pk * KNOCK_DIST);
        int py = playerGroundY + bob - Math.round(playerOffsetX * 0.4f) + Math.round(pk * KNOCK_DIST * 0.4f);
        int mx = monsterX - monsterOffsetX + Math.round(mk * KNOCK_DIST);
        int my = monsterGroundY - bob + Math.round(monsterOffsetX * 0.45f) - Math.round(mk * KNOCK_DIST * 0.4f);

        drawBody(g2, px, py, PLAYER_SCALE * (1 + SQUASH * pk), PLAYER_SCALE * (1 - SQUASH * pk),
                playerBox(), gp -> drawPlayer(gp, 0, 0), PLAYER_HURT_TINT, playerHitFlash * 0.7f, 1f);

        if (monsterName != null && !monsterName.isEmpty()) {
            long deathElapsed = monsterDeathStart == 0 ? 0 : System.currentTimeMillis() - monsterDeathStart;
            if (deathElapsed < DEATH_DURATION) {
                double sx = MONSTER_SCALE * (1 + SQUASH * mk);
                double sy = MONSTER_SCALE * (1 - SQUASH * mk);
                float tint = monsterHitFlash;
                float alpha = 1f;
                if (monsterDeathStart != 0) {
                    // Hold a white flash, then stretch up thin and fade out.
                    float d = deathElapsed / (float) DEATH_DURATION;
                    float vanish = Math.max(0f, (d - 0.2f) / 0.8f);
                    tint = Math.max(tint, d < 0.2f ? 1f : 1f - vanish);
                    sx *= 1 - 0.7f * vanish;
                    sy *= 1 + 0.35f * vanish;
                    alpha = 1f - vanish * vanish;
                    repaint();
                }
                drawBody(g2, mx, my, sx, sy, monsterBox(sprite), gm -> drawMonster(gm, monsterName, 0, 0),
                        Color.WHITE, tint, alpha);
            }
        }

        drawSlashes(g2, mx, monsterGroundY);
        drawDamageTexts(g2, px, playerGroundY, mx, monsterGroundY);
        drawRewards(g2, px, playerGroundY, mx, monsterGroundY);
        drawLevelUp(g2, px, playerGroundY);

        if (screenFlash > 0) {
            g2.setColor(new Color(255, 255, 240, Math.round(90 * screenFlash)));
            g2.fillRect(-40, -40, w + 80, h + 80);
        }

        g2.dispose();

        // Status boxes stay put while the scene shakes underneath them.
        Graphics2D gh = (Graphics2D) g.create();
        hud.paint(gh, w, h);
        gh.dispose();

        if (trauma > 0 || playerHitFlash > 0 || monsterHitFlash > 0
                || playerKnock > 0 || monsterKnock > 0 || screenFlash > 0) {
            repaint();
        } else {
            lastFrameNanos = 0;   // idle: don't count the gap before the next hit as elapsed time
        }
    }

    /** Vector monster drawing extent in its own unscaled coordinates (ground at y=0), covering wings, horns and weapons. */
    private static final Rectangle MONSTER_BOX = new Rectangle(-72, -120, 144, 132);

    /** Drawing extent of the current monster in its unscaled coordinates: the vector box, or the sprite's own size. */
    private Rectangle monsterBox(MonsterSprite sprite) {
        if (sprite == null || monsterSpritePx <= 0) return MONSTER_BOX;
        int uh = (int) Math.ceil(monsterSpritePx / MONSTER_SCALE);
        int halfW = (int) Math.ceil(uh * sprite.image.getWidth() / (double) sprite.image.getHeight() / 2) + 6;
        return new Rectangle(-halfW, -uh - 6, halfW * 2, uh + 18);
    }

    /** Height above the monster's ground line where hit effects should land: mid-body for sprites. */
    private int monsterHitHeight() {
        return monsterSpritePx > 0 ? Math.round(monsterSpritePx * 0.45f) : 24;
    }

    /** Scales the backdrop to cover the stage (cropping the excess) inside rounded corners. */
    private void drawBackdrop(Graphics2D g2, int w, int h) {
        double s = Math.max(w / (double) backdrop.getWidth(), h / (double) backdrop.getHeight());
        int dw = (int) Math.ceil(backdrop.getWidth() * s);
        int dh = (int) Math.ceil(backdrop.getHeight() * s);
        int dx = (w - dw) / 2;
        int dy = Math.round((h - dh) * BACKDROP_FOCUS_Y);
        Graphics2D g = (Graphics2D) g2.create();
        g.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 14, 14));
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(backdrop, dx, dy, dw, dh, null);
        g.dispose();
    }

    private static void drawGroundShadow(Graphics2D g2, int cx, int groundY, int width, int height) {
        g2.setColor(new Color(20, 30, 10, 40));
        g2.fillOval(cx - width / 2, groundY - height / 2, width, height);
        g2.setColor(new Color(20, 30, 10, 45));
        g2.fillOval(cx - width * 3 / 8, groundY - height * 3 / 8, width * 3 / 4, height * 3 / 4);
    }

    /**
     * The sprite downscaled to the given on-screen height (and mirrored if needed), cached until the stage
     * resizes. Halving step by step keeps the big source art crisp instead of aliasing in one bilinear jump.
     */
    private static BufferedImage spriteAt(MonsterSprite s, int targetH) {
        if (s.cached != null && s.cachedH == targetH) return s.cached;
        BufferedImage cur = s.image;
        int targetW = Math.max(1, (int) Math.round(targetH * s.image.getWidth() / (double) s.image.getHeight()));
        while (cur.getHeight() / 2 >= targetH) {
            cur = resize(cur, cur.getWidth() / 2, cur.getHeight() / 2, false);
        }
        s.cached = resize(cur, targetW, targetH, s.flip);
        s.cachedH = targetH;
        return s.cached;
    }

    private static BufferedImage resize(BufferedImage src, int w, int h, boolean flip) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (flip) g.drawImage(src, w, 0, -w, h, null);
        else g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static Rectangle playerBox() {
        int halfW = Math.max(30, (int) Math.ceil(playerBodyW() / PLAYER_SCALE / 2) + 4);
        return new Rectangle(-halfW, -(int) PLAYER_SPRITE_HEIGHT - 6, halfW * 2, (int) PLAYER_SPRITE_HEIGHT + 18);
    }

    /**
     * Draws a body anchored at its feet (x, groundY) with independent x/y scale (for squash & stretch).
     * When it needs a color flash or transparency, it's rendered offscreen first so the tint covers only
     * the body's own pixels rather than a blob around it.
     */
    private static void drawBody(Graphics2D g2, int x, int groundY, double sx, double sy, Rectangle box,
                                 Consumer<Graphics2D> painter, Color tint, float tintAmount, float alpha) {
        if (tintAmount <= 0f && alpha >= 1f) {
            Graphics2D g = (Graphics2D) g2.create();
            g.translate(x, groundY);
            g.scale(sx, sy);
            painter.accept(g);
            g.dispose();
            return;
        }
        int iw = Math.max(1, (int) Math.ceil(box.width * sx));
        int ih = Math.max(1, (int) Math.ceil(box.height * sy));
        BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gi = img.createGraphics();
        gi.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gi.scale(sx, sy);
        gi.translate(-box.x, -box.y);
        painter.accept(gi);
        if (tintAmount > 0f) {
            gi.setTransform(new java.awt.geom.AffineTransform());
            gi.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, Math.min(1f, tintAmount)));
            gi.setColor(tint);
            gi.fillRect(0, 0, iw, ih);
        }
        gi.dispose();

        Graphics2D g = (Graphics2D) g2.create();
        if (alpha < 1f) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
        g.drawImage(img, (int) Math.round(x + box.x * sx), (int) Math.round(groundY + box.y * sy), null);
        g.dispose();
    }

    /**
     * Draws the old-MapleStory-style level-up effect centered on the player: a golden light
     * pillar bursting up from the feet, thin light streaks shooting upward, twinkling stars
     * around the body, and "LEVEL UP!" letters dropping in one by one above the head.
     */
    private void drawLevelUp(Graphics2D g2, int playerCx, int playerGroundY) {
        if (levelUpFx == null) return;
        long elapsed = System.currentTimeMillis() - levelUpFx.start;
        if (elapsed >= LEVEL_UP_DURATION) {
            levelUpFx = null;
            return;
        }
        float t = elapsed / (float) LEVEL_UP_DURATION;
        float fadeOut = t > 0.75f ? Math.max(0f, 1f - (t - 0.75f) / 0.25f) : 1f;

        int bodyH = playerBodyH();
        int bodyW = playerBodyW();
        int bodyTop = playerGroundY - bodyH;

        // 1) Light pillar: shoots up to the top of the stage in the first 200ms, then slowly thins out.
        float grow = Math.min(1f, elapsed / 200f);
        float pillarAlpha = (t < 0.5f ? 1f : Math.max(0f, 1f - (t - 0.5f) / 0.35f));
        if (pillarAlpha > 0) {
            int pillarTop = Math.round(playerGroundY - playerGroundY * grow);
            int pillarH = playerGroundY - pillarTop;
            // Narrows over time, like the beam collapsing back into the character.
            float widthFactor = 1f - 0.45f * Math.max(0f, (t - 0.3f) / 0.5f);
            int[] widths = {Math.round(bodyW * 1.1f * widthFactor), Math.round(bodyW * 0.7f * widthFactor),
                    Math.round(bodyW * 0.32f * widthFactor)};
            Color[] cores = {new Color(255, 214, 90), new Color(255, 236, 150), new Color(255, 252, 225)};
            int[] alphas = {70, 90, 150};
            for (int i = 0; i < widths.length; i++) {
                int a = Math.round(alphas[i] * pillarAlpha);
                if (a <= 0 || widths[i] <= 0 || pillarH <= 0) continue;
                Color base = cores[i];
                g2.setPaint(new GradientPaint(0, playerGroundY, new Color(base.getRed(), base.getGreen(), base.getBlue(), a),
                        0, pillarTop, new Color(base.getRed(), base.getGreen(), base.getBlue(), 0)));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(playerCx - widths[i] / 2f, pillarTop,
                        widths[i], pillarH, widths[i], widths[i]));
            }
        }

        // 2) Burst ring at the feet on the first beat.
        if (elapsed < 600) {
            float rp = elapsed / 600f;
            int rx = Math.round(bodyW * 0.4f + rp * bodyW * 0.9f);
            int ry = Math.max(4, rx / 4);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 230, 120, Math.round(220 * (1 - rp))));
            g2.drawOval(playerCx - rx, playerGroundY - ry, rx * 2, ry * 2);
        }

        // 3) Thin light streaks racing upward from the feet.
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Streak s : levelUpFx.streaks) {
            long se = elapsed - s.delay;
            if (se < 0) continue;
            int x = playerCx + (int) Math.round(s.xOff * bodyW);
            int headY = playerGroundY - (int) Math.round(se * s.speed);
            int tailY = headY + s.length;
            if (tailY < 0) continue;
            int a = Math.round(230 * fadeOut);
            if (a <= 0) continue;
            g2.setPaint(new GradientPaint(0, headY, new Color(255, 250, 215, a),
                    0, tailY, new Color(255, 200, 70, 0)));
            g2.drawLine(x, headY, x, Math.min(tailY, playerGroundY));
        }

        // 4) Twinkling four-pointed stars around the body, drifting upward.
        for (Twinkle tw : levelUpFx.twinkles) {
            double twinkle = Math.abs(Math.sin(elapsed / 110.0 + tw.phase));
            float size = (float) (tw.size * (0.35 + 0.65 * twinkle));
            int sx = playerCx + (int) Math.round(tw.xOff * bodyW);
            int sy = playerGroundY - (int) Math.round(tw.yFrac * bodyH) - Math.round(elapsed * 0.03f);
            int a = Math.round(255 * fadeOut * (float) (0.4 + 0.6 * twinkle));
            if (a <= 0) continue;
            g2.setColor(new Color(255, 245, 190, a));
            g2.fill(starShape(sx, sy, size));
        }

        // 5) "LEVEL UP!" letters drop in one by one with a bounce, above the player's head.
        Font font = Theme.dosFont(Font.BOLD, 32f);
        java.awt.font.FontRenderContext frc = g2.getFontRenderContext();
        java.awt.font.GlyphVector whole = font.createGlyphVector(frc, LEVEL_UP_TEXT);
        java.awt.geom.Rectangle2D wb = whole.getVisualBounds();
        float textAlpha = fadeOut;
        // Float up a little while fading out, like the original.
        int lift = t > 0.75f ? Math.round((t - 0.75f) / 0.25f * 18) : 0;
        int baseY = Math.max((int) Math.ceil(wb.getHeight()) + 8, bodyTop - 6) - lift;
        float startX = (float) (playerCx - wb.getWidth() / 2 - wb.getX());

        Graphics2D gt = (Graphics2D) g2.create();
        gt.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
        for (int i = 0; i < LEVEL_UP_TEXT.length(); i++) {
            long le = elapsed - i * LETTER_STAGGER;
            if (le < 0) break;
            float p = Math.min(1f, le / (float) LETTER_DROP);
            float dropOffset = (1f - easeOutBack(p)) * -36f;
            Shape glyph = whole.getGlyphOutline(i, startX, baseY + dropOffset);
            if (glyph.getBounds().isEmpty()) continue;
            java.awt.geom.Rectangle2D gb = glyph.getBounds2D();

            gt.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gt.setColor(new Color(92, 38, 8));
            gt.draw(glyph);
            gt.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gt.setColor(new Color(255, 250, 220));
            gt.draw(glyph);
            gt.setPaint(new GradientPaint(0, (float) gb.getMinY(), new Color(255, 246, 130),
                    0, (float) gb.getMaxY(), new Color(255, 140, 20)));
            gt.fill(glyph);
        }
        gt.dispose();
        repaint();
    }

    /**
     * Sword trail: a crescent that sweeps across the monster in the first third of the effect, then
     * thins and fades, drawn in three layers (soft colored glow, colored blade, white-hot core).
     * Sparks burst from the impact point as the blade passes through.
     */
    private void drawSlashes(Graphics2D g2, int monsterCx, int monsterGroundY) {
        if (slashFxs.isEmpty()) return;
        long now = System.currentTimeMillis();
        int cx = monsterCx, cy = monsterGroundY - monsterHitHeight();
        // Grows with the stage so it still reads as big in fullscreen (1.0 at the default 260px height).
        float k = Math.max(1f, Math.min(1.8f, getHeight() / 260f));
        Iterator<SlashFx> it = slashFxs.iterator();
        while (it.hasNext()) {
            SlashFx fx = it.next();
            long elapsed = now - fx.start;
            if (elapsed >= SLASH_DURATION) {
                it.remove();
                continue;
            }
            float t = elapsed / (float) SLASH_DURATION;
            float reveal = t < 0.35f ? 1 - (float) Math.pow(1 - t / 0.35f, 3) : 1f;   // ease-out sweep
            float fade = t < 0.45f ? 1f : Math.max(0f, 1f - (t - 0.45f) / 0.55f);
            float thickness = 24f * k * (0.35f + 0.65f * fade);
            final float radius = 95f * k;
            Color c = fx.color;

            // Impact flash: a bright burst on the monster right as the blade connects.
            float ft = t / 0.3f;
            if (ft < 1) {
                float fr = (18 + 52 * ft) * k;
                int fa = Math.round(200 * (1 - ft));
                g2.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Float(cx, cy), fr,
                        new float[] {0f, 0.35f, 1f},
                        new Color[] {new Color(255, 255, 240, fa),
                                new Color(c.getRed(), c.getGreen(), c.getBlue(), fa / 2),
                                new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
                g2.fill(new Ellipse2D.Float(cx - fr, cy - fr, fr * 2, fr * 2));
            }

            Graphics2D g = (Graphics2D) g2.create();
            g.translate(cx, cy);
            if (fx.mirrored) g.scale(-1, 1);
            g.rotate(Math.toRadians(32));   // diagonal: top-left to bottom-right
            // Shift so the middle of the arc (its top, at -radius) cuts right through the monster.
            g.translate(0, radius);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(70 * fade)));
            g.fill(crescent(radius, thickness * 2.3f, reveal));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(225 * fade)));
            g.fill(crescent(radius, thickness, reveal));
            g.setColor(new Color(255, 250, 235, Math.round(245 * fade)));
            g.fill(crescent(radius, thickness * 0.4f, reveal));
            g.dispose();

            // Sparks fly out once the blade reaches the middle of the monster.
            float st = (t - 0.12f) / 0.6f;
            if (st > 0 && st < 1) {
                g2.setStroke(new BasicStroke(3f * k, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < SLASH_SPARKS; i++) {
                    double a = fx.sparkAngles[i];
                    double d0 = (12 + st * 75 * fx.sparkSpeeds[i]) * k;
                    double len = (20 * (1 - st) + 5) * k;
                    int alpha = Math.round(230 * (1 - st));
                    g2.setColor(i % 3 == 0 ? new Color(255, 255, 255, alpha)
                            : new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
                    g2.drawLine((int) Math.round(cx + Math.cos(a) * d0), (int) Math.round(cy + Math.sin(a) * d0),
                            (int) Math.round(cx + Math.cos(a) * (d0 + len)), (int) Math.round(cy + Math.sin(a) * (d0 + len)));
                }
            }
        }
        repaint();
    }

    /**
     * An arc band around (0,0) over the upper half-circle, fattest in the middle and pointed at both
     * ends; only the first `reveal` fraction (0..1) is built, so animating it draws the stroke.
     */
    private static Shape crescent(float radius, float maxThickness, float reveal) {
        final int steps = 28;
        double from = Math.toRadians(195), span = Math.toRadians(150);
        double[] ox = new double[steps + 1], oy = new double[steps + 1];
        double[] ix = new double[steps + 1], iy = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            double u = reveal * i / (double) steps;
            double a = from + span * u;
            double half = maxThickness * Math.sin(Math.PI * u) / 2;
            ox[i] = Math.cos(a) * (radius + half);
            oy[i] = Math.sin(a) * (radius + half);
            ix[i] = Math.cos(a) * (radius - half);
            iy[i] = Math.sin(a) * (radius - half);
        }
        Path2D.Double p = new Path2D.Double();
        p.moveTo(ox[0], oy[0]);
        for (int i = 1; i <= steps; i++) p.lineTo(ox[i], oy[i]);
        for (int i = steps; i >= 0; i--) p.lineTo(ix[i], iy[i]);
        p.closePath();
        return p;
    }

    /** Draws flying coins and the "+gold" / "+EXP" pop-ups, expiring each reward once all its parts finish. */
    private void drawRewards(Graphics2D g2, int playerCx, int playerGroundY, int monsterCx, int monsterGroundY) {
        if (rewardFxs.isEmpty()) return;
        int bodyH = playerBodyH();
        int bodyW = playerBodyW();
        long now = System.currentTimeMillis();
        Iterator<RewardFx> it = rewardFxs.iterator();
        while (it.hasNext()) {
            RewardFx fx = it.next();
            long elapsed = now - fx.start;
            if (elapsed >= REWARD_LABEL_DELAY + REWARD_LABEL_DURATION) {
                it.remove();
                continue;
            }

            // Coins pop out of the monster and arc into the player's chest.
            double endX = playerCx + bodyW * 0.15;
            double endY = playerGroundY - bodyH * 0.5;
            double startY = monsterGroundY - monsterHitHeight();
            for (Coin c : fx.coins) {
                long ce = elapsed - c.delay;
                if (ce < 0 || ce >= COIN_FLIGHT) continue;
                double p = ce / (double) COIN_FLIGHT;
                double eased = p * p * (3 - 2 * p);
                double x = (monsterCx + c.spreadX) + (endX - monsterCx - c.spreadX) * eased;
                double y = startY + (endY - startY) * eased - c.arcHeight * 4 * p * (1 - p);
                double spin = Math.abs(Math.cos(ce / 70.0 + c.spinPhase));
                drawCoin(g2, (float) x, (float) y, 7f, (float) Math.max(0.2, spin));
            }

            // "+gold" and "+EXP" rows rise beside the player once the coins have landed.
            long le = elapsed - REWARD_LABEL_DELAY;
            if (le < 0) continue;
            float lp = le / (float) REWARD_LABEL_DURATION;
            float alpha = lp < 0.6f ? 1f : Math.max(0f, 1f - (lp - 0.6f) / 0.4f);
            float pop = Math.min(1f, le / 160f);
            int rise = Math.round(lp * 30);
            int x = playerCx + bodyW / 2 + 8;
            int y = (int) Math.round(playerGroundY - bodyH * 0.62) - rise;

            Graphics2D gl = (Graphics2D) g2.create();
            gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * pop));
            Font valueFont = Theme.dosFont(Font.BOLD, 17f);
            if (fx.gold > 0) {
                drawCoin(gl, x + 8, y - 6, 8f, 1f);
                drawOutlinedText(gl, "+" + fx.gold + "G", valueFont, x + 22, y, new Color(255, 214, 80));
                y += 22;
            }
            if (fx.exp > 0) {
                Font badgeFont = Theme.dosFont(Font.BOLD, 10f);
                FontMetrics bfm = gl.getFontMetrics(badgeFont);
                int bw = bfm.stringWidth("EXP") + 8;
                int bh = 14;
                gl.setColor(EXP_BADGE);
                gl.fillRoundRect(x, y - bh + 1, bw, bh, 6, 6);
                gl.setColor(new Color(20, 50, 90));
                gl.drawRoundRect(x, y - bh + 1, bw, bh, 6, 6);
                gl.setFont(badgeFont);
                gl.setColor(Color.WHITE);
                gl.drawString("EXP", x + 4, y - 2);
                drawOutlinedText(gl, "+" + fx.exp, valueFont, x + bw + 6, y, EXP_TEXT);
            }
            gl.dispose();
        }
        repaint();
    }

    /** A spinning gold coin; xScale < 1 squashes it horizontally to fake rotation. */
    private static void drawCoin(Graphics2D g2, float cx, float cy, float r, float xScale) {
        float rw = r * xScale;
        Ellipse2D.Float body = new Ellipse2D.Float(cx - rw, cy - r, rw * 2, r * 2);
        g2.setPaint(new GradientPaint(cx, cy - r, COIN_LIGHT, cx, cy + r, COIN_DARK));
        g2.fill(body);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(COIN_RIM);
        g2.draw(body);
        if (xScale > 0.5f) {
            // Inner ring + highlight so it reads as a coin rather than a plain yellow dot.
            float ir = r * 0.55f, irw = ir * xScale;
            g2.setColor(new Color(COIN_RIM.getRed(), COIN_RIM.getGreen(), COIN_RIM.getBlue(), 140));
            g2.draw(new Ellipse2D.Float(cx - irw, cy - ir, irw * 2, ir * 2));
            g2.setColor(new Color(255, 255, 255, 190));
            g2.fill(new Ellipse2D.Float(cx - rw * 0.55f, cy - r * 0.7f, rw * 0.45f, r * 0.4f));
        }
    }

    /** Text with a dark outline so it stays readable over any background art. */
    private static void drawOutlinedText(Graphics2D g2, String text, Font font, float x, float y, Color fill) {
        Shape s = font.createGlyphVector(g2.getFontRenderContext(), text).getOutline(x, y);
        g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(20, 16, 10));
        g2.draw(s);
        g2.setColor(fill);
        g2.fill(s);
    }

    private static int playerBodyH() {
        return (int) Math.round(PLAYER_SPRITE_HEIGHT * PLAYER_SCALE);
    }

    private static int playerBodyW() {
        if (PLAYER_SPRITE == null) return (int) Math.round(playerBodyH() * 0.6);
        double spriteScale = PLAYER_SPRITE_HEIGHT / PLAYER_SPRITE.getHeight();
        return (int) Math.round(PLAYER_SPRITE.getWidth() * spriteScale * PLAYER_SCALE);
    }

    /** A four-pointed sparkle star centered at (cx, cy). */
    private static Shape starShape(float cx, float cy, float r) {
        float inner = r * 0.28f;
        Path2D.Float p = new Path2D.Float();
        p.moveTo(cx, cy - r);
        p.lineTo(cx + inner, cy - inner);
        p.lineTo(cx + r, cy);
        p.lineTo(cx + inner, cy + inner);
        p.lineTo(cx, cy + r);
        p.lineTo(cx - inner, cy + inner);
        p.lineTo(cx - r, cy);
        p.lineTo(cx - inner, cy - inner);
        p.closePath();
        return p;
    }

    /** Pulls back slightly below 0 before accelerating to 1: a wind-up before the lunge. */
    private static float easeInBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return c3 * x * x * x - c1 * x * x;
    }

    private static float easeOutCubic(float x) {
        return 1 - (float) Math.pow(1 - x, 3);
    }

    /** Overshoots slightly past 1 then settles, giving each dropped letter a small bounce. */
    private static float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    /** Draws and expires floating damage numbers that rise and fade above whichever side took the hit. */
    private void drawDamageTexts(Graphics2D g2, int playerCx, int playerGroundY, int monsterCx, int monsterGroundY) {
        if (damageTexts.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<DamageText> it = damageTexts.iterator();
        while (it.hasNext()) {
            DamageText dt = it.next();
            long elapsed = now - dt.start;
            if (elapsed >= DAMAGE_TEXT_DURATION) {
                it.remove();
                continue;
            }
            float progress = elapsed / (float) DAMAGE_TEXT_DURATION;
            int cx = (dt.onMonster ? monsterCx : playerCx) + dt.jitterX;
            // Over a big sprite, start from its upper body but keep the risen number on screen.
            int baseY = dt.onMonster
                    ? Math.max(58, monsterGroundY - Math.max(70, Math.round(monsterSpritePx * 0.8f)))
                    : playerGroundY - 70;
            // Rise fast then slow down; stay solid for the first half so it can be read, then fade.
            int y = baseY - Math.round(easeOutCubic(progress) * 34);
            float alpha = progress < 0.5f ? 1f : 1f - (progress - 0.5f) / 0.5f;

            // Pop: spawn oversized, overshoot below 1, settle (crits pop harder).
            float pop = 1f;
            if (elapsed < DAMAGE_POP) {
                pop = 1f + (dt.crit ? 0.8f : 0.4f) * (1f - easeOutBack(elapsed / (float) DAMAGE_POP));
            }
            Font font = Theme.dosFont(Font.BOLD, (dt.crit ? 26f : 19f) * pop);
            int tw = g2.getFontMetrics(font).stringWidth(dt.text);

            Color color = dt.crit ? new Color(255, 196, 50)
                    : dt.onMonster ? Color.WHITE : new Color(255, 120, 110);
            Graphics2D gt = (Graphics2D) g2.create();
            gt.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
            drawOutlinedText(gt, dt.text, font, cx - tw / 2f, y, color);
            if (dt.crit) {
                Font label = Theme.dosFont(Font.BOLD, 11f);
                int lw = gt.getFontMetrics(label).stringWidth("CRITICAL");
                drawOutlinedText(gt, "CRITICAL", label, cx - lw / 2f, y - font.getSize2D() + 2, new Color(255, 240, 180));
            }
            gt.dispose();
        }
        repaint();
    }

    private void drawPlatform(Graphics2D g2, int cx, int groundY, int width, int height, Color fill) {
        g2.setColor(fill);
        g2.fillOval(cx - width / 2, groundY - height / 2, width, height);
        g2.setColor(fill.darker());
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(cx - width / 2, groundY - height / 2, width, height);
        g2.setStroke(new BasicStroke(1));
    }

    // ---------- player (seen from behind, close-up foreground) ----------
    private static final BufferedImage PLAYER_SPRITE = loadImage("images/player.png");
    private static final double PLAYER_SPRITE_HEIGHT = 150.0;

    private void drawPlayer(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(x - 26, groundY - 6, 52, 10);

        if (PLAYER_SPRITE == null) return;
        double scale = PLAYER_SPRITE_HEIGHT / PLAYER_SPRITE.getHeight();
        int dw = (int) Math.round(PLAYER_SPRITE.getWidth() * scale);
        int dh = (int) Math.round(PLAYER_SPRITE.getHeight() * scale);
        Graphics2D gi = (Graphics2D) g2.create();
        gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gi.drawImage(PLAYER_SPRITE, x - dw / 2, groundY - dh, dw, dh, null);
        gi.dispose();
    }

    /** Loads a bundled resource image from the classpath, relative to this class's package. */
    private static BufferedImage loadImage(String path) {
        try (InputStream in = BattleStage.class.getResourceAsStream(path)) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception ignored) {
            // Missing/unreadable resource: caller falls back to no sprite drawn.
        }
        return null;
    }

    // ---------- monster dispatch ----------
    private void drawMonster(Graphics2D g2, String name, int x, int groundY) {
        MonsterSprite sprite = MONSTER_SPRITES.get(name);
        if (sprite != null && monsterSpritePx > 0) {
            // g2 is already scaled by MONSTER_SCALE, so draw at unscaled size to land at monsterSpritePx on screen.
            BufferedImage img = spriteAt(sprite, monsterSpritePx);
            double uw = img.getWidth() / MONSTER_SCALE;
            double uh = img.getHeight() / MONSTER_SCALE;
            g2.setColor(new Color(0, 0, 0, 80));
            int shadowW = (int) Math.round(uw * 0.6);
            g2.fillOval(x - shadowW / 2, groundY - 6, shadowW, 12);
            Graphics2D gi = (Graphics2D) g2.create();
            gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gi.drawImage(img, (int) Math.round(x - uw / 2), (int) Math.round(groundY - uh + 4),
                    (int) Math.round(uw), (int) Math.round(uh), null);
            gi.dispose();
            return;
        }

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(x - 26, groundY - 6, 52, 10);

        if (name.contains("마왕")) drawDemonLord(g2, x, groundY);
        else if (name.contains("리치")) drawLich(g2, x, groundY);
        else if (name.contains("마법사")) drawMage(g2, x, groundY);
        else if (name.contains("기사")) drawKnight(g2, x, groundY);
        else if (name.contains("망령")) drawGhost(g2, x, groundY);
        else if (name.contains("해골")) drawSkeleton(g2, x, groundY);
        else if (name.contains("오크")) drawOrc(g2, x, groundY);
        else if (name.contains("고블린")) drawGoblin(g2, x, groundY);
        else if (name.contains("박쥐")) drawBat(g2, x, groundY);
        else if (name.contains("늑대")) drawWolf(g2, x, groundY);
        else if (name.contains("멧돼지")) drawBoar(g2, x, groundY);
        else if (name.contains("들쥐")) drawRat(g2, x, groundY);
        else drawGenericBlob(g2, x, groundY, name);
    }

    private void drawRat(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(120, 100, 90));
        g2.fillOval(x - 20, groundY - 26, 40, 24);
        g2.fillOval(x - 26, groundY - 22, 14, 14);
        g2.setColor(new Color(150, 128, 116));
        g2.fillOval(x + 6, groundY - 34, 12, 12);
        g2.fillOval(x + 16, groundY - 34, 12, 12);
        g2.setColor(new Color(40, 40, 46));
        g2.fillOval(x - 22, groundY - 20, 3, 3);
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(190, 160, 150));
        g2.drawLine(x - 26, groundY - 10, x - 40, groundY - 4);
    }

    private void drawBoar(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(96, 74, 58));
        g2.fillRoundRect(x - 26, groundY - 36, 52, 32, 16, 16);
        g2.fillOval(x - 30, groundY - 32, 18, 18);
        g2.setColor(Color.WHITE);
        g2.fillOval(x - 30, groundY - 20, 6, 8);
        g2.fillOval(x - 24, groundY - 20, 6, 8);
        g2.setColor(new Color(40, 40, 46));
        g2.fillOval(x - 26, groundY - 26, 3, 3);
        g2.setColor(new Color(60, 46, 36));
        g2.fillRect(x - 4, groundY - 6, 8, 10);
        g2.fillRect(x + 12, groundY - 6, 8, 10);
    }

    private void drawWolf(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(120, 122, 130));
        g2.fillRoundRect(x - 22, groundY - 44, 46, 34, 14, 14);
        Path2D ears = new Path2D.Double();
        ears.moveTo(x - 18, groundY - 40);
        ears.lineTo(x - 12, groundY - 56);
        ears.lineTo(x - 4, groundY - 40);
        ears.closePath();
        g2.fill(ears);
        g2.setColor(new Color(150, 152, 160));
        g2.fillOval(x - 28, groundY - 34, 18, 16);
        g2.setColor(new Color(220, 60, 60));
        g2.fillOval(x - 26, groundY - 30, 3, 3);
        g2.fillRect(x - 6, groundY - 8, 8, 10);
        g2.fillRect(x + 10, groundY - 8, 8, 10);
    }

    private void drawBat(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(70, 54, 90));
        Path2D wingL = new Path2D.Double();
        wingL.moveTo(x - 6, groundY - 34);
        wingL.lineTo(x - 40, groundY - 46);
        wingL.lineTo(x - 26, groundY - 24);
        wingL.closePath();
        g2.fill(wingL);
        Path2D wingR = new Path2D.Double();
        wingR.moveTo(x + 6, groundY - 34);
        wingR.lineTo(x + 40, groundY - 46);
        wingR.lineTo(x + 26, groundY - 24);
        wingR.closePath();
        g2.fill(wingR);
        g2.fillOval(x - 12, groundY - 38, 24, 22);
        g2.setColor(new Color(220, 60, 60));
        g2.fillOval(x - 6, groundY - 30, 3, 3);
        g2.fillOval(x + 3, groundY - 30, 3, 3);
    }

    private void drawGoblin(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(86, 140, 74));
        g2.fillOval(x - 15, groundY - 46, 30, 28);
        Path2D earL = new Path2D.Double();
        earL.moveTo(x - 15, groundY - 36);
        earL.lineTo(x - 28, groundY - 40);
        earL.lineTo(x - 15, groundY - 28);
        earL.closePath();
        g2.fill(earL);
        Path2D earR = new Path2D.Double();
        earR.moveTo(x + 15, groundY - 36);
        earR.lineTo(x + 28, groundY - 40);
        earR.lineTo(x + 15, groundY - 28);
        earR.closePath();
        g2.fill(earR);
        g2.setColor(new Color(60, 100, 50));
        g2.fillRoundRect(x - 16, groundY - 20, 32, 20, 10, 10);
        g2.setColor(new Color(220, 220, 60));
        g2.fillOval(x - 8, groundY - 36, 5, 5);
        g2.fillOval(x + 3, groundY - 36, 5, 5);
        g2.setColor(new Color(150, 150, 160));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(x + 18, groundY - 18, x + 34, groundY - 34);
    }

    private void drawOrc(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(58, 96, 56));
        g2.fillRoundRect(x - 24, groundY - 60, 48, 40, 12, 12);
        g2.fillOval(x - 18, groundY - 54, 36, 30);
        g2.setColor(Color.WHITE);
        g2.fillRect(x - 10, groundY - 34, 5, 8);
        g2.fillRect(x + 5, groundY - 34, 5, 8);
        g2.setColor(new Color(220, 60, 60));
        g2.fillOval(x - 10, groundY - 44, 4, 4);
        g2.fillOval(x + 6, groundY - 44, 4, 4);
        g2.setColor(new Color(90, 90, 96));
        g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 22, groundY - 40, x + 40, groundY - 58);
        g2.fillRect(x + 34, groundY - 66, 14, 8);
    }

    private void drawSkeleton(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(224, 220, 205));
        g2.fillOval(x - 14, groundY - 52, 28, 26);
        g2.setColor(new Color(30, 30, 34));
        g2.fillOval(x - 8, groundY - 44, 5, 6);
        g2.fillOval(x + 3, groundY - 44, 5, 6);
        g2.fillRect(x - 3, groundY - 32, 6, 3);
        g2.setColor(new Color(224, 220, 205));
        g2.fillRoundRect(x - 12, groundY - 26, 24, 20, 6, 6);
        g2.setStroke(new BasicStroke(2));
        for (int i = 0; i < 3; i++) g2.drawLine(x - 10, groundY - 22 + i * 5, x + 10, groundY - 22 + i * 5);
    }

    private void drawGhost(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(150, 190, 220, 190));
        Path2D body = new Path2D.Double();
        body.moveTo(x - 20, groundY - 8);
        body.curveTo(x - 26, groundY - 50, x - 10, groundY - 60, x, groundY - 60);
        body.curveTo(x + 10, groundY - 60, x + 26, groundY - 50, x + 20, groundY - 8);
        body.lineTo(x + 12, groundY - 16);
        body.lineTo(x + 4, groundY - 8);
        body.lineTo(x - 4, groundY - 16);
        body.lineTo(x - 12, groundY - 8);
        body.closePath();
        g2.fill(body);
        g2.setColor(new Color(40, 40, 60));
        g2.fillOval(x - 9, groundY - 44, 6, 8);
        g2.fillOval(x + 3, groundY - 44, 6, 8);
    }

    private void drawLich(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(78, 58, 110));
        Path2D robe = new Path2D.Double();
        robe.moveTo(x - 22, groundY - 4);
        robe.lineTo(x - 16, groundY - 60);
        robe.lineTo(x + 16, groundY - 60);
        robe.lineTo(x + 22, groundY - 4);
        robe.closePath();
        g2.fill(robe);
        g2.setColor(new Color(224, 220, 205));
        g2.fillOval(x - 12, groundY - 74, 24, 22);
        g2.setColor(new Color(150, 90, 230));
        g2.fillOval(x - 7, groundY - 66, 4, 4);
        g2.fillOval(x + 3, groundY - 66, 4, 4);
        g2.setColor(new Color(150, 90, 230));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(x + 20, groundY - 6, x + 30, groundY - 66);
        g2.fillOval(x + 24, groundY - 72, 10, 10);
    }

    private void drawKnight(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(50, 54, 64));
        g2.fillRoundRect(x - 18, groundY - 52, 36, 38, 10, 10);
        g2.fillRoundRect(x - 13, groundY - 70, 26, 22, 8, 8);
        g2.setColor(new Color(120, 20, 30));
        g2.fillRect(x - 3, groundY - 78, 6, 10);
        g2.setColor(new Color(220, 60, 60));
        g2.fillRect(x - 9, groundY - 62, 18, 4);
        g2.setColor(new Color(150, 150, 160));
        g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 16, groundY - 42, x + 34, groundY - 66);
    }

    private void drawMage(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(60, 74, 130));
        Path2D robe = new Path2D.Double();
        robe.moveTo(x - 22, groundY - 4);
        robe.lineTo(x - 14, groundY - 56);
        robe.lineTo(x + 14, groundY - 56);
        robe.lineTo(x + 22, groundY - 4);
        robe.closePath();
        g2.fill(robe);
        g2.setColor(new Color(235, 205, 170));
        g2.fillOval(x - 11, groundY - 68, 22, 20);
        g2.setColor(new Color(40, 60, 120));
        Path2D hat = new Path2D.Double();
        hat.moveTo(x - 16, groundY - 60);
        hat.lineTo(x + 2, groundY - 92);
        hat.lineTo(x + 18, groundY - 60);
        hat.closePath();
        g2.fill(hat);
        g2.setColor(new Color(120, 170, 255));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(x + 20, groundY - 4, x + 30, groundY - 60);
        g2.fillOval(x + 24, groundY - 68, 10, 10);
    }

    private void drawDemonLord(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(120, 30, 34));
        Path2D wingL = new Path2D.Double();
        wingL.moveTo(x - 18, groundY - 60);
        wingL.lineTo(x - 56, groundY - 78);
        wingL.lineTo(x - 40, groundY - 40);
        wingL.closePath();
        g2.fill(wingL);
        Path2D wingR = new Path2D.Double();
        wingR.moveTo(x + 18, groundY - 60);
        wingR.lineTo(x + 56, groundY - 78);
        wingR.lineTo(x + 40, groundY - 40);
        wingR.closePath();
        g2.fill(wingR);

        g2.setColor(new Color(150, 34, 40));
        g2.fillRoundRect(x - 22, groundY - 66, 44, 46, 14, 14);
        g2.fillOval(x - 16, groundY - 88, 32, 30);

        g2.setColor(new Color(20, 20, 24));
        Path2D hornL = new Path2D.Double();
        hornL.moveTo(x - 12, groundY - 86);
        hornL.lineTo(x - 24, groundY - 108);
        hornL.lineTo(x - 6, groundY - 92);
        hornL.closePath();
        g2.fill(hornL);
        Path2D hornR = new Path2D.Double();
        hornR.moveTo(x + 12, groundY - 86);
        hornR.lineTo(x + 24, groundY - 108);
        hornR.lineTo(x + 6, groundY - 92);
        hornR.closePath();
        g2.fill(hornR);

        g2.setColor(new Color(255, 210, 60));
        g2.fillOval(x - 9, groundY - 76, 5, 5);
        g2.fillOval(x + 4, groundY - 76, 5, 5);
    }

    private void drawGenericBlob(Graphics2D g2, int x, int groundY, String name) {
        int hash = name.hashCode();
        Color c = new Color(90 + Math.abs(hash) % 120, 90 + Math.abs(hash / 3) % 120, 90 + Math.abs(hash / 7) % 120);
        g2.setColor(c);
        Ellipse2D body = new Ellipse2D.Double(x - 24, groundY - 46, 48, 42);
        g2.fill(body);
        g2.setColor(new Color(220, 60, 60));
        g2.fillOval(x - 10, groundY - 34, 6, 6);
        g2.fillOval(x + 4, groundY - 34, 6, 6);
    }
}
