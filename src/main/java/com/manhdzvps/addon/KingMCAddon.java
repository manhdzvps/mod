package com.manhdzvps.addon;

import com.manhdzvps.addon.modules.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.api.ClientModInitializer;

public class KingMCAddon extends MeteorAddon implements ClientModInitializer {
    public static final KingMCAddon INSTANCE = new KingMCAddon();

    @Override
    public void onInitialize() {
        Modules.get().add(new AutoLumberjack());
        Modules.get().add(new AutoJunkDiscard());
        Modules.get().add(new AutoBuyExp());
        Modules.get().add(new AutoRepair());
    }

    @Override
    public void onInitializeClient() {
        onInitialize();
    }

    @Override
    public String getPackage() {
        return "com.manhdzvps.addon";
    }
}
