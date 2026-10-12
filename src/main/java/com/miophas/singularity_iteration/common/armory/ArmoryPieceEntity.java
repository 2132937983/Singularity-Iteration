package com.miophas.singularity_iteration.common.armory;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * One flying suit piece.
 *
 * <p><b>Deliver</b> pieces carry the real item: they hold position at the launch point
 * until their launch time, fly along {@link ArmoryFlight} onto the owner's body and are
 * equipped on arrival, swapping whatever was worn back into the Armory. They are saved
 * with the chunk, and every failure path (owner offline / dead / in another dimension,
 * timeout, entity killed) gives the item back to the Armory or drops it - never deletes it.
 *
 * <p><b>Return</b> pieces are purely visual echoes of the gear that went home; the swap
 * already happened, so they carry a copy, are never saved and simply vanish.
 *
 * <p>Clients compute the path themselves from the synced launch data and the target's
 * interpolated position, so the flight is smooth regardless of the network update rate.
 *
 * <p>Suit-up sequence of a delivered piece: launch (ignition), cruise with thrusters,
 * {@link ArmoryFlight#PURGE_LEAD} ticks before arrival the piece worn in that slot unlocks
 * and is purged (it pops off the body, then boosts home), the new piece glides onto the
 * body, spends {@link ArmoryFlight#LATCH_TICKS} ticks seating (latch, bolt lock, pressure
 * hiss) and is then equipped.
 */
@SuppressWarnings("null")
public class ArmoryPieceEntity extends Entity {
    public static final byte MODE_DELIVER = 0, MODE_RETURN = 1;
    private static final int OWNER_GRACE_TICKS = 60;
    private static final int MAX_EXTRA_LIFETIME = 400;

    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ArmoryPieceEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_PIECE = SynchedEntityData.defineId(ArmoryPieceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(ArmoryPieceEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(ArmoryPieceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_START = SynchedEntityData.defineId(ArmoryPieceEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_END = SynchedEntityData.defineId(ArmoryPieceEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Long> DATA_LAUNCH = SynchedEntityData.defineId(ArmoryPieceEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_DURATION = SynchedEntityData.defineId(ArmoryPieceEntity.class, EntityDataSerializers.INT);

    // server-only state
    @Nullable private UUID owner;
    @Nullable private GlobalPos armory;
    /** 0.1.8.0: the wearer when it is not the owner (a companion, for example a Touhou Little Maid maid). */
    @Nullable private UUID companion;
    private int suit;
    private int missingOwnerTicks;
    private boolean finished;
    private boolean purged;

    /** Client-side audiovisual hook (sounds, particles), installed by the client setup. */
    public interface Effects { void tick(ArmoryPieceEntity piece); }
    @Nullable public static volatile Effects CLIENT_EFFECTS;
    /** Client-side memory of what has already been played for this piece. */
    public int fxFlags;
    /** Where a purged piece left the body (frozen when its thrusters fire). */
    @Nullable private Vec3 purgeEnd;

    public ArmoryPieceEntity(EntityType<? extends ArmoryPieceEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public static ArmoryPieceEntity deliver(ServerLevel level, ServerPlayer owner, ItemStack item, ArmoryPiece piece, Vec3 start,
                                            GlobalPos armory, int suit, int delayTicks, int durationTicks) {
        ArmoryPieceEntity e = new ArmoryPieceEntity(ArmoryRegistry.PIECE_ENTITY.get(), level);
        e.owner = owner.getUUID();
        e.armory = armory;
        e.suit = suit;
        e.setup(item, piece, MODE_DELIVER, owner.getId(), start, start, level.getGameTime() + delayTicks, durationTicks);
        return e;
    }

    /**
     * A deliver piece for a companion of {@code owner} (0.1.8.0, remote equip). The piece flies to
     * {@code wearer} and is equipped there; the old piece of that slot flies home to the Armory.
     */
    public static ArmoryPieceEntity deliverTo(ServerLevel level, ServerPlayer owner, net.minecraft.world.entity.LivingEntity wearer, ItemStack item,
                                              ArmoryPiece piece, Vec3 start, GlobalPos armory, int suit, int delayTicks, int durationTicks) {
        ArmoryPieceEntity e = deliver(level, owner, item, piece, start, armory, suit, delayTicks, durationTicks);
        e.companion = wearer.getUUID();
        e.entityData.set(DATA_TARGET, wearer.getId());
        return e;
    }

    public static ArmoryPieceEntity echo(ServerLevel level, Entity from, ItemStack copy, ArmoryPiece piece, Vec3 start, Vec3 end) {
        ArmoryPieceEntity e = new ArmoryPieceEntity(ArmoryRegistry.PIECE_ENTITY.get(), level);
        int duration = ArmoryFlight.PURGE_TICKS + ArmoryFlight.returnTicksFor(ArmoryFlight.length(start, end));
        e.setup(copy, piece, MODE_RETURN, from.getId(), start, end, level.getGameTime(), duration);
        return e;
    }

    private void setup(ItemStack item, ArmoryPiece piece, byte mode, int target, Vec3 start, Vec3 end, long launch, int duration) {
        entityData.set(DATA_ITEM, item);
        entityData.set(DATA_PIECE, piece.ordinal());
        entityData.set(DATA_MODE, mode);
        entityData.set(DATA_TARGET, target);
        entityData.set(DATA_START, start.toVector3f());
        entityData.set(DATA_END, end.toVector3f());
        entityData.set(DATA_LAUNCH, launch);
        entityData.set(DATA_DURATION, Math.max(1, duration));
        setPos(start);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ITEM, ItemStack.EMPTY);
        builder.define(DATA_PIECE, 0);
        builder.define(DATA_MODE, MODE_DELIVER);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_START, new Vector3f());
        builder.define(DATA_END, new Vector3f());
        builder.define(DATA_LAUNCH, 0L);
        builder.define(DATA_DURATION, 20);
    }

    // ------------------------------------------------------------------ accessors (renderer)
    public ItemStack item() { return entityData.get(DATA_ITEM); }
    public ArmoryPiece piece() { return ArmoryPiece.VALUES[Math.floorMod(entityData.get(DATA_PIECE), ArmoryPiece.VALUES.length)]; }
    public boolean isReturn() { return entityData.get(DATA_MODE) == MODE_RETURN; }
    public Vec3 start() { return new Vec3(entityData.get(DATA_START)); }
    public long launchTime() { return entityData.get(DATA_LAUNCH); }
    public int duration() { return entityData.get(DATA_DURATION); }
    @Nullable public Entity target() { return level().getEntity(entityData.get(DATA_TARGET)); }

    /**
     * Client clock of the flight: ticks since launch at the last client tick. The level time on a
     * client jumps when the server corrects it (once a second) and stalls when a tick is late, so
     * frames read this smooth counter instead and it only drifts gently back to the level time.
     */
    private double clientClock = Double.NaN;

    /** Raw flight progress 0..1 (negative before launch). */
    public double progress(float partialTick) {
        return age(partialTick) / duration();
    }

    /** Ticks since launch (negative before), smooth between ticks on the client. */
    public float age(float partialTick) {
        if (!Double.isNaN(clientClock)) return (float) (clientClock + partialTick);
        return (float) (level().getGameTime() + partialTick - launchTime());
    }

    private void advanceClientClock() {
        double level = level().getGameTime() - launchTime();
        if (Double.isNaN(clientClock) || Math.abs(level - clientClock) > 4) {
            clientClock = level;
        } else {
            clientClock += 1;
            clientClock += (level - clientClock) * 0.1;
        }
    }

    /** Ticks from launch until the piece touches its body slot (end of the snap). */
    public int snapEnd() {
        return duration() + ArmoryFlight.HOVER_TICKS + ArmoryFlight.SNAP_TICKS;
    }

    /** Ticks since the piece reached the body (negative before). */
    public float latchAge(float partialTick) {
        return age(partialTick) - snapEnd();
    }

    /** Delivery phases, by age: hatch (&lt;0), flight, hover, snap, latch. */
    public boolean inFlight(float partialTick) { float a = age(partialTick); return a >= 0 && a < duration(); }
    public boolean hovering(float partialTick) { float a = age(partialTick) - duration(); return a >= 0 && a < ArmoryFlight.HOVER_TICKS; }
    public boolean snapping(float partialTick) {
        float a = age(partialTick) - duration() - ArmoryFlight.HOVER_TICKS;
        return a >= 0 && a < ArmoryFlight.SNAP_TICKS;
    }

    /** A return piece still popping off the body. */
    public boolean purging(float partialTick) {
        return isReturn() && age(partialTick) < ArmoryFlight.PURGE_TICKS;
    }

    /** Where the piece is heading this frame. */
    public Vec3 destination(float partialTick) {
        if (isReturn()) return new Vec3(entityData.get(DATA_END));
        Entity target = target();
        return target != null ? ArmoryFlight.dockPoint(target, partialTick, piece()) : start();
    }

    public static float bodyYaw(Entity target, float partialTick) {
        return target instanceof net.minecraft.world.entity.LivingEntity living
            ? net.minecraft.util.Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot) : target.getYRot();
    }

    public Vec3 flightPosition(float partialTick) {
        if (isReturn()) return returnPosition(partialTick);
        float a = age(partialTick);
        if (a <= 0) return hatchPosition(a);
        Entity target = target();
        if (target == null) return start();
        float yaw = bodyYaw(target, partialTick);
        Vec3 dock = ArmoryFlight.dockPoint(target, partialTick, piece());
        Vec3 hold = ArmoryFlight.holdPoint(dock, yaw);
        int flight = duration();
        if (a < flight) {
            return ArmoryFlight.approach(start(), hold, ArmoryFlight.forward(yaw), piece(), ArmoryFlight.flightProfile(a / flight));
        }
        float hover = a - flight;
        if (hover < ArmoryFlight.HOVER_TICKS) return hold.add(0, bob(hover), 0);
        float snap = hover - ArmoryFlight.HOVER_TICKS;
        if (snap < ArmoryFlight.SNAP_TICKS) {
            // sucked in, accelerating: smooth start (no jerk out of the hover), hard arrival
            double s = snap / ArmoryFlight.SNAP_TICKS;
            double u = s * s * s * (1.6 - 0.6 * s);
            Vec3 from = hold.add(0, bob(ArmoryFlight.HOVER_TICKS), 0);
            return from.add(dock.subtract(from).scale(u));
        }
        return dock;
    }

    /** Hover float: settles in, bobs gently on its thrusters. */
    private static double bob(float ticks) {
        double settle = Math.min(1, ticks / 3.0);
        // the dip starts at 0 so the piece does not jump when the flight hands over to the hover
        return Math.sin(ticks * 0.75) * 0.055 * settle - 0.04 * (1 - Math.exp(-ticks / 1.5)) * Math.exp(-ticks / 4.0);
    }

    /** Pressurise in the hatch: rises out of the Armory top, shaking, before launch. */
    private Vec3 hatchPosition(float a) {
        double k = net.minecraft.util.Mth.clamp(1 + a / ArmoryFlight.HATCH_TICKS, 0, 1);
        double rise = -0.55 * Math.pow(1 - k, 2);
        double shake = k > 0.5 ? Math.sin(a * 13.0) * 0.015 * k : 0;
        return start().add(shake, rise, Math.cos(a * 11.0) * shake);
    }

    /** Burst direction of this piece: its own outward direction, scattered a little per entity. */
    private Vec3 burstOffset(float yaw, double k) {
        float scatter = ((getId() * 37) % 61 - 30);          // stable +-30 degrees per piece
        return ArmoryFlight.purgeOffset(piece(), yaw + scatter, ArmoryFlight.BURST_DISTANCE * k);
    }

    /** Purge (pop off the body), then a hard boost towards home. */
    private Vec3 returnPosition(float partialTick) {
        float age = age(partialTick);
        Entity target = target();
        if (age < ArmoryFlight.PURGE_TICKS) {
            if (target == null) return start();
            float yaw = target instanceof net.minecraft.world.entity.LivingEntity living
                ? net.minecraft.util.Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot) : target.getYRot();
            return ArmoryFlight.dockPoint(target, partialTick, piece()).add(burstOffset(yaw, ArmoryFlight.burstCurve(Math.max(0, age))));
        }
        if (purgeEnd == null) {
            if (target != null) {
                float yaw = target instanceof net.minecraft.world.entity.LivingEntity living ? living.yBodyRot : target.getYRot();
                purgeEnd = ArmoryFlight.dockPoint(target, 1.0F, piece()).add(burstOffset(yaw, ArmoryFlight.burstCurve(ArmoryFlight.PURGE_TICKS)));
            } else {
                purgeEnd = start();
            }
        }
        double u = (age - ArmoryFlight.PURGE_TICKS) / Math.max(1.0, duration() - ArmoryFlight.PURGE_TICKS);
        return ArmoryFlight.bezier(purgeEnd, destination(partialTick), ArmoryFlight.boost(u));
    }

    // ------------------------------------------------------------------ simulation
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) advanceClientClock();
        setPos(flightPosition(0));
        if (level().isClientSide) {
            Effects fx = CLIENT_EFFECTS;
            if (fx != null) fx.tick(this);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (isReturn()) {
            if (age(0) >= duration() + 2 || tickCount > duration() + 60) discard();
            return;
        }
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        net.minecraft.world.entity.LivingEntity wearer = companion == null ? player
            : level.getEntity(companion) instanceof net.minecraft.world.entity.LivingEntity l ? l : null;
        boolean present = wearer != null && wearer.isAlive() && wearer.level() == level;
        if (present && entityData.get(DATA_TARGET) != wearer.getId()) entityData.set(DATA_TARGET, wearer.getId());
        if (!present) {
            if (++missingOwnerTicks > OWNER_GRACE_TICKS) giveBack(player);
            return;
        }
        missingOwnerTicks = 0;
        float age = age(0);
        // the old piece unlocks and leaves while the new one brakes and hovers in front
        if (!purged && age >= duration()) purge(wearer, player);
        if (age >= snapEnd() + ArmoryFlight.LATCH_TICKS) dock(wearer, player);
        else if (tickCount > snapEnd() + MAX_EXTRA_LIFETIME + 200) giveBack(player);
    }

    /** An item that the Armory cannot take: owner inventory, else dropped at the wearer. */
    private static void handOver(ItemStack rest, net.minecraft.world.entity.LivingEntity wearer, @Nullable ServerPlayer player) {
        if (rest.isEmpty()) return;
        if (player != null && player.getInventory().add(rest)) return;
        if (wearer instanceof ServerPlayer p) p.drop(rest, false);
        else wearer.spawnAtLocation(rest);
    }

    /** The piece worn in this slot unlocks and is flown home ahead of the new one. */
    private void purge(net.minecraft.world.entity.LivingEntity wearer, @Nullable ServerPlayer player) {
        purged = true;
        ArmoryPiece piece = piece();
        ItemStack old = wearer.getItemBySlot(piece.slot);
        if (old.isEmpty() || old.getItem() instanceof ArmoryRemoteItem) return;   // the remote is stashed on docking instead
        boolean creative = wearer instanceof Player p && p.isCreative();
        if (!creative && net.minecraft.world.item.enchantment.EnchantmentHelper.has(old,
                net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) return;
        ItemStack copy = old.copy();
        wearer.setItemSlot(piece.slot, ItemStack.EMPTY);
        ServerLevel level = (ServerLevel) level();
        Vec3 at = ArmoryFlight.dockPoint(wearer, 1.0F, piece);
        ItemStack rest = storeHome(copy.copy(), piece);
        if (!rest.isEmpty()) {
            handOver(rest, wearer, player);
            return;
        }
        level.addFreshEntity(echo(level, wearer, copy, piece, at, homeTarget(level, armory, at)));
    }

    private void dock(net.minecraft.world.entity.LivingEntity wearer, @Nullable ServerPlayer player) {
        if (finished) return;
        finished = true;
        ArmoryPiece piece = piece();
        ItemStack incoming = item().copy();
        ItemStack old = wearer.getItemBySlot(piece.slot).copy();
        // 0.1.8.0: no vanilla armor equip sound on a player; the heavy mechanical seal below replaces it.
        if (wearer instanceof ServerPlayer sp) equipSilently(sp, piece.slot, incoming);
        else wearer.setItemSlot(piece.slot, incoming);
        entityData.set(DATA_ITEM, ItemStack.EMPTY);
        if (player != null) {
            // 0.1.8.0 public API
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.miophas.singularity_iteration.core.api.armory.ArmoryEvent.PieceDocked(
                player, wearer, piece.slot, wearer.getItemBySlot(piece.slot)));
        }
        ServerLevel level = (ServerLevel) level();
        Vec3 at = ArmoryFlight.dockPoint(wearer, 1.0F, piece);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 8, 0.25, 0.2, 0.25, 0.05);
        level.playSound(null, at.x, at.y, at.z, com.miophas.singularity_iteration.common.registry.mio_icif_sounds.ARMORY_SEAL.get(),
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.94F + level.random.nextFloat() * 0.12F);
        // the remote stays with its owner: it moves to a free inventory slot instead of flying home
        if (old.getItem() instanceof ArmoryRemoteItem && wearer instanceof ServerPlayer sp) {
            stash(sp, old);
            old = ItemStack.EMPTY;
        }
        // Normally the slot was purged before arrival; anything put there since goes home too.
        if (!old.isEmpty()) {
            ItemStack rest = storeHome(old, piece);
            boolean wentHome = rest.isEmpty();
            handOver(rest, wearer, player);
            if (wentHome) {
                Vec3 home = homeTarget(level, armory, at);
                level.addFreshEntity(echo(level, wearer, old, piece, at, home));
            }
        }
        discard();
    }

    /**
     * Equips a stack without the vanilla equip sound (0.1.8.0). It writes the player inventory
     * directly. Vanilla detects the equipment change in the next living tick: it updates the
     * attributes, fires {@code LivingEquipmentChangeEvent} and sends the equipment to the clients.
     * Only {@code setItemSlot} plays the armor equip sound.
     */
    static void equipSilently(ServerPlayer player, net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack) {
        var inv = player.getInventory();
        switch (slot) {
            case HEAD, CHEST, LEGS, FEET -> inv.armor.set(slot.getIndex(), stack);
            case MAINHAND -> inv.items.set(inv.selected, stack);
            case OFFHAND -> inv.offhand.set(0, stack);
            default -> player.setItemSlot(slot, stack);
        }
    }

    /** Puts a stack into a free main-inventory slot other than the selected one, else drops it. */
    static void stash(ServerPlayer player, ItemStack stack) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == inv.selected || !inv.items.get(i).isEmpty()) continue;
            inv.items.set(i, stack);
            return;
        }
        player.drop(stack, false);
    }

    /** Fly-home target: the home block's top when close, else a point high up towards it. */
    static Vec3 homeTarget(Level here, @Nullable GlobalPos armory, Vec3 from) {
        if (armory != null && armory.dimension() == here.dimension()) {
            Vec3 top = Vec3.atCenterOf(armory.pos()).add(0, here.getBlockEntity(armory.pos()) instanceof ArmorShowcaseBlockEntity ? 0.6 : 0.7, 0);
            if (top.distanceTo(from) <= ArmoryFlight.LOCAL_LAUNCH_RANGE) return top;
            Vec3 dir = top.subtract(from);
            return from.add(new Vec3(dir.x, 0, dir.z).normalize().scale(30)).add(0, 18, 0);
        }
        return from.add(0, 24, 0);
    }

    /**
     * Unlocks every listed worn piece at the same instant: the locks release, the pieces blow
     * off the body in all directions (one "whump") and then thrust home to {@code home}
     * (an Armory or an Armor Showcase), where {@code store} puts them away. Pieces that do not
     * fit go to the inventory instead. Returns the number of pieces purged.
     */
    public static int burstPurge(ServerPlayer player, java.util.List<ArmoryPiece> pieces, GlobalPos home,
                                 java.util.function.BiFunction<ArmoryPiece, ItemStack, ItemStack> store) {
        ServerLevel level = (ServerLevel) player.level();
        int purged = 0;
        for (ArmoryPiece piece : pieces) {
            ItemStack old = player.getItemBySlot(piece.slot);
            if (old.isEmpty() || !ArmoryRules.isSummonable(old)) continue;
            if (old.getItem() instanceof ArmoryRemoteItem) continue;      // the remote never flies away from its owner
            if (!player.isCreative() && net.minecraft.world.item.enchantment.EnchantmentHelper.has(old,
                    net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) continue;
            ItemStack copy = old.copy();
            player.setItemSlot(piece.slot, ItemStack.EMPTY);
            ItemStack rest = store.apply(piece, copy.copy());
            if (!rest.isEmpty()) {
                if (!player.getInventory().add(rest)) player.drop(rest, false);
                continue;
            }
            Vec3 at = ArmoryFlight.dockPoint(player, 1.0F, piece);
            ArmoryPieceEntity echo = echo(level, player, copy, piece, at, homeTarget(level, home, at));
            echo.armory = home;
            level.addFreshEntity(echo);
            purged++;
        }
        if (purged > 0) {
            Vec3 c = player.position().add(0, player.getBbHeight() * 0.55, 0);
            level.playSound(null, c.x, c.y, c.z, net.minecraft.sounds.SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.9F, 0.8F);
            level.sendParticles(ParticleTypes.POOF, c.x, c.y, c.z, 18, 0.35, 0.5, 0.35, 0.12);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, 14, 0.3, 0.5, 0.3, 0.25);
        }
        return purged;
    }

    /** Puts an item into the Armory this piece came from; returns what did not fit. */
    private ItemStack storeHome(ItemStack stack, ArmoryPiece piece) {
        if (armory == null || !(level() instanceof ServerLevel here)) return stack;
        ServerLevel home = here.getServer().getLevel(armory.dimension());
        if (home == null) return stack;
        if (home.getBlockEntity(armory.pos()) instanceof mio_icif_armory box) {
            return box.store(suit, piece, stack);
        }
        if (home.getBlockEntity(armory.pos()) instanceof ArmorShowcaseBlockEntity showcase) {
            return showcase.storePiece(piece, stack);
        }
        return stack;
    }

    /** Failure path: item back to the Armory, else to the player, else onto the ground. */
    private void giveBack(@Nullable ServerPlayer player) {
        if (finished) return;
        finished = true;
        ItemStack stack = item().copy();
        entityData.set(DATA_ITEM, ItemStack.EMPTY);
        ItemStack rest = storeHome(stack, piece());
        if (!rest.isEmpty() && player != null && player.getInventory().add(rest)) rest = ItemStack.EMPTY;
        if (!rest.isEmpty()) spawnAtLocation(rest);
        discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && !finished && !isReturn() && reason.shouldDestroy() && reason != RemovalReason.DISCARDED) {
            ItemStack stack = item();
            if (!stack.isEmpty()) {
                finished = true;
                ItemStack rest = storeHome(stack.copy(), piece());
                if (!rest.isEmpty()) spawnAtLocation(rest);
                entityData.set(DATA_ITEM, ItemStack.EMPTY);
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean shouldBeSaved() {
        return !isReturn() && super.shouldBeSaved();
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean ignoreExplosion(net.minecraft.world.level.Explosion explosion) { return true; }
    @Override public boolean fireImmune() { return true; }
    @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 256 * 256; }

    // ------------------------------------------------------------------ persistence
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!item().isEmpty()) tag.put("Item", item().save(registryAccess()));
        tag.putInt("Piece", entityData.get(DATA_PIECE));
        Vector3f s = entityData.get(DATA_START);
        tag.putFloat("SX", s.x); tag.putFloat("SY", s.y); tag.putFloat("SZ", s.z);
        tag.putLong("Launch", launchTime());
        tag.putInt("Duration", duration());
        tag.putInt("Suit", suit);
        tag.putBoolean("Purged", purged);
        if (owner != null) tag.putUUID("Owner", owner);
        if (armory != null) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, armory).result().ifPresent(t -> tag.put("Armory", t));
        if (companion != null) tag.putUUID("Companion", companion);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ItemStack stack = tag.contains("Item") ? ItemStack.parse(registryAccess(), tag.getCompound("Item")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        entityData.set(DATA_ITEM, stack);
        entityData.set(DATA_PIECE, tag.getInt("Piece"));
        entityData.set(DATA_MODE, MODE_DELIVER);
        entityData.set(DATA_START, new Vector3f(tag.getFloat("SX"), tag.getFloat("SY"), tag.getFloat("SZ")));
        entityData.set(DATA_LAUNCH, tag.getLong("Launch"));
        entityData.set(DATA_DURATION, Math.max(1, tag.getInt("Duration")));
        suit = tag.getInt("Suit");
        purged = tag.getBoolean("Purged");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        armory = tag.contains("Armory") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Armory")).result().orElse(null) : null;
        companion = tag.hasUUID("Companion") ? tag.getUUID("Companion") : null;
        if (stack.isEmpty()) finished = true;
    }
}
