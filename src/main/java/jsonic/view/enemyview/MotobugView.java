package jsonic.view.enemyview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.model.enemy.Enemy;
import jsonic.model.enemy.Motobug;

public class MotobugView extends EnemyView {

    private static final int FRAME_SIZE = 48; // motobug.png is a 4x48x32 strip, one walk cycle
    private static final int FRAME_COUNT = 4;

    private static final int SMOKE_FRAME_SIZE = 8; // motobug_smoke.png is a 3x8x8 strip
    private static final int SMOKE_FRAME_COUNT = 3;

    // exhaust attachment point relative to the sprite's top-left corner, tuned by eye
    private static final int SMOKE_OFFSET_X = -12;
    private static final int SMOKE_OFFSET_Y = 12;

    private BufferedImage[] frames;
    private BufferedImage[] smokeFrames;

    public MotobugView() {
        try {
            BufferedImage sheet = ImageIO.read(
                getClass().getResourceAsStream("/res/sprites/enemies/motobug.png")); // faces right by default
            frames = new BufferedImage[FRAME_COUNT];
            for (int i = 0; i < FRAME_COUNT; i++) {
                frames[i] = sheet.getSubimage(i * FRAME_SIZE, 0, FRAME_SIZE, sheet.getHeight());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("MotobugView: failed to load sprite from /res/sprites/enemies/motobug.png");
        }
        try {
            BufferedImage smokeSheet = ImageIO.read(
                getClass().getResourceAsStream("/res/sprites/enemies/motobug_smoke.png"));
            smokeFrames = new BufferedImage[SMOKE_FRAME_COUNT];
            for (int i = 0; i < SMOKE_FRAME_COUNT; i++) {
                smokeFrames[i] = smokeSheet.getSubimage(i * SMOKE_FRAME_SIZE, 0, SMOKE_FRAME_SIZE, smokeSheet.getHeight());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("MotobugView: failed to load sprite from /res/sprites/enemies/motobug_smoke.png");
        }
    }

    @Override
    public BufferedImage getCurrentFrame(Enemy enemy) {
        if (frames == null) return null;
        return frames[((Motobug) enemy).frameIndex];
    }

    @Override
    public BufferedImage getEffectFrame(Enemy enemy) {
        if (smokeFrames == null) return null;
        return smokeFrames[((Motobug) enemy).smokeIndex];
    }

    @Override
    public int getEffectOffsetX(Enemy enemy) { return SMOKE_OFFSET_X; }

    @Override
    public int getEffectOffsetY(Enemy enemy) { return SMOKE_OFFSET_Y; }
}
