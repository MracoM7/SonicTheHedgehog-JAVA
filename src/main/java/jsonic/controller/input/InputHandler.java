package jsonic.controller.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import jsonic.model.input.InputSnapshot;

/**
 * Receives raw keyboard events from AWT and turns them into boolean flags
 * that snapshot() reads once per frame. Movement keys stay continuous
 * (physics reads them every frame they're held); UI transition keys
 * (ESC, ENTER, T, M) are consumed right after being copied into the
 * snapshot, so one human press can't cross more than one state change.
 */
public class InputHandler implements KeyListener {

    // continuous-state flags (movement)
    private volatile boolean spacePressed;
    private volatile boolean leftPressed;
    private volatile boolean rightPressed;
    private volatile boolean upPressed;
    private volatile boolean downPressed;

    // event flags (ui transitions)
    // Reset by snapshot() immediately after copying, not by keyReleased.
    private volatile boolean escapePressed;
    private volatile boolean enterPressed;
    private volatile boolean debugPressed;
    private volatile boolean mutePressed;
    private volatile boolean upJustPressedFlag;
    private volatile boolean downJustPressedFlag;

    // keylistener — awt thread

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_SPACE) spacePressed = true;
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) leftPressed = true;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) rightPressed = true;
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
            // Guarded (unlike the other *JustPressed flags below): OS key-repeat
            // fires keyPressed repeatedly while held, and a menu cursor holding
            // Up/Down is common enough to need the "was it already down" check,
            // or the selection would skip options on repeat events.
            if (!upPressed) upJustPressedFlag = true;
            upPressed = true;
        }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
            if (!downPressed) downJustPressedFlag = true;
            downPressed = true;
        }
        if (code == KeyEvent.VK_ESCAPE) escapePressed = true;
        if (code == KeyEvent.VK_ENTER) enterPressed = true;
        if (code == KeyEvent.VK_T) debugPressed = true;
        if (code == KeyEvent.VK_M) mutePressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_SPACE) spacePressed = false;
        if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) leftPressed = false;
        if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) rightPressed = false;
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) upPressed = false;
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) downPressed = false;
        // ESC, ENTER, T and M are consumed by snapshot() instead, not here.
    }

    // snapshot — game loop thread

    /**
     * Builds an immutable snapshot and consumes the event flags right
     * after. Called exactly once per frame from the game loop thread, so
     * this copy-then-consume sequence needs no explicit synchronisation.
     */
    public InputSnapshot snapshot() {
        InputSnapshot snap = new InputSnapshot(
            spacePressed,
            leftPressed,
            rightPressed,
            upPressed,
            downPressed,
            escapePressed,
            enterPressed,
            debugPressed,
            mutePressed,
            upJustPressedFlag,
            downJustPressedFlag
        );
        // Next frame finds these false even if the key is still held down.
        escapePressed = false;
        enterPressed = false;
        debugPressed = false;
        mutePressed = false;
        upJustPressedFlag = false;
        downJustPressedFlag = false;
        return snap;
    }
}
