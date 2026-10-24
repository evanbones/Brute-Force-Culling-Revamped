package com.evandev.brute_force_culling.mixin.accessor;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("lerpSteps")
    int getLerpSteps();

    @Accessor("lerpSteps")
    void setLerpSteps(int steps);

    @Accessor("lerpX")
    double getLerpX();

    @Accessor("lerpY")
    double getLerpY();

    @Accessor("lerpZ")
    double getLerpZ();
}
