package com.evandev.brute_force_culling.mixin;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.impl.ICullingShader;
import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin implements ICullingShader {
    @Unique
    private Uniform bruteForceCulling$cullingCameraPos;
    @Unique
    private Uniform bruteForceCulling$cullingCameraDir;
    @Unique
    private Uniform bruteForceCulling$boxScale;
    @Unique
    private Uniform bruteForceCulling$renderDistance;
    @Unique
    private Uniform bruteForceCulling$depthSize;
    @Unique
    private Uniform bruteForceCulling$cullingSize;
    @Unique
    private Uniform bruteForceCulling$entityCullingSize;
    @Unique
    private Uniform bruteForceCulling$levelHeightOffset;
    @Unique
    private Uniform bruteForceCulling$levelMinSection;
    @Unique
    private Uniform bruteForceCulling$cullingFrustum;
    @Unique
    private Uniform bruteForceCulling$frustumPos;
    @Unique
    private Uniform bruteForceCulling$cullingViewMat;
    @Unique
    private Uniform bruteForceCulling$cullingProjMat;
    @Final
    @Shadow
    private int programId;

    @Shadow
    public abstract Uniform getUniform(String name);

    @Inject(
            method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Ljava/lang/String;Lcom/mojang/blaze3d/vertex/VertexFormat;)V",
            at = @At("RETURN")
    )
    private void construct(CallbackInfo ci) {
        this.bruteForceCulling$cullingCameraPos = this.getUniform("CullingCameraPos");
        this.bruteForceCulling$cullingCameraDir = this.getUniform("CullingCameraDir");
        this.bruteForceCulling$boxScale = this.getUniform("BoxScale");
        this.bruteForceCulling$renderDistance = this.getUniform("RenderDistance");
        this.bruteForceCulling$depthSize = this.getUniform("DepthSize");
        this.bruteForceCulling$cullingSize = this.getUniform("CullingSize");
        this.bruteForceCulling$levelHeightOffset = this.getUniform("LevelHeightOffset");
        this.bruteForceCulling$levelMinSection = this.getUniform("LevelMinSection");
        this.bruteForceCulling$entityCullingSize = this.getUniform("EntityCullingSize");
        this.bruteForceCulling$cullingFrustum = this.getUniform("CullingFrustum");
        this.bruteForceCulling$frustumPos = this.getUniform("FrustumPos");
        this.bruteForceCulling$cullingViewMat = this.getUniform("CullingViewMat");
        this.bruteForceCulling$cullingProjMat = this.getUniform("CullingProjMat");
    }

    @Override
    public Uniform bruteForceCulling$getCullingFrustum() {
        return bruteForceCulling$cullingFrustum;
    }

    @Override
    public Uniform bruteForceCulling$getCullingCameraPos() {
        return bruteForceCulling$cullingCameraPos;
    }

    @Override
    public Uniform bruteForceCulling$getRenderDistance() {
        return bruteForceCulling$renderDistance;
    }

    @Override
    public Uniform bruteForceCulling$getDepthSize() {
        return bruteForceCulling$depthSize;
    }

    @Override
    public Uniform bruteForceCulling$getCullingSize() {
        return bruteForceCulling$cullingSize;
    }

    @Override
    public Uniform bruteForceCulling$getLevelHeightOffset() {
        return bruteForceCulling$levelHeightOffset;
    }

    @Override
    public Uniform bruteForceCulling$getLevelMinSection() {
        return bruteForceCulling$levelMinSection;
    }

    @Override
    public Uniform bruteForceCulling$getEntityCullingSize() {
        return bruteForceCulling$entityCullingSize;
    }

    @Override
    public Uniform bruteForceCulling$getFrustumPos() {
        return bruteForceCulling$frustumPos;
    }

    @Override
    public Uniform bruteForceCulling$getCullingViewMat() {
        return bruteForceCulling$cullingViewMat;
    }

    @Override
    public Uniform bruteForceCulling$getCullingProjMat() {
        return bruteForceCulling$cullingProjMat;
    }

    @Override
    public Uniform bruteForceCulling$getCullingCameraDir() {
        return bruteForceCulling$cullingCameraDir;
    }

    @Override
    public Uniform bruteForceCulling$getBoxScale() {
        return bruteForceCulling$boxScale;
    }

    @Inject(at = @At("TAIL"), method = "apply")
    private void onApply(CallbackInfo ci) {
        if (CullingStateManager.updatingDepth) {
            ProgramManager.glUseProgram(this.programId);
        }
    }
}
