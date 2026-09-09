package com.singularity_iteration.mio_icif.api.energy;

import com.singularity_iteration.mio_icif.energy.grid.EnergyNetGlobal;
import com.singularity_iteration.mio_icif.energy.grid.FECompatTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FE↔EU 兼容 API 实现
 *
 * <p>通过 {@code FECompatTile} 创建和管理 FE 兼容节点。
 */
public class FECompatAPIImpl implements IFECompatAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(FECompatAPIImpl.class);
    private static final int FE_PER_EU = 4;

    private final Map<Long, FECompatTile> activeNodes = new ConcurrentHashMap<>();

    @Override
    public long createFECompatNode(Level world, BlockPos pos, Direction side) {
        if (world.isClientSide) return 0;
        try {
            FECompatTile tile = new FECompatTile(world, pos, side);
            if (!tile.isValid()) {
                LOGGER.debug("FE compat node at {} is not valid (no FE capability found)", pos);
                return 0;
            }
            tile.setRegistered(true);
            EnergyNetGlobal.addTile(tile, world, pos);
            long key = pos.asLong();
            activeNodes.put(key, tile);
            LOGGER.debug("Created FE compat node at {} (side={})", pos, side);
            return key;
        } catch (Exception e) {
            LOGGER.error("Failed to create FE compat node at {}: {}", pos, e.getMessage());
            return 0;
        }
    }

    @Override
    public boolean removeFECompatNode(Level world, BlockPos pos) {
        if (world.isClientSide) return false;
        long key = pos.asLong();
        FECompatTile tile = activeNodes.remove(key);
        if (tile != null) {
            try {
                EnergyNetGlobal.removeTile(tile);
                tile.setRegistered(false);
                LOGGER.debug("Removed FE compat node at {}", pos);
                return true;
            } catch (Exception e) {
                LOGGER.error("Failed to remove FE compat node at {}: {}", pos, e.getMessage());
            }
        }
        return false;
    }

    @Override
    public boolean isFECompatActive(Level world, BlockPos pos) {
        long key = pos.asLong();
        FECompatTile tile = activeNodes.get(key);
        return tile != null && tile.isValid() && tile.isRegistered();
    }

    @Override
    public int getFEPerEU() {
        return FE_PER_EU;
    }
}
