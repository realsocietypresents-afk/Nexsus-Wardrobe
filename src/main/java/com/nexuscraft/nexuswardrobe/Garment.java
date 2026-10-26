package com.nexuscraft.nexuswardrobe;

import java.util.Locale;

/** The clothes this plugin adds. Each is a dyed leather piece with a hidden tag, so the vanilla armour slot is all it needs. */
public enum Garment {

    WOOL_CAP("wool_cap", "Wool Cap", Slot.HEAD, 0.12, 0, 0xE8E2D0),
    PARKA("parka", "Parka", Slot.CHEST, 0.40, 0, 0x5B6B4A),
    WOOL_TROUSERS("wool_trousers", "Wool Trousers", Slot.LEGS, 0.26, 0, 0x8A7D6B),
    FUR_BOOTS("fur_boots", "Fur-Lined Boots", Slot.FEET, 0.18, 0, 0x6B4A2E),
    SUN_HAT("sun_hat", "Sun Hat", Slot.HEAD, 0.0, 5, 0xE6CF7A),
    LINEN_TUNIC("linen_tunic", "Linen Tunic", Slot.CHEST, 0.02, 5, 0xF1EBDD);

    /** Which armour slot a piece sits in. */
    public enum Slot { HEAD, CHEST, LEGS, FEET }

    private final String id;
    private final String title;
    private final Slot slot;
    private final double warmth;
    private final double cooling;
    private final int rgb;

    Garment(String id, String title, Slot slot, double warmth, double cooling, int rgb) {
        this.id = id;
        this.title = title;
        this.slot = slot;
        this.warmth = warmth;
        this.cooling = cooling;
        this.rgb = rgb;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public Slot slot() {
        return slot;
    }

    /** Insulation added to the wearer (0 to about 0.4 a piece). */
    public double warmth() {
        return warmth;
    }

    /** Degrees of sun-heat this piece takes off when worn outdoors in the sun. */
    public double cooling() {
        return cooling;
    }

    public int rgb() {
        return rgb;
    }

    public static Garment of(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (Garment g : values()) {
            if (g.id.equals(t) || g.name().toLowerCase(Locale.ROOT).equals(t)) {
                return g;
            }
        }
        return null;
    }
}
