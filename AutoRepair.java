package your.addon.modules.world; // TODO: đổi package cho khớp addon của m

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

/**
 * AutoRepair — theo dõi độ bền công cụ đang cầm + giáp đang mặc,
 * khi xuống dưới ngưỡng thì tự sửa.
 *
 * 2 cách sửa (chọn qua setting "method"), tùy KingMC dùng cách nào:
 *   - Command : gửi lệnh sửa đồ (vd /repair, /fix) — phổ biến trên server economy.
 *   - ShopGui : mở GUI sửa đồ kiểu shop, click item xác nhận theo tên hiển thị
 *               (dùng chung GuiShopUtils với AutoBuyExp).
 *
 * Nếu m không chắc KingMC dùng cách nào, thử /repair hoặc /fix thủ công trước
 * trong game — có phản hồi ngay là dùng Command, có GUI mở ra là dùng ShopGui.
 */
public class AutoRepair extends Module {

    public enum Method { Command, ShopGui }

    private final SettingGroup sgGeneral = settings.createGroup("General");

    private final Setting<Integer> threshold = sgGeneral.add(new IntSetting.Builder()
        .name("durability-threshold")
        .description("Sửa khi độ bền còn dưới % này.")
        .defaultValue(15)
        .min(1)
        .sliderMax(50)
        .build()
    );

    private final Setting<Method> method = sgGeneral.add(new EnumSetting.Builder<Method>()
        .name("method")
        .description("Cách sửa đồ trên server.")
        .defaultValue(Method.Command)
        .build()
    );

    private final Setting<String> repairCommand = sgGeneral.add(new StringSetting.Builder()
        .name("repair-command")
        .description("Lệnh sửa đồ, không có dấu / (vd: repair).")
        .defaultValue("repair")
        .visible(() -> method.get() == Method.Command)
        .build()
    );

    private final Setting<String> shopOpenCommand = sgGeneral.add(new StringSetting.Builder()
        .name("shop-open-command")
        .description("Lệnh mở GUI sửa đồ.")
        .defaultValue("repair")
        .visible(() -> method.get() == Method.ShopGui)
        .build()
    );

    private final Setting<String> confirmName = sgGeneral.add(new StringSetting.Builder()
        .name("confirm-item-name")
        .description("Chuỗi tên item xác nhận sửa trong GUI.")
        .defaultValue("Xác Nhận")
        .visible(() -> method.get() == Method.ShopGui)
        .build()
    );

    private final Setting<Integer> cooldownTicks = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Số tick chờ giữa các lần thử sửa, tránh spam khi đang đúng ngưỡng.")
        .defaultValue(100)
        .min(20)
        .sliderMax(600)
        .build()
    );

    private int cooldown = 0;

    public AutoRepair() {
        super(Categories.World, "auto-repair", "Tự sửa công cụ/giáp khi sắp hỏng.");
    }

    @Override
    public void onActivate() {
        cooldown = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        if (cooldown > 0) { cooldown--; return; }
        if (!needsRepair()) return;

        switch (method.get()) {
            case Command -> ChatUtils.sendPlayerMessage("/" + repairCommand.get());
            case ShopGui -> doShopRepair();
        }

        cooldown = cooldownTicks.get();
    }

    private boolean needsRepair() {
        for (ItemStack stack : mc.player.getInventory().armor) {
            if (isLowDurability(stack)) return true;
        }
        return isLowDurability(mc.player.getMainHandStack());
    }

    private boolean isLowDurability(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable()) return false;
        int max = stack.getMaxDamage();
        int dmg = stack.getDamage();
        int remainingPct = (int) (100.0 * (max - dmg) / max);
        return remainingPct <= threshold.get();
    }

    private void doShopRepair() {
        HandledScreen<?> screen = GuiShopUtils.currentHandledScreen(mc);
        if (screen == null) {
            // GUI chưa mở: gửi lệnh mở, lần tick sau (qua cooldown) sẽ thử click
            ChatUtils.sendPlayerMessage("/" + shopOpenCommand.get());
            return;
        }

        ScreenHandler handler = screen.getScreenHandler();
        int slot = GuiShopUtils.findSlotByName(handler, confirmName.get());
        if (slot != -1) GuiShopUtils.leftClick(mc, handler, slot);
    }
}
