package com.evandev.brute_force_culling.culling.impl;

import com.mojang.blaze3d.shaders.Uniform;

public interface ICullingShader {
    Uniform bruteForceCulling$getRenderDistance();

    Uniform bruteForceCulling$getCullingCameraPos();

    Uniform bruteForceCulling$getCullingCameraDir();

    Uniform bruteForceCulling$getBoxScale();

    Uniform bruteForceCulling$getDepthSize();

    Uniform bruteForceCulling$getCullingSize();

    Uniform bruteForceCulling$getLevelHeightOffset();

    Uniform bruteForceCulling$getLevelMinSection();

    Uniform bruteForceCulling$getEntityCullingSize();

    Uniform bruteForceCulling$getCullingFrustum();

    Uniform bruteForceCulling$getFrustumPos();

    Uniform bruteForceCulling$getCullingViewMat();

    Uniform bruteForceCulling$getCullingProjMat();
}
