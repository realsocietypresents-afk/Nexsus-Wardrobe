package com.nexuscraft.nexuswardrobe;

/** The weather where a player stands, in the units the model uses. */
public record Ambient(double feelsF, double uv, boolean sheltered, String precip) {
}
