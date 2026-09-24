package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.BridgePost;
import jsonic.model.item.Item;
import jsonic.view.SpriteLoader;

public class BridgePostView extends ItemView {

    private BufferedImage leftImage;
    private BufferedImage rightImage;

    public BridgePostView() {
        leftImage = SpriteLoader.load(getClass(), "/res/sprites/items/bridge_post_left.png");
        rightImage = SpriteLoader.load(getClass(), "/res/sprites/items/bridge_post_right.png");
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return ((BridgePost) item).isRight ? rightImage : leftImage;
    }
}
