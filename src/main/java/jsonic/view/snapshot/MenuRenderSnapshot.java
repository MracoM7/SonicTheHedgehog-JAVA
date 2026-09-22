package jsonic.view.snapshot;

import jsonic.view.IRenderSnapshot;

/**
 * Snapshot for the main menu (MenuState). Carries only what the title
 * screen itself needs (the selected level option, the blink timer) —
 * no game data, since nothing is playing yet.
 */
public final class MenuRenderSnapshot implements IRenderSnapshot {
    private final int selectedOption;
    private final int animTimer;

    public MenuRenderSnapshot(int selectedOption, int animTimer) {
        this.selectedOption = selectedOption;
        this.animTimer      = animTimer;
    }

    public int getSelectedOption() { return selectedOption; }

    /** Frame counter since the menu was entered — drives the "PRESS START" blink. */
    public int getAnimTimer() { return animTimer; }
}
