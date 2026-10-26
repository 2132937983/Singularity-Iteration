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

    /** 0.1.10.22: PROTECTION = a breaker or fuse cable. */
    public enum Category { GENERATOR, CONSUMER, STORAGE, TRANSFORMER, PROTECTION }

    /** One endpoint on the network. Rates are EU/t averaged over the last second. */
    /** {@code rated}: theoretical EU/t of the device (generator nameplate / consumer draw while working). */
    /** {@code subnet}: index into {@link #subnets} (-1 unknown); {@code voltage}: largest EU packet seen in the window. */
    /** 0.1.10.22: {@code priority}: load priority 0 critical, 1 normal, 2 deferrable (-1: none); {@code dispatch}: 0 must-run, 1 dispatchable (-1: none). */
    public record Device(BlockPos pos, String blockId, Category category, float input, float output,
                         long stored, long capacity, boolean switchable, boolean disabled, float rated, int subnet, float voltage,
                         int priority, int dispatch) {
        public Device(BlockPos pos, String blockId, Category category, float input, float output,
                      long stored, long capacity, boolean switchable, boolean disabled, float rated, int subnet, float voltage) {
            this(pos, blockId, category, input, output, stored, capacity, switchable, disabled, rated, subnet, voltage, -1, -1);
        }
        public Device(BlockPos pos, String blockId, Category category, float input, float output,
                      long stored, long capacity, boolean switchable, boolean disabled, float rated) {
            this(pos, blockId, category, input, output, stored, capacity, switchable, disabled, rated, -1, 0F);
        }
        public Device(BlockPos pos, String blockId, Category category, float input, float output,
                      long stored, long capacity, boolean switchable, boolean disabled) {
            this(pos, blockId, category, input, output, stored, capacity, switchable, disabled, 0F);
        }
    }

    /** One sub-network (cables between transformers) in global mode. Rates in EU/t. */
    public static final class Subnet {
        public int conductors, devices;
        public float generation, generationCapacity, consumption, demand, storageIn, storageOut;
        /** Cable rating (EU/packet) and the largest packet measured. */
        public long ratedPacket;
        public float measuredPacket;
    }

    /** 0.1.10.22 (G7): a link of the single-line diagram: device {@code device} (a buffer or an open breaker) also touches {@code subnet}. */
    public record Link(int device, int subnet) { }
    /** 0.1.10.22 (G5 / G7): one breaker or fuse: kind (0 breaker, 1 fuse), state, last cause, flow in the last tick, settings, counters. */
    public record Breaker(BlockPos pos, int kind, int state, int cause, float flow, long ratedFlow, long ratedVoltage, int delay,
                          boolean reclose, int recloseIn, int trips, float heat) { }
    /** 0.1.10.22 (G7): one alarm of this network (see {@code GridAlarms}). */
    public record Alarm(long time, int type, BlockPos pos, int detail, float value) { }

    public static final int MAX_LINKS = 512, MAX_BREAKERS = 64, MAX_ALARMS = 40;
    public final List<Link> links = new ArrayList<>();
    public final List<Breaker> breakers = new ArrayList<>();
    public final List<Alarm> alarms = new ArrayList<>();
    /** 0.1.10.22: generation by dispatch class and storage discharge (EU/t), unmet demand of critical loads. */
    public float generationMustRun, generationFuel, criticalDemand, criticalShortfall;
    /** 0.1.10.22: engine time of the windows of this network's sources (microseconds per tick), and its history. */
    public float costMicros;
    public final float[] costHistory = new float[HISTORY];
    /** 0.1.10.22: game time of the snapshot (alarm ages) and the dispatch mode (0 merit, 1 economic, 2 rotation). */
    public long gameTime;
    public int dispatchMode;

    /** Global mode: the walk crossed transformers and {@link #subnets} breaks the system down. */
    public boolean global;
    public final List<Subnet> subnets = new ArrayList<>();

    public float generation, consumption, storageIn, storageOut;
    /** Theoretical maximum generation and consumption (sum of nameplates) for the satisfaction meters. */
    public float generationCapacity, demand;
    public long stored, capacity;
    public int conductors;
    public boolean truncated;
    public final float[] genHistory = new float[HISTORY], useHistory = new float[HISTORY], chargeHistory = new float[HISTORY];
    public final List<Device> devices = new ArrayList<>();

    /** Share of the generators' nameplate actually produced (DSP "generation utilisation"). */
    public float generationRatio() { return generationCapacity <= 0 ? 0 : Math.min(1F, generation / generationCapacity); }

    /** Share of the full consumer demand that is being met. */
    public float demandRatio() { return demand <= 0 ? 1F : Math.min(1F, consumption / demand); }

    public float chargeFraction() {
        return capacity <= 0 ? 0 : (float) Math.min(1.0, stored / (double) capacity);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(generation); buf.writeFloat(consumption); buf.writeFloat(storageIn); buf.writeFloat(storageOut);
        buf.writeVarLong(stored); buf.writeVarLong(capacity);
        buf.writeVarInt(conductors); buf.writeBoolean(truncated);
        buf.writeFloat(generationCapacity); buf.writeFloat(demand);
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
            buf.writeFloat(d.rated());
            buf.writeVarInt(d.subnet() + 1);
            buf.writeFloat(d.voltage());
        }
        buf.writeBoolean(global);
        int m = Math.min(subnets.size(), 64);
        buf.writeVarInt(m);
        for (int i = 0; i < m; i++) {
            Subnet sn = subnets.get(i);
            buf.writeVarInt(sn.conductors); buf.writeVarInt(sn.devices);
            buf.writeFloat(sn.generation); buf.writeFloat(sn.generationCapacity); buf.writeFloat(sn.consumption); buf.writeFloat(sn.demand);
            buf.writeFloat(sn.storageIn); buf.writeFloat(sn.storageOut);
            buf.writeVarLong(Math.max(0, sn.ratedPacket)); buf.writeFloat(sn.measuredPacket);
        }
        // 0.1.10.22
        for (int i = 0; i < n; i++) { Device d = devices.get(i); buf.writeByte(d.priority() + 1); buf.writeByte(d.dispatch() + 1); }
        int links = Math.min(this.links.size(), MAX_LINKS);
        buf.writeVarInt(links);
        for (int i = 0; i < links; i++) { buf.writeVarInt(this.links.get(i).device()); buf.writeVarInt(this.links.get(i).subnet()); }
        int breakers = Math.min(this.breakers.size(), MAX_BREAKERS);
        buf.writeVarInt(breakers);
        for (int i = 0; i < breakers; i++) {
            Breaker b = this.breakers.get(i);
            buf.writeBlockPos(b.pos()); buf.writeByte(b.kind()); buf.writeByte(b.state()); buf.writeByte(b.cause()); buf.writeFloat(b.flow());
            buf.writeVarLong(b.ratedFlow()); buf.writeVarLong(b.ratedVoltage()); buf.writeVarInt(b.delay()); buf.writeBoolean(b.reclose());
            buf.writeVarInt(b.recloseIn()); buf.writeVarInt(b.trips()); buf.writeFloat(b.heat());
        }
        int alarms = Math.min(this.alarms.size(), MAX_ALARMS);
        buf.writeVarInt(alarms);
        for (int i = 0; i < alarms; i++) {
            Alarm a = this.alarms.get(i);
            buf.writeVarLong(a.time()); buf.writeByte(a.type()); buf.writeBlockPos(a.pos()); buf.writeVarInt(a.detail()); buf.writeFloat(a.value());
        }
        buf.writeFloat(generationMustRun); buf.writeFloat(generationFuel); buf.writeFloat(criticalDemand); buf.writeFloat(criticalShortfall);
        buf.writeFloat(costMicros);
        for (int i = 0; i < HISTORY; i++) buf.writeFloat(costHistory[i]);
        buf.writeVarLong(gameTime); buf.writeByte(dispatchMode);
    }

    public static EnergyNetworkSnapshot read(FriendlyByteBuf buf) {
        EnergyNetworkSnapshot s = new EnergyNetworkSnapshot();
        s.generation = buf.readFloat(); s.consumption = buf.readFloat(); s.storageIn = buf.readFloat(); s.storageOut = buf.readFloat();
        s.stored = buf.readVarLong(); s.capacity = buf.readVarLong();
        s.conductors = buf.readVarInt(); s.truncated = buf.readBoolean();
        s.generationCapacity = buf.readFloat(); s.demand = buf.readFloat();
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
            float rated = buf.readFloat();
            int subnet = buf.readVarInt() - 1;
            float voltage = buf.readFloat();
            s.devices.add(new Device(pos, id, cats[Math.min(flags & 0x0F, cats.length - 1)], in, out, stored, cap,
                (flags & 0x10) != 0, (flags & 0x20) != 0, rated, subnet, voltage));
        }
        s.global = buf.readBoolean();
        int m = buf.readVarInt();
        if (m < 0 || m > 64) throw new IllegalArgumentException("Bad subnet count " + m);
        for (int i = 0; i < m; i++) {
            Subnet sn = new Subnet();
            sn.conductors = buf.readVarInt(); sn.devices = buf.readVarInt();
            sn.generation = buf.readFloat(); sn.generationCapacity = buf.readFloat(); sn.consumption = buf.readFloat(); sn.demand = buf.readFloat();
            sn.storageIn = buf.readFloat(); sn.storageOut = buf.readFloat();
            sn.ratedPacket = buf.readVarLong(); sn.measuredPacket = buf.readFloat();
            s.subnets.add(sn);
        }
        // 0.1.10.22
        for (int i = 0; i < n; i++) {
            int priority = buf.readUnsignedByte() - 1, dispatch = buf.readUnsignedByte() - 1;
            Device d = s.devices.get(i);
            s.devices.set(i, new Device(d.pos(), d.blockId(), d.category(), d.input(), d.output(), d.stored(), d.capacity(), d.switchable(),
                d.disabled(), d.rated(), d.subnet(), d.voltage(), Math.max(-1, Math.min(2, priority)), Math.max(-1, Math.min(1, dispatch))));
        }
        int links = buf.readVarInt();
        if (links < 0 || links > MAX_LINKS) throw new IllegalArgumentException("Bad link count " + links);
        for (int i = 0; i < links; i++) s.links.add(new Link(buf.readVarInt(), buf.readVarInt()));
        int breakers = buf.readVarInt();
        if (breakers < 0 || breakers > MAX_BREAKERS) throw new IllegalArgumentException("Bad breaker count " + breakers);
        for (int i = 0; i < breakers; i++) {
            BlockPos pos = buf.readBlockPos();
            int kind = buf.readUnsignedByte(), state = buf.readUnsignedByte(), cause = buf.readUnsignedByte();
            float flow = buf.readFloat();
            long ratedFlow = buf.readVarLong(), ratedVoltage = buf.readVarLong();
            int delay = buf.readVarInt();
            boolean reclose = buf.readBoolean();
            int recloseIn = buf.readVarInt(), trips = buf.readVarInt();
            float heat = buf.readFloat();
            s.breakers.add(new Breaker(pos, kind, state, cause, flow, ratedFlow, ratedVoltage, delay, reclose, recloseIn, trips, heat));
        }
        int alarms = buf.readVarInt();
        if (alarms < 0 || alarms > MAX_ALARMS) throw new IllegalArgumentException("Bad alarm count " + alarms);
        for (int i = 0; i < alarms; i++) s.alarms.add(new Alarm(buf.readVarLong(), buf.readUnsignedByte(), buf.readBlockPos(), buf.readVarInt(), buf.readFloat()));
        s.generationMustRun = buf.readFloat(); s.generationFuel = buf.readFloat(); s.criticalDemand = buf.readFloat(); s.criticalShortfall = buf.readFloat();
        s.costMicros = buf.readFloat();
        for (int i = 0; i < HISTORY; i++) s.costHistory[i] = buf.readFloat();
        s.gameTime = buf.readVarLong(); s.dispatchMode = buf.readUnsignedByte();
        return s;
    }
}
