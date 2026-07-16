package com.evandev.brute_force_culling;

import com.evandev.brute_force_culling.client.ClientConfigSetup;
import com.evandev.brute_force_culling.client.ClientEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class BruteForceCulling {
    public BruteForceCulling(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        if (FMLEnvironment.dist.isClient()) {
            ClientConfigSetup.register(modContainer);
            ClientEvents.init(modEventBus);
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        CommonClass.init();
    }
}