package io.freemytv;

import android.content.Context;
import android.media.AudioManager;

/**
 * The home screen's sounds (moving focus and opening): the TV's own system sound effects, the ones the stock home
 * screen plays. Like there, they follow the TV's "System sounds" setting. They only play on the home screen, never
 * inside apps.
 */
final class Click {
    /**
     * As loud as Android lets an app play them: the system's own level is -6 dB (config_soundEffectVolumeDb, 0.5 on the
     * Mi TV and in AOSP). The same sounds from our own SoundPool came out quieter on the Mi TV, even 10 dB up.
     */
    private static final float VOLUME = 1f;
    private final AudioManager audio;

    Click(Context c) {
        audio = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
    }

    // Android plays the same sound for every direction unless the firmware maps them apart.
    void move() {
        if (audio != null && Prefs.sounds()) audio.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_DOWN, VOLUME);
    }

    void open() {
        if (audio != null && Prefs.sounds()) audio.playSoundEffect(AudioManager.FX_KEY_CLICK, VOLUME);
    }

    void release() {
    }
}
