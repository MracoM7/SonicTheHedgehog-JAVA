package jsonic.view.itemview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.item.Item;

public class RockView extends ItemView {

    private BufferedImage sprite;

    public RockView() {
        try {
            sprite = ImageIO.read(getClass().getResourceAsStream("/res/sprites/items/rock.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("RockView: failed to load sprite from /res/sprites/items/rock.png");
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        return sprite;
    }
}
