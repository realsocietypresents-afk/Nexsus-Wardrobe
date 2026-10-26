package com.nexuscraft.nexuswardrobe;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

/** Every PersistentDataContainer key this plugin reads or writes (plain text, so other plugins can find them too). */
public final class WardrobeKeys {

    /** On a garment: its id ("parka"). Survives renaming and dyeing, and cannot be faked by naming a lookalike. */
    public final NamespacedKey garment = key("nexuswardrobe:garment");
    /** NexusPath's list of plugins a player has switched off, lower case, comma separated. */
    public final NamespacedKey pathHidden = key("nexuspath:hidden");

    public static final PersistentDataType<String, String> STRING = PersistentDataType.STRING;

    private static NamespacedKey key(String text) {
        NamespacedKey k = NamespacedKey.fromString(text);
        if (k == null) {
            throw new IllegalStateException("bad key " + text);
        }
        return k;
    }
}
