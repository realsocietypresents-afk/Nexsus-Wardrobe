package com.nexuscraft.nexuswardrobe;

/** Every number the heat model uses (all degrees F unless said otherwise). The defaults are the tuned values; config.yml overrides them. */
public final class Params {

    public double comfortLowBase = 62;
    public double comfortHighBase = 84;
    /** How many degrees of cold one full point of insulation covers. */
    public double insulationCold = 50;
    /** How many degrees of heat tolerance one full point of insulation costs. */
    public double insulationHeat = 14;
    public double wetInsulationLoss = 0.65;
    public double wetChillBelow = 60;
    public double wetChill = 10;
    public double coldGain = 0.22;
    public double heatGain = 0.19;
    public double sunPerUv = 0.6;
    public double metalSun = 1.5;
    public double sprintHeat = 6;
    public double waterChill = 14;
    public double fireResistRelief = 20;
    public double netherF = 96;
    public double endF = 50;
    public double tauSlow = 75;
    public double tauFast = 45;
    public double rainWet = 0.012;
    public double stormWet = 0.02;
    public double snowWet = 0.005;
    public double dryRate = 0.004;
    public double dryNearFire = 0.02;
    public double drySun = 0.006;
    public double warmthMax = 30;
    public double buffWarmMax = 20;
    public double buffCoolMax = 15;
    public double normalCore = 98.6;

    public Params copy() {
        Params p = new Params();
        p.comfortLowBase = comfortLowBase;
        p.comfortHighBase = comfortHighBase;
        p.insulationCold = insulationCold;
        p.insulationHeat = insulationHeat;
        p.wetInsulationLoss = wetInsulationLoss;
        p.wetChillBelow = wetChillBelow;
        p.wetChill = wetChill;
        p.coldGain = coldGain;
        p.heatGain = heatGain;
        p.sunPerUv = sunPerUv;
        p.metalSun = metalSun;
        p.sprintHeat = sprintHeat;
        p.waterChill = waterChill;
        p.fireResistRelief = fireResistRelief;
        p.netherF = netherF;
        p.endF = endF;
        p.tauSlow = tauSlow;
        p.tauFast = tauFast;
        p.rainWet = rainWet;
        p.stormWet = stormWet;
        p.snowWet = snowWet;
        p.dryRate = dryRate;
        p.dryNearFire = dryNearFire;
        p.drySun = drySun;
        p.warmthMax = warmthMax;
        p.buffWarmMax = buffWarmMax;
        p.buffCoolMax = buffCoolMax;
        p.normalCore = normalCore;
        return p;
    }
}
