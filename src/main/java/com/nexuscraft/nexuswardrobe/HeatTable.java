package com.nexuscraft.nexuswardrobe;

import java.util.Locale;

/** What each kind of block gives off, as degrees of warmth at point-blank range. */
public final class HeatTable {

    private HeatTable() {
    }

    /** Degrees of warmth from a block, or 0. Fires only count when lit. */
    public static double heat(String material, boolean lit) {
        if (material == null) {
            return 0;
        }
        return switch (material.toUpperCase(Locale.ROOT)) {
            case "CAMPFIRE" -> lit ? 30 : 0;
            case "SOUL_CAMPFIRE" -> lit ? 22 : 0;
            case "FURNACE", "BLAST_FURNACE", "SMOKER" -> lit ? 16 : 0;
            case "FIRE" -> 14;
            case "SOUL_FIRE" -> 10;
            case "LAVA" -> 24;
            case "MAGMA_BLOCK" -> 10;
            case "LAVA_CAULDRON" -> 14;
            case "TORCH", "WALL_TORCH", "LANTERN" -> 2.5;
            case "SOUL_TORCH", "SOUL_WALL_TORCH", "SOUL_LANTERN" -> 1.5;
            default -> 0;
        };
    }

    /** Degrees of coolness from a block, or 0. */
    public static double chill(String material) {
        if (material == null) {
            return 0;
        }
        return switch (material.toUpperCase(Locale.ROOT)) {
            case "ICE", "PACKED_ICE" -> 4;
            case "BLUE_ICE" -> 6;
            case "SNOW_BLOCK", "POWDER_SNOW" -> 3;
            default -> 0;
        };
    }

    /** A block's pull fades with distance: full at 0, nothing at the edge of the reach. */
    public static double falloff(double distance, double reach) {
        if (distance >= reach + 1) {
            return 0;
        }
        double f = 1.0 - distance / (reach + 1);
        return f * Math.sqrt(f);
    }

    /** Several sources add up, but flatten out toward the cap, so a field of campfires is not a sauna. Smooth, and exact below 70% of the cap. */
    public static double combine(double sum, double cap) {
        if (sum <= 0) {
            return 0;
        }
        double knee = cap * 0.7;
        if (sum <= knee) {
            return sum;
        }
        double room = cap - knee;
        return cap - room * Math.exp(-(sum - knee) / room);
    }

    /** Warm and cool drinks and food: degrees and seconds. */
    public record Buff(double degrees, double seconds, boolean warm) {
    }

    public static Buff buffFor(String material) {
        if (material == null) {
            return null;
        }
        return switch (material.toUpperCase(Locale.ROOT)) {
            case "MUSHROOM_STEW" -> new Buff(14, 150, true);
            case "RABBIT_STEW" -> new Buff(16, 180, true);
            case "BEETROOT_SOUP" -> new Buff(12, 150, true);
            case "SUSPICIOUS_STEW" -> new Buff(10, 120, true);
            case "BAKED_POTATO" -> new Buff(6, 90, true);
            case "COOKED_BEEF", "COOKED_PORKCHOP", "COOKED_MUTTON", "COOKED_CHICKEN", "COOKED_RABBIT" -> new Buff(5, 90, true);
            case "COOKED_COD", "COOKED_SALMON" -> new Buff(4, 75, true);
            case "HONEY_BOTTLE" -> new Buff(8, 120, true);
            case "PUMPKIN_PIE" -> new Buff(6, 90, true);
            case "MELON_SLICE" -> new Buff(8, 60, false);
            case "SWEET_BERRIES", "GLOW_BERRIES" -> new Buff(3, 45, false);
            case "APPLE" -> new Buff(3, 45, false);
            case "MILK_BUCKET" -> new Buff(4, 45, false);
            default -> null;
        };
    }
}
