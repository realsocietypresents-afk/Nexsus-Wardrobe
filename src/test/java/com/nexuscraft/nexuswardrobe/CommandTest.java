package com.nexuscraft.nexuswardrobe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static com.nexuscraft.nexuswardrobe.Kit.expect;

/** /body and /wardrobe. */
public class CommandTest {

    public static void main(String[] args) {
        body();
        wardrobe();
        completion();
        Kit.finish();
    }

    static void body() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        WardrobeCommand c = new WardrobeCommand(r.service, r.items);
        r.seasons.feels = 28;
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        r.seconds(120);
        p.messages.clear();
        expect(c.onCommand(p, BukkitKit.command("body"), "body", new String[0]), "/body works");
        String t = p.text();
        expect(t.contains("Core") && t.contains("98") || t.contains("Core"), "it shows your core: " + t);
        expect(t.contains("feels like 28") && t.contains("NexusSeasons"), "and where the weather comes from");
        expect(t.contains("Your clothes") && t.contains("comfortable from"), "and what your clothes can take");
        expect(t.contains("Wet 0%"), "and how wet you are");
        expect(t.contains("What helps") || t.contains("getting cold"), "and what to do: " + t);

        r.service.body(p.getUniqueId()).wet = 0.6;
        r.service.consumed(p, "RABBIT_STEW");
        r.world.getBlockAt(101, 70, 100).setBlockData(new BukkitKit.LitData("CAMPFIRE", true));
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("body"), "body", new String[0]);
        t = p.text();
        expect(t.contains("Wet 60%") && t.contains("Fire nearby") && t.contains("Warm food"), "fire and food show up: " + t);

        r.seasons.feels = 70;
        r.seasons.sheltered = true;
        r.seasons.precip = "NONE";
        r.world.getBlockAt(101, 70, 100).setType(Material.AIR);
        BukkitKit.Rig mild = new BukkitKit.Rig();
        WardrobeCommand mc = new WardrobeCommand(mild.service, mild.items);
        BukkitKit.TestPlayer m = BukkitKit.join("Mia", mild.world);
        mild.seasons.feels = 70;
        mild.seasons.sheltered = true;
        mc.onCommand(m, BukkitKit.command("body"), "body", new String[0]);
        expect(m.text().contains("Comfortable") && m.text().contains("under a roof"), "a mild day indoors: " + m.text());

        BukkitKit.TestPlayer other = BukkitKit.join("Bob", r.world);
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("body"), "body", new String[]{"Bob"});
        expect(p.text().contains("only look at your own"), "players cannot read each other");
        p.perms.add("nexuswardrobe.admin");
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("body"), "body", new String[]{"Bob"});
        expect(p.text().contains("Core"), "an admin can: " + p.text());
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("body"), "body", new String[]{"Nobody"});
        expect(p.text().contains("No player called Nobody"), "unknown names are said so");

        p.setGameMode(org.bukkit.GameMode.CREATIVE);
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("body"), "body", new String[0]);
        expect(p.text().contains("does not apply"), "creative players are told it does not apply");

        BukkitKit.Console console = new BukkitKit.Console();
        c.onCommand(console, BukkitKit.command("body"), "body", new String[0]);
        expect(console.said("Usage"), "the console must name a player");

        BukkitKit.Rig soft = new BukkitKit.Rig();
        soft.comfort.body = false;
        soft.seasons.feels = -5;
        WardrobeCommand sc = new WardrobeCommand(soft.service, soft.items);
        BukkitKit.TestPlayer s = BukkitKit.join("Soft", soft.world);
        soft.seconds(300);
        s.messages.clear();
        sc.onCommand(s, BukkitKit.command("body"), "body", new String[0]);
        expect(s.text().contains("only warn you"), "gentle players are told why nothing happens to them: " + s.text());

        BukkitKit.Rig hot = new BukkitKit.Rig();
        WardrobeCommand hc = new WardrobeCommand(hot.service, hot.items);
        hot.seasons.feels = 105;
        hot.seasons.uv = 10;
        BukkitKit.TestPlayer h = BukkitKit.join("Hal", hot.world);
        hot.seconds(600);
        h.messages.clear();
        hc.onCommand(h, BukkitKit.command("body"), "body", new String[0]);
        expect(h.text().contains("shade"), "heat advice mentions shade: " + h.text());

        BukkitKit.Rig netherRig = new BukkitKit.Rig();
        WardrobeCommand nc = new WardrobeCommand(netherRig.service, netherRig.items);
        BukkitKit.TestPlayer n = BukkitKit.join("Nina", BukkitKit.nether("world_nether"));
        nc.onCommand(n, BukkitKit.command("body"), "body", new String[0]);
        expect(n.text().contains("the Nether"), "the Nether is named: " + n.text());
    }

    static void wardrobe() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        WardrobeCommand c = new WardrobeCommand(r.service, r.items);
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        c.onCommand(p, BukkitKit.command("wardrobe"), "wardrobe", new String[0]);
        String t = p.text();
        for (Garment g : Garment.values()) {
            expect(t.contains(g.title()), "the list names the " + g.title());
        }
        expect(t.contains("wool") && t.contains("rabbit hide") && t.contains("string"), "with recipes");

        p.messages.clear();
        c.onCommand(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{"give", "Ann", "parka"});
        expect(p.text().contains("No permission"), "players cannot hand out clothes");
        p.perms.add("nexuswardrobe.admin");
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{"give", "Ann", "parka"});
        ItemStack got = p.getInventory().getItem(0);
        expect(got != null && r.items.garmentOf(got) == Garment.PARKA, "an admin can: " + p.text());
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{"give", "Ann"});
        expect(p.text().contains("Usage"), "usage");
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{"give", "Ann", "cape"});
        expect(p.text().contains("Unknown garment") && p.text().contains("parka"), "unknown garments list the real ones");
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{"give", "Zed", "parka"});
        expect(p.text().contains("No player called Zed"), "unknown players");

        int[] reloads = {0};
        c.setReload(() -> reloads[0]++);
        p.messages.clear();
        c.onCommand(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{"reload"});
        expect(reloads[0] == 1 && p.text().contains("reloaded"), "reload runs");
        BukkitKit.TestPlayer plain = BukkitKit.join("Bob", r.world);
        c.onCommand(plain, BukkitKit.command("wardrobe"), "wardrobe", new String[]{"reload"});
        expect(reloads[0] == 1 && plain.text().contains("No permission"), "but not for players");
    }

    static void completion() {
        BukkitKit.Rig r = new BukkitKit.Rig();
        WardrobeCommand c = new WardrobeCommand(r.service, r.items);
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", r.world);
        BukkitKit.join("Bob", r.world);
        List<String> f = c.onTabComplete(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{""});
        expect(f.equals(List.of("list")), "players see only list: " + f);
        p.perms.add("nexuswardrobe.admin");
        f = c.onTabComplete(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{""});
        expect(f.containsAll(List.of("give", "reload", "list")), "admins see more: " + f);
        f = c.onTabComplete(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{"give", ""});
        expect(f.contains("Ann") && f.contains("Bob"), "give completes players: " + f);
        f = c.onTabComplete(p, BukkitKit.command("wardrobe"), "wardrobe", new String[]{"give", "Ann", "pa"});
        expect(f.equals(List.of("parka")), "and garments by prefix: " + f);
        f = c.onTabComplete(p, BukkitKit.command("body"), "body", new String[]{"B"});
        expect(f.equals(List.of("Bob")), "/body completes players for admins: " + f);
        BukkitKit.TestPlayer q = BukkitKit.join("Quin", r.world);
        expect(c.onTabComplete(q, BukkitKit.command("body"), "body", new String[]{""}).isEmpty(), "but not for players");
        expect(c.onTabComplete(p, BukkitKit.command("wardrobe"), "wardrobe", new String[0]) != null, "no args is fine");
    }
}
