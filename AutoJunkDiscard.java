package your.addon.modules.world; // TODO: đổi package cho khớp addon của m

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Arrays;
import java.util.List;

/**
 * AutoJunkDiscard — tự vứt hết item KHÔNG nằm trong whitelist.
 * Mặc định giữ lại các loại sapling (mầm). Hợp lý cho server economy
 * kiểu auto-sell-on-break, không cần giữ log/cobble/đồ thừa trong túi.
 *
 * LƯU Ý: InvUtils.dropSlot(...) là tên hàm phổ biến trong Meteor Client,
 * nếu version m dùng đặt tên khác (vd InvUtils.drop(slot)) thì đổi lại
 * 1 dòng đó, logic còn lại không đổi.
 */
public class AutoJunkDiscard extends Module {

    private final SettingGroup sgGeneral = settings.createGroup("General");

    private final Setting<List<Item>> whitelist = sgGeneral.add(new ItemListSetting.Builder()
        .name("whitelist")
        .description("Item được giữ lại, không bị vứt.")
        .defaultValue(Arrays.asList(
            Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING,
            Items.JUNGLE_SAPLING, Items.DARK_OAK_SAPLING, Items.ACACIA_SAPLING
        ))
        .build()
    );

    private final Setting<Boolean> keepTools = sgGeneral.add(new BoolSetting.Builder()
        .name("keep-tools")
        .description("Luôn giữ cuốc/rìu/kiếm/xẻng có durability, không vứt công cụ.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> interval = sgGeneral.add(new IntSetting.Builder()
        .name("check-interval")
        .description("Số tick giữa mỗi lần quét túi đồ.")
        .defaultValue(20)
        .min(5)
        .sliderMax(100)
        .build()
    );

    private int tickCounter = 0;

    public AutoJunkDiscard() {
        super(Categories.World, "auto-junk-discard", "Tự vứt item thừa, chỉ giữ whitelist (mặc định: sapling).");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        tickCounter++;
        if (tickCounter % interval.get() != 0) return;

        int size = mc.player.getInventory().size();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            if (whitelist.get().contains(item)) continue;
            if (keepTools.get() && isTool(item)) continue;

            InvUtils.dropSlot(slot);
        }
    }

    private boolean isTool(Item item) {
        String name = item.toString().toLowerCase();
        return name.contains("axe") || name.contains("pickaxe")
            || name.contains("sword") || name.contains("hoe") || name.contains("shovel");
    }
}
