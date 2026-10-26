package com.nexuscraft.nexuswardrobe;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import static com.nexuscraft.nexuswardrobe.Kit.expect;

/** The clothes as items and as recipes. */
public class ItemsTest {

    public static void main(String[] args) {
        items();
        recipes();
        Kit.finish();
    }

    static void items() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        for (Garment g : Garment.values()) {
            ItemStack item = r.items.create(g);
            expect(r.items.garmentOf(item) == g, g + " round-trips through its tag");
            expect(item.getType().name().startsWith("LEATHER_"), g + " is a piece of leather armour");
            ItemMeta meta = item.getItemMeta();
            expect(meta.getDisplayName().contains(g.title()), g + " carries its name");
            expect(meta.getLore().stream().anyMatch(l -> l.contains("NexusWardrobe")), g + " says where it is from");
            expect(meta instanceof LeatherArmorMeta lm && lm.getColor().asRGB() == g.rgb(), g + " is dyed its colour");
            Insulation.Piece piece = r.items.pieceOf(item);
            expect(piece.garment() == g, g + " is seen as a garment when worn");
        }
        expect(GarmentItems.base(Garment.PARKA) == Material.LEATHER_CHESTPLATE && GarmentItems.base(Garment.WOOL_CAP) == Material.LEATHER_HELMET
                && GarmentItems.base(Garment.WOOL_TROUSERS) == Material.LEATHER_LEGGINGS && GarmentItems.base(Garment.FUR_BOOTS) == Material.LEATHER_BOOTS, "slots map to armour");

        ItemStack lookalike = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
        ItemMeta m = lookalike.getItemMeta();
        m.setDisplayName("§fParka");
        lookalike.setItemMeta(m);
        expect(r.items.garmentOf(lookalike) == null, "a renamed leather chestplate is not a Parka");
        expect(r.items.pieceOf(lookalike).garment() == null && r.items.pieceOf(lookalike).material().equals("LEATHER_CHESTPLATE"), "it is just leather");
        ItemStack renamed = r.items.create(Garment.PARKA);
        ItemMeta rm = renamed.getItemMeta();
        rm.setDisplayName("My favourite coat");
        renamed.setItemMeta(rm);
        expect(r.items.garmentOf(renamed) == Garment.PARKA, "but renaming a real one keeps it a Parka");
        expect(r.items.garmentOf(null) == null && r.items.pieceOf(null) == null && r.items.pieceOf(new ItemStack(Material.AIR, 1)) == null, "nothing is nothing");
        expect(r.items.garmentOf(new ItemStack(Material.DIRT, 1)) == null, "dirt is not a garment");
        for (Garment g : Garment.values()) {
            expect(GarmentItems.recipeText(g).length() > 10, g + " has a printable recipe");
        }
    }

    static int registered(BukkitKit.Rig r) {
        int n = 0;
        for (Garment g : Garment.values()) {
            if (Bukkit.getRecipe(new org.bukkit.NamespacedKey(r.plugin, "garment_" + g.id())) != null) {
                n++;
            }
        }
        return n;
    }

    static void recipes() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        r.items.registerRecipes();
        expect(registered(r) == 6, "six recipes registered: " + registered(r));
        for (Garment g : Garment.values()) {
            org.bukkit.inventory.Recipe recipe = Bukkit.getRecipe(new org.bukkit.NamespacedKey(r.plugin, "garment_" + g.id()));
            expect(recipe instanceof ShapedRecipe s && r.items.garmentOf(s.getResult()) == g && s.getShape().length >= 2,
                    "the " + g.title() + " recipe makes a " + g.title());
        }
        r.items.registerRecipes();
        expect(registered(r) == 6, "registering twice does not double them");
        r.items.unregisterRecipes();
        expect(registered(r) == 0, "and they can be removed");
        r.items.unregisterRecipes();
        expect(registered(r) == 0, "removing twice is fine");
    }
}
