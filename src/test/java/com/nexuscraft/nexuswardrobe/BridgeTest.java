package com.nexuscraft.nexuswardrobe;

import org.bukkit.Bukkit;

import static com.nexuscraft.nexuswardrobe.Kit.expect;

/** The links to NexusSeasons and NexusComfort, present and absent. */
public class BridgeTest {

    public static void main(String[] args) {
        seasons();
        comfort();
        Kit.finish();
    }

    static void seasons() {
        BukkitKit.Rig absent = new BukkitKit.Rig(false, false);
        BukkitKit.TestPlayer p = BukkitKit.join("Ann", absent.world);
        SeasonsClimate none = new SeasonsClimate();
        expect(!none.available() && none.read(p) == null, "without NexusSeasons there is nothing to read");
        none.claimBodyHeat(true);
        expect(true, "and claiming heat is a harmless no-op");

        BukkitKit.Rig r = new BukkitKit.Rig();
        r.seasons.feels = 41.5;
        r.seasons.uv = 4;
        r.seasons.precip = "SNOW";
        r.seasons.sheltered = true;
        SeasonsClimate s = new SeasonsClimate();
        BukkitKit.TestPlayer q = BukkitKit.join("Bob", r.world);
        Ambient a = s.read(q);
        expect(s.available() && a != null && a.feelsF() == 41.5 && a.uv() == 4 && a.precip().equals("SNOW") && a.sheltered(), "it reads every field: " + a);
        s.claimBodyHeat(true);
        expect(r.seasons.claimed, "claiming heat reaches NexusSeasons");
        s.claimBodyHeat(false);
        expect(!r.seasons.claimed, "and giving it back too");
        BukkitKit.TestPlayer hell = BukkitKit.join("Hell", BukkitKit.nether("nether"));
        expect(s.read(hell) == null, "the Nether has no reading (NaN becomes null)");
        r.seasons.broken = true;
        expect(s.read(q) == null, "a failing service is treated as absent");
        r.seasons.broken = false;
        expect(s.read(q) != null, "and is picked up again when it recovers");
        expect(s.name().equals("NexusSeasons"), "named");
    }

    static void comfort() {
        {
            BukkitKit.Rig none = new BukkitKit.Rig(false, false);
            ComfortBridge b = new ComfortBridge();
            java.util.UUID id = java.util.UUID.randomUUID();
            expect(b.allowsBody(id) && b.preset(id).equals("standard"), "without NexusComfort everything is allowed");
            BukkitKit.Rig r = new BukkitKit.Rig();
            expect(b.allowsBody(id) && b.preset(id).equals("standard"), "standard allows body effects");
            r.comfort.body = false;
            r.comfort.preset = "gentle";
            expect(!b.allowsBody(id) && b.preset(id).equals("gentle"), "gentle does not");
            Bukkit.getServicesManager().clear();
            expect(b.allowsBody(id), "if NexusComfort disappears the bridge forgets it and allows");
        }
    }
}
