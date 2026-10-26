package com.nexuscraft.nexuswardrobe;

import java.util.List;

/** What a stage does to you. Kept as plain data; the plugin turns {@link Kind} into real potion effects. */
public final class Fx {

    public enum Kind { SLOWNESS, WEAKNESS, MINING_FATIGUE, NAUSEA, BLINDNESS, HUNGER }

    public record Effect(Kind kind, int amplifier) {
    }

    /**
     * @param effects      potion effects held while in the stage
     * @param damage       health lost each time damage fires (before the preset multiplier), 0 for none
     * @param everySeconds seconds between damage ticks
     * @param exhaustion   extra hunger exhaustion per second
     * @param freezeScreen whether to show the frozen-screen overlay
     */
    public record Plan(List<Effect> effects, double damage, int everySeconds, double exhaustion, boolean freezeScreen) {
        public static final Plan NONE = new Plan(List.of(), 0, 0, 0, false);
    }

    private Fx() {
    }

    public static Plan of(Stage stage) {
        return switch (stage) {
            case CHILLY -> new Plan(List.of(), 0, 0, 0.01, false);
            case COLD -> new Plan(List.of(new Effect(Kind.SLOWNESS, 0)), 0, 0, 0.04, false);
            case FREEZING -> new Plan(List.of(new Effect(Kind.SLOWNESS, 1), new Effect(Kind.WEAKNESS, 0),
                    new Effect(Kind.MINING_FATIGUE, 0)), 1.0, 5, 0.08, true);
            case DEEP_FREEZE -> new Plan(List.of(new Effect(Kind.SLOWNESS, 2), new Effect(Kind.WEAKNESS, 1),
                    new Effect(Kind.MINING_FATIGUE, 1), new Effect(Kind.BLINDNESS, 0)), 2.0, 3, 0.12, true);
            case WARM -> new Plan(List.of(), 0, 0, 0.01, false);
            case HOT -> new Plan(List.of(new Effect(Kind.HUNGER, 0)), 0, 0, 0.03, false);
            case HEATSTROKE -> new Plan(List.of(new Effect(Kind.WEAKNESS, 0), new Effect(Kind.SLOWNESS, 0),
                    new Effect(Kind.NAUSEA, 0)), 1.0, 5, 0.06, false);
            case SEVERE_HEAT -> new Plan(List.of(new Effect(Kind.WEAKNESS, 1), new Effect(Kind.SLOWNESS, 1),
                    new Effect(Kind.NAUSEA, 0), new Effect(Kind.BLINDNESS, 0)), 2.0, 3, 0.1, false);
            default -> Plan.NONE;
        };
    }
}
