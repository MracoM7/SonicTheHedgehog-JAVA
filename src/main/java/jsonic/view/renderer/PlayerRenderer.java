package jsonic.view.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import java.util.List;

import jsonic.utils.GameConstants;
import jsonic.model.entity.Player;
import jsonic.model.entity.Player.PlayerState;
import jsonic.model.item.BridgeLog;
import jsonic.model.item.Item;
import jsonic.model.tile.TileID;
import jsonic.view.DebugDraw;
import jsonic.view.Fonts;
import jsonic.view.snapshot.PlayRenderSnapshot;

/**
 * Renders the player and drives its animation frame-advance logic, keeping
 * these rendering counters isolated from the physics engine. Reads
 * everything from PlayRenderSnapshot, no direct controller dependency.
 */
public class PlayerRenderer {

    // animation constants
    private final int BORED_THRESHOLD = 300;
    private final int BORED_FRAME_DURATION = 25;
    private final int RUN_ANIM_BASE_SPEED = 45;
    private final int JUMP_ANIM_SPEED = 3;
    private final int BALANCE_ANIM_SPEED = 20; // slow rock side to side
    private final int SKID_ANIM_SPEED = 6;
    private final int SKID_POSE_MIN_FRAMES = 15; // isSkidding() can go true for only 2-3 frames at high speed, too brief on its own
    private final int PUSH_ANIM_SPEED = 8;
    private final int HURT_ANIM_SPEED = 6;
    private final float ROTATION_EASE_DEG_PER_FRAME = 9f;
    private final float Y_EASE_PIXELS_PER_FRAME = 2f * GameConstants.SCALE;

    private final int SPRITE_FEET_OFFSET_NATIVE = 20; // hand-tuned, deliberately not Player.BASE_RADIUS_Y (19)

    // debug overlay
    private static final int SENSOR_LINE_THICKNESS = 5;
    private static final int SENSOR_MARKER_RADIUS = 5;
    private static final float DEBUG_TEXT_SIZE = 15f;
    private static final int DEBUG_TEXT_LINE_HEIGHT = 17;

    // assets & internal state
    private BufferedImage idleSprite;
    private BufferedImage lookingUpSprite;
    private BufferedImage curlingUpSprite;
    private BufferedImage deathSprite;
    private BufferedImage bouncedUpSprite;
    private BufferedImage[] balanceSprites;
    private BufferedImage[] skidSprites;
    private BufferedImage[] pushingSprites;
    private BufferedImage[] hurtSprites;
    private BufferedImage[] runSprites;
    private BufferedImage[] fastSprites;
    private BufferedImage[] jumpSprites;
    private BufferedImage[] boredSprites;

    // Counters isolated in the Renderer: they do not affect the Model's physics
    private int spriteCounter = 0;
    private int spriteNum = 0;
    private PlayerState previousState = PlayerState.IDLE;
    private float visualAngle = 0f;
    private float visualY = 0f;
    private float yOffset = 0f; // visualY = player.getY() + yOffset; decays to 0
    private float lastTrueY = 0f;
    private boolean wasOnGround = false;

    // Independent counters for the overlay poses (balance/skid/push/hurt, see selectSprite()).
    private int balanceCounter, balanceIndex;
    private int skidCounter, skidIndex;
    private int skidPoseTimer = 0; // see SKID_POSE_MIN_FRAMES
    private int pushCounter, pushIndex;
    private int hurtCounter, hurtIndex;

    // constructor and initialisation

    public PlayerRenderer() {
        loadSprites();
    }

    /** Loads the sprite sheet crops; coordinates are hardcoded since the sheet layout never changes. */
    private void loadSprites() {
        try {
            BufferedImage sheet = ImageIO.read(
                getClass().getResourceAsStream("/res/sprites/player/sonic.png"));

            idleSprite = sheet.getSubimage(43, 257, 32, 40);
            lookingUpSprite = sheet.getSubimage(425, 257, 32, 40);
            curlingUpSprite = sheet.getSubimage(507, 265, 40, 32);
            deathSprite = sheet.getSubimage(287, 800, 34, 41);
            bouncedUpSprite = sheet.getSubimage(597, 253, 24, 48);

            balanceSprites = new BufferedImage[2];
            balanceSprites[0] = sheet.getSubimage(463, 439, 38, 40);
            balanceSprites[1] = sheet.getSubimage(535, 440, 33, 39);

            skidSprites = new BufferedImage[2];
            skidSprites[0] = sheet.getSubimage(481, 352, 30, 36);
            skidSprites[1] = sheet.getSubimage(547, 352, 34, 36);

            pushingSprites = new BufferedImage[4];
            pushingSprites[0] = sheet.getSubimage(407, 625, 29, 36);
            pushingSprites[1] = sheet.getSubimage(482, 624, 24, 37);
            pushingSprites[2] = sheet.getSubimage(548, 625, 28, 36);
            pushingSprites[3] = sheet.getSubimage(622, 624, 24, 37);

            hurtSprites = new BufferedImage[2];
            hurtSprites[0] = sheet.getSubimage(39, 811, 40, 28);
            hurtSprites[1] = sheet.getSubimage(109, 811, 39, 27);

            runSprites = new BufferedImage[6];
            runSprites[0] = sheet.getSubimage(46, 349, 24, 40);
            runSprites[1] = sheet.getSubimage(109, 347, 40, 40);
            runSprites[2] = sheet.getSubimage(178, 348, 32, 40);
            runSprites[3] = sheet.getSubimage(249, 349, 40, 40);
            runSprites[4] = sheet.getSubimage(319, 347, 40, 40);
            runSprites[5] = sheet.getSubimage(390, 348, 40, 40);

            fastSprites = new BufferedImage[4];
            fastSprites[0] = sheet.getSubimage(39, 532, 32, 40);
            fastSprites[1] = sheet.getSubimage(109, 532, 32, 40);
            fastSprites[2] = sheet.getSubimage(179, 533, 32, 39);
            fastSprites[3] = sheet.getSubimage(249, 532, 32, 40);

            jumpSprites = new BufferedImage[5];
            jumpSprites[0] = sheet.getSubimage(43, 625, 32, 32);
            jumpSprites[1] = sheet.getSubimage(113, 625, 32, 32);
            jumpSprites[2] = sheet.getSubimage(183, 625, 32, 32);
            jumpSprites[3] = sheet.getSubimage(253, 625, 32, 32);
            jumpSprites[4] = sheet.getSubimage(323, 625, 32, 32);

            boredSprites = new BufferedImage[4];
            boredSprites[0] = sheet.getSubimage(129, 257, 32, 40);
            boredSprites[1] = sheet.getSubimage(199, 257, 32, 40);
            boredSprites[2] = sheet.getSubimage(269, 257, 32, 40);
            boredSprites[3] = sheet.getSubimage(339, 257, 32, 40);

        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            System.err.println("PlayerRenderer: failed to load sprite from /res/sprites/player/sonic.png");
        }
    }

    // main rendering

    public void draw(Graphics2D g2, PlayRenderSnapshot snap, boolean animate) {
        Player player = snap.player;

        if (animate) updateAnimationLogic(player); // skipped while frozen (Pause, Game Over)
        BufferedImage image = selectSprite(player);

        if (image == null) return;

        int scale = GameConstants.SCALE;
        int spriteW = image.getWidth() * scale;
        int spriteH = image.getHeight() * scale;

        updateVisualY(player, snap.items);

        // Logical player centre in screen coordinates
        int screenCenterX = (int) player.getX() - snap.cameraX;
        int screenCenterY = (int) visualY - snap.cameraY;
        int visualOffsetY = SPRITE_FEET_OFFSET_NATIVE * scale;

        // Sprite offset RELATIVE to the player centre (origin = centre)
        int relX = -(spriteW / 2);
        int relY;
        if (player.getCurrentState() == PlayerState.CROUCHING) {
            relY = player.getCurrentRadiusY() - spriteH; // feet at the bottom edge of the hitbox
        } else {
            relY = -visualOffsetY;
        }

        // Rotation snapped to 45° like the original (jumping/rolling stay upright), eased
        // toward the target instead of snapping instantly so a tile transition reads as a lean.
        Graphics2D gg = (Graphics2D) g2.create();
        gg.translate(screenCenterX, screenCenterY);
        if (shouldRotate(player)) {
            visualAngle = approachAngle(visualAngle, snapTo45(player.getGroundAngle()), ROTATION_EASE_DEG_PER_FRAME);
        } else {
            visualAngle = 0f;
        }
        if (visualAngle != 0f) {
            // physical angle counter-clockwise (Y up) → screen rotation (Y down) = -angle
            gg.rotate(Math.toRadians(-visualAngle));
        }

        // Horizontal flip when facing left
        if (player.isFacingRight()) {
            gg.drawImage(image, relX, relY, spriteW, spriteH, null);
        } else {
            gg.drawImage(image, relX + spriteW, relY, -spriteW, spriteH, null);
        }
        gg.dispose();

        if (snap.debugMode) {
            drawDebugOverlay(g2, snap, player);
        }
    }

    /** Rounds to the nearest 45° step; ties break away from 0° instead of always up, or 337.5° would round to upright. */
    private float snapTo45(float angle) {
        float a = ((angle % 360f) + 360f) % 360f;
        float q = a / 45f;
        float steps = (a <= 180f) ? (float) Math.floor(q + 0.5f) : (float) Math.ceil(q - 0.5f);
        float snapped = (steps * 45f) % 360f;
        return (snapped < 0f) ? snapped + 360f : snapped;
    }

    /** Moves `current` toward `target` by at most `maxDelta` degrees, along the shortest circular path. */
    private float approachAngle(float current, float target, float maxDelta) {
        float diff = ((target - current + 540f) % 360f) - 180f; // shortest signed diff in (-180,180]
        if (Math.abs(diff) <= maxDelta) return ((target % 360f) + 360f) % 360f;
        float next = current + Math.signum(diff) * maxDelta;
        return ((next % 360f) + 360f) % 360f;
    }

    /**
     * Eases the sprite's drawn Y toward the true position (player.getY() itself is untouched)
     * so a discrete tile-height step reads as a brief glide, not a pop. Compares the actual Y
     * change against what smooth ground movement would predict; only the mismatch (a real
     * step) gets absorbed and decayed, so slopes/loops track exactly. Also applies the nearest
     * BridgeLog's sag, which is drawn-only.
     */
    private void updateVisualY(Player player, List<Item> items) {
        float trueY = player.getY();
        if (!player.isOnGround() || !wasOnGround) {
            yOffset = 0f;
        } else {
            float actualDelta = trueY - lastTrueY;
            float expectedDelta = -player.getGSpeed() * (float) Math.sin(Math.toRadians(player.getGroundAngle()));
            yOffset -= actualDelta - expectedDelta;
            yOffset = approachValue(yOffset, 0f, Y_EASE_PIXELS_PER_FRAME);
        }
        visualY = trueY + yOffset + nearestBridgeSag(player, items);
        lastTrueY = trueY;
        wasOnGround = player.isOnGround();
    }

    /** Sag of the closest BridgeLog within one tile of the player, or 0 if none is that close. */
    private int nearestBridgeSag(Player player, List<Item> items) {
        BridgeLog nearest = null;
        float nearestDist = GameConstants.TILE_SIZE;
        for (Item item : items) {
            if (!(item instanceof BridgeLog log)) continue;
            float dist = Math.abs(player.getX() - log.worldX);
            if (dist < nearestDist) {
                nearest = log;
                nearestDist = dist;
            }
        }
        return nearest == null ? 0 : nearest.getSagOffset();
    }

    /** Moves `current` toward `target` by at most `maxDelta`, linearly (no wraparound). */
    private float approachValue(float current, float target, float maxDelta) {
        float diff = target - current;
        if (Math.abs(diff) <= maxDelta) return target;
        return current + Math.signum(diff) * maxDelta;
    }

    /** True if the sprite should rotate with the ground (standing, on ground). */
    private boolean shouldRotate(Player player) {
        if (player.isDying())     return false; // always upright, whatever angle the ground was at the moment of death
        if (!player.isOnGround()) return false;
        switch (player.getCurrentState()) {
            case RUNNING:
            case IDLE:
            case LOOKING_UP:
            case CROUCHING:
            case BALANCE:
                return true;
            default: // JUMPING, ROLLING → ball shape, no rotation
                return false;
        }
    }

    // animation logic

    private void updateAnimationLogic(Player player) {
        // Overlay poses (see selectSprite()) each reset to frame 0 the instant their own condition goes false.
        if (player.isHitStun()) {
            hurtCounter++;
            if (hurtCounter > HURT_ANIM_SPEED) { hurtIndex = (hurtIndex + 1) % hurtSprites.length; hurtCounter = 0; }
        } else {
            hurtCounter = 0; hurtIndex = 0;
        }
        if (player.isPushing()) {
            pushCounter++;
            if (pushCounter > PUSH_ANIM_SPEED) { pushIndex = (pushIndex + 1) % pushingSprites.length; pushCounter = 0; }
        } else {
            pushCounter = 0; pushIndex = 0;
        }
        if (player.isSkidding()) skidPoseTimer = SKID_POSE_MIN_FRAMES;
        else if (skidPoseTimer > 0) skidPoseTimer--;

        if (skidPoseTimer > 0) {
            skidCounter++;
            if (skidCounter > SKID_ANIM_SPEED) { skidIndex = (skidIndex + 1) % skidSprites.length; skidCounter = 0; }
        } else {
            skidCounter = 0; skidIndex = 0;
        }
        if (player.getCurrentState() == PlayerState.BALANCE) {
            balanceCounter++;
            if (balanceCounter > BALANCE_ANIM_SPEED) { balanceIndex = (balanceIndex + 1) % balanceSprites.length; balanceCounter = 0; }
        } else {
            balanceCounter = 0; balanceIndex = 0;
        }

        // Reset counters when the base state changes (e.g. from JUMPING to RUNNING)
        if (player.getCurrentState() != previousState) {
            spriteNum = 0;
            spriteCounter = 0;
            previousState = player.getCurrentState();
        }

        if (!player.isOnGround()) {
            spriteCounter++;
            if (spriteCounter > JUMP_ANIM_SPEED) {
                spriteNum = (spriteNum + 1) % jumpSprites.length;
                spriteCounter = 0;
            }
        } else {
            if (Math.abs(player.getGSpeed()) > 0.1f) { // below this, treat as stopped rather than an animated crawl
                // +0.5f keeps the cycle from spinning up towards infinite speed as gSpeed -> 0
                int dynSpeed = Math.max(2,
                    (int)(RUN_ANIM_BASE_SPEED / (Math.abs(player.getGSpeed()) + 0.5f)));
                spriteCounter++;
                if (spriteCounter > dynSpeed) {
                    int maxFrames = (Math.abs(player.getGSpeed()) > player.getFastRunThreshold())
                        ? fastSprites.length : runSprites.length;
                    spriteNum = (spriteNum + 1) % maxFrames;
                    spriteCounter = 0;
                }
            } else {
                if (player.getCurrentState() != PlayerState.IDLE) {
                    spriteNum = 0; 
                    spriteCounter = 0;
                }
            }
        }
    }

    private BufferedImage selectSprite(Player player) {
        if (player.isDying()) return deathSprite; // wins over everything; the model is frozen while dying

        // Overlay poses (variations of RUNNING/JUMPING, not their own PlayerState) checked first.
        if (player.isHitStun()) return hurtSprites[hurtIndex];
        if (player.isBounced())                              return bouncedUpSprite;
        if (player.isPushing())                              return pushingSprites[pushIndex];
        if (skidPoseTimer > 0)                                return skidSprites[skidIndex];

        float absVel = Math.abs(player.getGSpeed());

        switch (player.getCurrentState()) {
            case JUMPING:
            case ROLLING:
                return jumpSprites[spriteNum % jumpSprites.length];
            case CROUCHING:
                return curlingUpSprite;
            case LOOKING_UP:
                return lookingUpSprite;
            case BALANCE:
                return balanceSprites[balanceIndex];
            case RUNNING:
                return (absVel > player.getFastRunThreshold())
                       ? fastSprites[spriteNum % fastSprites.length]
                       : runSprites [spriteNum % runSprites.length];
            case IDLE:
            default:
                return selectIdleOrBoredSprite(player.getIdleTimer());
        }
    }

    private BufferedImage selectIdleOrBoredSprite(int idleTimer) {
        if (idleTimer <= BORED_THRESHOLD) return idleSprite;
        
        int framesSinceBored = idleTimer - BORED_THRESHOLD;
        if (framesSinceBored < BORED_FRAME_DURATION * 2) {
            return boredSprites[framesSinceBored / BORED_FRAME_DURATION];
        }
        
        int loopFrames = framesSinceBored - (BORED_FRAME_DURATION * 2);
        return boredSprites[2 + ((loopFrames / BORED_FRAME_DURATION) % 2)];
    }

    // debug overlay

    private void drawDebugOverlay(Graphics2D g2, PlayRenderSnapshot snap, Player player) {
        int dbgX = (int) player.getX() - player.getCurrentRadiusX() - snap.cameraX;
        int dbgY = (int) player.getY() - player.getCurrentRadiusY() - snap.cameraY;
        int hitboxW = player.getCurrentRadiusX() * 2;
        int hitboxH = player.getCurrentRadiusY() * 2;

        // Physical hitbox - thick border growing inward, so the outer edge still matches the true hitbox
        g2.setColor(Color.RED);
        DebugDraw.thickRect(g2, dbgX, dbgY, hitboxW, hitboxH, DebugDraw.HITBOX_BORDER_THICKNESS);

        // Ground sensor line
        g2.setColor(Color.BLUE);
        g2.fillRect(
            dbgX,
            dbgY + hitboxH - SENSOR_LINE_THICKNESS,
            hitboxW,
            SENSOR_LINE_THICKNESS);

        // Text info: angle, ground speed and mode (quadrant), one per line for legibility at this
        // size - plain, no outline: the outline swallows the strokes and hurts readability, not helps
        g2.setColor(Color.YELLOW);
        g2.setFont(Fonts.debugSized(DEBUG_TEXT_SIZE));
        g2.drawString(String.format("%.0f°", player.getGroundAngle()), dbgX, dbgY - 2 - DEBUG_TEXT_LINE_HEIGHT * 2);
        g2.drawString(String.format("G:%.1f", player.getGSpeed()), dbgX, dbgY - 2 - DEBUG_TEXT_LINE_HEIGHT);
        g2.drawString(player.getGravityMode().toString(), dbgX, dbgY - 2);

        drawVelocityArrow(g2, player, snap.cameraX, snap.cameraY);
        drawDownVector(g2, player, snap.cameraX, snap.cameraY);

        // FLOOR mode only: on a wall/ceiling the axis changes and these would be misplaced.
        if (player.getGravityMode() == jsonic.model.entity.Player.PhysicsMode.FLOOR) {
            int leftSensorY  = player.getDebugLeftSensorY();
            int rightSensorY = player.getDebugRightSensorY();
            int leftMarkerX  = dbgX + 4;
            int rightMarkerX = dbgX + hitboxW - 4;

            g2.setColor(Color.MAGENTA);
            if (leftSensorY != TileID.NO_SURFACE) {
                g2.fillOval(leftMarkerX - SENSOR_MARKER_RADIUS, leftSensorY - snap.cameraY - 2 - SENSOR_MARKER_RADIUS,
                    SENSOR_MARKER_RADIUS * 2, SENSOR_MARKER_RADIUS * 2);
            }
            if (rightSensorY != TileID.NO_SURFACE) {
                g2.fillOval(rightMarkerX - SENSOR_MARKER_RADIUS, rightSensorY - snap.cameraY - 2 - SENSOR_MARKER_RADIUS,
                    SENSOR_MARKER_RADIUS * 2, SENSOR_MARKER_RADIUS * 2);
            }
        }
    }

    /** White vector indicating the character's "down" direction in the current mode. */
    private void drawDownVector(Graphics2D g2, Player player, int cameraX, int cameraY) {
        int cx = (int) player.getX() - cameraX;
        int cy = (int) player.getY() - cameraY;
        int len = player.getCurrentRadiusY();
        int dx = 0, dy = 0;
        switch (player.getGravityMode()) {
            case FLOOR:      dx =  0; dy =  1; break;
            case RIGHT_WALL: dx =  1; dy =  0; break;
            case CEILING:    dx =  0; dy = -1; break;
            case LEFT_WALL:  dx = -1; dy =  0; break;
        }
        g2.setColor(Color.WHITE);
        g2.drawLine(cx, cy, cx + dx * len, cy + dy * len);
        g2.fillOval(cx + dx * len - 3, cy + dy * len - 3, 6, 6);
    }

    private void drawVelocityArrow(Graphics2D g2, Player player, int cameraX, int cameraY) {
        if (Math.abs(player.getGSpeed()) < 0.5f) return;

        float rad  = (float) Math.toRadians(player.getGroundAngle());
        int   dbgX = (int) player.getX() - player.getCurrentRadiusX() - cameraX;
        int   dbgY = (int) player.getY() - player.getCurrentRadiusY() - cameraY;
        
        int   cx   = dbgX + player.getHitboxRadiusX();
        int   cy   = dbgY + player.getHitboxRadiusY();
        int   len  = (int) Math.min(Math.abs(player.getGSpeed()) * 3, 40);
        
        int   endX = (int)(cx + Math.cos(rad) * len * Math.signum(player.getGSpeed()));
        int   endY = (int)(cy - Math.sin(rad) * len * Math.signum(player.getGSpeed()));

        g2.setColor(Color.GREEN);
        g2.drawLine(cx, cy, endX, endY);
        g2.fillOval(endX - 3, endY - 3, 6, 6);
    }
}