package jsonic.view.itemview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.item.Item;

public class SpikeView extends ItemView {

    private BufferedImage sprite;

    public SpikeView() {
        try {
            sprite = ImageIO.read(getClass().getResourceAsStream("/res/sprites/items/spike.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("SpikeView: failed to load sprite from /res/sprites/items/spike.png");
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return sprite;
    }
}
