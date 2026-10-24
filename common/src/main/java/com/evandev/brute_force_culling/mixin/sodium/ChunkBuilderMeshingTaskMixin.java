package com.evandev.brute_force_culling.mixin.sodium;

import com.evandev.brute_force_culling.culling.VegetationThinning;
import com.evandev.brute_force_culling.culling.impl.IVegetationSection;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import net.caffeinemc.mods.sodium.client.util.task.CancellationToken;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkBuilderMeshingTask.class)
public abstract class ChunkBuilderMeshingTaskMixin extends ChunkBuilderTask<ChunkBuildOutput> {
    @Unique
    private boolean bruteForceCulling$thin;
    @Unique
    private boolean bruteForceCulling$sawVegetation;

    private ChunkBuilderMeshingTaskMixin(RenderSection render) {
        super(render, 0, null);
    }

    @Inject(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", at = @At("HEAD"), remap = false)
    private void bruteForceCulling$onExecuteHead(ChunkBuildContext context, CancellationToken token, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        bruteForceCulling$thin = VegetationThinning.isFar(this.render.getChunkX(), this.render.getChunkZ());
        bruteForceCulling$sawVegetation = false;
    }

    @WrapOperation(
            method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderModel(Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"),
            remap = false
    )
    private void bruteForceCulling$thinVegetation(BlockRenderer renderer, BakedModel model, BlockState state, BlockPos pos, BlockPos origin, Operation<Void> original) {
        // Sections remember whether they hold vegetation, so only those get rebuilt when the thinning boundary moves
        if ((bruteForceCulling$thin || !bruteForceCulling$sawVegetation) && VegetationThinning.isVegetation(state)) {
            bruteForceCulling$sawVegetation = true;
            if (bruteForceCulling$thin && VegetationThinning.isHidden(state, pos)) return;
        }
        original.call(renderer, model, state, pos, origin);
    }

    @Inject(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;", at = @At("RETURN"), remap = false)
    private void bruteForceCulling$onExecuteReturn(ChunkBuildContext context, CancellationToken token, CallbackInfoReturnable<ChunkBuildOutput> cir) {
        if (cir.getReturnValue() != null) {
            ((IVegetationSection) this.render).bruteForceCulling$setVegetation(bruteForceCulling$sawVegetation, bruteForceCulling$thin);
        }
    }
}
