package com.evandev.brute_force_culling.culling;

import com.evandev.brute_force_culling.Constants;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class DebugKeybind {
    public static final KeyMapping DEBUG_KEY = new KeyMapping(
            Constants.MOD_ID + ".key.debug",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.category." + Constants.MOD_ID);

    private DebugKeybind() {
    }
}
