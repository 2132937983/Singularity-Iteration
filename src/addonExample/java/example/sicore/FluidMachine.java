package example.sicore;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.inventory.SlotLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 附属只用 core 公开入口声明的带流体槽机器：罐体由构建器配置创建。 */
public final class FluidMachine extends GenericMachineBlockEntity {

    public static final int TANK_CAPACITY = 8000;

    public FluidMachine(BlockPos pos, BlockState state, BlockEntityType<?> type) {
        super(pos, state, type, 10000, 32, 0, 200, SlotLayout.builder().extra(1).output(1).build(), 16, CableTier.LV);
        var builder = MioIcifAPI.instance().getMachineBuilderAPI().createElectricMachineBuilder()
            .setName("fluid_machine")
            .setEnergyCapacity(10000)
            .setEnergyPerTick(16)
            .setProcessTime(200)
            .useStandardLayout(1, 1, false, 0)
            .addFluidTank(TANK_CAPACITY)
            .addFluidTank(TANK_CAPACITY)
            .withEntityType(type);
        setConfiguration(builder.getConfiguration());
        builder.buildAndRegister(CoreExampleMod.ID);
    }

    /** 延迟解析实体类型，避免注册期的初始化顺序问题。 */
    public static FluidMachine create(BlockPos pos, BlockState state) {
        return new FluidMachine(pos, state, CoreExampleMod.FLUID_MACHINE_ENTITY.get());
    }
}
