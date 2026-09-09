package com.singularity_iteration.mio_icif.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.blockentity.networking.EnergyAcceptorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

@SuppressWarnings("null")
public class AE2DirectBridge implements AE2Bridge {

    @Override
    public boolean hasGridNode(Level world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            try {
                IGridNode node = GridHelper.getExposedNode(world, pos, dir);
                if (node != null) return true;
            } catch (Exception ignored) {}
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof EnergyAcceptorBlockEntity acceptor) {
            try {
                var mainNode = acceptor.getMainNode();
                if (mainNode != null) {
                    IGridNode node = mainNode.getNode();
                    if (node != null) return true;
                }
            } catch (Exception ignored) {}
        }

        return false;
    }

    @Override
    @Nullable
    public Object getGridNode(Level world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            try {
                IGridNode node = GridHelper.getExposedNode(world, pos, dir);
                if (node != null) return node;
            } catch (Exception ignored) {}
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof EnergyAcceptorBlockEntity acceptor) {
            try {
                var mainNode = acceptor.getMainNode();
                if (mainNode != null) {
                    IGridNode node = mainNode.getNode();
                    if (node != null) return node;
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    @Override
    @Nullable
    public Object getEnergyService(Object gridNode) {
        if (!(gridNode instanceof IGridNode node)) return null;
        try {
            IGrid grid = node.getGrid();
            if (grid != null) {
                return grid.getEnergyService();
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public double injectAePower(Object energyService, double aeAmount) {
        if (!(energyService instanceof IEnergyService service)) return aeAmount;
        try {
            return service.injectPower(aeAmount, Actionable.MODULATE);
        } catch (Exception e) {
            return aeAmount;
        }
    }

    @Override
    public double getAeEnergyDemand(Object energyService) {
        if (!(energyService instanceof IEnergyService service)) return 0.0D;
        try {
            return service.getEnergyDemand(Double.MAX_VALUE);
        } catch (Exception e) {
            return 0.0D;
        }
    }
}