package jsonic.model.audio;

/** Lets Player trigger a sound effect without depending on jsonic.view directly; implemented by Level. */
public interface ISoundEmitter {

    public void playSound(String id);
}
