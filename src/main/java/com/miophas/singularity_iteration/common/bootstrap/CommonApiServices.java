package com.miophas.singularity_iteration.common.bootstrap;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;

import com.miophas.singularity_iteration.common.service.block.BlockAPIImpl;
import com.miophas.singularity_iteration.core.api.block.IBlockAPI;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.runtime.capability.MioIcifCapabilitiesImpl;
import com.miophas.singularity_iteration.core.runtime.crop.CropAPIImpl;
import com.miophas.singularity_iteration.core.api.crop.ICropAPI;
import com.miophas.singularity_iteration.core.runtime.energy.EnergyNetAPIImpl;
import com.miophas.singularity_iteration.core.runtime.energy.FECompatAPIImpl;
import com.miophas.singularity_iteration.core.api.energy.IEnergyNetAPI;
import com.miophas.singularity_iteration.core.api.energy.IFECompatAPI;
import com.miophas.singularity_iteration.common.service.fluid.FluidAPIImpl;
import com.miophas.singularity_iteration.core.api.fluid.IFluidAPI;
import com.miophas.singularity_iteration.core.runtime.fluid.FluidHandlerAPIImpl;
import com.miophas.singularity_iteration.core.api.fluid.IFluidHandlerAPI;
import com.miophas.singularity_iteration.core.runtime.generator.GeneratorAPIImpl;
import com.miophas.singularity_iteration.core.api.generator.IGeneratorAPI;
import com.miophas.singularity_iteration.core.runtime.heat.HeatAPIImpl;
import com.miophas.singularity_iteration.core.api.heat.IHeatAPI;
import com.miophas.singularity_iteration.core.api.item.IItemAPI;
import com.miophas.singularity_iteration.common.service.item.ItemAPIImpl;
import com.miophas.singularity_iteration.core.api.kinetic.IKineticAPI;
import com.miophas.singularity_iteration.core.runtime.kinetic.KineticAPIImpl;
import com.miophas.singularity_iteration.core.api.machine.IMachineAPI;
import com.miophas.singularity_iteration.core.runtime.machine.MachineAPIImpl;
import com.miophas.singularity_iteration.core.api.machine.builder.IMachineBuilderAPI;
import com.miophas.singularity_iteration.core.runtime.machine.builder.MachineBuilderAPIImpl;
import com.miophas.singularity_iteration.core.api.reactor.IReactorAPI;
import com.miophas.singularity_iteration.core.runtime.reactor.ReactorAPIImpl;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeAPI;
import com.miophas.singularity_iteration.core.api.recipe.IRecipeRegistrationAPI;
import com.miophas.singularity_iteration.common.service.recipe.RecipeAPIImpl;
import com.miophas.singularity_iteration.common.service.recipe.RecipeRegistrationAPIImpl;
import com.miophas.singularity_iteration.core.api.registry.IMioIcifRegistries;
import com.miophas.singularity_iteration.common.service.registry.MioIcifRegistriesImpl;
import com.miophas.singularity_iteration.core.api.upgrade.IUpgradeAPI;
import com.miophas.singularity_iteration.core.runtime.upgrade.UpgradeAPIImpl;

/**
 * API 实例持有者
 * 
 * <p>使用 Holder 模式实现延迟加载和单例模式
 */
public final class CommonApiServices {

    public static void install() { com.miophas.singularity_iteration.core.api.ApiServices.install(INSTANCE); }

    /** API 实例 */
    static final MioIcifAPI INSTANCE = new MioIcifAPIImpl();

    private CommonApiServices() {
        // 禁止实例化
    }

    /**
     * API 实现类
     */
    @SuppressWarnings("deprecation")
    private static class MioIcifAPIImpl implements MioIcifAPI {
        private final com.miophas.singularity_iteration.core.api.machine.IMultiblockAccess multiblocks = new com.miophas.singularity_iteration.core.api.machine.IMultiblockAccess() {
            @Override public com.miophas.singularity_iteration.core.api.machine.IMultiblockStructure findStructure(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
                return com.miophas.singularity_iteration.common.service.machine.MultiblockBridge.findStructure(level, pos);
            }
            @Override public com.miophas.singularity_iteration.core.api.machine.IMultiblockBuilder createBuilder() {
                return com.miophas.singularity_iteration.common.service.machine.MultiblockBridge.createBuilder();
            }
        };
        @Override public com.miophas.singularity_iteration.core.api.machine.IMultiblockAccess getMultiblockAPI() { return multiblocks; }


        // ========== API 实例（懒加载） ==========
        private volatile IEnergyNetAPI energyNetAPI;
        private volatile IFECompatAPI feCompatAPI;
        private volatile IGeneratorAPI generatorAPI;
        private volatile IItemAPI itemAPI;
        private volatile IRecipeAPI recipeAPI;
        private volatile IRecipeRegistrationAPI recipeRegistrationAPI;
        private volatile IFluidAPI fluidAPI;
        private volatile IFluidHandlerAPI fluidHandlerAPI;
        private volatile ICropAPI cropAPI;
        private volatile IBlockAPI blockAPI;
        private volatile IHeatAPI heatAPI;
        private volatile IKineticAPI kineticAPI;
        private volatile IReactorAPI reactorAPI;
        private volatile IUpgradeAPI upgradeAPI;
        private volatile com.miophas.singularity_iteration.core.api.world.IWindAPI windAPI;
        private volatile IMachineAPI machineAPI;
        private volatile IMachineBuilderAPI machineBuilderAPI;
        private volatile IMioIcifRegistries registries;
        private volatile IMioIcifCapabilities capabilities;

        // ========== 版本信息 ==========
        private static volatile String VERSION = null;
        private static final String MOD_ID = "mio_icif";
        
        /**
         * 懒加载获取版本号，避免类加载时 ModList 未初始化
         */
        private String fetchVersion() {
            if (VERSION == null) {
                synchronized (MioIcifAPIImpl.class) {
                    if (VERSION == null) {
                        VERSION = net.neoforged.fml.ModList.get()
                            .getModContainerById(MOD_ID)
                            .map(container -> container.getModInfo().getVersion().toString())
                            .orElseGet(() -> {
                                // 当 ModList 不可用时，尝试从构建时注入的系统属性获取
                                String sysVersion = System.getProperty("mio_icif.version");
                                return sysVersion != null ? sysVersion : "unknown";
                            });
                    }
                }
            }
            return VERSION;
        }

        // ========== API 获取方法 ==========

        @Override
        public IEnergyNetAPI getEnergyNetAPI() {
            if (energyNetAPI == null) synchronized (this) { if (energyNetAPI == null) energyNetAPI = new EnergyNetAPIImpl(); }
            return energyNetAPI;
        }

        @Override
        public IFECompatAPI getFECompatAPI() {
            if (feCompatAPI == null) synchronized (this) { if (feCompatAPI == null) feCompatAPI = new FECompatAPIImpl(); }
            return feCompatAPI;
        }

        @Override
        public IGeneratorAPI getGeneratorAPI() {
            if (generatorAPI == null) synchronized (this) { if (generatorAPI == null) generatorAPI = new GeneratorAPIImpl(); }
            return generatorAPI;
        }

        @Override
        public IItemAPI getItemAPI() {
            if (itemAPI == null) synchronized (this) { if (itemAPI == null) itemAPI = new ItemAPIImpl(); }
            return itemAPI;
        }

        @Override
        public IRecipeAPI getRecipeAPI() {
            if (recipeAPI == null) synchronized (this) { if (recipeAPI == null) recipeAPI = new RecipeAPIImpl(); }
            return recipeAPI;
        }

        @Override
        public IRecipeRegistrationAPI getRecipeRegistrationAPI() {
            if (recipeRegistrationAPI == null) synchronized (this) { if (recipeRegistrationAPI == null) recipeRegistrationAPI = new RecipeRegistrationAPIImpl(); }
            return recipeRegistrationAPI;
        }

        @Override
        public IFluidAPI getFluidAPI() {
            if (fluidAPI == null) synchronized (this) { if (fluidAPI == null) fluidAPI = new FluidAPIImpl(); }
            return fluidAPI;
        }

        @Override
        public IFluidHandlerAPI getFluidHandlerAPI() {
            if (fluidHandlerAPI == null) synchronized (this) { if (fluidHandlerAPI == null) fluidHandlerAPI = new FluidHandlerAPIImpl(); }
            return fluidHandlerAPI;
        }

        @Override
        public ICropAPI getCropAPI() {
            if (cropAPI == null) synchronized (this) { if (cropAPI == null) cropAPI = new CropAPIImpl(); }
            return cropAPI;
        }

        @Override
        public IBlockAPI getBlockAPI() {
            if (blockAPI == null) synchronized (this) { if (blockAPI == null) blockAPI = new BlockAPIImpl(); }
            return blockAPI;
        }

        @Override
        public IHeatAPI getHeatAPI() {
            if (heatAPI == null) synchronized (this) { if (heatAPI == null) heatAPI = new HeatAPIImpl(); }
            return heatAPI;
        }

        @Override
        public IKineticAPI getKineticAPI() {
            if (kineticAPI == null) synchronized (this) { if (kineticAPI == null) kineticAPI = new KineticAPIImpl(); }
            return kineticAPI;
        }

        @Override
        public IReactorAPI getReactorAPI() {
            if (reactorAPI == null) synchronized (this) { if (reactorAPI == null) reactorAPI = new ReactorAPIImpl(); }
            return reactorAPI;
        }

        @Override
        public IUpgradeAPI getUpgradeAPI() {
            if (upgradeAPI == null) synchronized (this) { if (upgradeAPI == null) upgradeAPI = new UpgradeAPIImpl(); }
            return upgradeAPI;
        }

        @Override
        public com.miophas.singularity_iteration.core.api.world.IWindAPI getWindAPI() {
            if (windAPI == null) synchronized (this) { if (windAPI == null) windAPI = new com.miophas.singularity_iteration.core.runtime.world.WindAPIImpl(); }
            return windAPI;
        }

        @Override
        public IMachineAPI getMachineAPI() {
            if (machineAPI == null) synchronized (this) { if (machineAPI == null) machineAPI = new MachineAPIImpl(); }
            return machineAPI;
        }

        @Override
        public IMachineBuilderAPI getMachineBuilderAPI() {
            if (machineBuilderAPI == null) synchronized (this) { if (machineBuilderAPI == null) machineBuilderAPI = new MachineBuilderAPIImpl(); }
            return machineBuilderAPI;
        }

        @Override
        public IMioIcifRegistries getRegistries() {
            if (registries == null) synchronized (this) { if (registries == null) registries = new MioIcifRegistriesImpl(); }
            return registries;
        }

        @Override
        public IMioIcifCapabilities getCapabilities() {
            if (capabilities == null) synchronized (this) { if (capabilities == null) capabilities = new MioIcifCapabilitiesImpl(); }
            return capabilities;
        }

        @Override
        public String getVersion() {
            return fetchVersion();
        }

        @Override
        public String getModId() {
            return MOD_ID;
        }
    }
}
