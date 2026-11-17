package example.sicore;

import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.41 (Wingman audit W2, decision 1 C): the upkeep reserve. The Armory charges it at deployment (less when it has
 * less), the reserve pays before the Armory, the rest goes back when the Wingman comes home, it survives a save, and the
 * Wingman shows its flight time. The SI common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11541GameTests {
    private Round11541GameTests() {}

    private static final BlockPos ARMORY = new BlockPos(2, 2, 2);
    private static final String SERVICE = "armory.WingmanService";
    /** Default reserve: 160 EU per second for 10 minutes. */
    private static final long RESERVE = 160L * 60 * 10;

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static BlockEntity armory(GameTestHelper h, ServerPlayer owner, long energy) {
        h.setBlock(ARMORY, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armory")));
        BlockEntity armory = h.getBlockEntity(ARMORY);
        storage(armory).setEnergy(energy);
        call(armory, "tryBind", owner);
        int slot = (Integer) call(Round33GameTests.common("armory.mio_icif_armory"), "slotOf", 0, Round1102GameTests.constant("armory.ArmoryPiece", "HEAD"));
        call(call(armory, "getItemHandler"), "setStackInSlot", slot, new ItemStack(Items.IRON_HELMET));
        return armory;
    }

    private static com.miophas.singularity_iteration.core.runtime.energy.CustomEUEnergyStorage storage(BlockEntity armory) {
        return ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal();
    }

    @SuppressWarnings("unchecked")
    private static Entity deploy(GameTestHelper h, ServerPlayer owner, BlockEntity armory) {
        call(Round33GameTests.common(SERVICE), "deploy", owner, armory, 0, Round1102GameTests.constant("armory.WingmanMode", "ESCORT"));
        List<Entity> out = (List<Entity>) call(Round33GameTests.common(SERVICE), "of", owner);
        h.assertTrue(out.size() == 1, "no Wingman deployed");
        return out.get(0);
    }

    private static long reserve(Entity w) { return (Long) call(w, "reserve"); }

    /** Deployment charges the full reserve; the return gives back what was not flown. */
    @GameTest(template = "empty", batch = "round11541_reserve", timeoutTicks = 40)
    public static void reserveIsChargedAndRefunded(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        BlockEntity armory = armory(h, owner, 500_000);
        Entity w = deploy(h, owner, armory);
        long after = storage(armory).getAmount();
        h.assertTrue(reserve(w) == RESERVE, "reserve " + reserve(w));
        h.assertTrue((Integer) call(w, "enduranceSeconds") == RESERVE / 160, "endurance " + call(w, "enduranceSeconds"));
        call(w, "setReserve", 40_000L);   // as if part of it was flown
        call(w, "secure");
        w.discard();
        h.assertTrue(storage(armory).getAmount() == after + 40_000, "refund: " + (storage(armory).getAmount() - after));
        Round33GameTests.remove(owner);
        h.setBlock(ARMORY, Blocks.AIR);
        h.succeed();
    }

    /** An Armory with little energy charges what it has left after the deployment. */
    @GameTest(template = "empty", batch = "round11541_partial", timeoutTicks = 40)
    public static void reserveIsPartialWhenTheArmoryIsLow(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        BlockEntity armory = armory(h, owner, 6_000);   // one piece costs a few hundred EU here
        Entity w = deploy(h, owner, armory);
        h.assertTrue(storage(armory).getAmount() == 0 && reserve(w) > 0 && reserve(w) < 6_000, "reserve " + reserve(w));
        call(w, "secure");
        w.discard();
        Round33GameTests.remove(owner);
        h.setBlock(ARMORY, Blocks.AIR);
        h.succeed();
    }

    /**
     * The reserve pays before the (loaded) Armory; when it is empty the Armory pays and the flight time says so. The upkeep
     * runs directly (a Wingman left to fly may leave the ticking chunks of the test area and stop).
     */
    @GameTest(template = "empty", batch = "round11541_order", timeoutTicks = 40)
    public static void reservePaysBeforeTheArmory(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        BlockEntity armory = armory(h, owner, 500_000);
        Entity w = deploy(h, owner, armory);
        long start = storage(armory).getAmount();
        call(w, "upkeep", h.getLevel(), owner);
        h.assertTrue(storage(armory).getAmount() == start && reserve(w) == RESERVE - 160,
            "the Armory paid while there was a reserve: armory " + (start - storage(armory).getAmount()) + ", reserve " + reserve(w));
        call(w, "setReserve", 0L);
        call(w, "upkeep", h.getLevel(), owner);
        h.assertTrue(storage(armory).getAmount() == start - 160, "the Armory did not pay without a reserve: " + (start - storage(armory).getAmount()));
        h.assertTrue((Integer) call(w, "enduranceSeconds") == -2, "endurance " + call(w, "enduranceSeconds"));
        call(w, "secure");
        w.discard();
        Round33GameTests.remove(owner);
        h.setBlock(ARMORY, Blocks.AIR);
        h.succeed();
    }

    /** The reserve survives a save; a save without it (older version) has none. */
    @GameTest(template = "empty", batch = "round11541_save", timeoutTicks = 20)
    public static void reserveSurvivesSave(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = Round1102GameTests.wingman(h, owner, new net.minecraft.world.phys.Vec3(1.5, 4, 1.5));
        call(w, "setReserve", 12_345L);
        CompoundTag tag = w.saveWithoutId(new CompoundTag());
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wingman"));
        Entity copy = type.create(h.getLevel());
        copy.load(tag);
        h.assertTrue(reserve(copy) == 12_345L, "saved reserve " + reserve(copy));
        tag.remove("Reserve");
        Entity old = type.create(h.getLevel());
        old.load(tag);
        h.assertTrue(reserve(old) == 0, "an old save got a reserve");
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** The Japanese guide pages use one word for the Wingman and the Armory (0.1.11.40 missed a spelling). */
    @GameTest(template = "empty", batch = "round11541_guide", timeoutTicks = 20)
    public static void japaneseGuideUsesOneWord(GameTestHelper h) {
        for (String page : new String[]{"machines/producer_block_armory.md", "suit/wingman_command.md", "suit/quantum_actions.md"}) {
            String path = "/assets/mio_icif/guides/mio_icif/manual/_ja_jp/" + page;
            try (InputStream in = Round33GameTests.common("armory.WingmanEntity").getResourceAsStream(path)) {
                h.assertTrue(in != null, "no " + path);
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                for (String old : new String[]{"ウィングマン", "アーマリー", "アーモリー"}) h.assertTrue(!text.contains(old), page + " still says " + old);
            } catch (java.io.IOException e) {
                throw new AssertionError(path, e);
            }
        }
        h.succeed();
    }
}
