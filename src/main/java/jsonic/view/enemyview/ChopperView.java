package jsonic.view.enemyview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.enemy.Chopper;
import jsonic.model.enemy.Enemy;

public class ChopperView extends EnemyView {

    private static final int FRAME_SIZE = 32; // chopper.png is a 2x32x32 strip
    private static final int FRAME_COUNT = 2;

    private BufferedImage[] frames;

    public ChopperView() {
        try {
            BufferedImage sheet = ImageIO.read(
                getClass().getResourceAsStream("/res/sprites/enemies/chopper.png"));
            frames = new BufferedImage[FRAME_COUNT];
            for (int i = 0; i < FRAME_COUNT; i++) {
                frames[i] = sheet.getSubimage(i * FRAME_SIZE, 0, FRAME_SIZE, sheet.getHeight());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ChopperView: failed to load sprite from /res/sprites/enemies/chopper.png");
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Enemy enemy) {
        if (frames == null) return null;
        return frames[((Chopper) enemy).frameIndex];
    }
}
