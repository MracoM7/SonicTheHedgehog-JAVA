package jsonic.view;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Builds the main JFrame, adds GameView to it, and toggles exclusive full-screen mode. */
public class GameWindow {

    private final JFrame frame;
    private final GameView gameView;
    private final GraphicsDevice device;

    private boolean fullscreen = false;
    private Rectangle windowedBounds; // restored when leaving fullscreen

    public GameWindow(GameView gameView) {
        this.gameView = gameView;
        this.device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setTitle("Sonic The Hedgehog - Java");

        frame.add(gameView);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void toggleFullscreen() {
        if (fullscreen) {
            device.setFullScreenWindow(null);
            frame.dispose();
            frame.setUndecorated(false);
            frame.setBounds(windowedBounds);
            frame.setVisible(true);
        } else {
            windowedBounds = frame.getBounds();
            frame.dispose();
            frame.setUndecorated(true);
            device.setFullScreenWindow(frame);
        }
        fullscreen = !fullscreen;

        SwingUtilities.invokeLater(() -> {
            frame.toFront();
            gameView.requestFocusInWindow();
            frame.validate();
            gameView.repaint();
        });
    }
}
