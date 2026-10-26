package com.nexuscraft.nexuswardrobe;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Player;

/** Looks at the blocks around a player for fires, furnaces, lava and ice. */
public final class HeatScanner {

    /** Warmth and coolness from the surroundings, in degrees. */
    public record Result(double heat, double chill) {
        public static final Result NONE = new Result(0, 0);
    }

    private final Params params;

    public HeatScanner(Params params) {
        this.params = params;
    }

    public Result scan(Player player, int radius) {
        Location at = player.getLocation();
        World world = at.getWorld();
        if (world == null) {
            return Result.NONE;
        }
        int bx = at.getBlockX();
        int by = at.getBlockY();
        int bz = at.getBlockZ();
        double heat = 0;
        double chill = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz + (dy - 0.5) * (dy - 0.5));
                    if (dist > radius + 0.5) {
                        continue;
                    }
                    Block block = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    String name = block.getType().name();
                    double h = HeatTable.heat(name, true);
                    double c = HeatTable.chill(name);
                    if (h <= 0 && c <= 0) {
                        continue;
                    }
                    if (h > 0) {
                        if (needsLight(name) && !lit(block)) {
                            h = 0;
                        }
                        heat += h * HeatTable.falloff(dist, radius);
                    }
                    if (c > 0) {
                        chill += c * HeatTable.falloff(dist, radius);
                    }
                }
            }
        }
        return new Result(HeatTable.combine(heat, params.warmthMax), HeatTable.combine(chill, 12));
    }

    private static boolean needsLight(String name) {
        return name.equals("CAMPFIRE") || name.equals("SOUL_CAMPFIRE") || name.equals("FURNACE") || name.equals("BLAST_FURNACE")
                || name.equals("SMOKER");
    }

    private static boolean lit(Block block) {
        BlockData data = block.getBlockData();
        return data instanceof Lightable l && l.isLit();
    }
}
