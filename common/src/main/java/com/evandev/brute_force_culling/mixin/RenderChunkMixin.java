package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.impl.IRenderSectionVisibility;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class RenderChunkMixin implements IRenderSectionVisibility {
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
        return ((SectionRenderDispatcher.RenderSection) (Object) this).getOrigin().getX();
    }

    @Override
    public int bruteForceCulling$getPositionY() {
        return ((SectionRenderDispatcher.RenderSection) (Object) this).getOrigin().getY();
    }

    @Override
    public int bruteForceCulling$getPositionZ() {
        return ((SectionRenderDispatcher.RenderSection) (Object) this).getOrigin().getZ();
    }
}
