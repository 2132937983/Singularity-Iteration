package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.55 (gaze and Iron Man phase P3): the worn suit flies on as a Wingman. Armour and hand gear come off (blocks in
 * the hand stay), a Wingman in Escort mode stands up in them in front of the owner, it takes the remote's Armory's first
 * free suit position; without a paired remote or with fewer than two armour pieces nothing happens.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11555GameTests {
    private Round11555GameTests() {}

    private static final BlockPos ARMORY = new BlockPos(2, 1, 2);

    private static BlockEntity armory(GameTestHelper h, ServerPlayer owner) {
        h.setBlock(ARMORY, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armory")));
        BlockEntity armory = h.getBlockEntity(ARMORY);
        ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().setEnergy(500_000);
        Round33GameTests.call(armory, "tryBind", owner);
        return armory;
    }

    private static ItemStack remote(GameTestHelper h) {
        ItemStack remote = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "normal/item_armory_remote")));
        @SuppressWarnings("unchecked")
        DataComponentType<GlobalPos> target = (DataComponentType<GlobalPos>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(
            ResourceLocation.fromNamespaceAndPath("mio_icif", "armory_target"));
        remote.set(target, GlobalPos.of(h.getLevel().dimension(), h.absolutePos(ARMORY)));
        return remote;
    }

    private static Component eject(ServerPlayer owner) {
        return (Component) Round33GameTests.call(Round33GameTests.common("armory.WingmanService"), "eject", owner);
    }

    @SuppressWarnings("unchecked")
    private static List<Mob> wingmen(ServerPlayer owner) {
        return (List<Mob>) Round33GameTests.call(Round33GameTests.common("armory.WingmanService"), "of", owner);
    }

    /** The suit leaves the owner and stands up as an Escort Wingman of the Armory's first free suit position. */
    @GameTest(template = "empty", batch = "round11555_eject", timeoutTicks = 40)
    public static void wornSuitFliesOnAsAWingman(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        BlockEntity armory = armory(h, owner);
        owner.getInventory().setItem(8, remote(h));   // not slot 0: that is the main hand, which the sword takes
        owner.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        owner.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        owner.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TORCH, 16));
        Component result = eject(owner);
        List<Mob> list = wingmen(owner);
        h.assertTrue(list.size() == 1, "no Wingman after the eject: " + result.getString());
        Mob w = list.get(0);
        h.assertTrue(owner.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && owner.getMainHandItem().isEmpty(), "the suit is still on the owner");
        h.assertTrue(owner.getOffhandItem().is(Items.TORCH) && owner.getOffhandItem().getCount() == 16, "the torches left the hand");
        for (int i = 0; i < 400 && (Byte) Round33GameTests.call(w, "swapPhase") != 0; i++) Round33GameTests.call(w, "tickSwap", owner, true);
        h.assertTrue(w.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && w.getItemBySlot(EquipmentSlot.LEGS).is(Items.IRON_LEGGINGS)
            && w.getMainHandItem().is(Items.IRON_SWORD) && w.getOffhandItem().isEmpty(), "the Wingman did not stand up in the suit");
        h.assertTrue("ESCORT".equals(((Enum<?>) Round33GameTests.call(w, "mode")).name()) && (Boolean) Round33GameTests.call(w, "individual"),
            "not an individual Escort Wingman");
        h.assertTrue((Boolean) Round33GameTests.call(armory, "deployed", Round33GameTests.call(w, "set")), "the suit position is not reserved");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** Without a paired remote, or with one armour piece, nothing comes off. */
    @GameTest(template = "empty", batch = "round11555_refuse", timeoutTicks = 40)
    public static void ejectNeedsARemoteAndTwoPieces(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        armory(h, owner);
        owner.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        eject(owner);
        h.assertTrue(wingmen(owner).isEmpty() && owner.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "ejected without a remote");
        owner.getInventory().setItem(8, remote(h));
        owner.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        eject(owner);
        h.assertTrue(wingmen(owner).isEmpty() && owner.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "ejected one piece");
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
