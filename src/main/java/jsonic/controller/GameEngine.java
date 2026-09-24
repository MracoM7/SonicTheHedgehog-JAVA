package jsonic.controller;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import jsonic.controller.input.InputHandler;
import jsonic.model.input.InputSnapshot;
import jsonic.view.GameView;
import jsonic.view.GameWindow;

/**
 * Application entry point (Singleton): builds the window/input machinery once at start(),
 * then drives every fixed-timestep tick (GameLoop.TickListener) by coordinating input,
 * state update, camera update and rendering through ControllerForView.
 */
public class GameEngine implements IGameEngine, GameLoop.TickListener {

    private static GameEngine instance = null;

    private InputHandler inputHandler;
    private GameWindow gameWindow;
    private GameLoop gameLoop;

    private GameEngine() {}

    // instance methods
    @Override
    public void start() {
        inputHandler = new InputHandler();

        GameView gameView = GameView.getInstance();
        gameWindow = new GameWindow(gameView);

        gameView.addKeyListener(inputHandler);

        // F11 fullscreen toggle: a window-management concern, not gameplay input
        gameView.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) gameWindow.toggleFullscreen();
            }
        });

        gameView.requestFocusInWindow();

        gameLoop = new GameLoop(this);
        gameLoop.start();
    }

    @Override
    public void onTick() {
        InputSnapshot input = inputHandler.snapshot();
        IControllerForView controllerForView = IControllerForView.getInstance();
        controllerForView.updateState(input);
        controllerForView.updateCamera();
        controllerForView.renderFrame();
    }

    @Override
    public int getCurrentFPS() {
        return gameLoop.getCurrentFPS();
    }

    // static methods
    public static GameEngine getInstance() {
        if (instance == null) {
            instance = new GameEngine();
        }
        return instance;
    }
}
