package com.evandev.brute_force_culling.culling.util;

import com.evandev.brute_force_culling.culling.impl.IRenderSectionVisibility;

/**
 * Stand-in {@link IRenderSectionVisibility} used on the vanilla (non-Sodium) rendering path,
 * where there is no real Sodium {@code RenderSection} to attach visibility bookkeeping to.
 */
public class DummySection implements IRenderSectionVisibility {
    private int x;
    private int y;
    private int z;

    public void set(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean bruteForceCulling$shouldCheckVisibility(int clientTick) {
        return true;
    }

    @Override
    public void bruteForceCulling$updateVisibleTick(int clientTick) {
    }

    @Override
    public int bruteForceCulling$getPositionX() {
        return x;
    }

    @Override
    public int bruteForceCulling$getPositionY() {
        return y;
    }

    @Override
    public int bruteForceCulling$getPositionZ() {
        return z;
    }
}
