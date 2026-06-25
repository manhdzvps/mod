package your.addon.modules.world; // TODO: đổi package cho khớp addon của m

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Helper dùng chung cho các module thao tác GUI shop (kiểu Donut cũ /
 * ShopGUI+ — chest GUI với item đại diện, click để mua/bán).
 *
 * Vì mỗi server đặt tên item khác nhau, các hàm tìm theo TÊN HIỂN THỊ
 * (displayName chứa chuỗi, không phân biệt hoa thường) thay vì hardcode
 * slot index — m chỉ cần điền đúng chuỗi tên item hiển thị trong shop
 * thật của KingMC vào setting tương ứng ở từng module.
 */
public class GuiShopUtils {

    public static HandledScreen<?> currentHandledScreen(MinecraftClient mc) {
        if (mc.currentScreen instanceof HandledScreen<?> screen) return screen;
        return null;
    }

    /** Tìm slot đầu tiên có tên hiển thị chứa "contains" (không phân biệt hoa thường). */
    public static int findSlotByName(ScreenHandler handler, String contains) {
        if (contains == null || contains.isEmpty()) return -1;
        String needle = contains.toLowerCase();

        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            String name = stack.getName().getString().toLowerCase();
            if (name.contains(needle)) return slot.id;
        }
        return -1;
    }

    /** Click chuột trái (pickup) vào 1 slot trong GUI hiện tại. */
    public static void leftClick(MinecraftClient mc, ScreenHandler handler, int slotId) {
        mc.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.PICKUP, mc.player);
    }

    /** Shift-click — dùng khi shop dùng shift-click để mua/bán nhanh. */
    public static void shiftClick(MinecraftClient mc, ScreenHandler handler, int slotId) {
        mc.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
    }

    /** Click chuột phải — vài shop dùng phải/trái để +/- số lượng trước khi xác nhận. */
    public static void rightClick(MinecraftClient mc, ScreenHandler handler, int slotId) {
        mc.interactionManager.clickSlot(handler.syncId, slotId, 1, SlotActionType.PICKUP, mc.player);
    }
}
