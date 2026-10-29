package game.ui;

import game.save.Settings;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background music: loops one of the MIDI songs in music/ (made by tools/music/MusicGen) through
 * Java's built-in synthesizer. Everything runs on one background thread, so opening the synthesizer
 * or switching songs never stalls the screen. If there is no sound device, music just stays off.
 */
public final class Music {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "music");
        t.setDaemon(true);
        return t;
    });

    private static Sequencer sequencer;
    private static Synthesizer synth;
    private static boolean unavailable;
    /** The song that should be playing (kept while muted, so turning the volume up resumes it). */
    private static String wanted;
    private static String playing;

    private Music() {}

    /** Plays the song (looping); does nothing if it is already playing. */
    public static void play(String name) {
        play(name, null);
    }

    /** As play(name), falling back to another song if this one hasn't been made yet. */
    public static void play(String name, String fallback) {
        WORKER.execute(() -> {
            String song = exists(name) ? name : fallback;
            if (song == null) return;
            wanted = song;
            if (Settings.getMusicVolume() <= 0) return;
            start(song);
        });
    }

    /** Applies a new volume (0 = off) right away. */
    public static void volumeChanged() {
        WORKER.execute(() -> {
            if (Settings.getMusicVolume() <= 0) {
                if (sequencer != null) sequencer.stop();
                playing = null;
            } else if (playing == null && wanted != null) {
                start(wanted);
            } else {
                applyVolume();
            }
        });
    }

    private static boolean exists(String name) {
        return name != null && Music.class.getResource("music/" + name + ".mid") != null;
    }

    private static void start(String song) {
        if (song.equals(playing) || !open()) return;
        try (InputStream in = new BufferedInputStream(Music.class.getResourceAsStream("music/" + song + ".mid"))) {
            Sequence seq = MidiSystem.getSequence(in);
            sequencer.stop();
            sequencer.setSequence(seq);
            sequencer.setLoopStartPoint(0);
            sequencer.setLoopEndPoint(-1);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTickPosition(0);
            sequencer.start();
            playing = song;
            applyVolume();
        } catch (Exception e) {
            System.out.println("음악을 재생하지 못했습니다: " + e.getMessage());
        }
    }

    /** Opens the sequencer wired to our own synthesizer once; false if there is no sound. */
    private static boolean open() {
        if (sequencer != null) return true;
        if (unavailable) return false;
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            sequencer.getTransmitter().setReceiver(synth.getReceiver());
            return true;
        } catch (Exception e) {
            unavailable = true;
            sequencer = null;
            System.out.println("음악 장치를 열지 못해 배경음악 없이 진행합니다: " + e.getMessage());
            return false;
        }
    }

    /** Volume is the channel volume controller on every channel (the songs never set it themselves). */
    private static void applyVolume() {
        if (synth == null) return;
        int v = Math.round(127 * Math.max(0, Math.min(100, Settings.getMusicVolume())) / 100f);
        for (MidiChannel ch : synth.getChannels()) if (ch != null) ch.controlChange(7, v);
    }
}
