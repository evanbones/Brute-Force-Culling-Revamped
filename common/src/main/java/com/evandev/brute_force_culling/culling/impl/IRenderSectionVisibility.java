package com.evandev.brute_force_culling.culling.impl;

public interface IRenderSectionVisibility {
    boolean bruteForceCulling$isVisibleAtFrame(int frame);

    void bruteForceCulling$markVisibleAtFrame(int frame);

    int bruteForceCulling$getPositionX();

    int bruteForceCulling$getPositionY();

    int bruteForceCulling$getPositionZ();
}
