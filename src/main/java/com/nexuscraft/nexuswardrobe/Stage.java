package com.nexuscraft.nexuswardrobe;

/** How the body is doing, from frozen to cooked. */
public enum Stage {

    DEEP_FREEZE("Hypothermia", "§9", true, 4),
    FREEZING("Freezing", "§b", true, 3),
    COLD("Cold", "§3", true, 2),
    CHILLY("Chilly", "§b", true, 1),
    COMFORTABLE("Comfortable", "§a", false, 0),
    WARM("Warm", "§e", false, 1),
    HOT("Hot", "§6", false, 2),
    HEATSTROKE("Heatstroke", "§c", false, 3),
    SEVERE_HEAT("Severe heatstroke", "§4", false, 4);

    private final String label;
    private final String color;
    private final boolean coldSide;
    private final int severity;

    Stage(String label, String color, boolean coldSide, int severity) {
        this.label = label;
        this.color = color;
        this.coldSide = coldSide;
        this.severity = severity;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }

    public boolean coldSide() {
        return coldSide;
    }

    public boolean hotSide() {
        return !coldSide && severity > 0;
    }

    /** 0 comfortable, 4 worst. */
    public int severity() {
        return severity;
    }

    /** True from COLD / HOT upward: the stages that start to cost you something. */
    public boolean hurts() {
        return severity >= 2;
    }

    public boolean damaging() {
        return severity >= 3;
    }

    /** Core temperature in degrees F to stage. Boundaries are inclusive on the comfortable side. */
    public static Stage of(double coreF) {
        if (coreF < 89.5) return DEEP_FREEZE;
        if (coreF < 92.0) return FREEZING;
        if (coreF < 94.5) return COLD;
        if (coreF < 96.5) return CHILLY;
        if (coreF <= 99.8) return COMFORTABLE;
        if (coreF <= 100.8) return WARM;
        if (coreF <= 102.0) return HOT;
        if (coreF <= 104.0) return HEATSTROKE;
        return SEVERE_HEAT;
    }
}
