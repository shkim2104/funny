package game.ui;

import game.model.CraftedGear;
import game.model.Player;
import game.save.SaveManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Drawings of forged gear (대장간 제작), one GEAR_W x GEAR_H picture per piece in the hero's figure units, so
 * a weapon drawn big is big in battle too. A weapon is held at (GRIP_X, GRIP_Y): the hand sits there. The
 * pictures live in PNGs beside the character's save and are kept here by item name once loaded.
 */
public final class GearArt {
    public static final int GEAR_W = 600, GEAR_H = 640, GRIP_X = 300, GRIP_Y = 470;

    private static final Map<String, BufferedImage> DRAWINGS = new HashMap<>();
    /** The drawing cut to what was drawn and centred on a square, for icons. */
    private static final Map<String, BufferedImage> ICONS = new HashMap<>();

    private GearArt() {}

    /** Reads every forged piece's drawing of this character (a missing file just leaves the piece without one). */
    public static void loadAll(String saveId, Player player) {
        if (saveId == null) return;
        for (CraftedGear c : player.getCrafted()) {
            File f = SaveManager.drawingFile(saveId, c.artKey());
            try {
                if (f.isFile()) put(c.getName(), ImageIO.read(f));
            } catch (Exception ignored) {
                // unreadable: the piece shows the plain doodle icon
            }
        }
    }

    /** Keeps a new drawing and writes it beside the save. */
    public static void save(String saveId, String itemName, BufferedImage drawing) {
        put(itemName, drawing);
        if (saveId == null) return;
        try {
            ImageIO.write(drawing, "png", SaveManager.drawingFile(saveId, CraftedGear.artKey(itemName)));
        } catch (Exception e) {
            System.out.println("장비 그림을 저장하지 못했습니다: " + e.getMessage());
        }
    }

    public static void put(String itemName, BufferedImage drawing) {
        if (drawing == null) return;
        DRAWINGS.put(itemName, drawing);
        ICONS.remove(itemName);
        ItemArt.forgetCrafted(itemName);
    }

    /** The full drawing (grip at GRIP_X, GRIP_Y), or null if there is none. */
    public static BufferedImage drawing(String itemName) {
        return DRAWINGS.get(itemName);
    }

    /** A square picture for icons: the drawn part only, centred, or null without a drawing. */
    static BufferedImage icon(String itemName) {
        BufferedImage src = DRAWINGS.get(itemName);
        if (src == null) return null;
        return ICONS.computeIfAbsent(itemName, k -> square(src));
    }

    private static BufferedImage square(BufferedImage src) {
        Rectangle r = drawnBounds(src);
        int side = Math.max(r.width, r.height) + 24;
        BufferedImage out = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        double s = 256.0 / side;
        g.translate(128 - r.getCenterX() * s, 128 - r.getCenterY() * s);
        g.scale(s, s);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Where anything was drawn; the whole picture if nothing was. */
    public static Rectangle drawnBounds(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y += 2) {
            for (int x = 0; x < img.getWidth(); x += 2) {
                if ((img.getRGB(x, y) >>> 24) > 30) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) return new Rectangle(0, 0, img.getWidth(), img.getHeight());
        return new Rectangle(minX, minY, maxX - minX + 2, maxY - minY + 2);
    }

    /** True if nothing has been drawn yet. */
    public static boolean isBlank(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y += 3) {
            for (int x = 0; x < img.getWidth(); x += 3) if ((img.getRGB(x, y) >>> 24) > 30) return false;
        }
        return true;
    }
}
