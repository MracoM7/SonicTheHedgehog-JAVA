package jsonic.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import jsonic.utils.GameConstants;
import jsonic.view.renderer.GameOverRenderer;
import jsonic.view.renderer.HUDRenderer;
import jsonic.view.renderer.LevelCompleteRenderer;
import jsonic.view.renderer.LevelRenderer;
import jsonic.view.renderer.PauseRenderer;
import jsonic.view.renderer.StageIntroRenderer;
import jsonic.view.renderer.TitleRenderer;
import jsonic.view.snapshot.*;

public class GameView extends JPanel implements IView {

    private static GameView instance = null;

    private final LevelRenderer levelRenderer = new LevelRenderer();
    private final HUDRenderer hudRenderer = new HUDRenderer();
    private final TitleRenderer titleRenderer = new TitleRenderer();
    private final PauseRenderer pauseRenderer = new PauseRenderer(levelRenderer, hudRenderer);
    private final GameOverRenderer gameOverRenderer = new GameOverRenderer(levelRenderer, hudRenderer);
    private final LevelCompleteRenderer levelCompleteRenderer = new LevelCompleteRenderer(levelRenderer, hudRenderer);
    private final StageIntroRenderer stageIntroRenderer = new StageIntroRenderer(levelRenderer, hudRenderer);

    // written by the game loop thread (render()), read by the AWT thread (paintComponent())
    private volatile IRenderSnapshot currentSnapshot = null;

    private GameView() {
        this.setPreferredSize(new Dimension(GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.setFocusable(true);
    }

    // instance methods
    @Override
    public void render(IRenderSnapshot snapshot) {
        this.currentSnapshot = snapshot;
        repaint(); // thread-safe: schedules paintComponent() on the AWT thread
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        IRenderSnapshot snapshot = currentSnapshot;
        if (snapshot == null) return;

        Graphics2D g2 = (Graphics2D) g;
        applyRenderingHints(g2);

        // Fixed logical resolution scaled to fit the panel's actual size,
        // aspect ratio preserved — 1:1 in windowed mode, letterboxed in
        // fullscreen (the black background from super.paintComponent()
        // shows through wherever this doesn't draw).
        double scale = Math.min(
            getWidth() / (double) GameConstants.SCREEN_WIDTH,
            getHeight() / (double) GameConstants.SCREEN_HEIGHT);
        int scaledW = (int) (GameConstants.SCREEN_WIDTH * scale);
        int scaledH = (int) (GameConstants.SCREEN_HEIGHT * scale);
        g2.translate((getWidth() - scaledW) / 2, (getHeight() - scaledH) / 2);
        g2.scale(scale, scale);
        // Renderers draw beyond the logical screen box on purpose (the
        // scrolling background especially); clip explicitly so that
        // overdraw doesn't bleed into the letterbox bars in fullscreen.
        g2.clipRect(0, 0, GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT);

        dispatch(g2, snapshot);

        g2.dispose();
        // sync() reduces tearing on Linux with non-compositing drivers
        java.awt.Toolkit.getDefaultToolkit().sync();
    }

    // selects the rendering path based on the snapshot type
    private void dispatch(Graphics2D g2, IRenderSnapshot snapshot) {
        if (snapshot instanceof PlayRenderSnapshot) {
            drawPlay(g2, (PlayRenderSnapshot) snapshot);

        } else if (snapshot instanceof PauseRenderSnapshot) {
            drawPause(g2, (PauseRenderSnapshot) snapshot);

        } else if (snapshot instanceof GameOverRenderSnapshot) {
            drawGameOver(g2, (GameOverRenderSnapshot) snapshot);

        } else if (snapshot instanceof LevelCompleteRenderSnapshot) {
            drawLevelComplete(g2, (LevelCompleteRenderSnapshot) snapshot);

        } else if (snapshot instanceof MenuRenderSnapshot) {
            drawMenu(g2, (MenuRenderSnapshot) snapshot);

        } else if (snapshot instanceof StageIntroRenderSnapshot) {
            drawStageIntro(g2, (StageIntroRenderSnapshot) snapshot);
        }
    }

    // rendering paths per state
    private void drawPlay(Graphics2D g2, PlayRenderSnapshot snap) {
        levelRenderer.draw(g2, snap);
        hudRenderer.draw(g2, snap);
    }

    private void drawPause(Graphics2D g2, PauseRenderSnapshot snap) {
        pauseRenderer.draw(g2, snap.gameSnapshot, snap.pauseFrames, snap.selectedOption);
    }

    private void drawGameOver(Graphics2D g2, GameOverRenderSnapshot snap) {
        gameOverRenderer.draw(g2, snap.gameSnapshot);
    }

    private void drawLevelComplete(Graphics2D g2, LevelCompleteRenderSnapshot snap) {
        levelCompleteRenderer.draw(g2, snap);
    }

    private void drawMenu(Graphics2D g2, MenuRenderSnapshot snap) {
        titleRenderer.draw(g2, snap);
    }

    private void drawStageIntro(Graphics2D g2, StageIntroRenderSnapshot snap) {
        stageIntroRenderer.draw(g2, snap);
    }

    private void applyRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    // static methods
    public static GameView getInstance() {
        if (instance == null) {
            instance = new GameView();
        }
        return instance;
    }
}
