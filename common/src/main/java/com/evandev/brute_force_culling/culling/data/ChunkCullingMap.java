package com.evandev.brute_force_culling.culling.data;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.EffectiveConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class ChunkCullingMap extends CullingMap {
    /**
     * Largest camera movement, in blocks, the culling shader can record for a culled chunk. Must match
     * MAX_SAFE_MOVEMENT in brute_force_culling_chunk_culling.fsh.
     */
    public static final double MAX_SAFE_MOVEMENT = 32.0;

    private int renderDistance = 0;
    private int spacePartitionSize = 0;
    private int sectionRange;
    private int cameraX;
    private int cameraZ;
    private double cameraPosX;
    private double cameraPosY;
    private double cameraPosZ;
    private int pendingSectionRange;
    private int pendingCameraX;
    private int pendingCameraZ;
    private double pendingPosX;
    private double pendingPosY;
    private double pendingPosZ;
    private int transferSectionRange;
    private int transferCameraX;
    private int transferCameraZ;
    private double transferPosX;
    private double transferPosY;
    private double transferPosZ;
    private double movedSinceCapture;

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

    /**
     * Records the camera the next culling map is drawn from. Called once per frame before the map is drawn.
     */
    public void updateCamera() {
        if (CullingStateManager.CAMERA == null) return;
        Vec3 camera = CullingStateManager.CAMERA.getPosition();
        this.pendingCameraX = Mth.floor(camera.x) >> 4;
        this.pendingCameraZ = Mth.floor(camera.z) >> 4;
        this.pendingPosX = camera.x;
        this.pendingPosY = camera.y;
        this.pendingPosZ = camera.z;
        this.pendingSectionRange = CullingStateManager.LEVEL_SECTION_RANGE;
        updateMovement(camera);
    }

    @Override
    protected void onTransferData() {
        this.transferCameraX = this.pendingCameraX;
        this.transferCameraZ = this.pendingCameraZ;
        this.transferPosX = this.pendingPosX;
        this.transferPosY = this.pendingPosY;
        this.transferPosZ = this.pendingPosZ;
        this.transferSectionRange = this.pendingSectionRange;
    }

    @Override
    protected void onReadData() {
        this.cameraX = this.transferCameraX;
        this.cameraZ = this.transferCameraZ;
        this.cameraPosX = this.transferPosX;
        this.cameraPosY = this.transferPosY;
        this.cameraPosZ = this.transferPosZ;
        this.sectionRange = this.transferSectionRange;
        if (CullingStateManager.CAMERA != null) updateMovement(CullingStateManager.CAMERA.getPosition());
    }

    private void updateMovement(Vec3 camera) {
        double dx = camera.x - cameraPosX;
        double dy = camera.y - cameraPosY;
        double dz = camera.z - cameraPosZ;
        this.movedSinceCapture = Math.sqrt(dx * dx + dy * dy + dz * dz);
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

        int index = ((posX + renderDistance) * spacePartitionSize * sectionRange + (posZ + renderDistance) * sectionRange + posY) << 2;
        if (index < 0 || index + 2 >= cullingBuffer.limit()) return true;

        // BGRA: green holds the verdict, red holds how far the camera may move before an occluded chunk could
        // come into view. The map is drawn from where the camera was a few frames ago, so once the camera has
        // moved past that distance (sprinting round a corner) the old verdict no longer holds.
        if ((cullingBuffer.get(index + 1) & 0xFF) > (checkForChunk ? 0 : 127)) return true;
        double encoded = (cullingBuffer.get(index + 2) & 0xFF) / 255.0;
        double safeMovement = encoded * encoded * MAX_SAFE_MOVEMENT;
        return movedSinceCapture > safeMovement;
    }
}
