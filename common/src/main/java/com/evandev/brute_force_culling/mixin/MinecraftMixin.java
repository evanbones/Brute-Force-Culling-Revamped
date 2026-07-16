package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V", shift = At.Shift.AFTER))
    private void afterRunTick(boolean renderLevel, CallbackInfo ci) {
        CullingStateManager.onProfilerPopPush("afterRunTick");
    }

    @Inject(method = "updateLevelInEngines", at = @At(value = "HEAD"))
    private void onUpdateLevelInEngines(ClientLevel level, CallbackInfo ci) {
        if (level == null) {
            CullingStateManager.cleanup();
        }
    }
}
