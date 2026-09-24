package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.FlowerYellow;
import jsonic.model.item.Item;
import jsonic.view.SpriteLoader;

public class FlowerYellowView extends ItemView {

    private static final int FRAME_COUNT = 2;

    private BufferedImage[] frames;

    public FlowerYellowView() {
        BufferedImage sheet = SpriteLoader.load(getClass(), "/res/sprites/items/flower_yellow.png");
        if (sheet == null) return;

        int cell = sheet.getHeight(); // square cells, packed left to right
        frames = new BufferedImage[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = sheet.getSubimage(i * cell, 0, cell, cell);
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return frames[((FlowerYellow) item).spriteNum];
    }
}
