package com.miophas.singularity_iteration.common.menu.producer;

import com.miophas.singularity_iteration.common.blockentity.producer.mio_icif_laser_tower;
import com.miophas.singularity_iteration.common.menu.base.mio_icif_machine_menu;
import com.miophas.singularity_iteration.common.registry.mio_icif_menus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.server.level.ServerPlayer;
import com.miophas.singularity_iteration.common.blockentity.producer.tower.LaserTowerStats;
import com.miophas.singularity_iteration.common.blockentity.producer.tower.TargetFilter;
import com.miophas.singularity_iteration.common.network.LaserTowerConfigPacket;
import com.miophas.singularity_iteration.common.network.LaserTowerSyncPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Laser tower GUI: battery slot, energy read-out and the adjustable scan box.
 *
 * <p>Range buttons go through vanilla {@code clickMenuButton}, so no custom packet is
 * needed; the server clamps every change to the tower's limits.
 */
@SuppressWarnings("null")
public class LaserTowerMenu extends mio_icif_machine_menu {
    public static final int DATA_H = 0;
    public static final int DATA_V = 1;
    public static final int DATA_MAX_H = 2;
    public static final int DATA_MAX_V = 3;
    public static final int DATA_TARGETS = 4;
    public static final int DATA_LAST_VOLLEY = 5;
    public static final int DATA_ENERGY_LO = 6;
    public static final int DATA_ENERGY_HI = 7;
    public static final int DATA_CAPACITY_LO = 8;
    public static final int DATA_CAPACITY_HI = 9;
    public static final int DATA_COST = 10;
    /** 0 idle, 1 engaged, 2 no power, 3 redstone off. */
    public static final int DATA_STATE = 11;
    public static final int DATA_STYLE = 12;
    /** Effective ticks between volleys (overclockers shorten it). */
    public static final int DATA_INTERVAL = 13;
    /** Effective input tier (transformer upgrades raise it). */
    public static final int DATA_TIER = 14;
    public static final int DATA_COUNT = 15;
    private static final int SYNC_INTERVAL = 10;

    /** Button ids: H-1, H+1, V-1, V+1, H-8, H+8, V-8, V+8. */
    public static final int BUTTON_COUNT = 8;

    public static final int BATTERY_SLOT = 0;
    public static final int UPGRADE_SLOT_START = 1;
    public static final int UPGRADE_SLOT_COUNT = 4;
    public static final int SLOT_COUNT = 1 + UPGRADE_SLOT_COUNT;
    /** Upgrade bay: a narrow panel attached to the right edge of the console. */
    public static final int UPGRADE_X = 181, UPGRADE_Y = 19;
    public static final int GUI_HEIGHT = 200;

    public LaserTowerMenu(int containerId, Inventory playerInventory, @Nullable RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, null, null);
    }

    public LaserTowerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null, null);
    }

    public LaserTowerMenu(int containerId, Inventory playerInventory, @Nullable mio_icif_laser_tower tower,
                          @Nullable ContainerData data) {
        super(mio_icif_menus.LASER_TOWER_MENU_TYPE.get(), containerId, SLOT_COUNT, playerInventory,
            tower != null ? tower.getItemHandler() : null, data, DATA_COUNT, tower);
        this.owner = playerInventory.player;
    }

    @Override
    protected void addMachineSlots() {
        addBatterySlot(BATTERY_SLOT, 8, 86);
        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            addUpgradeSlot(UPGRADE_SLOT_START + i, UPGRADE_X, UPGRADE_Y + i * 18);
        }
    }

    @Override protected int getPlayerInventoryY() { return 118; }
    @Override protected int getPlayerHotbarY() { return 176; }
    @Override protected int getBatterySlotIndex() { return BATTERY_SLOT; }
    @Override protected int getDataSlotCount() { return DATA_COUNT; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(blockEntity instanceof mio_icif_laser_tower tower) || id < 0 || id >= BUTTON_COUNT) return false;
        int step = id < 4 ? 1 : 8;
        int sign = (id & 1) == 0 ? -1 : 1;
        boolean horizontal = (id % 4) < 2;
        tower.adjustRange(horizontal ? sign * step : 0, horizontal ? 0 : sign * step);
        return true;
    }

    // ---------------------------------------------------------------- stats / target list sync
    private final Player owner;
    private long sentVersion = Long.MIN_VALUE;
    private int syncTimer;
    /** Client-side snapshot, replaced by {@link LaserTowerSyncPacket}. */
    private LaserTowerStats clientStats = new LaserTowerStats();
    private TargetFilter clientFilter = new TargetFilter();
    private int clientSyncCount;

    public LaserTowerStats stats() {
        return blockEntity instanceof mio_icif_laser_tower tower && !owner.level().isClientSide ? tower.stats() : clientStats;
    }
    public TargetFilter filter() {
        return blockEntity instanceof mio_icif_laser_tower tower && !owner.level().isClientSide ? tower.filter() : clientFilter;
    }
    /** Increments on every received sync; screens use it to refresh cached layouts. */
    public int syncCount() { return clientSyncCount; }

    public void acceptSync(LaserTowerStats stats, TargetFilter filter) {
        this.clientStats = stats;
        this.clientFilter = filter;
        clientSyncCount++;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(blockEntity instanceof mio_icif_laser_tower tower) || !(owner instanceof ServerPlayer player)) return;
        if (syncTimer-- > 0) return;
        syncTimer = SYNC_INTERVAL;
        long version = tower.syncVersion();
        if (version == sentVersion) return;
        sentVersion = version;
        PacketDistributor.sendToPlayer(player, new LaserTowerSyncPacket(containerId, tower.stats(), tower.filter()));
    }

    /** Server side: apply a validated {@link LaserTowerConfigPacket}. */
    public void applyConfig(int action, int index, String text) {
        if (!(blockEntity instanceof mio_icif_laser_tower tower)) return;
        switch (action) {
            case LaserTowerConfigPacket.ADD -> tower.addTargetEntry(text);
            case LaserTowerConfigPacket.REMOVE -> tower.removeTargetEntry(index);
            case LaserTowerConfigPacket.TOGGLE_MODE -> tower.toggleFilterMode();
            case LaserTowerConfigPacket.RESET_STATS -> tower.resetStats();
            default -> { return; }
        }
        syncTimer = 0;   // push the change on the next broadcast
    }

    private int word(int index) { return data.get(index) & 0xFFFF; }

    public int horizontalRange() { return data.get(DATA_H); }
    public int verticalRange() { return data.get(DATA_V); }
    public int maxHorizontal() { return data.get(DATA_MAX_H); }
    public int maxVertical() { return data.get(DATA_MAX_V); }
    public int maxTargets() { return data.get(DATA_TARGETS); }
    public int lastVolley() { return data.get(DATA_LAST_VOLLEY); }
    public int costPerTarget() { return word(DATA_COST); }
    public long energy() { return ((long) word(DATA_ENERGY_HI) << 16) | word(DATA_ENERGY_LO); }
    public int state() { return data.get(DATA_STATE); }
    public int style() { return data.get(DATA_STYLE); }
    public int volleyInterval() { return data.get(DATA_INTERVAL); }
    public int effectiveTier() { return data.get(DATA_TIER); }
    public long capacity() { return ((long) word(DATA_CAPACITY_HI) << 16) | word(DATA_CAPACITY_LO); }
}
