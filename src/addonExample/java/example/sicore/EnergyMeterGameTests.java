package example.sicore;

import com.miophas.singularity_iteration.core.runtime.energy.MeterReadings;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class EnergyMeterGameTests {
    @GameTest(template = "empty", batch = "energy_meter")
    public static void meterWordsSurviveRealContainerPacketEncoding(GameTestHelper h) {
        double[] values = {0, 0.000001, 1.0 / 3, 32, 128.125, 2048, 65536.75, 1.0e12, -2048.125};
        for (double value : values) {
            MeterReadings server = new MeterReadings(), client = new MeterReadings();
            server.update(2, value, -Math.abs(value), Math.abs(value), 70001);
            for (int i = 0; i < server.getCount(); i++) transmit(client, i, server.get(i));
            h.assertTrue(Double.doubleToLongBits(client.average()) == Double.doubleToLongBits(value), "Average lost double precision");
            h.assertTrue(client.minimum() == -Math.abs(value) && client.maximum() == Math.abs(value), "Signed extrema corrupted");
            h.assertTrue(client.samples() == 70001 && client.mode() == 2, "Count wrapped or mode changed in packet");
        }
        h.succeed();
    }

    @GameTest(template = "empty", batch = "energy_meter", timeoutTicks = 100)
    public static void realMeterSamplesCompletedTicksAndIncludesIdleTime(GameTestHelper h) {
        BlockPos sourcePos = new BlockPos(1, 1, 1), sinkPos = sourcePos.east();
        h.setBlock(sourcePos, CoreExampleMod.SOURCE.get());
        h.setBlock(sinkPos, CoreExampleMod.SINK.get());
        InheritedSource source = h.getBlockEntity(sourcePos);
        InheritedSink sink = h.getBlockEntity(sinkPos);
        boolean[] supply = {true}, pulse = {false};
        ServerPlayer[] player = {null};
        AbstractContainerMenu[] menu = {null};
        MeterReadings client = new MeterReadings();
        h.onEachTick(() -> {
            sink.getEnergyStorageInternal().consumeEnergyInternal(1024, false);
            if (supply[0] && (!pulse[0] || h.getLevel().getGameTime() % 2 == 0)) source.generate(32);
        });
        h.runAfterDelay(8, () -> {
            player[0] = h.makeMockServerPlayerInLevel();
            var at = h.absolutePos(sinkPos);
            player[0].teleportTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5);
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("mio_icif:item_tool_meter"));
            var stack = new ItemStack(item);
            player[0].setItemInHand(InteractionHand.MAIN_HAND, stack);
            // Mock players have no negotiated advanced_open_screen channel. Use the
            // registered menu factory; reads still travel through the actual packet codec.
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
            try {
                buffer.writeBlockPos(at);
                buffer.writeVarInt(0);
                menu[0] = BuiltInRegistries.MENU.get(ResourceLocation.parse("mio_icif:eu_meter"))
                    .create(1, player[0].getInventory(), buffer);
            } finally { buffer.release(); }
            player[0].containerMenu = menu[0];
            h.assertTrue(menu[0].getType() == BuiltInRegistries.MENU.get(ResourceLocation.parse("mio_icif:eu_meter")), "Real meter menu did not open");
            menu[0].addSlotListener(new ContainerListener() {
                public void slotChanged(AbstractContainerMenu menu, int slot, ItemStack stack) { }
                public void dataChanged(AbstractContainerMenu menu, int slot, int value) { transmit(client, slot, value); }
            });
        });
        h.runAfterDelay(15, () -> {
            exact(h, client, 32);
            int samples = client.samples();
            for (int i = 0; i < 20; i++) menu[0].broadcastChanges();
            h.assertTrue(client.samples() == samples, "Inventory synchronization added fake ticks");
            h.assertTrue(menu[0].clickMenuButton(player[0], 1), "Output mode rejected");
            h.assertTrue(client.mode() == 1 && client.samples() == 0 && client.average() == 0, "Mode/reset not synchronized immediately");
        });
        h.runAfterDelay(20, () -> { exact(h, client, 0); menu[0].clickMenuButton(player[0], 2); });
        h.runAfterDelay(26, () -> { exact(h, client, 32); menu[0].clickMenuButton(player[0], 3); });
        h.runAfterDelay(32, () -> {
            exact(h, client, 1); // IC2 voltage display is tier 1 for a 32 EU packet.
            supply[0] = false;
            source.getEnergyStorageInternal().setStored(0);
            menu[0].clickMenuButton(player[0], 0);
        });
        h.runAfterDelay(37, () -> {
            exact(h, client, 0);
            supply[0] = true;
            pulse[0] = true;
        });
        // Let the alternating supply reach END settlement before resetting the
        // 20-sample window. Otherwise its first sample depends on world-time parity.
        h.runAfterDelay(39, () -> {
            menu[0].clickMenuButton(player[0], 100);
        });
        h.runAfterDelay(60, () -> {
            h.assertTrue(client.samples() == 20, "Sampling time is not one sample per server tick: " + client.samples());
            h.assertTrue(Math.abs(client.average() - 16) < 1.0e-9 && client.minimum() == 0 && client.maximum() == 32,
                "Idle ticks omitted or stale samples replayed: " + client.average());
            h.setBlock(sinkPos, Blocks.AIR);
            h.setBlock(sourcePos, Blocks.AIR);
        });
        h.runAfterDelay(63, () -> {
            h.assertTrue(player[0].containerMenu != menu[0], "Removed node left a frozen meter open");
            player[0].server.getPlayerList().remove(player[0]);
            h.succeed();
        });
    }

    private static void exact(GameTestHelper h, MeterReadings data, double expected) {
        h.assertTrue(data.samples() > 0 && data.average() == expected && data.minimum() == expected && data.maximum() == expected,
            "Meter mode " + data.mode() + " expected " + expected + " but got " + data.average() + "/" + data.minimum() + "/" + data.maximum());
    }

    private static void transmit(MeterReadings client, int slot, int value) {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ClientboundContainerSetDataPacket.STREAM_CODEC.encode(buffer, new ClientboundContainerSetDataPacket(1, slot, value));
            var packet = ClientboundContainerSetDataPacket.STREAM_CODEC.decode(buffer);
            client.set(packet.getId(), packet.getValue());
        } finally { buffer.release(); }
    }
}
