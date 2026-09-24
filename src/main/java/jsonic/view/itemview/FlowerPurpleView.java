package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.FlowerPurple;
import jsonic.model.item.Item;
import jsonic.view.SpriteLoader;

public class FlowerPurpleView extends ItemView {

    private BufferedImage[] frames;

    public FlowerPurpleView() {
        BufferedImage sheet = SpriteLoader.load(getClass(), "/res/sprites/items/flower_purple.png");
        if (sheet == null) return;

        int frameCount = 3; // flower_purple.png packs 3 distinct frames; PINGPONG just replays them out of order
        int cellW = sheet.getWidth() / frameCount;
        int cellH = sheet.getHeight();
        frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = sheet.getSubimage(i * cellW, 0, cellW, cellH);
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return frames[((FlowerPurple) item).getCurrentFrameIndex()];
    }
}
