package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;
import com.miophas.singularity_iteration.core.api.energy.EnergyPortPolicy;
import com.miophas.singularity_iteration.core.api.energy.IEnergyPacketSource;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponent;
import com.miophas.singularity_iteration.core.prefab.component.EnergyComponentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Explicit packet policy with composition and the standard owned storage lifecycle. */
public final class ComposedPacketSource extends BlockEntity implements EnergyComponentHost, IEnergyPacketSource {
    private EnergyPacketPolicy packets = new EnergyPacketPolicy(32, 8, true);
    private int requestedFe, extractedFe;
    public boolean failPolicy;
    private final EnergyComponent energy = new EnergyComponent(this, 65536, 0, 2048, CableTier.LV,
        () -> new EnergyPortPolicy(0, 1 << Direction.EAST.ordinal(), true));

    public ComposedPacketSource(BlockPos pos, BlockState state) { super(CoreExampleMod.PACKET_SOURCE_ENTITY.get(), pos, state); }
    @Override public EnergyComponent energyComponent() { return energy; }
    @Override public EnergyPacketPolicy getEnergyPacketPolicy() {
        if (failPolicy) throw new IllegalStateException("Injected addon policy failure");
        return packets;
    }
    public void configure(int count, boolean partial) {
        packets = new EnergyPacketPolicy(32, count, partial);
        energy.portsChanged();
    }
    // Test instrument: an external draw during the machine tick, before native END settlement.
    public void requestFeProbe(int amount) { requestedFe = amount; }
    public int extractedFe() { return extractedFe; }
    public void tickFeProbe() { extractedFe = energy.fePort(Direction.EAST).extractEnergy(requestedFe, false); }
    @Override public void setLevel(Level level) { super.setLevel(level); energy.setLevel(level); }
    @Override public void onLoad() { super.onLoad(); energy.onLoad(); }
    @Override public void setRemoved() { energy.setRemoved(); super.setRemoved(); }
    @Override public void clearRemoved() { super.clearRemoved(); energy.clearRemoved(); }
    @Override public void onChunkUnloaded() { energy.onChunkUnloaded(); super.onChunkUnloaded(); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        energy.save(tag);
        tag.putInt("packet_count", packets.packetCount());
        tag.putBoolean("partial_packets", packets.partialPackets());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.load(tag);
        if (tag.contains("packet_count")) packets = new EnergyPacketPolicy(32,
            Math.clamp(tag.getInt("packet_count"), 0, EnergyPacketPolicy.MAX_PACKETS_PER_ROUND), tag.getBoolean("partial_packets"));
    }
}
