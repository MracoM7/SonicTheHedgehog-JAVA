package jsonic.model.item;

/**
 * Purely decorative Green Hill Zone flower: bobs up and down through its
 * 3 traced frames (bottom-anchored, stem stays planted). Ping-pongs
 * 0-1-2-1-... instead of looping so the motion doesn't snap on the wrap.
 *
 * Takes a phase at construction so the two markers (FLOWER_PURPLE_A/B_SPAWN)
 * start half a cycle apart, matching the original's alternating look.
 * Never interacts with the player. Sprite frames live in
 * view.itemview.FlowerPurpleView.
 */
public class FlowerPurple extends Item {

    private static final int[] PINGPONG = {0, 1, 2, 1}; // frame index per step
    private static final int BOB_FRAME_HOLD = 14; // ticks per step, slower than the yellow spin

    private int spriteCounter = 0;
    public int stepIndex;

    public FlowerPurple(boolean altPhase) {
        name = "FlowerPurple";
        fixedFootprintWidthTiles = 2;
        fixedFootprintHeightTiles = 3;
        stepIndex = altPhase ? PINGPONG.length / 2 : 0;
    }

    @Override
    public void update() {
        spriteCounter++;
        if (spriteCounter > BOB_FRAME_HOLD) {
            stepIndex = (stepIndex + 1) % PINGPONG.length;
            spriteCounter = 0;
        }
    }

    /** Which sprite frame FlowerPurpleView should show right now. */
    public int getCurrentFrameIndex() {
        return PINGPONG[stepIndex];
    }
}
