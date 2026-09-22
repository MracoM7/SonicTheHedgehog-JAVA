package jsonic.view.itemview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.item.Item;
import jsonic.model.item.Spring;

public class SpringView extends ItemView {

    private BufferedImage extendedSprite;
    private BufferedImage compressedSprite;

    public SpringView() {
        try {
            extendedSprite = ImageIO.read(getClass().getResourceAsStream(
                "/res/sprites/items/spring_yellow_extended.png"));
            compressedSprite = ImageIO.read(getClass().getResourceAsStream(
                "/res/sprites/items/spring_yellow_compressed.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("SpringView: failed to load sprites");
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        Spring spring = (Spring) item;
        return spring.extendedTimer > 0 ? extendedSprite : compressedSprite;
    }
}
