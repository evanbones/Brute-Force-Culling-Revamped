package com.evandev.brute_force_culling.culling.impl;

public interface IVegetationSection {
    boolean bruteForceCulling$hasVegetation();

    boolean bruteForceCulling$isVegetationThinned();

    void bruteForceCulling$setVegetation(boolean hasVegetation, boolean thinned);
}
