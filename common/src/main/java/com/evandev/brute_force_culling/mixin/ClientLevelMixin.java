package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.mixin.accessor.LivingEntityAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void bruteForceCulling$tickCulling(Entity entity, CallbackInfo ci) {
        CullingStateManager.tickEntityCount++;
        if (!CullingStateManager.isTickCullable(entity)) return;

        CullingStateManager.tickCulling++;
        // Skip the tick but keep the entity where the server says it is, so it doesn't jump when it reappears
        entity.setOldPosAndRot();
        if (entity instanceof LivingEntity && ((LivingEntityAccessor) entity).getLerpSteps() > 0) {
            LivingEntityAccessor lerp = (LivingEntityAccessor) entity;
            entity.setPos(lerp.getLerpX(), lerp.getLerpY(), lerp.getLerpZ());
            lerp.setLerpSteps(0);
        }
        ++entity.tickCount;
        ci.cancel();
    }
}
