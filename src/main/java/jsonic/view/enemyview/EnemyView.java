package jsonic.view.enemyview;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import jsonic.model.enemy.Enemy;
import jsonic.utils.GameConstants;
import jsonic.view.DebugDraw;

/**
 * Base class for all per-type enemy views: owns the sprite(s) for one concrete Enemy subclass.
 * Subclasses pick their frame via getCurrentFrame(), and override the two effect-slot methods
 * only if that badnik has an overlay sprite (Motobug's smoke, BuzzBomber's wing/exhaust).
 */
public abstract class EnemyView {

    public abstract BufferedImage getCurrentFrame(Enemy enemy);

    public BufferedImage getEffectFrame(Enemy enemy) { return null; }
    public int getEffectOffsetX(Enemy enemy) { return 0; }
    public int getEffectOffsetY(Enemy enemy) { return 0; }

    public BufferedImage getEffectFrame2(Enemy enemy) { return null; }
    public int getEffectOffsetX2(Enemy enemy) { return 0; }
    public int getEffectOffsetY2(Enemy enemy) { return 0; }

    public void draw(Graphics2D g2, Enemy enemy, int cameraX, int cameraY, boolean debugMode) {
        BufferedImage frame = getCurrentFrame(enemy);
        if (frame == null) return;

        int scale = GameConstants.SCALE;
        int drawW = frame.getWidth() * scale;
        int drawH = frame.getHeight() * scale;

        int hitboxCenterX = enemy.worldX + enemy.solidArea.x + enemy.solidArea.width / 2;
        int hitboxBottomY = enemy.worldY + enemy.solidArea.y + enemy.solidArea.height;

        int screenLeft = hitboxCenterX - cameraX - drawW / 2;
        int screenTop = hitboxBottomY - cameraY - drawH;

        if (enemy.facingRight) {
            g2.drawImage(frame, screenLeft, screenTop, drawW, drawH, null);
        } else {
            g2.drawImage(frame, screenLeft + drawW, screenTop, -drawW, drawH, null);
        }

        drawEffect(g2, getEffectFrame(enemy), getEffectOffsetX(enemy), getEffectOffsetY(enemy),
            enemy.facingRight, screenLeft, screenTop, drawW, scale);
        drawEffect(g2, getEffectFrame2(enemy), getEffectOffsetX2(enemy), getEffectOffsetY2(enemy),
            enemy.facingRight, screenLeft, screenTop, drawW, scale);

        if (debugMode) drawDebugHitbox(g2, enemy, cameraX, cameraY);
    }

    private void drawEffect(Graphics2D g2, BufferedImage effect, int offsetX, int offsetY,
                            boolean facingRight, int screenLeft, int screenTop, int drawW, int scale) {
        if (effect == null) return;

        int effW = effect.getWidth() * scale;
        int effH = effect.getHeight() * scale;
        int effX = offsetX * scale;
        int effY = offsetY * scale;

        if (facingRight) {
            g2.drawImage(effect, screenLeft + effX, screenTop + effY, effW, effH, null);
        } else {
            // Mirrors both the offset and the effect's own content, for effects that aren't left-right symmetric.
            int mirroredX = drawW - effX - effW;
            g2.drawImage(effect, screenLeft + mirroredX + effW, screenTop + effY, -effW, effH, null);
        }
    }

    private void drawDebugHitbox(Graphics2D g2, Enemy enemy, int cameraX, int cameraY) {
        g2.setColor(Color.RED);
        DebugDraw.thickRect(g2,
            enemy.worldX + enemy.solidArea.x - cameraX,
            enemy.worldY + enemy.solidArea.y - cameraY,
            enemy.solidArea.width,
            enemy.solidArea.height,
            DebugDraw.HITBOX_BORDER_THICKNESS);
    }
}
