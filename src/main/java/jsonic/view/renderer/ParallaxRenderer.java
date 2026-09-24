package jsonic.view.renderer;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import jsonic.utils.GameConstants;
import jsonic.view.SpriteLoader;

/**
 * Draws a level's parallax backdrop: 4 horizontal bands (sky, mountains, hills, water) from
 * that level's own backgrounds/ folder, each scrolling horizontally at its own fraction of
 * camera speed — farther layers move less. Only X parallaxes; each band sits at a fixed Y.
 */
public class ParallaxRenderer {

    // Water shimmer: cycles through 4 hand-offset frames to fake the original's palette-cycling sparkle.
    private static final int WATER_FRAME_TICKS = 8;

    private static final class Layer {
        final BufferedImage[] frames;
        final int frameTicks; // 0 = static, no cycling
        final float scrollFactor; // 0 = fixed (infinitely far), 1 = moves with the camera
        final int screenY;

        Layer(BufferedImage image, float scrollFactor, int screenY) {
            this(new BufferedImage[] { image }, 0, scrollFactor, screenY);
        }

        Layer(BufferedImage[] frames, int frameTicks, float scrollFactor, int screenY) {
            this.frames = frames;
            this.frameTicks = frameTicks;
            this.scrollFactor = scrollFactor;
            this.screenY = screenY;
        }

        BufferedImage currentFrame(int animTimer) {
            if (frames.length <= 1 || frameTicks <= 0) return frames[0];
            int idx = (animTimer / frameTicks) % frames.length;
            return frames[idx];
        }
    }

    private Layer[] layers = new Layer[0];
    private String loadedFolder; // which backgroundsFolder `layers` currently holds, or null for none

    // (Re)loads the 4 layers from a level's backgrounds folder - a no-op if already loaded.
    private void loadForLevel(String folder) {
        if (java.util.Objects.equals(folder, loadedFolder)) return;
        loadedFolder = folder;

        if (folder == null) {
            layers = new Layer[0];
            return;
        }

        layers = new Layer[] {
            new Layer(load(folder, "sky.png"), 0.05f, 0),
            new Layer(load(folder, "mountains.png"), 0.15f, 96),
            new Layer(load(folder, "hills.png"), 0.40f, 240),
            // Ordered by wrap-around shift, not filename, or the shimmer jumps backwards mid-cycle.
            new Layer(
                new BufferedImage[] {
                    load(folder, "water.png"), load(folder, "water2.png"),
                    load(folder, "water3.png"), load(folder, "water1.png")
                },
                WATER_FRAME_TICKS, 0.60f, 360),
        };
    }

    private BufferedImage load(String folder, String fileName) {
        return SpriteLoader.load(getClass(), folder + fileName);
    }

    // A few extra pixels of height closes the pixel-wide seam fullscreen's letterbox scaling can round into.
    private static final int OVERLAP_PX = 3;

    public void draw(Graphics2D g2, String backgroundsFolder, int cameraX, int animTimer) {
        loadForLevel(backgroundsFolder);

        for (Layer layer : layers) {
            BufferedImage image = layer.currentFrame(animTimer);
            if (image == null) continue;

            int scaledW = image.getWidth() * GameConstants.SCALE;
            int scaledH = image.getHeight() * GameConstants.SCALE + OVERLAP_PX;

            // cameraX is always >= 0 (Camera clamps it), so this is always in
            // (-scaledW, 0] — exactly where the tiling loop below needs to start.
            int scrollX = (int) (cameraX * layer.scrollFactor);
            int startX = -(scrollX % scaledW);

            for (int x = startX; x < GameConstants.SCREEN_WIDTH; x += scaledW) {
                g2.drawImage(image, x, layer.screenY, scaledW, scaledH, null);
            }
        }
    }
}
