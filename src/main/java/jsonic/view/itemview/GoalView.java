package jsonic.view.itemview;

import java.awt.image.BufferedImage;

import jsonic.model.item.Goal;
import jsonic.model.item.Item;
import jsonic.view.SpriteLoader;

public class GoalView extends ItemView {

    private BufferedImage robotnikImage;
    private BufferedImage sonicImage;

    public GoalView() {
        robotnikImage = SpriteLoader.load(getClass(), "/res/sprites/items/goal.png");
        sonicImage = SpriteLoader.load(getClass(), "/res/sprites/items/goal_sonic.png");
    }

    @Override
    public BufferedImage getCurrentFrame(Item item) {
        Goal goal = (Goal) item;
        return goal.isShowingRobotnik() ? robotnikImage : sonicImage;
    }
}
