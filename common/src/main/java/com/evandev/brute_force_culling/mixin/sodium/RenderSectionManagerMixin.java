package com.evandev.brute_force_culling.mixin.sodium;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.EffectiveConfig;
import com.evandev.brute_force_culling.culling.VegetationThinning;
import com.evandev.brute_force_culling.culling.impl.IRenderSectionVisibility;
import com.evandev.brute_force_culling.culling.impl.IVegetationSection;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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

    @Shadow(remap = false)
    public abstract void scheduleRebuild(int x, int y, int z, boolean playerChanged);

    @Shadow(remap = false)
    public abstract void markGraphDirty();

    @Inject(method = "update", at = @At(value = "HEAD"), remap = false)
    private void onUpdate(Camera camera, Viewport viewport, boolean spectator, CallbackInfo ci) {
        if (!CullingStateManager.consumeForcedGraphUpdate()) {
            CullingStateManager.updating();
        }

        BlockPos cameraPos = camera.getBlockPosition();
        if (VegetationThinning.update(cameraPos.getX() >> 4, cameraPos.getZ() >> 4, Minecraft.getInstance().options.getEffectiveRenderDistance())) {
            bruteForceCulling$rebuildThinnedSections();
        }
    }

    /**
     * Rebuilds the sections holding vegetation whose thinning no longer matches their distance from the camera.
     */
    private void bruteForceCulling$rebuildThinnedSections() {
        boolean scheduled = false;
        for (RenderSection section : this.sectionByPosition.values()) {
            IVegetationSection vegetation = (IVegetationSection) section;
            if (!vegetation.bruteForceCulling$hasVegetation() || !section.isBuilt()) continue;
            if (vegetation.bruteForceCulling$isVegetationThinned() == VegetationThinning.isFar(section.getChunkX(), section.getChunkZ())) continue;
            this.scheduleRebuild(section.getChunkX(), section.getChunkY(), section.getChunkZ(), false);
            scheduled = true;
        }
        if (scheduled) this.markGraphDirty();
    }

    @Inject(method = "needsUpdate", at = @At(value = "RETURN"), remap = false, cancellable = true)
    private void onNeedsUpdate(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        if (CullingStateManager.checkCulling || CullingStateManager.renderingShader()) return;
        if (!EffectiveConfig.shouldCullChunk()) return;

        if (CullingStateManager.consumeCullingDataRefresh()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isSectionVisible", at = @At(value = "RETURN"), remap = false, cancellable = true)
    private void onIsSectionVisible(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (CullingStateManager.checkCulling || CullingStateManager.renderingShader()) return;
        if (!EffectiveConfig.shouldCullChunk()) return;

        RenderSection section = this.sectionByPosition.get(SectionPos.asLong(x, y, z));
        if (section == null) return;

        cir.setReturnValue(CullingStateManager.shouldRenderChunk((IRenderSectionVisibility) section, false));
    }
}
