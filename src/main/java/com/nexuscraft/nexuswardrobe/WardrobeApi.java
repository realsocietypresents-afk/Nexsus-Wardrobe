package com.nexuscraft.nexuswardrobe;

import java.util.UUID;

/**
 * What other plugins may ask of NexusWardrobe. Look it up with {@code Bukkit.getServicesManager().load(WardrobeApi.class)}, or by
 * reflection on {@code com.nexuscraft.nexuswardrobe.WardrobeApi}. NexusCuisine can call {@link #warm} when a hot dish is eaten.
 */
public interface WardrobeApi {

    /** Core body temperature in degrees F (normal is 98.6). */
    double coreTempF(UUID player);

    /** The stage name: DEEP_FREEZE, FREEZING, COLD, CHILLY, COMFORTABLE, WARM, HOT, HEATSTROKE or SEVERE_HEAT. */
    String stage(UUID player);

    /** 0 (dry) to 1 (soaked). */
    double wetness(UUID player);

    /** Warms the player by this many degrees (effective) for this many seconds. Stacks gently and is capped. */
    void warm(UUID player, double degreesF, double seconds);

    /** The same, for cooling. */
    void cool(UUID player, double degreesF, double seconds);
}
