package com.nexuscraft.nexuswardrobe;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * NexusWardrobe: clothing and body temperature. Reads the weather from NexusSeasons (or the vanilla biome without it), counts what you
 * wear, how wet you are and what is burning nearby, and keeps a core temperature that can freeze or cook you.
 */
public final class NexusWardrobePlugin extends JavaPlugin {

    private WardrobeConfig config;
    private WardrobeKeys keys;
    private GarmentItems items;
    private SeasonsClimate seasons;
    private BodyService service;
    private WardrobeCommand command;
    private BukkitTask ticker;
    private BukkitTask saver;
    private BukkitTask claimer;
    private boolean claimed;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = new WardrobeConfig(getConfig());
        this.keys = new WardrobeKeys();
        this.items = new GarmentItems(this, keys);
        this.seasons = new SeasonsClimate();
        this.service = new BodyService(this, config, keys, items, seasons, new ComfortBridge(), System::currentTimeMillis);
        service.load();
        if (config.recipes) {
            items.registerRecipes();
        }
        getServer().getPluginManager().registerEvents(new WardrobeListener(service, System::currentTimeMillis), this);
        this.command = new WardrobeCommand(service, items);
        command.setReload(this::reloadAll);
        for (String name : new String[]{"body", "wardrobe"}) {
            getCommand(name).setExecutor(command);
            getCommand(name).setTabCompleter(command);
        }
        getServer().getServicesManager().register(WardrobeApi.class, new ApiImpl(service), this, ServicePriority.Normal);
        this.ticker = getServer().getScheduler().runTaskTimer(this, service::tick, 20L, 20L);
        this.saver = getServer().getScheduler().runTaskTimer(this, service::saveIfDirty, 6000L, 6000L);
        this.claimer = getServer().getScheduler().runTaskTimer(this, this::claimHeat, 20L, 200L);
        getLogger().info("NexusWardrobe enabled -- weather from "
                + (seasons.available() ? "NexusSeasons" : "the vanilla biome (install NexusSeasons for real weather)") + ".");
    }

    /** Asks NexusSeasons to hand over summer heat, once, whenever it turns up (it may load after us). */
    private void claimHeat() {
        if (!claimed && config.takeOverHeat && seasons.available()) {
            seasons.claimBodyHeat(true);
            claimed = true;
        }
    }

    @Override
    public void onDisable() {
        for (BukkitTask t : new BukkitTask[]{ticker, saver, claimer}) {
            if (t != null) {
                t.cancel();
            }
        }
        if (claimed) {
            seasons.claimBodyHeat(false);
        }
        if (items != null) {
            items.unregisterRecipes();
        }
        if (service != null) {
            service.save();
        }
        Bukkit.getServicesManager().unregisterAll(this);
    }

    private void reloadAll() {
        reloadConfig();
        this.config = new WardrobeConfig(getConfig());
        service.setConfig(config);
        if (config.recipes) {
            items.registerRecipes();
        } else {
            items.unregisterRecipes();
        }
        if (config.takeOverHeat) {
            claimHeat();
        } else if (claimed) {
            seasons.claimBodyHeat(false);
            claimed = false;
        }
    }
}
