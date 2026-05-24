package misanthropy.brute_force_culling_revived.util;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import misanthropy.brute_force_culling_revived.api.CullingStateManager;
import misanthropy.brute_force_culling_revived.api.data.ChunkCullingMap;
import misanthropy.brute_force_culling_revived.api.impl.ICollectorAccessor;
import misanthropy.brute_force_culling_revived.api.impl.IRenderSectionVisibility;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Semaphore;

public class SodiumSectionAsyncUtil {
    private static int frame = 0;
    private static OcclusionCuller occlusionCuller;
    private static Viewport viewport;
    private static float searchDistance;
    private static boolean useOcclusionCulling;
    private static Viewport shadowViewport;
    private static float shadowSearchDistance;
    private static boolean shadowUseOcclusionCulling;
    private static @Nullable VisibleChunkCollector collector;
    private static @Nullable VisibleChunkCollector shadowCollector;
    private static final Semaphore shouldUpdate = new Semaphore(0);

    private static final Object LOCK = new Object();
    private static volatile int generation = 0;

    public static boolean renderingEntities;
    public static boolean needSyncRebuild;

    public static void fromSectionManager(Long2ReferenceMap<RenderSection> sections, Level world) {
        synchronized (LOCK) {
            SodiumSectionAsyncUtil.occlusionCuller = new OcclusionCuller(sections, world);
        }
    }

    public static void reset() {
        synchronized (LOCK) {
            generation++;
            collector = null;
            shadowCollector = null;
            viewport = null;
            shadowViewport = null;
            occlusionCuller = null;
            needSyncRebuild = false;
            shouldUpdate.drainPermits();
        }
    }

    public static void asyncSearchRebuildSection() {
        shouldUpdate.acquireUninterruptibly();

        int currentGeneration;
        OcclusionCuller culler;

        synchronized (LOCK) {
            currentGeneration = generation;
            culler = occlusionCuller;
        }

        if (CullingStateManager.needPauseRebuild() || culler == null) {
            return;
        }

        VisibleChunkCollector sColl = null;
        Viewport localShadowViewport = shadowViewport;

        if (CullingStateManager.enabledShader() && localShadowViewport != null) {
            frame++;
            CullingStateManager.useOcclusionCulling = false;
            sColl = new AsynchronousChunkCollector(frame);
            culler.findVisible(sColl, localShadowViewport, shadowSearchDistance, shadowUseOcclusionCulling, frame);
            CullingStateManager.useOcclusionCulling = true;
        }

        VisibleChunkCollector mainColl = null;
        boolean localNeedSyncRebuild = false;
        Viewport localViewport = viewport;

        if (localViewport != null) {
            frame++;
            mainColl = CullingStateManager.checkCulling ?
                    new DebugChunkCollector(frame) : new AsynchronousChunkCollector(frame);

            culler.findVisible(mainColl, localViewport, searchDistance, useOcclusionCulling, frame);

            ChunkCullingMap chunkMap = CullingStateManager.CHUNK_CULLING_MAP;
            if (chunkMap != null) {
                chunkMap.queueUpdateCount++;
            }

            Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildList = mainColl.getRebuildLists();
            for (ArrayDeque<RenderSection> queue : rebuildList.values()) {
                if (!queue.isEmpty()) {
                    localNeedSyncRebuild = true;
                    break;
                }
            }
        }

        synchronized (LOCK) {
            if (currentGeneration == generation) {
                SodiumSectionAsyncUtil.collector = mainColl;
                SodiumSectionAsyncUtil.shadowCollector = sColl;
                SodiumSectionAsyncUtil.needSyncRebuild = localNeedSyncRebuild;
            }
        }
    }

    public static void pauseAsync() {
        synchronized (LOCK) {
            SodiumSectionAsyncUtil.collector = null;
            SodiumSectionAsyncUtil.shadowCollector = null;
        }
    }

    public static void update(Viewport viewport, float dist, boolean culling) {
        synchronized (LOCK) {
            if (CullingStateManager.renderingShader()) {
                SodiumSectionAsyncUtil.shadowViewport = viewport;
                SodiumSectionAsyncUtil.shadowSearchDistance = dist;
                SodiumSectionAsyncUtil.shadowUseOcclusionCulling = culling;
            } else {
                SodiumSectionAsyncUtil.viewport = viewport;
                SodiumSectionAsyncUtil.searchDistance = dist;
                SodiumSectionAsyncUtil.useOcclusionCulling = culling;
            }
        }
    }

    public static @Nullable VisibleChunkCollector getChunkCollector() {
        synchronized (LOCK) {
            return collector;
        }
    }

    public static @Nullable VisibleChunkCollector getShadowCollector() {
        synchronized (LOCK) {
            return shadowCollector;
        }
    }

    public static void shouldUpdate() {
        if (shouldUpdate.availablePermits() < 1) {
            shouldUpdate.release();
        }
    }

    public static class AsynchronousChunkCollector extends VisibleChunkCollector {
        private final Map<RenderRegion, ChunkRenderList> renderListMap = new HashMap<>();
        private final EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>> syncRebuildLists;
        private static final EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>> EMPTY_LIST = new EnumMap<>(ChunkUpdateType.class);

        static {
            for (ChunkUpdateType type : ChunkUpdateType.values()) {
                EMPTY_LIST.put(type, new ArrayDeque<>());
            }
        }

        private boolean sent;

        public AsynchronousChunkCollector(int frame) {
            super(frame);
            this.syncRebuildLists = new EnumMap<>(ChunkUpdateType.class);
            for (ChunkUpdateType type : ChunkUpdateType.values()) {
                this.syncRebuildLists.put(type, new ArrayDeque<>());
            }
        }

        @Override
        public void visit(@NotNull RenderSection section, boolean visible) {
            if (visible && section.getFlags() != 0) {
                RenderRegion region = section.getRegion();
                ChunkRenderList list = renderListMap.computeIfAbsent(region, r -> {
                    ChunkRenderList nl = new ChunkRenderList(r);
                    ((ICollectorAccessor) this).bruteForceRenderingRevived$addRenderList(nl);
                    return nl;
                });
                list.add(section);
            }
            ((ICollectorAccessor) this).bruteForceRenderingRevived$addAsyncToRebuildLists(section);
        }

        @Override
        public Map<ChunkUpdateType, ArrayDeque<RenderSection>> getRebuildLists() {
            if (!RenderSystem.isOnRenderThread()) return super.getRebuildLists();
            if (sent) return EMPTY_LIST;
            sent = true;

            if (CullingStateManager.needPauseRebuild()) return syncRebuildLists;

            super.getRebuildLists().forEach((type, sections) -> {
                for (RenderSection s : sections) {
                    if (!s.isDisposed() && s.getBuildCancellationToken() == null) {
                        syncRebuildLists.get(type).add(s);
                    }
                }
            });
            return syncRebuildLists;
        }
    }

    public static class DebugChunkCollector extends VisibleChunkCollector {
        private final Map<RenderRegion, ChunkRenderList> renderListMap = new HashMap<>();

        public DebugChunkCollector(int frame) {
            super(frame);
        }

        @Override
        public void visit(@NotNull RenderSection section, boolean visible) {
            if (visible && section.getFlags() != 0) {
                boolean shouldCull = !CullingStateManager.shouldRenderChunk((IRenderSectionVisibility) section, true);

                if (CullingStateManager.checkCulling) {
                    if (shouldCull) addToVisible(section);
                } else {
                    if (!shouldCull) addToVisible(section);
                }
            }
        }

        private void addToVisible(@NotNull RenderSection section) {
            RenderRegion region = section.getRegion();
            ChunkRenderList list = renderListMap.computeIfAbsent(region, r -> {
                ChunkRenderList nl = new ChunkRenderList(r);
                ((ICollectorAccessor) this).bruteForceRenderingRevived$addRenderList(nl);
                return nl;
            });
            if (list.size() < 256) list.add(section);
        }

        @Override
        public SortedRenderLists createRenderLists() {
            return super.createRenderLists();
        }
    }
}