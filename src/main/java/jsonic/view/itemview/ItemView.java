package jsonic.view.itemview;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import jsonic.model.item.Item;
import jsonic.utils.GameConstants;

/**
 * Base class for all per-type item views: owns the sprite(s) for one concrete Item subclass,
 * shared by every instance of that type (loaded once, in the concrete subclass's own
 * constructor - see ItemViewBinder). Subclasses implement getCurrentFrame() to pick the right
 * frame for a given Item instance; the drawing geometry itself (aspect-ratio scaling vs fixed
 * footprint, debug hitbox) is shared here, moved unchanged from the old ItemRenderer.
 */
public abstract class ItemView {

    public abstract BufferedImage getCurrentFrame(Item item);

    public void draw(Graphics2D g2, Item item, int cameraX, int cameraY, boolean debugMode) {
        BufferedImage frame = getCurrentFrame(item);
        if (frame == null) return;

        int screenX = item.worldX - cameraX;
        int screenY = item.worldY - cameraY;

        if (item.fixedFootprintWidthTiles > 0 && item.fixedFootprintHeightTiles > 0) {
            drawFixedFootprint(g2, item, frame, screenX, screenY);
        } else {
            drawStandard(g2, item, frame, screenX, screenY);
        }

        if (debugMode) drawDebugHitbox(g2, item, cameraX, cameraY);
    }

    // Fixed WxH tile footprint, stretched to fill it exactly, top-left anchored at (worldX, worldY).
    private void drawFixedFootprint(Graphics2D g2, Item item, BufferedImage frame, int screenX, int screenY) {
        int drawW = GameConstants.TILE_SIZE * item.fixedFootprintWidthTiles;
        int drawH = GameConstants.TILE_SIZE * item.fixedFootprintHeightTiles;
        if (item.facingRight) {
            g2.drawImage(frame, screenX, screenY, drawW, drawH, null);
        } else {
            g2.drawImage(frame, screenX + drawW, screenY, -drawW, drawH, null);
        }
    }

    // Scales to N tile heights (or widths, via renderWidthTiles), bottom-anchored so a taller item grows upward.
    private void drawStandard(Graphics2D g2, Item item, BufferedImage frame, int screenX, int screenY) {
        int srcW = frame.getWidth();
        int srcH = frame.getHeight();
        int drawH, drawW;
        if (item.renderWidthTiles > 0) {
            drawW = Math.round(GameConstants.TILE_SIZE * item.renderWidthTiles);
            drawH = (int) (((double) srcH / srcW) * drawW);
        } else {
            drawH = Math.round(GameConstants.TILE_SIZE * item.renderHeightTiles);
            drawW = (int) (((double) srcW / srcH) * drawH);
        }
        int offsetX = (GameConstants.TILE_SIZE - drawW) / 2; // centres within the slot even when squished by scaleX
        int scaledW = Math.max(1, Math.round(drawW * item.scaleX));
        int scaledOffsetX = offsetX + (drawW - scaledW) / 2;
        int drawY = screenY + GameConstants.TILE_SIZE - drawH;

        g2.drawImage(frame, screenX + scaledOffsetX, drawY, scaledW, drawH, null);
    }

    private void drawDebugHitbox(Graphics2D g2, Item item, int cameraX, int cameraY) {
        g2.setColor(Color.RED);
        g2.drawRect(
            item.worldX + item.solidArea.x - cameraX,
            item.worldY + item.solidArea.y - cameraY,
            item.solidArea.width,
            item.solidArea.height);
    }
}
