package com.evandev.brute_force_culling.culling.util;

import com.evandev.brute_force_culling.Constants;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

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
    public int getFrameBufferID() {
        var pipelineOptional = Iris.getPipelineManager().getPipeline();
        if (pipelineOptional.isPresent()) {
            WorldRenderingPipeline pipeline = pipelineOptional.get();
            if (pipeline instanceof IrisRenderingPipeline irisPipeline) {
                try {
                    Field field = IrisRenderingPipeline.class.getDeclaredField("sodiumTerrainPipeline");
                    field.setAccessible(true);
                    Object sodiumTerrainPipeline = field.get(irisPipeline);
                    if (sodiumTerrainPipeline != null) {
                        Field fbField = sodiumTerrainPipeline.getClass().getDeclaredField("terrainSolidFramebuffer");
                        fbField.setAccessible(true);
                        Object glFramebuffer = fbField.get(sodiumTerrainPipeline);
                        if (glFramebuffer != null) {
                            Field idField = glFramebuffer.getClass().getDeclaredField("id");
                            idField.setAccessible(true);
                            return idField.getInt(glFramebuffer);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return Minecraft.getInstance().getMainRenderTarget().frameBufferId;
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
