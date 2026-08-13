package com.evandev.brute_force_culling.culling;

import com.evandev.brute_force_culling.Constants;
import com.evandev.brute_force_culling.culling.data.ChunkCullingMap;
import com.evandev.brute_force_culling.culling.data.EntityCullingMap;
import com.evandev.brute_force_culling.culling.impl.IEntitiesForRender;
import com.evandev.brute_force_culling.culling.impl.IRenderSectionVisibility;
import com.evandev.brute_force_culling.culling.util.DepthContext;
import com.evandev.brute_force_culling.culling.util.LifeTimer;
import com.evandev.brute_force_culling.culling.util.ShaderLoader;
import com.evandev.brute_force_culling.mixin.accessor.LevelRenderAccessor;
import com.evandev.brute_force_culling.mixin.accessor.MinecraftAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Checks;

import java.io.IOException;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class CullingStateManager {
    public static final int DEPTH_SIZE = 5;
    public static final LifeTimer<Entity> visibleEntity = new LifeTimer<>();
    public static final LifeTimer<BlockPos> visibleBlock = new LifeTimer<>();
    private static final it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap SHADER_DEPTH_BUFFER_ID = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();
    public static volatile EntityCullingMap ENTITY_CULLING_MAP = null;
    public static volatile ChunkCullingMap CHUNK_CULLING_MAP = null;
    public static Matrix4f VIEW_MATRIX = new Matrix4f();
    public static Matrix4f PROJECTION_MATRIX = new Matrix4f();
    public static int DEPTH_INDEX;
    public static int MAIN_DEPTH_TEXTURE = 0;
    public static RenderTarget[] DEPTH_BUFFER_TARGET = new RenderTarget[DEPTH_SIZE];
    public static RenderTarget CHUNK_CULLING_MAP_TARGET;
    public static RenderTarget ENTITY_CULLING_MAP_TARGET;
    public static ShaderInstance CHUNK_CULLING_SHADER;
    public static ShaderInstance COPY_DEPTH_SHADER;
    public static ShaderInstance INSTANCED_ENTITY_CULLING_SHADER;
    public static Frustum FRUSTUM;
    public static boolean updatingDepth;
    public static boolean applyFrustum;
    public static int DEBUG = 0;
    public static int[] DEPTH_TEXTURE = new int[DEPTH_SIZE];
    public static ShaderLoader SHADER_LOADER = null;
    public static int fps = 0;
    public static int clientTickCount = 0;
    public static int entityCulling = 0;
    public static int entityCount = 0;
    public static int blockCulling = 0;
    public static int blockCount = 0;
    public static int chunkCulling = 0;
    public static int chunkCount = 0;
    public static long entityCullingTime = 0;
    public static long blockCullingTime = 0;
    public static long chunkCullingTime = 0;
    public static long chunkCullingInitTime = 0;
    public static long preChunkCullingInitTime = 0;
    public static long entityCullingInitTime = 0;
    public static long preEntityCullingInitTime = 0;
    public static int cullingInitCount = 0;
    public static int preCullingInitCount = 0;
    public static boolean checkCulling = false;
    public static int LEVEL_SECTION_RANGE;
    public static int LEVEL_POS_RANGE;
    public static int LEVEL_MIN_SECTION_ABS;
    public static int LEVEL_MIN_POS;
    public static Camera CAMERA;
    public static volatile boolean useOcclusionCulling = true;
    private static boolean isNewTickFrame = false;
    private static boolean isNextLoopFrame = false;
    private static int tick = 0;
    private static int preChunkCulling = 0;
    private static int preChunkCount = 0;
    private static long preEntityCullingTime = 0;
    private static long preBlockCullingTime = 0;
    private static long preChunkCullingTime = 0;
    private static boolean usingShader = false;
    private static double invLevelPosRange = 0;
    private static int frame;
    private static int lastVisibleUpdatedFrame;
    private static int continueUpdateCount;
    private static boolean lastUpdate;
    private static double cachedRenderDistanceSq = 0;
    private static int gl33 = -1;

    static {
        PROJECTION_MATRIX.identity();
    }

    static {
        RenderSystem.recordRenderCall(() -> {
            Minecraft mc = Minecraft.getInstance();
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            for (int i = 0; i < DEPTH_BUFFER_TARGET.length; ++i) {
                DEPTH_BUFFER_TARGET[i] = new TextureTarget(w, h, false, Minecraft.ON_OSX);
                DEPTH_BUFFER_TARGET[i].setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            }
            CHUNK_CULLING_MAP_TARGET = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            CHUNK_CULLING_MAP_TARGET.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            ENTITY_CULLING_MAP_TARGET = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            ENTITY_CULLING_MAP_TARGET.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        });
    }

    public static void toggleDebug() {
        DEBUG = DEBUG == 0 ? 1 : 0;
    }

    public static void init() {
        RenderSystem.recordRenderCall(CullingStateManager::initShader);

        if (ModIntegrationUtil.hasIris()) {
            try {
                SHADER_LOADER = (ShaderLoader) Class.forName("com.evandev.brute_force_culling.culling.util.IrisLoaderImpl")
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (Exception e) {
                Constants.LOG.error("Failed to load IrisLoaderImpl", e);
            }
        }
    }

    private static void initShader() {
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            CHUNK_CULLING_SHADER = new ShaderInstance(rm, Constants.MOD_ID + "_chunk_culling", com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION);
            INSTANCED_ENTITY_CULLING_SHADER = new ShaderInstance(rm, Constants.MOD_ID + "_instanced_entity_culling", com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION);
            COPY_DEPTH_SHADER = new ShaderInstance(rm, Constants.MOD_ID + "_copy_depth", com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void cleanup() {
        tick = 0;
        clientTickCount = 0;
        visibleEntity.clear();
        visibleBlock.clear();

        ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
        if (chunkMap != null) {
            chunkMap.cleanup();
            CHUNK_CULLING_MAP = null;
        }

        EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap != null) {
            entityMap.cleanup();
            ENTITY_CULLING_MAP = null;
        }

        SHADER_DEPTH_BUFFER_ID.clear();
    }

    public static int getKeepAliveTicks() {
        int f = Math.max(fps, 1);
        int ticks = (80 + f - 1) / f;
        return Math.max(3, Math.min(ticks, 20));
    }

    public static int mapChunkY(double posY) {
        if (LEVEL_POS_RANGE == 0) return 0;
        double offset = posY - LEVEL_MIN_POS;
        return (int) Math.floor(offset * invLevelPosRange * LEVEL_SECTION_RANGE);
    }

    public static boolean shouldRenderChunk(IRenderSectionVisibility section, boolean checkForChunk) {
        if (section == null) return false;

        if (renderingShader()) return true;

        final ChunkCullingMap map = CHUNK_CULLING_MAP;
        if (map == null) return true;

        if (!useOcclusionCulling) return true;

        preChunkCount++;
        if (!section.bruteForceCulling$shouldCheckVisibility(lastVisibleUpdatedFrame)) return true;

        final boolean timing = DEBUG > 0;
        long time = timing ? System.nanoTime() : 0;
        boolean visible = map.isChunkOffsetCameraVisible(
                section.bruteForceCulling$getPositionX(),
                section.bruteForceCulling$getPositionY(),
                section.bruteForceCulling$getPositionZ(),
                checkForChunk);
        if (timing) preChunkCullingTime += System.nanoTime() - time;

        if (visible) {
            section.bruteForceCulling$updateVisibleTick(lastVisibleUpdatedFrame);
            return true;
        }
        preChunkCulling++;
        return false;
    }

    public static void beginChunkCullingCount() {
        preChunkCulling = 0;
        preChunkCount = 0;
        preChunkCullingTime = 0;
    }

    public static void endChunkCullingCount() {
        chunkCulling = preChunkCulling;
        chunkCount = preChunkCount;
        chunkCullingTime = preChunkCullingTime;
    }

    public static boolean shouldSkipBlockEntity(BlockEntity blockEntity, AABB aabb, BlockPos pos) {
        if (renderingShader()) return false;

        blockCount++;

        final Vec3 camPos = CAMERA.getPosition();
        final double dx = pos.getX() - camPos.x;
        final double dy = pos.getY() - camPos.y;
        final double dz = pos.getZ() - camPos.z;
        final double distSq = dx * dx + dy * dy + dz * dz;

        if (distSq > cachedRenderDistanceSq) return false;

        final EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap == null || !EffectiveConfig.getCullBlockEntity()) return false;

        if (EffectiveConfig.shouldSkipBlockEntityType(blockEntity.getType())) return false;

        final boolean timing = DEBUG > 0;
        long time = timing ? System.nanoTime() : 0;
        boolean actualVisible = entityMap.isObjectVisible(blockEntity);
        if (timing) preBlockCullingTime += System.nanoTime() - time;

        boolean visible = actualVisible || visibleBlock.contains(pos);

        if (checkCulling) visible = !visible;

        if (!visible) {
            blockCulling++;
        } else if (actualVisible) {
            visibleBlock.updateUsageTick(pos, clientTickCount);
        }

        return !visible;
    }

    public static boolean shouldSkipEntity(Entity entity) {
        if (renderingShader()) return false;

        entityCount++;
        if (entity instanceof Player || entity.isCurrentlyGlowing()) return false;
        if (entity.distanceToSqr(CAMERA.getPosition()) < 4.0) return false;

        if (EffectiveConfig.shouldSkipEntityType(entity.getType())) return false;

        final EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap == null || !EffectiveConfig.getCullEntity()) return false;

        final boolean timing = DEBUG > 0;
        long time = timing ? System.nanoTime() : 0;
        boolean actualVisible = entityMap.isObjectVisible(entity);
        if (timing) preEntityCullingTime += System.nanoTime() - time;

        boolean visible = actualVisible || visibleEntity.contains(entity);

        if (checkCulling) visible = !visible;

        if (!visible) {
            entityCulling++;
        } else if (actualVisible) {
            visibleEntity.updateUsageTick(entity, clientTickCount);
        }

        return !visible;
    }

    public static void onLevelRendererAllChanged() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;

        LEVEL_SECTION_RANGE = level.getMaxSection() - level.getMinSection();
        LEVEL_MIN_SECTION_ABS = Math.abs(level.getMinSection());
        LEVEL_MIN_POS = level.getMinBuildHeight();
        LEVEL_POS_RANGE = level.getMaxBuildHeight() - level.getMinBuildHeight();
        invLevelPosRange = (LEVEL_POS_RANGE != 0) ? (1.0 / LEVEL_POS_RANGE) : 0;
    }

    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            clientTickCount++;
            ChunkCullingMap chunkCullingMap = CHUNK_CULLING_MAP;
            if (chunkCullingMap != null && !chunkCullingMap.isDone()) {
                if (mc.player.tickCount > 60 || clientTickCount > 60) {
                    chunkCullingMap.setDone();
                    onLevelRendererAllChanged();
                }
            }
        } else {
            cleanup();
        }
    }

    public static void onProfilerPopPush(String s) {
        Minecraft mc = Minecraft.getInstance();
        switch (s) {
            case "afterRunTick" -> {
                ++frame;
                updateMapData();
            }
            case "captureFrustum" -> {
                LevelRenderAccessor levelFrustum = (LevelRenderAccessor) mc.levelRenderer;
                Frustum frustum = levelFrustum.getCapturedFrustum() != null
                        ? levelFrustum.getCapturedFrustum()
                        : levelFrustum.getCullingFrustum();
                CullingStateManager.FRUSTUM = new Frustum(frustum).offsetToFullyIncludeCameraCube(32);

                ChunkCullingMap chunkMap = CullingStateManager.CHUNK_CULLING_MAP;
                if (chunkMap != null) chunkMap.updateCamera();
                checkShader();
            }
            case "destroyProgress" -> {
                updatingDepth = true;
                updateDepthMap();
                readMapData();
                CullingRenderEvent.updateCullingMap();
                updatingDepth = false;
            }
            default -> {
            }
        }
    }

    public static void onProfilerPush(String s) {
        if (s.equals("center")) {
            Minecraft mc = Minecraft.getInstance();
            EffectiveConfig.setLoaded();
            CAMERA = mc.gameRenderer.getMainCamera();
            int thisTick = clientTickCount % 20;

            isNewTickFrame = (tick != thisTick);
            isNextLoopFrame = (isNewTickFrame && thisTick == 0);

            if (isNewTickFrame) {
                tick = thisTick;
            }

            entityCulling = 0;
            entityCount = 0;
            blockCulling = 0;
            blockCount = 0;

            double renderDist = mc.options.getEffectiveRenderDistance() * 16.0;
            cachedRenderDistanceSq = renderDist * renderDist * 2.0;

            if (isNewTickFrame) {
                if (continueUpdateCount > 0) continueUpdateCount--;
            }

            if (isNextLoopFrame) {
                int keepAlive = getKeepAliveTicks();
                visibleBlock.tick(clientTickCount, keepAlive);
                visibleEntity.tick(clientTickCount, keepAlive);

                final EntityCullingMap entityMap = ENTITY_CULLING_MAP;
                if (entityMap != null) entityMap.getEntityTable().tickTemp(clientTickCount, keepAlive);

                entityCullingTime = preEntityCullingTime;
                preEntityCullingTime = 0;
                blockCullingTime = preBlockCullingTime;
                preBlockCullingTime = 0;
                chunkCullingInitTime = preChunkCullingInitTime;
                preChunkCullingInitTime = 0;
                cullingInitCount = preCullingInitCount;
                preCullingInitCount = 0;
                entityCullingInitTime = preEntityCullingInitTime;
                preEntityCullingInitTime = 0;

                final ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
                if (chunkMap != null) {
                    chunkMap.lastQueueUpdateCount = chunkMap.queueUpdateCount;
                    chunkMap.queueUpdateCount = 0;
                }
            }
        }
    }

    public static void readMapData() {
        if (checkCulling) return;

        final ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
        if (EffectiveConfig.getCullChunk() && chunkMap != null && chunkMap.isTransferred()) {
            long time = System.nanoTime();
            chunkMap.readData();
            lastVisibleUpdatedFrame = frame;
            preChunkCullingInitTime += System.nanoTime() - time;
        }

        final EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (EffectiveConfig.doEntityCulling() && entityMap != null && entityMap.isTransferred()) {
            long time = System.nanoTime();
            entityMap.readData();
            lastVisibleUpdatedFrame = frame;
            preEntityCullingInitTime += System.nanoTime() - time;
        }
    }

    public static void checkShader() {
        if (SHADER_LOADER != null) {
            boolean currentEnabled = SHADER_LOADER.enabledShader();
            if (currentEnabled != usingShader) {
                usingShader = currentEnabled;
                cleanup();
            }
        }
    }

    public static void updateViewMatrix(Vec3 pos, float xRot, float yRot) {
        com.mojang.blaze3d.vertex.PoseStack pose = new com.mojang.blaze3d.vertex.PoseStack();
        pose.mulPose(Axis.XP.rotationDegrees(xRot));
        pose.mulPose(Axis.YP.rotationDegrees(yRot + 180.0F));
        pose.translate((float) -pos.x, (float) -pos.y, (float) -pos.z);
        VIEW_MATRIX.set(pose.last().pose());
    }

    public static void updateDepthMap() {
        PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
        if (anyCulling() && !checkCulling && anyNeedTransfer() && continueUpdateDepth()) {
            Minecraft mc = Minecraft.getInstance();
            float sampling = (float) EffectiveConfig.getSampling();
            int width = mc.getWindow().getWidth();
            int height = mc.getWindow().getHeight();

            runOnDepthFrame((ctx) -> {
                int sw = Math.max(1, (int) (width * sampling * ctx.scale()));
                int sh = Math.max(1, (int) (height * sampling * ctx.scale()));
                if (ctx.frame().width != sw || ctx.frame().height != sh) ctx.frame().resize(sw, sh, Minecraft.ON_OSX);
            });

            int depthTexture = mc.getMainRenderTarget().getDepthTextureId();
            ShaderLoader loader = SHADER_LOADER;
            if (loader != null && loader.enabledShader()) {
                int fboId = loader.getFrameBufferID();
                if (!SHADER_DEPTH_BUFFER_ID.containsKey(fboId)) {
                    RenderSystem.assertOnRenderThreadOrInit();
                    com.mojang.blaze3d.platform.GlStateManager._glBindFramebuffer(36160, fboId);
                    int[] attachmentObjectType = new int[1];
                    org.lwjgl.opengl.GL30.glGetFramebufferAttachmentParameteriv(36160, 36096, 36048, attachmentObjectType);
                    if (attachmentObjectType[0] == 5890) {
                        int[] depthTextureID = new int[1];
                        org.lwjgl.opengl.GL30.glGetFramebufferAttachmentParameteriv(36160, 36096, 36049, depthTextureID);
                        depthTexture = depthTextureID[0];
                        SHADER_DEPTH_BUFFER_ID.put(fboId, depthTexture);
                    } else {
                        int fallback = loader.getDepthTextureID();
                        if (fallback != -1) {
                            depthTexture = fallback;
                            SHADER_DEPTH_BUFFER_ID.put(fboId, depthTexture);
                        }
                    }
                } else {
                    depthTexture = SHADER_DEPTH_BUFFER_ID.get(fboId);
                }
            }

            MAIN_DEPTH_TEXTURE = depthTexture;
            runOnDepthFrame((ctx) -> {
                useShader(COPY_DEPTH_SHADER);
                ctx.frame().clear(Minecraft.ON_OSX);
                ctx.frame().bindWrite(false);
                BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
                bb.addVertex(-1, -1, 0);
                bb.addVertex(1, -1, 0);
                bb.addVertex(1, 1, 0);
                bb.addVertex(-1, 1, 0);
                RenderSystem.setShaderTexture(0, ctx.lastTexture());
                BufferUploader.drawWithShader(bb.buildOrThrow());
                DEPTH_TEXTURE[ctx.index()] = ctx.frame().getColorTextureId();
            });
            bindMainFrameTarget();
        }
    }

    public static void updateMapData() {
        if (!anyCulling()) {
            cleanup();
            return;
        }

        if (anyNeedTransfer()) preCullingInitCount++;
        Minecraft mc = Minecraft.getInstance();

        if (EffectiveConfig.getCullChunk()) updateChunkCullingMap(mc);
        if (EffectiveConfig.doEntityCulling()) updateEntityCullingMap(mc);

        fps = ((MinecraftAccessor) mc).getFps();
    }

    private static void updateChunkCullingMap(Minecraft mc) {
        if (LEVEL_SECTION_RANGE == 0 && mc.level != null) {
            onLevelRendererAllChanged();
        }
        int dist = mc.options.getEffectiveRenderDistance();
        int renderingDiameter = dist * 2 + 1;
        int maxSize = renderingDiameter * LEVEL_SECTION_RANGE * renderingDiameter;
        int cSize = (int) Math.sqrt(maxSize) + 1;

        if (CHUNK_CULLING_MAP_TARGET.width != cSize) {
            CHUNK_CULLING_MAP_TARGET.resize(cSize, cSize, Minecraft.ON_OSX);

            ChunkCullingMap oldMap = CHUNK_CULLING_MAP;
            if (oldMap != null) oldMap.cleanup();

            ChunkCullingMap newMap = new ChunkCullingMap(cSize, cSize);
            CHUNK_CULLING_MAP = newMap;
            newMap.generateIndex(dist);
        }

        long time = System.nanoTime();
        ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
        if (chunkMap != null) chunkMap.transferData();
        preChunkCullingInitTime += System.nanoTime() - time;
    }

    private static void updateEntityCullingMap(Minecraft mc) {
        EntityCullingMap entityMap = ENTITY_CULLING_MAP;
        if (entityMap == null) {
            entityMap = new EntityCullingMap(8, 64);
            ENTITY_CULLING_MAP = entityMap;
        }

        int neededH = (entityMap.getEntityTable().size() / 64 * 64 + 64) / 8 + 1;
        int currentH = ENTITY_CULLING_MAP_TARGET.height;
        if (neededH > currentH || currentH > neededH * 4) {
            ENTITY_CULLING_MAP_TARGET.resize(8, neededH, Minecraft.ON_OSX);
            EntityCullingMap newMap = new EntityCullingMap(8, neededH);
            newMap.getEntityTable().copyTemp(entityMap.getEntityTable(), clientTickCount);
            newMap.copyDataFrom(entityMap);
            entityMap.cleanup();
            entityMap = newMap;
            ENTITY_CULLING_MAP = entityMap;
        }

        long time = System.nanoTime();
        entityMap.transferData();
        preEntityCullingInitTime += System.nanoTime() - time;

        if (mc.level != null) {
            entityMap.getEntityTable().clearIndexMap();

            for (Entity e : mc.level.entitiesForRendering()) {
                if (EffectiveConfig.shouldSkipEntityType(e.getType())) continue;
                entityMap.getEntityTable().addObject(e);
            }

            IEntitiesForRender levelRenderer = (IEntitiesForRender) mc.levelRenderer;
            for (var section : levelRenderer.bruteForceCulling$visibleSections()) {
                for (BlockEntity be : section.getCompiled().getRenderableBlockEntities()) {
                    if (EffectiveConfig.shouldSkipBlockEntityType(be.getType())) continue;
                    entityMap.getEntityTable().addObject(be);
                }
            }

            entityMap.getEntityTable().addAllTemp();
        }
    }

    public static void useShader(ShaderInstance instance) {
        RenderSystem.setShader(() -> instance);
    }

    public static void bindMainFrameTarget() {
        if (renderingShader()) {
            SHADER_LOADER.bindDefaultFrameBuffer();
        } else {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

    public static void runOnDepthFrame(Consumer<DepthContext> consumer) {
        float f = 1.0f;
        for (DEPTH_INDEX = 0; DEPTH_INDEX < DEPTH_BUFFER_TARGET.length; ++DEPTH_INDEX) {
            int lastTex = DEPTH_INDEX == 0 ? MAIN_DEPTH_TEXTURE : DEPTH_BUFFER_TARGET[DEPTH_INDEX - 1].getColorTextureId();
            consumer.accept(new DepthContext(DEPTH_BUFFER_TARGET[DEPTH_INDEX], DEPTH_INDEX, f, lastTex));
            f *= 0.35f;
        }
    }

    public static void callDepthTexture() {
        runOnDepthFrame(ctx -> RenderSystem.setShaderTexture(ctx.index(), DEPTH_TEXTURE[ctx.index()]));
    }

    public static boolean renderingShader() {
        return SHADER_LOADER != null && SHADER_LOADER.renderingShaderPass();
    }

    public static boolean enabledShader() {
        return SHADER_LOADER != null && SHADER_LOADER.enabledShader();
    }

    public static boolean anyNextTick() {
        return isNewTickFrame;
    }

    public static boolean isNextLoop() {
        return isNextLoopFrame;
    }

    public static boolean anyCulling() {
        return EffectiveConfig.getCullChunk() || EffectiveConfig.doEntityCulling();
    }

    public static boolean anyNeedTransfer() {
        ChunkCullingMap chunkMap = CHUNK_CULLING_MAP;
        EntityCullingMap entityCullingMap = ENTITY_CULLING_MAP;
        return (entityCullingMap != null && entityCullingMap.needTransferData()) || (chunkMap != null && chunkMap.needTransferData());
    }

    public static boolean gl33() {
        if (RenderSystem.isOnRenderThread() && gl33 < 0)
            gl33 = (GL.getCapabilities().OpenGL33 || Checks.checkFunctions(GL.getCapabilities().glVertexAttribDivisor)) ? 1 : 0;
        return gl33 == 1;
    }

    public static void updating() {
        continueUpdateCount = 10;
        lastUpdate = true;
    }

    public static boolean continueUpdateChunk() {
        if (continueUpdateCount > 0) return true;
        if (lastUpdate) {
            lastUpdate = false;
            return true;
        }
        return false;
    }

    public static boolean continueUpdateDepth() {
        return continueUpdateCount > 0;
    }

}
