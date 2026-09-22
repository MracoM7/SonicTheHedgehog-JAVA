package jsonic.controller.state;

import jsonic.controller.IControllerForModel;
import jsonic.controller.IControllerForView;
import jsonic.model.input.InputSnapshot;
import jsonic.model.world.Level;
import jsonic.view.IRenderSnapshot;
import jsonic.view.snapshot.LevelCompleteRenderSnapshot;

/** Results (Act Clear) screen: shows the score breakdown, waits for Enter, then returns to the title menu. */
public class LevelCompleteState extends State {

    private static final int WORLD_PHASE_FRAMES = 2 * 60; // world visible for 2s, then solid black

    private int completeFrames = 0;

    public LevelCompleteState(IControllerForView controllerForView, IControllerForModel controllerForModel, StateManager stateManager) {
        super(controllerForView, controllerForModel, stateManager);
    }

    @Override
    public void update(InputSnapshot input) {
        completeFrames++;
        if (input.enterJustPressed) {
            stateManager.setState(StateManager.MENU_STATE);
        }
    }

    @Override
    public IRenderSnapshot buildSnapshot() {
        Level level = controllerForModel.getLevel();
        boolean showWorld = completeFrames < WORLD_PHASE_FRAMES;
        return new LevelCompleteRenderSnapshot(
            buildPlaySnapshot(),
            showWorld,
            level.getLastTimeBonus(),
            level.getLastRingBonus(),
            level.getScore()
        );
    }
}
