package com.nexuscraft.nexuswardrobe;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Keeps every player's body: once a second it reads their surroundings and gear, steps the model, and applies what the stage does to them. */
public final class BodyService {

    /** What last hurt a player, for the death message. */
    public record Cause(boolean cold, long at) {
    }

    private static final class Track {
        Stage stage = Stage.COMFORTABLE;
        long lastStep;
        int damageClock;
        int scanClock;
        HeatScanner.Result sources = HeatScanner.Result.NONE;
        long adviceAt;
    }

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final WardrobeKeys keys;
    private final GarmentItems items;
    private final HeatScanner scanner;
    private final ComfortBridge comfort;
    private final LongSupplier clock;
    private final File file;
    private final Map<UUID, Body> bodies = new HashMap<>();
    private final Map<UUID, Track> tracks = new HashMap<>();
    private final Map<UUID, Cause> causes = new HashMap<>();
    private final Map<UUID, Long> savedAt = new HashMap<>();
    private volatile WardrobeConfig cfg;
    private volatile ClimateSource climate;
    private boolean dirty;

    public BodyService(org.bukkit.plugin.java.JavaPlugin plugin, WardrobeConfig cfg, WardrobeKeys keys, GarmentItems items,
                       ClimateSource climate, ComfortBridge comfort, LongSupplier clock) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.keys = keys;
        this.items = items;
        this.climate = climate;
        this.comfort = comfort;
        this.clock = clock;
        this.scanner = new HeatScanner(cfg.params);
        this.file = new File(plugin.getDataFolder(), "bodies.yml");
    }

    public void setConfig(WardrobeConfig cfg) {
        this.cfg = cfg;
    }

    public void setClimate(ClimateSource climate) {
        this.climate = climate;
    }

    public ClimateSource climate() {
        return climate;
    }

    public WardrobeConfig config() {
        return cfg;
    }

    // ---- who is affected ---------------------------------------------------------------------------------------

    /** True if the model does not apply to this player at all (creative, spectator, dead, or switched off in NexusPath). */
    public boolean exempt(Player player) {
        if (player == null || player.isDead()) {
            return true;
        }
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return true;
        }
        return cfg.respectPath && hiddenInPath(player);
    }

    boolean hiddenInPath(Player player) {
        try {
            String raw = player.getPersistentDataContainer().get(keys.pathHidden, WardrobeKeys.STRING);
            if (raw == null) {
                return false;
            }
            for (String part : raw.split(",")) {
                if (part.trim().toLowerCase(Locale.ROOT).equals("nexuswardrobe")) {
                    return true;
                }
            }
        } catch (RuntimeException unreadable) {
            return false;
        }
        return false;
    }

    /** True if stages may cost this player something (effects, damage). Otherwise they only get the warnings. */
    public boolean mayHurt(Player player) {
        return !cfg.respectComfort || comfort.allowsBody(player.getUniqueId());
    }

    // ---- the bodies ------------------------------------------------------------------------------------------------

    public Body body(UUID id) {
        return bodies.computeIfAbsent(id, k -> new Body());
    }

    public Body peek(UUID id) {
        return bodies.get(id);
    }

    public Stage stage(UUID id) {
        Body b = bodies.get(id);
        return b == null ? Stage.COMFORTABLE : b.stage();
    }

    public Cause lastCause(UUID id) {
        return causes.get(id);
    }

    public void clearCause(UUID id) {
        causes.remove(id);
    }

    /** Back to normal: after a death. */
    public void reset(UUID id) {
        bodies.put(id, new Body());
        Track t = tracks.get(id);
        if (t != null) {
            t.stage = Stage.COMFORTABLE;
            t.damageClock = 0;
        }
        dirty = true;
    }

    public void forget(UUID id) {
        bodies.remove(id);
        tracks.remove(id);
        savedAt.remove(id);
    }

    public Insulation.Gear gearOf(Player player) {
        PlayerInventory inv = player.getInventory();
        return Insulation.of(items.pieceOf(inv.getHelmet()), items.pieceOf(inv.getChestplate()), items.pieceOf(inv.getLeggings()),
                items.pieceOf(inv.getBoots()));
    }

    static Dim dimOf(World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> Dim.NETHER;
            case THE_END -> Dim.END;
            default -> Dim.OVERWORLD;
        };
    }

    /** What the model sees for this player right now, or null if there is no weather to read. */
    public Inputs inputsFor(Player player, HeatScanner.Result sources) {
        Dim dim = dimOf(player.getWorld());
        Ambient a = null;
        if (dim == Dim.OVERWORLD) {
            a = climate.read(player);
            if (a == null) {
                a = new BiomeClimate().read(player);
            }
        }
        double feels = a == null ? 68 : a.feelsF();
        boolean sheltered = a != null && a.sheltered();
        double uv = a == null ? 0 : a.uv();
        String precip = a == null ? "NONE" : a.precip();
        return new Inputs(dim, feels, sheltered, uv, precip, player.isInWater(), player.isSprinting(),
                player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE), sources.heat(), sources.chill(), gearOf(player));
    }

    // ---- once a second -----------------------------------------------------------------------------------------

    public void tick() {
        WardrobeConfig c = cfg;
        if (!c.enabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (exempt(player)) {
                continue;
            }
            step(player, c);
        }
    }

    void step(Player player, WardrobeConfig c) {
        UUID id = player.getUniqueId();
        Body body = body(id);
        Track track = tracks.computeIfAbsent(id, k -> new Track());
        long now = clock.getAsLong();
        double dt = track.lastStep == 0 ? 1.0 : Math.max(0.25, Math.min(5.0, (now - track.lastStep) / 1000.0));
        track.lastStep = now;

        if (track.scanClock <= 0) {
            track.sources = scanner.scan(player, c.sourceRadius);
            track.scanClock = c.scanEverySeconds;
        }
        track.scanClock--;

        Inputs in = inputsFor(player, track.sources);
        Thermo.Report report = Thermo.step(c.params, body, in, dt);
        dirty = true;

        Stage stage = body.stage();
        Stage before = track.stage;
        boolean hurt = mayHurt(player);
        if (stage != before) {
            track.stage = stage;
            track.damageClock = 0;
            if (c.messages) {
                String text = Texts.transition(before, stage, body.wet);
                if (text != null) {
                    player.sendMessage(text);
                }
            }
        }

        Fx.Plan plan = Fx.of(stage);
        if (hurt) {
            apply(player, plan, c);
        } else {
            clearFreeze(player);
        }
        if (c.actionbar) {
            String bar = Texts.actionBar(c, body, stage, report);
            if (bar != null) {
                player.sendActionBar(Component.text(bar));
            }
        }
        if (stage.damaging() && c.messages && now - track.adviceAt > 60_000L) {
            track.adviceAt = now;
            player.sendMessage(Texts.advice(stage, body.wet, in));
        }
    }

    private void apply(Player player, Fx.Plan plan, WardrobeConfig c) {
        for (Fx.Effect e : plan.effects()) {
            PotionEffectType type = typeOf(e.kind());
            if (type != null) {
                player.addPotionEffect(new PotionEffect(type, 60, e.amplifier(), true, false));
            }
        }
        if (plan.freezeScreen()) {
            player.setFreezeTicks(Math.max(0, player.getMaxFreezeTicks() - 4));
        } else {
            clearFreeze(player);
        }
        if (plan.exhaustion() > 0) {
            player.setExhaustion(player.getExhaustion() + (float) plan.exhaustion());
        }
        UUID id = player.getUniqueId();
        Track track = tracks.get(id);
        if (plan.damage() > 0 && c.damage) {
            track.damageClock++;
            if (track.damageClock >= plan.everySeconds()) {
                track.damageClock = 0;
                double amount = plan.damage();
                if ("intense".equalsIgnoreCase(comfort.preset(id))) {
                    amount *= c.intenseMultiplier;
                }
                hurt(player, amount, track.stage.coldSide(), c);
            }
        } else {
            track.damageClock = 0;
        }
    }

    private void clearFreeze(Player player) {
        if (player.getFreezeTicks() > 0 && !player.isInWater()) {
            player.setFreezeTicks(0);
        }
    }

    private void hurt(Player player, double amount, boolean cold, WardrobeConfig c) {
        double health = player.getHealth();
        if (!c.canKill) {
            amount = Math.min(amount, Math.max(0, health - 1.0));
        }
        if (amount <= 0) {
            return;
        }
        causes.put(player.getUniqueId(), new Cause(cold, clock.getAsLong()));
        player.damage(amount);
    }

    static PotionEffectType typeOf(Fx.Kind kind) {
        return switch (kind) {
            case SLOWNESS -> PotionEffectType.SLOWNESS;
            case WEAKNESS -> PotionEffectType.WEAKNESS;
            case MINING_FATIGUE -> PotionEffectType.MINING_FATIGUE;
            case NAUSEA -> PotionEffectType.NAUSEA;
            case BLINDNESS -> PotionEffectType.BLINDNESS;
            case HUNGER -> PotionEffectType.HUNGER;
        };
    }

    // ---- food and drink ------------------------------------------------------------------------------------

    /** A player ate or drank something: warm or cool them if it is that kind of thing. Returns what it did, or null. */
    public HeatTable.Buff consumed(Player player, String material) {
        HeatTable.Buff buff = HeatTable.buffFor(material);
        if (buff == null || exempt(player)) {
            return null;
        }
        Body b = body(player.getUniqueId());
        if (buff.warm()) {
            Thermo.warm(cfg.params, b, buff.degrees(), buff.seconds());
        } else {
            Thermo.cool(cfg.params, b, buff.degrees(), buff.seconds());
        }
        dirty = true;
        return buff;
    }

    // ---- saving ----------------------------------------------------------------------------------------------------

    public void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        var section = data.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            Body b = new Body();
            b.core = clamp(section.getDouble(key + ".core", 98.6), 80, 109);
            b.wet = clamp(section.getDouble(key + ".wet", 0), 0, 1);
            bodies.put(id, b);
            savedAt.put(id, (long) section.getDouble(key + ".at", 0));
        }
    }

    /** When someone logs in, the time they were away counts: slowly back toward normal, dried out. */
    public void onJoin(UUID id) {
        Body b = bodies.get(id);
        Long at = savedAt.remove(id);
        if (b == null) {
            return;
        }
        if (at != null && at > 0) {
            double away = Math.max(0, (clock.getAsLong() - at) / 1000.0);
            double recover = 1.0 - Math.exp(-away / 300.0);
            b.core += (cfg.params.normalCore - b.core) * recover;
            b.wet *= (1.0 - recover);
            b.warmLeft = 0;
            b.warmBuffF = 0;
            b.coolLeft = 0;
            b.coolBuffF = 0;
        }
        Track t = tracks.computeIfAbsent(id, k -> new Track());
        t.stage = b.stage();
        t.lastStep = 0;
    }

    public void saveIfDirty() {
        if (!dirty) {
            return;
        }
        save();
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        long now = clock.getAsLong();
        for (Map.Entry<UUID, Body> e : bodies.entrySet()) {
            Body b = e.getValue();
            if (Math.abs(b.core - 98.6) < 0.05 && b.wet < 0.01) {
                continue;
            }
            String base = "players." + e.getKey();
            data.set(base + ".core", Math.round(b.core * 100.0) / 100.0);
            data.set(base + ".wet", Math.round(b.wet * 100.0) / 100.0);
            data.set(base + ".at", (double) now);
        }
        try {
            data.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().warning("NexusWardrobe: could not save bodies.yml: " + e.getMessage());
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
