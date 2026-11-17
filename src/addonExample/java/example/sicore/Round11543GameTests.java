package example.sicore;

import java.lang.reflect.Field;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.43 (Wingman audit W4): the Fulton mode is gone (old saves load as Escort, a lift is over); the squad guards the
 * owner's place of death and flies home when the guard is over; the owner's hit calls the squad on hostile creatures only
 * (G-7); the escort stops hostile projectiles only (G-8). The SI common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round11543GameTests {
    private Round11543GameTests() {}

    private static Object call(Object target, String name, Object... args) { return Round33GameTests.call(target, name, args); }

    private static byte state(Entity w) { return (Byte) call(w, "state"); }

    /** Three modes; mode 3 (Fulton) and state 2 (lifting) of an older save load as Escort and active. */
    @GameTest(template = "empty", batch = "round11543_fulton", timeoutTicks = 20)
    public static void fultonIsGone(GameTestHelper h) {
        Class<?> modes = Round33GameTests.common("armory.WingmanMode");
        h.assertTrue(modes.getEnumConstants().length == 3, "modes: " + modes.getEnumConstants().length);
        h.assertTrue("ESCORT".equals(((Enum<?>) call(modes, "byId", 3)).name()), "byId(3)");
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = Round1102GameTests.wingman(h, owner, new Vec3(1.5, 4, 1.5));
        CompoundTag tag = w.saveWithoutId(new CompoundTag());
        tag.putByte("Mode", (byte) 3);
        tag.putByte("State", (byte) 2);
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wingman"));
        Entity old = type.create(h.getLevel());
        old.load(tag);
        h.assertTrue("ESCORT".equals(Round1102GameTests.mode(old)) && state(old) == 0, "old save: " + Round1102GameTests.mode(old) + " state " + state(old));
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** The owner dies: the Wingman holds the place of death; when the guard is over it flies home. */
    @GameTest(template = "empty", batch = "round11543_death", timeoutTicks = 60)
    public static void squadGuardsThePlaceOfDeath(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = Round1102GameTests.wingman(h, owner, new Vec3(1.5, 4, 1.5));
        call(w, "setReserve", 1_000_000L);
        Vec3 fell = owner.position();
        h.runAtTickTime(2, () -> owner.hurt(h.getLevel().damageSources().genericKill(), 1000));
        h.runAtTickTime(10, () -> {
            h.assertTrue(!owner.isAlive(), "the owner did not die");
            h.assertTrue(state(w) == 0 && (Boolean) call(w, "deathGuard") && "HOLD".equals(Round1102GameTests.order(w))
                && fell.distanceTo((Vec3) call(w, "orderPos")) < 0.5, "no guard: state " + state(w) + " order " + Round1102GameTests.order(w));
            setLong(w, "guardUntil", 0);
        });
        h.runAtTickTime(14, () -> {
            h.assertTrue(state(w) == 1 || w.isRemoved(), "still guarding after the time: state " + state(w));
            w.discard();
            Round33GameTests.remove(owner);
            h.succeed();
        });
    }

    /** G-7: the owner's hit makes a zombie a called target, a villager not (unless it fights the owner). */
    @GameTest(template = "empty", batch = "round11543_call", timeoutTicks = 20)
    public static void ownerCallsOnHostilesOnly(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        Entity w = Round1102GameTests.wingman(h, owner, new Vec3(1.5, 4, 1.5));
        Mob villager = mob(h, EntityType.VILLAGER, new Vec3(3.5, 2, 3.5));
        Mob zombie = mob(h, EntityType.ZOMBIE, new Vec3(3.5, 2, 1.5));
        Class<?> tactics = Round33GameTests.common("armory.WingmanTactics");
        owner.setLastHurtMob(villager);
        double vScore = (Double) call(tactics, "threat", w, owner, villager, null);
        owner.setLastHurtMob(zombie);
        double zScore = (Double) call(tactics, "threat", w, owner, zombie, null);
        h.assertTrue(vScore == Double.NEGATIVE_INFINITY, "a villager the owner hit is a target: " + vScore);
        h.assertTrue(zScore > 100, "a zombie the owner hit is no call: " + zScore);
        owner.setLastHurtByMob(villager);   // it fights the owner: now it is a target
        h.assertTrue((Double) call(tactics, "threat", w, owner, villager, null) > Double.NEGATIVE_INFINITY, "an attacking villager is ignored");
        villager.discard();
        zombie.discard();
        w.discard();
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** G-8: an arrow of a skeleton or of a player of no team is stopped; a pearl, a potion and a neutral mob's arrow pass. */
    @GameTest(template = "empty", batch = "round11543_escort", timeoutTicks = 20)
    public static void escortStopsHostileProjectilesOnly(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        ServerPlayer other = Round33GameTests.survivalPlayer(h);
        Mob skeleton = mob(h, EntityType.SKELETON, new Vec3(3.5, 2, 3.5));
        Mob villager = mob(h, EntityType.VILLAGER, new Vec3(0.5, 2, 3.5));
        boolean skel = incoming(h, owner, new Arrow(h.getLevel(), skeleton, new ItemStack(Items.ARROW), null));
        boolean pvp = incoming(h, owner, new Arrow(h.getLevel(), other, new ItemStack(Items.ARROW), null));
        boolean neutral = incoming(h, owner, new Arrow(h.getLevel(), villager, new ItemStack(Items.ARROW), null));
        boolean pearl = incoming(h, owner, new ThrownEnderpearl(h.getLevel(), other));
        boolean potion = incoming(h, owner, new ThrownPotion(h.getLevel(), skeleton));
        h.assertTrue(skel && pvp, "hostile not stopped: skeleton " + skel + ", player " + pvp);
        h.assertTrue(!neutral && !pearl && !potion, "stopped: neutral " + neutral + ", pearl " + pearl + ", potion " + potion);
        skeleton.discard();
        villager.discard();
        Round33GameTests.remove(other);
        Round33GameTests.remove(owner);
        h.succeed();
    }

    /** {@code p} flying straight at the owner from 5 blocks away (not added to the level). */
    private static boolean incoming(GameTestHelper h, ServerPlayer owner, Projectile p) {
        Vec3 c = owner.getBoundingBox().getCenter();
        p.setPos(c.x + 5, c.y, c.z);
        p.setDeltaMovement(-1, 0, 0);
        return (Boolean) call(Round33GameTests.common("armory.WingmanEntity"), "incoming", p, owner);
    }

    private static Mob mob(GameTestHelper h, EntityType<? extends Mob> type, Vec3 rel) {
        Mob m = type.create(h.getLevel());
        m.moveTo(h.absoluteVec(rel));
        m.setNoAi(true);
        h.getLevel().addFreshEntity(m);
        return m;
    }

    private static void setLong(Object target, String name, long value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.setLong(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(name, e);
        }
    }
}
