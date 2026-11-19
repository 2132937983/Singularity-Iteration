package example.sicore;

import com.miophas.singularity_iteration.core.api.energy.EnergyPacketPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.83: a receiving wireless node sizes its packets by what is attached. Nothing: LV (32 EU). An EV cable: 2,048 EU.
 * An IV cable: 8,192 EU. A superconducting cable: the node's largest packet (LuV, 32,768 EU), as many per tick as the
 * wireless link carries. The superconductor rates Long.MAX_VALUE and was read as "nothing attached", so the node sent
 * 32 EU packets into it: 64 x 32 = 2,048 EU/t whatever the link speed.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11583GameTests {
    private Round11583GameTests() {}

    private static final long LUV = 32_768L;

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }
    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static EnergyPacketPolicy policy(GameTestHelper h, BlockPos node) {
        return (EnergyPacketPolicy) call(h.getBlockEntity(node), "getEnergyPacketPolicy");
    }

    /** A receiving node (no target) at {@code node}, with {@code cable} (or nothing) on its east side. */
    private static void receiver(GameTestHelper h, BlockPos node, String cable) {
        if (cable != null) h.setBlock(node.east(), BuiltInRegistries.BLOCK.get(id(cable)));
        h.setBlock(node, BuiltInRegistries.BLOCK.get(id("wiring/block_wireless_power_transmission_node")));
    }

    @GameTest(template = "empty", batch = "round11583_wireless", timeoutTicks = 60)
    public static void receiverPacketsFollowTheAttachedCable(GameTestHelper h) {
        BlockPos alone = new BlockPos(1, 1, 1), superconductor = new BlockPos(1, 1, 3), ev = new BlockPos(4, 1, 1), iv = new BlockPos(4, 1, 3);
        receiver(h, alone, null);
        receiver(h, superconductor, "wiring/cable/block_superconducting_cable");
        receiver(h, ev, "wiring/cable/block_iron_cable");
        receiver(h, iv, "wiring/cable/block_glass_cable");
        h.runAfterDelay(25, () -> {
            BlockEntity node = h.getBlockEntity(superconductor);
            long transfer = (long) call(call(node, "getEnergyStorageInternal"), "getMaxExtract");
            h.assertTrue(policy(h, alone).packetSize() == 32, "a node with nothing attached sends " + policy(h, alone).packetSize() + " EU packets");
            EnergyPacketPolicy sc = policy(h, superconductor);
            h.assertTrue(sc.packetSize() == LUV, "a node on a superconductor sends " + sc.packetSize() + " EU packets (LuV expected)");
            h.assertTrue(sc.maximumTransfer() == Math.min(transfer, EnergyPacketPolicy.MAX_PACKETS_PER_ROUND * LUV),
                "a node on a superconductor moves " + sc.maximumTransfer() + " EU/t, the link carries " + transfer);
            h.assertTrue(sc.maximumTransfer() > 2_048, "the superconductor is still held to 2,048 EU/t");
            h.assertTrue(policy(h, ev).packetSize() == 2_048, "an EV cable gets " + policy(h, ev).packetSize() + " EU packets");
            h.assertTrue(policy(h, iv).packetSize() == 8_192, "an IV cable gets " + policy(h, iv).packetSize() + " EU packets");
            h.succeed();
        });
    }
}
