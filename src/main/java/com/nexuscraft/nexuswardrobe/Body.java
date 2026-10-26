package com.nexuscraft.nexuswardrobe;

/** One player's body: core temperature, how wet they are, and any warm or cool drink still working. */
public final class Body {

    public double core = 98.6;
    /** 0 dry to 1 soaked. */
    public double wet;
    public double warmBuffF;
    public double warmLeft;
    public double coolBuffF;
    public double coolLeft;

    /** The warm drink's current strength: full until its last 20 seconds, then fading out. */
    public double warmNow() {
        return warmLeft <= 0 ? 0 : warmBuffF * Math.min(1.0, warmLeft / 20.0);
    }

    public double coolNow() {
        return coolLeft <= 0 ? 0 : coolBuffF * Math.min(1.0, coolLeft / 20.0);
    }

    public Stage stage() {
        return Stage.of(core);
    }

    public Body copy() {
        Body b = new Body();
        b.core = core;
        b.wet = wet;
        b.warmBuffF = warmBuffF;
        b.warmLeft = warmLeft;
        b.coolBuffF = coolBuffF;
        b.coolLeft = coolLeft;
        return b;
    }
}
