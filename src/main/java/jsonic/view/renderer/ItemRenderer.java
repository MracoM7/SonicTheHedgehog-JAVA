package jsonic.view.renderer;

import java.awt.Graphics2D;
import java.util.List;

import jsonic.utils.GameConstants;
import jsonic.model.item.Item;
import jsonic.view.itemview.ItemView;
import jsonic.view.itemview.ItemViewBinder;
import jsonic.view.snapshot.PlayRenderSnapshot;

/** Draws all visible Items: looks up each one's ItemView (see ItemViewBinder) and delegates the drawing to it. */
public class ItemRenderer {

    /** @param behindVisuals draws only items whose renderBehindVisuals matches this value. */
    public void draw(Graphics2D g2, PlayRenderSnapshot snap, boolean behindVisuals) {
        List<Item> items = snap.items;
        int cameraX = snap.cameraX;
        int cameraY = snap.cameraY;

        for (Item item : items) {
            if (item.pendingDelete) continue;
            if (item.renderBehindVisuals != behindVisuals) continue;
            if (!isVisible(item, cameraX, cameraY)) continue;

            ItemView view = ItemViewBinder.getInstance().getViewFor(item);
            if (view == null) continue;

            view.draw(g2, item, cameraX, cameraY, snap.debugMode);
        }
    }

    private boolean isVisible(Item item, int cameraX, int cameraY) {
        int margin = GameConstants.TILE_SIZE * 3; // covers the largest fixed footprint in use (Flowers)
        return item.worldX + margin > cameraX - margin
            && item.worldX - margin < cameraX + GameConstants.SCREEN_WIDTH + margin
            && item.worldY + margin > cameraY - margin
            && item.worldY - margin < cameraY + GameConstants.SCREEN_HEIGHT + margin;
    }
}
