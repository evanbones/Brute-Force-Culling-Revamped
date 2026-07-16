package com.evandev.brute_force_culling.client;

import com.evandev.brute_force_culling.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClientConfigSetup {
    public static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> ModConfig.createScreen(parent));
    }
}