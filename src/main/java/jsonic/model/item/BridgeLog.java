package jsonic.model.item;

import jsonic.model.entity.ICameraTarget;
import jsonic.utils.GameConstants;

/**
 * One log of a Bridge (see TileID.BRIDGE_LEFT_POST/BRIDGE_RIGHT_POST).
 * Purely visual sag: the row is solid FULL ground (Level.spawnBridge()),
 * this log only draws lower the closer the player is horizontally,
 * tapering to flat within SAG_SPREAD tiles. Recomputed every frame from
 * the player's live X, nothing persisted.
 *
 * Takes ICameraTarget instead of Player so this package doesn't depend
 * on the entity package's concrete classes. Static sprite — see
 * view.itemview.BridgeLogView.
 */
public class BridgeLog extends Item {

    private static final float MAX_SAG = 4f * GameConstants.SCALE; // px of dip under the player
    private static final float SAG_SPREAD = 3f; // tile-widths either side

    private final ICameraTarget target;
    private final int restY;

    public BridgeLog(ICameraTarget target, int worldX, int worldY) {
        this.target = target;
        this.worldX = worldX;
        this.worldY = worldY;
        this.restY = worldY;
        name = "BridgeLog";
    }

    @Override
    public void update() {
        float tilesFromPlayer = Math.abs(target.getX() - worldX) / GameConstants.TILE_SIZE;
        float sagAmount = Math.max(0f, 1f - tilesFromPlayer / SAG_SPREAD);
        worldY = restY + Math.round(MAX_SAG * sagAmount);
    }

    /** Current sag below rest position; lets PlayerRenderer sink Sonic by the same amount. */
    public int getSagOffset() { return worldY - restY; }
}
