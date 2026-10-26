package com.nexuscraft.nexuswardrobe;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static com.nexuscraft.nexuswardrobe.Kit.expect;

/** Eating and drinking, dying of the cold, coming and going. */
public class ListenerTest {

    public static void main(String[] args) {
        food();
        death();
        comeAndGo();
        api();
        Kit.finish();
    }

    static String plain(PlayerDeathEvent e) {
        return e.deathMessage() == null ? "" : e.deathMessage().toString();
    }

    static void food() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        WardrobeListener l = new WardrobeListener(r.service, r.now::get);
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        l.onConsume(new PlayerItemConsumeEvent(p, new ItemStack(Material.RABBIT_STEW, 1), org.bukkit.inventory.EquipmentSlot.HAND));
        expect(p.text().contains("warmth spreads"), "a stew warms you and says so: " + p.text());
        expect(r.service.body(p.getUniqueId()).warmBuffF > 10, "and it counts");
        p.messages.clear();
        l.onConsume(new PlayerItemConsumeEvent(p, new ItemStack(Material.MELON_SLICE, 1), org.bukkit.inventory.EquipmentSlot.HAND));
        expect(p.text().contains("cools you"), "a melon cools you");
        p.messages.clear();
        l.onConsume(new PlayerItemConsumeEvent(p, new ItemStack(Material.DIRT, 1), org.bukkit.inventory.EquipmentSlot.HAND));
        expect(p.messages.isEmpty(), "plain food says nothing");

        BukkitKit.Rig quiet = new BukkitKit.Rig();
        quiet.plugin.getConfig().set("messages", false);
        quiet.rebuild();
        WardrobeListener ql = new WardrobeListener(quiet.service, quiet.now::get);
        BukkitKit.TestPlayer q = BukkitKit.join("Quiet", quiet.world);
        ql.onConsume(new PlayerItemConsumeEvent(q, new ItemStack(Material.RABBIT_STEW, 1), org.bukkit.inventory.EquipmentSlot.HAND));
        expect(q.messages.isEmpty() && quiet.service.body(q.getUniqueId()).warmBuffF > 0, "messages: false stays quiet but still warms");
    }

    static void death() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        WardrobeListener l = new WardrobeListener(r.service, r.now::get);
        r.seasons.feels = -20;
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        r.service.body(p.getUniqueId()).core = 85.0;
        r.seconds(10);
        PlayerDeathEvent e = new PlayerDeathEvent(p, List.of(), 0, Component.text("Ann died"));
        l.onDeath(e);
        String msg = plain(e);
        expect(msg.contains("Ann") && !msg.equals("Ann died") && (msg.contains("froze") || msg.contains("icicle") || msg.contains("fire") || msg.contains("coat")),
                "dying of the cold has its own death message: " + msg);
        expect(r.service.stage(p.getUniqueId()) == Stage.COMFORTABLE, "respawning resets the body");
        expect(r.service.lastCause(p.getUniqueId()) == null, "and forgets the cause");

        BukkitKit.Rig hot = new BukkitKit.Rig();
        WardrobeListener hl = new WardrobeListener(hot.service, hot.now::get);
        hot.seasons.feels = 110;
        hot.seasons.uv = 10;
        BukkitKit.TestPlayer h = BukkitKit.join("Hal", hot.world);
        hot.service.body(h.getUniqueId()).core = 106.0;
        hot.seconds(10);
        PlayerDeathEvent he = new PlayerDeathEvent(h, List.of(), 0, Component.text("Hal died"));
        hl.onDeath(he);
        expect(plain(he).contains("Hal") && (plain(he).contains("heat") || plain(he).contains("cooked") || plain(he).contains("overheated") || plain(he).contains("shade")),
                "and so does heat: " + plain(he));

        // an unrelated death is left alone
        BukkitKit.Rig other = new BukkitKit.Rig();
        WardrobeListener ol = new WardrobeListener(other.service, other.now::get);
        BukkitKit.TestPlayer o = BukkitKit.join("Ola", other.world);
        PlayerDeathEvent oe = new PlayerDeathEvent(o, List.of(), 0, Component.text("Ola fell from a high place"));
        ol.onDeath(oe);
        expect(plain(oe).equals("Ola fell from a high place"), "other deaths keep the vanilla message");

        // an old injury does not claim a later death
        BukkitKit.Rig stale = new BukkitKit.Rig();
        WardrobeListener sl = new WardrobeListener(stale.service, stale.now::get);
        stale.seasons.feels = -20;
        BukkitKit.TestPlayer s = BukkitKit.join("Sam", stale.world);
        stale.service.body(s.getUniqueId()).core = 85.0;
        stale.seconds(10);
        stale.now.addAndGet(60_000);
        PlayerDeathEvent se = new PlayerDeathEvent(s, List.of(), 0, Component.text("Sam was slain by Zombie"));
        sl.onDeath(se);
        expect(plain(se).equals("Sam was slain by Zombie"), "a death a minute after the last chill is somebody else's doing");
    }

    static void comeAndGo() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        WardrobeListener l = new WardrobeListener(r.service, r.now::get);
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        r.service.body(p.getUniqueId()).core = 92.0;
        l.onQuit(new PlayerQuitEvent(p, ""));
        expect(r.service.peek(p.getUniqueId()) == null, "leaving forgets the live body");
        expect(new java.io.File(r.dir, "bodies.yml").exists(), "but writes it down");
        BukkitBack.rejoin(r, l, p);
    }

    /** Logging back in finds the saved body, slightly recovered by the time away. */
    static final class BukkitBack {
        static void rejoin(BukkitKit.Rig r, WardrobeListener l, BukkitKit.TestPlayer p) {
            BukkitKit.Rig again = new BukkitKit.Rig().useFolder(r.dir);
            again.service.load();
            WardrobeListener al = new WardrobeListener(again.service, again.now::get);
            again.now.addAndGet(120_000);
            al.onJoin(new PlayerJoinEvent(p, ""));
            Body b = again.service.peek(p.getUniqueId());
            expect(b != null && b.core > 92.5 && b.core < 98.0, "two minutes away: a little better, not cured: " + (b == null ? null : b.core));
        }
    }

    static void api() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        WardrobeApi api = new ApiImpl(r.service);
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        expect(api.coreTempF(p.getUniqueId()) == 98.6 && api.stage(p.getUniqueId()).equals("COMFORTABLE") && api.wetness(p.getUniqueId()) == 0,
                "an unknown player is simply fine");
        api.warm(p.getUniqueId(), 12, 100);
        expect(r.service.body(p.getUniqueId()).warmBuffF == 12, "another plugin can warm a player");
        api.cool(p.getUniqueId(), 6, 50);
        expect(r.service.body(p.getUniqueId()).coolBuffF == 6, "or cool them");
        api.warm(p.getUniqueId(), -5, 100);
        api.warm(p.getUniqueId(), 5, 0);
        expect(r.service.body(p.getUniqueId()).warmBuffF == 12, "nonsense amounts are ignored");
        r.service.body(p.getUniqueId()).core = 91.0;
        r.service.body(p.getUniqueId()).wet = 0.5;
        expect(api.stage(p.getUniqueId()).equals("FREEZING") && api.coreTempF(p.getUniqueId()) == 91.0 && api.wetness(p.getUniqueId()) == 0.5,
                "and read the state");
    }
}
