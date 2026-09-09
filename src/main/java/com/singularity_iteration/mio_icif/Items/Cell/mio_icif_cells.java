package com.singularity_iteration.mio_icif.Items.Cell;

import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;


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


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    /**
     * 根据流体类型获取对应的已填充单元物品
     */
    @Nullable
    public static Item getFilledCellForFluid(Fluid fluid) {
        if (fluid == Fluids.WATER) return CELL_WATER.get();
        if (fluid == Fluids.LAVA) return CELL_LAVA.get();
        if (fluid == mio_icif_fluids.BIOGAS.get()) return CELL_BIOGAS.get();
        if (fluid == mio_icif_fluids.HOTWATER.get()) return CELL_HOTWATER.get();
        if (fluid == mio_icif_fluids.BIOMASS.get()) return CELL_BIOMASS.get();
        if (fluid == mio_icif_fluids.CONSTRUCTIONFOAM.get()) return CELL_CONSTRUCTIONFOAM.get();
        if (fluid == mio_icif_fluids.COOLANT.get()) return CELL_COOLANT.get();
        if (fluid == mio_icif_fluids.DISTILLEDWATER.get()) return CELL_DISTILLEDWATER.get();
        if (fluid == mio_icif_fluids.HOTCOOLANT.get()) return CELL_HOTCOOLANT.get();
        if (fluid == mio_icif_fluids.PAHOEHOELAVA.get()) return CELL_PAHOEHOELAVA.get();
        if (fluid == mio_icif_fluids.STEAM.get()) return CELL_STEAM.get();
        if (fluid == mio_icif_fluids.SUPERHEATEDSTEAM.get()) return CELL_SUPERHEATEDSTEAM.get();
        if (fluid == mio_icif_fluids.UUMATTER.get()) return CELL_UUMATTER.get();
        if (fluid == mio_icif_fluids.AIR.get()) return CELL_AIR.get();
        return null;
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
        if (fluid == Fluids.WATER) return CELL_TYPE_WATER;
        if (fluid == Fluids.LAVA) return CELL_TYPE_LAVA;
        if (fluid == mio_icif_fluids.BIOGAS.get()) return CELL_TYPE_BIOGAS;
        if (fluid == mio_icif_fluids.HOTWATER.get()) return CELL_TYPE_HOTWATER;
        if (fluid == mio_icif_fluids.BIOMASS.get()) return CELL_TYPE_BIOMASS;
        if (fluid == mio_icif_fluids.CONSTRUCTIONFOAM.get()) return CELL_TYPE_CONSTRUCTIONFOAM;
        if (fluid == mio_icif_fluids.COOLANT.get()) return CELL_TYPE_COOLANT;
        if (fluid == mio_icif_fluids.DISTILLEDWATER.get()) return CELL_TYPE_DISTILLEDWATER;
        if (fluid == mio_icif_fluids.HOTCOOLANT.get()) return CELL_TYPE_HOTCOOLANT;
        if (fluid == mio_icif_fluids.PAHOEHOELAVA.get()) return CELL_TYPE_PAHOEHOELAVA;
        if (fluid == mio_icif_fluids.STEAM.get()) return CELL_TYPE_STEAM;
        if (fluid == mio_icif_fluids.SUPERHEATEDSTEAM.get()) return CELL_TYPE_SUPERHEATEDSTEAM;
        if (fluid == mio_icif_fluids.UUMATTER.get()) return CELL_TYPE_UUMATTER;
        if (fluid == mio_icif_fluids.AIR.get()) return CELL_TYPE_AIR;
        return CELL_TYPE_GENERIC;
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
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof mio_icif_cell cell) {
            return cell.getContent() == fluid;
        }
        if (stack.getItem() instanceof mio_icif_dynamic_cell dynamicCell) {
            FluidStack cellFluid = dynamicCell.getFluid(stack);
            return !cellFluid.isEmpty() && cellFluid.getFluid() == fluid && cellFluid.getAmount() >= dynamicCell.getCapacity();
        }
        return false;
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
        if (stack.getItem() instanceof mio_icif_cell cell) {
            return cell.getContent() == Fluids.EMPTY;
        }
        if (stack.getItem() instanceof mio_icif_dynamic_cell dynamicCell) {
            return dynamicCell.getFluid(stack).isEmpty();
        }
        return false;
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