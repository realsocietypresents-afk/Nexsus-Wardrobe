package com.nexuscraft.nexuswardrobe;

/**
 * Everything the model needs to know about the player's surroundings for one step.
 *
 * @param feelsF       what the weather feels like where they stand (already includes wind, humidity and a roof)
 * @param uv           UV index (0 at night or indoors)
 * @param precip       "NONE", "RAIN", "SNOW" or "STORM", as it falls on this player
 * @param sourceHeatF  degrees added by fires, furnaces and lava nearby
 * @param coolSourceF  degrees taken off by ice and snow blocks nearby
 */
public record Inputs(Dim dim, double feelsF, boolean sheltered, double uv, String precip, boolean inWater, boolean sprinting,
                     boolean fireResistance, double sourceHeatF, double coolSourceF, Insulation.Gear gear) {
}
