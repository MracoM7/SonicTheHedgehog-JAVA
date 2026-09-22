package jsonic.view.renderer;

import java.awt.Color;
import java.awt.Graphics2D;

import jsonic.utils.GameConstants;
import jsonic.view.Fonts;
import jsonic.view.audio.AudioManager;
import jsonic.view.snapshot.PlayRenderSnapshot;

/**
 * Draws the pause overlay: frozen gameplay visible underneath with a "PAUSA" title and a
 * 4-option menu (Riprendi/Riavvia/Audio/Esci) on top, via Fonts.drawOutlinedText() (no sprite
 * rip exists for this text).
 */
public class PauseRenderer {

    private static final String[] OPTION_LABELS = { "RIPRENDI", "RIAVVIA", "AUDIO", "ESCI" };
    private static final int AUDIO_LABEL_INDEX = 2; // must match PauseState's OPTION_AUDIO

    private static final int BLINK_INTERVAL_FRAMES = 20; // ~3 times/sec at 60fps
    private static final int OPTION_LINE_HEIGHT = 50;
    private static final int TITLE_Y_OFFSET = -110; // relative to screen centre
    private static final int FIRST_OPTION_Y_OFFSET = -20; // relative to screen centre

    private static final Color SELECTED_COLOR = new Color(255, 222, 0); // matches HUD warning yellow
    private static final Color UNSELECTED_COLOR = Color.WHITE;

    private final LevelRenderer levelRenderer;
    private final HUDRenderer hudRenderer;

    // Shares GameView's own renderer instances instead of reloading the same sprites again.
    public PauseRenderer(LevelRenderer levelRenderer, HUDRenderer hudRenderer) {
        this.levelRenderer = levelRenderer;
        this.hudRenderer = hudRenderer;
    }

    public void draw(Graphics2D g2, PlayRenderSnapshot gameSnapshot, int pauseFrames, int selectedOption) {
        levelRenderer.draw(g2, gameSnapshot, false); // frozen: don't let the player's sprite keep animating in place
        hudRenderer.draw(g2, gameSnapshot);

        int centerX = GameConstants.SCREEN_WIDTH / 2;
        int centerY = GameConstants.SCREEN_HEIGHT / 2;

        g2.setFont(Fonts.sized(56));
        Fonts.drawOutlinedText(g2, "PAUSA", centerX, centerY + TITLE_Y_OFFSET, SELECTED_COLOR);

        g2.setFont(Fonts.sized(34));
        boolean blinkOn = (pauseFrames / BLINK_INTERVAL_FRAMES) % 2 == 0;
        for (int i = 0; i < OPTION_LABELS.length; i++) {
            boolean selected = (i == selectedOption);
            if (selected && !blinkOn) continue; // only the selected option blinks, as a cursor

            String text = OPTION_LABELS[i];
            if (i == AUDIO_LABEL_INDEX) text += AudioManager.isMuted() ? ": OFF" : ": ON";
            String label = selected ? "> " + text : text;
            int y = centerY + FIRST_OPTION_Y_OFFSET + i * OPTION_LINE_HEIGHT;
            Fonts.drawOutlinedText(g2, label, centerX, y, selected ? SELECTED_COLOR : UNSELECTED_COLOR);
        }
    }
}
