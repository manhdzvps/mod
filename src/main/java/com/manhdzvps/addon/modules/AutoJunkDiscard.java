package com.manhdzvps.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class AutoJunkDiscard extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> keepSapling = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-sapling")
        .description("Giữ lại sapling, vứt hết còn lại.")
        .defaultValue(true)
        .build());

    public AutoJunkDiscard() {
        super(KingMCAddonCategory.CATEGORY, "auto-junk-discard", "Tự động vứt item rác.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            var stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (isJunk(item)) {
                mc.player.getInventory().removeStack(i);
                mc.player.dropItem(stack, false);
            }
        }
    }

    private boolean isJunk(Item item) {
        if (keepSapling.get()) {
            if (item == Items.OAK_SAPLING || item == Items.BIRCH_SAPLING
                || item == Items.SPRUCE_SAPLING || item == Items.JUNGLE_SAPLING
                || item == Items.ACACIA_SAPLING || item == Items.DARK_OAK_SAPLING
                || item == Items.MANGROVE_PROPAGULE || item == Items.CHERRY_SAPLING)
                return false;
        }
        return item == Items.STICK || item == Items.APPLE;
    }
}
