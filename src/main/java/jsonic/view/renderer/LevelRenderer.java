package jsonic.view.renderer;

import java.awt.Graphics2D;

import jsonic.view.snapshot.PlayRenderSnapshot;

/**
 * Scene rendering coordinator for PlayState and states that show the world
 * below an overlay (Pause, GameOver). Fixed draw order: parallax sky,
 * items with renderBehindVisuals (e.g. Spring, hidden behind foliage),
 * level visual art + debug overlay, remaining items, enemies, player.
 * HUD and overlays are GameView's responsibility, not this class's.
 */
public class LevelRenderer {

    private final TileRenderer tileRenderer = new TileRenderer();
    private final ItemRenderer itemRenderer = new ItemRenderer();
    private final EnemyRenderer enemyRenderer = new EnemyRenderer();
    private final PlayerRenderer playerRenderer = new PlayerRenderer();
    private final LoopRenderer loopRenderer = new LoopRenderer();

    public void draw(Graphics2D g2, PlayRenderSnapshot snap) {
        draw(g2, snap, true);
    }

    /** @param animate false freezes the player's sprite animation (Pause, Game Over). */
    public void draw(Graphics2D g2, PlayRenderSnapshot snap, boolean animate) {
        tileRenderer.drawParallax(g2, snap);
        itemRenderer.draw(g2, snap, true); // behind the visual art (e.g. Spring)
        tileRenderer.drawVisuals(g2, snap);
        loopRenderer.draw(g2, snap); // loop debug overlay (debug mode only)
        itemRenderer.draw(g2, snap, false); // everything else, in front as before
        enemyRenderer.draw(g2, snap);
        playerRenderer.draw(g2, snap, animate);
    }
}
