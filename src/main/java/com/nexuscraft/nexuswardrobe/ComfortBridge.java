package com.nexuscraft.nexuswardrobe;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.UUID;

/** NexusComfort by reflection: may "body" things happen to this player, and which preset are they on. Everything is allowed when NexusComfort is absent. */
public final class ComfortBridge {

    static final String API = "com.nexuscraft.nexuscomfort.ComfortApi";

    private Object api;
    private Method allows;
    private Method preset;

    /** Finds NexusComfort's service every time (a map lookup), so one that is disabled or reloaded is noticed at once. */
    private boolean bind() {
        try {
            Class<?> type = Class.forName(API);
            Object found = Bukkit.getServicesManager().load(type);
            if (found == null) {
                api = null;
                return false;
            }
            if (found != api) {
                allows = type.getMethod("allows", UUID.class, String.class);
                preset = type.getMethod("preset", UUID.class);
                api = found;
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError absent) {
            api = null;
            return false;
        }
    }

    /** True if body effects may happen to this player. */
    public boolean allowsBody(UUID player) {
        if (!bind()) {
            return true;
        }
        try {
            return (Boolean) allows.invoke(api, player, "body");
        } catch (ReflectiveOperationException | RuntimeException e) {
            api = null;
            return true;
        }
    }

    /** "gentle", "standard" or "intense", or "standard" if unknown. */
    public String preset(UUID player) {
        if (!bind()) {
            return "standard";
        }
        try {
            Object p = preset.invoke(api, player);
            return p == null ? "standard" : p.toString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            api = null;
            return "standard";
        }
    }
}
