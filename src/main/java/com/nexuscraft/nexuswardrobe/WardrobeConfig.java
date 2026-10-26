package com.nexuscraft.nexuswardrobe;

import org.bukkit.configuration.file.FileConfiguration;

/** Every setting, read once from config.yml. A reload builds a new one. */
public final class WardrobeConfig {

    public final boolean enabled;
    public final boolean celsius;
    public final boolean actionbar;
    public final boolean messages;
    public final boolean damage;
    public final boolean canKill;
    public final double intenseMultiplier;
    public final boolean respectComfort;
    public final boolean respectPath;
    public final boolean takeOverHeat;
    public final int sourceRadius;
    public final int scanEverySeconds;
    public final boolean recipes;
    public final Params params;

    public WardrobeConfig(FileConfiguration c) {
        this.enabled = c.getBoolean("enabled", true);
        this.celsius = "C".equalsIgnoreCase(c.getString("units", "F"));
        this.actionbar = c.getBoolean("actionbar", true);
        this.messages = c.getBoolean("messages", true);
        this.damage = c.getBoolean("damage.enabled", true);
        this.canKill = c.getBoolean("damage.can-kill", true);
        this.intenseMultiplier = Math.max(0.0, c.getDouble("damage.intense-multiplier", 1.25));
        this.respectComfort = c.getBoolean("respect-comfort", true);
        this.respectPath = c.getBoolean("respect-path", true);
        this.takeOverHeat = c.getBoolean("climate.take-over-heat-exhaustion", true);
        Params p = new Params();
        p.netherF = c.getDouble("dimensions.nether-f", 96);
        p.endF = c.getDouble("dimensions.end-f", 50);
        this.sourceRadius = Math.max(1, Math.min(6, c.getInt("sources.radius", 3)));
        this.scanEverySeconds = Math.max(1, c.getInt("sources.scan-every-seconds", 2));
        this.recipes = c.getBoolean("garments.recipes", true);
        p.comfortLowBase = c.getDouble("model.comfort-low-f", 62);
        p.comfortHighBase = c.getDouble("model.comfort-high-f", 84);
        p.insulationCold = c.getDouble("model.insulation-cold", 50);
        p.insulationHeat = c.getDouble("model.insulation-heat", 14);
        p.wetInsulationLoss = c.getDouble("model.wet-insulation-loss", 0.65);
        p.wetChillBelow = c.getDouble("model.wet-chill-below-f", 60);
        p.wetChill = c.getDouble("model.wet-chill-f", 10);
        p.coldGain = c.getDouble("model.cold-gain", 0.22);
        p.heatGain = c.getDouble("model.heat-gain", 0.19);
        p.sunPerUv = c.getDouble("model.sun-per-uv", 0.6);
        p.metalSun = c.getDouble("model.metal-sun-f", 1.5);
        p.sprintHeat = c.getDouble("model.sprint-heat-f", 6);
        p.waterChill = c.getDouble("model.water-chill-f", 14);
        p.fireResistRelief = c.getDouble("model.fire-resistance-relief-f", 20);
        p.tauSlow = c.getDouble("model.tau-slow-seconds", 75);
        p.tauFast = c.getDouble("model.tau-fast-seconds", 45);
        p.rainWet = c.getDouble("model.rain-wet-per-second", 0.012);
        p.stormWet = c.getDouble("model.storm-wet-per-second", 0.02);
        p.snowWet = c.getDouble("model.snow-wet-per-second", 0.005);
        p.dryRate = c.getDouble("model.dry-per-second", 0.004);
        p.dryNearFire = c.getDouble("model.dry-near-fire-per-second", 0.02);
        p.drySun = c.getDouble("model.dry-in-sun-per-second", 0.006);
        p.warmthMax = c.getDouble("model.fire-cap-f", 30);
        p.buffWarmMax = c.getDouble("model.warm-drink-cap-f", 20);
        p.buffCoolMax = c.getDouble("model.cool-drink-cap-f", 15);
        this.params = p;
    }

    /** Degrees F to what the player sees. */
    public String degrees(double f) {
        return celsius ? Math.round((f - 32.0) * 5.0 / 9.0) + "\u00b0C" : Math.round(f) + "\u00b0F";
    }

    /** A core temperature to one decimal place, in the player's units. */
    public String core(double f) {
        double v = celsius ? (f - 32.0) * 5.0 / 9.0 : f;
        return (Math.round(v * 10.0) / 10.0) + (celsius ? "\u00b0C" : "\u00b0F");
    }
}
