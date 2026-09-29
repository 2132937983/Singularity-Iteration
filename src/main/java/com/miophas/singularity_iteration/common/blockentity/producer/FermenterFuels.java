package com.miophas.singularity_iteration.common.blockentity.producer;

import com.miophas.singularity_iteration.common.block.environment.fluid.mio_icif_fluids;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 发酵配方表（对齐 IC2 1.12.2 的 {@code Recipes.fermenter} / {@code FermenterRecipeManager}）。
 *
 * <p>IC2 以「输入流体 → {@code FermentationProperty(inputAmount, heat, outputFluid, outputAmount)}」
 * 注册，机器的流体过滤与 {@code work()} 都查询该表。默认注册 biomass → biogas，
 * 数值取自 {@code general.ini [balance/fermenter]}：20 mB 生物质 + 4000 HU → 400 mB 沼气。
 * 附属可调用 {@link #addRecipe} 追加/覆盖配方。
 */
public final class FermenterFuels {

    private FermenterFuels() {
    }

    /** 一条发酵配方：消耗 inputAmount mB 输入 + heat HU，产出 outputAmount mB output。 */
    public record Property(int inputAmount, int heat, Fluid output, int outputAmount) {
    }

    // 延迟构建：建表时才读取 DeferredHolder.get()，避免在流体注册前触发。
    private static volatile Map<Fluid, Property> map;

    private static Map<Fluid, Property> map() {
        Map<Fluid, Property> cache = map;
        if (cache != null) {
            return cache;
        }
        synchronized (FermenterFuels.class) {
            if (map != null) {
                return map;
            }
            Map<Fluid, Property> built = new LinkedHashMap<>();
            // 默认配方（IC2 balance/fermenter 默认值）
            built.put(mio_icif_fluids.BIOMASS.get(),
                new Property(20, 4000, mio_icif_fluids.BIOGAS.get(), 400));
            map = Map.copyOf(built);
            return map;
        }
    }

    /** 注册/覆盖一条发酵配方（对应 IC2 {@code Recipes.fermenter.addRecipe}）。 */
    public static synchronized void addRecipe(Fluid input, int inputAmount, int heat, Fluid output, int outputAmount) {
        if (input == null || output == null || inputAmount <= 0 || heat <= 0 || outputAmount <= 0) {
            throw new IllegalArgumentException("Invalid fermenter recipe");
        }
        Map<Fluid, Property> updated = new LinkedHashMap<>(map());
        updated.put(input, new Property(inputAmount, heat, output, outputAmount));
        map = Map.copyOf(updated);
    }

    /** 输入流体对应的配方；非配方流体返回 null。 */
    @Nullable
    public static Property get(@Nullable Fluid fluid) {
        return fluid == null ? null : map().get(fluid);
    }

    /** 是否为任一配方的输入流体。 */
    public static boolean accepts(@Nullable Fluid fluid) {
        return fluid != null && map().containsKey(fluid);
    }

    /** 是否为任一配方的产出流体（用于输出罐过滤）。 */
    public static boolean isOutputFluid(@Nullable Fluid fluid) {
        if (fluid == null) {
            return false;
        }
        for (Property property : map().values()) {
            if (property.output() == fluid) {
                return true;
            }
        }
        return false;
    }

    /** 全部已注册的输入流体。 */
    public static Set<Fluid> acceptedFluids() {
        return map().keySet();
    }
}
