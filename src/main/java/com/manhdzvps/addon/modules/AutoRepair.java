package com.manhdzvps.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;

public class AutoRepair extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> repairCommand = sgGeneral.add(new StringSetting.Builder()
        .name("repair-command")
        .description("Lệnh sửa đồ.")
        .defaultValue("/repair")
        .build());

    private final Setting<Integer> durabilityThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("durability-threshold")
        .description("Sửa khi độ bền còn bao nhiêu %.")
        .defaultValue(20)
        .min(5).max(50)
        .build());

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Delay giữa các lần sửa (tick).")
        .defaultValue(20)
        .min(10).max(100)
        .build());

    private int timer = 0;

    public AutoRepair() {
        super(KingMCAddonCategory.CATEGORY, "auto-repair", "Tự động sửa đồ khi sắp hỏng.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (timer > 0) { timer--; return; }
        if (needsRepair()) {
            mc.player.networkHandler.sendChatCommand(
                repairCommand.get().replace("/", ""));
            timer = cooldown.get();
        }
    }

    private boolean needsRepair() {
        for (ItemStack stack : mc.player.getInventory().armor) {
            if (stack.isEmpty()) continue;
            int max = stack.getMaxDamage();
            if (max <= 0) continue;
            int current = max - stack.getDamage();
            if ((current * 100 / max) <= durabilityThreshold.get()) return true;
        }
        ItemStack held = mc.player.getMainHandStack();
        if (!held.isEmpty() && held.getMaxDamage() > 0) {
            int max = held.getMaxDamage();
            int current = max - held.getDamage();
            if ((current * 100 / max) <= durabilityThreshold.get()) return true;
        }
        return false;
    }
}
