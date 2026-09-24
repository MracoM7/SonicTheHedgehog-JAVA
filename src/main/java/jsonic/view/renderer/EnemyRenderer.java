package jsonic.view.renderer;

import java.awt.Graphics2D;
import java.util.List;

import jsonic.model.enemy.Enemy;
import jsonic.utils.GameConstants;
import jsonic.view.enemyview.EnemyView;
import jsonic.view.enemyview.EnemyViewBinder;
import jsonic.view.snapshot.PlayRenderSnapshot;

/** Draws all visible Enemies: looks up each one's EnemyView (see EnemyViewBinder) and delegates the drawing to it. */
public class EnemyRenderer {

    public void draw(Graphics2D g2, PlayRenderSnapshot snap) {
        List<Enemy> enemies = snap.enemies;
        int cameraX = snap.cameraX;
        int cameraY = snap.cameraY;

        for (Enemy enemy : enemies) {
            if (enemy.defeated) continue;
            if (!isVisible(enemy, cameraX, cameraY)) continue;

            EnemyView view = EnemyViewBinder.getInstance().getViewFor(enemy);
            if (view == null) continue;

            view.draw(g2, enemy, cameraX, cameraY, snap.debugMode);
        }
    }

    private boolean isVisible(Enemy enemy, int cameraX, int cameraY) {
        return enemy.worldX + GameConstants.TILE_SIZE > cameraX - GameConstants.TILE_SIZE
            && enemy.worldX - GameConstants.TILE_SIZE < cameraX + GameConstants.SCREEN_WIDTH + GameConstants.TILE_SIZE
            && enemy.worldY + GameConstants.TILE_SIZE > cameraY - GameConstants.TILE_SIZE
            && enemy.worldY - GameConstants.TILE_SIZE < cameraY + GameConstants.SCREEN_HEIGHT + GameConstants.TILE_SIZE;
    }
}
