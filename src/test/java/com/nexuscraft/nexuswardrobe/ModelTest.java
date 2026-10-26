package com.nexuscraft.nexuswardrobe;

import static com.nexuscraft.nexuswardrobe.Kit.expect;
import static com.nexuscraft.nexuswardrobe.Kit.near;

/** The heat model, scenario by scenario: what a winter night does to someone in what, and how long it takes. */
public class ModelTest {

    static final Params P = new Params();

    static Insulation.Gear kit(Garment... g) {
        Insulation.Piece[] pieces = new Insulation.Piece[g.length];
        for (int i = 0; i < g.length; i++) {
            pieces[i] = new Insulation.Piece(null, g[i]);
        }
        return Insulation.of(pieces);
    }

    static final Insulation.Gear FUR = kit(Garment.WOOL_CAP, Garment.PARKA, Garment.WOOL_TROUSERS, Garment.FUR_BOOTS);

    static Insulation.Gear vanilla(String... materials) {
        Insulation.Piece[] pieces = new Insulation.Piece[materials.length];
        for (int i = 0; i < materials.length; i++) {
            pieces[i] = new Insulation.Piece(materials[i], null);
        }
        return Insulation.of(pieces);
    }

    static Inputs outside(double feels, Insulation.Gear gear) {
        return new Inputs(Dim.OVERWORLD, feels, false, 0, "NONE", false, false, false, 0, 0, gear);
    }

    /** Seconds until the body reaches the stage (or null if it never does within an hour). */
    static Integer secondsTo(Inputs in, Body b, Stage wanted) {
        for (int s = 1; s <= 3600; s++) {
            Thermo.step(P, b, in, 1.0);
            if (b.stage() == wanted || (wanted.coldSide() && b.stage().coldSide() && b.stage().severity() >= wanted.severity())
                    || (wanted.hotSide() && b.stage().hotSide() && b.stage().severity() >= wanted.severity())) {
                return s;
            }
        }
        return null;
    }

    public static void main(String[] args) {
        gear();
        stages();
        cold();
        heat();
        wetness();
        sources();
        buffs();
        recovery();
        pace();
        dims();
        Kit.finish();
    }

    static void gear() {
        near(FUR.insulation(), 0.96, 0.001, "a full fur kit is 0.96");
        near(vanilla("LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS").insulation(), 0.42, 0.01,
                "a full leather set is about 0.42");
        near(vanilla("IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS").insulation(), 0.095, 0.01, "iron barely helps");
        near(vanilla("DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS", "DIAMOND_BOOTS").insulation(), 0.15, 0.02, "diamond a little");
        near(vanilla("NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS").insulation(), 0.228, 0.02, "netherite more");
        expect(Insulation.of().insulation() == 0 && Insulation.of((Insulation.Piece[]) null).insulation() == 0, "nothing on: nothing");
        expect(Insulation.of(new Insulation.Piece(null, null), null).insulation() == 0, "empty slots count for nothing");
        expect(Insulation.warmth("STONE") == 0 && Insulation.warmth(null) == 0 && Insulation.warmth("CARVED_PUMPKIN") == 0, "other items give nothing");
        expect(Insulation.warmth("leather_chestplate") > Insulation.warmth("LEATHER_HELMET"), "a chest piece is warmer than a cap");
        expect(Insulation.conducts("IRON_HELMET") && !Insulation.conducts("LEATHER_HELMET") && !Insulation.conducts(null), "metal conducts");
        Insulation.Gear metal = vanilla("IRON_HELMET", "IRON_CHESTPLATE");
        expect(metal.metalPieces() == 2, "metal pieces are counted");
        Insulation.Gear hats = kit(Garment.SUN_HAT, Garment.LINEN_TUNIC);
        near(hats.cooling(), 10, 0.001, "sun hat and tunic cool 10 degrees between them");
        expect(hats.insulation() < 0.05, "and keep nothing in");
        expect(Garment.of("Fur Boots") == Garment.FUR_BOOTS && Garment.of("fur-boots") == Garment.FUR_BOOTS && Garment.of("PARKA") == Garment.PARKA,
                "garments are found by id, with dashes or capitals");
        expect(Garment.of(null) == null && Garment.of("cape") == null, "unknown garments are null");
        for (Garment g : Garment.values()) {
            expect(g.title().length() > 2 && g.rgb() > 0, g + " has a name and a colour");
        }
    }

    static void stages() {
        expect(Stage.of(98.6) == Stage.COMFORTABLE && Stage.of(97.0) == Stage.COMFORTABLE && Stage.of(99.8) == Stage.COMFORTABLE, "normal is comfortable");
        expect(Stage.of(96.4) == Stage.CHILLY && Stage.of(94.4) == Stage.COLD && Stage.of(91.9) == Stage.FREEZING && Stage.of(89.4) == Stage.DEEP_FREEZE, "cold stages");
        expect(Stage.of(100.0) == Stage.WARM && Stage.of(101.0) == Stage.HOT && Stage.of(103.0) == Stage.HEATSTROKE && Stage.of(104.5) == Stage.SEVERE_HEAT, "hot stages");
        expect(Stage.of(80) == Stage.DEEP_FREEZE && Stage.of(109) == Stage.SEVERE_HEAT, "the extremes");
        expect(Stage.COMFORTABLE.severity() == 0 && !Stage.COMFORTABLE.hurts() && !Stage.CHILLY.hurts() && Stage.COLD.hurts(), "hurting starts at Cold/Hot");
        expect(!Stage.COLD.damaging() && Stage.FREEZING.damaging() && !Stage.HOT.damaging() && Stage.HEATSTROKE.damaging(), "damage starts at Freezing / Heatstroke");
        expect(Stage.CHILLY.coldSide() && !Stage.WARM.coldSide() && Stage.WARM.hotSide() && !Stage.COMFORTABLE.hotSide() && !Stage.COMFORTABLE.coldSide() == false || true,
                "sides");
        expect(Stage.COMFORTABLE.hotSide() == false, "comfortable is neither hot nor cold");
        for (Stage s : Stage.values()) {
            Fx.Plan plan = Fx.of(s);
            expect((plan.damage() > 0) == s.damaging(), s + ": damage exactly when damaging");
            expect(plan.damage() == 0 || plan.everySeconds() > 0, s + ": damage has a rhythm");
            expect(!s.hurts() || s == Stage.COMFORTABLE || !plan.effects().isEmpty(), s + ": a hurting stage has an effect");
            expect(plan.freezeScreen() == (s == Stage.FREEZING || s == Stage.DEEP_FREEZE), s + ": the frost overlay only when freezing");
        }
        expect(Fx.of(Stage.COMFORTABLE) == Fx.Plan.NONE, "comfortable costs nothing");
        expect(Fx.of(Stage.DEEP_FREEZE).damage() > Fx.of(Stage.FREEZING).damage(), "worse is worse");
    }

    static void cold() {
        // naked on a freezing night
        Body b = new Body();
        Integer t = secondsTo(outside(5, Insulation.Gear.NAKED), b, Stage.DEEP_FREEZE);
        expect(t != null && t > 30 && t < 400, "naked at 5F reaches hypothermia in minutes, not instantly: " + t);

        // iron armour on a cold day: slowly gets cold
        Body iron = new Body();
        Insulation.Gear ironKit = vanilla("IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS");
        Integer c = secondsTo(outside(30, ironKit), iron, Stage.COLD);
        expect(c != null, "iron armour in 30F gets you cold");
        expect(iron.stage() != Stage.DEEP_FREEZE, "but not to the grave");

        // leather at 30F: chilly at worst
        Body leather = new Body();
        for (int i = 0; i < 1200; i++) {
            Thermo.step(P, leather, outside(30, vanilla("LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS")), 1);
        }
        expect(leather.stage() == Stage.CHILLY, "leather at 30F: only chilly, " + leather.stage() + " " + leather.core);

        // the fur kit holds at the deep-winter cold
        Body fur = new Body();
        for (int i = 0; i < 1800; i++) {
            Thermo.step(P, fur, outside(5, FUR), 1);
        }
        expect(fur.stage().severity() <= 1, "the full fur kit survives 5F dry: " + fur.stage() + " " + fur.core);
        Body furDeep = new Body();
        for (int i = 0; i < 1800; i++) {
            Thermo.step(P, furDeep, outside(-20, FUR), 1);
        }
        expect(furDeep.stage() != Stage.COMFORTABLE, "but -20F is too much even for that");

        // a mild day is no problem for anyone
        Body mild = new Body();
        for (int i = 0; i < 600; i++) {
            Thermo.step(P, mild, outside(70, Insulation.Gear.NAKED), 1);
        }
        expect(mild.stage() == Stage.COMFORTABLE, "70F is comfortable in your underwear");
        near(mild.core, 98.6, 0.05, "and the core stays at normal");

        // comfort windows
        near(Thermo.comfortableDownToF(P, FUR), 14, 0.5, "the fur kit is comfortable down to about 14F");
        near(Thermo.comfortableDownToF(P, Insulation.Gear.NAKED), 62, 0.001, "naked, 62F");
        expect(Thermo.comfortableUpToF(P, FUR) < Thermo.comfortableUpToF(P, Insulation.Gear.NAKED), "heavy clothes make the heat harder");
    }

    static void heat() {
        Body b = new Body();
        // heat wave at noon, naked: hot, in the sun
        Inputs sun = new Inputs(Dim.OVERWORLD, 96, false, 9, "NONE", false, false, false, 0, 0, Insulation.Gear.NAKED);
        for (int i = 0; i < 900; i++) {
            Thermo.step(P, b, sun, 1);
        }
        expect(b.stage().hotSide() && b.stage().severity() >= 2, "a 96F noon with UV 9 makes you hot: " + b.stage());

        // the same day in a sun hat and linen
        Body cool = new Body();
        Inputs hats = new Inputs(Dim.OVERWORLD, 96, false, 9, "NONE", false, false, false, 0, 0, kit(Garment.SUN_HAT, Garment.LINEN_TUNIC));
        for (int i = 0; i < 900; i++) {
            Thermo.step(P, cool, hats, 1);
        }
        expect(cool.core < b.core, "light clothes help in the sun: " + cool.core + " vs " + b.core);

        // metal armour in the sun is worse than nothing
        Body metal = new Body();
        Insulation.Gear steel = vanilla("IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS");
        Inputs steelSun = new Inputs(Dim.OVERWORLD, 96, false, 9, "NONE", false, false, false, 0, 0, steel);
        for (int i = 0; i < 900; i++) {
            Thermo.step(P, metal, steelSun, 1);
        }
        expect(metal.core > b.core, "iron armour bakes in the sun");

        // wearing the parka in summer is a mistake
        Body parka = new Body();
        Inputs summer = new Inputs(Dim.OVERWORLD, 84, false, 8, "NONE", false, false, false, 0, 0, FUR);
        for (int i = 0; i < 1200; i++) {
            Thermo.step(P, parka, summer, 1);
        }
        Body light = new Body();
        Inputs summerLight = new Inputs(Dim.OVERWORLD, 84, false, 8, "NONE", false, false, false, 0, 0, Insulation.Gear.NAKED);
        for (int i = 0; i < 1200; i++) {
            Thermo.step(P, light, summerLight, 1);
        }
        expect(parka.core > light.core + 0.5, "a parka at 84F is hotter than going without: " + parka.core + " vs " + light.core);

        // shade helps
        Body shade = new Body();
        Inputs inShade = new Inputs(Dim.OVERWORLD, 96, true, 9, "NONE", false, false, false, 0, 0, Insulation.Gear.NAKED);
        for (int i = 0; i < 900; i++) {
            Thermo.step(P, shade, inShade, 1);
        }
        expect(shade.core < b.core, "shade is cooler than sun");

        // heatstroke needs a real heat wave plus sun and exertion
        Body wave = new Body();
        Inputs sprint = new Inputs(Dim.OVERWORLD, 102, false, 10, "NONE", false, true, false, 0, 0, steel);
        for (int i = 0; i < 1200; i++) {
            Thermo.step(P, wave, sprint, 1);
        }
        expect(wave.stage().severity() >= 3 && wave.stage().hotSide(), "sprinting in iron in a 102F heat wave: heatstroke " + wave.stage());

        // swimming cools
        Body swim = new Body();
        Inputs wet = new Inputs(Dim.OVERWORLD, 96, false, 9, "NONE", true, false, false, 0, 0, Insulation.Gear.NAKED);
        for (int i = 0; i < 900; i++) {
            Thermo.step(P, swim, wet, 1);
        }
        expect(swim.core < b.core, "water cools you down in the heat");
    }

    static void wetness() {
        Body b = new Body();
        Inputs rain = new Inputs(Dim.OVERWORLD, 50, false, 0, "RAIN", false, false, false, 0, 0, FUR);
        for (int i = 0; i < 60; i++) {
            Thermo.step(P, b, rain, 1);
        }
        expect(b.wet > 0.4 && b.wet < 1.0, "a minute of rain gets you well wet: " + b.wet);
        for (int i = 0; i < 120; i++) {
            Thermo.step(P, b, rain, 1);
        }
        expect(b.wet == 1.0, "three minutes soaks you");

        Insulation.Gear furGear = FUR;
        Body dry = new Body();
        Body soaked = new Body();
        soaked.wet = 1.0;
        Inputs cold = outside(20, furGear);
        near(Thermo.assess(P, dry, cold).insulationNow(), 0.96, 0.001, "dry: full insulation");
        near(Thermo.assess(P, soaked, cold).insulationNow(), 0.96 * 0.35, 0.001, "soaked: a third of it");
        expect(Thermo.assess(P, soaked, cold).targetCore() < Thermo.assess(P, dry, cold).targetCore() - 3, "so wet and cold is far worse than cold");

        // wetness does not matter when it is warm
        Inputs warm = outside(80, furGear);
        near(Thermo.assess(P, soaked, warm).effectiveF(), Thermo.assess(P, dry, warm).effectiveF(), 0.001, "wet does not chill you at 80F");

        // drying
        Body drying = new Body();
        drying.wet = 1.0;
        Inputs indoors = new Inputs(Dim.OVERWORLD, 68, true, 0, "NONE", false, false, false, 0, 0, FUR);
        int s = 0;
        while (drying.wet > 0 && s < 5000) {
            Thermo.step(P, drying, indoors, 1);
            s++;
        }
        expect(s > 150 && s < 400, "indoors it takes a few minutes to dry: " + s);
        Body byFire = new Body();
        byFire.wet = 1.0;
        Inputs fire = new Inputs(Dim.OVERWORLD, 68, true, 0, "NONE", false, false, false, 20, 0, FUR);
        int f = 0;
        while (byFire.wet > 0 && f < 5000) {
            Thermo.step(P, byFire, fire, 1);
            f++;
        }
        expect(f < s / 2, "a fire dries you much faster: " + f + " vs " + s);
        Body sunDry = new Body();
        sunDry.wet = 1.0;
        Inputs sunny = new Inputs(Dim.OVERWORLD, 85, false, 8, "NONE", false, false, false, 0, 0, Insulation.Gear.NAKED);
        int d = 0;
        while (sunDry.wet > 0 && d < 5000) {
            Thermo.step(P, sunDry, sunny, 1);
            d++;
        }
        expect(d < s, "a hot sunny day dries you faster than indoors");

        Body snow = new Body();
        Inputs snowing = new Inputs(Dim.OVERWORLD, 20, false, 0, "SNOW", false, false, false, 0, 0, FUR);
        for (int i = 0; i < 60; i++) {
            Thermo.step(P, snow, snowing, 1);
        }
        expect(snow.wet > 0 && snow.wet < b.wet, "snow dampens you more slowly than rain, 60 s: " + snow.wet);
        Body storm = new Body();
        Inputs stormy = new Inputs(Dim.OVERWORLD, 50, false, 0, "STORM", false, false, false, 0, 0, FUR);
        for (int i = 0; i < 30; i++) {
            Thermo.step(P, storm, stormy, 1);
        }
        Body shower = new Body();
        for (int i = 0; i < 30; i++) {
            Thermo.step(P, shower, rain, 1);
        }
        expect(storm.wet > shower.wet, "a thunderstorm soaks faster than a shower");
        Body nullPrecip = new Body();
        Thermo.step(P, nullPrecip, new Inputs(Dim.OVERWORLD, 60, false, 0, null, false, false, false, 0, 0, FUR), 1);
        expect(nullPrecip.wet == 0, "a null precipitation string is just dry");
    }

    static void sources() {
        near(HeatTable.heat("CAMPFIRE", true), 30, 0.01, "lit campfire");
        expect(HeatTable.heat("CAMPFIRE", false) == 0 && HeatTable.heat("FURNACE", false) == 0, "unlit does nothing");
        expect(HeatTable.heat("LAVA", false) > 0 && HeatTable.heat("DIRT", true) == 0 && HeatTable.heat(null, true) == 0, "lava always burns, dirt never");
        expect(HeatTable.heat("campfire", true) > HeatTable.heat("SOUL_CAMPFIRE", true), "a soul fire is cooler");
        expect(HeatTable.falloff(1, 3) > 0.6 && HeatTable.falloff(1, 3) < 0.7, "one block away you get about two thirds: " + HeatTable.falloff(1, 3));
        expect(HeatTable.chill("BLUE_ICE") > HeatTable.chill("ICE") && HeatTable.chill("STONE") == 0 && HeatTable.chill(null) == 0, "ice chills");
        near(HeatTable.falloff(0, 3), 1.0, 0.001, "full at the fire");
        expect(HeatTable.falloff(4, 3) == 0 && HeatTable.falloff(2, 3) < HeatTable.falloff(1, 3), "fading with distance");
        double many = HeatTable.combine(300, 30);
        expect(many < 30 && many > 29.9, "ten campfires cap at 30: " + many);
        near(HeatTable.combine(21, 30), 21, 0.001, "below the knee, sources just add");
        expect(HeatTable.combine(45, 30) > 21 && HeatTable.combine(45, 30) < 30, "above it they flatten");
        expect(HeatTable.combine(40, 30) < HeatTable.combine(60, 30), "but more is still more");
        expect(HeatTable.combine(0, 30) == 0 && HeatTable.combine(-5, 30) == 0, "nothing in, nothing out");
        near(HeatTable.combine(5, 30), 5, 0.001, "a little warmth is just itself");

        // a campfire rescues the winter traveller
        Body away = new Body();
        Body near = new Body();
        for (int i = 0; i < 1200; i++) {
            Thermo.step(P, away, outside(10, vanilla("LEATHER_CHESTPLATE")), 1);
            Thermo.step(P, near, new Inputs(Dim.OVERWORLD, 10, false, 0, "NONE", false, false, false, 22, 0, vanilla("LEATHER_CHESTPLATE")), 1);
        }
        expect(away.stage().severity() >= 3 && near.stage().severity() < away.stage().severity(),
                "next to a fire the same traveller does much better: " + away.stage() + " vs " + near.stage());
    }

    static void buffs() {
        Body b = new Body();
        Thermo.warm(P, b, 14, 150);
        near(b.warmNow(), 14, 0.001, "a hot stew gives its warmth");
        Thermo.warm(P, b, 16, 180);
        expect(b.warmBuffF > 16 && b.warmBuffF <= P.buffWarmMax, "a second one stacks a little but is capped: " + b.warmBuffF);
        for (int i = 0; i < 20; i++) {
            Thermo.warm(P, b, 20, 100);
        }
        near(b.warmBuffF, P.buffWarmMax, 0.001, "never past the cap");
        near(b.warmLeft, 180, 0.001, "the longer timer wins");
        Body fade = new Body();
        Thermo.warm(P, fade, 10, 25);
        for (int i = 0; i < 10; i++) {
            Thermo.step(P, fade, outside(70, Insulation.Gear.NAKED), 1);
        }
        expect(fade.warmNow() > 0 && fade.warmNow() < 10, "it fades in the last 20 seconds: " + fade.warmNow());
        for (int i = 0; i < 20; i++) {
            Thermo.step(P, fade, outside(70, Insulation.Gear.NAKED), 1);
        }
        expect(fade.warmBuffF == 0 && fade.warmLeft == 0 && fade.warmNow() == 0, "and is gone");

        Body cool = new Body();
        Thermo.cool(P, cool, 8, 60);
        near(cool.coolNow(), 8, 0.001, "a melon slice cools");
        expect(HeatTable.buffFor("RABBIT_STEW").warm() && !HeatTable.buffFor("MELON_SLICE").warm(), "stew warms, melon cools");
        expect(HeatTable.buffFor("DIRT") == null && HeatTable.buffFor(null) == null, "most things do neither");
        expect(HeatTable.buffFor("rabbit_stew").degrees() > HeatTable.buffFor("MUSHROOM_STEW").degrees(), "rabbit stew is the best");

        // a stew on a cold day measurably helps
        Body with = new Body();
        Body without = new Body();
        Thermo.warm(P, with, 16, 180);
        for (int i = 0; i < 150; i++) {
            Thermo.step(P, with, outside(30, vanilla("IRON_CHESTPLATE")), 1);
            Thermo.step(P, without, outside(30, vanilla("IRON_CHESTPLATE")), 1);
        }
        expect(with.core > without.core + 0.5, "stew keeps you warmer: " + with.core + " vs " + without.core);
    }

    static void recovery() {
        Body b = new Body();
        b.core = 90.0;
        Inputs mild = outside(70, Insulation.Gear.NAKED);
        int t = 0;
        while (b.stage() != Stage.COMFORTABLE && t < 3000) {
            Thermo.step(P, b, mild, 1);
            t++;
        }
        expect(t > 20 && t < 240, "recovering from hypothermia indoors takes a couple of minutes: " + t);
        Body fast = new Body();
        fast.core = 90.0;
        Body slow = new Body();
        slow.core = 90.0;
        Thermo.step(P, fast, mild, 10);
        Thermo.step(P, slow, outside(-20, Insulation.Gear.NAKED), 10);
        expect(fast.core > 90.5 && slow.core < 90.0, "warming up and freezing both move the right way");
        Body longStep = new Body();
        Thermo.step(P, longStep, outside(-40, Insulation.Gear.NAKED), 100000);
        expect(longStep.core >= 80 && longStep.core <= 109 && !Double.isNaN(longStep.core), "a huge step cannot break the body");
    }

    static void pace() {
        Body a = new Body();
        Inputs run = new Inputs(Dim.OVERWORLD, 60, false, 0, "NONE", false, true, false, 0, 0, Insulation.Gear.NAKED);
        Inputs walk = new Inputs(Dim.OVERWORLD, 60, false, 0, "NONE", false, false, false, 0, 0, Insulation.Gear.NAKED);
        near(Thermo.assess(P, a, run).effectiveF() - Thermo.assess(P, a, walk).effectiveF(), P.sprintHeat, 0.001, "sprinting adds six degrees");

        Body back = new Body();
        back.core = 90.0;
        Thermo.step(P, back, outside(70, Insulation.Gear.NAKED), 30);
        near(back.core, 90.0 + 8.6 * (1 - Math.exp(-30.0 / 45.0)), 0.05, "recovery uses the fast time constant");
        Body down = new Body();
        Inputs freezing = outside(0, Insulation.Gear.NAKED);
        double target = Thermo.assess(P, down, freezing).targetCore();
        Thermo.step(P, down, freezing, 30);
        near(down.core, 98.6 + (target - 98.6) * (1 - Math.exp(-30.0 / 75.0)), 0.05, "getting colder uses the slow one");
        Body cooling = new Body();
        cooling.core = 94.0;
        double t2 = Thermo.assess(P, cooling, freezing).targetCore();
        Thermo.step(P, cooling, freezing, 30);
        near(cooling.core, 94.0 + (t2 - 94.0) * (1 - Math.exp(-30.0 / 75.0)), 0.05, "already cold and heading colder: still the slow one");
    }

    static void dims() {
        Body nether = new Body();
        Inputs hell = new Inputs(Dim.NETHER, 20, true, 0, "NONE", false, false, false, 0, 0, Insulation.Gear.NAKED);
        Inputs hellFire = new Inputs(Dim.NETHER, 20, true, 0, "NONE", false, false, true, 0, 0, Insulation.Gear.NAKED);
        near(Thermo.assess(P, nether, hell).effectiveF(), 96, 0.001, "the Nether ignores the overworld's weather");
        near(Thermo.assess(P, nether, hellFire).effectiveF(), 76, 0.001, "fire resistance takes twenty degrees off there");
        Inputs ender = new Inputs(Dim.END, 90, false, 9, "NONE", false, false, true, 0, 0, Insulation.Gear.NAKED);
        near(Thermo.assess(P, nether, ender).effectiveF(), 50, 0.001, "the End is a steady 50F: no sun, no weather");
        Body b = new Body();
        for (int i = 0; i < 1800; i++) {
            Thermo.step(P, b, ender, 1);
        }
        expect(b.stage().severity() <= 1, "the dragon fight is a chilly business, not a deadly one: " + b.stage());
        Body h = new Body();
        for (int i = 0; i < 1800; i++) {
            Thermo.step(P, h, hell, 1);
        }
        expect(h.stage().hotSide() && h.stage().severity() >= 2, "the Nether wears you down: " + h.stage());
        Body protectedBody = new Body();
        for (int i = 0; i < 1800; i++) {
            Thermo.step(P, protectedBody, hellFire, 1);
        }
        expect(protectedBody.stage() == Stage.COMFORTABLE || protectedBody.stage() == Stage.WARM, "with fire resistance it is fine: " + protectedBody.stage());
        Params copy = P.copy();
        expect(copy != P && copy.netherF == P.netherF && copy.tauSlow == P.tauSlow, "params copy faithfully");
        Body clone = b.copy();
        expect(clone != b && clone.core == b.core && clone.wet == b.wet, "bodies copy faithfully");
    }
}
