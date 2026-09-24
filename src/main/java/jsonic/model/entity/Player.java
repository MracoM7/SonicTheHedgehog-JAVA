package jsonic.model.entity;

import jsonic.model.audio.ISoundEmitter;
import jsonic.model.input.InputSnapshot;
import jsonic.model.physics.IPhysicsWorld;
import jsonic.model.physics.IPhysicsWorld.SensorDirection;
import jsonic.model.physics.IPhysicsWorld.SensorResult;
import jsonic.model.tile.TileID;
import jsonic.utils.GameConstants;

/**
 * Player physics: quadrant-based ground/air movement per the Sonic Physics Guide.
 * View-agnostic; only maintains position, velocity and angle state.
 */
public class Player extends Entity implements ICameraTarget, ICollector {

    private final IPhysicsWorld world;
    private final ISoundEmitter soundEmitter;

    private final int spawnCol;
    private final int spawnRow;

    private static final float SCALE_FACTOR = GameConstants.SCALE;

    // bounding box radii (SPG:Characters) - centered on (x,y), see tesina "Hitbox e raggi"
    private final int BASE_RADIUS_X = 9 * (int) SCALE_FACTOR;
    private final int BASE_RADIUS_Y = 19 * (int) SCALE_FACTOR;
    private final int ROLL_RADIUS_X = 7 * (int) SCALE_FACTOR;
    private final int ROLL_RADIUS_Y = 14 * (int) SCALE_FACTOR;

    // hitbox for item/enemy collisions - smaller than the radii above, more forgiving on contact
    private final int HITBOX_RADIUS_X = 8 * (int) SCALE_FACTOR;
    private final int HITBOX_RADIUS_Y = 16 * (int) SCALE_FACTOR;

    private final int PUSH_RADIUS = 10 * (int) SCALE_FACTOR;

    // current radius pair (BASE_ or ROLL_) for whichever pose is active - drives both the
    // physical hitbox and, added to (x,y), the ground sensor tip position (footSensor())
    private int currentRadiusX = BASE_RADIUS_X;
    private int currentRadiusY = BASE_RADIUS_Y;

    // distance in from the head/feet edges for the two checkGroundObstacle() probes
    private final int WALL_PROBE_INSET = 6 * (int) SCALE_FACTOR;

    // search height for checkObstacleAt() to tell a normal step from a real wall
    private final int WALL_TOP_SEARCH = 8 * GameConstants.TILE_SIZE;

    // ground sensor extension (uphill/downhill reach)
    private final int SENSOR_LOOK_UP = 8 * (int) SCALE_FACTOR;
    private final int SENSOR_LOOK_DOWN = 14 * (int) SCALE_FACTOR;

    // fixed-point (value/256) constants, from SPG:Forces
    private final float GRAVITY = (56f / 256f) * SCALE_FACTOR;
    private final float HURT_GRAVITY = (48f / 256f) * SCALE_FACTOR; // SPG:Getting_Hit, applied instead of GRAVITY while hitStun
    private final float JUMP_STRENGTH = -(6f + (128f / 256f)) * SCALE_FACTOR;
    private final float SHORT_JUMP_CUT = -4.0f * SCALE_FACTOR;
    private final float MAX_SPEED = 6.0f * SCALE_FACTOR;
    // shared with PlayerRenderer (getFastRunThreshold()): same cutoff decides both when the skid
    // pose kicks in here and when the run sprite switches to the legs-spinning fast-run sprite
    private final float FAST_RUN_THRESHOLD = MAX_SPEED * 0.8f;
    private final float ABS_MAX_SPEED = 16.0f * SCALE_FACTOR;
    private final float ACCELERATION = (12f / 256f) * SCALE_FACTOR;
    private final float DECELERATION = (128f / 256f) * SCALE_FACTOR;
    private final float FRICTION = (12f / 256f) * SCALE_FACTOR;
    private final float ROLL_FRICTION = (6f / 256f) * SCALE_FACTOR;
    private final float ROLL_BRAKE = (32f / 256f) * SCALE_FACTOR;
    private final float AIR_ACCELERATION = (24f / 256f) * SCALE_FACTOR;

    private final float SLOPE_GRAVITY = (32f / 256f) * SCALE_FACTOR;
    private final float SLOPE_ROLL_UP = (20f / 256f) * SCALE_FACTOR;
    private final float SLOPE_ROLL_DOWN = (80f / 256f) * SCALE_FACTOR;

    private final float FALL_THRESHOLD = 2.5f * SCALE_FACTOR;

    // hit reaction (SPG:Damage) - reuses the airborne state machine instead of a dedicated state
    private final float HURT_KNOCKBACK_X = 2.0f * SCALE_FACTOR;
    private final float HURT_KNOCKBACK_Y = -4.0f * SCALE_FACTOR;
    private static final int INVULNERABILITY_FRAMES = 120; // 2s @ 60fps

    // death (SPG:Getting_Hit#Dying): fixed launch velocity, then gravity pulls the body through the level
    private final float DEATH_LAUNCH_VELOCITY = -7.0f * SCALE_FACTOR;

    // bounceOffEnemy(): fraction of the real jump impulse for the involuntary hop after stomping
    // a badnik - the SPG has no fixed value for this (its own "Rebounding" section covers a
    // different case, reversing existing velocity when hitting an object already in motion), so
    // this is a feel-tuned constant, not a value taken from the guide.
    private static final float ENEMY_BOUNCE_FACTOR = 0.6f;

    // unscaled logic thresholds
    private static final float MOVING_THRESHOLD = 0.1f;
    private static final float ROLL_SPEED_THRESHOLD = 1.5f; // start/exit roll (SPG: 128spx)
    private static final float CROUCH_FRICTION = 0.8f;
    private static final float ANGLE_SNAP_EPSILON = 0.5f;
    // above this a sensor hit ahead counts as a real wall, not the current surface curving onward
    private static final float OBSTACLE_ANGLE_THRESHOLD = 60f;

    // landing angle ranges (SPG:Slope_Physics#Landing)
    private static final float LANDING_FLAT_LO = 339f;
    private static final float LANDING_FLAT_HI = 23f;
    private static final float LANDING_SLOPE_LO = 316f;
    private static final float LANDING_SLOPE_HI = 45f;

    private float velX = 0f;
    private float velY = 0f;
    private float gSpeed = 0f; // ground speed, scalar along the surface
    private float groundAngle = 0f; // 0-360, counter-clockwise

    public enum PlayerState {
        IDLE, RUNNING, JUMPING, ROLLING, CROUCHING, LOOKING_UP, BALANCE
    }
    private PlayerState currentState = PlayerState.IDLE;

    public enum PhysicsMode { FLOOR, RIGHT_WALL, CEILING, LEFT_WALL }
    private PhysicsMode currentGravityMode = PhysicsMode.FLOOR;

    private int downX, downY;
    private int perpX, perpY;
    private int downSign;
    private boolean axisVertical;
    private SensorDirection downDir;

    private boolean onGround = false;
    private boolean canJump = true;
    private boolean facingRight = true;

    // renderer-facing flags: don't affect physics, just pick a more specific sprite than RUNNING
    private boolean skidding = false;
    private boolean pushing = false;
    private boolean wasSkidding = false; // edge-detects skidding's rising edge so the sfx plays once, not every held frame

    // true from a hit (takeDamage()) until landing - shorter than the invulnerability window itself,
    // needed because isInvulnerable() alone can't tell "still flying from the hit" from "landed and
    // jumping normally, still blinking"
    private boolean hitStun = false;

    // frames left showing the spring pose (launch()) - fixed window, unlike hitStun above
    private int bounceTimer = 0;
    private static final int BOUNCE_POSE_FRAMES = 20;

    // true from a spring launch until landing - exempts velY from the variable-jump-height cut
    // below, which is meant to shorten a player-released jump, not a spring's fixed boost
    private boolean springLaunched = false;

    // true from death (Level.killPlayer()) until respawn/game-over - update() just free-falls the
    // body while this holds, ignoring input/collision entirely
    private boolean dying = false;

    private int idleTimer = 0;
    private int invulnerableTimer = 0;

    private int debugSensorLeftY = TileID.NO_SURFACE;
    private int debugSensorRightY = TileID.NO_SURFACE;

    public Player(IPhysicsWorld world, ISoundEmitter soundEmitter, int spawnCol, int spawnRow) {
        this.world = world;
        this.soundEmitter = soundEmitter;
        this.spawnCol = spawnCol;
        this.spawnRow = spawnRow;
        setSpawnPosition(spawnCol, spawnRow);
        computeModeVectors();
    }

    private void setSpawnPosition(int col, int row) {
        x = col * GameConstants.TILE_SIZE + BASE_RADIUS_X;
        y = row * GameConstants.TILE_SIZE + BASE_RADIUS_Y;
    }

    public void reset() {
        setSpawnPosition(spawnCol, spawnRow);
        velX = 0f;
        velY = 0f;
        gSpeed = 0f;
        groundAngle = 0f;

        onGround = false;
        canJump = true;
        facingRight = true;
        idleTimer = 0;
        invulnerableTimer = 0;
        skidding = false;
        pushing = false;
        dying = false;
        hitStun = false;
        bounceTimer = 0;
        springLaunched = false;

        currentState = PlayerState.IDLE;
        currentGravityMode = PhysicsMode.FLOOR;
        computeModeVectors();
    }

    // update cycle

    public void update(InputSnapshot input) {
        if (dying) {
            velY += GRAVITY;
            y += velY;
            return;
        }

        updatePhysicsGeometry();

        if (invulnerableTimer > 0) invulnerableTimer--;
        if (bounceTimer > 0) bounceTimer--;

        if (!input.spacePressed) canJump = true;

        boolean isMoving = input.leftPressed || input.rightPressed
                        || input.upPressed || input.downPressed;
        if (onGround && Math.abs(gSpeed) < MOVING_THRESHOLD && !isMoving) idleTimer++;
        else idleTimer = 0;

        // jump: impulse along the surface normal (SPG:Forces#Jumping)
        if (input.spacePressed && onGround && canJump) {
            float rad = (float) Math.toRadians(groundAngle);
            velX = gSpeed * (float) Math.cos(rad) + JUMP_STRENGTH * (float) Math.sin(rad);
            velY = -gSpeed * (float) Math.sin(rad) + JUMP_STRENGTH * (float) Math.cos(rad);
            onGround = false;
            canJump = false;
            currentState = PlayerState.JUMPING;
            groundAngle = 0f;
            currentGravityMode = PhysicsMode.FLOOR;
            soundEmitter.playSound("jump");
        }

        // variable jump height (early release) - not for a spring's fixed boost
        if (!input.spacePressed && !onGround && !springLaunched && velY < SHORT_JUMP_CUT) {
            velY = SHORT_JUMP_CUT;
        }

        updateActionState(input);

        if (onGround) groundUpdate(input);
        else airUpdate(input);

        if (x <= 0) {
            x = 0;
            if (velX < 0) velX = 0;
            if (gSpeed < 0) gSpeed = 0;
        }
        float rightEdge = world.getWorldWidth() - currentRadiusX;
        if (x >= rightEdge) {
            x = rightEdge;
            if (velX > 0) velX = 0;
            if (gSpeed > 0) gSpeed = 0;
        }
    }

    // ground physics

    private void groundUpdate(InputSnapshot input) {
        computeModeVectors();
        float rad = (float) Math.toRadians(groundAngle);

        if (currentState == PlayerState.BALANCE) {
            velX = 0; velY = 0; gSpeed = 0;
            if (input.leftPressed || input.rightPressed || input.spacePressed) {
                currentState = PlayerState.IDLE;
            } else {
                updateGroundSensors(input);
                return;
            }
        }

        // slope factor: not applied in Ceiling mode (SPG:Slope_Physics)
        if (currentGravityMode != PhysicsMode.CEILING) {
            float slope;
            if (currentState == PlayerState.ROLLING) {
                boolean downhill = gSpeed * Math.sin(rad) < 0;
                slope = downhill ? SLOPE_ROLL_DOWN : SLOPE_ROLL_UP;
            } else {
                slope = SLOPE_GRAVITY;
            }
            gSpeed -= (float) Math.sin(rad) * slope;
        }

        applyGroundInput(input);

        velX = gSpeed * (float) Math.cos(rad);
        velY = -gSpeed * (float) Math.sin(rad);

        x += velX;
        y += velY;

        checkGroundObstacle();
        updatePushingState(input);

        updateGroundSensors(input);

        if (isTooSteepToHold(currentGravityMode) && Math.abs(gSpeed) < FALL_THRESHOLD) {
            detachToAir();
        }
    }

    // true only for CEILING - slope gravity alone can't dislodge a slow player there like it does
    // on a wall (RIGHT_WALL/LEFT_WALL already decelerate and reverse gSpeed on their own)
    private static boolean isTooSteepToHold(PhysicsMode mode) {
        return mode == PhysicsMode.CEILING;
    }

    private void applyGroundInput(InputSnapshot input) {
        skidding = false;

        if (currentState == PlayerState.ROLLING) {
            if (gSpeed > 0) {
                gSpeed -= ROLL_FRICTION;
                if (input.leftPressed) gSpeed -= ROLL_BRAKE;
            } else if (gSpeed < 0) {
                gSpeed += ROLL_FRICTION;
                if (input.rightPressed) gSpeed += ROLL_BRAKE;
            }
        } else if (currentState != PlayerState.CROUCHING && currentState != PlayerState.LOOKING_UP) {
            if (input.leftPressed) {
                facingRight = false;
                if (gSpeed > 0) {
                    gSpeed -= DECELERATION;
                    skidding = gSpeed > FAST_RUN_THRESHOLD;
                } else {
                    gSpeed -= ACCELERATION;
                    if (gSpeed < -MAX_SPEED) gSpeed = -MAX_SPEED;
                }
            } else if (input.rightPressed) {
                facingRight = true;
                if (gSpeed < 0) {
                    gSpeed += DECELERATION;
                    skidding = -gSpeed > FAST_RUN_THRESHOLD;
                } else {
                    gSpeed += ACCELERATION;
                    if (gSpeed > MAX_SPEED) gSpeed = MAX_SPEED;
                }
            } else {
                if (gSpeed > 0) gSpeed = Math.max(0f, gSpeed - FRICTION);
                else gSpeed = Math.min(0f, gSpeed + FRICTION);
            }
        } else {
            gSpeed *= CROUCH_FRICTION;
        }

        gSpeed = Math.max(-ABS_MAX_SPEED, Math.min(ABS_MAX_SPEED, gSpeed));

        if (skidding && !wasSkidding) soundEmitter.playSound("skid");
        wasSkidding = skidding;
    }

    // ground sensors (a/b)

    private void computeModeVectors() {
        switch (currentGravityMode) {
            case FLOOR: downX = 0; downY = 1; perpX = 1; perpY = 0; downDir = SensorDirection.DOWN; break;
            case RIGHT_WALL: downX = 1; downY = 0; perpX = 0; perpY = 1; downDir = SensorDirection.RIGHT; break;
            case CEILING: downX = 0; downY = -1; perpX = -1; perpY = 0; downDir = SensorDirection.UP; break;
            case LEFT_WALL: downX = -1; downY = 0; perpX = 0; perpY = -1; downDir = SensorDirection.LEFT; break;
        }
        axisVertical = (downDir == SensorDirection.DOWN || downDir == SensorDirection.UP);
        downSign = (downDir == SensorDirection.DOWN || downDir == SensorDirection.RIGHT) ? +1 : -1;
    }

    private SensorResult footSensor(int perpOffset) {
        int tipX = Math.round(x) + downX * currentRadiusY + perpX * perpOffset;
        int tipY = Math.round(y) + downY * currentRadiusY + perpY * perpOffset;
        // same tunneling fix as checkAirToGroundCollisions/checkCeilingBonk: a fast run into a
        // surface that curves back toward the player (e.g. entering a loop) can outrun the fixed
        // window in a single frame and pass through it, so grow the reach by this frame's travel
        int lookUp = SENSOR_LOOK_UP + Math.round(Math.abs(gSpeed));
        return world.castSensor(tipX, tipY, downDir, lookUp, SENSOR_LOOK_DOWN);
    }

    private void updateGroundSensors(InputSnapshot input) {
        updateGroundSensors(input, true);
    }

    // allowResnap: one retry right after a quadrant change (e.g. FLOOR -> RIGHT_WALL), since the
    // foot sensors were placed with the old mode's axes and can both miss the new surface on a
    // curve. If even the retry finds nothing, treat it as a real launch, not a ledge.
    private void updateGroundSensors(InputSnapshot input, boolean allowResnap) {
        computeModeVectors();
        PhysicsMode modeBefore = currentGravityMode;

        // sensor offset follows the current width radius (matches SPG: standing 9, rolling 7)
        SensorResult a = footSensor(-currentRadiusX);
        SensorResult b = footSensor(+currentRadiusX);

        if (!a.found && !b.found) {
            // a failed resnap means the new axes just don't apply here (e.g. launched off a steep
            // ramp) - keep the first pass's result instead of detaching over it
            if (!allowResnap) return;
            detachToAir();
            return;
        }

        debugSensorLeftY = a.found ? a.surface : TileID.NO_SURFACE;
        debugSensorRightY = b.found ? b.surface : TileID.NO_SURFACE;

        boolean isMoving = Math.abs(gSpeed) > MOVING_THRESHOLD || input.leftPressed || input.rightPressed;
        if (currentGravityMode == PhysicsMode.FLOOR && !isMoving) {
            if (!a.found && !facingRight) { currentState = PlayerState.BALANCE; groundAngle = 0f; return; }
            if (!b.found && facingRight) { currentState = PlayerState.BALANCE; groundAngle = 0f; return; }
        }

        SurfacePick pick = pickSurface(a, b, downSign);

        if (axisVertical) y = pick.surface - downSign * currentRadiusY;
        else x = pick.surface - downSign * currentRadiusY;

        groundAngle = pick.angle;
        if (Math.abs(groundAngle) < ANGLE_SNAP_EPSILON || Math.abs(groundAngle - 360f) < ANGLE_SNAP_EPSILON) groundAngle = 0f;

        updateMode();

        if (allowResnap && currentGravityMode != modeBefore) {
            updateGroundSensors(input, false);
        }
    }

    // Widens the current quadrant's range by this many degrees before giving up on it, so a
    // stall-and-reverse right at a 45°/135°/225°/315° boundary doesn't flip PhysicsMode back and
    // forth every frame from a couple degrees of angle wobble. Landing still snaps to the plain,
    // unwidened quadrant - this only matters for the continuous per-frame case below.
    private static final float QUADRANT_HYSTERESIS_DEG = 6f;

    private void updateMode() {
        if (stillInQuadrant(groundAngle, currentGravityMode)) return;
        currentGravityMode = modeForAngle(groundAngle);
    }

    private static boolean stillInQuadrant(float angle, PhysicsMode mode) {
        float a = ((angle % 360f) + 360f) % 360f;
        float h = QUADRANT_HYSTERESIS_DEG;
        switch (mode) {
            case FLOOR:       return a <= 45f + h || a >= 315f - h;
            case RIGHT_WALL:  return a >= 45f - h && a <= 135f + h;
            case CEILING:     return a >= 135f - h && a <= 225f + h;
            default:          return a >= 225f - h && a <= 315f + h; // LEFT_WALL
        }
    }

    private static PhysicsMode modeForAngle(float angle) {
        float a = ((angle % 360f) + 360f) % 360f;
        if (a <= 45f || a >= 315f) return PhysicsMode.FLOOR;
        else if (a < 135f) return PhysicsMode.RIGHT_WALL;
        else if (a <= 225f) return PhysicsMode.CEILING;
        else return PhysicsMode.LEFT_WALL;
    }

    private void detachToAir() {
        onGround = false;
        currentState = PlayerState.JUMPING;
    }

    // air physics

    private void airUpdate(InputSnapshot input) {
        // both are ground-only poses; stop showing a stale value from the last grounded frame
        skidding = false;
        pushing = false;

        velY += hitStun ? HURT_GRAVITY : GRAVITY;

        // air acceleration only pushes velX toward MAX_SPEED; momentum beyond it is preserved (SPG:Forces#Midair)
        if (input.leftPressed) {
            facingRight = false;
            if (velX > -MAX_SPEED) {
                velX -= AIR_ACCELERATION;
                if (velX < -MAX_SPEED) velX = -MAX_SPEED;
            }
        } else if (input.rightPressed) {
            facingRight = true;
            if (velX < MAX_SPEED) {
                velX += AIR_ACCELERATION;
                if (velX > MAX_SPEED) velX = MAX_SPEED;
            }
        }

        x += velX;
        y += velY;

        checkWalls();
        checkCeilingBonk();
        checkAirToGroundCollisions();
    }

    private void checkAirToGroundCollisions() {
        if (velY < 0) return;

        int cx = Math.round(x), cy = Math.round(y);
        int tipY = cy + currentRadiusY;
        // grow the look-back by this frame's fall distance, or a fast enough fall tunnels through
        // a surface that ended up further back than the fixed SENSOR_LOOK_UP window reaches
        int lookBack = SENSOR_LOOK_UP + Math.round(Math.abs(velY));
        SensorResult a = world.castSensor(cx - currentRadiusX, tipY, SensorDirection.DOWN, lookBack, SENSOR_LOOK_DOWN);
        SensorResult b = world.castSensor(cx + currentRadiusX, tipY, SensorDirection.DOWN, lookBack, SENSOR_LOOK_DOWN);

        if (!a.found && !b.found) return;

        SurfacePick pick = pickSurface(a, b, +1); // cast is always DOWN here

        float feetY = y + currentRadiusY;
        if (feetY < pick.surface) return; // not landed yet

        // air-to-ground speed conversion (SPG:Slope_Physics#Landing), computed before committing
        // so a too-slow graze can be rejected below instead of re-detaching next frame
        float rad = (float) Math.toRadians(pick.angle);
        float landingGSpeed;
        if (inAngleRange(pick.angle, LANDING_FLAT_LO, LANDING_FLAT_HI)) {
            landingGSpeed = velX;
        } else {
            float sinSign = Math.signum((float) Math.sin(rad));
            if (inAngleRange(pick.angle, LANDING_SLOPE_LO, LANDING_SLOPE_HI)) {
                landingGSpeed = velY * 0.5f * -sinSign;
            } else {
                landingGSpeed = velY * -sinSign;
            }
        }
        if (isTooSteepToHold(modeForAngle(pick.angle)) && Math.abs(landingGSpeed) < FALL_THRESHOLD) return;

        y = pick.surface - currentRadiusY;
        onGround = true;
        hitStun = false; // knockback arc (see takeDamage()) ends the instant it lands
        springLaunched = false;

        debugSensorLeftY = a.found ? a.surface : TileID.NO_SURFACE;
        debugSensorRightY = b.found ? b.surface : TileID.NO_SURFACE;

        groundAngle = pick.angle;
        currentGravityMode = modeForAngle(pick.angle);
        gSpeed = landingGSpeed;

        currentState = (Math.abs(gSpeed) > MOVING_THRESHOLD) ? PlayerState.RUNNING : PlayerState.IDLE;
    }

    private void checkCeilingBonk() {
        if (velY >= 0) return;
        int cx = Math.round(x);
        int headY = Math.round(y) - currentRadiusY;
        // same tunneling fix as checkAirToGroundCollisions, for a fast jump into a thin ceiling
        int lookBack = SENSOR_LOOK_UP + Math.round(Math.abs(velY));
        SensorResult c = world.castSensor(cx, headY, SensorDirection.UP, lookBack, SENSOR_LOOK_DOWN);
        if (c.found && headY <= c.surface) {
            y = c.surface + currentRadiusY;
            velY = 0f;
        }
    }

    private void checkWalls() {
        int cx = Math.round(x), cy = Math.round(y);
        // same tunneling fix as checkAirToGroundCollisions/checkCeilingBonk, for a fast midair dash
        int lookBack = Math.round(Math.abs(velX));
        if (velX > 0) {
            SensorResult r = world.castSensor(cx, cy, SensorDirection.RIGHT, lookBack, PUSH_RADIUS + 2);
            if (r.found && r.surface <= cx + PUSH_RADIUS) {
                x = r.surface - PUSH_RADIUS;
                velX = 0; gSpeed = 0;
            }
        } else if (velX < 0) {
            SensorResult l = world.castSensor(cx, cy, SensorDirection.LEFT, lookBack, PUSH_RADIUS + 2);
            if (l.found && l.surface >= cx - PUSH_RADIUS) {
                x = l.surface + PUSH_RADIUS;
                velX = 0; gSpeed = 0;
            }
        }
    }

    // stops the player against a solid obstacle ahead along the current surface - generalizes
    // checkWalls() (horizontal-only, airborne) to all 4 quadrants
    private void checkGroundObstacle() {
        if (gSpeed == 0f) return;

        int sign = (int) Math.signum(gSpeed);
        int travelPerpX = perpX * sign;
        int travelPerpY = perpY * sign;
        SensorDirection dir = (travelPerpX > 0) ? SensorDirection.RIGHT
                            : (travelPerpX < 0) ? SensorDirection.LEFT
                            : (travelPerpY > 0) ? SensorDirection.DOWN
                            : SensorDirection.UP;
        boolean horizontal = (dir == SensorDirection.LEFT || dir == SensorDirection.RIGHT);
        int travelSign = (dir == SensorDirection.DOWN || dir == SensorDirection.RIGHT) ? +1 : -1;

        int cx = Math.round(x), cy = Math.round(y);

        // two probes (feet, head), inset from the true edges so neither lands on the player's own
        // ground tile and misreads it as a wall
        int reach = currentRadiusY - WALL_PROBE_INSET;
        int feetX = cx + downX * reach, feetY = cy + downY * reach;
        int headX = cx - downX * reach, headY = cy - downY * reach;

        if (checkObstacleAt(feetX, feetY, dir, horizontal, travelSign)) return;
        checkObstacleAt(headX, headY, dir, horizontal, travelSign);
    }

    /** One probe of checkGroundObstacle(). Returns true (and stops the player) if it hit a wall. */
    private boolean checkObstacleAt(int tipX, int tipY, SensorDirection dir,
                                     boolean horizontal, int travelSign) {
        int axisPos = horizontal ? tipX : tipY;

        SensorResult r = world.castSensor(tipX, tipY, dir, 0, PUSH_RADIUS + 2);
        if (!r.found || travelSign * r.surface > travelSign * axisPos + PUSH_RADIUS) return false;

        // a block tile (FULL, THREE_FOUR, ...) reports a fixed floor-relative angle no matter which
        // face was hit, so angle alone can't tell "wall" from "surface continuing". A curve's own
        // tiles read back as AIR here (Level.loadLoopsFromMap() consumes them into a LoopRegion),
        // so AIR at the hit point means this came from a loop/arc, not empty space.
        int probeX = horizontal ? r.surface + travelSign : tipX;
        int probeY = horizontal ? tipY : r.surface + travelSign;
        int hitTileID = world.getTileIdAt(probeX, probeY);
        boolean isCurve = world.isSlope(hitTileID) || hitTileID == TileID.AIR;

        if (isCurve && angleDiff(r.angle, groundAngle) <= OBSTACLE_ANGLE_THRESHOLD) return false;

        // still not necessarily a wall: find the column's true topmost surface. Within a normal
        // step's reach of the current foot line it's just more ground (handled next frame by
        // updateGroundSensors()); only a surface far above that is a genuine wall.
        int currentFootAxis = horizontal ? Math.round(y) + downY * currentRadiusY
                                          : Math.round(x) + downX * currentRadiusY;
        int aheadX = horizontal ? r.surface + travelSign * 2 : currentFootAxis;
        int aheadY = horizontal ? currentFootAxis : r.surface + travelSign * 2;
        SensorResult ahead = world.castSensor(aheadX, aheadY, downDir, WALL_TOP_SEARCH, SENSOR_LOOK_DOWN);
        if (ahead.found && Math.abs(ahead.surface - currentFootAxis) <= SENSOR_LOOK_UP) return false;

        int newAxisPos = r.surface - travelSign * PUSH_RADIUS;
        if (horizontal) x = newAxisPos; else y = newAxisPos;
        velX = 0; velY = 0; gSpeed = 0;
        return true;
    }

    // "pushing" pose: grounded, essentially stopped, holding the key into a wall ahead - a separate
    // check from checkGroundObstacle() since that one only fires the frame gSpeed reaches zero
    private void updatePushingState(InputSnapshot input) {
        pushing = false;
        if (currentGravityMode != PhysicsMode.FLOOR || Math.abs(gSpeed) > MOVING_THRESHOLD) return;

        boolean pressingForward = facingRight ? input.rightPressed : input.leftPressed;
        if (!pressingForward) return;

        SensorDirection dir = facingRight ? SensorDirection.RIGHT : SensorDirection.LEFT;
        int sign = facingRight ? 1 : -1;
        int cx = Math.round(x), cy = Math.round(y);

        SensorResult r = world.castSensor(cx, cy, dir, 0, PUSH_RADIUS + 2);
        pushing = r.found && sign * r.surface <= sign * cx + PUSH_RADIUS + 2;
    }

    // state machine & geometry

    private void updatePhysicsGeometry() {
        boolean isCompact = (currentState == PlayerState.ROLLING
                          || currentState == PlayerState.JUMPING);
        currentRadiusX = isCompact ? ROLL_RADIUS_X : BASE_RADIUS_X;
        currentRadiusY = isCompact ? ROLL_RADIUS_Y : BASE_RADIUS_Y;
    }

    private void updateActionState(InputSnapshot input) {
        if (onGround) {
            if (currentState != PlayerState.ROLLING && currentState != PlayerState.BALANCE) {
                currentState = (Math.abs(gSpeed) > MOVING_THRESHOLD) ? PlayerState.RUNNING : PlayerState.IDLE;
            }

            if (input.downPressed) {
                if (Math.abs(gSpeed) > ROLL_SPEED_THRESHOLD && currentState != PlayerState.ROLLING)
                    currentState = PlayerState.ROLLING;
                else if (Math.abs(gSpeed) <= ROLL_SPEED_THRESHOLD && currentState != PlayerState.ROLLING)
                    currentState = PlayerState.CROUCHING;
            } else if (input.upPressed && Math.abs(gSpeed) < MOVING_THRESHOLD
                    && currentState != PlayerState.ROLLING) {
                currentState = PlayerState.LOOKING_UP;
            }

            if (currentState == PlayerState.ROLLING && Math.abs(gSpeed) < ROLL_SPEED_THRESHOLD) {
                currentState = PlayerState.IDLE;
            }
        } else {
            if (currentState == PlayerState.CROUCHING || currentState == PlayerState.LOOKING_UP)
                currentState = PlayerState.JUMPING;
            if (velY > 0 && currentState != PlayerState.ROLLING && currentState != PlayerState.JUMPING)
                currentState = PlayerState.JUMPING;
        }
    }

    /** Result of choosing between two ground sensors: winning surface + resolved angle. */
    private static final class SurfacePick {
        final int surface;
        final float angle;
        SurfacePick(int surface, float angle) { this.surface = surface; this.angle = angle; }
    }

    // picks the sensor hit first along downSign and uses only its angle - averaging both (as SPG
    // describes) would blend a wrong angle across the player's whole straddle over a tile boundary
    private SurfacePick pickSurface(SensorResult a, SensorResult b, int downSign) {
        SensorResult winner = (a.found && b.found)
            ? (downSign * a.surface <= downSign * b.surface ? a : b)
            : (a.found ? a : b);
        return new SurfacePick(winner.surface, winner.angle);
    }

    /** True if angle is within [lo, hi], wrapping through 0/360 when lo > hi. */
    private static boolean inAngleRange(float angle, float lo, float hi) {
        return (lo <= hi) ? (angle >= lo && angle <= hi) : (angle >= lo || angle <= hi);
    }

    /** Smallest angular distance between two 0-360 angles (0-180). */
    private static float angleDiff(float a, float b) {
        float d = Math.abs(a - b) % 360f;
        return d > 180f ? 360f - d : d;
    }

    // damage (SPG:Damage)

    // reaction to a hazard/enemy hit, ignored while invulnerable. rings > 0: lose them + brief
    // invulnerability + knockback (an involuntary jump). rings == 0: die.
    @Override
    public void takeDamage(int amount) {
        if (invulnerableTimer > 0) return;

        if (world.getRingCount() > 0) {
            world.loseRings();
            invulnerableTimer = INVULNERABILITY_FRAMES;
            hitStun = true;

            onGround = false;
            currentState = PlayerState.JUMPING;
            velY = HURT_KNOCKBACK_Y;
            velX = facingRight ? -HURT_KNOCKBACK_X : HURT_KNOCKBACK_X;
        } else {
            world.killPlayer();
        }
    }

    public boolean isInvulnerable() { return invulnerableTimer > 0; }
    public boolean isHitStun() { return hitStun; }

    @Override public void reachGoal() { world.reachGoal(); }

    // spring launch: overrides velY outright and forces the player airborne, whatever it was
    // doing. Also zeroes velX - a vertical spring launches straight up, not into a sideways arc.
    @Override
    public void launch(float velocityY) {
        velY = velocityY;
        velX = 0f;
        onGround = false;
        canJump = false;
        currentState = PlayerState.JUMPING;
        currentGravityMode = PhysicsMode.FLOOR;
        bounceTimer = BOUNCE_POSE_FRAMES;
        springLaunched = true;
    }

    public boolean isBounced() { return bounceTimer > 0; }

    public float getVelocityY() { return velY; }

    /** Reaction to stomping an enemy from above: a smaller involuntary jump than the real jump impulse. */
    public void bounceOffEnemy() {
        velY = JUMP_STRENGTH * ENEMY_BOUNCE_FACTOR;
        onGround = false;
        currentState = PlayerState.JUMPING;
    }

    /** ICollector's unconditional bump - same small involuntary jump as bounceOffEnemy(). */
    @Override
    public void bounce() {
        bounceOffEnemy();
    }

    // switches the player into its death fall, called once by Level.killPlayer() - launches
    // upward like a jump, then leaves gravity to bring it back down (see the `dying` field)
    public void startDeathPose() {
        dying = true;
        onGround = false; // PlayerRenderer's landing-smoother only eases while grounded - leaving
                           // this true would fight the death fall and pin the sprite near deathStartY
        velX = 0f;
        velY = DEATH_LAUNCH_VELOCITY;
    }
    public boolean isDying() { return dying; }

    // getters

    @Override public float getX() { return x; }
    @Override public float getY() { return y; }
    @Override public float getVelocityX() { return velX; }
    @Override public int getCurrentRadiusY() { return currentRadiusY; }
    @Override public boolean isLookingUp() { return currentState == PlayerState.LOOKING_UP; }
    @Override public boolean isCurlingUp() { return currentState == PlayerState.CROUCHING; }

    @Override public void addRing() { world.addRing(); }

    public float getGroundAngle() { return groundAngle; }
    public PlayerState getCurrentState() { return currentState; }
    public PhysicsMode getGravityMode() { return currentGravityMode; }
    public int getHitboxRadiusX() { return HITBOX_RADIUS_X; }
    public int getHitboxRadiusY() { return HITBOX_RADIUS_Y; }
    public boolean isFacingRight() { return facingRight; }
    public int getCurrentRadiusX() { return currentRadiusX; }
    public float getGSpeed() { return gSpeed; }
    public float getFastRunThreshold() { return FAST_RUN_THRESHOLD; }
    public int getIdleTimer() { return idleTimer; }
    public boolean isOnGround() { return onGround; }
    public boolean isSkidding() { return skidding; }
    public boolean isPushing() { return pushing; }

    public int getDebugLeftSensorY() { return debugSensorLeftY; }
    public int getDebugRightSensorY() { return debugSensorRightY; }
}
