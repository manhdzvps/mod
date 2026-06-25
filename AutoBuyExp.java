package your.addon.modules.world; // TODO: đổi package cho khớp addon của m

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;

/**
 * AutoBuyExp — tự mở shop, tìm item "trái kinh nghiệm" theo tên hiển thị, click mua.
 * Viết theo flow phổ biến của shop kiểu Donut cũ:
 *   chạy lệnh mở shop -> GUI mở ra -> click item -> (tùy shop) GUI phụ xác nhận
 *   -> click nút xác nhận.
 *
 * QUAN TRỌNG — m cần tự điền 3 chỗ sau theo đúng KingMC, vì t không có
 * cách nào biết chính xác text/lệnh shop của m mà không thấy tận mắt:
 *   - open-command : lệnh hoặc cách mở shop (vd "shop", "warp shop"...).
 *   - item-name    : chuỗi xuất hiện trong tên item "trái kinh nghiệm" trong GUI.
 *   - confirm-name : chuỗi trên nút xác nhận mua, để trống nếu click item là mua luôn.
 * Cách lấy đúng chuỗi: mở shop thủ công, hover/click vào item, xem tên đầy đủ
 * (thường có mã màu, m chỉ cần lấy phần chữ chính không cần dấu màu).
 */
public class AutoBuyExp extends Module {

    private final SettingGroup sgGeneral = settings.createGroup("General");

    private final Setting<String> openCommand = sgGeneral.add(new StringSetting.Builder()
        .name("open-command")
        .description("Lệnh để mở shop, không có dấu / (vd: shop).")
        .defaultValue("shop")
        .build()
    );

    private final Setting<String> itemName = sgGeneral.add(new StringSetting.Builder()
        .name("item-name")
        .description("Chuỗi xuất hiện trong tên item trái kinh nghiệm trong GUI shop.")
        .defaultValue("Kinh Nghiệm")
        .build()
    );

    private final Setting<String> confirmName = sgGeneral.add(new StringSetting.Builder()
        .name("confirm-name")
        .description("Chuỗi trên nút xác nhận mua. Để trống nếu shop mua ngay khi click item.")
        .defaultValue("")
        .build()
    );

    private final Setting<Boolean> useShiftClick = sgGeneral.add(new BoolSetting.Builder()
        .name("shift-click")
        .description("Bật nếu shop dùng shift-click để mua nhanh thay vì click thường.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> buyTimes = sgGeneral.add(new IntSetting.Builder()
        .name("buy-times")
        .description("Số lần lặp lại quy trình mua mỗi khi bật module.")
        .defaultValue(1)
        .min(1)
        .sliderMax(64)
        .build()
    );

    private final Setting<Integer> delayTicks = sgGeneral.add(new IntSetting.Builder()
        .name("step-delay")
        .description("Số tick chờ giữa các bước (mở GUI, click) để server kịp xử lý.")
        .defaultValue(5)
        .min(1)
        .sliderMax(40)
        .build()
    );

    private enum Stage { IDLE, WAIT_OPEN, CLICK_ITEM, WAIT_CONFIRM, CLICK_CONFIRM, DONE }

    private Stage stage = Stage.IDLE;
    private int wait = 0;
    private int bought = 0;

    public AutoBuyExp() {
        super(Categories.World, "auto-buy-exp", "Tự mở shop và mua trái kinh nghiệm.");
    }

    @Override
    public void onActivate() {
        stage = Stage.IDLE;
        bought = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        switch (stage) {
            case IDLE -> {
                ChatUtils.sendPlayerMessage("/" + openCommand.get());
                wait = delayTicks.get() * 4; // mở GUI thường lâu hơn 1 click
                stage = Stage.WAIT_OPEN;
            }
            case WAIT_OPEN -> {
                if (--wait <= 0) {
                    stage = currentScreen() != null ? Stage.CLICK_ITEM : Stage.IDLE;
                }
            }
            case CLICK_ITEM -> {
                HandledScreen<?> screen = currentScreen();
                if (screen == null) { stage = Stage.IDLE; return; }

                ScreenHandler handler = screen.getScreenHandler();
                int slot = GuiShopUtils.findSlotByName(handler, itemName.get());
                if (slot == -1) {
                    warning("Không tìm thấy item '" + itemName.get() + "' trong shop, kiểm tra lại setting item-name.");
                    stage = Stage.IDLE;
                    return;
                }

                if (useShiftClick.get()) GuiShopUtils.shiftClick(mc, handler, slot);
                else GuiShopUtils.leftClick(mc, handler, slot);

                wait = delayTicks.get();
                stage = confirmName.get().isEmpty() ? Stage.DONE : Stage.WAIT_CONFIRM;
            }
            case WAIT_CONFIRM -> {
                if (--wait <= 0) stage = Stage.CLICK_CONFIRM;
            }
            case CLICK_CONFIRM -> {
                HandledScreen<?> screen = currentScreen();
                if (screen == null) { stage = Stage.IDLE; return; }

                ScreenHandler handler = screen.getScreenHandler();
                int slot = GuiShopUtils.findSlotByName(handler, confirmName.get());
                if (slot != -1) GuiShopUtils.leftClick(mc, handler, slot);

                stage = Stage.DONE;
            }
            case DONE -> {
                bought++;
                if (bought < buyTimes.get()) {
                    stage = Stage.IDLE;
                } else {
                    info("Đã mua xong " + bought + " lần trái kinh nghiệm.");
                    toggle(); // tự tắt module sau khi xong
                }
            }
        }
    }

    private HandledScreen<?> currentScreen() {
        return GuiShopUtils.currentHandledScreen(mc);
    }
}
