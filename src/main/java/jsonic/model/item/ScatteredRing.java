package jsonic.model.item;

import jsonic.model.entity.ICollector;
import jsonic.model.physics.IPhysicsWorld;
import jsonic.model.physics.IPhysicsWorld.SensorDirection;
import jsonic.model.physics.IPhysicsWorld.SensorResult;
import jsonic.utils.GameConstants;

/**
 * A ring knocked loose when the player takes damage with rings > 0 (see
 * Level.scatterRings()). Extends Ring — same sprite/collision — but flies
 * outward under gravity and bounces a few times before settling, vanishing
 * after LIFESPAN_FRAMES if never recollected.
 *
 * Needs IPhysicsWorld for its own ground bounce, injected through the
 * constructor instead of widening Item's interface for one subclass.
 */
public class ScatteredRing extends Ring {

    private static final float GRAVITY = (56f / 256f) * GameConstants.SCALE; // same magnitude as Player's
    private static final float BOUNCE_DAMPING = 0.6f; // velocity kept after each bounce
    private static final float HORIZONTAL_DRAG = 0.85f; // per-bounce horizontal slowdown
    private static final float SETTLE_VELOCITY = 1.5f * GameConstants.SCALE; // below this, stop bouncing
    private static final int LIFESPAN_FRAMES = 180; // 3s @ 60fps, then it vanishes unclaimed
    private static final int GROUND_LOOK_UP = GameConstants.TILE_SIZE;
    private static final int GROUND_LOOK_DOWN = GameConstants.TILE_SIZE / 4; // short: a ring falls slowly, doesn't need a full-tile lookahead
    // rings spawn overlapping the player's own hitbox, so without this delay they'd be instantly re-collected
    private static final int PICKUP_DELAY_FRAMES = 15;

    private final IPhysicsWorld world;
    private float vx, vy;
    private int lifespan = LIFESPAN_FRAMES;
    private int pickupDelay = PICKUP_DELAY_FRAMES;
    private boolean settled = false;

    public ScatteredRing(IPhysicsWorld world, int worldX, int worldY, float vx, float vy) {
        this.world = world;
        this.worldX = worldX;
        this.worldY = worldY;
        this.vx = vx;
        this.vy = vy;
    }

    @Override
    public void update() {
        if (isCollected) {
            super.update(); // spark animation plays out and self-deletes; no physics needed anymore
            return;
        }

        lifespan--;
        if (lifespan <= 0) {
            pendingDelete = true;
            return;
        }
        if (pickupDelay > 0) pickupDelay--;

        if (!settled) {
            vy += GRAVITY;
            worldX += Math.round(vx);
            worldY += Math.round(vy);
            bounceOffGroundIfNeeded();
        }

        super.update(); // spin animation keeps playing throughout the flight/bounce
    }

    private void bounceOffGroundIfNeeded() {
        int centerX = worldX + solidArea.x + solidArea.width / 2;
        int bottomY = worldY + solidArea.y + solidArea.height;

        SensorResult ground = world.castSensor(
            centerX, bottomY, SensorDirection.DOWN, GROUND_LOOK_UP + Math.round(Math.abs(vy)), GROUND_LOOK_DOWN);
        if (!ground.found || bottomY < ground.surface) return;

        worldY -= bottomY - ground.surface; // snap back onto the surface

        if (Math.abs(vy) < SETTLE_VELOCITY) {
            settled = true;
            vx = 0;
            vy = 0;
        } else {
            vy = -vy * BOUNCE_DAMPING;
            vx *= HORIZONTAL_DRAG;
        }
    }

    @Override
    public void onCollision(ICollector collector) {
        if (pickupDelay > 0) return;
        super.onCollision(collector);
    }
}
