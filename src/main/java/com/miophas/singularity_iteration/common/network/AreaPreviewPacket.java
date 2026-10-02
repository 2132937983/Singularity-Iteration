package com.miophas.singularity_iteration.common.network;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.area.WorkArea;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> client: work areas of one or more machines for the Area Preview.
 *
 * @param toggle  when true, a machine already shown is hidden instead (button / tool click)
 * @param ticks   how long the preview stays visible
 */
@SuppressWarnings("null")
public record AreaPreviewPacket(List<Entry> entries, boolean toggle, int ticks) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 64, MAX_AREAS = 128;

    public record Entry(BlockPos machine, List<WorkArea> areas) { }

    public static final Type<AreaPreviewPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "area_preview"));

    public static final StreamCodec<FriendlyByteBuf, AreaPreviewPacket> CODEC = StreamCodec.of(
        (buf, p) -> {
            buf.writeBoolean(p.toggle);
            buf.writeVarInt(p.ticks);
            int n = Math.min(MAX_ENTRIES, p.entries.size());
            buf.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                Entry e = p.entries.get(i);
                buf.writeBlockPos(e.machine);
                int m = Math.min(MAX_AREAS, e.areas.size());
                buf.writeVarInt(m);
                for (int j = 0; j < m; j++) e.areas.get(j).write(buf);
            }
        },
        buf -> {
            boolean toggle = buf.readBoolean();
            int ticks = buf.readVarInt();
            int n = Math.min(MAX_ENTRIES, buf.readVarInt());
            List<Entry> entries = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                BlockPos pos = buf.readBlockPos();
                int m = Math.min(MAX_AREAS, buf.readVarInt());
                List<WorkArea> areas = new ArrayList<>(m);
                for (int j = 0; j < m; j++) areas.add(WorkArea.read(buf));
                entries.add(new Entry(pos, areas));
            }
            return new AreaPreviewPacket(entries, toggle, ticks);
        });

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AreaPreviewPacket packet, IPayloadContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        context.enqueueWork(() ->
            com.miophas.singularity_iteration.common.client.area.AreaPreviewClient.accept(packet));
    }
}
