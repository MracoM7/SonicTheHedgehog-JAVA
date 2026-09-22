package jsonic.controller.state;

import jsonic.controller.IControllerForModel;
import jsonic.controller.IControllerForView;
import jsonic.model.input.InputSnapshot;
import jsonic.view.IRenderSnapshot;

/** Holds the single active State and switches it on request (setState()), by numeric ID. */
public class StateManager {

    public static final int MENU_STATE = 0;
    public static final int PLAY_STATE = 1;
    public static final int PAUSE_STATE = 2;
    public static final int GAMEOVER_STATE = 3;
    public static final int LEVEL_COMPLETE_STATE = 4;
    public static final int STAGE_INTRO_STATE = 5;

    private final IControllerForView controllerForView;
    private final IControllerForModel controllerForModel;
    private State currentState;

    public StateManager(IControllerForView controllerForView, IControllerForModel controllerForModel) {
        this.controllerForView = controllerForView;
        this.controllerForModel = controllerForModel;
        setState(MENU_STATE);
    }

    // instance methods
    public void setState(int stateID) {
        switch (stateID) {
            case MENU_STATE:
                currentState = new MenuState(controllerForView, controllerForModel, this);
                break;
            case PLAY_STATE:
                currentState = new PlayState(controllerForView, controllerForModel, this);
                break;
            case PAUSE_STATE:
                currentState = new PauseState(controllerForView, controllerForModel, this);
                break;
            case GAMEOVER_STATE:
                currentState = new GameOverState(controllerForView, controllerForModel, this);
                break;
            case LEVEL_COMPLETE_STATE:
                currentState = new LevelCompleteState(controllerForView, controllerForModel, this);
                break;
            case STAGE_INTRO_STATE:
                currentState = new StageIntroState(controllerForView, controllerForModel, this);
                break;
            default:
                throw new IllegalArgumentException("StateManager: unknown stateID: " + stateID);
        }
    }

    public void update(InputSnapshot input) {
        if (currentState != null) currentState.update(input);
    }

    public IRenderSnapshot buildSnapshot() {
        if (currentState == null) return null;
        return currentState.buildSnapshot();
    }
}
