package jsonic.controller.state;

import jsonic.view.audio.AudioManager;
import jsonic.utils.LevelRegistry;
import jsonic.controller.IControllerForModel;
import jsonic.controller.IControllerForView;
import jsonic.model.input.InputSnapshot;
import jsonic.view.IRenderSnapshot;
import jsonic.view.snapshot.MenuRenderSnapshot;

/** Title screen: picks a level with Up/Down (see LevelRegistry.ALL) and starts the stage intro on Enter. */
public class MenuState extends State {

    private int selectedOption = 0; // index into LevelRegistry.ALL
    private int animTimer = 0; // frames since entering the menu — drives the title screen's "PRESS START" blink

    public MenuState(IControllerForView controllerForView, IControllerForModel controllerForModel, StateManager stateManager) {
        super(controllerForView, controllerForModel, stateManager);
        AudioManager.playMusic(LevelRegistry.LEVEL_01.musicPath); // menu music, reuses LEVEL_01's track
    }

    @Override
    public void update(InputSnapshot input) {
        animTimer++;

        int optionCount = LevelRegistry.ALL.length;
        if (input.upJustPressed) {
            selectedOption = (selectedOption - 1 + optionCount) % optionCount;
        }
        if (input.downJustPressed) {
            selectedOption = (selectedOption + 1) % optionCount;
        }

        if (input.muteJustPressed) {
            AudioManager.toggleMuted();
        }

        if (input.enterJustPressed) {
            AudioManager.playSfx("menu_select");
            controllerForModel.setLevelConfig(LevelRegistry.ALL[selectedOption]);
            stateManager.setState(StateManager.STAGE_INTRO_STATE);
        }
    }

    @Override
    public IRenderSnapshot buildSnapshot() {
        return new MenuRenderSnapshot(selectedOption, animTimer);
    }
}
