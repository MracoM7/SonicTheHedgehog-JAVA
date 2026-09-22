package jsonic.model.enemy;

import java.awt.Rectangle;

import jsonic.utils.GameConstants;
import jsonic.model.item.BuzzBomberShot;
import jsonic.model.physics.IPhysicsWorld;

/**
 * Flying, shooting badnik: never touches the ground, hovers with a small vertical bob, patrols a
 * fixed range around its spawn point, and fires at most once per patrol leg when the player is
 * close. Two body poses (idle/gun-out) plus Enemy's two effect slots for wing blur and exhaust.
 */
public class BuzzBomber extends Enemy {

    private static final int PATROL_SPEED_NATIVE = 2; // guide value (4) read as too fast in testing
    private static final int PATROL_SPEED = PATROL_SPEED_NATIVE * GameConstants.SCALE;
    private static final int PATROL_RANGE = GameConstants.TILE_SIZE * 3;
    private static final double BOB_SPEED = 0.05;
    private static final int BOB_AMPLITUDE = GameConstants.TILE_SIZE / 2;

    // firing sequence: waits, shoots (gun-out pose), spawns the shot, waits before resuming flight
    private static final int FIRE_TRIGGER_RADIUS = GameConstants.TILE_SIZE * 6; // player X within this range
    private static final int PRE_FIRE_FRAMES = 29;
    private static final int FIRE_ANIM_FRAMES = 14 + 8 + 9; // guide's 3-sprite shot animation length
    private static final int POST_FIRE_FRAMES = 29;

    private static final int WING_ANIM_SPEED = 2; // guide: wings animate every 2 frames
    private static final int WING_FRAME_COUNT = 2;
    private static final int EXHAUST_ANIM_SPEED = 8;
    private static final int EXHAUST_FRAME_COUNT = 2;

    private static final int STINGER_OFFSET_X = 29; // tip of the tail stinger on the "fire" pose sprite
    private static final int STINGER_OFFSET_Y = 30;
    private static final int STINGER_NUDGE = 3; // extra push along the shot's own diagonal, past the tip

    // buzzbomber_fire.png's native pixel size — fire() needs it to place the stinger tip, without
    // holding a reference to the sprite itself (that lives in BuzzBomberView now)
    private static final int FIRE_SPRITE_WIDTH = 40;
    private static final int FIRE_SPRITE_HEIGHT = 32;

    private boolean initialized = false;
    private int spawnX;
    private int spawnY;

    private int direction = 1; // -1 = left, +1 = right
    private int bobTimer = 0;

    private boolean firedThisLeg = false; // at most one shot per patrol leg, reset on turnaround
    public boolean firing = false; // read by BuzzBomberView to pick idle vs fire pose
    private int firePhase = 0; // 0 = waiting to shoot, 1 = shot animation, 2 = cooldown
    private int firePhaseTimer = 0;

    private int wingTimer = 0;
    public int wingIndex = 0; // read by BuzzBomberView
    private int exhaustTimer = 0;
    public int exhaustIndex = 0; // read by BuzzBomberView

    public BuzzBomber() {
        // 48x24 hitbox matches the idle sprite's own canvas, per the original game's collision radii
        int scale = GameConstants.SCALE;
        solidArea = new Rectangle(0, 0, 48 * scale, 24 * scale);
    }

    @Override
    public void update(IPhysicsWorld world) {
        if (!initialized) {
            spawnX = worldX;
            spawnY = worldY;
            initialized = true;
        }

        if (firing) {
            updateFiring(world);
        } else {
            worldX += direction * PATROL_SPEED;
            if (worldX > spawnX + PATROL_RANGE) { direction = -1; firedThisLeg = false; }
            if (worldX < spawnX - PATROL_RANGE) { direction = 1;  firedThisLeg = false; }
            facingRight = direction > 0;

            bobTimer++;
            worldY = spawnY + (int) Math.round(Math.sin(bobTimer * BOB_SPEED) * BOB_AMPLITUDE);

            int centerX = worldX + solidArea.x + solidArea.width / 2;
            if (!firedThisLeg && Math.abs(world.getPlayerX() - centerX) < FIRE_TRIGGER_RADIUS) {
                firing = true;
                firePhase = 0;
                firePhaseTimer = PRE_FIRE_FRAMES;
                firedThisLeg = true;
                // aim at wherever the player actually is, not whichever way the patrol was facing
                facingRight = world.getPlayerX() >= centerX;
            }
        }
        // wings keep animating even while stopped to shoot; exhaust only shows while thrusting
        if (++wingTimer > WING_ANIM_SPEED) {
            wingTimer = 0;
            wingIndex = (wingIndex + 1) % WING_FRAME_COUNT;
        }

        if (!firing && ++exhaustTimer > EXHAUST_ANIM_SPEED) {
            exhaustTimer = 0;
            exhaustIndex = (exhaustIndex + 1) % EXHAUST_FRAME_COUNT;
        }
    }

    private void updateFiring(IPhysicsWorld world) {
        if (--firePhaseTimer > 0) return;

        switch (firePhase) {
            case 0 -> { firePhase = 1; firePhaseTimer = FIRE_ANIM_FRAMES; }
            case 1 -> { fire(world); firePhase = 2; firePhaseTimer = POST_FIRE_FRAMES; }
            default -> firing = false;
        }
    }

    private void fire(IPhysicsWorld world) {
        int scale = GameConstants.SCALE;
        int drawW = FIRE_SPRITE_WIDTH * scale;
        int drawH = FIRE_SPRITE_HEIGHT * scale;
        int spriteLeft = worldX + solidArea.x + solidArea.width / 2 - drawW / 2;
        int spriteTop = worldY + solidArea.y + solidArea.height - drawH;

        // stinger offset was measured on the unflipped sprite, mirror it like EnemyRenderer does
        int stingerLocalX = facingRight ? STINGER_OFFSET_X * scale : drawW - STINGER_OFFSET_X * scale;
        int stingerX = spriteLeft + stingerLocalX;
        int stingerY = spriteTop + STINGER_OFFSET_Y * scale;

        // nudge further along the shot's own diagonal flight path (down + forward), not straight down
        int nudge = STINGER_NUDGE * scale;
        stingerX += (facingRight ? 1 : -1) * nudge;
        stingerY += nudge;

        // BuzzBomberShot is top-left anchored at (worldX, worldY), not centred - offset by half its
        // tile-sized footprint so its visual centre lands on the stinger tip
        int half = GameConstants.TILE_SIZE / 2;
        world.spawnItem(new BuzzBomberShot(stingerX - half, stingerY - half, facingRight ? 1 : -1));
    }
}
