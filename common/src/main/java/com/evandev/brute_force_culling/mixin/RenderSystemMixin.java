package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingRenderEvent;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
public class RenderSystemMixin {
    @Inject(at = @At(value = "TAIL"), method = "setupShaderLights")
    private static void shader(ShaderInstance instance, CallbackInfo ci) {
        CullingRenderEvent.setUniform(instance);
    }
}