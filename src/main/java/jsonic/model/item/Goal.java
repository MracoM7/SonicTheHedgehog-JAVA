package jsonic.model.item;

import java.awt.Rectangle;
import jsonic.utils.GameConstants;
import jsonic.model.audio.ISoundEmitter;
import jsonic.model.entity.ICollector;

/**
 * End-of-level goal post. Touching it calls reachGoal() on the collector;
 * Level owns the transition to LevelCompleteState.
 *
 * isCollected only stops onCollision() firing again — unlike Ring it does
 * NOT set pendingDelete, the post stays visible as the finish line.
 *
 * Spin animation: a horizontal squish (the sprite sheet has no true
 * rotation frames, same shortcut as the original), showing Robotnik's
 * side for 8 spins then Sonic's side for 8 more, resting on Sonic.
 * Sprites live in view.itemview.GoalView; isShowingRobotnik() below is
 * the same face-selection rule that used to assign the image field.
 */
public class Goal extends Item {

    private static final int HALF_CYCLE_FRAMES = 5; // full width -> thinnest, or back
    private static final int CYCLE_FRAMES = HALF_CYCLE_FRAMES * 2;
    private static final int ROBOTNIK_CYCLES = 8;
    private static final int SONIC_CYCLES = 8;
    private static final int TOTAL_CYCLES = ROBOTNIK_CYCLES + SONIC_CYCLES;
    private static final float MIN_SCALE_X = 0.12f; // never fully 0 wide (edge-on sliver)

    public boolean spinning = false;
    public int spinTimer = 0;

    private final ISoundEmitter soundEmitter;

    public Goal(ISoundEmitter soundEmitter) {
        this.soundEmitter = soundEmitter;
        name = "Goal";

        renderHeightTiles = 3; // a single-tile flagpole is easy to miss next to real level geometry

        // matches the sprite's actual drawn footprint: goal.png is a square 48x48 sprite, scaled
        // uniformly by renderHeightTiles to 3x3 tiles, bottom-anchored and centred on the marker
        // tile (see ItemView.drawStandard()) - the old (0,0,62,48) matched neither the sprite's
        // native size nor its drawn size, likely stale from an earlier version of the asset
        int ts = GameConstants.TILE_SIZE;
        solidArea = new Rectangle(-ts, -ts * 2, ts * 3, ts * 3);
    }

    @Override
    public void update() {
        if (!spinning) return;

        spinTimer++;
        int totalFrames = TOTAL_CYCLES * CYCLE_FRAMES;
        if (spinTimer >= totalFrames) {
            spinning = false;
            scaleX = 1f;
            soundEmitter.playJingle("level_clear");
            return;
        }

        int posInCycle = spinTimer % CYCLE_FRAMES;

        // each cycle runs thin -> full -> thin, so the face swap always
        // lands on the near edge-on moment instead of popping mid-spin
        scaleX = (posInCycle < HALF_CYCLE_FRAMES)
            ? MIN_SCALE_X + (1f - MIN_SCALE_X) * posInCycle / HALF_CYCLE_FRAMES
            : 1f - (1f - MIN_SCALE_X) * (posInCycle - HALF_CYCLE_FRAMES) / HALF_CYCLE_FRAMES;
    }

    /** True once touched and the spin animation has finished; gates the level-complete transition. */
    public boolean isAnimationDone() {
        return isCollected && !spinning;
    }

    /** Which sprite face GoalView should show right now — same rule that used to pick the image field directly. */
    public boolean isShowingRobotnik() {
        if (!isCollected) return true;
        if (!spinning) return false;
        int cycleIndex = spinTimer / CYCLE_FRAMES;
        return cycleIndex < ROBOTNIK_CYCLES;
    }

    @Override
    public void onCollision(ICollector collector) {
        if (isCollected) return;
        isCollected = true;
        spinning = true;
        spinTimer = 0;
        soundEmitter.playSound("goal_touch");
        collector.reachGoal();
    }
}
