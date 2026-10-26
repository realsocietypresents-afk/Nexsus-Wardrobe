package com.nexuscraft.nexuswardrobe;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** The weather from vanilla alone: the biome's temperature, the time of day, and whether it is raining. Used when NexusSeasons is not installed. */
public final class BiomeClimate implements ClimateSource {

    /** Vanilla biome temperature (plains 0.8, desert 2.0, snowy plains 0.0, frozen peaks -0.7) to degrees F. */
    static double biomeToF(double temperature) {
        return 32.0 + temperature * 35.0;
    }

    @Override
    public Ambient read(Player player) {
        World world = player.getWorld();
        Block block = player.getLocation().getBlock();
        double f = biomeToF(block.getTemperature());
        long t = world.getTime();
        boolean day = t < 12000 || t >= 23000;
        if (!day) {
            f -= 12;
        }
        boolean sheltered = world.getHighestBlockYAt(block.getX(), block.getZ()) > block.getY() + 1;
        String precip = "NONE";
        if (!sheltered && (world.hasStorm() || world.isThundering())) {
            precip = block.getTemperature() <= 0.15 ? "SNOW" : world.isThundering() ? "STORM" : "RAIN";
            f -= 5;
        }
        if (sheltered) {
            f = f + (68 - f) * 0.5;
        }
        double uv = day && !sheltered && !"STORM".equals(precip) && !"RAIN".equals(precip) ? 6 : 0;
        return new Ambient(f, uv, sheltered, precip);
    }

    @Override
    public String name() {
        return "vanilla biome";
    }
}
