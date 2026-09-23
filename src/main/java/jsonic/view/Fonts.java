package jsonic.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.io.IOException;
import java.io.InputStream;

import jsonic.utils.GameConstants;

/**
 * Loads the game's one custom font (font.otf) once, shared by every renderer that draws
 * free-form text. Also holds the shared black-outline technique for labels with no sprite.
 */
public final class Fonts {

    private static final String FONT_PATH = "/res/fonts/font.otf";
    private static final Font BASE = load();

    // debug overlay only: font.otf is stylised (thin diagonals, uneven glyph widths) and hard to
    // read at the small sizes debug text needs - a plain monospaced font reads faster there.
    private static final Font DEBUG_BASE = new Font(Font.MONOSPACED, Font.BOLD, 1);

    private static final Color OUTLINE_COLOR = Color.BLACK;
    private static final int OUTLINE_OFFSET = 1 * GameConstants.SCALE; // outline thickness: 1 native pixel, scaled

    // font.otf's own glyph advances are uneven at these sizes, leaving visibly inconsistent
    // gaps between letters; overriding with a fixed per-glyph advance (spacing + own width)
    // keeps every gap identical instead of relying on the font's built-in metrics.
    private static final int LETTER_SPACING = 1 * GameConstants.SCALE;

    private Fonts() {}

    private static Font load() {
        try (InputStream in = Fonts.class.getResourceAsStream(FONT_PATH)) {
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            System.err.println("Fonts: failed to load font.otf, falling back to a system font");
            return new Font(Font.SANS_SERIF, Font.PLAIN, 1);
        }
    }

    public static Font sized(float size) {
        return BASE.deriveFont(size);
    }

    /** Plain, highly-legible font for the physics debug overlay (hitbox labels, loop angles) - not font.otf. */
    public static Font debugSized(float size) {
        return DEBUG_BASE.deriveFont(size);
    }

    public enum Align { LEFT, CENTER, RIGHT }

    /** Same as drawOutlinedText(g2, text, x, y, fill, Align), centred on x. */
    public static void drawOutlinedText(Graphics2D g2, String text, int x, int y, Color fill) {
        drawOutlinedText(g2, text, x, y, fill, Align.CENTER);
    }

    /**
     * Draws text anchored at x per `align` (its left edge, centre, or right edge), one letter
     * at a time for uniform spacing, with a black outline stroked along each glyph's real
     * contour (so it follows curves correctly) behind a fill colour. Antialiasing is off to
     * keep hard pixel edges.
     */
    public static void drawOutlinedText(Graphics2D g2, String text, int x, int y, Color fill, Align align) {
        Object previousHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        Stroke previousStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(2f * OUTLINE_OFFSET, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        FontRenderContext frc = g2.getFontRenderContext();
        Font font = g2.getFont();
        FontMetrics fm = g2.getFontMetrics();
        int textW = spacedWidth(fm, text);
        int cursorX = switch (align) {
            case LEFT -> x;
            case CENTER -> x - textW / 2;
            case RIGHT -> x - textW;
        };

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            GlyphVector gv = font.createGlyphVector(frc, String.valueOf(c));
            Shape outline = gv.getOutline(cursorX, y);

            g2.setColor(OUTLINE_COLOR);
            g2.draw(outline);
            g2.setColor(fill);
            g2.fill(outline);

            cursorX += fm.charWidth(c) + LETTER_SPACING;
        }

        g2.setStroke(previousStroke);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            previousHint != null ? previousHint : RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    }

    private static int spacedWidth(FontMetrics fm, String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) w += fm.charWidth(text.charAt(i)) + LETTER_SPACING;
        return text.isEmpty() ? 0 : w - LETTER_SPACING; // no trailing gap after the last glyph
    }
}
