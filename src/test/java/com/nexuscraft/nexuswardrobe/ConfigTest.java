package com.nexuscraft.nexuswardrobe;

import org.bukkit.configuration.file.YamlConfiguration;

import static com.nexuscraft.nexuswardrobe.Kit.expect;
import static com.nexuscraft.nexuswardrobe.Kit.near;

/** Defaults, overrides, units. */
public class ConfigTest {

    public static void main(String[] args) {
        WardrobeConfig d = new WardrobeConfig(new YamlConfiguration());
        Params p = new Params();
        expect(d.enabled && d.actionbar && d.messages && d.damage && d.canKill && d.respectComfort && d.respectPath && d.takeOverHeat && d.recipes && !d.celsius,
                "the defaults are on, hardcore, and Fahrenheit");
        near(d.intenseMultiplier, 1.25, 0.001, "intense multiplier");
        expect(d.sourceRadius == 3 && d.scanEverySeconds == 2, "scan settings");
        near(d.params.comfortLowBase, p.comfortLowBase, 0.0001, "model default: low");
        near(d.params.tauSlow, p.tauSlow, 0.0001, "model default: tau");
        near(d.params.netherF, 96, 0.0001, "nether");
        near(d.params.endF, 50, 0.0001, "end");
        near(d.params.dryRate, p.dryRate, 0.0000001, "dry");
        near(d.params.warmthMax, 30, 0.0001, "fire cap");

        YamlConfiguration y = new YamlConfiguration();
        y.set("units", "c");
        y.set("model.comfort-low-f", 50.0);
        y.set("dimensions.nether-f", 120.0);
        y.set("sources.radius", 99);
        y.set("sources.scan-every-seconds", 0);
        y.set("damage.intense-multiplier", -4.0);
        y.set("garments.recipes", false);
        WardrobeConfig c = new WardrobeConfig(y);
        expect(c.celsius, "units: c");
        near(c.params.comfortLowBase, 50, 0.0001, "model override");
        near(c.params.netherF, 120, 0.0001, "nether override");
        expect(c.sourceRadius == 6, "the scan radius is capped at 6: " + c.sourceRadius);
        expect(c.scanEverySeconds == 1, "and scans at least once a second");
        near(c.intenseMultiplier, 0, 0.0001, "a negative multiplier becomes none");
        expect(!c.recipes, "recipes can be off");

        expect(d.degrees(98.6).equals("99°F") && c.degrees(212).equals("100°C") && c.degrees(32).equals("0°C"), "degrees convert");
        expect(d.core(98.64).equals("98.6°F") && c.core(98.6).equals("37.0°C"), "core temperatures show a decimal: " + c.core(98.6));
        Kit.finish();
    }
}
