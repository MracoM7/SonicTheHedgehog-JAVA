package jsonic.model.enemy;

import java.awt.Rectangle;
import jsonic.utils.GameConstants;
import jsonic.model.physics.IPhysicsWorld;

/**
 * Base class for all badniks - shares Item's shape (worldX/Y, solidArea) but is its own hierarchy,
 * since an enemy is fought (stomp from above, hurt from any other angle) instead of collected; see
 * Level.checkEnemyCollisions(). Holds no sprite/image data - that lives entirely in view.enemyview
 * (one EnemyView per concrete subclass, looked up by EnemyViewBinder).
 */
public abstract class Enemy {

    public int worldX;
    public int worldY;

    public Rectangle solidArea = new Rectangle(0, 0, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);

    public boolean defeated = false;
    public boolean facingRight = true;

    /** Called once per frame while not defeated. */
    public abstract void update(IPhysicsWorld world);
}
