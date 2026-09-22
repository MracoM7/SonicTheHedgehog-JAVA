package jsonic.view.renderer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import jsonic.utils.GameConstants;
import jsonic.model.tile.TileID;
import jsonic.model.tile.TileMap;
import jsonic.model.tile.TileType;
import jsonic.view.snapshot.PlayRenderSnapshot;

/** Draws the level map: visual background, parallax and debug overlay. Reads everything from the snapshot, no persistent Model reference. */
public class TileRenderer {

    private final ParallaxRenderer parallaxRenderer = new ParallaxRenderer();

    // draw

    /** Parallax skybox, drawn before anything else so items can sit behind the level's visual art. */
    public void drawParallax(Graphics2D g2, PlayRenderSnapshot snap) {
        parallaxRenderer.draw(g2, snap.config.backgroundsFolder, snap.cameraX, snap.timeFrames);
    }

    /**
     * Level's visual foreground art plus the debug tile overlay. Scaled from the image's own
     * native size, not the fixed world constants, so a smaller level's art doesn't stretch past its real extent.
     */
    public void drawVisuals(Graphics2D g2, PlayRenderSnapshot snap) {
        if (snap.backgroundImage != null) {
            drawBackgroundImage(g2, snap.backgroundImage, snap.cameraX, snap.cameraY);
        }

        if (snap.tileMap.isDebugMode()) {
            drawDebugOverlay(g2, snap.tileMap, snap.cameraX, snap.cameraY);
        }
    }

    private static final int CHUNK_WIDTH = 2048; // comfortably under common GPU texture-size limits (4096/8192)

    private BufferedImage chunkedSource; // identity check: which image chunks[] was built from
    private BufferedImage[] chunks;

    /**
     * Draws only the on-screen slice, from pre-split chunks instead of the original 9088px-wide
     * image directly — that went blank in fullscreen past a few thousand pixels of scroll, since
     * the art exceeds the texture-size limit some GPUs enforce.
     */
    private void drawBackgroundImage(Graphics2D g2, BufferedImage image, int cameraX, int cameraY) {
        int scale = GameConstants.SCALE;
        BufferedImage[] imgChunks = getChunks(image);

        // Visible slice, in the image's own native (unscaled) pixels.
        int srcX1 = Math.max(0, cameraX / scale);
        int srcY1 = Math.max(0, cameraY / scale);
        int srcX2 = Math.min(image.getWidth(), (cameraX + GameConstants.SCREEN_WIDTH) / scale + 1);
        int srcY2 = Math.min(image.getHeight(), (cameraY + GameConstants.SCREEN_HEIGHT) / scale + 1);
        if (srcX1 >= srcX2 || srcY1 >= srcY2) return; // camera fully outside the art

        int firstChunk = srcX1 / CHUNK_WIDTH;
        int lastChunk = (srcX2 - 1) / CHUNK_WIDTH;

        for (int i = firstChunk; i <= lastChunk; i++) {
            BufferedImage chunk = imgChunks[i];
            int chunkOffsetX = i * CHUNK_WIDTH;

            // This chunk's share of the visible slice, in the chunk's own local pixels.
            int cSrcX1 = Math.max(srcX1, chunkOffsetX) - chunkOffsetX;
            int cSrcX2 = Math.min(srcX2, chunkOffsetX + chunk.getWidth()) - chunkOffsetX;

            int dstX1 = (chunkOffsetX + cSrcX1) * scale - cameraX;
            int dstY1 = srcY1 * scale - cameraY;
            int dstX2 = (chunkOffsetX + cSrcX2) * scale - cameraX;
            int dstY2 = srcY2 * scale - cameraY;

            g2.drawImage(chunk, dstX1, dstY1, dstX2, dstY2, cSrcX1, srcY1, cSrcX2, srcY2, null);
        }
    }

    /** Splits the art into independent CHUNK_WIDTH-wide images, cached per distinct source. */
    private BufferedImage[] getChunks(BufferedImage source) {
        if (source == chunkedSource) return chunks;

        chunkedSource = source;
        int w = source.getWidth(), h = source.getHeight();
        int count = (w + CHUNK_WIDTH - 1) / CHUNK_WIDTH;
        chunks = new BufferedImage[count];
        for (int i = 0; i < count; i++) {
            int x = i * CHUNK_WIDTH;
            int cw = Math.min(CHUNK_WIDTH, w - x);
            BufferedImage chunk = new BufferedImage(cw, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = chunk.createGraphics();
            cg.drawImage(source, -x, 0, null);
            cg.dispose();
            chunks[i] = chunk;
        }
        return chunks;
    }

    // debug overlay

    private void drawDebugOverlay(Graphics2D g2, TileMap tileMap,
            int cameraX, int cameraY) {
        int[][] map = tileMap.getMapTileNum();

        for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
            for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                int tileID = map[col][row];
                if (tileID == TileID.AIR)
                    continue;

                int worldX = col * GameConstants.TILE_SIZE;
                int worldY = row * GameConstants.TILE_SIZE;

                // Frustum culling: skip tiles outside the viewport
                if (!isVisible(worldX, worldY, cameraX, cameraY))
                    continue;

                int screenX = worldX - cameraX;
                int screenY = worldY - cameraY;

                g2.setColor(TileType.getDebugColor(tileID));
                drawTileShape(g2, tileID, screenX, screenY);
            }
        }
    }

    /**
     * Draws the tile's physical shape using the same heightmap as the physics engine.
     * "What you see = what the player feels under their feet."
     */
    private void drawTileShape(Graphics2D g2, int tileID, int screenX, int screenY) {
        final int TS = GameConstants.TILE_SIZE;

        switch (tileID) {
            case TileID.FULL:
            case TileID.THREE_FOUR:
            case TileID.HALF:
            case TileID.ONE_FOUR:
                int topOffset = TileType.getSolidTopOffset(tileID);
                g2.fillRect(screenX, screenY + topOffset, TS, TS - topOffset);
                break;

            default:
                int[] hm = TileType.getHeightmap(tileID);
                if (hm == null) {
                    g2.fillRect(screenX, screenY, TS, TS);
                    return;
                }
                for (int lx = 0; lx < TS; lx++) {
                    int surfaceOffset = hm[lx];
                    int drawH = TS - surfaceOffset;
                    if (drawH > 0) {
                        g2.fillRect(screenX + lx, screenY + surfaceOffset, 1, drawH);
                    }
                }
                break;
        }
    }

    private boolean isVisible(int worldX, int worldY, int cameraX, int cameraY) {
        return worldX + GameConstants.TILE_SIZE > cameraX - GameConstants.TILE_SIZE
                && worldX - GameConstants.TILE_SIZE < cameraX + GameConstants.SCREEN_WIDTH + GameConstants.TILE_SIZE
                && worldY + GameConstants.TILE_SIZE > cameraY - GameConstants.TILE_SIZE
                && worldY - GameConstants.TILE_SIZE < cameraY + GameConstants.SCREEN_HEIGHT + GameConstants.TILE_SIZE;
    }
}
