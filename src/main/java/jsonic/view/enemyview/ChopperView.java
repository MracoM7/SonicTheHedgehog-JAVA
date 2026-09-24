package jsonic.view.enemyview;

import java.awt.image.BufferedImage;

import jsonic.model.enemy.Chopper;
import jsonic.model.enemy.Enemy;
import jsonic.view.SpriteLoader;

public class ChopperView extends EnemyView {

    private static final int FRAME_SIZE = 32; // chopper.png is a 2x32x32 strip
    private static final int FRAME_COUNT = 2;

    private BufferedImage[] frames;

    public ChopperView() {
        BufferedImage sheet = SpriteLoader.load(getClass(), "/res/sprites/enemies/chopper.png");
        if (sheet == null) return;

        frames = new BufferedImage[FRAME_COUNT];
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = sheet.getSubimage(i * FRAME_SIZE, 0, FRAME_SIZE, sheet.getHeight());
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Enemy enemy) {
        if (frames == null) return null;
        return frames[((Chopper) enemy).frameIndex];
    }
}
