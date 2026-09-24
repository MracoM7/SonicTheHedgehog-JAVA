package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.Item;
import jsonic.view.SpriteLoader;

public class RockView extends ItemView {

    private BufferedImage sprite;

    public RockView() {
        sprite = SpriteLoader.load(getClass(), "/res/sprites/items/rock.png");
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return sprite;
    }
}
