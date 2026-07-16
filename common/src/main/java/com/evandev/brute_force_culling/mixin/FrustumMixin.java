package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.EffectiveConfig;
import com.evandev.brute_force_culling.culling.util.DummySection;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public abstract class FrustumMixin {
    @Unique
    private static final ThreadLocal<DummySection> DUMMY_SECTION = ThreadLocal.withInitial(DummySection::new);

    @Inject(method = "isVisible", at = @At(value = "RETURN"), cancellable = true)
    public void afterVisible(AABB aabb, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !CullingStateManager.applyFrustum || !EffectiveConfig.shouldCullChunk()
                || CullingStateManager.renderingShader()) {
            return;
        }
        double sizeX = aabb.maxX - aabb.minX;
        if (sizeX < 15.0 || sizeX > 17.0) {
            return;
        }

        DummySection section = DUMMY_SECTION.get();

        section.set(
                (int) ((aabb.minX + aabb.maxX) * 0.5D),
                (int) ((aabb.minY + aabb.maxY) * 0.5D),
                (int) ((aabb.minZ + aabb.maxZ) * 0.5D)
        );

        if (!CullingStateManager.shouldRenderChunk(section, true)) {
            cir.setReturnValue(false);
        }
    }
}
