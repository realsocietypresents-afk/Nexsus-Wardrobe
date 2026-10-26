package com.nexuscraft.nexuswardrobe;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Random;
import java.util.function.LongSupplier;

/** Food and drink, deaths, joining and leaving. */
public final class WardrobeListener implements Listener {

    private static final String[] FROZE = {"%s froze to death", "%s forgot their coat and froze", "%s turned into an icicle",
            "%s should have lit a fire"};
    private static final String[] COOKED = {"%s succumbed to heatstroke", "%s cooked in the sun", "%s overheated",
            "%s should have found some shade"};
    private static final long RECENT_MS = 10_000L;

    private final BodyService service;
    private final LongSupplier clock;
    private final Random random = new Random();

    public WardrobeListener(BodyService service, LongSupplier clock) {
        this.service = service;
        this.clock = clock;
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (event.getItem() == null) {
            return;
        }
        Player player = event.getPlayer();
        HeatTable.Buff buff = service.consumed(player, event.getItem().getType().name());
        if (buff != null && service.config().messages) {
            player.sendMessage(buff.warm() ? "§6The warmth spreads through you." : "§bThat cools you down.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        BodyService.Cause cause = service.lastCause(player.getUniqueId());
        if (cause != null && clock.getAsLong() - cause.at() <= RECENT_MS) {
            String[] lines = cause.cold() ? FROZE : COOKED;
            event.deathMessage(Component.text(String.format(lines[random.nextInt(lines.length)], player.getName())));
        }
        service.clearCause(player.getUniqueId());
        service.reset(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        service.onJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.save();
        service.forget(event.getPlayer().getUniqueId());
    }
}
