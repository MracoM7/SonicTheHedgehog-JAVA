package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.Item;
import jsonic.model.item.Spring;
import jsonic.view.SpriteLoader;

public class SpringView extends ItemView {

    private BufferedImage extendedSprite;
    private BufferedImage compressedSprite;

    public SpringView() {
        extendedSprite = SpriteLoader.load(getClass(), "/res/sprites/items/spring_yellow_extended.png");
        compressedSprite = SpriteLoader.load(getClass(), "/res/sprites/items/spring_yellow_compressed.png");
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        Spring spring = (Spring) item;
        return spring.extendedTimer > 0 ? extendedSprite : compressedSprite;
    }
}
