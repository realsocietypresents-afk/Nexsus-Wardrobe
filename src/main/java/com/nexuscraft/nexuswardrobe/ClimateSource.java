package com.nexuscraft.nexuswardrobe;

import org.bukkit.entity.Player;

/** Where the weather comes from. */
public interface ClimateSource {

    /** The weather around an overworld-type player, or null if it cannot be worked out. */
    Ambient read(Player player);

    /** A short name for the status screen. */
    String name();
}
