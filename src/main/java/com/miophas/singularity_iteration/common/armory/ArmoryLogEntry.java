package com.miophas.singularity_iteration.common.armory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * One summon in the Armory history.
 *
 * @param epochMillis    real time of the request
 * @param gameTime       world game time of the request
 * @param caller         player who summoned
 * @param suit           suit name at the time
 * @param cost           EU paid
 * @param straight       straight-line distance to the Armory in blocks, or -1 across dimensions
 * @param flight         longest flight path of the pieces in blocks
 * @param pieces         number of pieces flown
 * @param dimension      dimension of the caller
 */
public record ArmoryLogEntry(long epochMillis, long gameTime, String caller, String suit, long cost,
                             double straight, double flight, int pieces, String dimension) {

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putLong("ms", epochMillis); t.putLong("gt", gameTime); t.putString("by", caller); t.putString("suit", suit);
        t.putLong("eu", cost); t.putDouble("d", straight); t.putDouble("f", flight); t.putInt("n", pieces); t.putString("dim", dimension);
        return t;
    }

    public static ArmoryLogEntry load(CompoundTag t) {
        return new ArmoryLogEntry(t.getLong("ms"), t.getLong("gt"), t.getString("by"), t.getString("suit"), t.getLong("eu"),
            t.getDouble("d"), t.getDouble("f"), t.getInt("n"), t.getString("dim"));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeLong(epochMillis); buf.writeLong(gameTime); buf.writeUtf(caller, 64); buf.writeUtf(suit, 64);
        buf.writeVarLong(cost); buf.writeDouble(straight); buf.writeDouble(flight); buf.writeVarInt(pieces); buf.writeUtf(dimension, 128);
    }

    public static ArmoryLogEntry read(FriendlyByteBuf buf) {
        return new ArmoryLogEntry(buf.readLong(), buf.readLong(), buf.readUtf(64), buf.readUtf(64), buf.readVarLong(),
            buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readUtf(128));
    }
}
