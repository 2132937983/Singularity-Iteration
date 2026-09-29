package com.miophas.singularity_iteration.core.api.fluid;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 机器的流体端口路由契约。
 *
 * <p>比 {@link ISeparateFluidPorts} 更通用：允许按方向返回不同端口，也可以
 * 声明端口数量。默认实现忽略方向，保留原有“输入罐/输出罐”语义，因此既有的
 * {@link ISeparateFluidPorts} 实现无需改动即可被核心自动化识别。
 */
public interface IFluidPorts {

    IFluidHandler getInputFluidTank();

    IFluidHandler getOutputFluidTank();

    /**
     * 按方向取得端口。
     *
     * @param input true 取输入端口，false 取输出端口
     * @param side 访问方向，可为 null
     */
    default IFluidHandler fluidPort(boolean input, @Nullable Direction side) {
        return input ? getInputFluidTank() : getOutputFluidTank();
    }

    /** 端口是否随方向变化；为 true 时核心不会把端口缓存成单一实例。 */
    default boolean routesBySide() {
        return false;
    }

    /** 端口数量；默认两个（输入、输出）。 */
    default int portCount() {
        return 2;
    }
}
