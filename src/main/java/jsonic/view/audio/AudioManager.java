package jsonic.view.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Loads and plays every sound in the game: short one-shot sfx and the looping level music. Each
 * sfx's raw bytes are decoded once and cached, but played through a fresh Clip every time, so
 * overlapping instances of the same effect don't cut each other off.
 */
public final class AudioManager {

    private static final String SFX_PATH = "/res/audio/sfx/";
    private static final String JINGLE_PATH = "/res/audio/jingles/";

    private static final java.util.Map<String, byte[]> sfxCache = new java.util.HashMap<>();
    private static Clip musicClip;

    // opens/plays sfx and jingle Clips off the game loop thread - see openAndPlay()
    private static final java.util.concurrent.ExecutorService sfxExecutor =
        java.util.concurrent.Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "AudioManager-sfx");
            t.setDaemon(true);
            return t;
        });

    // mute (pause menu option)
    private static boolean muted = false;
    private static String pendingMusic = null; // last playMusic() request, resumed on unmute

    public static boolean isMuted() {
        return muted;
    }

    public static void toggleMuted() {
        muted = !muted;
        if (muted) stopMusic();
        else if (pendingMusic != null) playMusic(pendingMusic);
    }

    private AudioManager() {}

    public static void playSfx(String name) {
        if (muted) return;

        // computeIfAbsent is cheap after the first call; only opening the line itself (below) moves off-thread.
        byte[] data = sfxCache.computeIfAbsent(name, n -> loadBytes(SFX_PATH + n + ".wav"));
        if (data == null) return;

        sfxExecutor.execute(() -> openAndPlay(name, data));
    }

    // AudioSystem.getClip()/clip.open() ask the OS for an audio line - real latency, enough to
    // visibly stutter the game loop if called synchronously there instead of off-thread here.
    private static void openAndPlay(String name, byte[] data) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(
                new BufferedInputStream(new java.io.ByteArrayInputStream(data)));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            System.err.println("AudioManager: failed to play sfx '" + name + "': " + e);
        }
    }

    /**
     * Starts looping music from a full classpath resource path (each level owns its own
     * track, see LevelConfig.musicPath), replacing whatever was already playing.
     */
    public static void playMusic(String path) {
        pendingMusic = path; // remembered even while muted, so unmute resumes it
        stopMusic();
        if (muted || path == null) return;
        try (InputStream raw = AudioManager.class.getResourceAsStream(path)) {
            if (raw == null) {
                System.err.println("AudioManager: music not found: " + path);
                return;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
            musicClip = AudioSystem.getClip();
            musicClip.open(ais);
            setVolume(musicClip, -8f); // background music sits under sfx, not over it
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            System.err.println("AudioManager: failed to play music '" + path + "': " + e);
        }
    }

    /** Stops whatever music is playing and plays a one-shot jingle from res/audio/jingles/ (e.g. the level-clear tally) — never loops. */
    public static void playJingle(String name) {
        stopMusic();
        if (muted) return;
        try (InputStream raw = AudioManager.class.getResourceAsStream(JINGLE_PATH + name + ".wav")) {
            if (raw == null) {
                System.err.println("AudioManager: jingle not found: " + name);
                return;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (LineUnavailableException | UnsupportedAudioFileException | IOException e) {
            System.err.println("AudioManager: failed to play jingle '" + name + "': " + e);
        }
    }

    public static void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
            musicClip.close();
            musicClip = null;
        }
    }

    private static void setVolume(Clip clip, float decibels) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }

    private static byte[] loadBytes(String path) {
        try (InputStream in = AudioManager.class.getResourceAsStream(path)) {
            if (in == null) {
                System.err.println("AudioManager: sfx not found: " + path);
                return null;
            }
            return in.readAllBytes();
        } catch (IOException e) {
            System.err.println("AudioManager: failed to read " + path + ": " + e);
            return null;
        }
    }
}
