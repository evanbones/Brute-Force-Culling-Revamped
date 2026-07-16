package com.evandev.brute_force_culling.culling.impl;

/**
 * Contract for anything that can be tested for chunk-section-shaped visibility:
 * vanilla's own render-section object, Sodium's {@code RenderSection}, and
 * {@code DummySection}.
 */
public interface IRenderSectionVisibility {
    /**
     * Whether this section's visibility still needs to be tested against the culling map for
     * the given readback generation.
     */
    boolean bruteForceCulling$shouldCheckVisibility(int clientTick);

    void bruteForceCulling$updateVisibleTick(int clientTick);

    int bruteForceCulling$getPositionX();

    int bruteForceCulling$getPositionY();

    int bruteForceCulling$getPositionZ();
}
