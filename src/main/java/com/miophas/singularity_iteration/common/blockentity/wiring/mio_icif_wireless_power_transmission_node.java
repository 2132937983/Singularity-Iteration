package com.miophas.singularity_iteration.common.blockentity.wiring;

import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import com.miophas.singularity_iteration.core.api.energy.IEnergyConductiveEndpoint;
import com.miophas.singularity_iteration.core.api.energy.IEnergyStorageAccess;
import com.miophas.singularity_iteration.core.api.energy.IWirelessPowerNode;
import com.miophas.singularity_iteration.core.prefab.blockentity.GenericEnergyBlockEntity;
import com.miophas.singularity_iteration.core.api.energy.storage.EUApi;
import com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("null")
public class mio_icif_wireless_power_transmission_node extends GenericEnergyBlockEntity
        implements IWirelessPowerNode, IEnergyConductiveEndpoint, com.miophas.singularity_iteration.core.api.energy.IEnergyPacketSource {

    public static final long TRANSFER_SPEED = 32768L;
    public static final long DEFAULT_CAPACITY = 196608L;
    public static final long DEFAULT_MAX_RECEIVE = 32768L;
    public static final long DEFAULT_MAX_EXTRACT = 32768L;

    /** 0.5s（10 tick）点亮保持：缺电时传输是断续的，避免方块状态频繁闪烁。 */
    private static final int ACTIVE_HOLD_TICKS = 10;

    private BlockPos targetPosition = null;
    private boolean isActive = false;
    private long activeUntilTick = Long.MIN_VALUE;

    public mio_icif_wireless_power_transmission_node(BlockPos pos, BlockState state) {
        super(pos, state,
            MioIcifAPI.instance().getRegistries().getBlockEntityType("wireless_power_transmission_node"),
            DEFAULT_CAPACITY, DEFAULT_MAX_RECEIVE, DEFAULT_MAX_EXTRACT,
            MioIcifAPI.instance().getEnergyNetAPI().getCableTier("luv"));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_wireless_power_transmission_node blockEntity) {
        if (level.isClientSide()) return;

        blockEntity.updateRole();
        blockEntity.refreshPacket();
        blockEntity.wirelessTransfer();

        boolean lit = state.getValue(com.miophas.singularity_iteration.common.block.wiring.mio_icif_block_wireless_power_transmission_node.LIT);
        if (blockEntity.isActive != lit) {
            BlockState newState = state.setValue(com.miophas.singularity_iteration.common.block.wiring.mio_icif_block_wireless_power_transmission_node.LIT, blockEntity.isActive);
            level.setBlockAndUpdate(pos, newState);
            // 额外推送方块实体数据：客户端渲染器需要 IsActive 与 TargetPosition，
            // 只靠 setBlockAndUpdate 不会带上 BE 负载（getUpdatePacket 未实现时客户端永远收不到）。
            level.sendBlockUpdated(pos, state, newState, 3);
        }
    }

    /**
     * A linked node is the transmitter: it takes power from cables and sends it to its target.
     * A node without a target is a receiver: it used to stay a pure consumer, so the energy that
     * arrived wirelessly sat in its buffer and adjacent machines or cables never got any. It now
     * acts as a source and feeds the network from that buffer (at most one transfer per tick).
     */
    private void updateRole() {
        boolean receiver = targetPosition == null;
        if (receiver == isPowerSource()) return;
        if (receiver) setAsPowerSource(TRANSFER_SPEED);
        else setAsConsumer();
        getEnergyStorageInternal().setMaxExtract(receiver ? TRANSFER_SPEED : DEFAULT_MAX_EXTRACT);
        setChanged();
        com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.changed(this);   // republish faces
    }

    // ------------------------------------------------------------------ receiver packet size
    /**
     * A receiver emits packets no larger than its weakest neighbour takes: an adjacent machine's
     * input tier, or an adjacent cable's rating (LV when nothing is attached yet). A fixed LuV
     * packet blew up the low-tier machines people put right next to the node.
     */
    private long outputPacket = 32;
    private long packetCheckedTick = Long.MIN_VALUE;

    private long neighbourPacket() {
        long packet = Long.MAX_VALUE;
        if (level == null) return 32;
        for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
            BlockPos at = worldPosition.relative(side);
            if (!level.isLoaded(at)) continue;
            var be = level.getBlockEntity(at);
            if (be instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractWireBlockEntity wire && wire.getCableTier() != null) {
                packet = Math.min(packet, Math.max(1, wire.getCableTier().getPowerRating()));
            } else if (be instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity machine
                    && !(be instanceof mio_icif_wireless_power_transmission_node) && !machine.isPowerSource()) {
                double power = com.miophas.singularity_iteration.core.runtime.energy.grid.EnergyNetGlobal.getPowerFromTier(machine.getSinkTier());
                if (power >= 1) packet = Math.min(packet, (long) Math.min(power, Long.MAX_VALUE));
            }
        }
        return packet == Long.MAX_VALUE ? 32 : Math.min(packet, TRANSFER_SPEED);
    }

    private void refreshPacket() {
        if (level == null || level.getGameTime() - packetCheckedTick < 20 && packetCheckedTick != Long.MIN_VALUE) return;
        packetCheckedTick = level.getGameTime();
        long next = neighbourPacket();
        if (next != outputPacket) {
            outputPacket = next;
            if (isPowerSource()) com.miophas.singularity_iteration.core.runtime.energy.IndependentSiEnergy.changed(this);
        }
    }

    @Override
    public com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy getEnergyPacketPolicy() {
        long packet = Math.max(1, outputPacket);
        int count = (int) Math.max(1, Math.min(com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy.MAX_PACKETS_PER_ROUND, TRANSFER_SPEED / packet));
        return new com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy(packet, count, true);
    }

    /** Receiver nodes have no target: true when this node emits into the network. */
    public boolean isReceiver() { return targetPosition == null; }

    private void wirelessTransfer() {
        if (level == null || level.isClientSide) return;

        long now = level.getGameTime();
        boolean transferred = false;

        // the link has no range limit: an unloaded target is skipped instead of loading its chunk every tick
        if (targetPosition != null && level.isLoaded(targetPosition)) {
            IEnergyStorageAccess storage = getEnergyStorage();
            // whatever is buffered moves (it used to wait for a full 32768 EU, so a weakly fed node never sent)
            if (storage.getAmount() > 0) {
                net.minecraft.world.level.block.entity.BlockEntity targetBE = level.getBlockEntity(targetPosition);
                if (targetBE != null) {
                    if (targetBE instanceof com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity absBE) {
                        com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage targetStorage = absBE.getEnergyStorageInternal();
                        long targetCurrent = targetStorage.getAmount();
                        long targetMax = targetStorage.getCapacity();

                        if (targetCurrent < targetMax) {
                            long toTransfer = Math.min(Math.min(TRANSFER_SPEED, storage.getAmount()), targetMax - targetCurrent);
                            long used = storage.useEnergy(toTransfer, false);   // credit only what was actually taken
                            if (used > 0) {
                                targetStorage.setEnergy(targetCurrent + used);
                                transferred = true;
                            }
                        }
                    } else {
                        IEUEnergyStorage targetStorage = level.getCapability(EUApi.SIDED, targetPosition, null);

                        if (targetStorage != null) {
                            long targetCurrent = targetStorage.getAmount();
                            long targetMax = targetStorage.getCapacity();

                            if (targetCurrent < targetMax) {
                                long toTransfer = Math.min(Math.min(TRANSFER_SPEED, storage.getAmount()), targetMax - targetCurrent);
                                long extracted = storage.useEnergy(toTransfer, false);
                                if (extracted > 0) {
                                    long received = targetStorage.generateEnergy(extracted, false);
                                    if (received < 0 || received > extracted) {
                                        storage.generateEnergy(extracted, false);
                                    } else {
                                        long rejected = extracted - received;
                                        if (rejected > 0) storage.generateEnergy(rejected, false);
                                        transferred = received > 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (transferred) activeUntilTick = now + ACTIVE_HOLD_TICKS;
        isActive = transferred || now < activeUntilTick;
    }

    public BlockPos getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(BlockPos target) {
        this.targetPosition = target;
        setChanged();
        if (level != null && !level.isClientSide()) {
            // 立即把新目标同步给客户端，否则渲染器仍按旧目标（或 null）画粒子。
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isTransmitting() {
        return isActive;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (targetPosition != null) {
            tag.putInt("TargetX", targetPosition.getX());
            tag.putInt("TargetY", targetPosition.getY());
            tag.putInt("TargetZ", targetPosition.getZ());
        }
        tag.putBoolean("IsActive", isActive);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("TargetX")) {
            int x = tag.getInt("TargetX");
            int y = tag.getInt("TargetY");
            int z = tag.getInt("TargetZ");
            targetPosition = new BlockPos(x, y, z);
        } else {
            targetPosition = null;
        }
        isActive = tag.getBoolean("IsActive");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (targetPosition != null) {
            tag.putInt("TargetX", targetPosition.getX());
            tag.putInt("TargetY", targetPosition.getY());
            tag.putInt("TargetZ", targetPosition.getZ());
        }
        tag.putBoolean("IsActive", isActive);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("TargetX")) {
            int x = tag.getInt("TargetX");
            int y = tag.getInt("TargetY");
            int z = tag.getInt("TargetZ");
            targetPosition = new BlockPos(x, y, z);
        } else {
            targetPosition = null;
        }
        isActive = tag.getBoolean("IsActive");
    }

    /**
     * 默认实现返回 null，方块更新不会携带方块实体负载，客户端因此永远拿不到
     * {@code IsActive} / {@code TargetPosition}（现象是粒子不渲染，且只有重新进入存档才刷新）。
     */
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}