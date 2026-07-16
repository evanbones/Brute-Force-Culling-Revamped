package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public <E extends BlockEntity> void onRender(E blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (CullingStateManager.shouldSkipBlockEntity(blockEntity, null, blockEntity.getBlockPos())) {
            ci.cancel();
        }
    }
}
