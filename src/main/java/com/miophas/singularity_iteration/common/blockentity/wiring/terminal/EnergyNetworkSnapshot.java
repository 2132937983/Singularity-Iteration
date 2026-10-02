package com.miophas.singularity_iteration.common.blockentity.wiring.terminal;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * One second of energy-network telemetry as shown by the terminal: totals, rolling
 * history and the device table. Plain data with a compact wire format (rates in
 * EU/t as floats, history as fixed-length float arrays).
 */
public final class EnergyNetworkSnapshot {
    public static final int HISTORY = 112;
    public static final int MAX_DEVICES = 256;

    public enum Category { GENERATOR, CONSUMER, STORAGE, TRANSFORMER }

    /** One endpoint on the network. Rates are EU/t averaged over the last second. */
    public record Device(BlockPos pos, String blockId, Category category, float input, float output,
                         long stored, long capacity, boolean switchable, boolean disabled) { }

    public float generation, consumption, storageIn, storageOut;
    public long stored, capacity;
    public int conductors;
    public boolean truncated;
    public final float[] genHistory = new float[HISTORY], useHistory = new float[HISTORY], chargeHistory = new float[HISTORY];
    public final List<Device> devices = new ArrayList<>();

    public float chargeFraction() {
        return capacity <= 0 ? 0 : (float) Math.min(1.0, stored / (double) capacity);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(generation); buf.writeFloat(consumption); buf.writeFloat(storageIn); buf.writeFloat(storageOut);
        buf.writeVarLong(stored); buf.writeVarLong(capacity);
        buf.writeVarInt(conductors); buf.writeBoolean(truncated);
        for (int i = 0; i < HISTORY; i++) { buf.writeFloat(genHistory[i]); buf.writeFloat(useHistory[i]); buf.writeFloat(chargeHistory[i]); }
        int n = Math.min(devices.size(), MAX_DEVICES);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            Device d = devices.get(i);
            buf.writeBlockPos(d.pos());
            buf.writeUtf(d.blockId(), 160);
            buf.writeByte(d.category().ordinal() | (d.switchable() ? 0x10 : 0) | (d.disabled() ? 0x20 : 0));
            buf.writeFloat(d.input()); buf.writeFloat(d.output());
            buf.writeVarLong(Math.max(0, d.stored())); buf.writeVarLong(Math.max(0, d.capacity()));
        }
    }

    public static EnergyNetworkSnapshot read(FriendlyByteBuf buf) {
        EnergyNetworkSnapshot s = new EnergyNetworkSnapshot();
        s.generation = buf.readFloat(); s.consumption = buf.readFloat(); s.storageIn = buf.readFloat(); s.storageOut = buf.readFloat();
        s.stored = buf.readVarLong(); s.capacity = buf.readVarLong();
        s.conductors = buf.readVarInt(); s.truncated = buf.readBoolean();
        for (int i = 0; i < HISTORY; i++) { s.genHistory[i] = buf.readFloat(); s.useHistory[i] = buf.readFloat(); s.chargeHistory[i] = buf.readFloat(); }
        int n = buf.readVarInt();
        if (n < 0 || n > MAX_DEVICES) throw new IllegalArgumentException("Bad device count " + n);
        Category[] cats = Category.values();
        for (int i = 0; i < n; i++) {
            BlockPos pos = buf.readBlockPos();
            String id = buf.readUtf(160);
            int flags = buf.readUnsignedByte();
            float in = buf.readFloat(), out = buf.readFloat();
            long stored = buf.readVarLong(), cap = buf.readVarLong();
            s.devices.add(new Device(pos, id, cats[Math.min(flags & 0x0F, cats.length - 1)], in, out, stored, cap,
                (flags & 0x10) != 0, (flags & 0x20) != 0));
        }
        return s;
    }
}
