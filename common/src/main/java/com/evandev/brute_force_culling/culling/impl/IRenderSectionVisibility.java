package com.evandev.brute_force_culling.culling.impl;

public interface IRenderSectionVisibility {
    boolean bruteForceCulling$shouldCheckVisibility(int clientTick);

    void bruteForceCulling$updateVisibleTick(int clientTick);

    int bruteForceCulling$getPositionX();

    int bruteForceCulling$getPositionY();

    int bruteForceCulling$getPositionZ();
}
