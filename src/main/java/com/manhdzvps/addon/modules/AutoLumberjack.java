package com.manhdzvps.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

public class AutoLumberjack extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> instantFell = sgGeneral.add(new BoolSetting.Builder()
        .name("instant-fell")
        .description("Chỉ break 1 block gốc, server tự chặt cả cây.")
        .defaultValue(true)
        .build());

    public AutoLumberjack() {
        super(KingMCAddonCategory.CATEGORY, "auto-lumberjack", "Tự động chặt cây.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        BlockPos pos = findNearbyLog();
        if (pos == null) return;
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        mc.interactionManager.attackBlock(pos,
            net.minecraft.util.math.Direction.UP);
    }

    private BlockPos findNearbyLog() {
        BlockPos origin = mc.player.getBlockPos();
        for (int x = -5; x <= 5; x++)
            for (int y = 0; y <= 8; y++)
                for (int z = -5; z <= 5; z++) {
                    BlockPos p = origin.add(x, y, z);
                    Block b = mc.world.getBlockState(p).getBlock();
                    if (b == Blocks.OAK_LOG || b == Blocks.BIRCH_LOG
                        || b == Blocks.SPRUCE_LOG || b == Blocks.JUNGLE_LOG
                        || b == Blocks.ACACIA_LOG || b == Blocks.DARK_OAK_LOG
                        || b == Blocks.MANGROVE_LOG || b == Blocks.CHERRY_LOG)
                        return p;
                }
        return null;
    }
}
