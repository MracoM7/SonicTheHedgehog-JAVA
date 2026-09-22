package jsonic.controller.state;

import jsonic.controller.IControllerForModel;
import jsonic.controller.IControllerForView;
import jsonic.model.input.InputSnapshot;
import jsonic.model.world.Level;
import jsonic.view.IRenderSnapshot;
import jsonic.view.snapshot.PlayRenderSnapshot;

/**
 * One phase of a game session (menu, stage intro, play, pause, game over, level complete).
 * StateManager holds exactly one live instance at a time and calls update()/buildSnapshot()
 * on it every tick; a concrete State only sees the Model/View through the IControllerFor*
 * interfaces, never the concrete Controller classes.
 */
public abstract class State {

    protected final IControllerForView controllerForView;
    protected final IControllerForModel controllerForModel;
    protected final StateManager stateManager;

    protected State(IControllerForView controllerForView, IControllerForModel controllerForModel, StateManager stateManager) {
        this.controllerForView = controllerForView;
        this.controllerForModel = controllerForModel;
        this.stateManager = stateManager;
    }

    public abstract void update(InputSnapshot input);

    public abstract IRenderSnapshot buildSnapshot();

    // Shared by every state that shows the (possibly frozen) game world —
    // PlayState, PauseState, GameOverState, StageIntroState all built this
    // same 14-argument PlayRenderSnapshot independently before.
    protected PlayRenderSnapshot buildPlaySnapshot() {
        Level level = controllerForModel.getLevel();
        return new PlayRenderSnapshot(
            level.getPlayer(),
            level.getItems(),
            level.getEnemies(),
            level.getTileMap(),
            level.getLoops(),
            level.getConfig(),
            controllerForView.getCameraX(),
            controllerForView.getCameraY(),
            level.getRingCount(),
            level.getScore(),
            level.getLives(),
            level.getTimeFrames(),
            level.getTileMap().isDebugMode(),
            controllerForView.getCurrentFPS(),
            level.isTimeOver()
        );
    }
}
