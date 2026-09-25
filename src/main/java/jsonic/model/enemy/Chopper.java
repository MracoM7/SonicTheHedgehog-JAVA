package jsonic.model.enemy;

import jsonic.model.physics.IPhysicsWorld;
import jsonic.utils.GameConstants;

/**
 * Badnik that leaps out of the water near bridges: bounces continuously under simulated velocity
 * + gravity (no closed-form arc), relaunching the instant it lands. spawnY captured lazily on
 * first update().
 */
public class Chopper extends Enemy {

    private static final float GRAVITY = (24f / 256f) * GameConstants.SCALE; // guide value, used as-is
    private static final float LAUNCH_VELOCITY = -7f * GameConstants.SCALE; // guide value, used as-is
    private static final int ANIM_FRAME_SPEED = 10;
    private static final int ANIM_FRAME_COUNT = 2; // chopper.png is a 2x32x32 strip

    private boolean initialized = false;
    private int spawnY;

    private float yPos; // precise sub-pixel position
    private float velocityY;

    private int animCounter = 0;
    public int frameIndex = 0; // read by ChopperView to pick the current frame

    public Chopper() {
        // hitbox narrower than the sprite: guide Width/Height Radius 12/16 -> 24x32 full size
        int scale = GameConstants.SCALE;
        solidArea = new java.awt.Rectangle(4 * scale, 0, 24 * scale, 32 * scale);
    }

    @Override
    public void update(IPhysicsWorld world) {
        if (!initialized) {
            spawnY = worldY;
            yPos = spawnY;
            velocityY = LAUNCH_VELOCITY;
            initialized = true;
        }

        velocityY += GRAVITY;
        yPos += velocityY;
        if (yPos >= spawnY) {
            yPos = spawnY;
            velocityY = LAUNCH_VELOCITY;
        }
        worldY = Math.round(yPos);

        animCounter++;
        if (animCounter > ANIM_FRAME_SPEED) {
            frameIndex = (frameIndex + 1) % ANIM_FRAME_COUNT;
            animCounter = 0;
        }
    }
}
