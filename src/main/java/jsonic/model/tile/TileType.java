package jsonic.model.tile;

import java.awt.Color;

public final class TileType {

    private TileType() {}

    // heightmap tables

    private final static int TS = jsonic.utils.GameConstants.TILE_SIZE;

    public static final int[] HM_SLOPE_R_22 = new int[TS];
    public static final int[] HM_SLOPE_L_22 = new int[TS];

    static {
        for (int i = 0; i < TS; i++) {

            // 22.5° slope - TileMap already compresses X across a 2-tile pair, do NOT halve i
            // again here or the surface never reaches the top/bottom of the tile within a pair
            HM_SLOPE_R_22[i] = (TS - 1) - i;
            HM_SLOPE_L_22[i] = i;
        }
    }

    // debug colors

    static final Color[] DEBUG_COLORS = {
        new Color(0x1A, 0x1A, 0x1A, 160), // 0 FULL - black
        new Color(0x40, 0x40, 0x40, 160), // 1 THREE_FOUR - anthracite
        new Color(0x80, 0x80, 0x80, 160), // 2 HALF - grey
        new Color(0xB3, 0xB3, 0xB3, 160), // 3 ONE_FOUR - light grey
        new Color(0xAA, 0xAA, 0xAA, 80), // 4 free - grey
        new Color(0x99, 0xCC, 0xFF, 160), // 5 SLOPE_R_22 - pale blue
        new Color(0xAA, 0xAA, 0xAA, 80), // 6 free - grey
        new Color(0xAA, 0xAA, 0xAA, 80), // 7 free - grey
        new Color(0xFF, 0xE0, 0xB3, 160), // 8 SLOPE_L_22 - cream
        new Color(0xAA, 0xAA, 0xAA, 80), // 9 free - grey
        new Color(0x00, 0xFF, 0xCC, 160), // 10 LOOP_CENTER - cyan
        new Color(0x00, 0xCC, 0x99, 160), // 11 LOOP_BOTTOM - teal
        new Color(0xFF, 0xFF, 0x00, 160), // 12 LOOP_ARC_A - yellow
        new Color(0xFF, 0x88, 0x00, 160), // 13 LOOP_ARC_B - bright orange
        new Color(0xAA, 0xAA, 0xAA, 80), // 14 free - grey
        new Color(0xAA, 0xAA, 0xAA, 80), // 15 free - grey
        new Color(0xAA, 0xAA, 0xAA, 80), // 16 free - grey
        new Color(0x00, 0xFF, 0x00, 160), // 17 RING_SPAWN - green
        new Color(0xFF, 0x00, 0x00, 160), // 18 SPIKE_SPAWN - red
        new Color(0xFF, 0xFF, 0xFF, 160), // 19 GOAL_SPAWN - white
        new Color(0x80, 0x00, 0x80, 160), // 20 ENEMY_MOTOBUG_SPAWN - purple
        new Color(0xFF, 0xD7, 0x00, 160), // 21 FLOWER_YELLOW_SPAWN - gold
        new Color(0xFF, 0x69, 0xB4, 160), // 22 FLOWER_PURPLE_A_SPAWN - hot pink
        new Color(0x8B, 0x45, 0x13, 160), // 23 FLOWER_PURPLE_B_SPAWN - saddle brown
        new Color(0xDE, 0xB8, 0x87, 160), // 24 BRIDGE_LEFT_POST - burlywood
        new Color(0xA0, 0x52, 0x2D, 160), // 25 BRIDGE_RIGHT_POST - sienna
        new Color(0x46, 0x82, 0xB4, 160), // 26 ENEMY_BUZZBOMBER_SPAWN - steel blue
        new Color(0x20, 0xB2, 0xAA, 160), // 27 ENEMY_CHOPPER_SPAWN - light sea green
        new Color(0x9C, 0x8A, 0xC8, 160), // 28 ROCK_SPAWN - lavender grey
        new Color(0xD2, 0xB4, 0x14, 160), // 29 SPRING_YELLOW_SPAWN - yellow
    };

    public static Color getDebugColor(int tileID) {
        if (tileID >= 0 && tileID < DEBUG_COLORS.length) return DEBUG_COLORS[tileID];
        return new Color(255, 255, 255, 100);
    }

    // physics queries (static lookups by tile id)

    /**
     * Surface angle in degrees (0°-360°, counter-clockwise).
     *   0°   = flat floor
     *  90°   = right wall
     * 180°   = ceiling
     * 270°   = left wall
     */
    public static float getAngle(int tileID) {
        switch (tileID) {
            case TileID.FULL: case TileID.THREE_FOUR: case TileID.HALF: case TileID.ONE_FOUR: return 0.0f;
            case TileID.SLOPE_R_22: return 22.5f;
            case TileID.SLOPE_L_22: return 337.5f;
            default: return 0.0f;
        }
    }

    /** Tile heightmap (top-to-surface distance per X column); null for rectangular tiles, markers and air. */
    public static int[] getHeightmap(int tileID) {
        switch (tileID) {
            case TileID.SLOPE_R_22: return HM_SLOPE_R_22;
            case TileID.SLOPE_L_22: return HM_SLOPE_L_22;
            default: return null;
        }
    }

    /** True if the tile has a non-rectangular surface (slope). */
    public static boolean isSlope(int tileID) {
        return tileID == TileID.SLOPE_R_22 || tileID == TileID.SLOPE_L_22;
    }

    /** Local Y (from the tile top) where THREE_FOUR/HALF/ONE_FOUR become solid; 0 for any other tile. */
    public static int getSolidTopOffset(int tileID) {
        switch (tileID) {
            case TileID.THREE_FOUR: return TS / 4;
            case TileID.HALF: return TS / 2;
            case TileID.ONE_FOUR: return TS * 3 / 4;
            default: return 0;
        }
    }
}
