package jsonic.model.tile;

public final class TileID {

    private TileID() {}

    public static final int AIR = -1;

    public static final int FULL = 0;
    public static final int THREE_FOUR = 1;
    public static final int HALF = 2;
    public static final int ONE_FOUR = 3;

    // ID 4, 6, 7, 9 = free for future use (were the 45°/67.5° ramps, unused - see test_slope.csv)

    public static final int SLOPE_R_22 = 5;
    public static final int SLOPE_L_22 = 8;

    public static final int LOOP_CENTER = 10;
    public static final int LOOP_BOTTOM = 11; // same column as loop center
    public static final int LOOP_ARC_A = 12;
    public static final int LOOP_ARC_B = 13;

    // ID 14, 15, 16 = free for future use

    public static final int RING_SPAWN = 17;
    public static final int SPIKE_SPAWN = 18; // solid + damages on landing, see Level.spawnSolidObstacle()
    public static final int GOAL_SPAWN = 19;
    public static final int ENEMY_MOTOBUG_SPAWN = 20;

    // purely decorative, no collision. Two markers for the purple flower so neighbouring
    // placements can be given opposite bob phases (see FlowerPurple)
    public static final int FLOWER_YELLOW_SPAWN = 21;
    public static final int FLOWER_PURPLE_A_SPAWN = 22;
    public static final int FLOWER_PURPLE_B_SPAWN = 23;

    // paired markers one row above the bridge's walkable surface (that row is ordinary FULL
    // tiles, the bridge itself is a visual overlay); every log in between is filled automatically
    public static final int BRIDGE_LEFT_POST = 24;
    public static final int BRIDGE_RIGHT_POST = 25;

    public static final int ENEMY_BUZZBOMBER_SPAWN = 26;
    public static final int ENEMY_CHOPPER_SPAWN = 27;

    public static final int ROCK_SPAWN = 28; // solid obstacle, see Level.spawnSolidObstacle()
    public static final int SPRING_YELLOW_SPAWN = 29;

    public static final int NO_SURFACE = Integer.MAX_VALUE;
}