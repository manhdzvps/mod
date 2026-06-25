package com.manhdzvps.addon.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class GuiShopUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static int findSlotByName(String name) {
        if (mc.player == null || mc.player.currentScreenHandler == null) return -1;
        var slots = mc.player.currentScreenHandler.slots;
        for (int i = 0; i < slots.size(); i++) {
            var stack = slots.get(i).getStack();
            if (stack.isEmpty()) continue;
            Text displayName = stack.getName();
            if (displayName.getString().contains(name)) return i;
        }
        return -1;
    }

    public static void clickSlot(int slot) {
        if (mc.player == null || mc.player.currentScreenHandler == null) return;
        mc.interactionManager.clickSlot(
            mc.player.currentScreenHandler.syncId,
            slot, 0, SlotActionType.PICKUP, mc.player
        );
    }

    public static boolean isGuiOpen() {
        return mc.currentScreen != null;
    }
}
