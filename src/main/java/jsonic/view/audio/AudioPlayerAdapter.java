package jsonic.view.audio;

import jsonic.model.audio.IAudioPlayer;

/** Thin adapter over AudioManager's static methods, so the Model can depend on IAudioPlayer instead of jsonic.view directly. */
public class AudioPlayerAdapter implements IAudioPlayer {

    @Override
    public void playSfx(String id) {
        AudioManager.playSfx(id);
    }

    @Override
    public void playMusic(String path) {
        AudioManager.playMusic(path);
    }

    @Override
    public void playJingle(String name) {
        AudioManager.playJingle(name);
    }

    @Override
    public void stopMusic() {
        AudioManager.stopMusic();
    }
}
