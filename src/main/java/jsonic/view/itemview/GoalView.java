package jsonic.view.itemview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.item.Goal;
import jsonic.model.item.Item;

public class GoalView extends ItemView {

    private BufferedImage robotnikImage;
    private BufferedImage sonicImage;

    public GoalView() {
        robotnikImage = loadSprite("/res/sprites/items/goal.png");
        sonicImage = loadSprite("/res/sprites/items/goal_sonic.png");
    }

    private BufferedImage loadSprite(String path) {
        try {
            return ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("GoalView: failed to load sprite from " + path);
            return null;
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        Goal goal = (Goal) item;
        return goal.isShowingRobotnik() ? robotnikImage : sonicImage;
    }
}
