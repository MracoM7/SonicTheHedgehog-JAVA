package jsonic.controller.state;

import jsonic.controller.IControllerForModel;
import jsonic.controller.IControllerForView;
import jsonic.model.input.InputSnapshot;
import jsonic.model.world.Level;
import jsonic.utils.GameConstants;
import jsonic.view.IRenderSnapshot;

/** Active gameplay: advances the Level each tick and reacts to pause/game-over/level-complete. */
public class PlayState extends State {

    public PlayState(IControllerForView controllerForView, IControllerForModel controllerForModel, StateManager stateManager) {
        super(controllerForView, controllerForModel, stateManager);
    }

    @Override
    public void update(InputSnapshot input) {
        if (input.escapeJustPressed) {
            stateManager.setState(StateManager.PAUSE_STATE);
            return;
        }

        Level level = controllerForModel.getLevel();
        level.update(input);

        if (level.isGameOver()) {
            stateManager.setState(StateManager.GAMEOVER_STATE);
            return;
        }

        if (level.isGoalReached()) {
            stateManager.setState(StateManager.LEVEL_COMPLETE_STATE);
            return;
        }

        if (input.debugJustPressed && GameConstants.DEBUG_ENABLED) {
            level.toggleDebug();
        }
    }

    @Override
    public IRenderSnapshot buildSnapshot() {
        return buildPlaySnapshot();
    }
}
