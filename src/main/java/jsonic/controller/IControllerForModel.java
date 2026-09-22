package jsonic.controller;

import jsonic.utils.LevelConfig;
import jsonic.model.world.Level;

/** Boundary interface for ControllerForModel — the only way the rest of the system reaches the Model. */
public interface IControllerForModel {

    public Level getLevel();

    public void setLevelConfig(LevelConfig levelConfig);

    // static methods
    public static IControllerForModel getInstance() {
        return ControllerForModel.getInstance();
    }
}
