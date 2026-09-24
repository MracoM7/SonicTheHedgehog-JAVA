package jsonic.model.world;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import jsonic.utils.GameConstants;
import jsonic.utils.LevelConfig;
import jsonic.model.entity.Player;
import jsonic.model.enemy.BuzzBomber;
import jsonic.model.enemy.Chopper;
import jsonic.model.enemy.Enemy;
import jsonic.model.enemy.Motobug;
import jsonic.model.audio.IAudioPlayer;
import jsonic.model.audio.ISoundEmitter;
import jsonic.model.input.InputSnapshot;
import jsonic.model.item.BridgeLog;
import jsonic.model.item.BridgePost;
import jsonic.model.item.FlowerPurple;
import jsonic.model.item.FlowerYellow;
import jsonic.model.item.Goal;
import jsonic.model.item.Item;
import jsonic.model.item.Ring;
import jsonic.model.item.Rock;
import jsonic.model.item.ScatteredRing;
import jsonic.model.item.Spike;
import jsonic.model.item.Spring;
import jsonic.model.physics.IPhysicsWorld;
import jsonic.model.physics.LoopRegion;
import jsonic.model.tile.TileID;
import jsonic.model.tile.TileMap;
import jsonic.model.tile.TileType;

/**
 * Complete model of a game level: holds all entities and the logical map,
 * updates physics/collisions every frame, and implements IPhysicsWorld as
 * the single access point for Player. Receives LevelConfig instead of
 * hardcoded paths — adding a level means creating a new LevelConfig in
 * LevelRegistry, not modifying this class.
 */
public class Level implements IPhysicsWorld, ISoundEmitter {

    private final TileMap tileMap;
    private final Player player;
    private final LevelConfig config; // kept for reset() and background image
    private final IAudioPlayer audio;
    private final List<Item> items = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();

    // special geometry: analytic loops.
    // Standalone partial arcs (LOOP_ARC_A/B markers, e.g. a valley) are always active, no state.
    private final List<LoopRegion> arcs = new ArrayList<>();

    // A full 360° loop is two ~180° arcs (right: FLOOR->CEILING, left: CEILING->FLOOR), only one
    // solid at a time (SPG's Layer Switcher). Active flips only on a genuine 180° crossing while
    // grounded (see updateLoopSwitches()) - wrapping past 0°/360° on lap completion must NOT flip
    // it back, or reversing right after landing would send the player up the wrong half.
    private static final float SWITCH_MARGIN_DEG = 12f; // see loadLoopsFromMap() for why each half needs this
    private static final class FullLoop {
        final LoopRegion right; // FLOOR(0°) -> CEILING(180°)
        final LoopRegion left;  // CEILING(180°) -> FLOOR(360°)
        boolean rightActive = true;
        float prevAngle;
        boolean hasPrevAngle = false; // no crossing can be detected without a prior sample

        FullLoop(LoopRegion right, LoopRegion left) {
            this.right = right;
            this.left = left;
        }

        LoopRegion active() { return rightActive ? right : left; }
        boolean containsPoint(float px, float py) { return right.containsPoint(px, py); }
    }
    private final List<FullLoop> fullLoops = new ArrayList<>();

    private int ringCount = 0;
    private int score = 0;
    private int timeFrames = 0; // counts up while playing; @60fps, see getTimeFrames()

    // Scoring verified against the real Sonic 1 disassembly (sonicretro/s1disasm) - see tesina for sources.
    private static final int RING_BONUS_PER_RING = 100; // end-of-act ring tally; a ring picked up mid-level scores 0 directly (Rings.asm never calls AddPoints)

    // Airborne combo chain (ReactToItem.asm's React_BadnikHit), not a flat value per enemy; resets on landing.
    private static final int[] ENEMY_COMBO_POINTS = {100, 200, 500, 1000}; // 1st, 2nd, 3rd, 4th-15th
    private static final int ENEMY_COMBO_MEGA_COUNT = 16;
    private static final int ENEMY_COMBO_MEGA_POINTS = 10000;
    private int enemyComboCount = 0;
    private boolean wasOnGroundForCombo = true;

    // Time bonus at goal touch, 15s buckets from 0:00 (0D Signpost.asm's TimeBonuses); 0 past 5:00.
    private static final int[] TIME_BONUS_TABLE = {
        50000, 50000, 10000, 5000,  // 0:00-0:59
         4000,  4000,  3000, 3000,  // 1:00-1:59
         2000,  2000,  2000, 2000,  // 2:00-2:59
         1000,  1000,  1000, 1000,  // 3:00-3:59
          500,   500,   500,  500,  // 4:00-4:59
    };

    // end-of-act score breakdown, captured at goal touch for the results screen
    private int lastTimeBonus = 0;
    private int lastRingBonus = 0;

    private static final int STARTING_LIVES = 3;
    private int lives = STARTING_LIVES;
    private boolean gameOver = false;

    // see GameConstants.TIME_LIMIT_FRAMES — shared with HUDRenderer's last-minute TIME blink

    // while dying, everything else freezes and Sonic free-falls off screen (see update());
    // DEATH_MAX_FRAMES is a backstop in case the fall margin is never reached
    private static final int DEATH_MAX_FRAMES = 180; // 3s @ 60fps
    private static final float DEATH_FALL_MARGIN = GameConstants.SCREEN_HEIGHT;
    private int deathTimer = 0;
    private float deathStartY = 0f;
    private boolean pendingGameOver = false; // outcome killPlayer() decided, applied once the fall resolves
    private boolean timeOver = false; // true only for a timeout death; tells GameOverRenderer to show TIME OVER

    // true for the single frame after restartAct() teleports the player, so the
    // Controller can hard-snap the Camera instead of letting it glide in
    private boolean justRespawned = false;

    // set from loadLevel()'s spawn scan; isGoalReached() reads its spin animation
    // directly so the level-complete transition waits for it to finish
    private Goal goal;

    // constructor

    public Level(LevelConfig config, IAudioPlayer audio) {
        this.config = config;
        this.audio = audio;
        this.tileMap = new TileMap(config.mapCsvPath);
        this.player = new Player(this, this, config.startCol, config.startRow);
        loadLevel();
    }

    // loading

    /**
     * Scans the map and instantiates dynamic objects from spawn tiles,
     * then builds the loops. Also called by reset() after tileMap.reload()
     * restores the spawn tiles — and (re)starts the level music each time,
     * including after a Game Over continue.
     */
    private void loadLevel() {
        audio.playMusic(config.musicPath);

        items.clear();
        enemies.clear();
        arcs.clear();
        fullLoops.clear();

        // 1. Scan spawn tiles (rings, spikes, goal, enemies, future checkpoints)
        for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
            for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                int tileID = tileMap.getTileAt(col, row);

                if (tileID == TileID.ROCK_SPAWN) {
                    spawnSolidObstacle(new Rock(), col, row);
                    continue;
                }
                if (tileID == TileID.SPIKE_SPAWN) {
                    spawnSolidObstacle(new Spike(), col, row);
                    continue;
                }

                Item spawnedItem = switch (tileID) {
                    case TileID.RING_SPAWN -> new Ring();
                    case TileID.GOAL_SPAWN -> new Goal(this);
                    case TileID.FLOWER_YELLOW_SPAWN -> new FlowerYellow();
                    case TileID.FLOWER_PURPLE_A_SPAWN -> new FlowerPurple(false);
                    case TileID.FLOWER_PURPLE_B_SPAWN -> new FlowerPurple(true);
                    case TileID.SPRING_YELLOW_SPAWN -> new Spring(this);
                    default -> null;
                };
                if (spawnedItem != null) {
                    spawnedItem.worldX = col * GameConstants.TILE_SIZE;
                    spawnedItem.worldY = row * GameConstants.TILE_SIZE;
                    // flowers sit a couple pixels too high at the marker's exact tile-top position
                    if (spawnedItem instanceof FlowerYellow || spawnedItem instanceof FlowerPurple) {
                        spawnedItem.worldY += 2;
                    }
                    items.add(spawnedItem);
                    if (spawnedItem instanceof Goal g) goal = g;
                    tileMap.setTileAt(col, row, TileID.AIR);
                    continue;
                }

                Enemy spawnedEnemy = switch (tileID) {
                    case TileID.ENEMY_MOTOBUG_SPAWN -> new Motobug();
                    case TileID.ENEMY_BUZZBOMBER_SPAWN -> new BuzzBomber();
                    case TileID.ENEMY_CHOPPER_SPAWN -> new Chopper();
                    default -> null;
                };
                if (spawnedEnemy != null) {
                    spawnedEnemy.worldX = col * GameConstants.TILE_SIZE;
                    spawnedEnemy.worldY = row * GameConstants.TILE_SIZE;
                    enemies.add(spawnedEnemy);
                    tileMap.setTileAt(col, row, TileID.AIR);
                }
            }
        }

        // 2. Read loops directly from the map via tile markers
        loadLoopsFromMap();

        // 3. Bridges: paired BRIDGE_LEFT_POST/BRIDGE_RIGHT_POST markers
        loadBridgesFromMap();
    }

    /**
     * Spawns an item whose footprint should also block the player like
     * ordinary ground, not just trigger onCollision() on overlap — Rock
     * and Spike. Marks every tile covered by the item's solidArea as
     * FULL, so Player's sensor-based physics treats it as real geometry.
     */
    private void spawnSolidObstacle(Item item, int col, int row) {
        item.worldX = col * GameConstants.TILE_SIZE;
        item.worldY = row * GameConstants.TILE_SIZE;
        items.add(item);

        int ts = GameConstants.TILE_SIZE;
        int startCol = col + Math.floorDiv(item.solidArea.x, ts);
        int startRow = row + Math.floorDiv(item.solidArea.y, ts);
        int tilesWide = item.solidArea.width / ts;
        int tilesTall = item.solidArea.height / ts;
        for (int r = startRow; r < startRow + tilesTall; r++) {
            for (int c = startCol; c < startCol + tilesWide; c++) {
                tileMap.setTileAt(c, r, TileID.FULL);
            }
        }
    }

    /**
     * Scans the map for the 4 loop marker tiles:
     *   LOOP_CENTER → centre of the circle (cx, cy)
     *   LOOP_BOTTOM → same column as CENTER, defines the radius
     *   LOOP_ARC_A  → angular endpoint A (optional, associated with the nearest CENTER)
     *   LOOP_ARC_B  → angular endpoint B (optional, associated with the nearest CENTER)
     *
     * For each CENTER found: an explicit ARC_A/ARC_B pair becomes one always-active partial arc
     * (added to `arcs`), otherwise a full loop becomes a FullLoop pairing its two 180° halves
     * (added to `fullLoops`) - see those fields' comments for why. All markers are then removed
     * from the grid.
     *
     * Angle convention: atan2(dx_tile, dy_tile) relative to CENTER →
     *   below = 0°, right = 90°, above = 180°, left = 270°.
     */
    private void loadLoopsFromMap() {
        List<int[]> centers = new ArrayList<>();
        List<int[]> bottoms = new ArrayList<>();
        List<int[]> arcAs = new ArrayList<>();
        List<int[]> arcBs = new ArrayList<>();

        // collect (col, row) positions of all markers
        for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
            for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                int id = tileMap.getTileAt(col, row);
                switch (id) {
                    case TileID.LOOP_CENTER: centers.add(new int[]{col, row}); break;
                    case TileID.LOOP_BOTTOM: bottoms.add(new int[]{col, row}); break;
                    case TileID.LOOP_ARC_A: arcAs.add(new int[]{col, row}); break;
                    case TileID.LOOP_ARC_B: arcBs.add(new int[]{col, row}); break;
                    default: break;
                }
            }
        }

        // Remove markers from the grid - not solid, LoopRegion/FullLoop handles physics there.
        // LOOP_BOTTOM is the exception: a THREE_FOUR fallback (not AIR or FULL), sitting lower
        // than the circle's own surface so it never wins the "nearest surface" comparison and
        // cause a flat-angle stutter entering the loop. It only matters as a safety net for the
        // one edge case where the circle itself finds nothing, right at the 180° switch margin.
        for (int[] c : centers) tileMap.setTileAt(c[0], c[1], TileID.AIR);
        for (int[] b : bottoms) tileMap.setTileAt(b[0], b[1], TileID.THREE_FOUR);
        for (int[] a : arcAs) tileMap.setTileAt(a[0], a[1], TileID.AIR);
        for (int[] b : arcBs) tileMap.setTileAt(b[0], b[1], TileID.AIR);

        // build a LoopRegion for each CENTER
        for (int[] center : centers) {
            int cc = center[0], cr = center[1];

            // world-pixel centre: top-left corner of the LOOP_CENTER tile
            float cx = cc * GameConstants.TILE_SIZE;
            float cy = cr * GameConstants.TILE_SIZE;

            // look for LOOP_BOTTOM on the same column to determine the radius
            float radius = -1f;
            for (int[] bottom : bottoms) {
                if (bottom[0] == cc) {
                    radius = Math.abs(bottom[1] - cr) * (float) GameConstants.TILE_SIZE;
                    break;
                }
            }
            if (radius <= 0f) {
                System.err.println("Level: LOOP_CENTER col=" + cc + " row=" + cr
                    + " has no LOOP_BOTTOM in the same column — skipped.");
                continue;
            }

            // ARC_A/ARC_B are matched to whichever CENTER they're closest to, not to
            // this centre by distance alone, or a lone arc pair could get stolen by every other loop
            int[] nearestA = findNearest(arcAs, cc, cr);
            int[] nearestB = findNearest(arcBs, cc, cr);

            if (nearestA != null && nearestB != null
                    && isClosestCenter(nearestA, centers, cc, cr) && isClosestCenter(nearestB, centers, cc, cr)) {
                float angleA = tileAngle(nearestA[0], nearestA[1], cc, cr);
                float angleB = tileAngle(nearestB[0], nearestB[1], cc, cr);
                arcs.add(new LoopRegion(cx, cy, radius, angleA, angleB));
            } else {
                // Each half overlaps the OTHER only around the 180° switch point, never around
                // 0°/360° (no overlap needed there, LOOP_BOTTOM bridges it physically). Without
                // this margin, castSensor() (which only queries the active half) stops finding
                // ground the instant the player crosses 180°, before updateLoopSwitches() - which
                // only runs while grounded - gets a chance to see the far side and flip.
                LoopRegion right = new LoopRegion(cx, cy, radius, 0f, 180f + SWITCH_MARGIN_DEG);
                LoopRegion left = new LoopRegion(cx, cy, radius, 180f - SWITCH_MARGIN_DEG, 360f);
                fullLoops.add(new FullLoop(right, left));
            }
        }
    }

    /**
     * Scans each row for BRIDGE_LEFT_POST/BRIDGE_RIGHT_POST pairs and fills
     * the span between them with a post, a BridgeLog per tile, and a
     * closing post. Both markers are cleared to AIR afterwards.
     */
    private void loadBridgesFromMap() {
        for (int row = 0; row < GameConstants.MAX_WORLD_ROW; row++) {
            int leftCol = -1;
            for (int col = 0; col < GameConstants.MAX_WORLD_COL; col++) {
                int tileID = tileMap.getTileAt(col, row);
                if (tileID == TileID.BRIDGE_LEFT_POST) {
                    leftCol = col;
                } else if (tileID == TileID.BRIDGE_RIGHT_POST && leftCol >= 0) {
                    spawnBridge(leftCol, col, row);
                    tileMap.setTileAt(leftCol, row, TileID.AIR);
                    tileMap.setTileAt(col, row, TileID.AIR);
                    leftCol = -1;
                }
            }
        }
    }

    /**
     * The posts sit at the marker row; the log chain and walkable surface
     * sit one row below, filled with solid FULL ground — only the sag
     * (BridgeLog) is purely visual.
     */
    private void spawnBridge(int leftCol, int rightCol, int row) {
        int postY = row * GameConstants.TILE_SIZE;
        int logRow = row + 1;
        int logY = logRow * GameConstants.TILE_SIZE;

        BridgePost left = new BridgePost(false);
        left.worldX = leftCol * GameConstants.TILE_SIZE;
        left.worldY = postY;
        items.add(left);

        for (int col = leftCol; col <= rightCol; col++) {
            tileMap.setTileAt(col, logRow, TileID.FULL);
        }
        for (int col = leftCol + 1; col < rightCol; col++) {
            items.add(new BridgeLog(player, col * GameConstants.TILE_SIZE, logY));
        }

        BridgePost right = new BridgePost(true);
        right.worldX = rightCol * GameConstants.TILE_SIZE;
        right.worldY = postY;
        items.add(right);
    }

    /**
     * Returns the tile in the list closest to (refCol, refRow) by Euclidean
     * tile distance, or null if the list is empty.
     */
    private static int[] findNearest(List<int[]> tiles, int refCol, int refRow) {
        int[] best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int[] t : tiles) {
            int dc = t[0] - refCol, dr = t[1] - refRow;
            int dist = dc * dc + dr * dr;
            if (dist < bestDist) { bestDist = dist; best = t; }
        }
        return best;
    }

    /**
     * True if (refCol, refRow) is the closest centre to `marker` among ALL
     * centres in `centers` — i.e. this marker isn't claimed by some other,
     * nearer loop elsewhere on the map.
     */
    private static boolean isClosestCenter(int[] marker, List<int[]> centers, int refCol, int refRow) {
        int distToRef = distSqTiles(marker, refCol, refRow);
        for (int[] c : centers) {
            if (c[0] == refCol && c[1] == refRow) continue;
            if (distSqTiles(marker, c[0], c[1]) < distToRef) return false;
        }
        return true;
    }

    private static int distSqTiles(int[] tile, int col, int row) {
        int dc = tile[0] - col, dr = tile[1] - row;
        return dc * dc + dr * dr;
    }

    /**
     * Computes the angle of tile (col, row) relative to centre (refCol, refRow)
     * using the loop convention: atan2(dx, dy) →
     *   below = 0°, right = 90°, above = 180°, left = 270°.
     */
    private static float tileAngle(int col, int row, int refCol, int refRow) {
        float dx = col - refCol;
        float dy = row - refRow;
        float angle = (float) Math.toDegrees(Math.atan2(dx, dy));
        return (angle < 0f) ? angle + 360f : angle;
    }

    // update

    public void update(InputSnapshot input) {
        if (deathTimer > 0) {
            deathTimer--;
            player.update(input); // lets the death-bounce fall play out; Player ignores input/collision while dying
            boolean fellOffScreen = player.getY() > deathStartY + DEATH_FALL_MARGIN;
            if (fellOffScreen || deathTimer == 0) {
                deathTimer = 0;
                if (pendingGameOver) { gameOver = true; pendingGameOver = false; }
                else { restartAct(); }
            }
            return; // frozen: no items/enemies update, no timer tick, while the death fall plays out
        }

        timeFrames++;
        if (timeFrames >= GameConstants.TIME_LIMIT_FRAMES) {
            timeOver = true;
            killPlayer();
            return;
        }

        items.removeIf(item -> item.pendingDelete);
        for (Item item : items) item.update();

        enemies.removeIf(enemy -> enemy.defeated);
        for (Enemy enemy : enemies) enemy.update(this);

        player.update(input);

        // bottomless-pit death is always fatal; return immediately since
        // killPlayer() can teleport the player, invalidating the checks below
        if (player.getY() > GameConstants.WORLD_HEIGHT) {
            killPlayer();
            return;
        }

        updateLoopSwitches();
        updateEnemyCombo();
        checkItemCollisions();
        checkEnemyCollisions();
    }

    private void checkItemCollisions() {
        int hitX = (int) player.getX() - player.getHitboxRadiusX();
        int hitY = (int) player.getY() - player.getHitboxRadiusY();
        Rectangle playerHitbox = new Rectangle(
            hitX, hitY,
            player.getHitboxRadiusX() * 2,
            player.getHitboxRadiusY() * 2
        );

        // snapshot, not `items` itself: onCollision() can scatter new ScatteredRing
        // objects into `items` mid-loop and trigger ConcurrentModificationException
        for (Item item : new ArrayList<>(items)) {
            if (item.isCollected) continue;
            Rectangle itemArea = new Rectangle(
                item.worldX + item.solidArea.x,
                item.worldY + item.solidArea.y,
                item.solidArea.width,
                item.solidArea.height
            );
            if (!playerHitbox.intersects(itemArea)) continue;

            if (item.requireLandingFromAbove) {
                // falling onto it, or already resting on top; isOnGround() keeps it
                // firing every frame the player stands still, not just on the way down
                float itemCenterY = item.worldY + item.solidArea.y + item.solidArea.height / 2f;
                boolean fromAbove = player.getY() < itemCenterY
                    && (player.isOnGround() || player.getVelocityY() > 0);
                if (!fromAbove) continue;
            }

            item.onCollision(player);
            if (justRespawned || gameOver) return;
        }
    }

    /**
     * Stomp from above defeats the enemy and bounces the player; any other contact hurts the
     * player. While ROLLING or JUMPING, Sonic is curled and defeats an enemy from any side -
     * JUMPING needs this too, since velocityY is still negative jumping up into a hovering enemy.
     * Never defeats while in hit-stun, though: takeDamage() also sets JUMPING for its knockback
     * arc, but that's an involuntary bounce, not an attack, so it must not chain into a second
     * enemy touched mid-flight.
     */
    private void checkEnemyCollisions() {
        int hitX = (int) player.getX() - player.getHitboxRadiusX();
        int hitY = (int) player.getY() - player.getHitboxRadiusY();
        Rectangle playerHitbox = new Rectangle(
            hitX, hitY,
            player.getHitboxRadiusX() * 2,
            player.getHitboxRadiusY() * 2
        );

        for (Enemy enemy : enemies) {
            if (enemy.defeated) continue;
            Rectangle enemyArea = new Rectangle(
                enemy.worldX + enemy.solidArea.x,
                enemy.worldY + enemy.solidArea.y,
                enemy.solidArea.width,
                enemy.solidArea.height
            );
            if (!playerHitbox.intersects(enemyArea)) continue;

            float enemyCenterY = enemy.worldY + enemy.solidArea.y + enemy.solidArea.height / 2f;
            boolean fromAbove = player.getVelocityY() > 0 && player.getY() < enemyCenterY;
            Player.PlayerState state = player.getCurrentState();
            boolean curled = state == Player.PlayerState.ROLLING || state == Player.PlayerState.JUMPING;
            boolean defeats = !player.isHitStun() && (fromAbove || curled);

            if (defeats) {
                enemy.defeated = true;
                score += enemyComboPoints();
                enemyComboCount++;
                player.bounceOffEnemy();
                audio.playSfx("enemy_destroy");
            } else {
                player.takeDamage(1);
            }

            if (justRespawned || gameOver) return;
        }
    }

    /** Points for the next enemy defeat, given how many are already chained in the current combo. */
    private int enemyComboPoints() {
        if (enemyComboCount + 1 >= ENEMY_COMBO_MEGA_COUNT) return ENEMY_COMBO_MEGA_POINTS;
        int index = Math.min(enemyComboCount, ENEMY_COMBO_POINTS.length - 1);
        return ENEMY_COMBO_POINTS[index];
    }

    /** Clears the enemy combo the instant the player lands, exactly like Sonic_ResetOnFloor. */
    private void updateEnemyCombo() {
        boolean onGround = player.isOnGround();
        if (onGround && !wasOnGroundForCombo) enemyComboCount = 0;
        wasOnGroundForCombo = onGround;
    }

    // reset

    /**
     * Replays the current act from scratch, keeping lives/score/timeFrames — the silent
     * death-respawn path. Timer is NOT reset: the original never clears v_time on a normal
     * respawn, only on a genuinely fresh act (restartLevel() below). deathTimer/gameOver
     * flags are cleared defensively since the pause-menu "Riavvia" can land here mid-death.
     */
    public void restartAct() {
        ringCount = 0;
        justRespawned = true;
        deathTimer = 0;
        pendingGameOver = false;
        gameOver = false;
        timeOver = false;
        tileMap.reload();
        loadLevel(); // rebuilds items and loops
        player.reset();
    }

    /**
     * The "Riavvia" pause-menu entry point: a deliberate do-over that also resets score,
     * lives and the timer — the original always pairs "lives = 3" with "clear time" too.
     */
    public void restartLevel() {
        score = 0;
        lives = STARTING_LIVES;
        timeFrames = 0;
        restartAct();
    }

    /**
     * Player has died (0 rings on hit, fell off the map, or time over). Decides the outcome
     * now but applies it once the death-fall finishes (see update()). A time over is always
     * a game over even with lives left (Sonic_HandleDeath forces the card regardless of
     * f_timeover), though lives still decrements either way, just not clamped to 0 unless
     * it's genuinely the last one.
     */
    @Override
    public void killPlayer() {
        if (gameOver || deathTimer > 0) return; // already over, or already dying; ignore a same-window double-kill
        lives--;
        boolean outOfLives = lives <= 0;
        if (outOfLives) lives = 0;
        pendingGameOver = outOfLives || timeOver;
        deathStartY = player.getY();
        player.startDeathPose();
        deathTimer = DEATH_MAX_FRAMES;
        audio.playSfx("death");
        if (pendingGameOver) audio.stopMusic(); // no game-over jingle sourced yet; silence beats the level music looping under that screen
    }

    // debug

    public void toggleDebug() { tileMap.toggleDebugMode(); }

    // getters

    public Player getPlayer() { return player; }
    public List<Item> getItems() { return items; }
    public List<Enemy> getEnemies() { return enemies; }
    public TileMap getTileMap() { return tileMap; }
    @Override
    public int getRingCount() { return ringCount; } // also part of IPhysicsWorld
    @Override
    public int getPlayerX() { return (int) player.getX(); } // part of IPhysicsWorld
    public int getLives() { return lives; }
    public boolean isGameOver() { return gameOver; }
    public boolean isTimeOver() { return timeOver; }
    public boolean isGoalReached() { return goal != null && goal.isAnimationDone(); }
    public int getScore() { return score; }
    public int getTimeFrames() { return timeFrames; }

    /** Reads and clears the one-shot post-respawn flag — see justRespawned above. */
    public boolean consumeJustRespawned() {
        boolean wasJustRespawned = justRespawned;
        justRespawned = false;
        return wasJustRespawned;
    }
    /** All currently-solid loop geometry (standalone arcs + each full loop's active half) — for debug rendering and castSensor(). */
    public List<LoopRegion> getLoops() {
        List<LoopRegion> all = new ArrayList<>(arcs);
        for (FullLoop fl : fullLoops) all.add(fl.active());
        return all;
    }
    // iphysicsworld

    @Override
    public int getWorldWidth() {
        // falls back to the fixed constant for a level with no background image (e.g. LEVEL_TEST)
        if (config.backgroundImage == null) return GameConstants.WORLD_WIDTH;
        return config.backgroundImage.getWidth() * GameConstants.SCALE;
    }

    @Override
    public int getTileIdAt(int worldX, int worldY) {
        return tileMap.getTileIdAt(worldX, worldY);
    }

    @Override
    public int getSurfaceY(int worldX, int worldY) {
        return tileMap.getSurfaceY(worldX, worldY);
    }

    @Override
    public int getSurfaceX(int worldX, int worldY, boolean castRight) {
        return tileMap.getSurfaceX(worldX, worldY, castRight);
    }

    @Override
    public SensorResult castSensor(int tipX, int tipY, SensorDirection dir,
                                   int lookBack, int lookFwd) {
        SensorResult best = tileMap.castSensor(tipX, tipY, dir, lookBack, lookFwd);

        // analytic loop/arc surfaces: active only while inside the circle - the angular
        // span filter (LoopRegion.isInArc()) does the rest, see the `arcs`/`fullLoops` field
        // comments. A full loop only ever offers its currently-active half.
        float px = player.getX(), py = player.getY();
        for (LoopRegion loop : arcs) {
            if (!loop.containsPoint(px, py)) continue;
            SensorResult hit = loop.castSensor(tipX, tipY, dir, lookBack, lookFwd);
            best = nearer(best, hit, dir);
        }
        for (FullLoop fl : fullLoops) {
            if (!fl.containsPoint(px, py)) continue;
            SensorResult hit = fl.active().castSensor(tipX, tipY, dir, lookBack, lookFwd);
            best = nearer(best, hit, dir);
        }
        return best;
    }

    // Updates which half of each full loop is solid, by edge-detecting an actual crossing of the
    // 180° boundary between consecutive grounded frames - not by re-testing "angle < 180" every
    // frame, which can't tell a genuine crossing at the top from wrapping past 0°/360° at the
    // bottom (both land on a small positive angle). Airborne frames neither flip nor reset this.
    private void updateLoopSwitches() {
        float px = player.getX(), py = player.getY();
        boolean grounded = player.isOnGround();
        float angle = ((player.getGroundAngle() % 360f) + 360f) % 360f;
        for (FullLoop fl : fullLoops) {
            if (!fl.containsPoint(px, py)) {
                fl.hasPrevAngle = false;
                continue;
            }
            if (!grounded) continue; // never initialise or flip while airborne

            if (!fl.hasPrevAngle) {
                // first grounded contact with this loop: match whichever half the entry ramp
                // actually put the player on, instead of trusting a fixed default that's only
                // correct for one of the two possible approach directions
                fl.rightActive = angle < 180f;
            } else {
                // shortest signed step from prevAngle to angle, then re-attach it to prevAngle's
                // own scale (unwrapped) so a genuine crossing of 180° can be told apart from a
                // same-sized step that only looks similar after wrapping through 0°/360°
                float delta = ((angle - fl.prevAngle + 540f) % 360f) - 180f;
                float unwrapped = fl.prevAngle + delta;
                float lo = Math.min(fl.prevAngle, unwrapped), hi = Math.max(fl.prevAngle, unwrapped);
                if (lo <= 180f && hi >= 180f) fl.rightActive = !fl.rightActive;
            }
            fl.prevAngle = angle;
            fl.hasPrevAngle = true;
        }
    }

    /** Returns whichever of the two results is hit first in direction dir. */
    private static SensorResult nearer(SensorResult a, SensorResult b, SensorDirection dir) {
        if (!a.found) return b;
        if (!b.found) return a;
        int sign = (dir == SensorDirection.DOWN || dir == SensorDirection.RIGHT) ? +1 : -1;
        return (sign * a.surface <= sign * b.surface) ? a : b;
    }

    @Override
    public boolean isSlope(int tileID) {
        return TileType.isSlope(tileID);
    }

    @Override
    public void addRing() {
        // no direct score here - see the ring-bonus comment near RING_BONUS_PER_RING
        ringCount++;
        audio.playSfx("ring");
    }

    @Override
    public void loseRings() {
        scatterRings(ringCount);
        ringCount = 0;
        audio.playSfx("ring_loss");
    }

    /**
     * Bursts up to SCATTER_MAX_RINGS ScatteredRing objects outward from the
     * player in an even fan, matching (a simplified version of) the classic
     * ring-loss scatter. Capped rather than spawning exactly `count` so a
     * player carrying, say, 40 rings doesn't launch 40 objects at once.
     */
    private static final int SCATTER_MAX_RINGS = 32; // Sonic_LoseRings' own cap (s1disasm)
    private static final float SCATTER_SPEED = 5f * GameConstants.SCALE;

    private void scatterRings(int count) {
        int n = Math.min(count, SCATTER_MAX_RINGS);
        if (n <= 0) return;

        int px = (int) player.getX();
        int py = (int) player.getY();

        for (int i = 0; i < n; i++) {
            double angle = i * (2 * Math.PI / n);
            float vx = (float) (Math.cos(angle) * SCATTER_SPEED);
            float vy = (float) (-Math.sin(angle) * SCATTER_SPEED); // screen Y is inverted: "up" is negative
            items.add(new ScatteredRing(this, px, py, vx, vy));
        }
    }

    // killPlayer() is implemented above (§RESET) — it's also part of the
    // IPhysicsWorld contract, called by Player.takeDamage() when ringCount == 0.

    @Override
    public void reachGoal() {
        // scored at the moment of touch — isGoalReached() itself only flips
        // once the spin animation finishes
        lastTimeBonus = timeBonus();
        lastRingBonus = ringCount * RING_BONUS_PER_RING;
        score += lastTimeBonus + lastRingBonus;
    }

    public int getLastTimeBonus() { return lastTimeBonus; }
    public int getLastRingBonus() { return lastRingBonus; }

    @Override
    public void spawnItem(Item item) {
        items.add(item);
    }

    @Override
    public void playSound(String id) {
        audio.playSfx(id);
    }

    @Override
    public void playJingle(String name) {
        audio.playJingle(name);
    }

    /** Sonic 1's own time bonus table (TimeBonuses in 0D Signpost.asm): the faster the act, the bigger the bonus. */
    private int timeBonus() {
        int index = (timeFrames / 60) / 15; // 15-second buckets
        if (index >= TIME_BONUS_TABLE.length) return 0; // NoTimeBonus: 5:00+
        return TIME_BONUS_TABLE[index];
    }

    public LevelConfig getConfig() {
        return config;
    }
}
