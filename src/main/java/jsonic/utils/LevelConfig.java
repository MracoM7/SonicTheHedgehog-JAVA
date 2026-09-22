package jsonic.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/** Everything one level needs: its map, art, music and spawn point. See LevelRegistry for the actual instances. */
public final class LevelConfig {

    // shown in the menu's level picker (TitleRenderer) - the only thing that identifies
    // a level to the player, so the menu never has to know level names itself
    public final String displayName;

    public final String mapCsvPath;
    public final BufferedImage backgroundImage;

    // classpath folder ParallaxRenderer loads sky/mountains/hills/water from for this
    // level, and the splash card StageIntroRenderer shows on entry - null means "none"
    // (e.g. LEVEL_TEST, a physics test course with no scenery of its own)
    public final String backgroundsFolder;
    public final String splashCardPath;

    // this level's own background music, played on entry (see Level.loadLevel()) - null
    // means silence, same "none" convention as backgroundsFolder/splashCardPath above
    public final String musicPath;

    // this level's own results-screen card (LevelCompleteRenderer) - unlike the fields
    // above, null here doesn't mean "no card": every level is meant to have one, so a
    // missing/unset one falls back to a shared passed_generic.png instead of skipping it
    public final String passedCardPath;

    public final int startCol;
    public final int startRow;

    public LevelConfig(String displayName, String mapCsvPath, String backgroundImagePath, String backgroundsFolder,
                        String splashCardPath, String musicPath, String passedCardPath, int startCol, int startRow) {
        this.displayName = displayName;
        this.mapCsvPath = mapCsvPath;
        this.backgroundImage = loadImage(backgroundImagePath);
        this.backgroundsFolder = backgroundsFolder;
        this.splashCardPath = splashCardPath;
        this.musicPath = musicPath;
        this.passedCardPath = passedCardPath;
        this.startCol = startCol;
        this.startRow = startRow;
    }

    private static BufferedImage loadImage(String path) {
        if (path == null || path.isEmpty()) return null;
        try (InputStream is = LevelConfig.class.getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("LevelConfig: image not found: " + path);
                return null;
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("LevelConfig: failed to load image: " + path);
            return null;
        }
    }
}
