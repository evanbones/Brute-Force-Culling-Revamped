package com.evandev.brute_force_culling.culling.instanced;

import com.evandev.brute_force_culling.culling.CullingRenderEvent;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import java.nio.FloatBuffer;
import java.util.function.Consumer;

public class InstanceVertexRenderer implements AutoCloseable {
    private static final String[] SAMPLER_NAMES = {
            "Sampler0", "Sampler1", "Sampler2", "Sampler3", "Sampler4", "Sampler5",
            "Sampler6", "Sampler7", "Sampler8", "Sampler9", "Sampler10", "Sampler11"
    };

    protected final VertexAttrib mainAttrib;
    protected final VertexAttrib update;
    protected final VertexFormat.Mode mode;
    protected int indexCount;
    protected int instanceCount;
    private int arrayObjectId;
    private VertexFormat.IndexType indexType;
    private boolean updating = false;

    public InstanceVertexRenderer(VertexFormat.Mode mode, VertexAttrib mainAttrib, Consumer<FloatBuffer> consumer, VertexAttrib update) {
        this.mainAttrib = mainAttrib;
        this.update = update;
        this.mode = mode;
        RenderSystem.glGenVertexArrays((id) -> this.arrayObjectId = id);
        init(consumer);
        this.indexCount = mode.indexCount(mainAttrib.vertexCount());
    }

    public void init(Consumer<FloatBuffer> buffer) {
        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        bindVertexArray();
        mainAttrib.bind();
        mainAttrib.init(buffer);
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        RenderSystem.glBindVertexArray(previousVao);
    }

    public void bind() {
        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(this.mode);
        this.indexType = autoStorageIndexBuffer.type();
        autoStorageIndexBuffer.bind(this.indexCount);
    }

    public void addInstanceAttrib(Consumer<FloatBuffer> consumer) {
        if (!updating) {
            int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
            bindVertexArray();
            update.bind();
            updating = true;
            RenderSystem.glBindVertexArray(previousVao);
            GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        }
        update.addAttrib(consumer);
        instanceCount++;
    }

    public void unbind() {
        GlStateManager._glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void enableVertexAttribArray() {
        update.enableVertexAttribArray();
        mainAttrib.enableVertexAttribArray();
    }

    public void disableVertexAttribArray() {
        mainAttrib.disableVertexAttribArray();
        update.disableVertexAttribArray();
    }

    private void bindVertexArray() {
        RenderSystem.glBindVertexArray(this.arrayObjectId);
    }

    public void drawWithShader(ShaderInstance shader) {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> this._drawWithShader(shader));
        } else {
            this._drawWithShader(shader);
        }
    }

    private void _drawWithShader(ShaderInstance shader) {
        int drawInstanceCount = this.instanceCount;
        this.instanceCount = 0;
        this.updating = false;

        if (this.indexCount == 0 || drawInstanceCount == 0) return;

        RenderSystem.assertOnRenderThread();

        int previousVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);

        for (int i = 0; i < SAMPLER_NAMES.length; ++i) {
            shader.setSampler(SAMPLER_NAMES[i], RenderSystem.getShaderTexture(i));
        }

        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (shader.COLOR_MODULATOR != null) shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        if (shader.FOG_START != null) shader.FOG_START.set(RenderSystem.getShaderFogStart());
        if (shader.FOG_END != null) shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        if (shader.FOG_COLOR != null) shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        if (shader.FOG_SHAPE != null) shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        if (shader.TEXTURE_MATRIX != null) shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        if (shader.GAME_TIME != null) shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        if (shader.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }
        if (shader.LINE_WIDTH != null && (this.mode == VertexFormat.Mode.LINES || this.mode == VertexFormat.Mode.LINE_STRIP)) {
            shader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }

        CullingRenderEvent.setUniform(shader);
        RenderSystem.setupShaderLights(shader);

        bindVertexArray();
        bind();
        update.bind();
        enableVertexAttribArray();
        shader.apply();

        GL31.glDrawElementsInstanced(this.mode.asGLMode, this.indexCount, this.indexType.asGLType, 0, drawInstanceCount);

        shader.clear();
        disableVertexAttribArray();
        unbind();

        GlStateManager._glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        RenderSystem.glBindVertexArray(previousVao);
    }

    @Override
    public void close() {
        mainAttrib.close();
        update.close();
        if (this.arrayObjectId > 0) {
            RenderSystem.glDeleteVertexArrays(this.arrayObjectId);
            this.arrayObjectId = 0;
        }
    }
}
