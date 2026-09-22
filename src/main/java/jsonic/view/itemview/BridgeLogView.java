package jsonic.view.itemview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.item.Item;

public class BridgeLogView extends ItemView {

    private BufferedImage sprite;

    public BridgeLogView() {
        try {
            sprite = ImageIO.read(getClass().getResourceAsStream("/res/sprites/items/bridge_log.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("BridgeLogView: failed to load sprite from /res/sprites/items/bridge_log.png");
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return sprite;
    }
}
