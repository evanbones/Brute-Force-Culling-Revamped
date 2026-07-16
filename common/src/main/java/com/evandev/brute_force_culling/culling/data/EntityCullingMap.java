package com.evandev.brute_force_culling.culling.data;

import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.EffectiveConfig;
import com.evandev.brute_force_culling.culling.ModIntegrationUtil;
import com.evandev.brute_force_culling.culling.util.IndexedSet;
import com.evandev.brute_force_culling.culling.util.LifeTimer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

public class EntityCullingMap extends CullingMap {
    public static final AABB INFINITE_EXTENT_AABB = new AABB(
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    private final EntityMap entityMap = new EntityMap();

    public EntityCullingMap(int width, int height) {
        super(width, height);
    }

    @Override
    protected boolean shouldUpdate() {
        return true;
    }

    @Override
    int configDelayCount() {
        return EffectiveConfig.getDepthUpdateDelay();
    }

    @Override
    int bindFrameBufferId() {
        return CullingStateManager.ENTITY_CULLING_MAP_TARGET.frameBufferId;
    }

    public boolean isObjectVisible(Object o) {
        AABB aabb = ModIntegrationUtil.getObjectAABB(o);

        if (aabb == INFINITE_EXTENT_AABB) {
            return true;
        }

        int idx = getEntityTable().getIndex(o);
        if (idx == -1) {
            getEntityTable().addTemp(o, CullingStateManager.clientTickCount);
            return true;
        }

        int bufferIdx = 1 + idx * 4;

        if (getEntityTable().tempObjectTimer.contains(o)) {
            getEntityTable().addTemp(o, CullingStateManager.clientTickCount);
        }

        if (bufferIdx < cullingBuffer.limit()) {
            return (cullingBuffer.get(bufferIdx) & 0xFF) > 0;
        }

        return true;
    }

    @Override
    public void readData() {
        super.readData();
        getEntityTable().readUpload();
    }

    public EntityMap getEntityTable() {
        return entityMap;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        getEntityTable().clear();
    }

    public static class EntityMap {
        public final LifeTimer<Object> tempObjectTimer = new LifeTimer<>();
        private final IndexedSet<Object> indexMap = new IndexedSet<>();
        private HashSet<Object> uploadTemp = new HashSet<>();
        private HashSet<Object> readTemp = new HashSet<>();
        private HashMap<Object, Integer> uploadEntity = new HashMap<>();
        private HashMap<Object, Integer> readEntity = new HashMap<>();

        public void addObject(Object obj) {
            if (obj instanceof Entity e && !e.isAlive()) return;
            if (obj instanceof BlockEntity be && be.isRemoved()) return;
            indexMap.add(obj);
        }

        public void addTemp(Object obj, int tickCount) {
            tempObjectTimer.updateUsageTick(obj, tickCount);
        }

        public void copyTemp(EntityMap other, int tickCount) {
            other.tempObjectTimer.foreach(o -> addTemp(o, tickCount));
            this.uploadTemp.addAll(other.uploadTemp);
            this.uploadEntity.putAll(other.uploadEntity);
            this.readTemp.clear();
            this.readTemp.addAll(this.uploadTemp);
            this.readEntity.clear();
            this.readEntity.putAll(this.uploadEntity);
        }

        public int getIndex(Object obj) {
            return readEntity.getOrDefault(obj, -1);
        }

        public void readUpload() {
            HashSet<Object> tempSet = readTemp;
            readTemp = uploadTemp;
            uploadTemp = tempSet;
            uploadTemp.clear();
            HashMap<Object, Integer> tempMap = readEntity;
            readEntity = uploadEntity;
            uploadEntity = tempMap;
            uploadEntity.clear();
        }

        public void clearUpload() {
            uploadTemp.clear();
            uploadEntity.clear();
        }

        public void clearIndexMap() {
            indexMap.clear();
        }

        public void tickTemp(int tickCount) {
            tempObjectTimer.tick(tickCount, 3);
        }

        public void addAllTemp() {
            tempObjectTimer.foreach(this::addObject);
        }

        public void clear() {
            indexMap.clear();
            tempObjectTimer.clear();
            uploadTemp.clear();
            readTemp.clear();
            uploadEntity.clear();
            readEntity.clear();
        }

        private void addAttribute(Consumer<Consumer<FloatBuffer>> consumer, AABB aabb, int index) {
            consumer.accept(buffer -> {
                buffer.put((float) index);
                float size = (float) Math.max(aabb.getXsize(), aabb.getZsize());
                buffer.put(size + 0.5F);
                buffer.put((float) aabb.getYsize() + 0.5F);
                Vec3 pos = aabb.getCenter();
                buffer.put((float) pos.x);
                buffer.put((float) pos.y);
                buffer.put((float) pos.z);
            });
        }

        public void addEntityAttribute(Consumer<Consumer<FloatBuffer>> consumer) {
            clearUpload();
            indexMap.forEach((o, index) -> {
                AABB aabb = ModIntegrationUtil.getObjectAABB(o);
                if (aabb != null) {
                    addAttribute(consumer, aabb, index);
                    uploadTemp.add(o);
                    uploadEntity.put(o, index);
                }
            });
        }

        public int size() {
            return indexMap.size();
        }
    }
}
