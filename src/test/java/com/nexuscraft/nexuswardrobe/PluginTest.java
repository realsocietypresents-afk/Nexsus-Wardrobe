package com.nexuscraft.nexuswardrobe;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static com.nexuscraft.nexuswardrobe.Kit.expect;

/** Start-up wiring, hand-over of summer heat, saving, reload and shutdown. */
public class PluginTest {

    public static void main(String[] args) throws Exception {
        BukkitKit.reset();
        BukkitKit.FakeClimate fake = new BukkitKit.FakeClimate();
        BukkitKit.TestPlugin other = new BukkitKit.TestPlugin();
        // NexusSeasons loads LATER than Wardrobe: the claim must still happen
        NexusWardrobePlugin plugin = new NexusWardrobePlugin();
        plugin.onEnable();
        PluginCommand body = plugin.getCommand("body");
        PluginCommand wardrobe = plugin.getCommand("wardrobe");
        expect(body.getExecutor() instanceof WardrobeCommand && body.getTabCompleter() instanceof WardrobeCommand, "/body is wired");
        expect(wardrobe.getExecutor() instanceof WardrobeCommand && wardrobe.getTabCompleter() instanceof WardrobeCommand, "/wardrobe is wired");
        expect(PluginManager.REGISTERED.stream().anyMatch(WardrobeListener.class::isInstance), "the listener is registered");
        expect(Bukkit.getServicesManager().load(WardrobeApi.class) != null, "the API is published");
        expect(BukkitScheduler.PERIODS.size() == 3 && BukkitScheduler.PERIODS.contains(20L) && BukkitScheduler.PERIODS.contains(6000L)
                && BukkitScheduler.PERIODS.contains(200L), "three timers: tick, save, and the heat hand-over: " + BukkitScheduler.PERIODS);
        expect(Bukkit.getRecipe(new org.bukkit.NamespacedKey(plugin, "garment_parka")) != null, "recipes are registered");

        Runnable claimer = BukkitScheduler.REPEATING.get(2);
        claimer.run();
        expect(!fake.claimed, "no NexusSeasons yet: nothing to claim");
        Bukkit.getServicesManager().register(com.nexusuniverse.seasons.climate.ClimateApi.class, fake, other,
                org.bukkit.plugin.ServicePriority.Normal);
        claimer.run();
        expect(fake.claimed, "once NexusSeasons turns up, summer heat is handed over");

        // a body is saved on shutdown
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", BukkitKit.overworld("world"));
        ((ApiImpl) Bukkit.getServicesManager().load(WardrobeApi.class)).warm(p.getUniqueId(), 5, 5);
        Runnable ticker = BukkitScheduler.REPEATING.get(0);
        fake.feels = -20;
        for (int i = 0; i < 400; i++) {
            ticker.run();
        }
        File dir = org.bukkit.plugin.java.JavaPlugin.DATA_FOLDER;
        plugin.onDisable();
        expect(!fake.claimed, "stopping hands summer heat back to NexusSeasons");
        expect(new File(dir, "bodies.yml").exists() && Files.readString(new File(dir, "bodies.yml").toPath()).contains(p.getUniqueId().toString()),
                "stopping saves the bodies");
        expect(Bukkit.getServicesManager().load(WardrobeApi.class) == null, "and withdraws the API");
        expect(Bukkit.getRecipe(new org.bukkit.NamespacedKey(plugin, "garment_parka")) == null, "and the recipes");
        expect(BukkitScheduler.HANDLES.stream().allMatch(h -> h.isCancelled()), "and all timers");
        plugin.onDisable();
        expect(true, "stopping twice is fine");

        // the shipped files agree with the code
        String yml = Files.readString(new File("src/main/resources/plugin.yml").toPath());
        for (String must : List.of("main: com.nexuscraft.nexuswardrobe.NexusWardrobePlugin", "body:", "wardrobe:", "nexuswardrobe.admin:",
                "softdepend: [NexusSeasons, NexusComfort, NexusPath]")) {
            expect(yml.contains(must), "plugin.yml has " + must);
        }

        // recipes off and heat hand-over off: reload honours both
        BukkitKit.reset();
        NexusWardrobePlugin second = new NexusWardrobePlugin();
        second.getConfig().set("garments.recipes", false);
        second.getConfig().set("climate.take-over-heat-exhaustion", false);
        BukkitKit.FakeClimate f2 = new BukkitKit.FakeClimate();
        Bukkit.getServicesManager().register(com.nexusuniverse.seasons.climate.ClimateApi.class, f2, other, org.bukkit.plugin.ServicePriority.Normal);
        second.onEnable();
        BukkitScheduler.REPEATING.get(2).run();
        expect(Bukkit.getRecipe(new org.bukkit.NamespacedKey(second, "garment_parka")) == null, "garments.recipes: false registers none");
        expect(!f2.claimed, "take-over-heat-exhaustion: false leaves NexusSeasons alone");
        second.getConfig().set("garments.recipes", true);
        second.getConfig().set("climate.take-over-heat-exhaustion", true);
        WardrobeCommand cmd = (WardrobeCommand) second.getCommand("wardrobe").getExecutor();
        BukkitKit.TestPlayer admin = BukkitKit.join("Admin", BukkitKit.overworld("world"));
        admin.perms.add("nexuswardrobe.admin");
        cmd.onCommand(admin, second.getCommand("wardrobe"), "wardrobe", new String[]{"reload"});
        expect(Bukkit.getRecipe(new org.bukkit.NamespacedKey(second, "garment_parka")) != null && f2.claimed, "reload turns both on");
        second.getConfig().set("climate.take-over-heat-exhaustion", false);
        second.getConfig().set("garments.recipes", false);
        cmd.onCommand(admin, second.getCommand("wardrobe"), "wardrobe", new String[]{"reload"});
        expect(Bukkit.getRecipe(new org.bukkit.NamespacedKey(second, "garment_parka")) == null && !f2.claimed, "and off again");
        second.onDisable();
        Kit.finish();
    }
}
