package jsonic.controller;

/** Boundary interface for GameEngine — the surface Main uses to start the application. */
public interface IGameEngine {

    public void start();

    public int getCurrentFPS();

    // static methods
    public static IGameEngine getInstance() {
        return GameEngine.getInstance();
    }
}
