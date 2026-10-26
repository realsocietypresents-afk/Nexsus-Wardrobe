package com.nexuscraft.nexuswardrobe;

import com.nexuscraft.nexuscomfort.ComfortApi;
import com.nexusuniverse.seasons.climate.ClimateApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Test doubles for the server: players that remember what happened to them, a fake NexusSeasons and NexusComfort, a clock. */
final class BukkitKit {

    private BukkitKit() {
    }

    static final class TestPlugin extends JavaPlugin {
    }

    static class TestPlayer extends Player {
        final String name;
        final List<String> messages = new ArrayList<>();
        final Set<String> perms = new HashSet<>();
        final List<PotionEffect> effects = new ArrayList<>();
        boolean water;
        boolean dead;

        TestPlayer(String name) {
            this.name = name;
        }

        @Override public String getName() { return name; }
        @Override public boolean isOnline() { return true; }
        @Override public boolean isDead() { return dead; }
        @Override public boolean isInWater() { return water; }
        @Override public boolean hasPermission(String node) { return perms.contains(node); }
        @Override public void sendMessage(String m) { messages.add(m); }
        @Override public boolean addPotionEffect(PotionEffect e) { effects.add(e); return true; }
        @Override public boolean hasPotionEffect(PotionEffectType type) {
            for (PotionEffect e : effects) {
                if (e.getType() == type) {
                    return true;
                }
            }
            return false;
        }

        String text() {
            return Kit.strip(String.join("\n", messages));
        }

        boolean has(PotionEffectType type) {
            return hasPotionEffect(type);
        }

        int amp(PotionEffectType type) {
            int best = -1;
            for (PotionEffect e : effects) {
                if (e.getType() == type) {
                    best = Math.max(best, e.getAmplifier());
                }
            }
            return best;
        }
    }

    /** A command sender that is not a player. */
    static final class Console implements org.bukkit.command.CommandSender {
        final List<String> out = new ArrayList<>();

        @Override public boolean hasPermission(String node) { return true; }
        @Override public void sendMessage(String message) { out.add(message); }
        @Override public void sendMessage(net.kyori.adventure.text.Component message) { }
        @Override public String getName() { return "CONSOLE"; }

        boolean said(String fragment) {
            for (String m : out) {
                if (m.replaceAll("§.", "").contains(fragment)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** A lit furnace or campfire block's data. */
    static final class LitData extends BlockData implements Lightable {
        private boolean lit;

        LitData(String material, boolean lit) {
            super(material);
            this.lit = lit;
        }

        @Override public boolean isLit() { return lit; }
        @Override public void setLit(boolean lit) { this.lit = lit; }
    }

    /** A NexusSeasons that reports whatever the test says. */
    static final class FakeClimate implements ClimateApi {
        double feels = 68;
        double uv;
        boolean sheltered;
        String precip = "NONE";
        boolean claimed;
        boolean broken;

        @Override public double temperatureF(Player p) { return feels; }
        @Override public double feelsLikeF(Player p) {
            if (broken) {
                throw new IllegalStateException("gone");
            }
            return p.getWorld().getEnvironment() == World.Environment.NORMAL ? feels : Double.NaN;
        }
        @Override public double windMph(Player p) { return 0; }
        @Override public double uvIndex(Player p) { return uv; }
        @Override public boolean sheltered(Player p) { return sheltered; }
        @Override public String precipitation(Player p) { return precip; }
        @Override public void setBodyHeatHandledElsewhere(boolean handled) { claimed = handled; }
        @Override public boolean bodyHeatHandledElsewhere() { return claimed; }
    }

    static final class FakeComfort implements ComfortApi {
        boolean body = true;
        String preset = "standard";

        @Override public boolean allows(UUID player, String category) { return !category.equals("body") || body; }
        @Override public String preset(UUID player) { return preset; }
    }

    static File reset() {
        Bukkit.WORLDS.clear();
        Bukkit.PLAYERS.clear();
        BukkitScheduler.LATER.clear();
        BukkitScheduler.REPEATING.clear();
        BukkitScheduler.HANDLES.clear();
        BukkitScheduler.DELAYS.clear();
        BukkitScheduler.PERIODS.clear();
        org.bukkit.plugin.PluginManager.REGISTERED.clear();
        Bukkit.getServicesManager().clear();
        try {
            File dir = Files.createTempDirectory("nxwardrobe").toFile();
            JavaPlugin.DATA_FOLDER = dir;
            return dir;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static World overworld(String name) {
        return Bukkit.createWorld(new WorldCreator(name));
    }

    static World nether(String name) {
        return Bukkit.createWorld(new WorldCreator(name).environment(World.Environment.NETHER));
    }

    static World end(String name) {
        return Bukkit.createWorld(new WorldCreator(name).environment(World.Environment.THE_END));
    }

    static TestPlayer join(String name, World world) {
        TestPlayer p = new TestPlayer(name);
        p.stubSetLocation(new Location(world, 100, 70, 100));
        Bukkit.PLAYERS.put(p.getUniqueId(), p);
        return p;
    }

    static Command command(String name) {
        return new Command() {
            @Override
            public String getName() {
                return name;
            }
        };
    }

    /** One running server: a plugin, the fakes, a clock that moves one second per tick(). */
    static final class Rig {
        final File dir = reset();
        final TestPlugin plugin = new TestPlugin();
        final FakeClimate seasons = new FakeClimate();
        final FakeComfort comfort = new FakeComfort();
        final AtomicLong now = new AtomicLong(1_000_000L);
        final World world = overworld("world");
        WardrobeConfig config;
        WardrobeKeys keys = new WardrobeKeys();
        GarmentItems items;
        BodyService service;

        Rig() {
            this(true, true);
        }

        Rig(boolean withSeasons, boolean withComfort) {
            if (withSeasons) {
                Bukkit.getServicesManager().register(ClimateApi.class, seasons, plugin, org.bukkit.plugin.ServicePriority.Normal);
            }
            if (withComfort) {
                Bukkit.getServicesManager().register(ComfortApi.class, comfort, plugin, org.bukkit.plugin.ServicePriority.Normal);
            }
            rebuild();
        }

        /** Pretend the server restarted: same data folder, brand new objects. */
        Rig useFolder(File folder) {
            JavaPlugin.DATA_FOLDER = folder;
            rebuild();
            return this;
        }

        void rebuild() {
            config = new WardrobeConfig(plugin.getConfig());
            items = new GarmentItems(plugin, keys);
            service = new BodyService(plugin, config, keys, items, new SeasonsClimate(), new ComfortBridge(), now::get);
        }

        /** Runs the once-a-second tick this many times, a second apart. */
        void seconds(int n) {
            for (int i = 0; i < n; i++) {
                now.addAndGet(1000);
                service.tick();
            }
        }
    }

}
