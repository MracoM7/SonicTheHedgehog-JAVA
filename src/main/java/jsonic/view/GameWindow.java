package jsonic.view;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;

/**
 * Creates and configures the main game window: builds the JFrame, adds
 * GameView to it, and toggles between windowed and fullscreen.
 *
 * Fullscreen uses a borderless window resized to the screen rather than
 * GraphicsDevice's exclusive full-screen mode, which is more prone to
 * compositor/window-manager quirks on Linux.
 */
public class GameWindow {

    private final JFrame frame;
    private final GameView gameView;

    private boolean fullscreen = false;
    private Rectangle windowedBounds; // restored when leaving fullscreen

    public GameWindow(GameView gameView) {
        this.gameView = gameView;

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setTitle("Sonic Java 2D");

        frame.add(gameView);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Switches between the fixed-size windowed mode and a borderless
     * window sized to the whole screen. GameView scales its fixed
     * logical resolution to fit whatever size the frame ends up at
     * (see GameView.paintComponent), so no coordination is needed here
     * beyond resizing the frame itself.
     */
    public void toggleFullscreen() {
        if (fullscreen) {
            frame.dispose();
            frame.setUndecorated(false);
            frame.setBounds(windowedBounds);
            frame.setVisible(true);
        } else {
            windowedBounds = frame.getBounds();
            Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            frame.dispose();
            frame.setUndecorated(true);
            frame.setBounds(screenBounds);
            frame.setVisible(true);
        }
        fullscreen = !fullscreen;
        gameView.requestFocusInWindow(); // dispose()+setVisible() drops focus

        // dispose()+setVisible() recreates the native peer; the very first
        // paint after that can race the peer becoming live and get dropped,
        // leaving a blank window until something else forces a repaint.
        // Forcing one here, after validate() confirms the peer is ready,
        // closes that race instead of hoping the next tick lands cleanly.
        frame.validate();
        gameView.repaint();
    }
}
