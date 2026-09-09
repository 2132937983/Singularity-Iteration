package com.singularity_iteration.mio_icif.integration.ae2;

import com.singularity_iteration.mio_icif.Singularity_Iteration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

@SuppressWarnings("null")
public class AE2Compat {

    public static final double EU_TO_AE_RATIO = 2.0D;
    public static final ResourceLocation ENERGY_ACCEPTOR_ID = ResourceLocation.fromNamespaceAndPath("ae2", "energy_acceptor");

    private static boolean ae2Loaded = false;
    private static AE2Bridge bridge = null;

    static {
        try {
            Class.forName("appeng.api.networking.GridHelper");
            ae2Loaded = true;
            Singularity_Iteration.LOGGER.info("[AE2Compat] AE2 detected, loading direct API bridge.");
        } catch (ClassNotFoundException e) {
            ae2Loaded = false;
            Singularity_Iteration.LOGGER.info("[AE2Compat] AE2 not loaded, compatibility layer disabled.");
        }

        if (ae2Loaded) {
            try {
                bridge = (AE2Bridge) Class.forName(
                    "com.singularity_iteration.mio_icif.integration.ae2.AE2DirectBridge"
                ).getDeclaredConstructor().newInstance();
                Singularity_Iteration.LOGGER.info("[AE2Compat] AE2 direct API bridge initialized successfully.");
            } catch (Exception e) {
                bridge = null;
                Singularity_Iteration.LOGGER.warn("[AE2Compat] Failed to initialize AE2 direct API bridge, falling back to IEnergyStorage: {}", e.getMessage());
            }
        }
    }

    public static boolean isAE2Loaded() {
        return ae2Loaded;
    }

    public static boolean isGridApiAvailable() {
        return bridge != null;
    }

    public static boolean isEnergyAcceptor(LevelAccessor world, BlockPos pos) {
        if (!ae2Loaded) return false;
        try {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock());
            return ENERGY_ACCEPTOR_ID.equals(blockId);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAe2NetworkBlock(LevelAccessor world, BlockPos pos) {
        if (!ae2Loaded) return false;
        try {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock());
            return ENERGY_ACCEPTOR_ID.equals(blockId);
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    public static Object getGridNode(Level world, BlockPos pos) {
        if (bridge == null) return null;
        return bridge.getGridNode(world, pos);
    }

    @Nullable
    public static Object getEnergyService(Object gridNode) {
        if (bridge == null) return null;
        return bridge.getEnergyService(gridNode);
    }

    public static double injectAePower(Object energyService, double aeAmount) {
        if (bridge == null) return aeAmount;
        return bridge.injectAePower(energyService, aeAmount);
    }

    public static double getAeEnergyDemand(Object energyService) {
        if (bridge == null) return 0.0D;
        return bridge.getAeEnergyDemand(energyService);
    }

    @Nullable
    public static IEnergyStorage getFeStorage(Level world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) return null;

        try {
            IEnergyStorage storage = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (storage != null && storage.canReceive()) return storage;

            for (Direction dir : Direction.values()) {
                storage = world.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
                if (storage != null && storage.canReceive()) return storage;
            }
        } catch (Exception ignored) {}
        return null;
    }
}