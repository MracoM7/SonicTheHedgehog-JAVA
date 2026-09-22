package jsonic.view.snapshot;

import jsonic.view.IRenderSnapshot;

/**
 * Snapshot for the pause screen (PauseState).
 *
 * Pause shows the frozen game world below the overlay — so it carries
 * the same data as PlayRenderSnapshot, plus the pause-menu's own state
 * (blink timer, selected option) that PlayRenderSnapshot has no use for.
 */
public final class PauseRenderSnapshot implements IRenderSnapshot {

    public final PlayRenderSnapshot gameSnapshot;
    public final int pauseFrames; // frames since pausing — drives the "PAUSA" blink
    public final int selectedOption; // see PauseState's OPTION_* constants

    public PauseRenderSnapshot(PlayRenderSnapshot gameSnapshot, int pauseFrames, int selectedOption) {
        this.gameSnapshot = gameSnapshot;
        this.pauseFrames = pauseFrames;
        this.selectedOption = selectedOption;
    }
}
