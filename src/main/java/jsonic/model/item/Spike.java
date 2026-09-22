package jsonic.model.item;

import java.awt.Rectangle;
import jsonic.utils.GameConstants;
import jsonic.model.entity.ICollector;

/**
 * Hazard: spikes. Solid like Rock (see Level.spawnSolidObstacle()) but also
 * hurts the player if they land on it from above (requireLandingFromAbove).
 *
 * Unlike Ring, onCollision() never sets isCollected/pendingDelete — a spike
 * never gets "used up". It fires every frame the player rests on top (see
 * checkItemCollisions()'s isOnGround() check), so bounce() unconditionally
 * shoves the player off even while takeDamage() itself is a no-op during
 * invulnerability — never a safe place to just stand.
 *
 * Static sprite, no animation — see view.itemview.SpikeView.
 */
public class Spike extends Item {

    public Spike() {
        name = "Spike";
        renderHeightTiles = 2f; // same height as Rock; width follows its own aspect ratio
        requireLandingFromAbove = true;

        // matches the solid FULL block spawnSolidObstacle() marks, so the
        // "landed on top" check and the debug hitbox reflect the real footprint
        int ts = GameConstants.TILE_SIZE;
        solidArea = new Rectangle(-ts, -ts, ts * 3, ts * 2);
    }

    @Override
    public void onCollision(ICollector collector) {
        // bounce() first and unconditionally: takeDamage() alone no-ops
        // while invulnerable, which would otherwise let the player stand
        // on this solid hazard undisturbed until invulnerability wears off.
        collector.bounce();
        collector.takeDamage(1);
    }
}
