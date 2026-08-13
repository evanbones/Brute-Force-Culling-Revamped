package com.evandev.brute_force_culling.culling;

import com.evandev.brute_force_culling.Constants;
import com.evandev.brute_force_culling.culling.impl.IAABBObject;
import com.evandev.brute_force_culling.platform.Services;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.joml.FrustumIntersection;
import org.joml.Vector4f;

import java.lang.reflect.Field;

public class ModIntegrationUtil {
    private static Field frustumPlanesField;

    private ModIntegrationUtil() {
    }

    public static boolean hasMod(String id) {
        return Services.PLATFORM.isModLoaded(id);
    }

    public static boolean hasSodium() {
        return hasMod("sodium");
    }

    public static boolean hasIris() {
        return hasMod("iris");
    }

    public static boolean hasNvidium() {
        return false;
    }

    public static AABB getObjectAABB(Object o) {
        if (o instanceof BlockEntity be) return new AABB(be.getBlockPos());
        if (o instanceof Entity e) return e.getBoundingBox();
        if (o instanceof IAABBObject aabb) return aabb.getAABB();
        return null;
    }

    public static Vector4f[] getFrustumPlanes(FrustumIntersection frustum) {
        try {
            if (frustumPlanesField == null) {
                frustumPlanesField = FrustumIntersection.class.getDeclaredField("planes");
                frustumPlanesField.setAccessible(true);
            }
            return (Vector4f[]) frustumPlanesField.get(frustum);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Constants.LOG.error("Failed to get frustum planes", e);
        }

        return new Vector4f[0];
    }
}
