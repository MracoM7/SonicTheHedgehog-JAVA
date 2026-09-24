package jsonic.model.item;

import java.awt.Rectangle;

import jsonic.model.entity.ICollector;
import jsonic.utils.GameConstants;

/**
 * BuzzBomber's projectile: flies straight in the direction it was fired,
 * damages the player on contact, and disappears after LIFESPAN_FRAMES if it
 * never hits one. Passes straight through terrain — the original game's
 * own behaviour, not a simplification. Sprite frames live in
 * view.itemview.BuzzBomberShotView.
 */
public class BuzzBomberShot extends Item {

    private static final int SPEED_X = 2 * GameConstants.SCALE;
    private static final int SPEED_Y = 2 * GameConstants.SCALE; // flies diagonally toward the player
    private static final int LIFESPAN_FRAMES = 120; // 2s @ 60fps fallback if it never hits the player
    private static final int ANIM_FRAME_SPEED = 8; // ticks per frame

    private final int direction; // -1 = left, +1 = right
    private int lifespan = LIFESPAN_FRAMES;

    private int animCounter = 0;
    public int frameIndex = 0;

    public BuzzBomberShot(int worldX, int worldY, int direction) {
        this.direction = direction;
        this.worldX = worldX;
        this.worldY = worldY;

        name = "BuzzBomberShot";
        facingRight = direction > 0;

        // fixed 1x1 footprint: renders at its exact flight position instead
        // of ItemRenderer's "standing on a tile" bottom-anchoring
        fixedFootprintWidthTiles = 1;
        fixedFootprintHeightTiles = 1;
        solidArea = new Rectangle(0, 0, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);
    }

    @Override
    public void update() {
        worldX += direction * SPEED_X;
        worldY += SPEED_Y;

        animCounter++;
        if (animCounter > ANIM_FRAME_SPEED) {
            // Frames 0 and 1 (muzzle spark) each show once at the start;
            // once past them, loop between just the last two frames.
            frameIndex = switch (frameIndex) {
                case 0  -> 1;
                case 1  -> 2;
                case 2  -> 3;
                default -> 2;
            };
            animCounter = 0;
        }

        if (--lifespan <= 0) pendingDelete = true;
    }

    @Override
    public void onCollision(ICollector collector) {
        pendingDelete = true;
        collector.takeDamage(1);
    }
}
