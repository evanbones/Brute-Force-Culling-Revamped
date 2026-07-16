package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.impl.IEntitiesForRender;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelRenderer.class)
public abstract class LevelRenderMixin implements IEntitiesForRender {
    @Final
    @Shadow
    private ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections;

    @Inject(method = "applyFrustum", at = @At(value = "HEAD"))
    private void onApplyFrustumHead(Frustum frustum, CallbackInfo ci) {
        CullingStateManager.applyFrustum = true;
        CullingStateManager.updating();
        CullingStateManager.beginChunkCullingCount();
    }

    @Inject(method = "applyFrustum", at = @At(value = "RETURN"))
    private void onApplyFrustumReturn(Frustum frustum, CallbackInfo ci) {
        CullingStateManager.applyFrustum = false;
        CullingStateManager.endChunkCullingCount();
    }

    @Inject(method = "prepareCullFrustum", at = @At(value = "HEAD"))
    private void onPrepareCullFrustum(Vec3 cameraPosition, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        CullingStateManager.PROJECTION_MATRIX.set(projectionMatrix);
    }

    @Inject(method = "allChanged", at = @At(value = "TAIL"))
    private void onAllChanged(CallbackInfo ci) {
        CullingStateManager.onLevelRendererAllChanged();
    }

    @Override
    public List<SectionRenderDispatcher.RenderSection> bruteForceCulling$visibleSections() {
        return visibleSections;
    }
}
