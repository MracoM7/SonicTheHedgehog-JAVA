package jsonic.view.itemview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.item.FlowerYellow;
import jsonic.model.item.Item;

public class FlowerYellowView extends ItemView {

    private static final int FRAME_COUNT = 2;

    private BufferedImage[] frames;

    public FlowerYellowView() {
        try {
            BufferedImage sheet = ImageIO.read(
                getClass().getResourceAsStream("/res/sprites/items/flower_yellow.png"));
            int cell = sheet.getHeight(); // square cells, packed left to right
            frames = new BufferedImage[FRAME_COUNT];
            for (int i = 0; i < FRAME_COUNT; i++) {
                frames[i] = sheet.getSubimage(i * cell, 0, cell, cell);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("FlowerYellowView: failed to load sprite from /res/sprites/items/flower_yellow.png");
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return frames[((FlowerYellow) item).spriteNum];
    }
}
