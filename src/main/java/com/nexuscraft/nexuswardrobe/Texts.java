package com.nexuscraft.nexuswardrobe;

/** Everything the plugin says. */
final class Texts {

    private Texts() {
    }

    /** The chat line when the body moves between stages, or null for none. */
    static String transition(Stage from, Stage to, double wet) {
        if (to == Stage.COMFORTABLE) {
            return from == Stage.COMFORTABLE ? null : "§aYou feel like yourself again.";
        }
        return switch (to) {
            case CHILLY -> from.severity() > to.severity() && from.coldSide() ? "§bThe worst of the cold has passed." : "§bYou're getting chilly.";
            case COLD -> from.severity() > to.severity() && from.coldSide() ? "§3Still cold, but better." : "§3You're cold. Your fingers are going stiff.";
            case FREEZING -> from.severity() > to.severity() && from.coldSide() ? "§bYou're not frozen solid any more." : "§b§lYou're freezing! §7Get warm: a fire, warmer clothes, a hot stew."
                    + (wet > 0.4 ? " §9And get dry." : "");
            case DEEP_FREEZE -> "§9§lHypothermia! §7You are freezing to death. Get to a fire NOW.";
            case WARM -> from.hotSide() && from.severity() > to.severity() ? "§eYou're cooling off a little." : "§eYou're warm.";
            case HOT -> from.hotSide() && from.severity() > to.severity() ? "§6Still hot, but the worst is over." : "§6You're overheating. Find shade, water or a cool drink.";
            case HEATSTROKE -> from.hotSide() && from.severity() > to.severity() ? "§cThe heatstroke is easing." : "§c§lHeatstroke! §7Get out of the sun, strip off the heavy clothes, drink something cool.";
            case SEVERE_HEAT -> "§4§lSevere heatstroke! §7You are cooking. Water, shade, NOW.";
            default -> null;
        };
    }

    /** One line of what to do about it. */
    static String advice(Stage stage, double wet, Inputs in) {
        if (stage.coldSide()) {
            StringBuilder s = new StringBuilder("§7What helps: ");
            if (wet > 0.4) {
                s.append("get out of the wet (stand by a fire or under a roof), ");
            }
            s.append("stand near a campfire or furnace, put on warmer clothes (§f/wardrobe§7), eat a hot stew.");
            return s.toString();
        }
        return "§7What helps: get into shade, take off heavy clothes, swim, eat a melon slice, wear a Sun Hat or Linen Tunic (§f/wardrobe§7).";
    }

    /** The action bar line, or null when everything is fine. */
    static String actionBar(WardrobeConfig c, Body b, Stage stage, Thermo.Report r) {
        boolean damp = b.wet >= 0.5;
        if (stage == Stage.COMFORTABLE && !damp) {
            return null;
        }
        StringBuilder s = new StringBuilder();
        if (stage != Stage.COMFORTABLE) {
            s.append(stage.color()).append(stage.label()).append(" §f").append(c.core(b.core));
        }
        if (damp) {
            if (s.length() > 0) {
                s.append(" §7| ");
            }
            s.append(b.wet >= 0.85 ? "§9Soaked " : "§9Damp ").append(Math.round(b.wet * 100)).append('%');
        }
        if (b.warmNow() > 0.5) {
            s.append(" §7| §6Warm food");
        }
        if (b.coolNow() > 0.5) {
            s.append(" §7| §bCool treat");
        }
        return s.toString();
    }
}
