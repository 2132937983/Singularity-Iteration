package com.singularity_iteration.mio_icif.api;

import com.singularity_iteration.mio_icif.api.block.BlockAPIImpl;
import com.singularity_iteration.mio_icif.api.block.IBlockAPI;
import com.singularity_iteration.mio_icif.api.capability.IMioIcifCapabilities;
import com.singularity_iteration.mio_icif.api.capability.MioIcifCapabilitiesImpl;
import com.singularity_iteration.mio_icif.api.crop.CropAPIImpl;
import com.singularity_iteration.mio_icif.api.crop.ICropAPI;
import com.singularity_iteration.mio_icif.api.energy.EnergyNetAPIImpl;
import com.singularity_iteration.mio_icif.api.energy.FECompatAPIImpl;
import com.singularity_iteration.mio_icif.api.energy.IEnergyNetAPI;
import com.singularity_iteration.mio_icif.api.energy.IFECompatAPI;
import com.singularity_iteration.mio_icif.api.fluid.FluidAPIImpl;
import com.singularity_iteration.mio_icif.api.fluid.IFluidAPI;
import com.singularity_iteration.mio_icif.api.fluid.FluidHandlerAPIImpl;
import com.singularity_iteration.mio_icif.api.fluid.IFluidHandlerAPI;
import com.singularity_iteration.mio_icif.api.generator.GeneratorAPIImpl;
import com.singularity_iteration.mio_icif.api.generator.IGeneratorAPI;
import com.singularity_iteration.mio_icif.api.heat.HeatAPIImpl;
import com.singularity_iteration.mio_icif.api.heat.IHeatAPI;
import com.singularity_iteration.mio_icif.api.item.IItemAPI;
import com.singularity_iteration.mio_icif.api.item.ItemAPIImpl;
import com.singularity_iteration.mio_icif.api.kinetic.IKineticAPI;
import com.singularity_iteration.mio_icif.api.kinetic.KineticAPIImpl;
import com.singularity_iteration.mio_icif.api.machine.IMachineAPI;
import com.singularity_iteration.mio_icif.api.machine.MachineAPIImpl;
import com.singularity_iteration.mio_icif.api.machine.builder.IMachineBuilderAPI;
import com.singularity_iteration.mio_icif.api.machine.builder.MachineBuilderAPIImpl;
import com.singularity_iteration.mio_icif.api.reactor.IReactorAPI;
import com.singularity_iteration.mio_icif.api.reactor.ReactorAPIImpl;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeAPI;
import com.singularity_iteration.mio_icif.api.recipe.IRecipeRegistrationAPI;
import com.singularity_iteration.mio_icif.api.recipe.RecipeAPIImpl;
import com.singularity_iteration.mio_icif.api.recipe.RecipeRegistrationAPIImpl;
import com.singularity_iteration.mio_icif.api.registry.IMioIcifRegistries;
import com.singularity_iteration.mio_icif.api.registry.MioIcifRegistriesImpl;
import com.singularity_iteration.mio_icif.api.upgrade.IUpgradeAPI;
import com.singularity_iteration.mio_icif.api.upgrade.UpgradeAPIImpl;

/**
 * API 实例持有者
 * 
 * <p>使用 Holder 模式实现延迟加载和单例模式
 */
final class MioIcifAPIHolder {

    /** API 实例 */
    static final MioIcifAPI INSTANCE = new MioIcifAPIImpl();

    private MioIcifAPIHolder() {
        // 禁止实例化
    }

    /**
     * API 实现类
     */
    @SuppressWarnings("deprecation")
    private static class MioIcifAPIImpl implements MioIcifAPI {

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