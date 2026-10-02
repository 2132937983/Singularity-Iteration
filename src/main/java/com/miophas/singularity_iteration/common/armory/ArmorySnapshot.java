package com.miophas.singularity_iteration.common.armory;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Everything the remote controller shows, sent server -> client while the remote is open. */
public record ArmorySnapshot(GlobalPos armory, int status, String owner, long energy, long capacity, boolean busy,
                             List<String> names, List<List<ItemStack>> suits, List<ArmoryLogEntry> log,
                             int mana, int maintenanceState, int etaTicks, int pendingPieces) {
    public static final int OK = 0, MISSING = 1, NOT_OWNER = 2, UNBOUND = 3;

    public static ArmorySnapshot of(GlobalPos pos, int status) {
        return new ArmorySnapshot(pos, status, "", 0, mio_icif_armory.CAPACITY, false, List.of(), List.of(), List.of(), 0, 0, 0, 0);
    }

    public static ArmorySnapshot of(GlobalPos pos, mio_icif_armory armory, int status) {
        List<String> names = new ArrayList<>();
        List<List<ItemStack>> suits = new ArrayList<>();
        for (int s = 0; s < mio_icif_armory.SETS; s++) {
            names.add(armory.setName(s));
            List<ItemStack> row = new ArrayList<>();
            for (ArmoryPiece p : ArmoryPiece.COLUMNS) row.add(armory.piece(s, p).copy());
            suits.add(row);
        }
        return new ArmorySnapshot(pos, status, armory.ownerName(), armory.getEnergyStorage().getAmount(),
            armory.getEnergyStorage().getCapacity(), armory.isBusy(), names, suits, armory.log(), armory.mana(),
            armory.maintenance().state(), armory.maintenance().etaTicks(), armory.maintenance().pendingPieces());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        GlobalPos.STREAM_CODEC.encode(buf, armory);
        buf.writeVarInt(status); buf.writeUtf(owner, 64); buf.writeVarLong(energy); buf.writeVarLong(capacity); buf.writeBoolean(busy);
        buf.writeVarInt(names.size());
        for (int i = 0; i < names.size(); i++) {
            buf.writeUtf(names.get(i), 64);
            for (ItemStack stack : suits.get(i)) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
        buf.writeVarInt(log.size());
        for (ArmoryLogEntry e : log) e.write(buf);
        buf.writeVarInt(mana); buf.writeVarInt(maintenanceState); buf.writeInt(etaTicks); buf.writeVarInt(pendingPieces);
    }

    public static ArmorySnapshot read(RegistryFriendlyByteBuf buf) {
        GlobalPos pos = GlobalPos.STREAM_CODEC.decode(buf);
        int status = buf.readVarInt(); String owner = buf.readUtf(64);
        long energy = buf.readVarLong(), capacity = buf.readVarLong(); boolean busy = buf.readBoolean();
        int n = Math.min(mio_icif_armory.SETS, buf.readVarInt());
        List<String> names = new ArrayList<>(); List<List<ItemStack>> suits = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            names.add(buf.readUtf(64));
            List<ItemStack> row = new ArrayList<>();
            for (int j = 0; j < mio_icif_armory.PIECES; j++) row.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            suits.add(row);
        }
        int m = Math.min(mio_icif_armory.LOG_SIZE, buf.readVarInt());
        List<ArmoryLogEntry> log = new ArrayList<>();
        for (int i = 0; i < m; i++) log.add(ArmoryLogEntry.read(buf));
        int mana = buf.readVarInt(), state = buf.readVarInt(), eta = buf.readInt(), pending = buf.readVarInt();
        return new ArmorySnapshot(pos, status, owner, energy, capacity, busy, names, suits, log, mana, state, eta, pending);
    }

    public int pieces(int set) {
        if (set < 0 || set >= suits.size()) return 0;
        int n = 0;
        for (ItemStack s : suits.get(set)) if (!s.isEmpty()) n++;
        return n;
    }
}
