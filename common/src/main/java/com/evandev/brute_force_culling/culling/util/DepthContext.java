package com.evandev.brute_force_culling.culling.util;

import com.mojang.blaze3d.pipeline.RenderTarget;

public record DepthContext(RenderTarget frame, int index, float scale, int lastTexture) {
}
