package jsonic.model.item;

import java.awt.Rectangle;
import jsonic.utils.GameConstants;
import jsonic.model.entity.ICollector;

/**
 * Collectible ring.
 *
 * Two animations:
 *   NORMAL : 4-frame loop while in the world
 *   SPARK  : 4 fast frames after collection, then pendingDelete = true
 *
 * Sprite frames live in view.itemview.RingView, shared by every Ring
 * instance — this class only tracks which frame (spriteNum) is current.
 */
public class Ring extends Item {

    // animation constants

    private static final int NORMAL_ANIM_SPEED = 10;
    private static final int SPARK_ANIM_SPEED = 4;
    private static final int FRAME_COUNT = 4;

    private int spriteCounter = 0;
    public int spriteNum = 0;

    // constructor

    public Ring() {
        name = "Ring";

        // half-tile hitbox, centred in the full tile footprint
        int ts = GameConstants.TILE_SIZE;
        solidArea = new Rectangle(ts / 4, ts / 4, ts / 2, ts / 2);
    }

    // update

    @Override
    public void update() {
        updateAnimation();
    }

    private void updateAnimation() {
        if (pendingDelete) return;

        spriteCounter++;

        if (!isCollected) {
            if (spriteCounter > NORMAL_ANIM_SPEED) {
                spriteNum = (spriteNum + 1) % FRAME_COUNT;
                spriteCounter = 0;
            }
        } else {
            if (spriteCounter > SPARK_ANIM_SPEED) {
                spriteNum++;
                spriteCounter = 0;
                if (spriteNum >= FRAME_COUNT) {
                    pendingDelete = true;
                    return;
                }
            }
        }
    }

    // collision

    /** Adds a ring to the collector and starts the spark animation. */
    @Override
    public void onCollision(ICollector collector) {
        if (isCollected) return; // guard against multiple collisions in the same frame

        collector.addRing();
        isCollected = true;

        spriteNum = 0;
        spriteCounter = 0;
    }
}
