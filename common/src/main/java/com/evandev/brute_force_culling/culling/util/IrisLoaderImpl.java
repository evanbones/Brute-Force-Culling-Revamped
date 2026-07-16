package com.evandev.brute_force_culling.culling.util;

import com.evandev.brute_force_culling.Constants;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

/**
 * Only classloaded via reflection from {@code CullingStateManager.init()} when Iris is present,
 * so it is safe for this class to reference Iris types directly.
 */
public class IrisLoaderImpl implements ShaderLoader {
    private static Field renderTargetsField;

    @Override
    public int getDepthTextureID() {
        var pipelineOptional = Iris.getPipelineManager().getPipeline();
        if (pipelineOptional.isPresent()) {
            WorldRenderingPipeline pipeline = pipelineOptional.get();
            if (pipeline instanceof IrisRenderingPipeline irisPipeline) {
                try {
                    if (renderTargetsField == null) {
                        renderTargetsField = IrisRenderingPipeline.class.getDeclaredField("renderTargets");
                        renderTargetsField.setAccessible(true);
                    }
                    RenderTargets renderTargets = (RenderTargets) renderTargetsField.get(irisPipeline);
                    if (renderTargets != null) {
                        return renderTargets.getDepthTexture();
                    }
                } catch (ReflectiveOperationException e) {
                    Constants.LOG.error("Failed to read Iris render targets depth texture", e);
                }
            }
        }

        return -1;
    }

    @Override
    public boolean renderingShaderPass() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }

    @Override
    public boolean enabledShader() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    @Override
    public void bindDefaultFrameBuffer() {
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }
}
