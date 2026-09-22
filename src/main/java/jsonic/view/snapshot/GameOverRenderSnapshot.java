package jsonic.view.snapshot;

import jsonic.view.IRenderSnapshot;

/**
 * Snapshot for the game over screen (GameOverState).
 *
 * Shows the frozen game world below the "GAME OVER" overlay — same data
 * as PlayRenderSnapshot, same pattern as PauseRenderSnapshot.
 */
public final class GameOverRenderSnapshot implements IRenderSnapshot {

    public final PlayRenderSnapshot gameSnapshot;

    public GameOverRenderSnapshot(PlayRenderSnapshot gameSnapshot) {
        this.gameSnapshot = gameSnapshot;
    }
}
