package com.evandev.brute_force_culling.culling.util;

public interface ShaderLoader {
    /**
     * @return the GL texture id currently holding the scene depth, or -1 if unavailable.
     */
    int getDepthTextureID();

    boolean renderingShaderPass();

    boolean enabledShader();

    void bindDefaultFrameBuffer();
}
