package com.evandev.brute_force_culling.mixin.sodium;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.EffectiveConfig;
import com.evandev.brute_force_culling.culling.impl.IRenderSectionVisibility;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OcclusionCuller.class)
public abstract class OcclusionCullerMixin {
    @Inject(method = "isSectionVisible", at = @At(value = "RETURN"), remap = false, cancellable = true)
    private static void onIsSectionVisible(RenderSection section, Viewport viewport, float maxDistance, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (CullingStateManager.checkCulling) {
            cir.setReturnValue(true);
            return;
        }
        if (EffectiveConfig.shouldCullChunk() && !CullingStateManager.shouldRenderChunk((IRenderSectionVisibility) section, true)) {
            cir.setReturnValue(false);
        }
    }
}
