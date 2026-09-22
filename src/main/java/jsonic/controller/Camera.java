package jsonic.controller;

import jsonic.model.entity.ICameraTarget;
import jsonic.utils.GameConstants;

/**
 * Computes the viewport position every frame from the player's position:
 * follows X directly, follows Y through a tracker with a deadzone (so small
 * jumps don't move the camera), and pans up/down when looking up or
 * crouching. Depends on ICameraTarget rather than the concrete Player so
 * it could follow any entity (e.g. a cutscene).
 */
public class Camera {

    // current position (top-left corner of the viewport)
    private float x;
    private float y;

    // invisible vertical tracker: moves only once the target exits the
    // deadzone, dampening small vertical jumps (e.g. landing on a low step)
    private float trackerY = 0f;

    // behaviour constants - hand-tuned for this project, not taken from the SPG (its camera is a
    // structurally different border/vertical-focus model)
    private static final float PAN_SPEED = 4.0f; // pixels/frame during panning
    private static final int DEADZONE_HEIGHT = GameConstants.TILE_SIZE; // half-height of the vertical deadzone

    // dimensions
    private final int screenWidth;
    private final int screenHeight;
    private int worldWidth;
    private int worldHeight;

    // panning state
    private boolean isPanningUp = false;
    private boolean isPanningDown = false;
    private float panTargetY = 0f;

    private boolean initialized = false; // avoids a jump on the first frame

    // constructor
    public Camera(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void setWorldDimensions(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    // update

    /** Updates the Camera position for one frame; target is usually the Player. */
    public void update(ICameraTarget target) {
        if (!initialized) {
            snapToTarget(target);
        }

        // death fall: freeze instead of following, so the body visibly falls off screen
        // instead of staying centred forever
        if (target.isDying()) return;

        // 1. x axis: follows the target directly
        this.x = target.getX() - (screenWidth / 2f);

        // 2. vertical tracker with deadzone
        float targetCamY = target.getY() - (screenHeight / 2f);
        if (targetCamY > trackerY + DEADZONE_HEIGHT) trackerY = targetCamY - DEADZONE_HEIGHT;
        else if (targetCamY < trackerY - DEADZONE_HEIGHT) trackerY = targetCamY + DEADZONE_HEIGHT;

        // 3. vertical panning
        boolean pressingUp = target.isLookingUp();
        boolean pressingDown = target.isCurlingUp() && target.getVelocityX() == 0;

        if (pressingUp) {
            if (!isPanningUp) {
                // stop a tile below the feet, not right at them
                float targetFeetY = target.getY() + target.getCurrentRadiusY();
                panTargetY = targetFeetY + GameConstants.TILE_SIZE - screenHeight;
                isPanningUp = true;
                isPanningDown = false;
            }
            if (this.y > panTargetY) {
                this.y -= PAN_SPEED;
                if (this.y < panTargetY) this.y = panTargetY;
            }

        } else if (pressingDown) {
            if (!isPanningDown) {
                // stop a tile above Sonic, or a tile short of the level bottom if that comes first
                panTargetY = Math.min(target.getY() - GameConstants.TILE_SIZE, worldHeight - screenHeight - GameConstants.TILE_SIZE);
                isPanningDown = true;
                isPanningUp = false;
            }
            if (this.y < panTargetY) {
                this.y += PAN_SPEED;
                if (this.y > panTargetY) this.y = panTargetY;
            }

        } else {
            // No panning: returns to the neutral position, never slower than the target's own
            // vertical speed (a fixed rate alone can't keep up with a fast launch, e.g. a Spring)
            isPanningUp = false;
            isPanningDown = false;

            float catchUpSpeed = Math.max(PAN_SPEED * 2, Math.abs(target.getVelocityY()));
            if (this.y > trackerY) {
                this.y -= catchUpSpeed;
                if (this.y < trackerY) this.y = trackerY;
            } else if (this.y < trackerY) {
                this.y += catchUpSpeed;
                if (this.y > trackerY) this.y = trackerY;
            }
        }

        // 4. clamping to world edges
        this.x = clamp(this.x, 0, worldWidth - screenWidth);
        this.y = clamp(this.y, 0, worldHeight - screenHeight);
    }

    /**
     * Hard-teleports the camera onto target with no lag/deadzone — the same
     * framing used for the very first frame of the game, now also reusable
     * whenever the player itself gets teleported (respawn after death), so
     * the view doesn't glide in from wherever it was when the player died.
     */
    public void snapToTarget(ICameraTarget target) {
        x = target.getX() - (screenWidth / 2f);
        y = target.getY() - (screenHeight / 2f);
        trackerY = y;
        initialized = true;
        isPanningUp = false;
        isPanningDown = false;
        x = clamp(x, 0, worldWidth - screenWidth);
        y = clamp(y, 0, worldHeight - screenHeight);
    }

    // utility

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    // getters

    public float getX() { return x; }
    public float getY() { return y; }
}
