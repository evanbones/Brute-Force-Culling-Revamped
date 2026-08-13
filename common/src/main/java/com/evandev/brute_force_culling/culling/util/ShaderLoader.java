package com.evandev.brute_force_culling.culling.util;

public interface ShaderLoader {
    int getDepthTextureID();

    int getFrameBufferID();

    boolean renderingShaderPass();

    boolean enabledShader();

    void bindDefaultFrameBuffer();
}
