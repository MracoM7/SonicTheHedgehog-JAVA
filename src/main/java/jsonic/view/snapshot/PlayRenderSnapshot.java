package jsonic.view.snapshot;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import jsonic.model.entity.Player;
import jsonic.model.enemy.Enemy;
import jsonic.model.item.Item;
import jsonic.model.physics.LoopRegion;
import jsonic.model.tile.TileMap;
import jsonic.utils.LevelConfig;
import jsonic.view.IRenderSnapshot;

/**
 * Snapshot for the active game state (PlayState).
 *
 * Contains everything that LevelRenderer, PlayerRenderer,
 * ItemRenderer, TileRenderer and HUDRenderer need
 * to draw a complete frame.
 */
public final class PlayRenderSnapshot implements IRenderSnapshot {

    public final Player player;
    public final List<Item> items;
    public final List<Enemy> enemies;
    public final TileMap tileMap;
    public final List<LoopRegion> loops;
    public final LevelConfig config;
    public final BufferedImage backgroundImage; // = config.backgroundImage, kept for existing callers
    public final int cameraX;
    public final int cameraY;
    public final int ringCount;
    public final int score;
    public final int lives;
    public final int timeFrames;
    public final boolean debugMode;
    public final int fps;
    public final boolean timeOver;

    public PlayRenderSnapshot(
            Player player,
            List<Item> items,
            List<Enemy> enemies,
            TileMap tileMap,
            List<LoopRegion> loops,
            LevelConfig config,
            int cameraX,
            int cameraY,
            int ringCount,
            int score,
            int lives,
            int timeFrames,
            boolean debugMode,
            int fps,
            boolean timeOver) {

        this.player = player;
        // Defensive copies: Level keeps mutating items/enemies/loops on the game
        // loop thread while AWT paints whatever snapshot it was last handed —
        // a live reference here throws ConcurrentModificationException.
        this.items = new ArrayList<>(items);
        this.enemies = new ArrayList<>(enemies);
        this.tileMap = tileMap;
        this.loops = new ArrayList<>(loops);
        this.config = config;
        this.backgroundImage = config.backgroundImage;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.ringCount = ringCount;
        this.score = score;
        this.lives = lives;
        this.timeFrames = timeFrames;
        this.debugMode = debugMode;
        this.fps = fps;
        this.timeOver = timeOver;
    }
}
