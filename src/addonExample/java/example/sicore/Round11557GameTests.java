package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.57 (gaze and Iron Man phase P5, decisions A / A): one piece called on its own. From the Armory only that piece
 * leaves its suit; without it in the Armory the nearest Wingman hands over that one piece (the rest of its suit stays);
 * with neither, or a bound piece worn there, the call is refused.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11557GameTests {
    private Round11557GameTests() {}

    private static final BlockPos ARMORY = new BlockPos(2, 1, 2);
    private static final int HEAD = 3;   // ArmoryPiece.HEAD.ordinal()

    private static Component call(ServerPlayer owner, Object armory) {
        Class<?> type = Round33GameTests.common("armory.ArmoryPieceCall");
        try {
            var m = type.getDeclaredMethod("call", ServerPlayer.class, Round33GameTests.common("armory.mio_icif_armory"), int.class);
            m.setAccessible(true);
            return (Component) m.invoke(null, owner, armory, HEAD);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ArmoryPieceCall.call", e.getCause() != null ? e.getCause() : e);
        }
    }

    private static String key(Component c) { return c.getContents() instanceof TranslatableContents t ? t.getKey() : c.getString(); }

    private static Object piece(String name) {
        for (Object p : Round33GameTests.common("armory.ArmoryPiece").getEnumConstants()) if (((Enum<?>) p).name().equals(name)) return p;
        throw new AssertionError(name);
    }

    /** From the Armory: the helmet leaves suit 0, the chestplate stays. */
    @GameTest(template = "empty", batch = "round11557_armory", timeoutTicks = 40)
    public static void armorySendsOnlyThatPiece(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        h.setBlock(ARMORY, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armory")));
        BlockEntity armory = h.getBlockEntity(ARMORY);
        ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().setEnergy(500_000);
        Round33GameTests.call(armory, "tryBind", owner);
        Object inv = Round33GameTests.call(armory, "getItemHandler");
        Class<?> armoryType = Round33GameTests.common("armory.mio_icif_armory");
        int headSlot = (Integer) Round33GameTests.call(armoryType, "slotOf", 0, piece("HEAD"));
        int chestSlot = (Integer) Round33GameTests.call(armoryType, "slotOf", 0, piece("CHEST"));
        Round33GameTests.call(inv, "insertItem", headSlot, new ItemStack(Items.DIAMOND_HELMET), false);
        Round33GameTests.call(inv, "insertItem", chestSlot, new ItemStack(Items.DIAMOND_CHESTPLATE), false);
        Component result = call(owner, armory);
        h.assertTrue("message.mio_icif.armory.piece_called".equals(key(result)), "not called from the Armory: " + result.getString());
        h.assertTrue(((ItemStack) Round33GameTests.call(armory, "piece", 0, piece("HEAD"))).isEmpty(), "the helmet is still in the Armory");
        h.assertTrue(((ItemStack) Round33GameTests.call(armory, "piece", 0, piece("CHEST"))).is(Items.DIAMOND_CHESTPLATE), "the chestplate left too");
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** Without the piece in an Armory the nearest Wingman hands over that one piece; the rest of its suit stays. */
    @GameTest(template = "empty", batch = "round11557_wingman", timeoutTicks = 40)
    public static void wingmanHandsOverOnePiece(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        owner.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        owner.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 2, 3.5));
        Vec3 place = (Vec3) Round33GameTests.call(Round33GameTests.common("armory.WingmanEntity"), "swapPlace", owner);
        w.moveTo(place.x, place.y, place.z);
        w.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        w.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        w.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        Component result = call(owner, null);
        h.assertTrue("message.mio_icif.wingman.piece_handover".equals(key(result)), "no handover: " + result.getString());
        for (int i = 0; i < 400 && (Byte) Round33GameTests.call(w, "swapPhase") != 0; i++) Round33GameTests.call(w, "tickSwap", owner, true);
        h.assertTrue(owner.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET) && w.getItemBySlot(EquipmentSlot.HEAD).is(Items.LEATHER_HELMET),
            "the helmets did not change places");
        h.assertTrue(owner.getItemBySlot(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE) && w.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE),
            "more than the helmet was swapped");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** Nothing to call, or a bound piece worn there: refused. */
    @GameTest(template = "empty", batch = "round11557_refuse", timeoutTicks = 20)
    public static void callIsRefusedWithoutAPiece(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        h.assertTrue("message.mio_icif.armory.piece_none".equals(key(call(owner, null))), "no piece anywhere is not refused");
        ItemStack bound = new ItemStack(Items.IRON_HELMET);
        bound.enchant(h.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
            .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE), 1);
        owner.setItemSlot(EquipmentSlot.HEAD, bound);
        h.assertTrue("message.mio_icif.armory.piece_bound".equals(key(call(owner, null))), "a bound helmet is not refused");
        Round33GameTests.remove(owner);
        h.succeed();
    }
}
