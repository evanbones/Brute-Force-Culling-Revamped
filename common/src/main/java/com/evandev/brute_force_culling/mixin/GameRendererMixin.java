package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onCameraSetup(DeltaTracker deltaTracker, CallbackInfo ci) {
        GameRenderer self = (GameRenderer) (Object) this;
        var camera = self.getMainCamera();
        CullingStateManager.updateViewMatrix(camera.getPosition(), camera.getXRot(), camera.getYRot());
    }
}
