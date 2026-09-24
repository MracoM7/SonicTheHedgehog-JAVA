package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.Item;
import jsonic.view.SpriteLoader;

public class SpikeView extends ItemView {

    private BufferedImage sprite;

    public SpikeView() {
        sprite = SpriteLoader.load(getClass(), "/res/sprites/items/spike.png");
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return sprite;
    }
}
