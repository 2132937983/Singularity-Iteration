// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.integration;

import com.miophas.singularity_iteration.common.item.crop.CropSeedItem;
import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import com.miophas.singularity_iteration.core.api.crop.PlantType;
import com.miophas.singularity_iteration.core.api.item.ICropSeedItem;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;

/**
 * <p><b>EN:</b> Ender IO Farming Station support for crop sticks (0.1.8.2). Loaded only when Ender IO is
 * present; it uses reflection, so SI has no compile dependency on Ender IO. Tested against the
 * API of Ender IO 8.2.11 ({@code com.enderio.enderio.api.farm}). Two farm tasks:
 * <ul>
 *   <li>PLANT: a seed bag (or a registered base seed) in the station's seed slot for the position
 *       is planted on an empty crop stick on the farmland. The rules are the same as for a player
 *       ({@link CropSeedItem#plantFromMachine}); the bag keeps its traits.</li>
 *   <li>HARVEST: a harvestable crop on a crop stick is harvested with the normal crop rules
 *       ({@link IPlanter#doHarvest()}); the drops go into the station's output slots
 *       ({@code collectDrops}); what does not fit drops on the ground.</li>
 * </ul>
 * The station gives the soil position; the crop stick is the block above it, as for the Ender IO
 * crop tasks. Place the crop sticks by hand.
 * <p><b>ZH:</b> Ender IO 农场站对作物架的支持（0.1.8.2）。仅在 Ender IO 存在时加载；使用反射，因此 SI 不在编译期依赖 Ender IO。
 * 针对 Ender IO 8.2.11 的 API（{@code com.enderio.enderio.api.farm}）。两个农场任务：
 * <ul>
 *   <li>种植：该位置种子槽中的种子袋（或已注册的基础种子）种到耕地上的空作物架上。规则与玩家相同（{@link CropSeedItem#plantFromMachine}），种子袋保留属性。</li>
 *   <li>收获：按正常作物规则（{@link IPlanter#doHarvest()}）收获作物架上可收获的作物，掉落物放入农场站的输出槽（{@code collectDrops}），放不下的掉在地上。</li>
 * </ul>
 * 农场站给出土壤位置，作物架是其上方的方块，与 Ender IO 的作物任务相同。作物架需手动放置。
 * <p><b>JA:</b> Ender IO の Farming Station の作物棒対応（0.1.8.2）。Ender IO があるときだけ読み込む。リフレクションを使うので、SI は Ender IO に
 * コンパイル時の依存を持たない。Ender IO 8.2.11 の API（{@code com.enderio.enderio.api.farm}）に合わせてある。農場タスクは 2 つ：
 * <ul>
 *   <li>植え付け：その位置の種子スロットの種子袋（または登録済みの基本種子）を、耕地の上の空の作物棒に植える。規則はプレイヤーと同じ
 *       （{@link CropSeedItem#plantFromMachine}）。種子袋の特性値は保たれる。</li>
 *   <li>収穫：作物棒の収穫できる作物を通常の作物規則（{@link IPlanter#doHarvest()}）で収穫する。ドロップは Farming Station の出力スロットへ
 *       （{@code collectDrops}）、入らない分は地面に落ちる。</li>
 * </ul>
 * Farming Station は土の位置を渡す。作物棒はその上のブロックで、Ender IO の作物タスクと同じ。作物棒は手で置く。
 */
public final class EnderIOFarmIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String API = "com.enderio.enderio.api.farm.";

    private EnderIOFarmIntegration() {}

    /** EN: Adds the task registration listener (call only when Ender IO is loaded). ZH: 添加任务注册监听器（仅在 Ender IO 已加载时调用）。JA: タスク登録のリスナーを追加する（Ender IO が読み込まれているときだけ呼ぶ）。 */
    @SuppressWarnings("unchecked")
    public static void register(IEventBus modEventBus) {
        try {
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(API + "RegisterFarmTasksEvent");
            Class<?> taskClass = Class.forName(API + "FarmTask");
            Class<?> typeClass = Class.forName(API + "FarmTaskType");
            Class<?> machineClass = Class.forName(API + "FarmingMachine");
            Class<?> interactionClass = Class.forName(API + "FarmInteraction");
            Method registerTask = eventClass.getMethod("register", typeClass, taskClass);
            Method seedsForPos = machineClass.getMethod("getSeedsForPos", BlockPos.class);
            Object finished = enumConstant(interactionClass, "FINISHED"), ignored = enumConstant(interactionClass, "IGNORED");
            Object plantType = enumConstant(typeClass, "PLANT"), harvestType = enumConstant(typeClass, "HARVEST");
            Object plantTask = task(taskClass, (pos, machine) -> plant(pos, machine, seedsForPos) ? finished : ignored);
            Object harvestTask = task(taskClass, (pos, machine) -> harvest(pos, machine) ? finished : ignored);
            modEventBus.addListener(EventPriority.NORMAL, false, eventClass, event -> {
                try {
                    registerTask.invoke(event, plantType, plantTask);
                    registerTask.invoke(event, harvestType, harvestTask);
                    LOGGER.info("[SI] Ender IO Farming Station: crop stick plant and harvest tasks registered");
                } catch (ReflectiveOperationException failure) {
                    LOGGER.warn("[SI] Ender IO farm task registration failed", failure);
                }
            });
        } catch (ReflectiveOperationException | LinkageError failure) {
            LOGGER.warn("[SI] Ender IO farm API not found; crop stick support for the Farming Station is off", failure);
        }
    }

    private interface Body { Object run(BlockPos pos, BlockEntity machine); }

    private static Object task(Class<?> taskClass, Body body) {
        return Proxy.newProxyInstance(EnderIOFarmIntegration.class.getClassLoader(), new Class<?>[]{taskClass}, (proxy, method, args) -> {
            if ("process".equals(method.getName()) && args != null && args.length == 2
                    && args[0] instanceof BlockPos pos && args[1] instanceof BlockEntity machine) return body.run(pos, machine);
            return switch (method.getName()) {
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == (args == null ? null : args[0]);
                case "toString" -> "SI crop stick farm task";
                default -> null;
            };
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(Class<?> type, String name) {
        return Enum.valueOf((Class<? extends Enum>) type, name);
    }

    /** EN: Plants on the crop stick above {@code soil}; true when it planted. ZH: 在 {@code soil} 上方的作物架上种植；种植成功时为真。JA: {@code soil} の上の作物棒に植える。植えたら真。 */
    static boolean plant(BlockPos soil, BlockEntity machine, Method seedsForPos) {
        Level level = machine.getLevel();
        if (level == null || level.isClientSide) return false;
        BlockPos stick = soil.above();
        if (!level.isLoaded(stick) || !(level.getBlockEntity(stick) instanceof IPlanter planter)
                || planter.getPlant() != null || planter.isHybridBase()) return false;
        try {
            ItemStack seeds = (ItemStack) seedsForPos.invoke(machine, soil);
            if (seeds == null || seeds.isEmpty()) return false;
            if (seeds.getItem() instanceof ICropSeedItem bag) {
                PlantType plant = com.miophas.singularity_iteration.core.runtime.crop.PlantRegistry.instance.getPlant(bag.cropModId(seeds), bag.cropId(seeds));
                if (plant == null) return false;
            }
            return CropSeedItem.plantFromMachine(level, planter, seeds);
        } catch (ReflectiveOperationException failure) {
            return false;
        }
    }

    /** EN: Harvests the crop stick above {@code soil}; true when it harvested. ZH: 收获 {@code soil} 上方的作物架；收获成功时为真。JA: {@code soil} の上の作物棒を収穫する。収穫したら真。 */
    static boolean harvest(BlockPos soil, BlockEntity machine) {
        Level level = machine.getLevel();
        if (level == null || level.isClientSide) return false;
        BlockPos stick = soil.above();
        if (!level.isLoaded(stick) || !(level.getBlockEntity(stick) instanceof IPlanter planter)) return false;
        PlantType plant = planter.getPlant();
        if (plant == null || !plant.isHarvestable(planter)) return false;
        List<ItemStack> drops = new ArrayList<>(planter.doHarvest());
        if (drops.isEmpty()) return false;
        try {
            Method collect = machine.getClass().getMethod("collectDrops", List.class, BlockPos.class);
            collect.invoke(machine, drops, stick);
        } catch (ReflectiveOperationException failure) {
            // EN: No collectDrops: everything drops on the ground.
            // ZH: 没有 collectDrops：全部掉在地上。
            // JA: collectDrops がない：すべて地面に落とす。
        }
        for (ItemStack rest : drops) {
            if (!rest.isEmpty()) level.addFreshEntity(new ItemEntity(level, stick.getX() + 0.5, stick.getY() + 0.5, stick.getZ() + 0.5, rest.copy()));
        }
        return true;
    }
}
