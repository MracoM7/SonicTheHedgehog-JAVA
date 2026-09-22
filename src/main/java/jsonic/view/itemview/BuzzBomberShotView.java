package jsonic.view.itemview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.item.BuzzBomberShot;
import jsonic.model.item.Item;

public class BuzzBomberShotView extends ItemView {

    private static final int FRAME_COUNT = 4;
    private static final int FRAME_SIZE = 16; // buzzbomber_shot.png is a 4x16x16 strip

    private BufferedImage[] frames;

    public BuzzBomberShotView() {
        try {
            BufferedImage sheet = ImageIO.read(
                getClass().getResourceAsStream("/res/sprites/items/buzzbomber_shot.png"));
            frames = new BufferedImage[FRAME_COUNT];
            for (int i = 0; i < FRAME_COUNT; i++) {
                frames[i] = sheet.getSubimage(i * FRAME_SIZE, 0, FRAME_SIZE, sheet.getHeight());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("BuzzBomberShotView: failed to load sprite from /res/sprites/items/buzzbomber_shot.png");
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return frames[((BuzzBomberShot) item).frameIndex];
    }
}
