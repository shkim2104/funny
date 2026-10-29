import javax.imageio.ImageIO;
import javax.sound.midi.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Composes the game's background music as MIDI files (src/game/ui/music/*.mid), written out as notes
 * the way tools/pencil-art writes out drawings. Every song is a whole number of 4/4 bars and its
 * length is pinned to the last bar, so the game can loop it without a gap.
 *
 * Usage (from simsimhaeseo_mandeum/):
 *   javac -encoding UTF-8 -d tools/music/build tools/music/MusicGen.java
 *   java -cp tools/music/build MusicGen src/game/ui/music [pianoRollDir]
 * With pianoRollDir, it also draws a piano-roll picture of each song there.
 */
public class MusicGen {
    static final int PPQ = 480;
    static final int DRUMS = 9;
    // General MIDI drum keys.
    static final int KICK = 36, SNARE = 38, HAT = 42, CRASH = 49, TAMBOURINE = 54;

    /** One song: a single-track sequence plus small helpers for writing notes in beats. */
    static final class Song {
        final String name;
        final int bars;
        final Sequence seq;
        final Track track;

        Song(String name, double bpm, int bars) throws Exception {
            this.name = name;
            this.bars = bars;
            seq = new Sequence(Sequence.PPQ, PPQ);
            track = seq.createTrack();
            int mpq = (int) Math.round(60_000_000 / bpm);
            track.add(new MidiEvent(new MetaMessage(0x51, new byte[]{(byte) (mpq >> 16), (byte) (mpq >> 8), (byte) mpq}, 3), 0));
        }

        /** Sets a channel's instrument (General MIDI program, 0-based) and a little reverb. */
        void instrument(int ch, int program, int reverb) {
            msg(ShortMessage.PROGRAM_CHANGE, ch, program, 0, 0);
            msg(ShortMessage.CONTROL_CHANGE, ch, 91, reverb, 0);
        }

        void note(int ch, int pitch, double beat, double dur, int vel) {
            long on = Math.round(beat * PPQ), off = Math.max(on + 1, Math.round((beat + dur) * PPQ) - 1);
            msg(ShortMessage.NOTE_ON, ch, pitch, Math.max(1, Math.min(127, vel)), on);
            msg(ShortMessage.NOTE_OFF, ch, pitch, 0, off);
        }

        void drum(int key, double beat, int vel) {
            note(DRUMS, key, beat, 0.25, vel);
        }

        private void msg(int cmd, int ch, int d1, int d2, long tick) {
            try {
                track.add(new MidiEvent(new ShortMessage(cmd, ch, d1, d2), tick));
            } catch (InvalidMidiDataException e) {
                throw new RuntimeException(e);
            }
        }

        /** Pins the length to exactly `bars` bars, so looping returns to bar 1 on the beat. */
        void finish() throws Exception {
            track.add(new MidiEvent(new MetaMessage(0x01, new byte[]{'.'}, 1), bars * 4L * PPQ));
        }
    }

    // ================= notation helpers =================

    /** "C5", "F#4", "Bb3" → MIDI key (C4 = 60). */
    static int pitch(String n) {
        int semi = "C D EF G A B".indexOf(n.charAt(0)), i = 1;
        if (n.charAt(1) == '#') { semi++; i = 2; }
        else if (n.charAt(1) == 'b') { semi--; i = 2; }
        return (Integer.parseInt(n.substring(i)) + 1) * 12 + semi;
    }

    /**
     * Writes a melody bar by bar; each bar is "pitch/beats ..." with r for a rest, e.g. "E5/1 G5/1 C6/2".
     * Every bar must add up to 4 beats, so a typo can't quietly shift the rest of the song.
     */
    static void melody(Song s, int ch, int vel, String... bars) {
        for (int b = 0; b < bars.length; b++) {
            double t = b * 4;
            for (String tok : bars[b].trim().split("\\s+")) {
                String[] p = tok.split("/");
                double d = Double.parseDouble(p[1]);
                // The first note of each bar is leaned on a little, like a player phrasing it.
                int v = vel + (t == b * 4 ? 6 : 0);
                if (!p[0].equals("r")) s.note(ch, pitch(p[0]), t, d * 0.92, v);
                t += d;
            }
            if (Math.abs(t - (b + 1) * 4) > 1e-9) {
                throw new IllegalStateException(s.name + " bar " + (b + 1) + " has " + (t - b * 4) + " beats");
            }
        }
    }

    /** A chord symbol like "Dm", "Bb", "F#" → {root pitch class, third interval}. */
    static int[] chord(String c) {
        int pc = "C D EF G A B".indexOf(c.charAt(0)), i = 1;
        if (c.length() > 1 && c.charAt(1) == '#') { pc++; i = 2; }
        else if (c.length() > 1 && c.charAt(1) == 'b') { pc--; i = 2; }
        boolean minor = c.substring(i).startsWith("m");
        return new int[]{(pc + 12) % 12, minor ? 3 : 4};
    }

    /** Root key of a chord in a given octave. */
    static int root(String c, int octave) {
        return (octave + 1) * 12 + chord(c)[0];
    }

    /** Plays the chord's three notes together. */
    static void triad(Song s, int ch, String c, int octave, double beat, double dur, int vel) {
        int r = root(c, octave), third = chord(c)[1];
        s.note(ch, r, beat, dur, vel);
        s.note(ch, r + third, beat, dur, vel - 4);
        s.note(ch, r + 7, beat, dur, vel - 6);
    }

    /** Eighth notes from offsets above the chord root; "3" in the pattern means the chord's third. */
    static void pattern(Song s, int ch, String c, int octave, int bar, int[] offsets, double len, int vel) {
        int r = root(c, octave), third = chord(c)[1];
        for (int i = 0; i < offsets.length; i++) {
            int o = offsets[i] == 3 ? third : offsets[i] == 15 ? 12 + third : offsets[i];
            s.note(ch, r + o, bar * 4 + i * 0.5, len, vel + (i % 2 == 0 ? 8 : 0));
        }
    }

    // ================= the songs =================

    /** 타이틀 「공책 속 모험」: a music box over rippling piano, C major, gentle. */
    static Song title() throws Exception {
        Song s = new Song("title", 92, 16);
        s.instrument(0, 10, 70);   // music box
        s.instrument(1, 0, 60);    // piano
        s.instrument(2, 48, 80);   // strings
        s.instrument(3, 42, 60);   // cello
        String[] chords = {"C", "G", "Am", "Em", "F", "C", "F", "G", "C", "G", "Am", "Em", "F", "G", "C", "C"};
        melody(s, 0, 88,
                "E5/1 G5/1 C6/1.5 B5/0.5", "A5/1 G5/1 D5/2", "C5/1 E5/1 A5/1.5 G5/0.5", "G5/3 r/1",
                "F5/1 A5/1 C6/1 A5/1", "G5/1 E5/1 C5/2", "D5/1 F5/1 A5/1 C6/1", "B5/3 r/1",
                "E5/1 G5/1 C6/1.5 D6/0.5", "E6/1 D6/1 B5/2", "C6/1 B5/1 A5/1.5 G5/0.5", "E5/3 r/1",
                "F5/1 A5/1 C6/1 F6/1", "E6/1 D6/1 B5/1 G5/1", "C6/4", "r/2 G5/1 B5/1");
        int[] arp = {0, 7, 12, 15, 19, 15, 12, 7};
        for (int b = 0; b < 16; b++) {
            pattern(s, 1, chords[b], 3, b, arp, 0.9, 46);
            triad(s, 2, chords[b], 4, b * 4, 4, 34);
            s.note(3, root(chords[b], 2), b * 4, 3.8, 52);
        }
        s.finish();
        return s;
    }

    /** 마을: a bouncy recorder tune with oom-pah guitar, G major. */
    static Song town() throws Exception {
        Song s = new Song("town", 112, 16);
        s.instrument(0, 74, 40);   // recorder
        s.instrument(1, 24, 40);   // nylon guitar
        s.instrument(2, 32, 20);   // acoustic bass
        s.instrument(3, 9, 60);    // glockenspiel (doubles the tune the second time round)
        String[] chords = {"G", "D", "Em", "C", "G", "C", "D", "D", "G", "D", "Em", "C", "Am", "D", "G", "G"};
        String[] tune = {
                "B4/0.5 D5/0.5 G5/1 F#5/0.5 G5/0.5 A5/1", "F#5/1.5 E5/0.5 D5/2", "E5/0.5 F#5/0.5 G5/1 B5/1 A5/0.5 G5/0.5", "E5/2 C5/1 r/1",
                "D5/0.5 G5/0.5 B5/1 A5/0.5 G5/0.5 B5/1", "C6/1.5 B5/0.5 A5/1 G5/1", "F#5/1 A5/1 D6/1 C6/1", "A5/3 r/1",
                "B5/0.5 A5/0.5 G5/1 D5/1 G5/1", "A5/1 F#5/1 D5/2", "G5/0.5 A5/0.5 B5/1 E6/1 D6/0.5 B5/0.5", "C6/2 G5/1 E5/1",
                "A5/1 C6/1 E6/1 C6/1", "D6/1 C6/0.5 A5/0.5 F#5/1 A5/1", "G5/4", "r/3 D5/1"};
        melody(s, 0, 92, tune);
        String[] secondHalf = new String[16];
        for (int b = 0; b < 16; b++) secondHalf[b] = b < 8 ? "r/4" : up(tune[b]);
        melody(s, 3, 44, secondHalf);
        for (int b = 0; b < 16; b++) {
            String c = chords[b];
            s.note(2, root(c, 2), b * 4, 0.9, 82);
            s.note(2, root(c, 2) + 7, b * 4 + 2, 0.9, 74);
            triad(s, 1, c, 4, b * 4 + 1, 0.45, 58);
            triad(s, 1, c, 4, b * 4 + 3, 0.45, 58);
            for (int e = 0; e < 8; e++) s.drum(HAT, b * 4 + e * 0.5, e % 2 == 0 ? 42 : 30);
            s.drum(KICK, b * 4, 58);
            s.drum(KICK, b * 4 + 2, 50);
            s.drum(TAMBOURINE, b * 4 + 1, 44);
            s.drum(TAMBOURINE, b * 4 + 3, 44);
        }
        s.finish();
        return s;
    }

    /** A melody bar moved up an octave. */
    static String up(String bar) {
        StringBuilder sb = new StringBuilder();
        for (String tok : bar.trim().split("\\s+")) {
            String[] p = tok.split("/");
            if (!p[0].equals("r")) {
                int o = p[0].length() - 1;
                p[0] = p[0].substring(0, o) + (Integer.parseInt(p[0].substring(o)) + 1);
            }
            sb.append(p[0]).append('/').append(p[1]).append(' ');
        }
        return sb.toString().trim();
    }

    /** 1던전 초원: setting out — flute over galloping strings, D major with a bright C chord. */
    static Song dungeon1() throws Exception {
        Song s = new Song("dungeon1", 132, 16);
        s.instrument(0, 73, 50);   // flute
        s.instrument(1, 48, 50);   // strings
        s.instrument(2, 45, 30);   // pizzicato
        s.instrument(3, 60, 60);   // french horn
        String[] chords = {"D", "C", "G", "D", "D", "C", "G", "A", "Bm", "G", "D", "A", "Bm", "G", "A", "A"};
        melody(s, 0, 96,
                "D5/1 F#5/0.5 A5/0.5 D6/1.5 A5/0.5", "G5/1 E5/1 C5/1.5 E5/0.5", "D5/1 G5/1 B5/1 A5/0.5 G5/0.5", "A5/3 F#5/1",
                "D5/1 F#5/0.5 A5/0.5 D6/1 E6/1", "E6/1 C6/1 G5/1.5 C6/0.5", "B5/1 D6/1 G5/1 B5/1", "A5/2 C#6/1 E6/1",
                "F#6/1.5 E6/0.5 D6/1 B5/1", "D6/1.5 B5/0.5 G5/2", "A5/1 D6/1 F#6/1 E6/0.5 D6/0.5", "E6/2 C#6/1 A5/1",
                "B5/1 D6/1 F#6/1 D6/1", "G6/1 F#6/0.5 E6/0.5 D6/1 B5/1", "C#6/1 E6/1 A6/2", "A5/0.5 B5/0.5 C#6/0.5 E6/0.5 A6/2");
        int[] gallop = {0, 7, 0, 7, 0, 7, 12, 7};
        for (int b = 0; b < 16; b++) {
            String c = chords[b];
            pattern(s, 1, c, 4, b, gallop, 0.42, 56);
            for (int q = 0; q < 4; q++) s.note(2, root(c, 2) + (q == 2 ? 7 : 0), b * 4 + q, 0.6, 84);
            int r = root(c, 4), third = chord(c)[1];
            s.note(3, r + third, b * 4, 1.9, 54);
            s.note(3, r + 7, b * 4 + 2, 1.9, 50);
            for (int e = 0; e < 8; e++) s.drum(HAT, b * 4 + e * 0.5, e % 2 == 0 ? 44 : 32);
            s.drum(KICK, b * 4, 74);
            s.drum(KICK, b * 4 + 2, 70);
            if (b % 2 == 1) s.drum(KICK, b * 4 + 2.5, 60);
            s.drum(SNARE, b * 4 + 1, 66);
            s.drum(SNARE, b * 4 + 3, 66);
            if (b % 8 == 0) s.drum(CRASH, b * 4, 72);
        }
        s.finish();
        return s;
    }

    /** 보스전: brass and pounding strings in D minor, with choir and timpani. */
    static Song boss() throws Exception {
        Song s = new Song("boss", 160, 16);
        s.instrument(0, 61, 50);   // brass section
        s.instrument(1, 48, 40);   // strings, played staccato
        s.instrument(2, 33, 15);   // fingered bass
        s.instrument(3, 52, 80);   // choir aahs
        s.instrument(4, 47, 50);   // timpani
        String[] chords = {"Dm", "Bb", "C", "A", "Dm", "Bb", "Gm", "A", "Dm", "Bb", "C", "A", "Gm", "A", "Dm", "A"};
        melody(s, 0, 104,
                "D5/1.5 A4/0.5 D5/1 F5/1", "F5/1.5 D5/0.5 Bb4/2", "C5/1 E5/1 G5/1.5 F5/0.5", "E5/2 C#5/2",
                "D5/0.5 E5/0.5 F5/1 A5/1 G5/0.5 F5/0.5", "D5/2 F5/1 Bb5/1", "G5/1.5 F5/0.5 D5/1 Bb4/1", "A4/2 C#5/1 E5/1",
                "A5/1.5 F5/0.5 D5/1 A5/1", "Bb5/2 F5/1 D5/1", "C6/1.5 G5/0.5 E5/1 G5/1", "A5/2 E5/1 C#5/1",
                "G5/1 Bb5/1 D6/1 C6/0.5 Bb5/0.5", "A5/1 C#6/1 E6/2", "D6/2 A5/1 F5/1", "E5/1 C#5/1 A4/1 r/1");
        int[] drive = {0, 0, 12, 0, 7, 0, 12, 7};
        for (int b = 0; b < 16; b++) {
            String c = chords[b];
            pattern(s, 1, c, 3, b, drive, 0.35, 80);
            for (int e = 0; e < 8; e++) s.note(2, root(c, 2), b * 4 + e * 0.5, 0.42, e % 2 == 0 ? 96 : 84);
            triad(s, 3, c, 4, b * 4, 4, 46);
            s.note(4, root(c, 2), b * 4, 0.9, 100);
            s.note(4, root(c, 2), b * 4 + 2, 0.9, 88);
            for (int e = 0; e < 8; e++) s.drum(HAT, b * 4 + e * 0.5, e % 2 == 0 ? 54 : 40);
            s.drum(KICK, b * 4, 96);
            s.drum(KICK, b * 4 + 1.5, 84);
            s.drum(KICK, b * 4 + 2, 92);
            s.drum(SNARE, b * 4 + 1, 88);
            s.drum(SNARE, b * 4 + 3, 88);
            if (b % 8 == 0) s.drum(CRASH, b * 4, 100);
        }
        // Snare roll into the loop.
        for (int i = 0; i < 8; i++) s.drum(SNARE, 15 * 4 + 2 + i * 0.25, 60 + i * 6);
        s.finish();
        return s;
    }

    // ================= output =================

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        File rolls = args.length > 1 ? new File(args[1]) : null;
        for (Song s : new Song[]{title(), town(), dungeon1(), boss()}) {
            MidiSystem.write(s.seq, 0, new File(out, s.name + ".mid"));
            double seconds = s.seq.getMicrosecondLength() / 1e6;
            System.out.printf("%s.mid  %d bars  %.1fs  %d events%n", s.name, s.bars, seconds, s.track.size());
            if (rolls != null) pianoRoll(s, new File(rolls, "roll_" + s.name + ".png"));
        }
    }

    /** A piano-roll picture: one colored bar per note, a line per bar, drums along the bottom. */
    static void pianoRoll(Song s, File file) throws Exception {
        int w = 1600, h = 560, lo = 24, hi = 96;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(247, 243, 230));
        g.fillRect(0, 0, w, h);
        double total = s.bars * 4.0 * PPQ;
        g.setColor(new Color(110, 150, 205, 90));
        for (int b = 0; b <= s.bars; b++) g.drawLine((int) (b * 4 * PPQ / total * w), 0, (int) (b * 4 * PPQ / total * w), h);
        Color[] colors = {new Color(196, 52, 48), new Color(52, 96, 170), new Color(96, 70, 40), new Color(54, 134, 58),
                new Color(128, 64, 176), new Color(196, 140, 20), Color.GRAY, Color.GRAY, Color.GRAY, new Color(60, 60, 60)};
        java.util.Map<Integer, Long> onTick = new java.util.HashMap<>();
        for (int i = 0; i < s.track.size(); i++) {
            MidiMessage m = s.track.get(i).getMessage();
            if (!(m instanceof ShortMessage)) continue;
            ShortMessage sm = (ShortMessage) m;
            int key = sm.getChannel() * 128 + sm.getData1();
            long t = s.track.get(i).getTick();
            if (sm.getCommand() == ShortMessage.NOTE_ON && sm.getData2() > 0) onTick.put(key, t);
            else if (sm.getCommand() == ShortMessage.NOTE_OFF && onTick.containsKey(key)) {
                long t0 = onTick.remove(key);
                int x0 = (int) (t0 / total * w), x1 = (int) Math.max(x0 + 2, t / total * w);
                int y = sm.getChannel() == DRUMS ? h - 12 - (sm.getData1() - 35) / 2
                        : (int) (h - 40 - (sm.getData1() - lo) / (double) (hi - lo) * (h - 60));
                g.setColor(colors[Math.min(sm.getChannel(), colors.length - 1)]);
                g.fillRect(x0, y, x1 - x0, sm.getChannel() == DRUMS ? 3 : 5);
            }
        }
        g.setColor(new Color(50, 48, 58));
        g.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        g.drawString(s.name, 12, 28);
        g.dispose();
        ImageIO.write(img, "png", file);
    }
}
