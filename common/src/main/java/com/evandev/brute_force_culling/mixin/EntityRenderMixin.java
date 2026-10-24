package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderMixin {
    @ModifyReturnValue(method = "shouldRender", at = @At("RETURN"))
    private boolean onShouldRender(boolean original, @Local(argsOnly = true) Entity entity) {
        if (original && !entity.noCulling && CullingStateManager.shouldSkipEntity(entity)) {
            return false;
        }
        return original;
    }
}
