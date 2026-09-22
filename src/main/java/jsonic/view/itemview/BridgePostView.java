package jsonic.view.itemview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.item.BridgePost;
import jsonic.model.item.Item;

public class BridgePostView extends ItemView {

    private BufferedImage leftImage;
    private BufferedImage rightImage;

    public BridgePostView() {
        leftImage = loadSprite("/res/sprites/items/bridge_post_left.png");
        rightImage = loadSprite("/res/sprites/items/bridge_post_right.png");
    }

    private BufferedImage loadSprite(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("BridgePostView: failed to load sprite from " + path);
            return null;
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return ((BridgePost) item).isRight ? rightImage : leftImage;
    }
}
