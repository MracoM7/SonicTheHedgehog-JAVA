package jsonic.view;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;

/**
 * Shared drawing for the physics debug overlay (hitbox/loop borders): Graphics2D's own stroke is
 * centered on the path, so a thicker one grows both in and out of the true collision boundary.
 * These grow only inward instead, keeping the outer edge exactly on the real hitbox/radius.
 */
public final class DebugDraw {

    public static final float HITBOX_BORDER_THICKNESS = 3f;

    private DebugDraw() {}

    public static void thickRect(Graphics2D g2, int x, int y, int w, int h, float thickness) {
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(thickness));
        int inset = Math.round(thickness / 2f);
        g2.drawRect(x + inset, y + inset, w - inset * 2, h - inset * 2);
        g2.setStroke(old);
    }

    public static void thickArc(Graphics2D g2, int cx, int cy, int r, int startDeg, int sweepDeg, float thickness) {
        Stroke old = g2.getStroke();
        g2.setStroke(new BasicStroke(thickness));
        int insetR = Math.round(r - thickness / 2f);
        g2.drawArc(cx - insetR, cy - insetR, insetR * 2, insetR * 2, startDeg, sweepDeg);
        g2.setStroke(old);
    }
}
