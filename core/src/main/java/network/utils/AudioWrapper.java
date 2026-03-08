package network.utils;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.watabou.noosa.audio.Sample;

import network.Multiplayer;

/**
 * Centralized audio playback with multiplayer support.
 * Sounds tied to a specific cell are only played if that cell is visible to the local hero.
 * Global sounds are always played.
 */
public class AudioWrapper {
    private AudioWrapper() {
        /* This utility class should not be instantiated */
    }


    /**
     * Plays a sound associated with a specific cell.
     * The sound is only audible if the cell is visible to the local hero.
     *
     * @param assetName The sound asset identifier (e.g., Assets.Sounds.ITEM)
     * @param cell      The map cell where the sound originates
     */
    public static void play(String assetName, int cell) {
        play(assetName, 1f, 1f, cell);
    }

    /**
     * Plays a sound with custom volume and pitch, associated with a specific cell.
     *
     * @param assetName The sound asset identifier
     * @param volume    Volume multiplier (1.0 = normal)
     * @param pitch     Pitch multiplier (1.0 = normal)
     * @param cell      The map cell where the sound originates
     */
    public static void play(String assetName, float volume, float pitch, int cell) {
        if (shouldPlay(cell)) {
            Sample.INSTANCE.play(assetName, volume, pitch);
        }
    }

    /**
     * Delayed playback for a cell‑specific sound.
     *
     * @param assetName The sound asset identifier
     * @param delay     Delay in seconds
     * @param volume    Volume multiplier
     * @param pitch     Pitch multiplier
     * @param cell      The map cell where the sound originates
     */
    public static void playDelayed(String assetName, float delay, float volume, float pitch, int cell) {
        if (shouldPlay(cell)) {
            Sample.INSTANCE.playDelayed(assetName, delay, volume, pitch);
        }
    }

    /**
     * Plays a sound globally (always audible, regardless of vision).
     *
     * @param assetName The sound asset identifier
     */
    public static void playGlobal(String assetName) {
        Sample.INSTANCE.play(assetName);
    }

    /**
     * Plays a global sound with custom volume and pitch.
     *
     * @param assetName The sound asset identifier
     * @param volume    Volume multiplier
     * @param pitch     Pitch multiplier
     */
    public static void playGlobal(String assetName, float volume, float pitch) {
        Sample.INSTANCE.play(assetName, volume, pitch);
    }

    /**
     * Plays a delayed global sound with custom volume and pitch.
     *
     * @param assetName The sound asset identifier
     * @param delay     Delay in seconds
     * @param volume    Volume multiplier
     * @param pitch     Pitch multiplier
     */
    public static void playGlobalDelayed(String assetName, float delay, float volume, float pitch) {
        Sample.INSTANCE.playDelayed(assetName, delay, volume, pitch);
    }

    public static void playGlobalDelayed(String assetName, float delay) {
        Sample.INSTANCE.playDelayed(assetName, delay);
    }

    // Internal visibility check
    private static boolean shouldPlay(int cell) {
        Hero local = Multiplayer.localHero();
        return local != null && local.fieldOfView != null && local.fieldOfView[cell];
    }
}