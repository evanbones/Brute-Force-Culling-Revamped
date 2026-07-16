package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import net.minecraft.util.profiling.InactiveProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InactiveProfiler.class)
public class InactiveProfilerMixin {
    @Inject(method = "popPush(Ljava/lang/String;)V", at = @At(value = "HEAD"))
    public void onPopPush(String name, CallbackInfo ci) {
        CullingStateManager.onProfilerPopPush(name);
    }

    @Inject(method = "push(Ljava/lang/String;)V", at = @At(value = "HEAD"))
    public void onPush(String name, CallbackInfo ci) {
        if (!name.isEmpty() && name.charAt(0) == 'c') {
            CullingStateManager.onProfilerPush(name);
        }
    }
}
