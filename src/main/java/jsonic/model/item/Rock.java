package jsonic.model.item;

import java.awt.Rectangle;
import jsonic.utils.GameConstants;

/**
 * Green Hill Zone rock: a solid obstacle, no animation, onCollision() is
 * Item's no-op default. Blocking comes from the FULL tile block TileMap
 * marks in Level.spawnSolidObstacle(). Static sprite — see view.itemview.RockView.
 */
public class Rock extends Item {

    public Rock() {
        name = "Rock";
        renderHeightTiles = 2; // native 48x32 sprite -> 3x2 tiles once scaled up

        // not used for collision (TileMap blocks the player), only so the
        // debug hitbox shows the real 3x2 footprint spawnSolidObstacle() marks
        int ts = GameConstants.TILE_SIZE;
        solidArea = new Rectangle(-ts, -ts, ts * 3, ts * 2);
    }
}
