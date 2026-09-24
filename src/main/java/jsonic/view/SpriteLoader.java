package jsonic.view;

import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Reads one sprite from the classpath, logging and returning null instead of throwing if it's missing or unreadable. */
public final class SpriteLoader {

    private SpriteLoader() {}

    public static BufferedImage load(Class<?> caller, String path) {
        try {
            return ImageIO.read(caller.getResourceAsStream(path));
        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println(caller.getSimpleName() + ": failed to load sprite from " + path);
            return null;
        }
    }
}
