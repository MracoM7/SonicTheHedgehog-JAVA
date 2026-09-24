package jsonic.model.enemy;

import jsonic.model.physics.IPhysicsWorld;
import jsonic.model.physics.IPhysicsWorld.SensorDirection;
import jsonic.model.physics.IPhysicsWorld.SensorResult;
import jsonic.model.tile.TileID;
import jsonic.utils.GameConstants;

/**
 * Simplest Green Hill Zone badnik: walks at constant speed, pausing at a
 * wall or ledge for a beat before turning around, then resumes — matching
 * the original game's behaviour. Snaps to the ground surface every frame
 * rather than simulating gravity — enough for a badnik that never needs to
 * fall.
 */
public class Motobug extends Enemy {

    private static final int PATROL_SPEED_NATIVE = 1; // px/frame, original game value
    private static final int PATROL_SPEED = PATROL_SPEED_NATIVE * GameConstants.SCALE;
    private static final int PROBE_AHEAD = 6; // px past the sprite's own edge to look for a wall/ledge
    private static final int TURN_DELAY_FRAMES = 60; // 1s at 60 ticks/s before reversing direction

    // a step up to about one tile is just walked onto (ground-snap handles it); +8 slack so an
    // exact 1-tile step isn't borderline
    private static final int STEP_CLIMB_HEIGHT = GameConstants.TILE_SIZE + 8;
    private static final int GROUND_LOOK_DOWN = GameConstants.TILE_SIZE + 8;
    private static final int ANIM_FRAME_SPEED = 8; // ticks per frame
    private static final int ANIM_FRAME_COUNT = 4; // motobug.png is a 4x48x32 strip, one walk cycle

    private static final int SMOKE_ANIM_SPEED = 2;
    private static final int SMOKE_FRAME_COUNT = 3; // motobug_smoke.png is a 3x8x8 strip

    private int direction = -1; // -1 = left, +1 = right
    private boolean turning = false;
    private int turnTimer = 0;

    private int animCounter = 0;
    public int frameIndex = 0; // read by MotobugView to pick the current body frame

    private int smokeCounter = 0;
    public int smokeIndex = 0; // read by MotobugView to pick the current smoke frame

    public Motobug() {
        // 40x32 hitbox per the original game, centred in the 48x32 sprite canvas
        int scale = GameConstants.SCALE;
        solidArea = new java.awt.Rectangle(4 * scale, 0, 40 * scale, 32 * scale);
    }

    @Override
    public void update(IPhysicsWorld world) {
        if (turning) {
            // paused at the edge: no floor/wall checks while turning, so it can't wander off it
            if (++turnTimer >= TURN_DELAY_FRAMES) {
                direction = -direction;
                turning = false;
                turnTimer = 0;
            }
        } else if (obstacleAhead(world)) {
            turning = true;
            turnTimer = 0;
        } else {
            worldX += direction * PATROL_SPEED;
        }
        facingRight = direction > 0;

        // ground-hug: no gravity simulated, just snap the feet onto the surface each frame
        int centerX = worldX + solidArea.x + solidArea.width / 2;
        int feetY = worldY + solidArea.y + solidArea.height;
        SensorResult ground = world.castSensor(centerX, feetY, SensorDirection.DOWN, STEP_CLIMB_HEIGHT, GROUND_LOOK_DOWN);
        if (ground.found) {
            worldY += ground.surface - feetY;
        }

        if (!turning) {
            animCounter++;
            if (animCounter > ANIM_FRAME_SPEED) {
                frameIndex = (frameIndex + 1) % ANIM_FRAME_COUNT;
                animCounter = 0;
            }
        }

        smokeCounter++;
        if (smokeCounter > SMOKE_ANIM_SPEED) {
            smokeIndex = (smokeIndex + 1) % SMOKE_FRAME_COUNT;
            smokeCounter = 0;
        }
    }

    /** True if a wall or a ledge blocks the current direction of travel. */
    private boolean obstacleAhead(IPhysicsWorld world) {
        int leadX = direction > 0
            ? worldX + solidArea.x + solidArea.width + PROBE_AHEAD
            : worldX + solidArea.x - PROBE_AHEAD;
        int feetY = worldY + solidArea.y + solidArea.height;

        // Genuine wall: solid matter sampled above where a climbable step's
        // top could ever be, so a normal bump doesn't count as a wall.
        boolean wallAhead = world.getTileIdAt(leadX, feetY - STEP_CLIMB_HEIGHT) != TileID.AIR;

        // Walkable ground ahead, within one climbable step up or down.
        // Doubles as the ledge check: nothing found means a drop too deep.
        SensorResult groundAhead = world.castSensor(leadX, feetY, SensorDirection.DOWN, STEP_CLIMB_HEIGHT, GROUND_LOOK_DOWN);

        return wallAhead || !groundAhead.found;
    }
}
