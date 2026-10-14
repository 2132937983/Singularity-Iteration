package example.sicore;

import com.miophas.singularity_iteration.core.api.suit.SuitActionSpec;
import com.miophas.singularity_iteration.core.api.suit.SuitQuantumActions;
import com.miophas.singularity_iteration.core.api.suit.SuitQuantumCore;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricArmor;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.0 (quantum actions, phase Q1): the quantum bay expander and the unchanged
 * slot count of old pieces, the Mk.II gate of the deflector and predictor actions, coherence and
 * decoherence, the blink wall rule, the phase shift damage rule, and the public action API.
 * The SI common classes are reached by reflection (the addon module may only use the core API).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1100GameTests {
    private static final String MODULES = "suit.SuitModules", CORE = "suit.quantum.QuantumCore", ACTIONS = "suit.quantum.QuantumActions";

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object type(String name) {
        return Enum.valueOf((Class) Round33GameTests.common("suit.SuitModuleType"), name);
    }

    static Object unitRef(String name) {
        return Round33GameTests.call(Round33GameTests.common("suit.UnitRef"), "of", type(name));
    }

    static boolean install(ItemStack piece, String unit, int tier) {
        return (Boolean) Round33GameTests.call(Round33GameTests.common(MODULES), "install", piece, unitRef(unit), tier);
    }

    static int capacity(ItemStack piece) {
        return (Integer) Round33GameTests.call(Round33GameTests.common(MODULES), "capacity", piece);
    }

    static ItemStack charged(String path) {
        ItemStack stack = Round33GameTests.item(path);
        if (stack.getItem() instanceof AbstractElectricArmor armor) armor.setEnergy(stack, armor.getMaxEnergy(stack));
        return stack;
    }

    static int intField(Object target, String name) {
        try {
            return target.getClass().getField(name).getInt(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    static void setInt(Object target, String name, int value) {
        try {
            target.getClass().getField(name).setInt(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }

    /** An old helmet keeps four slots; one expander makes five; a second one and a nano piece are refused. */
    @GameTest(template = "empty", batch = "round1100_bays")
    public static void bayExpanderAddsOneSlot(GameTestHelper h) {
        ItemStack helmet = Round33GameTests.item("armor/item_armor_quantum_helmet");
        h.assertTrue(capacity(helmet) == 4, "a helmet without bay has " + capacity(helmet) + " slots");
        String[] units = {"ENTITY_ESP", "BALLISTIC", "BEHAVIOR_PREDICTOR", "HOLOMAP"};
        for (String u : units) h.assertTrue(install(helmet, u, 1), "install " + u);
        h.assertTrue(!install(helmet, "SMART_GUIDANCE", 1), "a fifth unit fits without a bay");
        h.assertTrue((Boolean) Round33GameTests.call(Round33GameTests.common(MODULES), "expand", helmet), "the expander was refused");
        h.assertTrue(capacity(helmet) == 5, "a helmet with a bay has " + capacity(helmet) + " slots");
        h.assertTrue(install(helmet, "SMART_GUIDANCE", 1), "the fifth unit does not fit the bay");
        h.assertTrue(!(Boolean) Round33GameTests.call(Round33GameTests.common(MODULES), "expand", helmet), "a second bay was accepted");
        ItemStack nano = Round33GameTests.item("armor/item_armor_nano_helmet");
        h.assertTrue(!(Boolean) Round33GameTests.call(Round33GameTests.common(MODULES), "expand", nano), "a nano piece took a quantum bay");
        h.succeed();
    }

    /** A Mk.I deflector offers no action key; the Mk.II does. The phase modulator does from Mk.I. */
    @GameTest(template = "empty", batch = "round1100_actions", timeoutTicks = 20)
    public static void deflectorActionNeedsMk2(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack chest = charged("armor/item_armor_quantum_chestplate");
        install(chest, "DEFLECTOR", 1);
        install(chest, "PHASE_MODULATOR", 1);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        List<?> mk1 = (List<?>) Round33GameTests.call(Round33GameTests.common(ACTIONS), "available", p);
        h.assertTrue(keys(mk1).equals(List.of("phase_modulator")), "Mk.I actions " + keys(mk1));
        install(chest, "DEFLECTOR", 2);                                  // in-place upgrade to Mk.II
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        List<?> mk2 = (List<?>) Round33GameTests.call(Round33GameTests.common(ACTIONS), "available", p);
        h.assertTrue(keys(mk2).equals(List.of("phase_modulator", "deflector")), "Mk.II actions " + keys(mk2));
        h.succeed();
    }

    private static List<String> keys(List<?> units) {
        return units.stream().map(u -> (String) Round33GameTests.call(u, "key")).toList();
    }

    /** Spending all coherence decoheres the core; the public view reports it. */
    @GameTest(template = "empty", batch = "round1100_actions", timeoutTicks = 20)
    public static void coherenceRunsOutAndDecoheres(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        Object state = Round33GameTests.call(Round33GameTests.common(CORE), "state", p);
        h.assertTrue(SuitQuantumCore.coherence(p) == 100F, "a new core is not full: " + SuitQuantumCore.coherence(p));
        Round33GameTests.call(Round33GameTests.common(CORE), "spend", p, state, 40F);
        h.assertTrue(Math.abs(SuitQuantumCore.coherence(p) - 60F) < 0.01F && !SuitQuantumCore.decoherent(p), "after 40: " + SuitQuantumCore.coherence(p));
        Round33GameTests.call(Round33GameTests.common(CORE), "spend", p, state, 60F);
        h.assertTrue(SuitQuantumCore.coherence(p) == 0F && SuitQuantumCore.decoherent(p) && intField(state, "decoherentTicks") > 0,
            "no decoherence at 0");
        h.succeed();
    }

    /** A blink toward a wall lands in front of it, never behind it. */
    @GameTest(template = "empty", batch = "round1100_actions", timeoutTicks = 40)
    public static void blinkNeverPassesWalls(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        BlockPos origin = h.absolutePos(new BlockPos(1, 2, 1));
        // a stone floor and a wall 3 blocks east, open space behind it
        for (int x = -1; x <= 9; x++) for (int z = -2; z <= 2; z++) h.getLevel().setBlock(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
        for (int x = -1; x <= 9; x++) for (int y = 0; y <= 3; y++) for (int z = -2; z <= 2; z++)
            h.getLevel().setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        int wallX = origin.getX() + 3;
        for (int y = 0; y <= 3; y++) for (int z = -2; z <= 2; z++) h.getLevel().setBlock(new BlockPos(wallX, origin.getY() + y, origin.getZ() + z), Blocks.STONE.defaultBlockState(), 3);
        p.teleportTo(h.getLevel(), origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, -90F, 0F);     // facing east
        ItemStack legs = charged("armor/item_armor_quantum_leggings");
        install(legs, "PHASE_JUMP_DRIVE", 1);
        p.setItemSlot(EquipmentSlot.LEGS, legs);
        Object state = Round33GameTests.call(Round33GameTests.common(CORE), "state", p);
        double before = p.getX();
        boolean jumped = (Boolean) Round33GameTests.call(Round33GameTests.common(ACTIONS), "blink", p, state, 1);
        h.assertTrue(jumped, "the blink found no landing in front of the wall");
        h.assertTrue(p.getX() > before + 0.5 && p.getX() + p.getBbWidth() / 2 <= wallX + 0.001, "landed at x=" + p.getX() + " (wall at " + wallX + ")");
        h.succeed();
    }

    /** Out of phase: a creature hit does nothing, the void still hurts. */
    @GameTest(template = "empty", batch = "round1100_actions", timeoutTicks = 20)
    public static void phaseShiftPassesCreatureDamage(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        Zombie zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(2.5, 2, 2.5));
        Object state = Round33GameTests.call(Round33GameTests.common(CORE), "state", p);
        setInt(state, "phaseTicks", 20);
        float health = p.getHealth();
        p.hurt(p.damageSources().mobAttack(zombie), 4F);
        h.assertTrue(p.getHealth() == health, "a creature hit passed the phase shift: " + p.getHealth());
        p.invulnerableTime = 0;
        p.hurt(p.damageSources().fellOutOfWorld(), 2F);
        h.assertTrue(p.getHealth() < health, "the void did not hurt during the phase shift");
        h.succeed();
    }

    /** The action API: addon ids register and resolve; a vanilla namespace is refused. */
    @GameTest(template = "empty", batch = "round1100_api")
    public static void actionApiRegisters(GameTestHelper h) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "test_action_unit");
        SuitQuantumActions.register(id, SuitActionSpec.single(10, 40), (player, piece, input) -> true);
        h.assertTrue(SuitQuantumActions.get(id.toString()) != null && SuitQuantumActions.get(id.toString()).spec().coherenceCost() == 10,
            "the registered action is missing");
        boolean refused = false;
        try {
            SuitQuantumActions.register(ResourceLocation.withDefaultNamespace("bad"), SuitActionSpec.single(1, 1), (player, piece, input) -> true);
        } catch (IllegalArgumentException e) {
            refused = true;
        }
        h.assertTrue(refused, "a minecraft: action id was accepted");
        h.succeed();
    }
}
