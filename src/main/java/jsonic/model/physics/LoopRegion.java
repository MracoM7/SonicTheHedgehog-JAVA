package jsonic.model.physics;

import jsonic.model.physics.IPhysicsWorld.SensorDirection;
import jsonic.model.physics.IPhysicsWorld.SensorResult;
import jsonic.model.tile.TileID;

/**
 * Loop-the-loop / concave arc as an analytic circle instead of tiles - a heightmap can only
 * describe one surface per column, so it can't represent the near-vertical sides of a loop.
 * Sonic runs on the inner face; the normal always points toward the centre.
 * <p>
 * A purely geometric arc between arcStartDeg and arcEndDeg (smaller to larger, with 360° wrap) -
 * this class only answers "is this point inside the circle" and "where does a sensor ray hit
 * this arc's span". It has no notion of a full 360° loop: that's Level's FullLoop, which pairs
 * two of these (right and left half) and decides which one is currently solid.
 * <p>
 * Angle convention (same as TileType/Player): bottom = 0°, right = 90°, top = 180°, left = 270°.
 * Built from the LOOP_CENTER/LOOP_BOTTOM/LOOP_ARC_A/LOOP_ARC_B map tiles by Level.loadLoopsFromMap().
 */
public final class LoopRegion {

    private final float cx; // centre X (world px)
    private final float cy; // centre Y (world px)
    private final float radius;

    private final float arcStartDeg; // 0-360
    private final float arcEndDeg; // 0-360

    public LoopRegion(float centerX, float centerY, float radius,
                      float arcStartDeg, float arcEndDeg) {
        this.cx = centerX;
        this.cy = centerY;
        this.radius = radius;
        this.arcStartDeg = arcStartDeg;
        this.arcEndDeg = arcEndDeg;
    }

    // getters (also used by LoopRenderer for debug drawing)

    public float getCenterX() { return cx; }
    public float getCenterY() { return cy; }
    public float getRadius() { return radius; }
    public float getArcStart() { return arcStartDeg; }
    public float getArcEnd() { return arcEndDeg; }

    // physics api

    // true if the point is inside the circle - the "activation volume"; no angular
    // filter here, castSensor() applies that
    public boolean containsPoint(float px, float py) {
        float dx = px - cx, dy = py - cy;
        return dx * dx + dy * dy < radius * radius;
    }

    // intersects the sensor ray with the loop/arc circle, filtering out hits outside this
    // half's angular span, and returns the nearest surface within [tip-lookBack, tip+lookFwd]
    // along the cast axis
    public SensorResult castSensor(int tipX, int tipY, SensorDirection dir,
                                   int lookBack, int lookFwd) {
        boolean vertical = (dir == SensorDirection.DOWN || dir == SensorDirection.UP);
        int sign = (dir == SensorDirection.DOWN || dir == SensorDirection.RIGHT) ? +1 : -1;

        // fixed axis (perpendicular to cast) and variable axis (along cast)
        float fixed = vertical ? tipX : tipY;
        float centerAlongCast = vertical ? cy : cx;
        float centerOnFixed = vertical ? cx : cy;

        float disc = radius * radius - (fixed - centerOnFixed) * (fixed - centerOnFixed);
        if (disc < 0) return SensorResult.NONE; // the ray does not intersect the circle
        float root = (float) Math.sqrt(disc);

        float s1 = centerAlongCast - root;
        float s2 = centerAlongCast + root;

        int tip = vertical ? tipY : tipX;
        int winLo = Math.min(tip + sign * (-lookBack), tip + sign * lookFwd);
        int winHi = Math.max(tip + sign * (-lookBack), tip + sign * lookFwd);

        int best = TileID.NO_SURFACE;
        int bestProj = Integer.MAX_VALUE;
        float bestAngle = 0f;

        for (float s : new float[]{ s1, s2 }) {
            int si = Math.round(s);
            if (si < winLo || si > winHi) continue;

            // angle of the impact point on the circle
            float hx = vertical ? fixed : si;
            float hy = vertical ? si : fixed;
            float hitAngle = (float) Math.toDegrees(Math.atan2(hx - cx, hy - cy));
            if (hitAngle < 0) hitAngle += 360f;

            if (!isInArc(hitAngle)) continue;

            int proj = sign * si; // hit first along dir
            if (proj < bestProj) {
                bestProj = proj;
                best = si;
                bestAngle = hitAngle;
            }
        }

        if (best == TileID.NO_SURFACE) return SensorResult.NONE;
        return new SensorResult(true, best, bestAngle);
    }

    // true if angle is within [arcStartDeg, arcEndDeg], wrapping through 0° - public so
    // debug rendering can draw just this half's actual span
    public boolean isInArc(float angle) {
        float a = ((angle % 360f) + 360f) % 360f;
        float s = ((arcStartDeg % 360f) + 360f) % 360f;
        float e = ((arcEndDeg % 360f) + 360f) % 360f;
        if (s <= e) return a >= s && a <= e;
        else return a >= s || a <= e; // wrap-around through 0°
    }
}
