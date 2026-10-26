package com.nexuscraft.nexuswardrobe;

import java.util.Locale;

/**
 * How much warmth a worn piece gives. Vanilla armour first: leather keeps a little heat in, metal keeps almost none,
 * and metal also bakes in the sun. A Wardrobe garment (see {@link Garment}) overrides its base material.
 */
public final class Insulation {

    private Insulation() {
    }

    /** What the wearer has on, boiled down to the three numbers the heat model needs. */
    public record Gear(double insulation, double cooling, int metalPieces) {
        public static final Gear NAKED = new Gear(0, 0, 0);
    }

    /** One worn piece: its vanilla material name (upper case), or a garment tag. Either may be null for an empty slot. */
    public record Piece(String material, Garment garment) {
    }

    /** The warmth of a vanilla armour piece by material name, e.g. LEATHER_CHESTPLATE. Unknown things give nothing. */
    public static double warmth(String material) {
        if (material == null) {
            return 0;
        }
        String m = material.toUpperCase(Locale.ROOT);
        int us = m.lastIndexOf('_');
        String kind = us < 0 ? m : m.substring(0, us);
        String part = us < 0 ? "" : m.substring(us + 1);
        double scale = switch (part) {
            case "HELMET" -> 0.7;
            case "CHESTPLATE" -> 1.4;
            case "LEGGINGS" -> 1.0;
            case "BOOTS" -> 0.7;
            default -> 0.0;
        };
        double per = switch (kind) {
            case "LEATHER" -> 0.11;
            case "NETHERITE" -> 0.06;
            case "DIAMOND" -> 0.035;
            case "CHAINMAIL" -> 0.03;
            case "TURTLE" -> 0.03;
            case "IRON" -> 0.025;
            case "GOLDEN" -> 0.02;
            default -> 0.0;
        };
        if (m.equals("ELYTRA")) {
            return 0.05;
        }
        return per * scale;
    }

    /** True for armour that gets hot in the sun. */
    public static boolean conducts(String material) {
        if (material == null) {
            return false;
        }
        String m = material.toUpperCase(Locale.ROOT);
        return m.startsWith("IRON_") || m.startsWith("GOLDEN_") || m.startsWith("DIAMOND_") || m.startsWith("CHAINMAIL_")
                || m.startsWith("NETHERITE_");
    }

    public static Gear of(Piece... pieces) {
        double warm = 0;
        double cool = 0;
        int metal = 0;
        if (pieces != null) {
            for (Piece p : pieces) {
                if (p == null) {
                    continue;
                }
                if (p.garment() != null) {
                    warm += p.garment().warmth();
                    cool += p.garment().cooling();
                } else {
                    warm += warmth(p.material());
                    if (conducts(p.material())) {
                        metal++;
                    }
                }
            }
        }
        return new Gear(warm, cool, metal);
    }
}
