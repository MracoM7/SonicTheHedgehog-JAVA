package jsonic.view.enemyview;

import java.awt.image.BufferedImage;

import jsonic.model.enemy.BuzzBomber;
import jsonic.model.enemy.Enemy;
import jsonic.view.SpriteLoader;

public class BuzzBomberView extends EnemyView {

    // native (pre-SCALE) offsets, matching EnemyView's draw() convention
    private static final int WING_OFFSET_X = 4;
    private static final int WING_OFFSET_Y = -4;
    private static final int EXHAUST_OFFSET_X = 0; // exhaust nub on the underside of the thorax
    private static final int EXHAUST_OFFSET_Y = 16;

    private BufferedImage idleImage;
    private BufferedImage fireImage;
    private BufferedImage[] wingFrames;
    private BufferedImage[] exhaustFrames;

    public BuzzBomberView() {
        idleImage = SpriteLoader.load(getClass(), "/res/sprites/enemies/buzzbomber_idle.png");
        fireImage = SpriteLoader.load(getClass(), "/res/sprites/enemies/buzzbomber_fire.png");
        wingFrames = new BufferedImage[] {
            SpriteLoader.load(getClass(), "/res/sprites/enemies/buzzbomber_wing1.png"),
            SpriteLoader.load(getClass(), "/res/sprites/enemies/buzzbomber_wing2.png")
        };
        exhaustFrames = new BufferedImage[] {
            SpriteLoader.load(getClass(), "/res/sprites/enemies/buzzbomber_fire1.png"),
            SpriteLoader.load(getClass(), "/res/sprites/enemies/buzzbomber_fire2.png")
        };
    }

    @Override
    public BufferedImage getCurrentFrame(Enemy enemy) {
        return ((BuzzBomber) enemy).firing ? fireImage : idleImage;
    }

    @Override
    public BufferedImage getEffectFrame(Enemy enemy) {
        return wingFrames[((BuzzBomber) enemy).wingIndex];
    }

    @Override
    public int getEffectOffsetX(Enemy enemy) { return WING_OFFSET_X; }

    @Override
    public int getEffectOffsetY(Enemy enemy) { return WING_OFFSET_Y; }

    @Override
    public BufferedImage getEffectFrame2(Enemy enemy) {
        BuzzBomber b = (BuzzBomber) enemy;
        return b.firing ? null : exhaustFrames[b.exhaustIndex];
    }

    @Override
    public int getEffectOffsetX2(Enemy enemy) { return EXHAUST_OFFSET_X; }

    @Override
    public int getEffectOffsetY2(Enemy enemy) { return EXHAUST_OFFSET_Y; }
}
