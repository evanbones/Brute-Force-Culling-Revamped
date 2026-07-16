package com.evandev.brute_force_culling.culling;

import com.evandev.brute_force_culling.Constants;
import com.evandev.brute_force_culling.culling.data.ChunkCullingMap;
import com.evandev.brute_force_culling.culling.data.EntityCullingMap;
import com.evandev.brute_force_culling.culling.impl.ICullingShader;
import com.evandev.brute_force_culling.culling.instanced.EntityCullingInstanceRenderer;
import com.evandev.brute_force_culling.mixin.accessor.FrustumAccessor;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CullingRenderEvent {
    private static final float[] VEC3_BUFFER = new float[3];
    private static final float[] FRUSTUM_BUFFER = new float[24];
    private static final float[] DEPTH_SIZE_BUFFER = new float[10];
    private static final List<String> cachedMonitorTexts = new ArrayList<>();
    public static EntityCullingInstanceRenderer ENTITY_CULLING_INSTANCE_RENDERER;
    private static long lastDebugUpdateTime;
    private static int cachedWidthScale = 80;
    private static int cachedBottom = 0;

    static {
        RenderSystem.recordRenderCall(() -> ENTITY_CULLING_INSTANCE_RENDERER = new EntityCullingInstanceRenderer());
    }

    public static void updateCullingMap() {
        if (!CullingStateManager.anyCulling() || CullingStateManager.checkCulling)
            return;

        CullingStateManager.callDepthTexture();

        EntityCullingMap entityCullingMap = CullingStateManager.ENTITY_CULLING_MAP;
        if (EffectiveConfig.doEntityCulling() && entityCullingMap != null && entityCullingMap.needTransferData()) {
            CullingStateManager.ENTITY_CULLING_MAP_TARGET.clear(Minecraft.ON_OSX);
            CullingStateManager.ENTITY_CULLING_MAP_TARGET.bindWrite(false);
            entityCullingMap.getEntityTable().addEntityAttribute(ENTITY_CULLING_INSTANCE_RENDERER::addInstanceAttrib);
            ENTITY_CULLING_INSTANCE_RENDERER.drawWithShader(CullingStateManager.INSTANCED_ENTITY_CULLING_SHADER);
        }

        ChunkCullingMap chunkCullingMap = CullingStateManager.CHUNK_CULLING_MAP;
        if (EffectiveConfig.getCullChunk() && chunkCullingMap != null && chunkCullingMap.needTransferData()) {
            CullingStateManager.useShader(CullingStateManager.CHUNK_CULLING_SHADER);
            CullingStateManager.CHUNK_CULLING_MAP_TARGET.clear(Minecraft.ON_OSX);
            CullingStateManager.CHUNK_CULLING_MAP_TARGET.bindWrite(false);

            BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bufferbuilder.addVertex(-1.0f, -1.0f, 0.0f);
            bufferbuilder.addVertex(1.0f, -1.0f, 0.0f);
            bufferbuilder.addVertex(1.0f, 1.0f, 0.0f);
            bufferbuilder.addVertex(-1.0f, 1.0f, 0.0f);
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        }

        CullingStateManager.bindMainFrameTarget();
    }

    @SuppressWarnings("resource")
    public static void setUniform(ShaderInstance shader) {
        if (!(shader instanceof ICullingShader shaderInstance)) return;
        Minecraft mc = Minecraft.getInstance();
        GameRenderer gameRenderer = mc.gameRenderer;

        Uniform cameraPos = shaderInstance.bruteForceCulling$getCullingCameraPos();
        if (cameraPos != null) {
            Vec3 pos = gameRenderer.getMainCamera().getPosition();
            VEC3_BUFFER[0] = (float) pos.x;
            VEC3_BUFFER[1] = (float) pos.y;
            VEC3_BUFFER[2] = (float) pos.z;
            cameraPos.set(VEC3_BUFFER);
        }

        Uniform cameraDir = shaderInstance.bruteForceCulling$getCullingCameraDir();
        if (cameraDir != null) {
            Vector3f dir = gameRenderer.getMainCamera().getLookVector();
            VEC3_BUFFER[0] = dir.x;
            VEC3_BUFFER[1] = dir.y;
            VEC3_BUFFER[2] = dir.z;
            cameraDir.set(VEC3_BUFFER);
        }

        Uniform boxScale = shaderInstance.bruteForceCulling$getBoxScale();
        if (boxScale != null) boxScale.set(4.0f);

        if (CullingStateManager.FRUSTUM != null) {
            Uniform frustumPos = shaderInstance.bruteForceCulling$getFrustumPos();
            if (frustumPos != null) {
                FrustumAccessor accessor = (FrustumAccessor) CullingStateManager.FRUSTUM;
                VEC3_BUFFER[0] = (float) accessor.camX();
                VEC3_BUFFER[1] = (float) accessor.camY();
                VEC3_BUFFER[2] = (float) accessor.camZ();
                frustumPos.set(VEC3_BUFFER);
            }

            Uniform frustum = shaderInstance.bruteForceCulling$getCullingFrustum();
            if (frustum != null) {
                Vector4f[] frustumData = ModIntegrationUtil.getFrustumPlanes(((FrustumAccessor) CullingStateManager.FRUSTUM).frustumIntersection());

                Arrays.fill(FRUSTUM_BUFFER, 0.0f);
                int planeCount = Math.min(frustumData.length, 6);
                for (int i = 0; i < planeCount; i++) {
                    Vector4f vec = frustumData[i];
                    FRUSTUM_BUFFER[i * 4] = vec.x();
                    FRUSTUM_BUFFER[i * 4 + 1] = vec.y();
                    FRUSTUM_BUFFER[i * 4 + 2] = vec.z();
                    FRUSTUM_BUFFER[i * 4 + 3] = vec.w();
                }
                frustum.set(FRUSTUM_BUFFER);
            }
        }

        Uniform viewMat = shaderInstance.bruteForceCulling$getCullingViewMat();
        if (viewMat != null) viewMat.set(CullingStateManager.VIEW_MATRIX);

        Uniform projMat = shaderInstance.bruteForceCulling$getCullingProjMat();
        if (projMat != null) projMat.set(CullingStateManager.PROJECTION_MATRIX);

        Uniform renderDist = shaderInstance.bruteForceCulling$getRenderDistance();
        if (renderDist != null) {
            float distance = mc.options.getEffectiveRenderDistance();
            if (shader == CullingStateManager.COPY_DEPTH_SHADER) {
                distance = (CullingStateManager.DEPTH_INDEX > 0) ? 2.0f : 0.0f;
            }
            renderDist.set(distance);
        }

        if (shader == CullingStateManager.COPY_DEPTH_SHADER
                && CullingStateManager.DEPTH_INDEX > 0
                && shader.SCREEN_SIZE != null) {
            shader.SCREEN_SIZE.set(
                    (float) CullingStateManager.DEPTH_BUFFER_TARGET[CullingStateManager.DEPTH_INDEX - 1].width,
                    (float) CullingStateManager.DEPTH_BUFFER_TARGET[CullingStateManager.DEPTH_INDEX - 1].height);
        }

        Uniform depthSize = shaderInstance.bruteForceCulling$getDepthSize();
        if (depthSize != null) {
            Arrays.fill(DEPTH_SIZE_BUFFER, 0.0f);
            if (shader == CullingStateManager.COPY_DEPTH_SHADER) {
                DEPTH_SIZE_BUFFER[0] = (float) CullingStateManager.DEPTH_BUFFER_TARGET[CullingStateManager.DEPTH_INDEX].width;
                DEPTH_SIZE_BUFFER[1] = (float) CullingStateManager.DEPTH_BUFFER_TARGET[CullingStateManager.DEPTH_INDEX].height;
            } else {
                for (int i = 0; i < CullingStateManager.DEPTH_SIZE; ++i) {
                    DEPTH_SIZE_BUFFER[i * 2] = (float) CullingStateManager.DEPTH_BUFFER_TARGET[i].width;
                    DEPTH_SIZE_BUFFER[i * 2 + 1] = (float) CullingStateManager.DEPTH_BUFFER_TARGET[i].height;
                }
            }
            depthSize.set(DEPTH_SIZE_BUFFER);
        }

        Uniform cullingSize = shaderInstance.bruteForceCulling$getCullingSize();
        if (cullingSize != null) {
            cullingSize.set(
                    (float) CullingStateManager.CHUNK_CULLING_MAP_TARGET.width,
                    (float) CullingStateManager.CHUNK_CULLING_MAP_TARGET.height);
        }

        Uniform entityCullingSize = shaderInstance.bruteForceCulling$getEntityCullingSize();
        if (entityCullingSize != null) {
            entityCullingSize.set(
                    (float) CullingStateManager.ENTITY_CULLING_MAP_TARGET.width,
                    (float) CullingStateManager.ENTITY_CULLING_MAP_TARGET.height);
        }

        Uniform levelHeightOffset = shaderInstance.bruteForceCulling$getLevelHeightOffset();
        if (levelHeightOffset != null) {
            levelHeightOffset.set(CullingStateManager.LEVEL_SECTION_RANGE);
        }

        Uniform levelMinSection = shaderInstance.bruteForceCulling$getLevelMinSection();
        if (levelMinSection != null) {
            Level level = mc.level;
            if (level != null) levelMinSection.set(level.getMinSection());
        }
    }

    public static void renderDebugOverlay(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || CullingStateManager.DEBUG <= 0) return;

        long currentTime = Util.getMillis();
        Font font = mc.font;

        if (currentTime - lastDebugUpdateTime > 100) {
            lastDebugUpdateTime = currentTime;
            cachedMonitorTexts.clear();

            String fpsStr = mc.fpsString;
            int spaceIdx = fpsStr.indexOf(' ');
            cachedMonitorTexts.add("FPS: " + (spaceIdx != -1 ? fpsStr.substring(0, spaceIdx) : fpsStr));

            String on = I18n.get("hud." + Constants.MOD_ID + ".enable");
            String off = I18n.get("hud." + Constants.MOD_ID + ".disable");

            cachedMonitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".cull_entity") + ": " + (EffectiveConfig.getCullEntity() ? on : off));
            cachedMonitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".entity_culling") + ": " +
                    CullingStateManager.entityCulling + "/" + CullingStateManager.entityCount +
                    " (" + String.format("%.2f", CullingStateManager.entityCullingTime / 1_000_000.0) + "ms)");

            cachedMonitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".cull_block_entity") + ": " + (EffectiveConfig.getCullBlockEntity() ? on : off));
            cachedMonitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".block_culling") + ": " +
                    CullingStateManager.blockCulling + "/" + CullingStateManager.blockCount +
                    " (" + String.format("%.2f", CullingStateManager.blockCullingTime / 1_000_000.0) + "ms)");

            String chunkState = EffectiveConfig.getCullChunk()
                    ? (EffectiveConfig.shouldCullChunk() ? on : I18n.get("hud." + Constants.MOD_ID + ".warming_up"))
                    : off;
            cachedMonitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".cull_chunk") + ": " + chunkState);
            cachedMonitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".chunk_culling") + ": " +
                    CullingStateManager.chunkCulling + "/" + CullingStateManager.chunkCount);
            cachedMonitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".chunk_culling_time") + ": " +
                    String.format("%.2f", CullingStateManager.chunkCullingTime / 1_000_000.0) + "ms");
            cachedMonitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".chunk_culling_init") + ": " +
                    String.format("%.2f", CullingStateManager.chunkCullingInitTime / 1_000_000.0) + "ms (" + CullingStateManager.cullingInitCount + ")");

            int maxTextWidth = 0;
            for (String s : cachedMonitorTexts) {
                maxTextWidth = Math.max(maxTextWidth, font.width(s));
            }
            cachedWidthScale = Math.max(80, maxTextWidth / 2 + 10);
            cachedBottom = 20 + (font.lineHeight * cachedMonitorTexts.size());
        }

        int width = mc.getWindow().getGuiScaledWidth() / 2;
        int height = 20;

        guiGraphics.fill(width - cachedWidthScale - 2, height - 2, width + cachedWidthScale + 2, cachedBottom + 2, 0x66000000);
        for (int i = 0; i < cachedMonitorTexts.size(); ++i) {
            String text = cachedMonitorTexts.get(i);
            guiGraphics.drawString(font, text, (int) (width - (font.width(text) / 2f)), height + font.lineHeight * i, 0xFFFFFF);
        }
    }
}
