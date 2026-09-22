package jsonic.view.snapshot;

import jsonic.view.IRenderSnapshot;

/**
 * Snapshot for the stage intro splash (StageIntroState).
 *
 * Carries the frozen world (same data as PlayRenderSnapshot, same pattern
 * as Pause/GameOver) for the transition's second beat, once the card art
 * moves from a black backdrop to sitting over the level — plus a single
 * decided boolean for which backdrop to draw, so the renderer needs no
 * timing constants of its own.
 */
public final class StageIntroRenderSnapshot implements IRenderSnapshot {

    public final PlayRenderSnapshot gameSnapshot;
    public final boolean showWorld; // false: black backdrop. true: frozen world behind the card art.

    public StageIntroRenderSnapshot(PlayRenderSnapshot gameSnapshot, boolean showWorld) {
        this.gameSnapshot = gameSnapshot;
        this.showWorld = showWorld;
    }
}
