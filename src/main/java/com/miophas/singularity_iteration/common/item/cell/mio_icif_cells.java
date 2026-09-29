package com.miophas.singularity_iteration.common.item.cell;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.core.api.item.IFluidCellItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings("null")
public class mio_icif_cells {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    public static final DeferredItem<mio_icif_dynamic_cell> CELL_EMPTY = ITEMS.register("cell/item_cell_empty",
            () -> new mio_icif_dynamic_cell(new Item.Properties().stacksTo(64)));

    // 水单元 - 装有1000mb水
    public static final DeferredItem<mio_icif_cell> CELL_WATER = ITEMS.register("cell/item_cell_water",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), Fluids.WATER));

    // 岩浆单元 - 装有1000mb岩浆
    public static final DeferredItem<mio_icif_cell> CELL_LAVA = ITEMS.register("cell/item_cell_lava",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), Fluids.LAVA));

    // 生物气体单元
    public static final DeferredItem<mio_icif_cell> CELL_BIOGAS = ITEMS.register("cell/item_cell_biogas",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.BIOGAS.get()));

    // 热水单元
    public static final DeferredItem<mio_icif_cell> CELL_HOTWATER = ITEMS.register("cell/item_cell_hotwater",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.HOTWATER.get()));

    // 生物质单元
    public static final DeferredItem<mio_icif_cell> CELL_BIOMASS = ITEMS.register("cell/item_cell_biomass",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.BIOMASS.get()));

    // 建筑泡沫单元
    public static final DeferredItem<mio_icif_cell> CELL_CONSTRUCTIONFOAM = ITEMS.register("cell/item_cell_constructionfoam",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.CONSTRUCTIONFOAM.get()));

    // 冷却液单元
    public static final DeferredItem<mio_icif_cell> CELL_COOLANT = ITEMS.register("cell/item_cell_coolant",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.COOLANT.get()));

    // 蒸馏水单元
    public static final DeferredItem<mio_icif_cell> CELL_DISTILLEDWATER = ITEMS.register("cell/item_cell_distilledwater",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.DISTILLEDWATER.get()));

    // 热冷却液单元
    public static final DeferredItem<mio_icif_cell> CELL_HOTCOOLANT = ITEMS.register("cell/item_cell_hotcoolant",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.HOTCOOLANT.get()));

    // 熔岩单元 (Pahoehoe Lava)
    public static final DeferredItem<mio_icif_cell> CELL_PAHOEHOELAVA = ITEMS.register("cell/item_cell_pahoehoelava",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.PAHOEHOELAVA.get()));

    // 蒸汽单元
    public static final DeferredItem<mio_icif_cell> CELL_STEAM = ITEMS.register("cell/item_cell_steam",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.STEAM.get()));

    // 过热蒸汽单元
    public static final DeferredItem<mio_icif_cell> CELL_SUPERHEATEDSTEAM = ITEMS.register("cell/item_cell_superheatedsteam",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.SUPERHEATEDSTEAM.get()));

    // UU物质单元
    public static final DeferredItem<mio_icif_cell> CELL_UUMATTER = ITEMS.register("cell/item_cell_uumatter",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.UUMATTER.get()));

    // 压缩空气单元
    public static final DeferredItem<mio_icif_cell> CELL_AIR = ITEMS.register("cell/item_cell_air",
            () -> new mio_icif_cell(new Item.Properties().stacksTo(64).craftRemainder(mio_icif_cells.CELL_EMPTY.get()), mio_icif_fluids.AIR.get()));


    // ==================== 流体 ↔ 静态单元 单一映射表 ====================
    // 新增一种流体只需在下方 define(...) 增加一行；物品描述 ID / 客户端类型 ID / 满容器
    // 全部由这张表派生，避免此前 6 处硬编码列表各自维护而漏改的问题。

    /** 一个静态流体单元的定义：绑定流体、客户端模型类型 ID、物品描述 ID、对应物品。 */
    public record CellDefinition(Fluid fluid, int typeId, String descriptionId, DeferredItem<mio_icif_cell> item) {
    }

    // 表在建表时才读取 DeferredHolder.get()，因此必须惰性构建：
    // 类加载 / 模组构造阶段流体尚未注册，若在 static 初始化里 get() 会崩溃。
    private static volatile List<CellDefinition> staticCellDefinitions;
    private static volatile Map<Fluid, CellDefinition> staticCellsByFluid;

    private static Map<Fluid, CellDefinition> byFluid() {
        Map<Fluid, CellDefinition> cache = staticCellsByFluid;
        if (cache != null) return cache;
        synchronized (mio_icif_cells.class) {
            if (staticCellsByFluid != null) return staticCellsByFluid;
            List<CellDefinition> definitions = new ArrayList<>();
            definitions.add(new CellDefinition(Fluids.WATER, CELL_TYPE_WATER, "item.mio_icif.cell_water", CELL_WATER));
            definitions.add(new CellDefinition(Fluids.LAVA, CELL_TYPE_LAVA, "item.mio_icif.cell_lava", CELL_LAVA));
            definitions.add(new CellDefinition(mio_icif_fluids.BIOGAS.get(), CELL_TYPE_BIOGAS,
                    "item.mio_icif.cell_biogas", CELL_BIOGAS));
            definitions.add(new CellDefinition(mio_icif_fluids.HOTWATER.get(), CELL_TYPE_HOTWATER,
                    "item.mio_icif.cell_hotwater", CELL_HOTWATER));
            definitions.add(new CellDefinition(mio_icif_fluids.BIOMASS.get(), CELL_TYPE_BIOMASS,
                    "item.mio_icif.cell_biomass", CELL_BIOMASS));
            definitions.add(new CellDefinition(mio_icif_fluids.CONSTRUCTIONFOAM.get(), CELL_TYPE_CONSTRUCTIONFOAM,
                    "item.mio_icif.cell_constructionfoam", CELL_CONSTRUCTIONFOAM));
            definitions.add(new CellDefinition(mio_icif_fluids.COOLANT.get(), CELL_TYPE_COOLANT,
                    "item.mio_icif.cell_coolant", CELL_COOLANT));
            definitions.add(new CellDefinition(mio_icif_fluids.DISTILLEDWATER.get(), CELL_TYPE_DISTILLEDWATER,
                    "item.mio_icif.cell_distilledwater", CELL_DISTILLEDWATER));
            definitions.add(new CellDefinition(mio_icif_fluids.HOTCOOLANT.get(), CELL_TYPE_HOTCOOLANT,
                    "item.mio_icif.cell_hotcoolant", CELL_HOTCOOLANT));
            definitions.add(new CellDefinition(mio_icif_fluids.PAHOEHOELAVA.get(), CELL_TYPE_PAHOEHOELAVA,
                    "item.mio_icif.cell_pahoehoelava", CELL_PAHOEHOELAVA));
            definitions.add(new CellDefinition(mio_icif_fluids.STEAM.get(), CELL_TYPE_STEAM,
                    "item.mio_icif.cell_steam", CELL_STEAM));
            definitions.add(new CellDefinition(mio_icif_fluids.SUPERHEATEDSTEAM.get(), CELL_TYPE_SUPERHEATEDSTEAM,
                    "item.mio_icif.cell_superheatedsteam", CELL_SUPERHEATEDSTEAM));
            definitions.add(new CellDefinition(mio_icif_fluids.UUMATTER.get(), CELL_TYPE_UUMATTER,
                    "item.mio_icif.cell_uumatter", CELL_UUMATTER));
            definitions.add(new CellDefinition(mio_icif_fluids.AIR.get(), CELL_TYPE_AIR,
                    "item.mio_icif.cell_air", CELL_AIR));

            Map<Fluid, CellDefinition> map = new IdentityHashMap<>();
            for (CellDefinition definition : definitions) {
                map.put(definition.fluid(), definition);
            }
            staticCellDefinitions = List.copyOf(definitions);
            staticCellsByFluid = map;
            return map;
        }
    }

    /** 所有已定义的静态单元（注册顺序）。 */
    public static List<CellDefinition> staticCellDefinitions() {
        byFluid();
        return staticCellDefinitions;
    }

    /** 该流体是否存在对应的静态单元。 */
    public static boolean hasStaticCell(Fluid fluid) {
        return byFluid().containsKey(fluid);
    }

    /** 该流体的静态单元定义（可能为 null）。 */
    @Nullable
    public static CellDefinition getDefinitionForFluid(Fluid fluid) {
        return byFluid().get(fluid);
    }

    /** 静态单元的物品描述 ID；无对应单元时返回 null。 */
    @Nullable
    public static String getDescriptionIdForFluid(Fluid fluid) {
        CellDefinition definition = byFluid().get(fluid);
        return definition != null ? definition.descriptionId() : null;
    }

    /** 单元的容量（非单元或空栈返回 0）。 */
    public static int getCellCapacity(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        return stack.getItem() instanceof IFluidCellItem cell ? cell.getCapacity() : 0;
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    /**
     * 根据流体类型获取对应的已填充单元物品
     */
    @Nullable
    public static Item getFilledCellForFluid(Fluid fluid) {
        CellDefinition definition = byFluid().get(fluid);
        return definition != null ? definition.item().get() : null;
    }

    public static final int CELL_TYPE_EMPTY = 0;
    public static final int CELL_TYPE_WATER = 1;
    public static final int CELL_TYPE_LAVA = 2;
    public static final int CELL_TYPE_BIOGAS = 3;
    public static final int CELL_TYPE_HOTWATER = 4;
    public static final int CELL_TYPE_BIOMASS = 5;
    public static final int CELL_TYPE_CONSTRUCTIONFOAM = 6;
    public static final int CELL_TYPE_COOLANT = 7;
    public static final int CELL_TYPE_DISTILLEDWATER = 8;
    public static final int CELL_TYPE_HOTCOOLANT = 9;
    public static final int CELL_TYPE_PAHOEHOELAVA = 10;
    public static final int CELL_TYPE_STEAM = 11;
    public static final int CELL_TYPE_SUPERHEATEDSTEAM = 12;
    public static final int CELL_TYPE_UUMATTER = 13;
    public static final int CELL_TYPE_AIR = 14;
    public static final int CELL_TYPE_GENERIC = 15;

    public static int getCellTypeForFluid(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return CELL_TYPE_EMPTY;
        CellDefinition definition = byFluid().get(fluid);
        return definition != null ? definition.typeId() : CELL_TYPE_GENERIC;
    }

    /**
     * 创建部分填充的流体单元。
     * 如果流体类型有对应的静态单元，返回静态单元（带 NBT 部分填充数据）；
     * 否则返回动态单元。
     *
     * @param fluid 流体类型
     * @param amount 填充量（mB）
     * @return 部分填充的单元物品堆
     */
    public static ItemStack createPartiallyFilledCell(Fluid fluid, int amount) {
        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            return ItemStack.EMPTY;
        }
        
        // 尝试获取静态单元
        Item staticCell = getFilledCellForFluid(fluid);
        if (staticCell != null && staticCell instanceof mio_icif_cell cell) {
            ItemStack stack = new ItemStack(staticCell);
            int fillAmount = Math.min(amount, cell.getCapacity());
            if (fillAmount < cell.getCapacity()) {
                // 部分填充：写入 NBT
                cell.writeFluidToNBT(stack, new net.neoforged.neoforge.fluids.FluidStack(fluid, fillAmount));
            }
            return stack;
        }
        
        if (CELL_EMPTY.get() instanceof mio_icif_dynamic_cell dynamicCell) {
            ItemStack stack = new ItemStack(dynamicCell);
            int fillAmount = Math.min(amount, dynamicCell.getCapacity());
            dynamicCell.writeFluidToNBT(stack, new net.neoforged.neoforge.fluids.FluidStack(fluid, fillAmount));
            return stack;
        }
        
        return ItemStack.EMPTY;
    }

    // ==================== 通用流体单元工具方法 ====================

    /**
     * 检查物品堆是否为包含指定流体的单元（静态或动态）。
     */
    public static boolean isCellContainingFluid(ItemStack stack, Fluid fluid) {
        if (stack.isEmpty() || fluid == null || fluid == Fluids.EMPTY) return false;
        if (!(stack.getItem() instanceof IFluidCellItem cell)) return false;
        FluidStack content = cell.getFluid(stack);
        // 统一按数量判定：静态与动态单元都必须在数量上满足该单元容量，
        // 避免部分填充（含被排空后残留 NBT）的单元被当成满单元参与配方。
        return !content.isEmpty()
            && content.getFluid() == fluid
            && content.getAmount() >= cell.getCapacity();
    }

    /**
     * 检查物品堆是否为包含指定流体中任一种的单元。
     */
    public static boolean isCellContainingAnyFluid(ItemStack stack, Fluid... fluids) {
        for (Fluid fluid : fluids) {
            if (isCellContainingFluid(stack, fluid)) return true;
        }
        return false;
    }

    /**
     * 检查物品堆是否为空单元（静态空单元或空动态单元）。
     */
    public static boolean isEmptyCell(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 以实际内容为准：静态单元排空后会带显式空标记，动态单元则移除内容。
        return stack.getItem() instanceof IFluidCellItem cell && cell.getFluid(stack).isEmpty();
    }

    /**
     * 检查物品堆是否为任意流体单元（静态或动态，空或满）。
     */
    public static boolean isFluidCell(ItemStack stack) {
        return stack.getItem() instanceof mio_icif_cell || stack.getItem() instanceof mio_icif_dynamic_cell;
    }

    /**
     * 获取单元中的流体。如果不是单元则返回 EMPTY。
     */
    public static FluidStack getCellFluid(ItemStack stack) {
        if (stack.getItem() instanceof mio_icif_cell cell) {
            return cell.getFluid(stack);
        }
        if (stack.getItem() instanceof mio_icif_dynamic_cell dynamicCell) {
            return dynamicCell.getFluid(stack);
        }
        return FluidStack.EMPTY;
    }

    /**
     * 获取单元排空后的空容器。
     * 静态单元返回空静态单元，动态单元返回空动态单元。
     */
    public static ItemStack getEmptyCellForStack(ItemStack stack) {
        if (stack.getItem() instanceof mio_icif_dynamic_cell) {
            return new ItemStack(CELL_EMPTY.get());
        }
        if (stack.getItem() instanceof mio_icif_cell) {
            return new ItemStack(CELL_EMPTY.get());
        }
        return ItemStack.EMPTY;
    }

    /**
     * 获取空单元填充指定流体后的满容器。
     * 优先返回静态单元，无对应静态单元时返回动态单元。
     */
    public static ItemStack getFilledCellForFluidStack(Fluid fluid) {
        Item staticCell = getFilledCellForFluid(fluid);
        if (staticCell != null) {
            return new ItemStack(staticCell);
        }
        return createDynamicFilledCell(fluid, 1000);
    }

    /**
     * 始终创建动态单元并注入指定流体。
     * 用于混合模式等需要统一产出动态单元的场景。
     *
     * <p><b>容量语义</b>：单元本身有物理上限（{@link IFluidCellItem#getCapacity()}），
     * 因此本方法只写入 {@code min(amount, capacity)}。若产出量可能超过单元容量，
     * 调用方必须自行处理余量（例如装罐机的 MIX 模式会把余量转入输出液体槽），
     * 否则余量会被丢弃。可用 {@link #getCellCapacity(ItemStack)} 预先计算可容纳量。
     */
    public static ItemStack createDynamicFilledCell(Fluid fluid, int amount) {
        if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack dynamicStack = new ItemStack(CELL_EMPTY.get());
        if (CELL_EMPTY.get() instanceof mio_icif_dynamic_cell dynamicCell) {
            int fillAmount = Math.min(amount, dynamicCell.getCapacity());
            dynamicCell.writeFluidToNBT(dynamicStack, new FluidStack(fluid, fillAmount));
        }
        return dynamicStack;
    }
}