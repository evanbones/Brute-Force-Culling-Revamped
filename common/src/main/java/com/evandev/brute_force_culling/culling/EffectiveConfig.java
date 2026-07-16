package com.evandev.brute_force_culling.culling;

import com.evandev.brute_force_culling.config.ModConfig;

import java.util.List;

public class EffectiveConfig {
    private static final double MIN_SAMPLING = 0.05;

    private static volatile boolean loaded = false;

    public static void setLoaded() {
        loaded = true;
    }

    private static boolean unload() {
        return !loaded || !ModConfig.get().enabled;
    }

    public static double getSampling() {
        if (unload()) return 0.5;
        return Math.max(ModConfig.get().sampling, MIN_SAMPLING);
    }

    public static boolean doEntityCulling() {
        if (unload() || !CullingStateManager.gl33()) return false;
        return ModConfig.get().cullEntity || ModConfig.get().cullBlockEntity;
    }

    public static boolean getCullEntity() {
        if (unload() || !CullingStateManager.gl33()) return false;
        return ModConfig.get().cullEntity;
    }

    public static boolean getCullBlockEntity() {
        if (unload() || !CullingStateManager.gl33()) return false;
        return ModConfig.get().cullBlockEntity;
    }

    public static boolean getCullChunk() {
        if (unload()) return false;
        return ModConfig.get().cullChunk;
    }

    public static boolean shouldCullChunk() {
        if (unload()) return false;
        var chunkCullingMap = CullingStateManager.CHUNK_CULLING_MAP;
        if (chunkCullingMap == null || !chunkCullingMap.isDone()) return false;
        return ModConfig.get().cullChunk;
    }

    public static int getShaderDynamicDelay() {
        return CullingStateManager.enabledShader() ? 1 : 0;
    }

    public static int getDepthUpdateDelay() {
        if (unload()) return 1;
        int delay = ModConfig.get().updateDelay;
        return delay <= 9 ? delay + getShaderDynamicDelay() : delay;
    }

    public static List<String> getEntitiesSkip() {
        if (unload()) return List.of();
        return ModConfig.get().entitySkip;
    }

    public static List<String> getBlockEntitiesSkip() {
        if (unload()) return List.of();
        return ModConfig.get().blockEntitySkip;
    }

    public static List<String> getModsSkip() {
        if (unload()) return List.of();
        return ModConfig.get().modSkip;
    }
}
