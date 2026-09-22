package jsonic.controller.state;

import jsonic.controller.IControllerForModel;
import jsonic.controller.IControllerForView;
import jsonic.model.input.InputSnapshot;
import jsonic.view.IRenderSnapshot;
import jsonic.view.snapshot.GameOverRenderSnapshot;

/** Game Over screen: waits for Enter, then returns to the title menu. */
public class GameOverState extends State {

    public GameOverState(IControllerForView controllerForView, IControllerForModel controllerForModel, StateManager stateManager) {
        super(controllerForView, controllerForModel, stateManager);
    }

    @Override
    public void update(InputSnapshot input) {
        if (input.enterJustPressed) {
            stateManager.setState(StateManager.MENU_STATE);
        }
    }

    @Override
    public IRenderSnapshot buildSnapshot() {
        return new GameOverRenderSnapshot(buildPlaySnapshot());
    }
}
