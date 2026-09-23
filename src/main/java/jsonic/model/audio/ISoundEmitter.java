package jsonic.model.audio;

/** Lets Player/Item trigger a sound effect or jingle without depending on jsonic.view directly; implemented by Level. */
public interface ISoundEmitter {

    public void playSound(String id);

    public void playJingle(String name);
}
