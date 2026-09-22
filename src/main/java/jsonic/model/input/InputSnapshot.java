package jsonic.model.input;

/**
 * Immutable snapshot of all key states for a single frame. Movement keys
 * stay continuous state (physics needs to know if they're held); keys that
 * trigger a state transition (ESC, ENTER, T, M) are exposed as "just
 * pressed" edges, true for one frame only, so a single press can't cross
 * more than one state change. upJustPressed/downJustPressed are the same
 * edge for Up/Down, used by menu navigation instead of physics.
 */
public final class InputSnapshot {

    // movement (continuous state)
    public final boolean spacePressed;
    public final boolean leftPressed;
    public final boolean rightPressed;
    public final boolean upPressed;
    public final boolean downPressed;

    // ui transitions (edge — true only the frame of the press)
    public final boolean escapeJustPressed;
    public final boolean enterJustPressed;
    public final boolean debugJustPressed;
    public final boolean muteJustPressed;
    public final boolean upJustPressed;
    public final boolean downJustPressed;

    // constructor

    public InputSnapshot(
            boolean spacePressed,
            boolean leftPressed,
            boolean rightPressed,
            boolean upPressed,
            boolean downPressed,
            boolean escapeJustPressed,
            boolean enterJustPressed,
            boolean debugJustPressed,
            boolean muteJustPressed,
            boolean upJustPressed,
            boolean downJustPressed) {

        this.spacePressed = spacePressed;
        this.leftPressed = leftPressed;
        this.rightPressed = rightPressed;
        this.upPressed = upPressed;
        this.downPressed = downPressed;
        this.escapeJustPressed = escapeJustPressed;
        this.enterJustPressed = enterJustPressed;
        this.debugJustPressed = debugJustPressed;
        this.muteJustPressed = muteJustPressed;
        this.upJustPressed = upJustPressed;
        this.downJustPressed = downJustPressed;
    }
}
