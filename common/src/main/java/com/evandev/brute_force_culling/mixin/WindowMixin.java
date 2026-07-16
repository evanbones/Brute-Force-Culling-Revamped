package com.evandev.brute_force_culling.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Window.class)
public class WindowMixin {
    @WrapOperation(remap = false, method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", ordinal = 3))
    private static void onHint(int hint, int value, Operation<Void> original) {
        original.call(hint, bruteForceCulling$supportsGl33() ? 3 : 2);
    }

    @Unique
    private static boolean bruteForceCulling$supportsGl33() {
        if (!GLFW.glfwInit()) {
            return false;
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

        long window = GLFW.glfwCreateWindow(1, 1, "OpenGL 3.3 Test", 0, 0);
        if (window == 0) {
            return false;
        }

        GLFW.glfwDestroyWindow(window);
        return true;
    }
}
