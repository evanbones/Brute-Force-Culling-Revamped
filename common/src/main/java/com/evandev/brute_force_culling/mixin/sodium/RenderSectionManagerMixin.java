package com.evandev.brute_force_culling.mixin.sodium;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.EffectiveConfig;
import com.evandev.brute_force_culling.culling.impl.IRenderSectionVisibility;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.client.Camera;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionManager.class)
public abstract class RenderSectionManagerMixin {
    @Shadow(remap = false)
    @Final
    private Long2ReferenceMap<RenderSection> sectionByPosition;

    @Inject(method = "update", at = @At(value = "HEAD"), remap = false)
    private void onUpdate(Camera camera, Viewport viewport, boolean spectator, CallbackInfo ci) {
        CullingStateManager.updating();
        CullingStateManager.beginChunkCullingCount();
    }

    @Inject(method = "update", at = @At(value = "TAIL"), remap = false)
    private void afterUpdate(Camera camera, Viewport viewport, boolean spectator, CallbackInfo ci) {
        CullingStateManager.endChunkCullingCount();
    }

    @Inject(method = "isSectionVisible", at = @At(value = "RETURN"), remap = false, cancellable = true)
    private void onIsSectionVisible(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (!EffectiveConfig.shouldCullChunk()) return;

        RenderSection section = this.sectionByPosition.get(SectionPos.asLong(x, y, z));
        if (section == null) return;

        cir.setReturnValue(
                CullingStateManager.shouldRenderChunk((IRenderSectionVisibility) section, false)
                        && CullingStateManager.FRUSTUM.isVisible(new AABB(
                        section.getOriginX(), section.getOriginY(), section.getOriginZ(),
                        section.getOriginX() + 16, section.getOriginY() + 16, section.getOriginZ() + 16))
        );
    }
}
