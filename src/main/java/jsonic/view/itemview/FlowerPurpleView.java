package jsonic.view.itemview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.item.FlowerPurple;
import jsonic.model.item.Item;

public class FlowerPurpleView extends ItemView {

    private BufferedImage[] frames;

    public FlowerPurpleView() {
        try {
            BufferedImage sheet = ImageIO.read(
                getClass().getResourceAsStream("/res/sprites/items/flower_purple.png"));
            int frameCount = 3; // flower_purple.png packs 3 distinct frames; PINGPONG just replays them out of order
            int cellW = sheet.getWidth() / frameCount;
            int cellH = sheet.getHeight();
            frames = new BufferedImage[frameCount];
            for (int i = 0; i < frameCount; i++) {
                frames[i] = sheet.getSubimage(i * cellW, 0, cellW, cellH);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("FlowerPurpleView: failed to load sprite from /res/sprites/items/flower_purple.png");
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return frames[((FlowerPurple) item).getCurrentFrameIndex()];
    }
}
