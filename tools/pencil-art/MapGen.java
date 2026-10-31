import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Draws the pencil town map and world map (into the game's images folder) and the title scene.
 * The title scene goes to the preview folder only, unless "--title" is passed, so it can be
 * looked at before it replaces town_bg.png.
 *
 * Usage (from simsimhaeseo_mandeum/):
 *   java -cp tools/pencil-art/build MapGen src/game/ui/images previewDir [--title]
 */
public class MapGen {
    public static void main(String[] args) throws Exception {
        File images = new File(args[0]);
        File preview = new File(args[1]);
        boolean applyTitle = args.length > 2 && args[2].equals("--title");
        preview.mkdirs();

        write(Maps.TOWN_W, Maps.TOWN_H, 101, Maps::town, new File(images, "town.png"), new File(preview, "map_town.png"));
        write(Maps.WORLD_W, Maps.WORLD_H, 202, Maps::world, new File(images, "dungeonentrence.png"), new File(preview, "map_world.png"));
        write(Maps.MULTI_W, Maps.MULTI_H, 404, Maps::multiverse, new File(images, "multiverse.png"), new File(preview, "map_multiverse.png"));
        new File(images, "colosseum").mkdirs();
        write(Backdrops.W, Backdrops.H, 505, Maps::arena, new File(images, "colosseum/bg.png"), new File(preview, "map_arena.png"));
        write(Backdrops.W, Backdrops.H, 303, Maps::title, applyTitle ? new File(images, "town_bg.png") : null,
                new File(preview, "title_preview.png"));
    }

    private static void write(int w, int h, long seed, Monsters.Drawing d, File target, File copy) throws Exception {
        BufferedImage img = Backdrops.paper(w, h, seed);
        Pencil p = new Pencil(img, seed * 7, 1.4);
        p.paperBacked = true;
        d.draw(p);
        p.dispose();
        Backdrops.rule(img);
        if (target != null) ImageIO.write(img, "png", target);
        ImageIO.write(img, "png", copy);
        System.out.println((target != null ? target : copy).getName() + " done");
    }
}
