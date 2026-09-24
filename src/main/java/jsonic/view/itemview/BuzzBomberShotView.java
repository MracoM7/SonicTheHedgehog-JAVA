package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.BuzzBomberShot;
import jsonic.model.item.Item;
import jsonic.view.SpriteLoader;

public class BuzzBomberShotView extends ItemView {

    private static final int FRAME_COUNT = 4;
    private static final int FRAME_SIZE = 16; // buzzbomber_shot.png is a 4x16x16 strip

    private BufferedImage[] frames;

    public BuzzBomberShotView() {
        BufferedImage sheet = SpriteLoader.load(getClass(), "/res/sprites/items/buzzbomber_shot.png");
        if (sheet == null) return;

        frames = new BufferedImage[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = sheet.getSubimage(i * FRAME_SIZE, 0, FRAME_SIZE, sheet.getHeight());
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return frames[((BuzzBomberShot) item).frameIndex];
    }
}
