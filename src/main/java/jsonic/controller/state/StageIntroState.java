package jsonic.controller.state;

import jsonic.controller.IControllerForModel;
import jsonic.controller.IControllerForView;
import jsonic.model.input.InputSnapshot;
import jsonic.view.IRenderSnapshot;
import jsonic.view.snapshot.StageIntroRenderSnapshot;

/** Stage-intro splash card: black backdrop, then the frozen level, then hands off to PlayState. */
public class StageIntroState extends State {

    private static final int BLACK_PHASE_FRAMES = 60;  // beat 1 duration: card over solid black (1s @ 60fps)
    private static final int DISPLAY_FRAMES = 120; // total duration (2s @ 60fps); beat 2 (card over world) fills the rest

    private int introFrames = 0;

    public StageIntroState(IControllerForView controllerForView, IControllerForModel controllerForModel, StateManager stateManager) {
        super(controllerForView, controllerForModel, stateManager);
    }

    @Override
    public void update(InputSnapshot input) {
        introFrames++;
        if (introFrames >= DISPLAY_FRAMES) {
            stateManager.setState(StateManager.PLAY_STATE);
        }
    }

    @Override
    public IRenderSnapshot buildSnapshot() {
        boolean showWorld = introFrames >= BLACK_PHASE_FRAMES;
        return new StageIntroRenderSnapshot(buildPlaySnapshot(), showWorld);
    }
}
