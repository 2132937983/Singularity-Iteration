package example.sicore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.220 (Wingman cave overhaul G0): cave scenes that reproduce how a Wingman fights underground, and the baseline
 * numbers of the current flight. Each scene is a stone block inside a barrier shell (the {@code cave} template, 44 x 18 x 11,
 * so the Wingman cannot leave the loaded test area), with caves carved into it, an owner, a Wingman with an iron sword and
 * still zombies (no AI, 6 health; a "hunter" targets the owner).
 *
 * <p>Every tick of {@link #TICKS} the test records: ticks with the head against a ceiling, ticks stuck (a target, slower than
 * 0.05 blocks per tick and farther than 2.5 blocks from it), ticks the avoidance pushed it up ({@code WingmanNavDebug.Probe}),
 * ticks on a wrong target (one behind rock while a reachable one is there), the tick of the first hit, kills, and the mean
 * air below its feet. The numbers go to the log and to {@code wingman_nav_baseline.txt} in the run folder.
 *
 * <p>G0 only recorded (the scenes must run, the Wingman must stay in its scene); each later batch adds the limits it
 * reaches to the scene ({@link Run#limits}). 0.1.11.221 (G1): no scene keeps the head against the ceiling for more than a
 * tenth of the run (the tunnel a fifth: its target is above the 2-high spot through the rock, and without a path the
 * Wingman lingered there until G2), and in the cavern and in the open the first hit comes within 80 ticks. 0.1.11.222 (G2):
 * the tunnel is back to a tenth, and the Wingman flies up the shaft and hits the hunter within 150 ticks. The WingmanEntity is reached by
 * reflection (the addon module may only use the core API).
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round111220GameTests {
    private Round111220GameTests() {}

    static final int TICKS = 300;
    static final int X = 44, Y = 18, Z = 11;

    // ---- building

    static void fill(GameTestHelper h, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) h.setBlock(new BlockPos(x, y, z), state);
    }

    static void carve(GameTestHelper h, int x0, int y0, int z0, int x1, int y1, int z1) {
        fill(h, x0, y0, z0, x1, y1, z1, Blocks.AIR.defaultBlockState());
    }

    /** A barrier shell round the template; inside it stone ({@code solid}) or a stone floor with air above. */
    static void shell(GameTestHelper h, boolean solid) {
        BlockState barrier = Blocks.BARRIER.defaultBlockState(), stone = Blocks.STONE.defaultBlockState();
        fill(h, 0, 0, 0, X - 1, 0, Z - 1, barrier);
        fill(h, 0, Y - 1, 0, X - 1, Y - 1, Z - 1, barrier);
        fill(h, 0, 1, 0, 0, Y - 2, Z - 1, barrier);
        fill(h, X - 1, 1, 0, X - 1, Y - 2, Z - 1, barrier);
        fill(h, 1, 1, 0, X - 2, Y - 2, 0, barrier);
        fill(h, 1, 1, Z - 1, X - 2, Y - 2, Z - 1, barrier);
        if (solid) fill(h, 1, 1, 1, X - 2, Y - 2, Z - 2, stone);
        else fill(h, 1, 1, 1, X - 2, 1, Z - 2, stone);
    }

    /** A still husk (a zombie that does not burn in daylight; no AI) standing at {@code (x, y, z)}; it hunts {@code prey} when given. */
    static Zombie zombie(GameTestHelper h, double x, double y, double z, ServerPlayer prey) {
        ServerLevel level = h.getLevel();
        Zombie mob = EntityType.HUSK.create(level);
        if (mob == null) throw new AssertionError("no zombie");
        Vec3 at = h.absoluteVec(new Vec3(x, y, z));
        mob.moveTo(at.x, at.y, at.z, 90, 0);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.setHealth(6);
        level.addFreshEntity(mob);
        if (prey != null) mob.setTarget(prey);
        return mob;
    }

    // ---- the run

    /** A scene: what it holds and how it is scored. */
    static final class Run {
        final String scene;
        final ServerPlayer owner;
        final Mob wingman;
        final List<Zombie> enemies = new ArrayList<>();
        /** Targets that count as wrong while a right one is alive (behind rock). */
        final List<Zombie> wrong = new ArrayList<>();
        int ticks, ceiling, stuck, upward, wrongTicks, firstHit = -1, airSamples;
        double airBelow;
        final float[] health = new float[8];
        final int[] hurtAt = {-1, -1, -1, -1, -1, -1, -1, -1};
        final String[] cause = new String[8];

        final BlockPos origin;

        Run(GameTestHelper h, String scene, Vec3 owner, Vec3 wingman) {
            this.scene = scene;
            this.origin = h.absolutePos(BlockPos.ZERO);
            this.owner = Round33GameTests.survivalPlayer(h);
            this.owner.moveTo(h.absoluteVec(owner));
            this.owner.getAbilities().invulnerable = true;
            this.wingman = (Mob) Round1102GameTests.wingman(h, this.owner, wingman);
            this.wingman.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            Round33GameTests.call(this.wingman, "setReserve", 1_000_000L);
        }

        Zombie add(Zombie z) { health[enemies.size()] = z.getHealth(); enemies.add(z); return z; }

        void tick(GameTestHelper h) {
            if (ticks >= TICKS) return;
            ticks++;
            ServerLevel level = h.getLevel();
            AABB box = wingman.getBoundingBox();
            if (!level.noCollision(wingman, box.move(0, 0.06, 0))) ceiling++;
            Object probe = Round33GameTests.call(wingman, "navProbe");
            if ((Boolean) Round33GameTests.call(probe, "upward")) upward++;
            LivingEntity target = (LivingEntity) Round33GameTests.call(wingman, "currentTarget");
            if (target != null && target.isAlive() && wingman.getDeltaMovement().length() < 0.05 && wingman.distanceTo(target) > 2.5) stuck++;
            if (target != null && wrong.contains(target) && enemies.stream().anyMatch(e -> e.isAlive() && !wrong.contains(e))) wrongTicks++;
            for (int i = 0; i < enemies.size(); i++) {
                Zombie e = enemies.get(i);
                if (hurtAt[i] < 0 && (!e.isAlive() || e.getHealth() < health[i])) {
                    hurtAt[i] = ticks;
                    DamageSource src = e.getLastDamageSource();
                    cause[i] = (src == null ? (e.isRemoved() ? "removed:" + e.getRemovalReason() : "?") : src.getMsgId()
                        + (src.getEntity() == null ? "" : "/" + src.getEntity().getType().toShortString()));
                    if (firstHit < 0) firstHit = ticks;
                }
            }
            BlockPos feet = wingman.blockPosition();
            int below = 0;
            while (below < 16 && level.getBlockState(feet.below(below + 1)).getCollisionShape(level, feet.below(below + 1)).isEmpty()) below++;
            airBelow += below + (wingman.getY() - feet.getY());
            airSamples++;
        }

        int kills() { return (int) enemies.stream().filter(e -> !e.isAlive()).count(); }

        String report() {
            StringBuilder hits = new StringBuilder();
            for (int i = 0; i < enemies.size(); i++) hits.append(i == 0 ? "" : ",").append(hurtAt[i]).append(hurtAt[i] < 0 ? "" : ":" + cause[i]);
            Vec3 at = wingman.position().subtract(Vec3.atLowerCornerOf(origin));
            return String.format("scene=%s ticks=%d ceiling=%d stuck=%d upward=%d wrongTarget=%d firstHit=%d kills=%d/%d airBelow=%.2f hits=[%s] end=(%.1f,%.1f,%.1f) nav=%s",
                scene, ticks, ceiling, stuck, upward, wrongTicks, firstHit, kills(), enemies.size(), airSamples == 0 ? 0 : airBelow / airSamples, hits,
                at.x, at.y, at.z, navCounts());
        }

        /** Searches started / found / failed by the Wingman's navigator (0.1.11.222). */
        String navCounts() {
            try {
                Object nav = Round33GameTests.call(wingman, "nav");
                StringBuilder b = new StringBuilder();
                for (String f : new String[]{"searches", "found", "failed"}) {
                    java.lang.reflect.Field field = nav.getClass().getDeclaredField(f);
                    field.setAccessible(true);
                    b.append(b.length() == 0 ? "" : "/").append(field.getInt(nav));
                }
                return b.toString();
            } catch (ReflectiveOperationException | AssertionError e) {
                return "-";
            }
        }

        /** The limits of the scene, checked after the run (G0: none). */
        java.util.function.Consumer<Run> limits = r -> { };

        /** The report taken at the end of the run, before the scene is cleared. */
        String last = "";

        void limit(boolean ok, String what) {
            if (!ok) throw new net.minecraft.gametest.framework.GameTestAssertException(scene + ": " + what + " (" + last + ")");
        }

        void finish(GameTestHelper h) {
            last = report();
            String line = "[wingman-nav] " + last;
            System.out.println(line);
            try {
                Files.writeString(Path.of("wingman_nav_baseline.txt"), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ignored) {
                // the log line is enough
            }
            AABB inside = new AABB(h.absolutePos(BlockPos.ZERO).getCenter(), h.absolutePos(new BlockPos(X - 1, Y - 1, Z - 1)).getCenter());
            boolean home = inside.inflate(0.5).contains(wingman.position());
            for (Entity e : enemies) e.discard();
            wingman.discard();
            Round33GameTests.remove(owner);
            h.assertTrue(ticks == TICKS, scene + ": ran " + ticks + " of " + TICKS + " ticks");
            h.assertTrue(home, scene + ": the Wingman left its scene (at " + wingman.position() + ")");
            limits.accept(this);
            h.succeed();
        }
    }

    /** 0.1.11.221 (G1): the head against the ceiling at most a tenth of the run. */
    static void notOnTheCeiling(Run r) { underTheCeiling(r, TICKS / 10); }

    static void underTheCeiling(Run r, int most) { r.limit(r.ceiling <= most, "the head is against the ceiling for " + r.ceiling + " ticks"); }

    /** 0.1.11.221 (G1): the open scenes keep their old pace: the first hit within 80 ticks. */
    static void hitsSoon(Run r) { r.limit(r.firstHit > 0 && r.firstHit <= 80, "the first hit came at tick " + r.firstHit); }

    static void play(GameTestHelper h, Run run) {
        h.onEachTick(() -> run.tick(h));
        h.runAtTickTime(TICKS + 1, () -> run.finish(h));
    }

    static final Vec3 OWNER = new Vec3(3.5, 2, 5.5), WINGMAN = new Vec3(6.5, 2.2, 5.5);

    // ---- the scenes

    /**
     * A 3-high tunnel with a lower lintel; a step up, a shaft and an upper tunnel that runs back over it. The hunter stands in
     * the upper tunnel, 8 blocks above the lower one through rock; the way there is the shaft (about 50 blocks).
     */
    @GameTest(template = "cave", batch = "round111220_tunnel", timeoutTicks = TICKS + 40)
    public static void tunnelAndShaft(GameTestHelper h) {
        shell(h, true);
        carve(h, 2, 2, 4, 28, 4, 6);
        carve(h, 29, 3, 4, 36, 5, 6);
        carve(h, 33, 3, 4, 35, 12, 6);
        carve(h, 14, 10, 4, 35, 12, 6);
        fill(h, 15, 4, 4, 15, 4, 6, Blocks.STONE.defaultBlockState());
        Run run = new Run(h, "tunnel", OWNER, WINGMAN);
        run.limits = r -> { notOnTheCeiling(r); r.limit(r.firstHit > 0 && r.firstHit <= 150, "the hunter up the shaft is hit at tick " + r.firstHit); };   // 0.1.11.222 (G2): up the shaft
        run.add(zombie(h, 18.5, 10, 5.5, run.owner));
        play(h, run);
    }

    /**
     * Two tunnels, one over the other, joined only by a shaft at the far end. A zombie in the upper tunnel is 9 blocks from the
     * owner through rock (wrong: about 75 blocks of flight); another stands in the owner's tunnel 23 blocks away.
     */
    @GameTest(template = "cave", batch = "round111220_layers", timeoutTicks = TICKS + 40)
    public static void twoLayers(GameTestHelper h) {
        shell(h, true);
        carve(h, 2, 2, 4, 41, 4, 6);
        carve(h, 3, 9, 4, 41, 11, 6);
        carve(h, 40, 5, 4, 41, 8, 6);
        Run run = new Run(h, "layers", OWNER, WINGMAN);
        run.limits = Round111220GameTests::notOnTheCeiling;
        run.wrong.add(run.add(zombie(h, 9.5, 9, 5.5, null)));
        run.add(zombie(h, 26.5, 2, 5.5, null));
        play(h, run);
    }

    /**
     * A cavern 12 blocks high with two rock curtains hanging from the roof and a pillar that leaves a 5-block gap at the floor.
     * A hunter stands in the open, another zombie behind the pillar.
     */
    @GameTest(template = "cave", batch = "round111220_cavern", timeoutTicks = TICKS + 40)
    public static void cavernAndPillar(GameTestHelper h) {
        shell(h, true);
        BlockState stone = Blocks.STONE.defaultBlockState();
        carve(h, 2, 2, 2, 41, 13, 8);
        fill(h, 12, 10, 2, 13, 13, 8, stone);
        fill(h, 24, 8, 2, 25, 13, 8, stone);
        fill(h, 30, 7, 2, 32, 13, 8, stone);
        fill(h, 19, 2, 2, 20, 2, 8, stone);
        Run run = new Run(h, "cavern", OWNER, WINGMAN);
        run.limits = r -> { notOnTheCeiling(r); hitsSoon(r); };
        run.add(zombie(h, 16.5, 2, 5.5, run.owner));
        run.add(zombie(h, 37.5, 2, 5.5, null));
        play(h, run);
    }

    /** A tunnel that ends at a glass wall; the hunter is in a closed pocket behind it (no way there). */
    @GameTest(template = "cave", batch = "round111220_deadend", timeoutTicks = TICKS + 40)
    public static void deadEnd(GameTestHelper h) {
        shell(h, true);
        carve(h, 2, 2, 4, 20, 4, 6);
        fill(h, 21, 2, 4, 21, 4, 6, Blocks.GLASS.defaultBlockState());
        carve(h, 22, 2, 4, 26, 4, 6);
        Run run = new Run(h, "deadend", OWNER, WINGMAN);
        run.limits = Round111220GameTests::notOnTheCeiling;
        run.add(zombie(h, 24.5, 2, 5.5, run.owner));
        play(h, run);
    }

    /** The control: a stone floor and open air up to the barrier roof 15 blocks above it, one hunter. */
    @GameTest(template = "cave", batch = "round111220_open", timeoutTicks = TICKS + 40)
    public static void openGround(GameTestHelper h) {
        shell(h, false);
        Run run = new Run(h, "open", OWNER, WINGMAN);
        run.limits = r -> { notOnTheCeiling(r); hitsSoon(r); };
        run.add(zombie(h, 24.5, 2, 5.5, run.owner));
        play(h, run);
    }
}
