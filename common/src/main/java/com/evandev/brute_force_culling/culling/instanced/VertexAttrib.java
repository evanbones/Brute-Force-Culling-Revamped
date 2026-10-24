package com.evandev.brute_force_culling.culling.instanced;

import com.evandev.brute_force_culling.culling.instanced.attribute.GLVertex;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL15.*;

public abstract class VertexAttrib implements AutoCloseable {
    protected final List<GLVertex> vertices;
    protected final int vertexID;
    protected final int vertexSize;
    protected final FloatBuffer buffer;
    private int vertexCount;

    public VertexAttrib(int floatCapacity, GLVertex... vertices) {
        this.vertices = List.of(vertices);
        this.buffer = MemoryUtil.memAllocFloat(floatCapacity);
        this.vertexID = GlStateManager._glGenBuffers();

        int size = 0;
        for (GLVertex vertex : this.vertices) {
            size += vertex.size() * vertex.elementType().size();
        }
        this.vertexSize = size;
    }

    public int vertexCount() {
        return vertexCount;
    }

    public void setVertexCount(int count) {
        this.vertexCount = count;
    }

    public abstract void init(Consumer<FloatBuffer> bufferConsumer);

    public abstract void addAttrib(Consumer<FloatBuffer> bufferConsumer);

    public void bind() {
        GL15.glBindBuffer(GL_ARRAY_BUFFER, vertexID);
    }

    public void enableVertexAttribArray() {
        bind();

        boolean update = needUpdate();

        if (update) {
            buffer.flip();
            GL15.glBufferData(GL_ARRAY_BUFFER, buffer, GL_STREAM_DRAW);
        }

        int offset = 0;
        for (GLVertex vertex : vertices) {
            GlStateManager._vertexAttribPointer(vertex.index(), vertex.size(), vertex.elementType().glType(), false, vertexSize, offset);
            GlStateManager._enableVertexAttribArray(vertex.index());
            if (update) GL33.glVertexAttribDivisor(vertex.index(), 1);
            offset += vertex.size() * vertex.elementType().size();
        }
    }

    public void disableVertexAttribArray() {
        buffer.clear();
        for (GLVertex vertex : vertices) {
            GlStateManager._disableVertexAttribArray(vertex.index());
        }
    }

    public abstract boolean needUpdate();

    @Override
    public void close() {
        RenderSystem.glDeleteBuffers(this.vertexID);
        MemoryUtil.memFree(this.buffer);
    }
}
