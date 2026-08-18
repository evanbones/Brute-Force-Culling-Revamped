package com.evandev.brute_force_culling.culling.data;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.EffectiveConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ChunkCullingMap extends CullingMap {
    public int queueUpdateCount = 0;
    public int lastQueueUpdateCount = 0;
    private int renderDistance = 0;
    private int spacePartitionSize = 0;
    private int pendingCameraX;
    private int pendingCameraZ;
    private int transferCameraX;
    private int transferCameraZ;
    private int cameraX;
    private int cameraZ;
    private int sectionRange;
    private int pendingSectionRange;
    private int transferSectionRange;

    public ChunkCullingMap(int width, int height) {
        super(width, height);
    }

    @Override
    protected boolean shouldUpdate() {
        return CullingStateManager.continueUpdateChunk();
    }

    @Override
    int configDelayCount() {
        return EffectiveConfig.getDepthUpdateDelay();
    }

    @Override
    int bindFrameBufferId() {
        return CullingStateManager.CHUNK_CULLING_MAP_TARGET.frameBufferId;
    }

    public void generateIndex(int renderDistance) {
        this.renderDistance = renderDistance;
        this.spacePartitionSize = 2 * renderDistance + 1;
    }

    public void updateCamera() {
        if (CullingStateManager.CAMERA == null) return;
        Vec3 camera = CullingStateManager.CAMERA.getPosition();
        this.pendingCameraX = Mth.floor(camera.x) >> 4;
        this.pendingCameraZ = Mth.floor(camera.z) >> 4;
        this.pendingSectionRange = CullingStateManager.LEVEL_SECTION_RANGE;
    }

    @Override
    protected void onTransferData() {
        this.transferCameraX = this.pendingCameraX;
        this.transferCameraZ = this.pendingCameraZ;
        this.transferSectionRange = this.pendingSectionRange;
    }

    @Override
    protected void onReadData() {
        this.cameraX = this.transferCameraX;
        this.cameraZ = this.transferCameraZ;
        this.sectionRange = this.transferSectionRange;
    }

    public boolean hasData() {
        return sectionRange > 0;
    }

    public boolean isChunkOffsetCameraVisible(int x, int y, int z, boolean checkForChunk) {
        return isChunkVisible((x >> 4) - cameraX, CullingStateManager.mapChunkY(y), (z >> 4) - cameraZ, checkForChunk);
    }

    public boolean isChunkVisible(int posX, int posY, int posZ, boolean checkForChunk) {
        if (sectionRange <= 0 || spacePartitionSize <= 0) return true;
        if (posX < -renderDistance || posX > renderDistance) return true;
        if (posZ < -renderDistance || posZ > renderDistance) return true;
        if (posY < 0 || posY >= sectionRange) return true;

        int index = 1 + (((posX + renderDistance) * spacePartitionSize * sectionRange + (posZ + renderDistance) * sectionRange + posY) << 2);

        if (index >= 0 && index < cullingBuffer.limit()) {
            return (cullingBuffer.get(index) & 0xFF) > (checkForChunk ? 0 : 127);
        }
        return true;
    }
}
