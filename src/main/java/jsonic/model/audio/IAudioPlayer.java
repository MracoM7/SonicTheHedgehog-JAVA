package jsonic.model.audio;

/** Lets Level trigger music/sfx without depending on jsonic.view directly; implemented in the View, injected by the Controller. */
public interface IAudioPlayer {

    public void playSfx(String id);

    public void playMusic(String path);

    public void playJingle(String name);

    public void stopMusic();
}
