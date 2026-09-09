package com.singularity_iteration.mio_icif.energy.grid;

import java.util.*;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

class GridUpdater {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final EnergyNetLocal enet;
    private final Queue<GridChange> changes = new ArrayDeque<>();

    private boolean isChangeStep;

    GridUpdater(EnergyNetLocal enet) {
        this.enet = enet;
    }

    void startChangeCalc(Queue<GridChange> changes, Map<IEnergyTile, GridChange> additions) {
        assert !changes.isEmpty();
        assert this.changes.isEmpty();
        this.isChangeStep = true;

        GridChange change;
        while ((change = changes.poll()) != null) {
            this.changes.add(change);
            if (change.type == GridChange.Type.ADDITION) {
                GridChange removedChange = additions.remove(change.ioTile);
                assert removedChange == change;
            }
        }

        prepareUpdate();

        try {
            updateGridSync();
        } catch (Exception e) {
            LOGGER.error("Unhandled exception in GridUpdater.updateGrid()", e);
        } finally {
            this.isChangeStep = false;
        }
    }

    void startTransferCalc() {
        this.isChangeStep = false;
        boolean hasSources = EnergyNetGlobal.getCalculator().runSyncStep(this.enet);
        if (this.enet.hasGrids() && hasSources) {
            Collection<Grid> grids = this.enet.getGrids();
            if (grids.size() > 1 && (this.enet.getWorld().getGameTime() & 1L) == 0L) {
                this.enet.shuffleGrids();
            }
            for (Grid grid : grids) {
                EnergyNetGlobal.getCalculator().runAsyncStep(grid);
            }
        }
    }

    void reset() {
        this.isChangeStep = false;
        this.changes.clear();
    }

    public boolean isInChangeStep() {
        return this.isChangeStep;
    }

    private void prepareUpdate() {
        Iterator<GridChange> it = this.changes.iterator();
        while (it.hasNext()) {
            GridChange change = it.next();
            if (!ChangeHandler.prepareSync(this.enet, change))
                it.remove();
        }
    }

    private void updateGridSync() {
        try {
            GridChange change;
            while ((change = this.changes.poll()) != null) {
                switch (change.type) {
                    case ADDITION:
                        ChangeHandler.applyAddition(this.enet, change.ioTile, change.pos, change.subTiles, this.changes);
                        break;
                    case REMOVAL:
                        ChangeHandler.applyRemoval(this.enet, change.ioTile, change.pos);
                        break;
                }
            }
            notifyCalculatorSync();
        } catch (Exception e) {
            LOGGER.error("Unhandled exception in GridUpdater.updateGridSync()", e);
        }
    }

    private void notifyCalculatorSync() {
        for (Grid grid : this.enet.getGrids()) {
            if (grid.clearDirty()) {
                EnergyNetGlobal.getCalculator().handleGridChange(grid);
            }
        }
    }
}