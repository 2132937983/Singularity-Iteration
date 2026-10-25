package example.sicore;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.18: the teleporter sends its particle state (active, cooldown) to the client. Up
 * to 0.1.10.17 neither value reached the client, so the pads never showed particles.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1118GameTests {
    private Round1118GameTests() {}

    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static BlockEntity teleporter(GameTestHelper h) {
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_teleporter_elc"));
        h.assertTrue(block != Blocks.AIR, "teleporter not registered");
        h.setBlock(AT, block);
        BlockEntity be = h.getBlockEntity(AT);
        h.assertTrue(be != null, "teleporter has no block entity");
        return be;
    }

    /** The update tag and the update packet carry the particle state; the packet carries nothing else. */
    @GameTest(batch = "round1118_teleporter", template = "empty", timeoutTicks = 20)
    public static void teleporterSendsParticleState(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        BlockEntity be = teleporter(h);
        call(be, "setActive", true);
        CompoundTag update = be.getUpdateTag(registries);
        h.assertTrue(update.getBoolean("isActive") && update.contains("cooldown"), "update tag: " + update.getAllKeys());
        h.assertTrue(!update.contains("inventory"), "update tag carries the inventory again");
        var packet = be.getUpdatePacket();
        h.assertTrue(packet instanceof ClientboundBlockEntityDataPacket, "no update packet");
        CompoundTag sent = ((ClientboundBlockEntityDataPacket) packet).getTag();
        h.assertTrue(sent.getAllKeys().equals(Set.of("isActive", "cooldown")), "packet keys: " + sent.getAllKeys());
        h.succeed();
    }

    /**
     * A client copy takes the particle state from a packet and keeps everything else (target, items);
     * it counts the cooldown down by itself, so the colour changes back without another packet.
     */
    @GameTest(batch = "round1118_teleporter", template = "empty", timeoutTicks = 20)
    public static void clientCopyTakesOnlyTheParticleState(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        BlockEntity be = teleporter(h);
        BlockPos target = be.getBlockPos().offset(5, 0, 0);
        call(be, "setTargetPos", target);
        if (be instanceof Container c && c.getContainerSize() > 0) c.setItem(0, new ItemStack(Items.REDSTONE, 7));
        BlockEntity copy = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), be.saveWithFullMetadata(registries), registries);
        h.assertTrue(copy != null, "no client copy");
        h.assertTrue(!(boolean) call(copy, "isActive"), "the copy starts active");

        call(be, "setActive", true);
        call(be, "onTeleportFrom", be);                         // the target pad of a teleport cools down
        int cooldown = (int) call(be, "getCooldown");
        h.assertTrue(cooldown > 0, "no cooldown after a teleport");
        copy.onDataPacket(null, (ClientboundBlockEntityDataPacket) be.getUpdatePacket(), registries);
        h.assertTrue((boolean) call(copy, "isActive"), "packet: not active on the client");
        h.assertTrue((int) call(copy, "getCooldown") == cooldown, "packet: cooldown " + call(copy, "getCooldown") + ", expected " + cooldown);
        h.assertTrue(target.equals(call(copy, "getTargetPos")), "packet: the target of the copy changed");
        if (copy instanceof Container c && c.getContainerSize() > 0) {
            h.assertTrue(c.getItem(0).is(Items.REDSTONE) && c.getItem(0).getCount() == 7, "packet: the items of the copy changed");
        }
        for (int i = 0; i < cooldown; i++) call(copy, "tickClient");
        h.assertTrue((int) call(copy, "getCooldown") == 0, "the client copy did not count the cooldown down");
        h.succeed();
    }
}
