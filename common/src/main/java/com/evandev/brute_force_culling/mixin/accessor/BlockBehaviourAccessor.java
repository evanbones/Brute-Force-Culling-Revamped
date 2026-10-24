package com.evandev.brute_force_culling.mixin.accessor;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.class)
public interface BlockBehaviourAccessor {
    @Accessor("hasCollision")
    boolean bruteForceCulling$hasCollision();
}
