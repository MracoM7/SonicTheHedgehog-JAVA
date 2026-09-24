package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.Item;
import jsonic.model.item.Ring;
import jsonic.view.SpriteLoader;

/** Shared by Ring and ScatteredRing (same sprite/animation, see ItemViewBinder). */
public class RingView extends ItemView {

    // ring.png is a single strip of all 8 frames (4 spin + 4 spark), each its own native
    // width — the mid-spin frame is edge-on and narrower than the rest, hence a per-frame
    // width array instead of a fixed size.
    private static final int[] FRAME_WIDTHS = {16, 16, 8, 16, 16, 16, 16, 16};

    private BufferedImage[] ringFrames;
    private BufferedImage[] sparkFrames;

    public RingView() {
        loadSprites();
    }

    private void loadSprites() {
        BufferedImage sheet = SpriteLoader.load(getClass(), "/res/sprites/items/ring.png");
        if (sheet == null) return;

        BufferedImage[] frames = new BufferedImage[FRAME_WIDTHS.length];
        int x = 0;
        for (int i = 0; i < FRAME_WIDTHS.length; i++) {
            frames[i] = sheet.getSubimage(x, 0, FRAME_WIDTHS[i], sheet.getHeight());
            x += FRAME_WIDTHS[i];
        }

        ringFrames = new BufferedImage[] { frames[0], frames[1], frames[2], frames[3] };
        sparkFrames = new BufferedImage[] { frames[4], frames[5], frames[6], frames[7] };
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        Ring ring = (Ring) item;
        return ring.isCollected ? sparkFrames[ring.spriteNum] : ringFrames[ring.spriteNum];
    }
}
