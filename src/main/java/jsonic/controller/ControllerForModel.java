package jsonic.controller;

import jsonic.utils.LevelConfig;
import jsonic.utils.LevelRegistry;
import jsonic.model.world.Level;

/**
 * Singleton and sole access point to the Model for the rest of the system: owns the active
 * Level and its LevelConfig, rebuilding the Level from scratch whenever the menu selects
 * a different one.
 */
public class ControllerForModel implements IControllerForModel {

    private static ControllerForModel instance = null;

    private Level level;
    private LevelConfig selectedLevelConfig = LevelRegistry.LEVEL_01;

    private ControllerForModel() {}

    // private methods
    private Level buildLevel() {
        return new Level(selectedLevelConfig);
    }

    // instance methods
    @Override
    public Level getLevel() {
        if (level == null) level = buildLevel();
        return level;
    }

    @Override
    public void setLevelConfig(LevelConfig levelConfig) {
        this.selectedLevelConfig = levelConfig;
        this.level = null; // rebuilt next time getLevel() is called
    }

    // static methods
    public static ControllerForModel getInstance() {
        if (instance == null) {
            instance = new ControllerForModel();
        }
        return instance;
    }
}
