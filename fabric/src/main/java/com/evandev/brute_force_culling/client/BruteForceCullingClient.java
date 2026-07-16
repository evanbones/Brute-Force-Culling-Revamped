package com.evandev.brute_force_culling.client;

import com.evandev.brute_force_culling.culling.CullingRenderEvent;
import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.DebugKeybind;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class BruteForceCullingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CullingStateManager.init();

        KeyBindingHelper.registerKeyBinding(DebugKeybind.DEBUG_KEY);
        ClientTickEvents.START_CLIENT_TICK.register(client -> CullingStateManager.onClientTick());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (DebugKeybind.DEBUG_KEY.consumeClick()) {
                CullingStateManager.toggleDebug();
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> CullingRenderEvent.renderDebugOverlay(guiGraphics));
    }
}
