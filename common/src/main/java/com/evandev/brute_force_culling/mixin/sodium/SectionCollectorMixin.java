package com.evandev.brute_force_culling.mixin.sodium;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.EffectiveConfig;
import com.evandev.brute_force_culling.culling.impl.IRenderSectionVisibility;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SectionCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionCollector.class)
public abstract class SectionCollectorMixin {
    @Shadow(remap = false)
    public abstract void visitWithFlags(RenderSection section, int flags);

    @Inject(method = "visit(Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;)V", at = @At("HEAD"), remap = false, cancellable = true)
    private void bruteForceCulling$onVisit(RenderSection section, CallbackInfo ci) {
        if (CullingStateManager.checkCulling || CullingStateManager.renderingShader()) return;
        if (!EffectiveConfig.shouldCullChunk()) return;

        if (!CullingStateManager.shouldRenderChunk((IRenderSectionVisibility) section, true)) {
            this.visitWithFlags(section, 0);
            ci.cancel();
        }
    }
}
