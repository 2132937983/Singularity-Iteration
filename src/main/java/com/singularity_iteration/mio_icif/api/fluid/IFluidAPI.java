package com.singularity_iteration.mio_icif.api.fluid;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

/**
 * 流体 API
 *
 * <p>提供与 mio_icif 流体的访问，包括：
 * <ul>
 *   <li>查询模组流体</li>
 *   <li>获取流体属性</li>
 *   <li>判断气体流体</li>
 * </ul>
 *
 * <h3>创建自定义气体流体</h3>
 * <p>Addon 开发者可以使用 {@link GasFluidSource} 和 {@link GasFluidFlowing} 创建
 * 具有"向上飘升 + 限高消失 + 水平扩散"行为的气体流体：
 * <pre>{@code
 * // 定义流体属性
 * public static final BaseFlowingFluid.Properties MY_GAS_PROPERTIES =
 *     new BaseFlowingFluid.Properties(
 *         () -> MY_GAS, () -> MY_GAS_FLOWING,
 *         () -> MY_GAS_BLOCK
 *     ).canMultiply(true).flowSpeed(4).levelDecreasePerBlock(1);
 *
 * // 注册气体流体
 * public static final DeferredHolder<Fluid, FlowingFluid> MY_GAS = FLUIDS.register("my_gas",
 *     () -> new GasFluidSource(MY_GAS_PROPERTIES, 320));
 * public static final DeferredHolder<Fluid, FlowingFluid> MY_GAS_FLOWING = FLUIDS.register("my_gas_flowing",
 *     () -> new GasFluidFlowing(MY_GAS_PROPERTIES, 320));
 * }</pre>
 *
 * @see GasFluidSource
 * @see GasFluidFlowing
 */
public interface IFluidAPI {

    /**
     * 根据 ID 获取流体
     *
     * @param id 流体 ID
     * @return 流体，如果不存在则返回 null
     */
    Fluid getFluid(ResourceLocation id);

    /**
     * 获取生物气体
     *
     * @return 生物气体流体
     */
    Fluid getBiogas();

    /**
     * 获取热水
     *
     * @return 热水流体
     */
    Fluid getHotWater();

    /**
     * 获取生物质
     *
     * @return 生物质流体
     */
    Fluid getBiomass();

    /**
     * 获取建筑泡沫
     *
     * @return 建筑泡沫流体
     */
    Fluid getConstructionFoam();

    /**
     * 获取冷却液
     *
     * @return 冷却液流体
     */
    Fluid getCoolant();

    /**
     * 获取蒸馏水
     *
     * @return 蒸馏水流体
     */
    Fluid getDistilledWater();

    /**
     * 获取热冷却液
     *
     * @return 热冷却液流体
     */
    Fluid getHotCoolant();

    /**
     * 获取绳状熔岩
     *
     * @return 绳状熔岩流体
     */
    Fluid getPahoehoeLava();

    /**
     * 获取蒸汽
     *
     * @return 蒸汽流体
     */
    Fluid getSteam();

    /**
     * 获取过热蒸汽
     *
     * @return 过热蒸汽流体
     */
    Fluid getSuperheatedSteam();

    /**
     * 获取 UU 物质
     *
     * @return UU 物质流体
     */
    Fluid getUUMatter();

    /**
     * 获取空气
     *
     * @return 空气流体
     */
    Fluid getAir();

    /**
     * 获取所有注册流体 ID
     *
     * @return 流体 ID 集合
     */
    Collection<ResourceLocation> getAllFluidIds();

    /**
     * 判断指定流体是否为气体流体（具有向上飘升行为）。
     *
     * <p>气体流体包括本模组的生物气体、蒸汽、过热蒸汽和压缩空气，
     * 以及使用 {@link GasFluidSource}/{@link GasFluidFlowing} 注册的 Addon 气体流体。
     *
     * @param fluid 流体
     * @return true 如果是气体流体
     */
    boolean isGasFluid(Fluid fluid);
}