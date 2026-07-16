package com.evandev.brute_force_culling.client;

import com.evandev.brute_force_culling.culling.CullingRenderEvent;
import com.evandev.brute_force_culling.culling.CullingStateManager;
import com.evandev.brute_force_culling.culling.DebugKeybind;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public class ClientEvents {
    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ClientEvents::clientSetup);
        modEventBus.addListener(ClientEvents::registerKeyMappings);

        NeoForge.EVENT_BUS.addListener(ClientEvents::onKeyInput);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRenderGui);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Pre event) {
        CullingStateManager.onClientTick();
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        CullingStateManager.init();
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(DebugKeybind.DEBUG_KEY);
    }

    private static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && DebugKeybind.DEBUG_KEY.matches(event.getKey(), event.getScanCode())) {
            CullingStateManager.toggleDebug();
        }
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        CullingRenderEvent.renderDebugOverlay(event.getGuiGraphics());
    }
}
