package jsonic.model.physics;

import jsonic.model.item.Item;

/**
 * Abstracts the physical world from the Player's perspective — Player has
 * no direct knowledge of Level, TileMap or TileType. Implemented by Level.
 */
public interface IPhysicsWorld {

    // directional sensors (SPG quadrant model)
    // world-frame cast direction; Player rotates these with its own PhysicsMode so "down" becomes
    // DOWN on the floor, RIGHT on the right wall, UP on the ceiling, LEFT on the left wall
    enum SensorDirection { DOWN, UP, LEFT, RIGHT }

    // result of a sensor cast: surface hit (world coordinate on the cast axis) and its angle
    final class SensorResult {
        public final boolean found;
        public final int surface; // worldY for DOWN/UP, worldX for LEFT/RIGHT
        public final float angle; // degrees 0-360, angle of the hit tile

        public static final SensorResult NONE = new SensorResult(false, jsonic.model.tile.TileID.NO_SURFACE, 0f);

        public SensorResult(boolean found, int surface, float angle) {
            this.found = found;
            this.surface = surface;
            this.angle = angle;
        }
    }

    // casts a sensor from (tipX, tipY) along dir, returns the nearest surface within
    // [tip-lookBack, tip+lookFwd], or SensorResult.NONE if nothing is in that window
    public SensorResult castSensor(int tipX, int tipY, SensorDirection dir, int lookBack, int lookFwd);

    // grid queries
    /** ID of the tile at the given world coordinates. Returns TileID.AIR outside the boundaries. */
    public int getTileIdAt(int worldX, int worldY);

    /**
     * Exact Y of the walkable surface at the given world coordinates.
     * @return surface Y in pixels, or TileID.NO_SURFACE if air.
     */
    public int getSurfaceY(int worldX, int worldY);

    /**
     * Exact X of a wall surface at the given world coordinates.
     * @param castRight true = looks for the left face of the wall to the right of worldX
     * @return surface X in pixels, or TileID.NO_SURFACE if not found.
     */
    public int getSurfaceX(int worldX, int worldY, boolean castRight);

    /** True if the tile has a non-rectangular surface (ramp or curve). */
    public boolean isSlope(int tileID);

    /** Right edge of the level in world pixels (the level's background image width, scaled). */
    public int getWorldWidth();

    /** Player's current world X — the only way an Enemy is allowed to know where the player is. */
    public int getPlayerX();

    // player-to-world notifications
    /** Notifies that the player has collected a ring. */
    public void addRing();

    /** Current ring count — Player checks this on taking damage to decide lose-rings vs. die. */
    public int getRingCount();

    /** Clears the ring count after the player takes damage while holding rings. */
    public void loseRings();

    /** Notifies that the player has died (0 rings on hit, or fell off the map). */
    public void killPlayer();

    /** Notifies that the player has reached the end-of-level goal. */
    public void reachGoal();

    /** Adds a dynamically-created Item at runtime, e.g. a badnik's projectile. */
    public void spawnItem(Item item);
}
