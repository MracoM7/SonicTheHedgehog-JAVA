package jsonic.model.item;

import java.awt.Rectangle;
import jsonic.utils.GameConstants;
import jsonic.model.entity.ICollector;

/**
 * Base class for every interactive object in the world. Positioned by
 * Level.loadLevel(); update() runs every frame, onCollision() fires when
 * the player touches it. Holds no sprite/image data — that lives entirely
 * in view.itemview (one ItemView per concrete subclass, looked up by
 * ItemViewBinder), so the Model package has zero dependency on AWT
 * graphics types.
 */
public class Item {

    public String name;

    // tiles tall (aspect preserved, bottom-anchored); Goal and others override it
    public float renderHeightTiles = 1f;

    // width-driven alternative to renderHeightTiles (0 = disabled); used by Spring
    public float renderWidthTiles = 0f;

    // true = draw behind the level's visual layer instead of in front; used by Spring
    public boolean renderBehindVisuals = false;

    // horizontal squish factor for ItemRenderer, 1 = normal width; used by Goal
    public float scaleX = 1f;

    // false mirrors the sprite; used by BuzzBomberShot for a leftward shot
    public boolean facingRight = true;

    // fixed WxH tile footprint, top-left anchored, overrides renderHeightTiles when set
    public int fixedFootprintWidthTiles  = 0;
    public int fixedFootprintHeightTiles = 0;

    public int worldX;
    public int worldY;

    public Rectangle solidArea = new Rectangle(0, 0, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);

    // true = onCollision() only fires when the player lands on top, not from
    // the side; see Level.checkItemCollisions(). Used by Spring and Spike.
    public boolean requireLandingFromAbove = false;

    public boolean isCollected = false;
    public boolean pendingDelete = false;

    public void update() {
        // no-op by default; subclasses override for animation
    }

    /** Called by Level on contact with the player. No-op by default. */
    public void onCollision(ICollector collector) {
    }
}
