package jsonic.controller;

import jsonic.model.input.InputSnapshot;

/** Boundary interface for ControllerForView — what GameEngine and the View can drive per tick. */
public interface IControllerForView {

    public void updateState(InputSnapshot input);

    public void updateCamera();

    public void renderFrame();

    public int getCameraX();

    public int getCameraY();

    public int getCurrentFPS();

    // static methods
    public static IControllerForView getInstance() {
        return ControllerForView.getInstance();
    }
}
