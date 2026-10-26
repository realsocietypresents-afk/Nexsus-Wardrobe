package com.nexuscraft.nexuswardrobe;

/** The whole heat model. Pure maths: no server needed. One call is one step of {@code dt} seconds. */
public final class Thermo {

    private Thermo() {
    }

    /** What happened in a step, for the status screen and for tests. */
    public record Report(double effectiveF, double comfortLowF, double comfortHighF, double coldStress, double heatStress,
                         double targetCore, double insulationNow) {
    }

    /** Degrees of warmth/cold the surroundings and gear add up to, and the comfort window for this gear. */
    public static Report assess(Params p, Body b, Inputs in) {
        double ambient = switch (in.dim()) {
            case NETHER -> p.netherF;
            case END -> p.endF;
            default -> in.feelsF();
        };
        double e = ambient;
        if (in.dim() == Dim.OVERWORLD && !in.sheltered() && in.uv() > 0) {
            double sun = in.uv() * p.sunPerUv;
            sun = Math.max(0, sun - in.gear().cooling());
            if (in.uv() >= 3) {
                sun += in.gear().metalPieces() * p.metalSun;
            }
            e += sun;
        }
        e += in.sourceHeatF() - in.coolSourceF() + b.warmNow() - b.coolNow();
        if (in.sprinting()) {
            e += p.sprintHeat;
        }
        if (in.inWater()) {
            e -= p.waterChill;
        }
        if (in.fireResistance() && in.dim() == Dim.NETHER) {
            e -= p.fireResistRelief;
        }
        if (e < p.wetChillBelow) {
            e -= p.wetChill * b.wet;
        }
        double iNow = in.gear().insulation() * (1.0 - p.wetInsulationLoss * b.wet);
        double low = p.comfortLowBase - p.insulationCold * iNow;
        double high = p.comfortHighBase - p.insulationHeat * in.gear().insulation();
        double cold = Math.max(0, low - e);
        double heat = Math.max(0, e - high);
        double target = p.normalCore - p.coldGain * cold + p.heatGain * heat;
        target = Math.max(80, Math.min(109, target));
        return new Report(e, low, high, cold, heat, target, iNow);
    }

    /** Advances the body by {@code dt} seconds and returns what it worked out. */
    public static Report step(Params p, Body b, Inputs in, double dt) {
        Report r = assess(p, b, in);
        double away = Math.abs(r.targetCore() - p.normalCore);
        double now = Math.abs(b.core - p.normalCore);
        double tau = away > now ? p.tauSlow : p.tauFast;
        b.core += (r.targetCore() - b.core) * (1.0 - Math.exp(-dt / tau));
        wetStep(p, b, in, r, dt);
        if (b.warmLeft > 0) {
            b.warmLeft = Math.max(0, b.warmLeft - dt);
            if (b.warmLeft == 0) {
                b.warmBuffF = 0;
            }
        }
        if (b.coolLeft > 0) {
            b.coolLeft = Math.max(0, b.coolLeft - dt);
            if (b.coolLeft == 0) {
                b.coolBuffF = 0;
            }
        }
        return r;
    }

    static void wetStep(Params p, Body b, Inputs in, Report r, double dt) {
        if (in.inWater()) {
            b.wet = 1.0;
            return;
        }
        double up = 0;
        switch (in.precip() == null ? "NONE" : in.precip()) {
            case "RAIN" -> up = p.rainWet;
            case "STORM" -> up = p.stormWet;
            case "SNOW" -> up = p.snowWet;
            default -> {
            }
        }
        double down = p.dryRate;
        if (in.sourceHeatF() >= 8) {
            down += p.dryNearFire;
        }
        if (up == 0 && !in.sheltered() && in.dim() == Dim.OVERWORLD && r.effectiveF() > 75) {
            down += p.drySun;
        }
        b.wet = Math.max(0, Math.min(1, b.wet + (up - (up > 0 ? 0 : down)) * dt));
    }

    /** Drinking or eating something warm: adds to any warmth already working (never past the cap). */
    public static void warm(Params p, Body b, double degreesF, double seconds) {
        b.warmBuffF = Math.min(p.buffWarmMax, Math.max(b.warmBuffF, degreesF) + (b.warmLeft > 0 ? degreesF * 0.25 : 0));
        b.warmLeft = Math.max(b.warmLeft, seconds);
    }

    public static void cool(Params p, Body b, double degreesF, double seconds) {
        b.coolBuffF = Math.min(p.buffCoolMax, Math.max(b.coolBuffF, degreesF) + (b.coolLeft > 0 ? degreesF * 0.25 : 0));
        b.coolLeft = Math.max(b.coolLeft, seconds);
    }

    /** The coldest air this gear stays comfortable in, dry. */
    public static double comfortableDownToF(Params p, Insulation.Gear gear) {
        return p.comfortLowBase - p.insulationCold * gear.insulation();
    }

    /** The hottest air this gear stays comfortable in. */
    public static double comfortableUpToF(Params p, Insulation.Gear gear) {
        return p.comfortHighBase - p.insulationHeat * gear.insulation();
    }
}
