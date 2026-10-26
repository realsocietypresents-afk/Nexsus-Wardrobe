package com.nexuscraft.nexuswardrobe;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static com.nexuscraft.nexuswardrobe.Kit.expect;
import static com.nexuscraft.nexuswardrobe.Kit.near;

/** The once-a-second tick on a pretend server: cold, heat, effects, damage, and everyone who is spared. */
public class ServiceTest {

    public static void main(String[] args) {
        coldNight();
        gearHelps();
        heatWave();
        opOuts();
        damageRules();
        otherWorlds();
        sources();
        weatherSources();
        food();
        persistence();
        clockJumps();
        Kit.finish();
    }

    static void dress(BukkitKit.TestPlayer p, GarmentItems items) {
        p.getInventory().setHelmet(items.create(Garment.WOOL_CAP));
        p.getInventory().setChestplate(items.create(Garment.PARKA));
        p.getInventory().setLeggings(items.create(Garment.WOOL_TROUSERS));
        p.getInventory().setBoots(items.create(Garment.FUR_BOOTS));
    }

    static void coldNight() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        r.seasons.feels = 5;
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        r.seconds(1);
        expect(r.service.stage(p.getUniqueId()) == Stage.COMFORTABLE, "the first second changes nothing much");
        expect(p.lastActionBar == null, "and says nothing while comfortable");
        r.seconds(60);
        Stage s1 = r.service.stage(p.getUniqueId());
        expect(s1.coldSide() && s1.severity() >= 1, "a minute naked at 5F: cold side: " + s1);
        expect(p.text().contains("chilly") || p.text().contains("cold"), "you were told: " + p.text());
        expect(p.lastActionBar != null && Kit.strip(p.lastActionBar).contains("°F"), "and the action bar shows your temperature: " + p.lastActionBar);
        expect(p.has(PotionEffectType.SLOWNESS), "being cold slows you");
        r.seconds(200);
        expect(r.service.stage(p.getUniqueId()) == Stage.DEEP_FREEZE, "a few minutes on: hypothermia");
        expect(p.has(PotionEffectType.BLINDNESS) && p.has(PotionEffectType.WEAKNESS) && p.amp(PotionEffectType.SLOWNESS) == 2, "with the full set of effects");
        expect(p.getFreezeTicks() > 100 && p.getFreezeTicks() < 140, "and the frost overlay, below the level where vanilla also hurts: " + p.getFreezeTicks());
        expect(p.getHealth() < 20, "it hurts: " + p.getHealth());
        expect(p.getExhaustion() > 1, "and burns food: " + p.getExhaustion());
        expect(p.text().contains("Hypothermia") && p.text().contains("What helps"), "you were told what to do about it");
        int adviceCount = p.text().split("What helps", -1).length - 1;
        expect(adviceCount <= 5, "the advice is not spammed: " + adviceCount);
        BodyService.Cause c = r.service.lastCause(p.getUniqueId());
        expect(c != null && c.cold(), "the cause is remembered as cold");

        // warm up again
        r.seasons.feels = 70;
        r.seconds(400);
        expect(r.service.stage(p.getUniqueId()) == Stage.COMFORTABLE, "back indoors you recover: " + r.service.stage(p.getUniqueId()));
        expect(p.text().contains("like yourself again"), "and are told");
        r.seconds(5);
        expect(p.lastActionBar == null || p.lastActionBar.isEmpty() || !Kit.strip(p.lastActionBar).contains("Hypo"), "the action bar clears");
    }

    static void gearHelps() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        r.seasons.feels = 5;
        BukkitKit.TestPlayer naked = BukkitKit.join("Naked", r.world);
        BukkitKit.TestPlayer clad = BukkitKit.join("Clad", r.world);
        dress(clad, r.items);
        r.seconds(900);
        expect(r.service.stage(naked.getUniqueId()).severity() >= 3, "naked: " + r.service.stage(naked.getUniqueId()));
        expect(r.service.stage(clad.getUniqueId()).severity() <= 1, "dressed in the fur kit: " + r.service.stage(clad.getUniqueId()));
        near(r.service.gearOf(clad).insulation(), 0.96, 0.001, "the service reads garments off the armour slots");
        ItemStack iron = new ItemStack(Material.IRON_CHESTPLATE, 1);
        naked.getInventory().setChestplate(iron);
        near(r.service.gearOf(naked).insulation(), 0.025 * 1.4, 0.001, "and plain armour by material");
        expect(r.service.gearOf(naked).metalPieces() == 1, "counting metal");

        // wet ruins the fur
        BukkitKit.Rig w = new BukkitKit.Rig();
        w.seasons.feels = 20;
        w.seasons.precip = "RAIN";
        BukkitKit.TestPlayer wet = BukkitKit.join("Wet", w.world);
        dress(wet, w.items);
        w.seconds(1200);
        expect(w.service.body(wet.getUniqueId()).wet > 0.9, "standing in the rain soaks you");
        expect(w.service.stage(wet.getUniqueId()).severity() >= 3, "and the fur kit is no use soaked: " + w.service.stage(wet.getUniqueId()));
        expect(Kit.strip(wet.lastActionBar).contains("Soaked"), "the action bar says so: " + wet.lastActionBar);
        w.seasons.precip = "NONE";
        w.seasons.sheltered = true;
        w.seasons.feels = 40;
        w.seconds(900);
        expect(w.service.body(wet.getUniqueId()).wet < 0.2, "under a roof you dry out");
    }

    static void heatWave() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        r.seasons.feels = 102;
        r.seasons.uv = 10;
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE, 1));
        p.setSprinting(true);
        r.seconds(900);
        Stage s = r.service.stage(p.getUniqueId());
        expect(s.hotSide() && s.severity() >= 3, "sprinting in iron through a heat wave: " + s);
        expect(p.has(PotionEffectType.NAUSEA) && p.has(PotionEffectType.WEAKNESS), "nauseous and weak");
        expect(p.getHealth() < 20, "heatstroke hurts");
        BodyService.Cause c = r.service.lastCause(p.getUniqueId());
        expect(c != null && !c.cold(), "remembered as heat");
        expect(p.text().contains("Heatstroke"), "you were warned: " + p.text());
        expect(p.getFreezeTicks() == 0, "no frost overlay in the heat");

        // sun hat and linen tunic help, shade helps more
        BukkitKit.Rig hats = new BukkitKit.Rig();
        hats.seasons.feels = 96;
        hats.seasons.uv = 9;
        BukkitKit.TestPlayer a = BukkitKit.join("Bare", hats.world);
        BukkitKit.TestPlayer b = BukkitKit.join("Hat", hats.world);
        b.getInventory().setHelmet(hats.items.create(Garment.SUN_HAT));
        b.getInventory().setChestplate(hats.items.create(Garment.LINEN_TUNIC));
        hats.seconds(900);
        expect(hats.service.body(b.getUniqueId()).core < hats.service.body(a.getUniqueId()).core - 0.3, "light clothes keep you cooler");
        double sunny = hats.service.body(a.getUniqueId()).core;
        hats.seasons.sheltered = true;
        hats.seconds(600);
        expect(hats.service.body(a.getUniqueId()).core < sunny - 0.5, "shade brings the bare one down: " + sunny + " -> " + hats.service.body(a.getUniqueId()).core);
    }

    static void opOuts() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        r.seasons.feels = -10;
        BukkitKit.TestPlayer creative = BukkitKit.join("Creative", r.world);
        creative.setGameMode(GameMode.CREATIVE);
        BukkitKit.TestPlayer spectator = BukkitKit.join("Spec", r.world);
        spectator.setGameMode(GameMode.SPECTATOR);
        BukkitKit.TestPlayer dead = BukkitKit.join("Dead", r.world);
        dead.dead = true;
        BukkitKit.TestPlayer path = BukkitKit.join("Path", r.world);
        path.getPersistentDataContainer().set(r.keys.pathHidden, PersistentDataType.STRING, "nexusroast, NexusWardrobe");
        BukkitKit.TestPlayer other = BukkitKit.join("OtherPath", r.world);
        other.getPersistentDataContainer().set(r.keys.pathHidden, PersistentDataType.STRING, "nexusroast");
        BukkitKit.TestPlayer gentle = BukkitKit.join("Gentle", r.world);
        BukkitKit.TestPlayer normal = BukkitKit.join("Normal", r.world);
        // gentle is the one comfort says no to: the fake answers per call, so use a separate rig below
        r.seconds(600);
        for (BukkitKit.TestPlayer p : new BukkitKit.TestPlayer[]{creative, spectator, dead, path}) {
            expect(r.service.peek(p.getUniqueId()) == null && p.effects.isEmpty() && p.getHealth() == 20 && p.messages.isEmpty(),
                    p.getName() + " is left completely alone");
        }
        expect(r.service.exempt(creative) && r.service.exempt(spectator) && r.service.exempt(dead) && r.service.exempt(path), "exempt says so");
        expect(!r.service.exempt(other) && !r.service.exempt(normal), "hiding some other plugin changes nothing");
        expect(other.getHealth() < 20 && normal.getHealth() < 20, "everyone else suffers");

        BukkitKit.Rig noPath = new BukkitKit.Rig();
        noPath.plugin.getConfig().set("respect-path", false);
        noPath.rebuild();
        noPath.seasons.feels = -10;
        BukkitKit.TestPlayer hidden = BukkitKit.join("Hidden", noPath.world);
        hidden.getPersistentDataContainer().set(noPath.keys.pathHidden, PersistentDataType.STRING, "nexuswardrobe");
        noPath.seconds(600);
        expect(hidden.getHealth() < 20, "respect-path: false ignores NexusPath");

        // comfort: gentle players get warnings only
        BukkitKit.Rig g = new BukkitKit.Rig();
        g.comfort.body = false;
        g.seasons.feels = -10;
        BukkitKit.TestPlayer soft = BukkitKit.join("Soft", g.world);
        g.seconds(600);
        expect(g.service.stage(soft.getUniqueId()) == Stage.DEEP_FREEZE, "the body still cools: " + g.service.stage(soft.getUniqueId()));
        expect(soft.effects.isEmpty() && soft.getHealth() == 20 && soft.getExhaustion() == 0 && soft.getFreezeTicks() == 0,
                "but with comfort turned down: no effects, no damage, no hunger");
        expect(soft.text().contains("Hypothermia") && soft.lastActionBar != null, "the warnings still come");
        expect(!g.service.mayHurt(soft), "mayHurt says no");

        BukkitKit.Rig ignore = new BukkitKit.Rig();
        ignore.comfort.body = false;
        ignore.plugin.getConfig().set("respect-comfort", false);
        ignore.rebuild();
        ignore.seasons.feels = -10;
        BukkitKit.TestPlayer tough = BukkitKit.join("Tough", ignore.world);
        ignore.seconds(600);
        expect(tough.getHealth() < 20, "respect-comfort: false ignores NexusComfort");

        BukkitKit.Rig off = new BukkitKit.Rig();
        off.plugin.getConfig().set("enabled", false);
        off.rebuild();
        off.seasons.feels = -10;
        BukkitKit.TestPlayer p = BukkitKit.join("P", off.world);
        off.seconds(600);
        expect(p.getHealth() == 20 && off.service.peek(p.getUniqueId()) == null, "enabled: false does nothing at all");
    }

    static void damageRules() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        r.seasons.feels = -10;
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        r.service.body(p.getUniqueId()).core = 91.0; // freezing, will fall to deep freeze
        r.seconds(1);
        double h = p.getHealth();
        expect(Stage.of(r.service.body(p.getUniqueId()).core).damaging(), "set up in a damaging stage");
        r.seconds(30);
        expect(p.getHealth() < h - 3, "damage arrives over time");
        expect(p.damageCalls >= 6 && p.damageCalls <= 14, "in a rhythm, not every second: " + p.damageCalls);

        BukkitKit.Rig safe = new BukkitKit.Rig();
        safe.plugin.getConfig().set("damage.can-kill", false);
        safe.rebuild();
        safe.seasons.feels = -30;
        BukkitKit.TestPlayer s = BukkitKit.join("Safe", safe.world);
        safe.seconds(3000);
        expect(s.getHealth() >= 1.0 && s.getHealth() <= 1.01, "can-kill: false leaves a last half-heart: " + s.getHealth());

        BukkitKit.Rig noDamage = new BukkitKit.Rig();
        noDamage.plugin.getConfig().set("damage.enabled", false);
        noDamage.rebuild();
        noDamage.seasons.feels = -30;
        BukkitKit.TestPlayer n = BukkitKit.join("N", noDamage.world);
        noDamage.seconds(600);
        expect(n.getHealth() == 20 && n.has(PotionEffectType.SLOWNESS), "damage.enabled: false keeps the effects but never hurts");

        BukkitKit.Rig mult = new BukkitKit.Rig();
        mult.comfort.preset = "intense";
        mult.plugin.getConfig().set("damage.intense-multiplier", 3.0);
        mult.rebuild();
        mult.seasons.feels = -30;
        BukkitKit.TestPlayer m = BukkitKit.join("M", mult.world);
        mult.service.body(m.getUniqueId()).core = 85.0;
        mult.seconds(3);
        expect(m.lastDamage >= 5.9, "the intense preset multiplies the damage: " + m.lastDamage);
    }

    static void otherWorlds() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        r.seasons.feels = -20; // the overworld is a deep freeze, which must not matter elsewhere
        BukkitKit.TestPlayer hell = BukkitKit.join("Hell", BukkitKit.nether("world_nether"));
        BukkitKit.TestPlayer ender = BukkitKit.join("Ender", BukkitKit.end("world_the_end"));
        BukkitKit.TestPlayer fireproof = BukkitKit.join("Fireproof", BukkitKit.nether("world_nether"));
        fireproof.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1000, 0));
        r.seconds(1800);
        expect(r.service.stage(hell.getUniqueId()).hotSide() && r.service.stage(hell.getUniqueId()).severity() >= 2, "the Nether heats you: " + r.service.stage(hell.getUniqueId()));
        expect(r.service.stage(ender.getUniqueId()).severity() <= 1, "the End is only chilly: " + r.service.stage(ender.getUniqueId()));
        expect(r.service.stage(fireproof.getUniqueId()).severity() <= 1, "fire resistance protects in the Nether: " + r.service.stage(fireproof.getUniqueId()));
        expect(!ender.has(PotionEffectType.BLINDNESS), "no freezing in the End");
    }

    static void sources() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        HeatScanner scanner = new HeatScanner(r.config.params);
        HeatScanner.Result none = scanner.scan(p, 3);
        expect(none.heat() == 0 && none.chill() == 0, "bare ground gives nothing");
        r.world.getBlockAt(101, 70, 100).setBlockData(new BukkitKit.LitData("CAMPFIRE", true));
        double lit = scanner.scan(p, 3).heat();
        expect(lit > 8, "a lit campfire beside you warms: " + lit);
        r.world.getBlockAt(101, 70, 100).setBlockData(new BukkitKit.LitData("CAMPFIRE", false));
        expect(scanner.scan(p, 3).heat() == 0, "an unlit one does not");
        r.world.getBlockAt(101, 70, 100).setType(Material.AIR);
        r.world.getBlockAt(103, 70, 100).setBlockData(new BukkitKit.LitData("CAMPFIRE", true));
        double far = scanner.scan(p, 3).heat();
        r.world.getBlockAt(103, 70, 100).setType(Material.AIR);
        r.world.getBlockAt(100, 70, 101).setBlockData(new BukkitKit.LitData("CAMPFIRE", true));
        double close = scanner.scan(p, 3).heat();
        expect(far < close && far > 0, "farther is cooler: " + far + " < " + close);
        r.world.getBlockAt(106, 70, 100).setBlockData(new BukkitKit.LitData("CAMPFIRE", true));
        expect(scanner.scan(p, 3).heat() == close, "out of reach is out of mind");
        r.world.getBlockAt(100, 70, 101).setType(Material.AIR);
        r.world.getBlockAt(100, 69, 100).setType(Material.LAVA);
        expect(scanner.scan(p, 3).heat() > 5, "lava underfoot is hot");
        r.world.getBlockAt(100, 69, 100).setType(Material.AIR);
        for (int dx = -1; dx <= 1; dx++) {
            r.world.getBlockAt(100 + dx, 70, 99).setType(Material.BLUE_ICE);
        }
        HeatScanner.Result ice = scanner.scan(p, 3);
        expect(ice.chill() > 2 && ice.heat() == 0, "ice chills: " + ice.chill());

        // the real tick: sitting at a campfire turns a deadly night into a comfortable one
        BukkitKit.Rig camp = new BukkitKit.Rig();
        camp.seasons.feels = 15;
        BukkitKit.TestPlayer cold = BukkitKit.join("Cold", camp.world);
        BukkitKit.TestPlayer warm = BukkitKit.join("Warm", camp.world);
        warm.stubSetLocation(new org.bukkit.Location(camp.world, 200, 70, 200));
        camp.world.getBlockAt(200, 70, 201).setBlockData(new BukkitKit.LitData("CAMPFIRE", true));
        camp.world.getBlockAt(201, 70, 200).setBlockData(new BukkitKit.LitData("CAMPFIRE", true));
        for (Material m : new Material[]{Material.LEATHER_CHESTPLATE}) {
            cold.getInventory().setChestplate(new ItemStack(m, 1));
            warm.getInventory().setChestplate(new ItemStack(m, 1));
        }
        camp.seconds(900);
        expect(camp.service.stage(cold.getUniqueId()).severity() >= 3, "away from the fire: " + camp.service.stage(cold.getUniqueId()));
        expect(camp.service.stage(warm.getUniqueId()).severity() <= 1, "by the fire: " + camp.service.stage(warm.getUniqueId()));
    }

    static void weatherSources() {
        // no NexusSeasons: the vanilla biome decides
        BukkitKit.Rig r = new BukkitKit.Rig(false, false);
        BukkitKit.TestPlayer plains = BukkitKit.join("Plains", r.world);
        BukkitKit.TestPlayer tundra = BukkitKit.join("Tundra", r.world);
        tundra.stubSetLocation(new org.bukkit.Location(r.world, 500, 70, 500));
        r.world.stubSetTemperature(500, 70, 500, -0.5);
        r.world.setTime(18000);
        r.seconds(900);
        expect(r.service.stage(plains.getUniqueId()).severity() <= 1, "plains at night without NexusSeasons: " + r.service.stage(plains.getUniqueId()));
        expect(r.service.stage(tundra.getUniqueId()).severity() >= 3, "a frozen biome at night: " + r.service.stage(tundra.getUniqueId()));
        expect(r.service.climate().name().length() > 0, "named");
        Ambient desert = new BiomeClimate().read(plains);
        expect(desert != null && desert.feelsF() < 80, "plains are mild: " + desert.feelsF());
        r.world.stubSetTemperature(100, 70, 100, 2.0);
        r.world.setTime(6000);
        Ambient hot = new BiomeClimate().read(plains);
        expect(hot.feelsF() > 95 && hot.uv() > 0, "a desert at noon is hot and bright: " + hot.feelsF());
        r.world.setStorm(true);
        Ambient rain = new BiomeClimate().read(plains);
        expect(rain.precip().equals("RAIN") && rain.feelsF() < hot.feelsF() && rain.uv() == 0, "rain cools and dims");
        r.world.stubSetTemperature(100, 70, 100, -0.2);
        expect(new BiomeClimate().read(plains).precip().equals("SNOW"), "cold biomes get snow");
        r.world.setThundering(true);
        r.world.stubSetTemperature(100, 70, 100, 0.8);
        expect(new BiomeClimate().read(plains).precip().equals("STORM"), "thunder is a storm");

        // a broken NexusSeasons falls back instead of crashing the tick
        BukkitKit.Rig broken = new BukkitKit.Rig();
        broken.seasons.broken = true;
        BukkitKit.TestPlayer p = BukkitKit.join("P", broken.world);
        broken.seconds(5);
        expect(broken.service.peek(p.getUniqueId()) != null, "the tick survives a failing climate service");

        // a nether player never asks the overworld's weather
        BukkitKit.TestPlayer n = BukkitKit.join("N", BukkitKit.nether("world_nether"));
        Inputs in = broken.service.inputsFor(n, HeatScanner.Result.NONE);
        expect(in.dim() == Dim.NETHER && in.precip().equals("NONE") && !in.sheltered(), "nether inputs carry no weather");
    }

    static void food() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        r.seasons.feels = 30;
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE, 1));
        BukkitKit.TestPlayer q = BukkitKit.join("Bob", r.world);
        q.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE, 1));
        HeatTable.Buff buff = r.service.consumed(p, "RABBIT_STEW");
        expect(buff != null && buff.warm(), "rabbit stew warms");
        near(r.service.body(p.getUniqueId()).warmBuffF, 16, 0.001, "by sixteen degrees");
        expect(r.service.consumed(p, "DIRT") == null && r.service.consumed(p, "MELON_SLICE") != null, "dirt is not food, melon cools");
        r.seconds(150);
        expect(r.service.body(p.getUniqueId()).core > r.service.body(q.getUniqueId()).core + 0.3, "the stew keeps you warmer than going without");
        BukkitKit.TestPlayer creative = BukkitKit.join("Cr", r.world);
        creative.setGameMode(GameMode.CREATIVE);
        expect(r.service.consumed(creative, "RABBIT_STEW") == null, "creative players are not affected");
    }

    static void clockJumps() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        r.seasons.feels = -20;
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        r.seconds(1);
        double before = r.service.body(p.getUniqueId()).core;
        Body probe = r.service.body(p.getUniqueId()).copy();
        Inputs in = r.service.inputsFor(p, HeatScanner.Result.NONE);
        double target = Thermo.assess(r.config.params, probe, in).targetCore();
        r.now.addAndGet(600_000); // the server froze for ten minutes
        r.service.tick();
        double after = r.service.body(p.getUniqueId()).core;
        near(after, before + (target - before) * (1 - Math.exp(-5.0 / 75.0)), 0.05, "a long stall counts as five seconds, not ten minutes: " + before + " -> " + after);
        r.now.addAndGet(-5_000_000); // the clock stepped backwards
        double b2 = r.service.body(p.getUniqueId()).core;
        r.service.tick();
        expect(r.service.body(p.getUniqueId()).core <= b2 && b2 - r.service.body(p.getUniqueId()).core < 0.5, "a clock that goes backwards cannot break anything");

        BukkitKit.Rig day = new BukkitKit.Rig(false, false);
        BukkitKit.TestPlayer q = BukkitKit.join("Quin", day.world);
        day.world.setTime(6000);
        double noon = new BiomeClimate().read(q).feelsF();
        day.world.setTime(18000);
        double midnight = new BiomeClimate().read(q).feelsF();
        near(noon - midnight, 12, 0.001, "nights are twelve degrees colder in the vanilla fallback");
    }

    static void persistence() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        Body b = r.service.body(p.getUniqueId());
        b.core = 92.0;
        b.wet = 0.8;
        r.service.save();
        expect(new java.io.File(r.dir, "bodies.yml").exists(), "bodies.yml is written");
        BukkitKit.Rig again = new BukkitKit.Rig().useFolder(r.dir);
        again.service.load();
        Body loaded = again.service.peek(p.getUniqueId());
        expect(loaded != null, "a restart finds the saved body");
        near(loaded.core, 92.0, 0.01, "same core temperature");
        near(loaded.wet, 0.8, 0.01, "same wetness");
        again.now.addAndGet(0);
        // away for ten minutes: mostly recovered
        again.service.onJoin(p.getUniqueId());
        expect(again.service.peek(p.getUniqueId()).core <= 92.01, "joined at once: not healed in an instant");
        BukkitKit.Rig later = new BukkitKit.Rig().useFolder(r.dir);
        later.service.load();
        later.now.addAndGet(600_000);
        later.service.onJoin(p.getUniqueId());
        Body healed = later.service.peek(p.getUniqueId());
        expect(healed.core > 97.5 && healed.wet < 0.15, "ten minutes away and you have mostly recovered: " + healed.core + " " + healed.wet);

        // a comfortable body is not written at all
        BukkitKit.Rig clean = new BukkitKit.Rig();
        BukkitKit.TestPlayer q = BukkitKit.join("Calm", clean.world);
        clean.service.body(q.getUniqueId());
        clean.service.save();
        BukkitKit.Rig reload = new BukkitKit.Rig().useFolder(clean.dir);
        reload.service.load();
        expect(reload.service.peek(q.getUniqueId()) == null, "nothing is stored for someone who is fine");
        reload.service.reset(p.getUniqueId());
        expect(reload.service.stage(p.getUniqueId()) == Stage.COMFORTABLE, "reset clears a body");
        reload.service.forget(p.getUniqueId());
        expect(reload.service.peek(p.getUniqueId()) == null, "forget drops it");
    }
}
