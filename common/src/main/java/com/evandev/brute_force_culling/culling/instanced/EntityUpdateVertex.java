package com.evandev.brute_force_culling.culling.instanced;

import com.evandev.brute_force_culling.culling.instanced.attribute.GLFloatVertex;

import java.nio.FloatBuffer;
import java.util.function.Consumer;

public class EntityUpdateVertex extends VertexAttrib {

    public EntityUpdateVertex(int index) {
        super(
                GLFloatVertex.createF1(index, "index"),
                GLFloatVertex.createF2(index + 1, "Size"),
                GLFloatVertex.createF3(index + 2, "EntityCenter")
        );
    }

    @Override
    public void addAttrib(Consumer<FloatBuffer> bufferConsumer) {
        bufferConsumer.accept(this.buffer);
    }

    @Override
    public void init(Consumer<FloatBuffer> bufferConsumer) {
    }

    @Override
    public boolean needUpdate() {
        return true;
    }
}
