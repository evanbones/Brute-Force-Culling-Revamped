package com.evandev.brute_force_culling.culling.instanced;

import com.evandev.brute_force_culling.culling.instanced.attribute.GLFloatVertex;
import com.evandev.brute_force_culling.culling.instanced.attribute.GLVertex;

import java.nio.FloatBuffer;
import java.util.function.Consumer;

public class PixelVertexBuffer extends VertexAttrib {

    private final int componentStride;

    public PixelVertexBuffer(int index) {
        super(GLFloatVertex.createF2(index, "Position"));

        int stride = 0;
        for (GLVertex vertex : vertices) {
            stride += vertex.size();
        }
        this.componentStride = stride;
    }

    @Override
    public void addAttrib(Consumer<FloatBuffer> bufferConsumer) {
    }

    @Override
    public void init(Consumer<FloatBuffer> bufferConsumer) {
        bufferConsumer.accept(this.buffer);
        setVertexCount(this.buffer.limit() / componentStride);
    }

    @Override
    public boolean needUpdate() {
        return false;
    }
}
