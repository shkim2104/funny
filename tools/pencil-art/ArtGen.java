import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the pencil-on-notebook dungeon art into the game's image folder:
 * images/dungeonN/bg.png plus one PNG per monster (file names match BattleStage's addSprite list).
 *
 * Usage (from simsimhaeseo_mandeum/):
 *   javac -encoding UTF-8 -d tools/pencil-art/build tools/pencil-art/*.java
 *   java -cp tools/pencil-art/build ArtGen src/game/ui/images [sheetDir]
 * With sheetDir, it also writes a contact sheet per dungeon there for a quick look.
 */
public class ArtGen {
    /** A dungeon: its backdrop and its monsters (file name → drawing), in World.buildDungeons() order. */
    private static final class DungeonArt {
        final int number;
        final Monsters.Drawing backdrop;
        final Map<String, Monsters.Drawing> monsters = new LinkedHashMap<>();

        DungeonArt(int number, Monsters.Drawing backdrop) {
            this.number = number;
            this.backdrop = backdrop;
        }

        DungeonArt add(String file, Monsters.Drawing d) {
            monsters.put(file, d);
            return this;
        }
    }

    private static DungeonArt[] dungeons() {
        return new DungeonArt[]{
                new DungeonArt(1, Backdrops::meadow)
                        .add("rat", Monsters::rat)
                        .add("boar", Monsters::boar)
                        .add("dog", Monsters::dog)
                        .add("raccoon", Monsters::raccoon)
                        .add("boss_wolf", Monsters::alphaWolf),
                new DungeonArt(2, Backdrops::cave)
                        .add("bat", p -> Monsters.bat(p, 0.55, null, Monsters.RED))
                        .add("goblin", Monsters::goblin)
                        .add("cave_spider", Monsters::caveSpider)
                        .add("cave_bear", Monsters::caveBear)
                        .add("boss_orc_chief", Monsters::orcChief),
                new DungeonArt(3, Backdrops::ruins)
                        .add("skeleton_soldier", LateMonsters::skeletonSoldier)
                        .add("wraith", LateMonsters::wraith)
                        .add("zombie", LateMonsters::zombie)
                        .add("ruin_bandit", LateMonsters::ruinBandit)
                        .add("boss_lich", LateMonsters::lich),
                new DungeonArt(4, Backdrops::castle)
                        .add("dark_knight", LateMonsters::darkKnight)
                        .add("fallen_mage", LateMonsters::fallenMage)
                        .add("hellhound", LateMonsters::hellhound)
                        .add("demon_archer", LateMonsters::demonArcher)
                        .add("boss_demon_lord", LateMonsters::demonLord),
                new DungeonArt(5, Backdrops::iceCanyon)
                        .add("frost_wolf", LateMonsters::frostWolf)
                        .add("ice_spirit", LateMonsters::iceSpirit)
                        .add("snow_golem", LateMonsters::snowGolem)
                        .add("glacier_bat", p -> Monsters.bat(p, 0.2, Monsters.ICE, Monsters.ICE))
                        .add("boss_frost_queen", LateMonsters::frostQueen),
                new DungeonArt(6, Backdrops::volcano)
                        .add("fire_salamander", LateMonsters::fireSalamander)
                        .add("lava_golem", LateMonsters::lavaGolem)
                        .add("flame_wraith", LateMonsters::flameWraith)
                        .add("volcano_ogre", LateMonsters::volcanoOgre)
                        .add("boss_flame_lord", LateMonsters::flameLord),
                new DungeonArt(7, Backdrops::skyCastle)
                        .add("storm_griffin", LateMonsters::stormGriffin)
                        .add("sky_knight", LateMonsters::skyKnight)
                        .add("thunder_golem", LateMonsters::thunderGolem)
                        .add("fallen_angel", LateMonsters::fallenAngel)
                        .add("boss_fallen_king", LateMonsters::fallenKing),
        };
    }

    public static void main(String[] args) throws Exception {
        File root = new File(args[0]);
        File sheets = args.length > 1 ? new File(args[1]) : null;
        for (DungeonArt d : dungeons()) {
            File dir = new File(root, "dungeon" + d.number);
            dir.mkdirs();
            BufferedImage bg = Backdrops.paper(d.number * 7919L);
            Pencil bp = new Pencil(bg, d.number * 31L, 1.5);
            d.backdrop.draw(bp);
            bp.dispose();
            Backdrops.rule(bg);
            ImageIO.write(bg, "png", new File(dir, "bg.png"));

            int i = 0;
            BufferedImage[] made = new BufferedImage[d.monsters.size()];
            for (Map.Entry<String, Monsters.Drawing> e : d.monsters.entrySet()) {
                BufferedImage img = new BufferedImage(Monsters.SIZE, Monsters.SIZE, BufferedImage.TYPE_INT_ARGB);
                // Bold lines: in battle a monster is shown at roughly a quarter of this size.
                Pencil p = new Pencil(img, (d.number * 100L + i) * 977L, 1.8);
                e.getValue().draw(p);
                p.dispose();
                ImageIO.write(img, "png", new File(dir, e.getKey() + ".png"));
                made[i++] = img;
            }
            if (sheets != null) writeSheet(bg, made, new File(sheets, "dungeon" + d.number + "_sheet.png"));
            System.out.println("dungeon" + d.number + ": bg + " + made.length + " monsters");
        }
        writeIcons(new File(root, "items"), Icons.items(), 501, sheets == null ? null : new File(sheets, "items_sheet.png"));
        writeIcons(new File(root, "augments"), Icons.augments(), 777, sheets == null ? null : new File(sheets, "augments_sheet.png"));
    }

    /** One 256px transparent PNG per icon; optionally a grid sheet of them on paper. */
    private static void writeIcons(File dir, Map<String, Monsters.Drawing> icons, long seed, File sheet) throws Exception {
        dir.mkdirs();
        int cols = 8, cell = 160, i = 0;
        int rows = (icons.size() + cols - 1) / cols;
        BufferedImage grid = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_RGB);
        Graphics2D gg = grid.createGraphics();
        gg.setColor(new Color(32, 34, 40));   // the game's dark list background
        gg.fillRect(0, 0, grid.getWidth(), grid.getHeight());
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (Map.Entry<String, Monsters.Drawing> e : icons.entrySet()) {
            BufferedImage img = new BufferedImage(Icons.SIZE, Icons.SIZE, BufferedImage.TYPE_INT_ARGB);
            // Bold lines: icons are shown at 44~128px.
            Pencil p = new Pencil(img, seed * 1000 + i, 2.0);
            e.getValue().draw(p);
            p.dispose();
            ImageIO.write(img, "png", new File(dir, e.getKey() + ".png"));
            gg.drawImage(img, (i % cols) * cell + 8, (i / cols) * cell + 8, cell - 16, cell - 16, null);
            i++;
        }
        gg.dispose();
        if (sheet != null) ImageIO.write(grid, "png", sheet);
        System.out.println(dir.getName() + ": " + icons.size() + " icons");
    }

    /** Backdrop on top, the monsters in a row under it, each over the paper so the pencil reads. */
    private static void writeSheet(BufferedImage bg, BufferedImage[] monsters, File out) throws Exception {
        int cell = 320;
        int w = cell * monsters.length, bh = (int) Math.round(bg.getHeight() * (w / (double) bg.getWidth()));
        BufferedImage sheet = new BufferedImage(w, bh + cell, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(bg, 0, 0, w, bh, null);
        g.setColor(Pencil.PAPER);
        g.fillRect(0, bh, w, cell);
        for (int i = 0; i < monsters.length; i++) g.drawImage(monsters[i], i * cell, bh, cell, cell, null);
        g.dispose();
        out.getParentFile().mkdirs();
        ImageIO.write(sheet, "png", out);
    }
}
