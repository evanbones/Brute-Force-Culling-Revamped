package com.evandev.brute_force_culling.culling;

import com.evandev.brute_force_culling.Constants;
import com.evandev.brute_force_culling.culling.data.ChunkCullingMap;
import com.evandev.brute_force_culling.culling.data.EntityCullingMap;
import com.evandev.brute_force_culling.culling.impl.ICullingShader;
import com.evandev.brute_force_culling.culling.instanced.EntityCullingInstanceRenderer;
import com.evandev.brute_force_culling.mixin.accessor.FrustumAccessor;
import com.mojang.blaze3d.pipeline.RenderTarget;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CullingRenderEvent {
    private static final float[] VEC3_BUFFER = new float[3];
    private static final float[] FRUSTUM_BUFFER = new float[24];
    private static final float[] DEPTH_SIZE_BUFFER = new float[10];
    private static final long DEBUG_REFRESH_INTERVAL_MS = 100L;
    private static final int PANEL_TOP = 20;
    private static final List<String> monitorTexts = new ArrayList<>();
    public static EntityCullingInstanceRenderer ENTITY_CULLING_INSTANCE_RENDERER;
    private static long nextDebugUpdateTime;
    private static int panelHalfWidth = 80;
    private static int panelBottom = 0;

    static {
        RenderSystem.recordRenderCall(() -> ENTITY_CULLING_INSTANCE_RENDERER = new EntityCullingInstanceRenderer());
    }

    public static void updateCullingMap() {
        if (!CullingStateManager.anyCulling() || CullingStateManager.checkCulling)
            return;

        CullingStateManager.callDepthTexture();

        EntityCullingMap entityCullingMap = CullingStateManager.ENTITY_CULLING_MAP;
        if (EffectiveConfig.doEntityCulling() && entityCullingMap != null && entityCullingMap.readyForDraw()) {
            CullingStateManager.ENTITY_CULLING_MAP_TARGET.clear(Minecraft.ON_OSX);
            CullingStateManager.ENTITY_CULLING_MAP_TARGET.bindWrite(false);
            entityCullingMap.getEntityTable().addEntityAttribute(ENTITY_CULLING_INSTANCE_RENDERER::addInstanceAttrib);
            ENTITY_CULLING_INSTANCE_RENDERER.drawWithShader(CullingStateManager.INSTANCED_ENTITY_CULLING_SHADER);
            entityCullingMap.markDrawn();
        }

        ChunkCullingMap chunkCullingMap = CullingStateManager.CHUNK_CULLING_MAP;
        if (EffectiveConfig.getCullChunk() && chunkCullingMap != null && chunkCullingMap.readyForDraw()) {
            CullingStateManager.useShader(CullingStateManager.CHUNK_CULLING_SHADER);
            CullingStateManager.CHUNK_CULLING_MAP_TARGET.clear(Minecraft.ON_OSX);
            CullingStateManager.CHUNK_CULLING_MAP_TARGET.bindWrite(false);

            BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            bufferbuilder.addVertex(-1.0f, -1.0f, 0.0f);
            bufferbuilder.addVertex(1.0f, -1.0f, 0.0f);
            bufferbuilder.addVertex(1.0f, 1.0f, 0.0f);
            bufferbuilder.addVertex(-1.0f, 1.0f, 0.0f);
            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
            chunkCullingMap.markDrawn();
        }

        CullingStateManager.bindMainFrameTarget();
    }

    @SuppressWarnings("resource")
    public static void setUniform(ShaderInstance shader) {
        if (!(shader instanceof ICullingShader shaderInstance) || !shaderInstance.bruteForceCulling$hasCullingUniforms()) return;
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

                int planeCount = Math.min(frustumData.length, 6);
                if (planeCount < 6) Arrays.fill(FRUSTUM_BUFFER, 0.0f);
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

        if (shader == CullingStateManager.COPY_DEPTH_SHADER && shader.SCREEN_SIZE != null) {
            // Size of the texture being downsampled: the main depth buffer for the first level, then the level above.
            RenderTarget source = CullingStateManager.DEPTH_INDEX > 0
                    ? CullingStateManager.DEPTH_BUFFER_TARGET[CullingStateManager.DEPTH_INDEX - 1]
                    : mc.getMainRenderTarget();
            shader.SCREEN_SIZE.set((float) Math.max(source.width, 1), (float) Math.max(source.height, 1));
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

        Font font = mc.font;
        long currentTime = Util.getMillis();
        if (currentTime >= nextDebugUpdateTime) {
            nextDebugUpdateTime = currentTime + DEBUG_REFRESH_INTERVAL_MS;
            rebuildMonitorTexts(mc, font);
        }

        if (monitorTexts.isEmpty()) return;

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        guiGraphics.fill(centerX - panelHalfWidth - 2, PANEL_TOP - 2, centerX + panelHalfWidth + 2, panelBottom + 2, 0x66000000);
        renderText(guiGraphics, monitorTexts, centerX, font);

        if (CullingStateManager.checkTexture) {
            renderTexturePreviews(guiGraphics, mc);
        }
    }

    private static void rebuildMonitorTexts(Minecraft mc, Font font) {
        boolean profiling = CullingStateManager.DEBUG > 1;
        monitorTexts.clear();

        monitorTexts.add("FPS: " + CullingStateManager.fps);

        String on = I18n.get("hud." + Constants.MOD_ID + ".enable");
        String off = I18n.get("hud." + Constants.MOD_ID + ".disable");

        monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".cull_entity") + ": " + (EffectiveConfig.getCullEntity() ? on : off));
        monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".entity_culling") + ": " +
                CullingStateManager.entityCulling + "/" + CullingStateManager.entityCount +
                (profiling ? " (" + formatMillis(CullingStateManager.entityCullingTime) + ")" : ""));

        if (EffectiveConfig.getTickCulling()) {
            monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".tick_culling") + ": " +
                    CullingStateManager.tickCulling + "/" + CullingStateManager.tickEntityCount);
        }

        monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".cull_block_entity") + ": " + (EffectiveConfig.getCullBlockEntity() ? on : off));
        monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".block_culling") + ": " +
                CullingStateManager.blockCulling + "/" + CullingStateManager.blockCount +
                (profiling ? " (" + formatMillis(CullingStateManager.blockCullingTime) + ")" : ""));

        if (profiling && EffectiveConfig.doEntityCulling()) {
            monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".entity_culling_init") + ": " +
                    formatMillis(CullingStateManager.entityCullingInitTime));
        }

        String chunkState = EffectiveConfig.getCullChunk()
                ? (EffectiveConfig.shouldCullChunk() ? on : I18n.get("hud." + Constants.MOD_ID + ".warming_up"))
                : off;
        monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".cull_chunk") + ": " + chunkState);
        if (EffectiveConfig.getCullChunk()) {
            monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".chunk_culling") + ": " +
                    CullingStateManager.chunkCulling + "/" + CullingStateManager.chunkCount);
        }
        if (profiling) {
            monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".chunk_culling_time") + ": " +
                    formatMillis(CullingStateManager.chunkCullingTime));
            monitorTexts.add(I18n.get("hud." + Constants.MOD_ID + ".chunk_culling_init") + ": " +
                    formatMillis(CullingStateManager.chunkCullingInitTime) + " (" + CullingStateManager.cullingInitCount + ")");
        }

        int maxTextWidth = 0;
        for (String s : monitorTexts) {
            maxTextWidth = Math.max(maxTextWidth, font.width(s));
        }
        panelHalfWidth = Math.max(80, maxTextWidth / 2 + 10);
        panelBottom = PANEL_TOP + font.lineHeight * monitorTexts.size();
    }

    private static String formatMillis(long nanos) {
        return String.format("%.2fms", nanos / 1_000_000.0);
    }

    private static void renderText(GuiGraphics guiGraphics, List<String> texts, int centerX, Font font) {
        for (int i = 0; i < texts.size(); ++i) {
            String text = texts.get(i);
            guiGraphics.drawString(font, text, (int) (centerX - (font.width(text) / 2f)), PANEL_TOP + font.lineHeight * i, 0xFFFFFF);
        }
    }

    private static void renderTexturePreviews(GuiGraphics guiGraphics, Minecraft mc) {
        float scale = 0.4f;
        int screenH = mc.getWindow().getGuiScaledHeight();
        int screenW = mc.getWindow().getGuiScaledWidth();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        for (int i = 0; i < CullingStateManager.DEPTH_TEXTURE.length; i++) {
            int size = (int) (screenH * scale);
            drawTextureId(guiGraphics, CullingStateManager.DEPTH_TEXTURE[i], 0, screenH - size, size, size);
            scale *= 0.5f;
        }

        int mapSize = (int) (screenH * 0.25f);
        if (EffectiveConfig.doEntityCulling()) {
            drawTextureId(guiGraphics, CullingStateManager.ENTITY_CULLING_MAP_TARGET.getColorTextureId(), screenW - mapSize, 0, mapSize, mapSize);
        }
        if (EffectiveConfig.getCullChunk()) {
            drawTextureId(guiGraphics, CullingStateManager.CHUNK_CULLING_MAP_TARGET.getColorTextureId(), screenW - mapSize, mapSize, mapSize, mapSize);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void drawTextureId(GuiGraphics guiGraphics, int textureId, int x, int y, int width, int height) {
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = guiGraphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix, (float) x, (float) y + height, 0.0F).setUv(0.0F, 0.0F);
        bufferbuilder.addVertex(matrix, (float) x + width, (float) y + height, 0.0F).setUv(1.0F, 0.0F);
        bufferbuilder.addVertex(matrix, (float) x + width, (float) y, 0.0F).setUv(1.0F, 1.0F);
        bufferbuilder.addVertex(matrix, (float) x, (float) y, 0.0F).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }
}
