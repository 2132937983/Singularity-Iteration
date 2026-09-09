package com.singularity_iteration.mio_icif.api.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 流体单元物品接口。
 *
 * <p>用于描述可以存储流体的物品（如流体单元、桶等）。
 * 实现此接口的物品可以存储和传输特定类型的流体。
 *
 * <h3>IC2 风格单元语义（Addon 开发者必读）</h3>
 * <p>本接口对应的实现（{@code mio_icif_cells} 下的各类单元）遵循 IC2 原版流体单元的约定，
 * 与桶（Bucket）行为对齐，请勿按普通 {@code ItemStack} 容器理解：
 * <ul>
 *   <li><b>固定容量</b>：每个单元有固定容量（通常为 1000 mB，与原版桶一致；部分单元为 2000 mB）。
 *   {@link #getCapacity()} 返回的是<b>单格</b>容量，而非堆叠总容量。</li>
 *   <li><b>空单元返还</b>：单元参与合成配方时通过 {@code .craftRemainder(CELL_EMPTY)} 返回空单元，
 *       与原版桶合成后返还空桶一致。Addon 自定义单元若参与合成，也必须设置 craftRemainder，
 *       否则单元会被消耗而非返还。</li>
 *   <li><b>按流体种类区分</b>：不同流体的单元是不同的物品（水单元 / 岩浆单元 / 生物气单元等），
 *       不可混用；同种流体单元之间可正常堆叠。</li>
 *   <li><b>单流体存储</b>：单元同一时刻只容纳一种流体。{@code fill} 时若待填充流体与已存储流体
 *       不同且单元非空，应拒绝（返回 0），不允许混合。</li>
 *   <li><b>单位</b>：所有容量与流量方法均以毫桶（mB）为单位。</li>
 *   <li><b>空/满判定</b>：{@link #isEmpty(ItemStack)} / {@link #isFull(ItemStack)} 是基于容量与
 *       当前存量的便捷判定，不要假设单元一定满容量。</li>
 * </ul>
 */
public interface IFluidCellItem {

    /**
     * 获取流体单元的最大容量（mB）
     * @return 容量（毫桶）
     */
    int getCapacity();

    /**
     * 获取物品中当前存储的流体
     * @param stack 物品堆
     * @return 当前流体堆（可能为空）
     */
    FluidStack getFluid(ItemStack stack);

    /**
     * 检查是否可以存储指定流体
     * @param fluid 流体
     * @return true 如果可以存储
     */
    boolean canHoldFluid(Fluid fluid);

    /**
     * 向物品中填充流体
     *
     * @param stack     物品堆
     * @param fluid     要填充的流体堆
     * @param simulate  是否仅模拟（不实际填充）
     * @return 实际填充的量（mB）
     */
    int fill(ItemStack stack, FluidStack fluid, boolean simulate);

    /**
     * 从物品中排出流体
     *
     * @param stack     物品堆
     * @param amount    要排出的量（mB）
     * @param simulate  是否仅模拟（不实际排出）
     * @return 实际排出的流体堆
     */
    FluidStack drain(ItemStack stack, int amount, boolean simulate);

    /**
     * 获取可以排出的最大量
     * @param stack 物品堆
     * @return 最大可排出量（mB）
     */
    default int getMaxDrain(ItemStack stack) {
        return getFluid(stack).getAmount();
    }

    /**
     * 检查流体单元是否为空
     * @param stack 物品堆
     * @return true 如果没有流体
     */
    default boolean isEmpty(ItemStack stack) {
        return getFluid(stack).isEmpty();
    }

    /**
     * 检查流体单元是否已满
     * @param stack 物品堆
     * @return true 如果流体已满
     */
    default boolean isFull(ItemStack stack) {
        return getFluid(stack).getAmount() >= getCapacity();
    }

    /**
     * 获取此单元在排空后应该变成的空容器物品。
     * <p>例如水单元排空后变成空单元，桶排空后变成空桶。
     * 默认实现返回 {@link ItemStack#EMPTY}，表示不返还容器。
     *
     * @param stack 当前的满单元物品堆
     * @return 排空后的空容器物品堆
     */
    default ItemStack getEmptyContainer(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    /**
     * 获取此空单元在填充指定流体后应该变成的满容器物品。
     * <p>例如空单元填充水后变成水单元，空桶填充水后变成水桶。
     * 默认实现返回 {@link ItemStack#EMPTY}，表示无法填充。
     *
     * @param stack 当前的空单元物品堆
     * @param fluid 要填充的流体类型
     * @return 填充后的满容器物品堆
     */
    default ItemStack getFilledContainer(ItemStack stack, Fluid fluid) {
        return ItemStack.EMPTY;
    }
}