package jsonic.model.item;

import java.awt.Rectangle;

import jsonic.model.audio.ISoundEmitter;
import jsonic.model.entity.ICollector;
import jsonic.utils.GameConstants;

/**
 * Yellow spring: launches the player upward on contact. Rests in the
 * compressed pose; the extended pose is the launch reaction, not idle.
 *
 * Like Spike, never "used up": onCollision() doesn't set isCollected, so
 * repeated contact keeps launching. The extended-pose window after a
 * bounce also acts as the re-trigger guard.
 *
 * Sprites live in view.itemview.SpringView — extendedTimer > 0 selects the
 * extended pose there, exactly as it did via the image field before.
 */
public class Spring extends Item {

    private static final int EXTENDED_FRAMES = 8;

    // hand-tuned by playtesting; Player.JUMP_STRENGTH = -19.5 for reference
    private static final float LAUNCH_VELOCITY = -32f;

    public int extendedTimer = 0;

    private final ISoundEmitter soundEmitter;

    public Spring(ISoundEmitter soundEmitter) {
        this.soundEmitter = soundEmitter;
        name = "Spring";

        renderWidthTiles = 2; // both poses this wide, height follows aspect ratio
        renderBehindVisuals = true; // hideable behind trees painted in the level art
        requireLandingFromAbove = true; // bumping it sideways shouldn't launch the player

        // matches the resting (compressed) pose footprint, 2x1 tiles centred on
        // the marker; the taller extended pose is covered by EXTENDED_FRAMES instead
        int ts = GameConstants.TILE_SIZE;
        solidArea = new Rectangle(-ts / 2, 0, ts * 2, ts);
    }

    @Override
    public void update() {
        if (extendedTimer <= 0) return;
        extendedTimer--;
    }

    @Override
    public void onCollision(ICollector collector) {
        if (extendedTimer > 0) return; // mid-bounce already, ignore repeated contact this window

        collector.launch(LAUNCH_VELOCITY);
        extendedTimer = EXTENDED_FRAMES;
        soundEmitter.playSound("spring");
    }
}
