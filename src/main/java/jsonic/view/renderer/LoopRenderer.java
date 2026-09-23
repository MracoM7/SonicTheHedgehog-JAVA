package jsonic.view.renderer;

import java.awt.Color;
import java.awt.Graphics2D;

import jsonic.model.physics.LoopRegion;
import jsonic.view.DebugDraw;
import jsonic.view.Fonts;
import jsonic.view.snapshot.PlayRenderSnapshot;

/**
 * Debug-only overlay for analytic loops (LoopRegion): the collision circle, its centre,
 * and radial tick marks with their angle, to verify the surface "felt" by Player at a glance.
 */
public class LoopRenderer {

    private static final int TICK_MARKER_RADIUS = 4;
    private static final float DEBUG_TEXT_SIZE = 14f;

    public void draw(Graphics2D g2, PlayRenderSnapshot snap) {
        if (!snap.debugMode || snap.loops == null) return;

        for (LoopRegion loop : snap.loops) {
            int cx = (int) loop.getCenterX() - snap.cameraX;
            int cy = (int) loop.getCenterY() - snap.cameraY;
            int r  = (int) loop.getRadius();

            // Only this arc's own span (Level.getLoops() already picks the currently-solid
            // half); -90° because drawArc starts at 3 o'clock, our convention at 6 o'clock.
            // Thick border growing inward, so the outer edge still matches the true collision radius.
            g2.setColor(new Color(0x00, 0xE5, 0xFF, 220)); // cyan
            int startJava = Math.round(loop.getArcStart() - 90f);
            int sweepJava = Math.round(((loop.getArcEnd() - loop.getArcStart()) % 360f + 360f) % 360f);
            DebugDraw.thickArc(g2, cx, cy, r, startJava, sweepJava, DebugDraw.HITBOX_BORDER_THICKNESS);

            // Centre
            g2.setColor(Color.YELLOW);
            g2.drawLine(cx - 5, cy, cx + 5, cy);
            g2.drawLine(cx, cy - 5, cx, cy + 5);

            // Radial tick marks every 45° with angle label (SPG convention),
            // skipped outside the arc's span so they don't imply collision
            // where there isn't any.
            Color tickColor = new Color(0x00, 0xE5, 0xFF, 140);
            g2.setFont(Fonts.debugSized(DEBUG_TEXT_SIZE));
            for (int deg = 0; deg < 360; deg += 45) {
                if (!loop.isInArc(deg)) continue;
                double a = Math.toRadians(deg);
                // point on the circle: 0°=bottom, 90°=right, 180°=top
                int px = cx + (int) (Math.sin(a) * r);
                int py = cy + (int) (Math.cos(a) * r);
                g2.setColor(tickColor);
                g2.fillOval(px - TICK_MARKER_RADIUS, py - TICK_MARKER_RADIUS, TICK_MARKER_RADIUS * 2, TICK_MARKER_RADIUS * 2);
                // no outline at this size: it swallows the strokes and hurts readability
                g2.drawString(deg + "°", px + TICK_MARKER_RADIUS + 2, py);
            }
        }
    }
}
