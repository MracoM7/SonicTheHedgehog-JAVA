package jsonic.view.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

import jsonic.utils.GameConstants;
import jsonic.view.Fonts;
import jsonic.view.SpriteLoader;
import jsonic.view.snapshot.LevelCompleteRenderSnapshot;

/**
 * Draws the results (Act Clear) screen: the frozen level first, then solid black (the reverse
 * of StageIntroRenderer's beats, see LevelCompleteState), with the level's own passed-card art
 * on top (LevelConfig.passedCardPath, falls back to passed_generic.png) and a SCORE / TIME
 * BONUS / RING BONUS tally using the same sprites as the in-game HUD.
 */
public class LevelCompleteRenderer {

    private static final String GENERIC_PASSED_PATH = "/res/levels/passed_generic.png";

    private static final int GLYPH_WIDTH = 7;
    private static final int GLYPH_HEIGHT = 11;
    private static final int SCALE = 3;
    private static final int LINE_HEIGHT = 50;
    private static final int WORD_GAP = 25; // space between two labels drawn side by side (e.g. "TIME" "BONUS")
    private static final int VALUE_GAP = 128; // space between the widest label and its number column

    // Block's top-left corner relative to screen centre; move the whole tally by changing only these two.
    private static final int BLOCK_X_OFFSET = -240;
    private static final int BLOCK_Y_OFFSET = -24;

    private final LevelRenderer levelRenderer;
    private final HUDRenderer hudRenderer;

    private final BufferedImage[] digitGlyphs = new BufferedImage[10];
    private BufferedImage scoreLabel;
    private BufferedImage timeLabel;
    private BufferedImage bonusLabel;
    private BufferedImage ringLabel;

    private BufferedImage passedCard;
    private String loadedPath; // which passedCardPath is currently loaded, or null for none

    // Shares GameView's own renderer instances instead of reloading the same sprites again.
    public LevelCompleteRenderer(LevelRenderer levelRenderer, HUDRenderer hudRenderer) {
        this.levelRenderer = levelRenderer;
        this.hudRenderer = hudRenderer;
        BufferedImage sheet = SpriteLoader.load(getClass(), "/res/sprites/hud/hud_digits.png");
        if (sheet != null) {
            for (int i = 0; i < digitGlyphs.length; i++) {
                digitGlyphs[i] = sheet.getSubimage(i * GLYPH_WIDTH, 0, GLYPH_WIDTH, GLYPH_HEIGHT);
            }
        }
        scoreLabel = SpriteLoader.load(getClass(), "/res/sprites/screens/results_score.png");
        timeLabel = SpriteLoader.load(getClass(), "/res/sprites/screens/results_time.png");
        bonusLabel = SpriteLoader.load(getClass(), "/res/sprites/screens/results_bonus.png");
        ringLabel = SpriteLoader.load(getClass(), "/res/sprites/screens/results_ring.png");
    }

    // (Re)loads the passed card for a level, falling back to the generic one - a no-op if already loaded.
    private void loadForLevel(String passedCardPath) {
        if (Objects.equals(passedCardPath, loadedPath)) return;
        loadedPath = passedCardPath;

        passedCard = passedCardPath == null ? null : SpriteLoader.load(getClass(), passedCardPath);
        if (passedCard == null) passedCard = SpriteLoader.load(getClass(), GENERIC_PASSED_PATH);
    }

    public void draw(Graphics2D g2, LevelCompleteRenderSnapshot snap) {
        loadForLevel(snap.gameSnapshot.config.passedCardPath);

        int screenW = GameConstants.SCREEN_WIDTH;
        int screenH = GameConstants.SCREEN_HEIGHT;

        if (snap.showWorld) {
            levelRenderer.draw(g2, snap.gameSnapshot, false); // frozen: don't let the player's sprite keep animating in place
            hudRenderer.draw(g2, snap.gameSnapshot);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, screenW, screenH);
        }

        if (passedCard != null) {
            g2.drawImage(passedCard, 0, 0, screenW, screenH, null);
        }

        int labelX = screenW / 2 + BLOCK_X_OFFSET;
        int widestLabel = Math.max(scoreLabel.getWidth(),
            Math.max(timeLabel.getWidth() + bonusLabel.getWidth(), ringLabel.getWidth() + bonusLabel.getWidth()) + WORD_GAP / SCALE);
        int valueX = labelX + widestLabel * SCALE + VALUE_GAP;
        int y = screenH / 2 + BLOCK_Y_OFFSET;

        drawLine(g2, scoreLabel, snap.finalScore, labelX, valueX, y);
        y += LINE_HEIGHT;

        drawTwoWordLine(g2, timeLabel, bonusLabel, snap.timeBonus, labelX, valueX, y);
        y += LINE_HEIGHT;

        drawTwoWordLine(g2, ringLabel, bonusLabel, snap.ringBonus, labelX, valueX, y);

        g2.setFont(Fonts.sized(26f));
        Fonts.drawOutlinedText(g2, "PRESS ENTER TO RETURN TO MENU", screenW / 2, screenH - 60, Color.WHITE, Fonts.Align.CENTER);
    }

    private void drawLine(Graphics2D g2, BufferedImage label, int value, int labelX, int valueX, int y) {
        if (label != null) {
            g2.drawImage(label, labelX, y, label.getWidth() * SCALE, label.getHeight() * SCALE, null);
        }
        drawBitmapNumber(g2, String.valueOf(value), valueX, y, SCALE);
    }

    private void drawTwoWordLine(Graphics2D g2, BufferedImage first, BufferedImage second, int value, int labelX, int valueX, int y) {
        int cursorX = labelX;
        if (first != null) {
            g2.drawImage(first, cursorX, y, first.getWidth() * SCALE, first.getHeight() * SCALE, null);
            cursorX += first.getWidth() * SCALE + WORD_GAP;
        }
        if (second != null) {
            g2.drawImage(second, cursorX, y, second.getWidth() * SCALE, second.getHeight() * SCALE, null);
        }
        drawBitmapNumber(g2, String.valueOf(value), valueX, y, SCALE);
    }

    private void drawBitmapNumber(Graphics2D g2, String text, int x, int y, int scale) {
        int cellW = GLYPH_WIDTH * scale;
        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') continue;
            g2.drawImage(digitGlyphs[c - '0'], cursorX, y, cellW, GLYPH_HEIGHT * scale, null);
            cursorX += cellW;
        }
    }
}
