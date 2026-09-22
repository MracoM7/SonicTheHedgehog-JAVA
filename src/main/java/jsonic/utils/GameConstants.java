package jsonic.utils;

/** Shared sizing and timing constants used across all three layers. */
public final class GameConstants {

    private GameConstants() {}

    // tile
    public static final int ORIGINAL_TILE_SIZE = 16;
    public static final int SCALE = 3;
    public static final int TILE_SIZE = ORIGINAL_TILE_SIZE * SCALE; // 48px

    // screen — matches the real Genesis resolution (320x224px = 20x14 tiles of 16px)
    public static final int MAX_SCREEN_COL = 20;
    public static final int MAX_SCREEN_ROW = 14;
    public static final int SCREEN_WIDTH = TILE_SIZE * MAX_SCREEN_COL; // 960px
    public static final int SCREEN_HEIGHT = TILE_SIZE * MAX_SCREEN_ROW; // 672px

    // world
    public static final int MAX_WORLD_COL = 640;
    public static final int MAX_WORLD_ROW = 80;
    public static final int WORLD_WIDTH = TILE_SIZE * MAX_WORLD_COL;
    public static final int WORLD_HEIGHT = TILE_SIZE * MAX_WORLD_ROW;

    // timing
    public static final int FPS = 60;
    public static final int TIME_LIMIT_FRAMES = 10 * 60 * FPS; // 10:00 forces a time over (HUD Update.asm); shared with HUDRenderer's TIME blink threshold

}