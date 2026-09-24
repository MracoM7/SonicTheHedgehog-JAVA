package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.Item;
import jsonic.view.SpriteLoader;

public class BridgeLogView extends ItemView {

    private BufferedImage sprite;

    public BridgeLogView() {
        sprite = SpriteLoader.load(getClass(), "/res/sprites/items/bridge_log.png");
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return sprite;
    }
}
