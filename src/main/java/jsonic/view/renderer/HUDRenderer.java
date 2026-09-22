package jsonic.view.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

import jsonic.utils.GameConstants;
import jsonic.view.Fonts;
import jsonic.view.snapshot.PlayRenderSnapshot;

/**
 * Renders the HUD: rings/score/lives/time counters and debug info, all from PlayRenderSnapshot.
 * Labels/digits are sprites, not drawString() — the original HUD font has no system equivalent.
 * RINGS and TIME blink red independently (at 0 rings / last minute) rather than sharing one
 * blink flag like the original did, which was a display bug there, not intentional design.
 */
public class HUDRenderer {

    private static final int GLYPH_WIDTH = 7; // hud_digits.png cell pitch
    private static final int GLYPH_HEIGHT = 11;
    private static final int COLON_INDEX = 10; // hud_digits.png: glyphs 0-9 are digits, glyph 10 is ':'
    private static final int SCALE = 3; // shared by every HUD piece so they all sit on one pixel grid
    private static final double LIFE_DIGIT_SCALE = SCALE * 0.65; // lives digit reads too large next to the small "x"

    // Layout in native (pre-SCALE) pixels, multiplied by SCALE below.
    private static final int MARGIN_X_NATIVE = 10;
    private static final int ROW_Y_SCORE_NATIVE = 10;
    private static final int ROW_Y_TIME_NATIVE = 30;
    private static final int ROW_Y_RINGS_NATIVE = 50;
    private static final int DIGIT_COLUMN_X_NATIVE = 65;
    private static final int LIVES_MARGIN_NATIVE = 10;
    private static final int LIVES_ICON_GAP_NATIVE = 4; // icon -> "SONIC" label
    private static final int LIVES_ROW_GAP_NATIVE = 2; // "SONIC" -> "x N" row, and "x" -> digit

    private static final int ROW_Y_DEBUG_FPS_NATIVE = 95;
    private static final int ROW_Y_DEBUG_X_NATIVE = 110;
    private static final int ROW_Y_DEBUG_Y_NATIVE = 125;

    private static final int TIME_WARNING_FRAMES = GameConstants.TIME_LIMIT_FRAMES - 60 * GameConstants.FPS; // last minute before the limit
    private static final int BLINK_INTERVAL_FRAMES = 16; // ~4 times/sec at 60fps

    private final BufferedImage[] digitGlyphs = new BufferedImage[11];
    private BufferedImage labelScore;
    private BufferedImage labelTime;
    private BufferedImage labelTimeRed;
    private BufferedImage labelRings;
    private BufferedImage labelRingsRed;
    private BufferedImage lifeIcon;
    private BufferedImage labelSonic;
    private BufferedImage xChar;

    public HUDRenderer() {
        loadFont();
    }

    private void loadFont() {
        try {
            BufferedImage sheet = ImageIO.read(
                getClass().getResourceAsStream("/res/sprites/hud/hud_digits.png"));
            for (int i = 0; i < digitGlyphs.length; i++) {
                digitGlyphs[i] = sheet.getSubimage(i * GLYPH_WIDTH, 0, GLYPH_WIDTH, GLYPH_HEIGHT);
            }
            labelScore = ImageIO.read(getClass().getResourceAsStream("/res/sprites/hud/hud_label_score.png"));
            labelTime = ImageIO.read(getClass().getResourceAsStream("/res/sprites/hud/hud_label_time.png"));
            labelTimeRed = ImageIO.read(getClass().getResourceAsStream("/res/sprites/hud/hud_label_time_red.png"));
            labelRings = ImageIO.read(getClass().getResourceAsStream("/res/sprites/hud/hud_label_rings.png"));
            labelRingsRed = ImageIO.read(getClass().getResourceAsStream("/res/sprites/hud/hud_label_rings_red.png"));
            lifeIcon = ImageIO.read(getClass().getResourceAsStream("/res/sprites/hud/hud_life_icon.png"));
            labelSonic = ImageIO.read(getClass().getResourceAsStream("/res/sprites/hud/hud_label_sonic.png"));
            xChar = ImageIO.read(getClass().getResourceAsStream("/res/sprites/hud/hud_x_char.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("HUDRenderer: failed to load HUD assets");
        }
    }

    public void draw(Graphics2D g2, PlayRenderSnapshot snap) {

        int digitYOffset = (labelRings.getHeight() * SCALE - GLYPH_HEIGHT * SCALE) / 2; // centres shorter digits on the label

        boolean blinkOn = (snap.timeFrames / BLINK_INTERVAL_FRAMES) % 2 == 0;
        boolean ringsWarning = snap.ringCount == 0;
        boolean timeWarning = snap.timeFrames >= TIME_WARNING_FRAMES;

        int marginX = MARGIN_X_NATIVE * SCALE;
        int rowScoreY = ROW_Y_SCORE_NATIVE * SCALE;
        int rowTimeY = ROW_Y_TIME_NATIVE * SCALE;
        int rowRingsY = ROW_Y_RINGS_NATIVE * SCALE;
        int digitX = DIGIT_COLUMN_X_NATIVE * SCALE;

        drawLabel(g2, labelScore, marginX, rowScoreY);
        drawBitmapNumber(g2, String.valueOf(snap.score), digitX, rowScoreY + digitYOffset, SCALE);

        drawLabel(g2, (timeWarning && blinkOn) ? labelTimeRed : labelTime, marginX, rowTimeY);
        drawBitmapNumber(g2, formatTime(snap.timeFrames), digitX, rowTimeY + digitYOffset, SCALE);

        drawLabel(g2, (ringsWarning && blinkOn) ? labelRingsRed : labelRings, marginX, rowRingsY);
        drawBitmapNumber(g2, String.valueOf(snap.ringCount), digitX, rowRingsY + digitYOffset, SCALE);

        drawLivesIndicator(g2, snap.lives);

        // debug info
        if (snap.debugMode) {
            g2.setFont(Fonts.sized(10f * SCALE + 2f));
            Fonts.drawOutlinedText(g2, "FPS: " + snap.fps, marginX, ROW_Y_DEBUG_FPS_NATIVE * SCALE, Color.YELLOW, Fonts.Align.LEFT);
            Fonts.drawOutlinedText(g2, "X: " + (int) snap.player.getX(), marginX, ROW_Y_DEBUG_X_NATIVE * SCALE, Color.YELLOW, Fonts.Align.LEFT);
            Fonts.drawOutlinedText(g2, "Y: " + (int) snap.player.getY(), marginX, ROW_Y_DEBUG_Y_NATIVE * SCALE, Color.YELLOW, Fonts.Align.LEFT);
        }
    }

    /** Bottom-left "SONIC x N" indicator: portrait, then "SONIC" above "x N", left-aligned. */
    private void drawLivesIndicator(Graphics2D g2, int lives) {
        int iconX = LIVES_MARGIN_NATIVE * SCALE;
        int iconY = GameConstants.SCREEN_HEIGHT - lifeIcon.getHeight() * SCALE - LIVES_MARGIN_NATIVE * SCALE;
        drawLabel(g2, lifeIcon, iconX, iconY);

        int textX = iconX + lifeIcon.getWidth() * SCALE + LIVES_ICON_GAP_NATIVE * SCALE;
        drawLabel(g2, labelSonic, textX, iconY);

        int xCharH = xChar.getHeight() * SCALE;
        int xCharY = iconY + labelSonic.getHeight() * SCALE + LIVES_ROW_GAP_NATIVE * SCALE;
        drawLabel(g2, xChar, textX, xCharY);

        int digitH = (int) Math.round(GLYPH_HEIGHT * LIFE_DIGIT_SCALE);
        int digitX = textX + xChar.getWidth() * SCALE + LIVES_ROW_GAP_NATIVE * SCALE;
        int digitY = xCharY + (xCharH - digitH) / 2; // centred on the "x" row, not the "SONIC" row above it
        drawBitmapNumber(g2, String.valueOf(lives), digitX, digitY, LIFE_DIGIT_SCALE);
    }

    private void drawLabel(Graphics2D g2, BufferedImage label, int x, int y) {
        g2.drawImage(label, x, y, label.getWidth() * SCALE, label.getHeight() * SCALE, null);
    }

    /**
     * Draws digits (and ':') using hud_digits.png glyphs, side by side on
     * a uniform grid, scaled up by `scale`.
     */
    private void drawBitmapNumber(Graphics2D g2, String text, int x, int y, double scale) {
        int cellW = (int) Math.round(GLYPH_WIDTH * scale);
        int digitH = (int) Math.round(GLYPH_HEIGHT * scale);
        int cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int glyph;
            if (c == ':') {
                glyph = COLON_INDEX;
            } else if (c >= '0' && c <= '9') {
                glyph = c - '0';
            } else {
                continue;
            }
            g2.drawImage(digitGlyphs[glyph], cursorX, y, cellW, digitH, null);
            cursorX += cellW;
        }
    }

    /** MM:SS, matching the classic Sonic HUD time format. */
    private String formatTime(int timeFrames) {
        int totalSeconds = timeFrames / GameConstants.FPS;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
