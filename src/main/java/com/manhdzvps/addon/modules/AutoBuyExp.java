package com.manhdzvps.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AutoBuyExp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> openCommand = sgGeneral.add(new StringSetting.Builder()
        .name("open-command")
        .description("Lệnh mở shop mua exp.")
        .defaultValue("/shop")
        .build());

    private final Setting<String> itemName = sgGeneral.add(new StringSetting.Builder()
        .name("item-name")
        .description("Tên item trái kinh nghiệm trong shop.")
        .defaultValue("Trái Kinh Nghiệm")
        .build());

    private final Setting<Integer> stepDelay = sgGeneral.add(new IntSetting.Builder()
        .name("step-delay")
        .description("Delay giữa các bước (tick).")
        .defaultValue(10)
        .min(5).max(40)
        .build());

    private int state = 0;
    private int timer = 0;

    public AutoBuyExp() {
        super(KingMCAddonCategory.CATEGORY, "auto-buy-exp", "Tự động mua trái kinh nghiệm.");
    }

    @Override
    public void onActivate() {
        state = 0;
        timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (timer > 0) { timer--; return; }

        switch (state) {
            case 0 -> {
                mc.player.networkHandler.sendChatCommand(
                    openCommand.get().replace("/", ""));
                timer = stepDelay.get();
                state = 1;
            }
            case 1 -> {
                int slot = GuiShopUtils.findSlotByName(itemName.get());
                if (slot != -1) {
                    GuiShopUtils.clickSlot(slot);
                    timer = stepDelay.get();
                    state = 2;
                }
            }
            case 2 -> {
                if (mc.currentScreen != null)
                    mc.currentScreen.close();
                state = 0;
                timer = stepDelay.get();
            }
        }
    }
}
