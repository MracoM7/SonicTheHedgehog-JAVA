package jsonic.view.renderer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;

import jsonic.utils.GameConstants;
import jsonic.view.snapshot.PlayRenderSnapshot;

/**
 * Draws the Game Over overlay: frozen gameplay underneath, "GAME OVER" or "TIME OVER"
 * sprite on top depending on whether the final death was the 10-minute timeout.
 */
public class GameOverRenderer {

    private final LevelRenderer levelRenderer;
    private final HUDRenderer hudRenderer;

    private BufferedImage gameOverText;
    private BufferedImage timeOverText;

    // Shares GameView's own renderer instances instead of reloading the same sprites again.
    public GameOverRenderer(LevelRenderer levelRenderer, HUDRenderer hudRenderer) {
        this.levelRenderer = levelRenderer;
        this.hudRenderer = hudRenderer;
        try {
            gameOverText = ImageIO.read(getClass().getResourceAsStream("/res/sprites/screens/game_over.png"));
            timeOverText = ImageIO.read(getClass().getResourceAsStream("/res/sprites/screens/time_over.png"));
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("GameOverRenderer: failed to load game_over.png/time_over.png");
        }
    }

    public void draw(Graphics2D g2, PlayRenderSnapshot gameSnapshot) {
        levelRenderer.draw(g2, gameSnapshot, false); // frozen: don't let the player's sprite keep animating in place
        hudRenderer.draw(g2, gameSnapshot);

        int screenW = GameConstants.SCREEN_WIDTH;
        int screenH = GameConstants.SCREEN_HEIGHT;

        int scale = GameConstants.SCALE;
        BufferedImage text = gameSnapshot.timeOver ? timeOverText : gameOverText;
        if (text != null) {
            int w = text.getWidth() * scale;
            int h = text.getHeight() * scale;
            g2.drawImage(text, (screenW - w) / 2, (screenH - h) / 2, w, h, null);
        }
    }
}
