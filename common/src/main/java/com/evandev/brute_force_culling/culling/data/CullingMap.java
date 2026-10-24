package com.evandev.brute_force_culling.culling.data;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL12.GL_BGRA;

public abstract class CullingMap {
    private static Boolean persistentSupported;

    protected final int pboId;
    protected final int width;
    protected final int height;
    protected final ByteBuffer cullingBuffer;
    private final ByteBuffer mappedBuffer;
    private final boolean persistent;
    protected boolean done;
    protected boolean transferred;
    protected int delayCount = 0;
    private boolean drawnSinceTransfer;
    private boolean pendingRead;
    private long fenceSync;

    public CullingMap(int width, int height) {
        this.width = width;
        this.height = height;
        int size = width * height * 4;

        // Until the first readback lands, everything reads as visible instead of culled
        ByteBuffer visible = BufferUtils.createByteBuffer(size);
        while (visible.hasRemaining()) {
            visible.put((byte) 0xFF);
        }
        visible.flip();

        this.pboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_PIXEL_PACK_BUFFER, pboId);

        boolean immutable = persistentSupported();
        ByteBuffer mapped = null;
        if (immutable) {
            int flags = GL44.GL_MAP_READ_BIT | GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT;
            GL44.glBufferStorage(GL31.GL_PIXEL_PACK_BUFFER, visible, flags);
            mapped = GL30.glMapBufferRange(GL31.GL_PIXEL_PACK_BUFFER, 0L, size, flags);
        }

        if (!immutable) {
            GL15.glBufferData(GL31.GL_PIXEL_PACK_BUFFER, visible, GL15.GL_DYNAMIC_READ);
        }
        this.mappedBuffer = mapped;
        this.persistent = mapped != null;
        this.cullingBuffer = visible;

        GL15.glBindBuffer(GL31.GL_PIXEL_PACK_BUFFER, 0);
        CullingStateManager.bindMainFrameTarget();
    }

    private static boolean persistentSupported() {
        if (persistentSupported == null) {
            try {
                persistentSupported = GL.getCapabilities().GL_ARB_buffer_storage;
            } catch (Throwable t) {
                persistentSupported = Boolean.FALSE;
            }
        }
        return persistentSupported;
    }

    public boolean needTransferData() {
        return delayCount <= 0;
    }

    public boolean readyForDraw() {
        return delayCount <= 0 && !pendingRead;
    }

    public void markDrawn() {
        drawnSinceTransfer = true;
    }

    public void transferData() {
        if (delayCount <= 0) {
            if (drawnSinceTransfer && !pendingRead) {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, bindFrameBufferId());
                GL15.glBindBuffer(GL31.GL_PIXEL_PACK_BUFFER, pboId);
                GL11.glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, 0);
                GL15.glBindBuffer(GL31.GL_PIXEL_PACK_BUFFER, 0);
                if (persistent) {
                    GL42.glMemoryBarrier(GL44.GL_CLIENT_MAPPED_BUFFER_BARRIER_BIT);
                }
                CullingStateManager.bindMainFrameTarget();
                if (fenceSync != 0) {
                    GL32.glDeleteSync(fenceSync);
                }
                fenceSync = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
                pendingRead = true;
                drawnSinceTransfer = false;
                onTransferData();
                delayCount = configDelayCount() + dynamicDelayCount();
            }
        } else if (shouldUpdate()) {
            delayCount--;
        }

        if (delayCount <= 0 && pendingRead && fenceSignaled()) {
            setTransferred(true);
        }
    }

    private boolean fenceSignaled() {
        if (fenceSync == 0) return true;
        return GL32.glGetSynci(fenceSync, GL32.GL_SYNC_STATUS, null) == GL32.GL_SIGNALED;
    }

    protected abstract boolean shouldUpdate();

    protected void onTransferData() {
    }

    protected void onReadData() {
    }

    public void readData() {
        if (persistent) {
            cullingBuffer.clear();
            cullingBuffer.put(mappedBuffer.duplicate().clear());
            cullingBuffer.clear();
        } else {
            GL15.glBindBuffer(GL31.GL_PIXEL_PACK_BUFFER, pboId);
            GL15.glGetBufferSubData(GL31.GL_PIXEL_PACK_BUFFER, 0, cullingBuffer);
            GL15.glBindBuffer(GL31.GL_PIXEL_PACK_BUFFER, 0);
        }
        if (fenceSync != 0) {
            GL32.glDeleteSync(fenceSync);
            fenceSync = 0;
        }
        pendingRead = false;
        onReadData();
        setTransferred(false);
    }

    abstract int configDelayCount();

    public int dynamicDelayCount() {
        if (CullingStateManager.fps > 100) {
            return CullingStateManager.fps / 100;
        }
        return 0;
    }

    abstract int bindFrameBufferId();

    public boolean isDone() {
        return done;
    }

    public void setDone() {
        done = true;
    }

    public void copyDataFrom(CullingMap other) {
        ByteBuffer src = other.cullingBuffer.duplicate();
        ByteBuffer dst = this.cullingBuffer.duplicate();
        src.position(0);
        dst.position(0);
        src.limit(Math.min(src.capacity(), dst.capacity()));
        dst.put(src);
    }

    public void cleanup() {
        if (persistent) {
            GL15.glBindBuffer(GL31.GL_PIXEL_PACK_BUFFER, pboId);
            GL15.glUnmapBuffer(GL31.GL_PIXEL_PACK_BUFFER);
            GL15.glBindBuffer(GL31.GL_PIXEL_PACK_BUFFER, 0);
        }
        cullingBuffer.clear();
        GL15.glDeleteBuffers(pboId);
        if (fenceSync != 0) {
            GL32.glDeleteSync(fenceSync);
            fenceSync = 0;
        }
        pendingRead = false;
    }

    public boolean isTransferred() {
        return transferred;
    }

    public void setTransferred(boolean transferred) {
        this.transferred = transferred;
    }
}
