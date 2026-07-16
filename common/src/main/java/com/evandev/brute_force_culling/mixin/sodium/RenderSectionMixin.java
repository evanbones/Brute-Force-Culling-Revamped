package com.evandev.brute_force_culling.mixin.sodium;

import com.evandev.brute_force_culling.culling.impl.IRenderSectionVisibility;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderSection.class)
public abstract class RenderSectionMixin implements IRenderSectionVisibility {
    @Unique
    private int bruteForceCulling$cullingLastVisibleFrame = -1;

    @Override
    public boolean bruteForceCulling$shouldCheckVisibility(int frame) {
        return frame != bruteForceCulling$cullingLastVisibleFrame;
    }

    @Override
    public void bruteForceCulling$updateVisibleTick(int frame) {
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
}
