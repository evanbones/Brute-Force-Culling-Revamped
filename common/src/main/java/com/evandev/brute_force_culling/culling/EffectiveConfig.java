package com.evandev.brute_force_culling.culling;

import com.evandev.brute_force_culling.config.ModConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EffectiveConfig {
    private static final double MIN_SAMPLING = 0.05;
    private static final Map<EntityType<?>, Boolean> ENTITY_SKIP_CACHE = new ConcurrentHashMap<>();
    private static final Map<BlockEntityType<?>, Boolean> BLOCK_ENTITY_SKIP_CACHE = new ConcurrentHashMap<>();
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

    public static long getAsyncSignalIntervalNanos() {
        if (unload()) return 0L;
        int hz = ModConfig.get().asyncSignalHz;
        return hz <= 0 ? 0L : (1_000_000_000L / hz);
    }

    public static boolean shouldSkipEntityType(EntityType<?> type) {
        Boolean cached = ENTITY_SKIP_CACHE.get(type);
        if (cached != null) return cached;
        if (unload()) return false;
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        boolean skip = getModsSkip().contains(key.getNamespace()) || getEntitiesSkip().contains(key.toString());
        ENTITY_SKIP_CACHE.put(type, skip);
        return skip;
    }

    public static boolean shouldSkipBlockEntityType(BlockEntityType<?> type) {
        Boolean cached = BLOCK_ENTITY_SKIP_CACHE.get(type);
        if (cached != null) return cached;
        if (unload()) return false;
        ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
        boolean skip = key != null && (getModsSkip().contains(key.getNamespace()) || getBlockEntitiesSkip().contains(key.toString()));
        BLOCK_ENTITY_SKIP_CACHE.put(type, skip);
        return skip;
    }

    public static void clearTypeSkipCaches() {
        ENTITY_SKIP_CACHE.clear();
        BLOCK_ENTITY_SKIP_CACHE.clear();
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
