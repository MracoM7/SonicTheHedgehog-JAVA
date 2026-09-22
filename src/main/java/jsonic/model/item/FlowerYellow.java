package jsonic.model.item;

/**
 * Purely decorative Green Hill Zone flower: alternates between its 2
 * traced frames to read as spinning (the original cycles more rotation
 * frames; only 2 are traced here). Never interacts with the player.
 * Sprite frames live in view.itemview.FlowerYellowView.
 */
public class FlowerYellow extends Item {

    private static final int FRAME_COUNT = 2;
    private static final int SPIN_FRAME_HOLD = 8; // ticks per frame swap

    private int spriteCounter = 0;
    public int spriteNum = 0;

    public FlowerYellow() {
        name = "FlowerYellow";
        fixedFootprintWidthTiles = 2;
        fixedFootprintHeightTiles = 2;
    }

    @Override
    public void update() {
        spriteCounter++;
        if (spriteCounter > SPIN_FRAME_HOLD) {
            spriteNum = (spriteNum + 1) % FRAME_COUNT;
            spriteCounter = 0;
        }
    }
}
