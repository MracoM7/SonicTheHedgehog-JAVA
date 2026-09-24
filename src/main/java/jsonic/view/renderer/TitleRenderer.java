package jsonic.view.renderer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import jsonic.utils.GameConstants;
import jsonic.utils.LevelRegistry;
import jsonic.view.Fonts;
import jsonic.view.SpriteLoader;
import jsonic.view.audio.AudioManager;
import jsonic.view.snapshot.MenuRenderSnapshot;

/** Draws the title screen: background, the animated "SONIC THE HEDGEHOG" logo and the blinking "PRESS START" prompt. */
public class TitleRenderer {

    private static final int BLINK_ON_FRAMES = 40;
    private static final int BLINK_OFF_FRAMES = 20;

    private static final int LOGO_FRAME_COUNT = 18;
    private static final int LOGO_FRAME_W = 320; // full-screen-sized frames, not a tight logo crop
    private static final int LOGO_FRAME_H = 224;
    private static final int LOGO_TICKS_PER_FRAME = 8; // 8 frames of animation per logo frame, so 60fps / 8 = 7.5fps

    private static final int LOGO_LOOP_START_FRAME = 16; // frames 0-15 play once; 16-17 (finger-wag) is the loop

    // Where the sheet's own "PRESS START" placeholder bar sat within a logo frame (native, pre-scale).
    private static final int PRESS_START_LOCAL_X = 80;
    private static final int PRESS_START_LOCAL_Y = 180;

    // Measured wing-silhouette midpoint - the logo art isn't centred in its own 320px frame (true midpoint 160).
    private static final double LOGO_VISUAL_CENTER_X = 151.5;

    private BufferedImage background;
    private BufferedImage[] logoFrames;
    private BufferedImage pressStart;

    public TitleRenderer() {
        background = SpriteLoader.load(getClass(), "/res/sprites/menu/title_background.png");
        loadLogoFrames();
        pressStart = SpriteLoader.load(getClass(), "/res/sprites/menu/press_start.png");
    }

    private void loadLogoFrames() {
        BufferedImage sheet = SpriteLoader.load(getClass(), "/res/sprites/menu/title_logo.png");
        if (sheet == null) return;
        logoFrames = new BufferedImage[LOGO_FRAME_COUNT];
        for (int i = 0; i < LOGO_FRAME_COUNT; i++) {
            logoFrames[i] = sheet.getSubimage(i * LOGO_FRAME_W, 0, LOGO_FRAME_W, LOGO_FRAME_H);
        }
    }

    public void draw(Graphics2D g2, MenuRenderSnapshot snap) {
        int screenW = GameConstants.SCREEN_WIDTH;
        int screenH = GameConstants.SCREEN_HEIGHT;

        if (background != null) {
            g2.drawImage(background, 0, 0, screenW, screenH, null);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, screenW, screenH);
        }

        int scale = GameConstants.SCALE;
        int logoX = 0, logoY = 0;
        if (logoFrames != null) {
            BufferedImage frame = logoFrames[currentLogoFrame(snap.getAnimTimer())];
            int logoW = LOGO_FRAME_W * scale;
            int logoH = LOGO_FRAME_H * scale;
            logoX = (int) Math.round(screenW / 2.0 - LOGO_VISUAL_CENTER_X * scale);
            logoY = (screenH - logoH) / 2; // PRESS START sits at the logo frame's own bottom edge, so this centres both
            g2.drawImage(frame, logoX, logoY, logoW, logoH, null);
        }

        if (pressStart != null && isBlinkVisible(snap.getAnimTimer())) {
            int psW = pressStart.getWidth() * scale;
            int psH = pressStart.getHeight() * scale;
            int barW = 144 * scale; // sheet's own placeholder bar width, so a trimmed sprite still centres correctly
            int psX = logoX + PRESS_START_LOCAL_X * scale + (barW - psW) / 2;
            int psY = logoY + PRESS_START_LOCAL_Y * scale;
            g2.drawImage(pressStart, psX, psY, psW, psH, null);
        }

        drawLevelHint(g2, snap);
    }

    private int currentLogoFrame(int animTimer) {
        int introTicks = LOGO_LOOP_START_FRAME * LOGO_TICKS_PER_FRAME;
        if (animTimer < introTicks) {
            return animTimer / LOGO_TICKS_PER_FRAME;
        }
        int loopFrameCount = LOGO_FRAME_COUNT - LOGO_LOOP_START_FRAME;
        int loopTicks = loopFrameCount * LOGO_TICKS_PER_FRAME;
        int loopElapsedTicks = (animTimer - introTicks) % loopTicks;
        return LOGO_LOOP_START_FRAME + loopElapsedTicks / LOGO_TICKS_PER_FRAME;
    }

    private boolean isBlinkVisible(int animTimer) {
        int cycle = BLINK_ON_FRAMES + BLINK_OFF_FRAMES;
        return (animTimer % cycle) < BLINK_ON_FRAMES;
    }

    /** Dev-only level/audio readout, not part of the original game. */
    private void drawLevelHint(Graphics2D g2, MenuRenderSnapshot snap) {
        String levelText = "LEVEL (UP/DOWN): " + LevelRegistry.ALL[snap.getSelectedOption()].displayName;
        g2.setFont(Fonts.sized(32f));
        Fonts.drawOutlinedText(g2, levelText, 10, GameConstants.SCREEN_HEIGHT - 50, Color.WHITE, Fonts.Align.LEFT);

        String muteText = "AUDIO (M): " + (AudioManager.isMuted() ? "OFF" : "ON");
        Fonts.drawOutlinedText(g2, muteText, 10, GameConstants.SCREEN_HEIGHT - 10, Color.WHITE, Fonts.Align.LEFT);
    }
}
