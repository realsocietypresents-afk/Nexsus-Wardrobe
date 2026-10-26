package com.nexuscraft.nexuswardrobe;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Reads NexusSeasons' {@code ClimateApi} by reflection, so the two plugins never need each other at compile time.
 * Looked up fresh each time until found, so it works whichever plugin loads first.
 */
public final class SeasonsClimate implements ClimateSource {

    static final String API = "com.nexusuniverse.seasons.climate.ClimateApi";

    private Object api;
    private Method feels;
    private Method uv;
    private Method sheltered;
    private Method precip;
    private Method claim;

    /** Finds NexusSeasons' service every time (a map lookup), so a reloaded or removed one is noticed at once. */
    private boolean bind() {
        try {
            Class<?> type = Class.forName(API);
            Object found = Bukkit.getServicesManager().load(type);
            if (found == null) {
                api = null;
                return false;
            }
            if (found != api) {
                feels = type.getMethod("feelsLikeF", Player.class);
                uv = type.getMethod("uvIndex", Player.class);
                sheltered = type.getMethod("sheltered", Player.class);
                precip = type.getMethod("precipitation", Player.class);
                claim = type.getMethod("setBodyHeatHandledElsewhere", boolean.class);
                api = found;
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError absent) {
            api = null;
            return false;
        }
    }

    public boolean available() {
        return bind();
    }

    /** Asks NexusSeasons to stand its summer heat exhaustion down (true) or take it back (false). */
    public void claimBodyHeat(boolean claimed) {
        if (bind()) {
            try {
                claim.invoke(api, claimed);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // an older NexusSeasons without the hook: both systems simply run
            }
        }
    }

    @Override
    public Ambient read(Player player) {
        if (!bind()) {
            return null;
        }
        try {
            double f = (Double) feels.invoke(api, player);
            if (Double.isNaN(f)) {
                return null;
            }
            double u = (Double) uv.invoke(api, player);
            boolean s = (Boolean) sheltered.invoke(api, player);
            String p = String.valueOf(precip.invoke(api, player));
            return new Ambient(f, Double.isNaN(u) ? 0 : u, s, p);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null; // a failing service is treated as absent for this reading; it is looked up again next time
        }
    }

    @Override
    public String name() {
        return "NexusSeasons";
    }
}
