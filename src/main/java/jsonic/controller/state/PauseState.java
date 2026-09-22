package jsonic.controller.state;

import jsonic.controller.IControllerForModel;
import jsonic.controller.IControllerForView;
import jsonic.model.input.InputSnapshot;
import jsonic.view.IRenderSnapshot;
import jsonic.view.audio.AudioManager;
import jsonic.view.snapshot.PauseRenderSnapshot;

/** Pause menu: navigates Riprendi/Riavvia/Audio/Esci over the frozen game world. */
public class PauseState extends State {

    private static final int OPTION_RESUME = 0;
    private static final int OPTION_RESTART = 1;
    private static final int OPTION_AUDIO = 2; // index must match PauseRenderer's AUDIO_LABEL_INDEX
    private static final int OPTION_EXIT = 3;
    private static final int OPTION_COUNT = 4;

    private int pauseFrames = 0; // frames since pausing — drives the "PAUSA" blink
    private int selectedOption = OPTION_RESUME;

    public PauseState(IControllerForView controllerForView, IControllerForModel controllerForModel, StateManager stateManager) {
        super(controllerForView, controllerForModel, stateManager);
    }

    @Override
    public void update(InputSnapshot input) {
        pauseFrames++;

        if (input.escapeJustPressed) {
            stateManager.setState(StateManager.PLAY_STATE);
            return;
        }

        if (input.upJustPressed) {
            selectedOption = (selectedOption - 1 + OPTION_COUNT) % OPTION_COUNT;
        }
        if (input.downJustPressed) {
            selectedOption = (selectedOption + 1) % OPTION_COUNT;
        }

        if (input.enterJustPressed) {
            switch (selectedOption) {
                case OPTION_RESUME -> stateManager.setState(StateManager.PLAY_STATE);
                case OPTION_RESTART -> {
                    controllerForModel.getLevel().restartLevel();
                    stateManager.setState(StateManager.PLAY_STATE);
                }
                case OPTION_AUDIO -> AudioManager.toggleMuted(); // stays in the pause menu
                case OPTION_EXIT -> stateManager.setState(StateManager.MENU_STATE);
            }
        }
    }

    @Override
    public IRenderSnapshot buildSnapshot() {
        return new PauseRenderSnapshot(buildPlaySnapshot(), pauseFrames, selectedOption);
    }
}
