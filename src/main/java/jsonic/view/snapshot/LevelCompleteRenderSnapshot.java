package jsonic.view.snapshot;

import jsonic.view.IRenderSnapshot;

/**
 * Snapshot for the level-complete (results) screen (LevelCompleteState).
 *
 * Carries the frozen world (same data as PlayRenderSnapshot, same pattern as Pause/GameOver/
 * StageIntro) since the per-level passed-card art is revealed over it, the same way the stage
 * intro's splash card is — just with the two phases reversed (world first, then solid black).
 */
public final class LevelCompleteRenderSnapshot implements IRenderSnapshot {

    public final PlayRenderSnapshot gameSnapshot;
    public final boolean showWorld; // true: frozen world behind the card art. false: black backdrop.

    public final int timeBonus;
    public final int ringBonus;
    public final int finalScore;

    public LevelCompleteRenderSnapshot(PlayRenderSnapshot gameSnapshot, boolean showWorld,
                                        int timeBonus, int ringBonus, int finalScore) {
        this.gameSnapshot = gameSnapshot;
        this.showWorld = showWorld;
        this.timeBonus = timeBonus;
        this.ringBonus = ringBonus;
        this.finalScore = finalScore;
    }
}
