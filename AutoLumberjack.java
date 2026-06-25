package your.addon.modules.world; // TODO: đổi thành package thật trong addon của m

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * AutoLumberjack — module Meteor Client để tự trồng + tự chặt cây.
 * Hỗ trợ cả mode thường (1 sapling/ô) và giant tree mode (cụm 2x2,
 * áp dụng cho Oak / Spruce / Dark Oak).
 *
 * Cách thêm vào addon (giống meteor-rejects):
 *   1. Copy file này vào package modules trong addon của m.
 *   2. Trong class Addon, chỗ onInitialize(), thêm:
 *        Modules.get().add(new AutoLumberjack());
 *   3. Sửa lại tên package ở dòng 1 cho khớp project.
 *
 * LƯU Ý: code dùng các util phổ biến của Meteor Client (BlockUtils,
 * InvUtils...) — tên method có thể lệch chút theo đúng version Meteor
 * m đang build. Nếu lỗi compile, m chỉ cần đối chiếu lại signature
 * trong source Meteor Client / meteor-rejects hiện có của m rồi sửa tên gọi,
 * logic tổng thể không đổi.
 *
 * Phần Baritone (pathTo) để dạng TODO vì API Baritone phụ thuộc version
 * m đang dùng — t note rõ chỗ cần điền bên dưới.
 */
public class AutoLumberjack extends Module {

    public enum SaplingType {
        Oak(Blocks.OAK_SAPLING, Blocks.OAK_LOG),
        Spruce(Blocks.SPRUCE_SAPLING, Blocks.SPRUCE_LOG),
        Birch(Blocks.BIRCH_SAPLING, Blocks.BIRCH_LOG),
        Jungle(Blocks.JUNGLE_SAPLING, Blocks.JUNGLE_LOG),
        DarkOak(Blocks.DARK_OAK_SAPLING, Blocks.DARK_OAK_LOG),
        Acacia(Blocks.ACACIA_SAPLING, Blocks.ACACIA_LOG);

        public final Block sapling, log;

        SaplingType(Block sapling, Block log) {
            this.sapling = sapling;
            this.log = log;
        }

        public Item saplingItem() {
            return sapling.asItem();
        }
    }

    // ---------- Settings ----------

    private final SettingGroup sgGeneral = settings.createGroup("General");
    private final SettingGroup sgGiant = settings.createGroup("Giant Tree (2x2)");

    private final Setting<SaplingType> saplingType = sgGeneral.add(new EnumSetting.Builder<SaplingType>()
        .name("sapling-type")
        .description("Loại cây để trồng và chặt.")
        .defaultValue(SaplingType.Spruce)
        .build()
    );

    private final Setting<BlockPos> pos1 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("pos1")
        .description("Góc 1 của khu vực farm.")
        .build()
    );

    private final Setting<BlockPos> pos2 = sgGeneral.add(new BlockPosSetting.Builder()
        .name("pos2")
        .description("Góc 2 của khu vực farm.")
        .build()
    );

    private final Setting<Boolean> autoReplant = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-replant")
        .description("Tự trồng lại sapling sau khi chặt xong.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoEquipAxe = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-equip-axe")
        .description("Tự động cầm rìu khi chặt.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> reach = sgGeneral.add(new DoubleSetting.Builder()
        .name("reach")
        .description("Khoảng cách chặt/trồng tối đa (block).")
        .defaultValue(4.5)
        .min(1)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> useBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("use-baritone")
        .description("Dùng Baritone để di chuyển tới cây ngoài tầm với.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> instantFell = sgGeneral.add(new BoolSetting.Builder()
        .name("instant-fell")
        .description("Bật nếu server có plugin/rìu chặt rụng cả cây chỉ 1 nhát (kiểu treecapitator). Tắt thì dùng flood-fill chặt từng log như vanilla.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> giantTreeMode = sgGiant.add(new BoolSetting.Builder()
        .name("giant-tree-mode")
        .description("Trồng theo cụm 2x2 để có cơ hội ra giant tree. Chỉ Oak/Spruce/Dark Oak hỗ trợ giant tree trong vanilla.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> giantSpacing = sgGiant.add(new IntSetting.Builder()
        .name("plot-spacing")
        .description("Số block trống giữa các cụm 2x2 (để có chỗ cho cây to lên).")
        .defaultValue(3)
        .min(0)
        .sliderMax(8)
        .visible(giantTreeMode::get)
        .build()
    );

    // ---------- State ----------

    private final List<BlockPos[]> giantClusters = new ArrayList<>();
    private int tickCounter = 0;

    public AutoLumberjack() {
        super(Categories.World, "auto-lumberjack", "Tự động trồng và chặt cây, hỗ trợ giant tree 2x2.");
    }

    @Override
    public void onActivate() {
        giantClusters.clear();
        tickCounter = 0;
        if (giantTreeMode.get()) buildGiantClusters();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        tickCounter++;
        if (tickCounter % 5 != 0) return; // quét mỗi 5 tick, tránh spam packet

        SaplingType type = saplingType.get();

        if (giantTreeMode.get()) {
            tickGiantMode(type);
        } else {
            tickNormalMode(type);
        }
    }

    // ---------- Mode thường: 1 sapling / ô ----------

    private void tickNormalMode(SaplingType type) {
        for (BlockPos pos : area()) {
            BlockPos above = pos.up();
            Block ground = mc.world.getBlockState(pos).getBlock();
            Block aboveBlock = mc.world.getBlockState(above).getBlock();

            if (aboveBlock == type.sapling) continue; // đang chờ lớn

            if (isLog(above, type)) {
                chopTree(above, type, 64);
                return; // 1 cây / lần quét
            }

            if (autoReplant.get() && isPlantable(ground) && aboveBlock == Blocks.AIR) {
                plantSapling(above, type);
                return;
            }
        }
    }

    // ---------- Mode giant tree: cụm 2x2 ----------

    private void tickGiantMode(SaplingType type) {
        if (!(type == SaplingType.Oak || type == SaplingType.Spruce || type == SaplingType.DarkOak)) {
            warning("Giant tree chỉ hỗ trợ Oak, Spruce, Dark Oak trong vanilla.");
            return;
        }

        if (giantClusters.isEmpty()) buildGiantClusters();

        for (BlockPos[] cluster : giantClusters) {
            BlockPos base = cluster[0].up();

            if (isLog(base, type) && isGiantTrunk(base, type)) {
                chopTree(base, type, 256); // giant tree to hơn, tăng giới hạn flood-fill
                return;
            }

            for (BlockPos sap : cluster) {
                BlockPos above = sap.up();
                Block ground = mc.world.getBlockState(sap).getBlock();
                Block aboveBlock = mc.world.getBlockState(above).getBlock();

                if (autoReplant.get() && isPlantable(ground) && aboveBlock == Blocks.AIR) {
                    plantSapling(above, type);
                }
            }
        }
    }

    private void buildGiantClusters() {
        giantClusters.clear();
        int step = 2 + giantSpacing.get();

        BlockPos min = minPos(), max = maxPos();
        for (int x = min.getX(); x + 1 <= max.getX(); x += step) {
            for (int z = min.getZ(); z + 1 <= max.getZ(); z += step) {
                BlockPos a = new BlockPos(x, min.getY(), z);
                BlockPos b = new BlockPos(x + 1, min.getY(), z);
                BlockPos c = new BlockPos(x, min.getY(), z + 1);
                BlockPos d = new BlockPos(x + 1, min.getY(), z + 1);
                giantClusters.add(new BlockPos[]{a, b, c, d});
            }
        }
    }

    private boolean isGiantTrunk(BlockPos base, SaplingType type) {
        int logNeighbors = 0;
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            if (isLog(base.offset(d), type)) logNeighbors++;
        }
        return logNeighbors >= 2; // thân 2x2 sẽ có >=2 hướng liền log
    }

    // ---------- Chặt cây (flood fill log liền kề) ----------

    private void chopTree(BlockPos start, SaplingType type, int limit) {
        List<BlockPos> logs;
        if (instantFell.get()) {
            // server tự rụng cả cây khi chặt gốc, chỉ cần break 1 block
            logs = Collections.singletonList(start);
        } else {
            logs = floodFillLogs(start, type, limit);
        }
        breakBlocks(logs);
    }

    private List<BlockPos> floodFillLogs(BlockPos start, SaplingType type, int limit) {
        List<BlockPos> result = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos cur = queue.poll();
            if (!isLog(cur, type)) continue;
            result.add(cur);

            for (Direction d : Direction.values()) {
                BlockPos n = cur.offset(d);
                if (!visited.contains(n) && isLog(n, type)) {
                    visited.add(n);
                    queue.add(n);
                }
            }
        }
        return result;
    }

    private void breakBlocks(List<BlockPos> blocks) {
        if (autoEquipAxe.get()) equipAxe();

        // chặt từ trên xuống để tránh log rớt giữa chừng gây kẹt
        blocks.sort((a, b) -> b.getY() - a.getY());

        for (BlockPos pos : blocks) {
            double dist = mc.player.getPos().distanceTo(
                new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)
            );

            if (dist > reach.get()) {
                if (useBaritone.get()) pathTo(pos);
                continue; // bỏ qua, lần quét sau xử lý tiếp khi đã tới gần
            }

            BlockUtils.breakBlock(pos);
        }
    }

    // ---------- Trồng sapling ----------

    private void plantSapling(BlockPos pos, SaplingType type) {
        int slot = InvUtils.findInHotbar(type.saplingItem()).slot();
        if (slot == -1) {
            warning("Hết sapling trong hotbar.");
            return;
        }

        InvUtils.swap(slot, false);
        BlockUtils.place(pos, InvUtils.findInHotbar(type.saplingItem()), false);
    }

    private void equipAxe() {
        int slot = InvUtils.findInHotbar(item -> item.toString().toLowerCase().contains("axe")).slot();
        if (slot != -1) InvUtils.swap(slot, false);
    }

    // ---------- Baritone (điền theo version m dùng) ----------

    private void pathTo(BlockPos pos) {
        // TODO: gọi Baritone API thật, ví dụ (Baritone API chuẩn):
        //
        // BaritoneAPI.getProvider().getPrimaryBaritone()
        //     .getCustomGoalProcess()
        //     .setGoalAndPath(new GoalBlock(pos));
        //
        // Để trống vì cần khớp đúng version Baritone m đang fork/build.
    }

    // ---------- Helpers ----------

    private boolean isLog(BlockPos pos, SaplingType type) {
        return mc.world.getBlockState(pos).getBlock() == type.log;
    }

    private boolean isPlantable(Block b) {
        return b == Blocks.GRASS_BLOCK || b == Blocks.DIRT || b == Blocks.PODZOL || b == Blocks.ROOTED_DIRT;
    }

    private BlockPos minPos() {
        BlockPos a = pos1.get(), b = pos2.get();
        return new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
    }

    private BlockPos maxPos() {
        BlockPos a = pos1.get(), b = pos2.get();
        return new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    private Iterable<BlockPos> area() {
        BlockPos min = minPos(), max = maxPos();
        List<BlockPos> list = new ArrayList<>();
        for (int x = min.getX(); x <= max.getX(); x++)
            for (int z = min.getZ(); z <= max.getZ(); z++)
                list.add(new BlockPos(x, min.getY(), z));
        return list;
    }
}
