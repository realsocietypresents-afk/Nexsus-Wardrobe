package com.nexuscraft.nexuswardrobe;

import java.util.UUID;

/** The service other plugins see. */
final class ApiImpl implements WardrobeApi {

    private final BodyService service;

    ApiImpl(BodyService service) {
        this.service = service;
    }

    @Override
    public double coreTempF(UUID player) {
        Body b = service.peek(player);
        return b == null ? 98.6 : b.core;
    }

    @Override
    public String stage(UUID player) {
        return service.stage(player).name();
    }

    @Override
    public double wetness(UUID player) {
        Body b = service.peek(player);
        return b == null ? 0 : b.wet;
    }

    @Override
    public void warm(UUID player, double degreesF, double seconds) {
        if (degreesF > 0 && seconds > 0) {
            Thermo.warm(service.config().params, service.body(player), degreesF, seconds);
        }
    }

    @Override
    public void cool(UUID player, double degreesF, double seconds) {
        if (degreesF > 0 && seconds > 0) {
            Thermo.cool(service.config().params, service.body(player), degreesF, seconds);
        }
    }
}
