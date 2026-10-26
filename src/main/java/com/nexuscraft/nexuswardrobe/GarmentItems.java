package com.nexuscraft.nexuswardrobe;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The clothes as items: dyed leather armour carrying a hidden tag, and their crafting recipes. */
public final class GarmentItems {

    private final JavaPlugin plugin;
    private final WardrobeKeys keys;
    private final List<NamespacedKey> recipeKeys = new ArrayList<>();

    public GarmentItems(JavaPlugin plugin, WardrobeKeys keys) {
        this.plugin = plugin;
        this.keys = keys;
    }

    static Material base(Garment g) {
        return switch (g.slot()) {
            case HEAD -> Material.LEATHER_HELMET;
            case CHEST -> Material.LEATHER_CHESTPLATE;
            case LEGS -> Material.LEATHER_LEGGINGS;
            case FEET -> Material.LEATHER_BOOTS;
        };
    }

    public ItemStack create(Garment g) {
        ItemStack item = new ItemStack(base(g), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName("§f" + g.title());
        List<String> lore = new ArrayList<>();
        if (g.warmth() > 0.05) {
            lore.add("§7Keeps the cold out. §fWarmth " + Math.round(g.warmth() * 100));
        }
        if (g.cooling() > 0) {
            lore.add("§7Keeps the sun off. §fCooling " + Math.round(g.cooling()));
        }
        lore.add("§8NexusWardrobe");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(keys.garment, WardrobeKeys.STRING, g.id());
        if (meta instanceof LeatherArmorMeta leather) {
            leather.setColor(Color.fromRGB(g.rgb()));
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Which garment this item is, or null if it is not one of ours. */
    public Garment garmentOf(ItemStack item) {
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return Garment.of(meta.getPersistentDataContainer().get(keys.garment, WardrobeKeys.STRING));
    }

    /** The model's view of one worn item (null for an empty slot). */
    public Insulation.Piece pieceOf(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        return new Insulation.Piece(item.getType().name().toUpperCase(Locale.ROOT), garmentOf(item));
    }

    // ---- recipes -----------------------------------------------------------------------------------------------

    private static RecipeChoice anyWool() {
        List<Material> wools = new ArrayList<>();
        for (Material m : Material.values()) {
            if (m.name().endsWith("_WOOL")) {
                wools.add(m);
            }
        }
        return new RecipeChoice.MaterialChoice(wools);
    }

    /** What each garment is made of, as the printable recipe. */
    public static String recipeText(Garment g) {
        return switch (g) {
            case WOOL_CAP -> "5 wool in a helmet shape (any colour)";
            case PARKA -> "6 wool and 2 leather: leather on the top corners, wool across the middle and bottom rows";
            case WOOL_TROUSERS -> "7 wool in a leggings shape (any colour)";
            case FUR_BOOTS -> "2 wool above 2 rabbit hide, in a boots shape";
            case SUN_HAT -> "5 wheat in a helmet shape";
            case LINEN_TUNIC -> "8 string in a chestplate shape";
        };
    }

    public void registerRecipes() {
        unregisterRecipes();
        RecipeChoice wool = anyWool();
        add(Garment.WOOL_CAP, r -> {
            r.shape("WWW", "W W");
            r.setIngredient('W', wool);
        });
        add(Garment.PARKA, r -> {
            r.shape("L L", "WWW", "WWW");
            r.setIngredient('L', Material.LEATHER);
            r.setIngredient('W', wool);
        });
        add(Garment.WOOL_TROUSERS, r -> {
            r.shape("WWW", "W W", "W W");
            r.setIngredient('W', wool);
        });
        add(Garment.FUR_BOOTS, r -> {
            r.shape("W W", "H H");
            r.setIngredient('W', wool);
            r.setIngredient('H', Material.RABBIT_HIDE);
        });
        add(Garment.SUN_HAT, r -> {
            r.shape("WWW", "W W");
            r.setIngredient('W', Material.WHEAT);
        });
        add(Garment.LINEN_TUNIC, r -> {
            r.shape("S S", "SSS", "SSS");
            r.setIngredient('S', Material.STRING);
        });
    }

    private void add(Garment g, java.util.function.Consumer<ShapedRecipe> shape) {
        NamespacedKey key = new NamespacedKey(plugin, "garment_" + g.id());
        ShapedRecipe recipe = new ShapedRecipe(key, create(g));
        shape.accept(recipe);
        if (Bukkit.addRecipe(recipe)) {
            recipeKeys.add(key);
        }
    }

    public void unregisterRecipes() {
        for (NamespacedKey key : recipeKeys) {
            Bukkit.removeRecipe(key);
        }
        recipeKeys.clear();
    }
}
