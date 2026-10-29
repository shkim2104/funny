package game.ui;

import game.save.Settings;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Short sound effects, synthesized in code (square / triangle tones and noise) into in-memory clips:
 * no sound files to ship. Clips are made on first use on a background thread; with no sound device,
 * effects just stay silent.
 */
public final class Sfx {
    public enum Sound { HIT, CRIT, HURT, HEAL, COIN, LEVEL_UP, CARD, TICK, WHEEL_STOP, PAGE }

    private static final float RATE = 22050f;
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sfx");
        t.setDaemon(true);
        return t;
    });
    private static final Map<Sound, Clip> CLIPS = new EnumMap<>(Sound.class);
    private static boolean unavailable;

    private Sfx() {}

    public static void play(Sound s) {
        if (Settings.getSfxVolume() <= 0) return;
        WORKER.execute(() -> {
            Clip c = clip(s);
            if (c == null) return;
            gain(c);
            c.stop();
            c.setFramePosition(0);
            c.start();
        });
    }

    private static Clip clip(Sound s) {
        if (unavailable) return null;
        Clip c = CLIPS.get(s);
        if (c != null) return c;
        try {
            byte[] pcm = render(s);
            c = AudioSystem.getClip();
            c.open(new AudioFormat(RATE, 16, 1, true, false), pcm, 0, pcm.length);
            CLIPS.put(s, c);
            return c;
        } catch (Exception e) {
            unavailable = true;
            System.out.println("효과음 장치를 열지 못해 효과음 없이 진행합니다: " + e.getMessage());
            return null;
        }
    }

    private static void gain(Clip c) {
        if (!c.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl g = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
        float v = Math.max(1, Settings.getSfxVolume()) / 100f;
        g.setValue(Math.max(g.getMinimum(), Math.min(g.getMaximum(), (float) (20 * Math.log10(v)))));
    }

    // ---------- synthesis ----------

    private static byte[] render(Sound s) {
        switch (s) {
            case HIT: return mix(noise(0.07, 0.55, 900), tone(110, 55, 0.08, 0.7, Wave.TRIANGLE));
            case CRIT: return mix(noise(0.1, 0.5, 2500), tone(880, 330, 0.14, 0.45, Wave.SQUARE));
            case HURT: return tone(330, 140, 0.13, 0.45, Wave.SQUARE);
            case HEAL: return seq(0.07, 0.4, Wave.TRIANGLE, 523, 659, 784, 1047);
            case COIN: return seq(new double[]{0.06, 0.16}, 0.35, Wave.SQUARE, 988, 1319);
            case LEVEL_UP: return seq(0.09, 0.35, Wave.SQUARE, 523, 659, 784, 1047, 1319);
            case CARD: return tone(400, 1200, 0.16, 0.35, Wave.TRIANGLE);
            case TICK: return tone(1800, 1800, 0.012, 0.35, Wave.SQUARE);
            case WHEEL_STOP: return mix(seq(0.05, 0.3, Wave.TRIANGLE, 784, 988, 1175), tone(1568, 1568, 0.4, 0.25, Wave.TRIANGLE));
            case PAGE: return noise(0.22, 0.3, 1400);
            default: return new byte[2];
        }
    }

    private enum Wave { SQUARE, TRIANGLE }

    /** A tone gliding from f0 to f1 Hz with a quick attack and a falling tail. */
    private static byte[] tone(double f0, double f1, double seconds, double vol, Wave wave) {
        int n = (int) (seconds * RATE);
        short[] out = new short[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = f0 + (f1 - f0) * t;
            phase += f / RATE;
            double p = phase - Math.floor(phase);
            double v = wave == Wave.SQUARE ? (p < 0.5 ? 1 : -1) * 0.6 : 4 * Math.abs(p - 0.5) - 1;
            out[i] = (short) (v * env(i, n) * vol * 32767);
        }
        return bytes(out);
    }

    /** Notes one after another, each `step` seconds long (or per-note lengths). */
    private static byte[] seq(double step, double vol, Wave wave, double... freqs) {
        double[] lens = new double[freqs.length];
        java.util.Arrays.fill(lens, step);
        return seq(lens, vol, wave, freqs);
    }

    private static byte[] seq(double[] lens, double vol, Wave wave, double... freqs) {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < freqs.length; i++) {
            byte[] b = tone(freqs[i], freqs[i], lens[i], vol, wave);
            bo.write(b, 0, b.length);
        }
        return bo.toByteArray();
    }

    /** Noise through a simple low-pass filter (lower cutoff = duller). */
    private static byte[] noise(double seconds, double vol, double cutoffHz) {
        int n = (int) (seconds * RATE);
        short[] out = new short[n];
        Random r = new Random(7);
        double a = Math.min(1, 2 * Math.PI * cutoffHz / RATE), y = 0;
        for (int i = 0; i < n; i++) {
            y += a * ((r.nextDouble() * 2 - 1) - y);
            out[i] = (short) (Math.max(-1, Math.min(1, y * 2.2)) * env(i, n) * vol * 32767);
        }
        return bytes(out);
    }

    /** 3 ms attack, then a curved fade to silence, so nothing clicks. */
    private static double env(int i, int n) {
        int attack = (int) (0.003 * RATE);
        double a = i < attack ? i / (double) attack : 1;
        double t = i / (double) n;
        return a * Math.pow(1 - t, 1.6);
    }

    private static byte[] mix(byte[] a, byte[] b) {
        byte[] out = new byte[Math.max(a.length, b.length)];
        for (int i = 0; i + 1 < out.length; i += 2) {
            int s = sample(a, i) + sample(b, i);
            s = Math.max(-32768, Math.min(32767, s));
            out[i] = (byte) s;
            out[i + 1] = (byte) (s >> 8);
        }
        return out;
    }

    private static int sample(byte[] b, int i) {
        return i + 1 < b.length ? (short) ((b[i] & 0xff) | (b[i + 1] << 8)) : 0;
    }

    private static byte[] bytes(short[] s) {
        byte[] b = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            b[i * 2] = (byte) s[i];
            b[i * 2 + 1] = (byte) (s[i] >> 8);
        }
        return b;
    }
}
