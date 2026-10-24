package com.evandev.brute_force_culling.mixin.sodium;

import com.evandev.brute_force_culling.culling.impl.IRenderSectionVisibility;
import com.evandev.brute_force_culling.culling.impl.IVegetationSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderSection.class)
public abstract class RenderSectionMixin implements IRenderSectionVisibility, IVegetationSection {
    @Unique
    private int bruteForceCulling$cullingLastVisibleFrame = -1;
    @Unique
    private volatile boolean bruteForceCulling$hasVegetation;
    @Unique
    private volatile boolean bruteForceCulling$vegetationThinned;

    @Override
    public boolean bruteForceCulling$isVisibleAtFrame(int frame) {
        return frame == bruteForceCulling$cullingLastVisibleFrame;
    }

    @Override
    public void bruteForceCulling$markVisibleAtFrame(int frame) {
        bruteForceCulling$cullingLastVisibleFrame = frame;
    }

    @Override
    public int bruteForceCulling$getPositionX() {
        return ((RenderSection) (Object) this).getOriginX();
    }

    @Override
    public int bruteForceCulling$getPositionY() {
        return ((RenderSection) (Object) this).getOriginY();
    }

    @Override
    public int bruteForceCulling$getPositionZ() {
        return ((RenderSection) (Object) this).getOriginZ();
    }

    @Override
    public boolean bruteForceCulling$hasVegetation() {
        return bruteForceCulling$hasVegetation;
    }

    @Override
    public boolean bruteForceCulling$isVegetationThinned() {
        return bruteForceCulling$vegetationThinned;
    }

    @Override
    public void bruteForceCulling$setVegetation(boolean hasVegetation, boolean thinned) {
        bruteForceCulling$hasVegetation = hasVegetation;
        bruteForceCulling$vegetationThinned = thinned;
    }
}
