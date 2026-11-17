package example.sicore;

import com.miophas.singularity_iteration.core.api.item.IWeaponItem;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.42 (Wingman audit W3, decision 2 C): a Wingman wears its gear as a player does, never wears a piece out, flies
 * home for repairs below 10 %, a worn suit cannot deploy, and an energy weapon out of charge makes it fight in melee.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11542GameTests {
    private Round11542GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static byte state(Entity w) { return (Byte) call(w, "state"); }

    /** A Wingman of {@code owner} with an upkeep reserve (no Armory there), wearing {@code chest} and holding {@code hand}. */
    private static Mob wingman(GameTestHelper h, ServerPlayer owner, ItemStack chest, ItemStack hand) {
        Mob w = (Mob) Round1102GameTests.wingman(h, owner, new Vec3(1.5, 4, 1.5));
        call(w, "setReserve", 10_000_000L);
        w.setItemSlot(EquipmentSlot.CHEST, chest);
        w.setItemSlot(EquipmentSlot.MAINHAND, hand);
        return w;
    }

    private static ItemStack damaged(net.minecraft.world.item.Item item, int left) {
        ItemStack s = new ItemStack(item);
        s.setDamageValue(s.getMaxDamage() - left);
        return s;
    }

    /** A hit wears the armor by a quarter of the damage; a melee hit wears the sword by 1. */
    @GameTest(template = "empty", batch = "round11542_wear", timeoutTicks = 20)
    public static void hitsWearArmorAndWeapon(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Mob w = wingman(h, owner, new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_SWORD));
        Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.moveTo(w.getX() + 1, w.getY(), w.getZ());
        zombie.setNoAi(true);
        h.getLevel().addFreshEntity(zombie);
        w.hurt(h.getLevel().damageSources().mobAttack(zombie), 8);   // a mob hit: the generic damage type bypasses armor
        h.assertTrue(w.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 2, "chestplate damage " + w.getItemBySlot(EquipmentSlot.CHEST).getDamageValue());
        h.assertTrue(w.doHurtTarget(zombie) && w.getMainHandItem().getDamageValue() == 1, "sword damage " + w.getMainHandItem().getDamageValue());
        h.assertTrue(state(w) == 0, "returned with fresh gear");
        zombie.discard();
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** Below 10 % the Wingman flies home; the last point of durability is never taken. */
    @GameTest(template = "empty", batch = "round11542_worn", timeoutTicks = 20)
    public static void wornGearSendsHomeAndNeverBreaks(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        int max = new ItemStack(Items.IRON_CHESTPLATE).getMaxDamage();   // 240: 10 % = 24
        Mob w = wingman(h, owner, damaged(Items.IRON_CHESTPLATE, 26), ItemStack.EMPTY);
        Zombie hitter = EntityType.ZOMBIE.create(h.getLevel());
        hitter.setNoAi(true);
        hitter.moveTo(w.getX() + 1, w.getY(), w.getZ());
        h.getLevel().addFreshEntity(hitter);
        w.hurt(h.getLevel().damageSources().mobAttack(hitter), 12);      // wears 3: 23 left
        h.assertTrue(state(w) == 1 && w.getHealth() > 1, "state " + state(w) + " health " + w.getHealth() + " left "
            + (max - w.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()));
        Mob x = wingman(h, owner, damaged(Items.IRON_CHESTPLATE, 2), ItemStack.EMPTY);
        x.hurt(h.getLevel().damageSources().mobAttack(hitter), 15);
        ItemStack chest = x.getItemBySlot(EquipmentSlot.CHEST);
        h.assertTrue(!chest.isEmpty() && chest.getMaxDamage() - chest.getDamageValue() == 1, "the chestplate broke or lost its last point");
        w.discard();
        x.discard();
        hitter.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** A suit with a piece below 10 % does not deploy. */
    @GameTest(template = "empty", batch = "round11542_deploy", timeoutTicks = 20)
    public static void wornSuitDoesNotDeploy(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_armory")));
        BlockEntity armory = h.getBlockEntity(at);
        ((AbstractEnergyBlockEntity) armory).getEnergyStorageInternal().setEnergy(500_000);
        call(armory, "tryBind", owner);
        int slot = (Integer) call(Round33GameTests.common("armory.mio_icif_armory"), "slotOf", 0, Round1102GameTests.constant("armory.ArmoryPiece", "CHEST"));
        call(call(armory, "getItemHandler"), "setStackInSlot", slot, damaged(Items.IRON_CHESTPLATE, 5));
        Object msg = call(Round33GameTests.common("armory.WingmanService"), "deploy", owner, armory, 0, Round1102GameTests.constant("armory.WingmanMode", "ESCORT"));
        String key = ((Component) msg).getContents() instanceof TranslatableContents t ? t.getKey() : "";
        h.assertTrue("message.mio_icif.wingman.needs_repair".equals(key), "deploy answered " + key);
        Round33GameTests.remove(owner);
        h.setBlock(at, Blocks.AIR);
        h.succeed();
    }

    /**
     * An energy weapon out of charge: the Wingman switches to melee for a while instead of hovering at its station. The
     * ranged attack runs directly (a Wingman left to fly takes a station outside the ticking chunks of the test area).
     */
    @GameTest(template = "empty", batch = "round11542_dry", timeoutTicks = 20)
    public static void emptyEnergyWeaponFightsInMelee(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        owner.moveTo(h.absoluteVec(new Vec3(4.5, 2, 0.5)));
        ItemStack gun = Round33GameTests.item("item_tool_laser_miner");
        ((IWeaponItem) gun.getItem()).setEnergy(gun, 0);
        Mob w = wingman(h, owner, ItemStack.EMPTY, gun);
        w.moveTo(h.absoluteVec(new Vec3(1.5, 3, 1.5)));
        Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.moveTo(h.absoluteVec(new Vec3(3.5, 2, 3.5)));
        zombie.setNoAi(true);
        h.getLevel().addFreshEntity(zombie);
        for (int i = 0; i < 12 && intField(w, "dryUntil") < 0; i++) call(w, "engageRanged", zombie, owner);   // turns, then tries to fire
        h.assertTrue(intField(w, "dryUntil") > w.tickCount, "never went dry (dryUntil " + intField(w, "dryUntil") + ")");
        zombie.discard();
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    private static int intField(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }
}
