package jsonic.controller;

import jsonic.controller.state.StateManager;
import jsonic.model.input.InputSnapshot;
import jsonic.model.world.Level;
import jsonic.utils.GameConstants;
import jsonic.utils.LevelConfig;
import jsonic.view.IRenderSnapshot;
import jsonic.view.IView;

/**
 * Singleton owning the Camera and the state machine (StateManager) that models a game
 * session's phases (menu, stage intro, play, pause, game over, level complete). Drives one
 * tick per call: update the current state, update the camera, then render its snapshot.
 */
public class ControllerForView implements IControllerForView {

    private static ControllerForView instance = null;

    private final IControllerForModel controllerForModel = IControllerForModel.getInstance();
    private final IView view = IView.getInstance();
    private final Camera camera = new Camera(GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT);
    private final StateManager stateManager;

    private Level lastKnownLevel = null;

    private ControllerForView() {
        stateManager = new StateManager(this, controllerForModel);
    }

    // private methods
    private void applyCameraWorldDimensions(LevelConfig config) {
        if (config.backgroundImage != null) {
            camera.setWorldDimensions(
                config.backgroundImage.getWidth() * GameConstants.SCALE,
                config.backgroundImage.getHeight() * GameConstants.SCALE
            );
        } else {
            camera.setWorldDimensions(GameConstants.WORLD_WIDTH, GameConstants.WORLD_HEIGHT);
        }
    }

    // instance methods
    @Override
    public void updateState(InputSnapshot input) {
        stateManager.update(input);
    }

    @Override
    public void updateCamera() {
        Level current = controllerForModel.getLevel();
        if (current != lastKnownLevel) {
            applyCameraWorldDimensions(current.getConfig());
            lastKnownLevel = current;
        }

        if (current.consumeJustRespawned()) camera.snapToTarget(current.getPlayer());
        else camera.update(current.getPlayer());
    }

    @Override
    public void renderFrame() {
        IRenderSnapshot snapshot = stateManager.buildSnapshot();
        if (snapshot != null) view.render(snapshot);
    }

    @Override
    public int getCameraX() {
        return (int) camera.getX();
    }

    @Override
    public int getCameraY() {
        return (int) camera.getY();
    }

    @Override
    public int getCurrentFPS() {
        return IGameEngine.getInstance().getCurrentFPS();
    }

    // static methods
    public static ControllerForView getInstance() {
        if (instance == null) {
            instance = new ControllerForView();
        }
        return instance;
    }
}
