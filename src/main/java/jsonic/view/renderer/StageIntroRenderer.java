package jsonic.view.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import jsonic.utils.GameConstants;
import jsonic.view.SpriteLoader;
import jsonic.view.snapshot.StageIntroRenderSnapshot;

/**
 * Draws the stage-intro splash card over two beats: solid black first, then the frozen level
 * (StageIntroState decides when). The card itself is drawn identically both times, straight
 * from its own real alpha channel — only the backdrop changes, no pixel processing.
 */
public class StageIntroRenderer {

    private final LevelRenderer levelRenderer;
    private final HUDRenderer hudRenderer;

    private BufferedImage splash;
    private String loadedPath; // which splashCardPath is currently loaded, or null for none

    // Shares GameView's own renderer instances instead of reloading the same sprites again.
    public StageIntroRenderer(LevelRenderer levelRenderer, HUDRenderer hudRenderer) {
        this.levelRenderer = levelRenderer;
        this.hudRenderer = hudRenderer;
    }

    // (Re)loads the splash card for a level - a no-op if it's already loaded.
    private void loadForLevel(String splashCardPath) {
        if (java.util.Objects.equals(splashCardPath, loadedPath)) return;
        loadedPath = splashCardPath;

        splash = splashCardPath == null ? null : SpriteLoader.load(getClass(), splashCardPath);
    }

    public void draw(Graphics2D g2, StageIntroRenderSnapshot snap) {
        loadForLevel(snap.gameSnapshot.config.splashCardPath);

        int screenW = GameConstants.SCREEN_WIDTH;
        int screenH = GameConstants.SCREEN_HEIGHT;

        if (snap.showWorld) {
            levelRenderer.draw(g2, snap.gameSnapshot);
            hudRenderer.draw(g2, snap.gameSnapshot);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, screenW, screenH);
        }

        if (splash != null) {
            g2.drawImage(splash, 0, 0, screenW, screenH, null);
        }
    }
}
