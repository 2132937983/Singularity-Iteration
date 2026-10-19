// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.registry.mio_icif_sounds;
import com.miophas.singularity_iteration.common.suit.SuitEnergyPool;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Wingman (0.1.8.0): a stored Armory suit that flies as an empty-suit drone next to its owner.
 *
 * <h2>Specification</h2>
 * <ul>
 *   <li>The Wingman wears the real items of one Armory suit (four armor pieces, main hand, off
 *       hand). The items are moved, not copied. Thus the Wingman has the armor value, the
 *       weapon damage, the enchantments and the electric armor protection of the suit, as for a
 *       player who wears it. The base values are the player base values (20 health, attack
 *       damage 1, attack speed 4).</li>
 *   <li>The items never disappear. Recall, owner logout, owner in a different dimension, low
 *       power and a removal (for example {@code /kill}) put the items back into the Armory. If
 *       the Armory has no free position, the items go to the owner inventory, else they drop
 *       at the Wingman.</li>
 *   <li>A hit that would kill the Wingman sets it to 1 health. It then returns to the Armory
 *       and is invulnerable on the way. Only void damage and {@code /kill} remove it at once.</li>
 *   <li>The owner and the other Wingmen of the owner cannot hurt it.</li>
 *   <li>Upkeep: {@link #UPKEEP_EU} EU per second. The electric pieces of the suit pay first (the
 *       full-set pool applies). If they cannot pay, the Armory pays when it is loaded. After
 *       {@link #LOW_POWER_LIMIT} seconds without power, the Wingman returns.</li>
 * </ul>
 */
@SuppressWarnings("null")
public class WingmanEntity extends Mob {
    /** EU per second of flight. */
    public static final long UPKEEP_EU = 160;
    /** EU of one Fulton recovery (paid by the Armory). */
    public static final long FULTON_EU = 4000;
    /** Seconds without power before the Wingman returns. */
    public static final int LOW_POWER_LIMIT = 5;
    /** Mobs that a Fulton recovery cannot take (data pack tag). */
    public static final TagKey<EntityType<?>> FULTON_IMMUNE = TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "wingman/fulton_immune"));

    public static final byte ACTIVE = 0, RETURNING = 1, LIFTING = 2;

    private static final double CRUISE = 0.9, COMBAT = 1.15, ACCEL = 0.14;
    private static final double SCAN_RANGE = 24, LEASH = 28, CATCH_UP = 64;

    private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.BYTE);
    /** 0.1.9.1 client effects: locked target (entity id, -1 none), shot counter, thruster boost counter. */
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHOTS = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOOSTS = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.INT);

    // server state (saved)
    @Nullable private UUID owner;
    @Nullable private GlobalPos armory;
    private int set;
    /** 0.1.10.5: the token of this sortie ({@link WingmanSorties}); null in a save of an older version. */
    @Nullable private UUID sortie;
    // server state (not saved)
    @Nullable private LivingEntity target;
    private int attackCooldown, deflectCooldown, lowPowerSeconds, stateTicks, slot, slotCount = 1;
    // 0.1.8.4 combat state: circling direction (+1/-1), melee peel-off, blade dash, Slash Art timer, blocked line
    private int strafeSign = 1, retreatTicks, dashTicks, artsCooldown, blockedTicks;
    /** Ticks between two Slash Arts of a blade-wielding Wingman. */
    private static final int ARTS_INTERVAL = 200;
    private int lastFulton = -1;
    private boolean secured, protectedReturn;
    // 0.1.9.1 tactics: firing station (re-planned every STATION_TICKS), evasion cooldown
    @Nullable private Vec3 station;
    private int stationTicks, boostCooldown;
    private static final int STATION_TICKS = 8, BOOST_COOLDOWN = 26;
    // 0.1.10.2 quantum command (saved): an individual mode survives a squad mode change; an order
    // anchors the Wingman at a point instead of the owner (see WingmanOrder)
    private boolean individual;
    private WingmanOrder order = WingmanOrder.NONE;
    @Nullable private Vec3 orderPos;
    // not saved: the focus target and the age of the order
    private int orderTarget = -1, orderAge;

    public WingmanEntity(EntityType<? extends WingmanEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0);   // items leave only through secure()
    }

    /**
     * Mob attributes plus every attribute a player has (0.1.10.5). Weapon mods read player
     * attributes from any wielder: SlashBlade: Resharped reads {@code SWEEPING_DAMAGE_RATIO} and
     * {@code ENTITY_INTERACTION_RANGE}, and a missing attribute throws in
     * {@code getAttributeValue}, which crashed the server in every tick of a slash effect. The map
     * is built from this supplier at construction, so saved Wingmen get the new attributes too.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.ATTACK_SPEED, 4.0)
            .add(Attributes.FLYING_SPEED, 0.6)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, SCAN_RANGE)
            .add(Attributes.LUCK)
            .add(Attributes.BLOCK_INTERACTION_RANGE, 4.5)
            .add(Attributes.ENTITY_INTERACTION_RANGE, 3.0)
            .add(Attributes.BLOCK_BREAK_SPEED)
            .add(Attributes.SUBMERGED_MINING_SPEED)
            .add(Attributes.SNEAKING_SPEED)
            .add(Attributes.MINING_EFFICIENCY)
            .add(Attributes.SWEEPING_DAMAGE_RATIO);
    }

    /** A new Wingman that wears {@code items} (indexed by {@link ArmoryPiece#ordinal()}). */
    static WingmanEntity create(ServerLevel level, ServerPlayer owner, GlobalPos armory, int set, WingmanMode mode, ItemStack[] items, Vec3 at) {
        WingmanEntity w = new WingmanEntity(ArmoryRegistry.WINGMAN.get(), level);
        w.owner = owner.getUUID();
        w.armory = armory;
        w.set = set;
        w.setMode(mode);
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            ItemStack stack = items[p.ordinal()];
            if (stack != null && !stack.isEmpty()) w.setItemSlot(p.slot, stack);   // first tick: no equip sound
        }
        w.moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
        WingmanSorties ledger = WingmanSorties.get(level.getServer());
        if (ledger != null) w.sortie = ledger.open(w.getUUID());
        return w;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MODE, (byte) 0);
        builder.define(DATA_STATE, ACTIVE);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_SHOTS, 0);
        builder.define(DATA_BOOSTS, 0);
    }

    // ------------------------------------------------------------------ accessors
    public WingmanMode mode() { return WingmanMode.byId(entityData.get(DATA_MODE)); }
    public void setMode(WingmanMode mode) { entityData.set(DATA_MODE, (byte) mode.ordinal()); target = null; }
    public byte state() { return entityData.get(DATA_STATE); }
    private void setState(byte state) { entityData.set(DATA_STATE, state); stateTicks = 0; }
    @Nullable public UUID owner() { return owner; }
    /** Server: the current target. */
    @Nullable LivingEntity currentTarget() { return target; }
    /** Client: the locked target id (-1 none), the shot and boost counters (effects start when they change). */
    public int lockedTargetId() { return entityData.get(DATA_TARGET); }
    public int shotCount() { return entityData.get(DATA_SHOTS); }
    public int boostCount() { return entityData.get(DATA_BOOSTS); }
    public int set() { return set; }
    @Nullable GlobalPos armory() { return armory; }
    /** 0.1.10.5: the sortie token (server), see {@link WingmanSorties}. */
    @Nullable UUID sortie() { return sortie; }
    void setSlot(int slot, int count) { this.slot = slot; this.slotCount = Math.max(1, count); }

    /** 0.1.10.2: true when the Wingman keeps its own mode when the squad mode changes. */
    public boolean individual() { return individual; }
    /** 0.1.10.2: the current order ({@link WingmanOrder#NONE} without one). */
    public WingmanOrder order() { return order; }
    /** 0.1.10.2: the point of an anchored order, else null. */
    @Nullable public Vec3 orderPos() { return orderPos; }
    /** 0.1.10.2: entity id of the focus target (-1 none). */
    public int orderTargetId() { return orderTarget; }
    /** 0.1.10.2: index of this Wingman in the owner's squad (0-based), as the formation uses it. */
    public int squadSlot() { return slot; }

    /** 0.1.10.2: the mode of this Wingman alone ({@code individual}) or as part of the squad. */
    public void setMode(WingmanMode mode, boolean individual) {
        setMode(mode);
        this.individual = individual;
    }

    /**
     * 0.1.10.2: gives an order. {@code pos}: the point of an anchored order (null: where the Wingman
     * is now); {@code focus}: the target of a focus order (a focus order without a target is no order).
     */
    public void order(WingmanOrder order, @Nullable Vec3 pos, @Nullable LivingEntity focus) {
        if (order == WingmanOrder.FOCUS && focus == null) order = WingmanOrder.NONE;
        this.order = order;
        this.orderPos = order.anchored() ? (pos != null ? pos : position()) : null;
        this.orderTarget = order == WingmanOrder.FOCUS ? focus.getId() : -1;
        orderAge = 0;
        target = order == WingmanOrder.FOCUS ? focus : null;
        station = null;
    }

    /** 0.1.10.2 "form up": the order ends and the tactical mode applies again. */
    public void clearOrder() {
        order = WingmanOrder.NONE;
        orderPos = null;
        orderTarget = -1;
        target = null;
        station = null;
    }

    /**
     * 0.1.10.0: one shot of an entangled volley of the owner's quantum suit: the Wingman turns to
     * {@code at} and its muzzle flashes (the bolts themselves are resolved by the suit).
     */
    public void entangledShot(Vec3 at) {
        face(at);
        swing(InteractionHand.MAIN_HAND);
        entityData.set(DATA_SHOTS, entityData.get(DATA_SHOTS) + 1);
    }

    /** Fraction of the stored energy of the electric pieces, or -1 without electric pieces. */
    public float energyFraction() {
        var api = MioIcifAPI.instance().getItemAPI();
        long stored = 0, capacity = 0;
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            ItemStack s = getItemBySlot(p.slot);
            if (s.isEmpty() || !api.isElectricArmor(s)) continue;
            stored += Math.max(0, api.getElectricArmorStored(s));
            capacity += Math.max(0, api.getElectricArmorMaxEnergy(s));
        }
        return capacity <= 0 ? -1 : (float) stored / capacity;
    }

    // ------------------------------------------------------------------ server logic
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel world) || secured) return;
        stateTicks++;
        // 0.1.9.0: a retired suit piece (legacy shell) turns into its suit piece with the frame core
        if (stateTicks % 20 == 1) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST}) {
                ItemStack worn = getItemBySlot(slot);
                if (!com.miophas.singularity_iteration.common.suit.core.LegacyArmor.isLegacy(worn)) continue;
                var result = com.miophas.singularity_iteration.common.suit.core.LegacyArmor.convert(worn);
                if (result == null) continue;
                setItemSlot(slot, result.stack().getItem() instanceof net.minecraft.world.item.ArmorItem ? result.stack() : ItemStack.EMPTY);
                if (!(result.stack().getItem() instanceof net.minecraft.world.item.ArmorItem)) spawnAtLocation(result.stack());
                for (ItemStack extra : result.extras()) spawnAtLocation(extra);
            }
        }
        if (attackCooldown > 0) attackCooldown--;
        if (deflectCooldown > 0) deflectCooldown--;
        if (boostCooldown > 0) boostCooldown--;
        syncTarget(world);
        if (artsCooldown > 0) artsCooldown--;
        if (state() == ACTIVE) SlashBladeCombat.tick(this);   // a blade's combo actions run on the wielder's tick
        disarmIdle();                                          // 0.1.10.4: the nanosaber goes off after a fight
        ServerPlayer player = owner == null ? null : world.getServer().getPlayerList().getPlayer(owner);
        boolean ownerHere = player != null && player.isAlive() && player.level() == world && !player.isSpectator();
        if (state() != RETURNING && !ownerHere) {
            startReturn(null);
        }
        if (tickCount % 20 == 0 && state() != RETURNING) upkeep(world, player);
        switch (state()) {
            case RETURNING -> tickReturn(world);
            case LIFTING -> tickLift(world, player);
            default -> {
                if (!ownerHere) return;
                // 0.1.10.2 quantum command: an order replaces the mode and the owner leash
                if (order != WingmanOrder.NONE) { tickOrder(world, player); return; }
                if (distanceToSqr(player) > CATCH_UP * CATCH_UP) catchUp(player);
                switch (mode()) {
                    case OFFENSIVE -> tickOffensive(player);
                    case DISPERSAL -> tickDispersal(player);
                    case ESCORT -> tickEscort(world, player);
                    case FULTON -> tickFulton(world, player);
                }
            }
        }
    }

    private void upkeep(ServerLevel world, @Nullable ServerPlayer player) {
        // 0.1.10.5: count the nominal EU that is covered. Energy Saving makes a piece pay less EU
        // for the same share; before, the saved EU counted as unpaid (the Armory paid it again, or
        // the Wingman flew home for low power).
        long due = UPKEEP_EU;
        var api = MioIcifAPI.instance().getItemAPI();
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            ItemStack piece = getItemBySlot(s);
            if (piece.isEmpty() || !api.isElectricArmor(piece) || SuitEnergyPool.available(this, piece) <= 0) continue;
            due -= SuitEnergyPool.payPart(this, piece, due);
            if (due <= 0) break;
        }
        if (due > 0) {
            mio_icif_armory home = loadedArmory(world);
            var payment = home == null ? null : home.getEnergyStorageInternal().scexReserveInternal(due);
            if (payment != null) { payment.commit(); due = 0; }
        }
        if (due <= 0) { lowPowerSeconds = 0; return; }
        if (++lowPowerSeconds >= LOW_POWER_LIMIT) {
            startReturn(player == null ? null : Component.translatable("message.mio_icif.wingman.low_power"));
        }
    }

    /** The Armory of this Wingman when its chunk is loaded (no chunk load), else null. */
    @Nullable
    private mio_icif_armory loadedArmory(ServerLevel world) {
        if (armory == null) return null;
        ServerLevel level = world.getServer().getLevel(armory.dimension());
        if (level == null || !level.isLoaded(armory.pos())) return null;
        return level.getBlockEntity(armory.pos()) instanceof mio_icif_armory a ? a : null;
    }

    // ---- modes
    private void tickOffensive(ServerPlayer player) {
        LivingEntity focus = ownerFocus(player);
        if (focus != null && focus != target) target = focus;          // the owner's new target first, at once
        else if (tickCount % 5 == 0 || !valid(target, player, LEASH)) {
            // 0.1.9.1: threat scoring (owner's call, fusing creepers, attackers, hunters, focus fire)
            target = WingmanTactics.select(this, player, player.position(), SCAN_RANGE, valid(target, player, LEASH) ? target : null,
                e -> valid(e, player, LEASH));
        }
        if (target != null) engage(target, player);
        else steer(formation(player), CRUISE, player);
    }

    private void tickDispersal(ServerPlayer player) {
        double angle = Math.toRadians(player.getYRot()) + slot * (Math.PI * 2 / slotCount);
        Vec3 station = player.position().add(-Math.sin(angle) * 10, 3.5, Math.cos(angle) * 10);
        if (tickCount % 10 == 0 || !valid(target, player, LEASH)) {
            target = WingmanTactics.select(this, player, station, 14, valid(target, player, LEASH) ? target : null, e -> valid(e, player, LEASH));
            // decoy: the mobs near the station that hunt the owner turn to this Wingman
            for (Mob mob : level().getEntitiesOfClass(Mob.class, new AABB(station, station).inflate(12),
                    m -> m instanceof Enemy && m.getTarget() == player)) mob.setTarget(this);
        }
        if (target != null) engage(target, player);
        else steer(station, CRUISE, player);
    }

    private void tickEscort(ServerLevel world, ServerPlayer player) {
        // projectiles that fly at the owner are destroyed in flight
        Projectile threat = deflectCooldown > 0 ? null : nearestThreat(player);
        if (threat != null) {
            steer(threat.position(), COMBAT * 1.3, player);
            if (distanceToSqr(threat) < 2.6 * 2.6) {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK, threat.getX(), threat.getY(), threat.getZ(), 10, 0.15, 0.15, 0.15, 0.2);
                world.playSound(null, threat.getX(), threat.getY(), threat.getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 0.8F, 1.3F);
                threat.discard();
                deflectCooldown = 3;
            }
            return;
        }
        if (tickCount % 5 == 0 || !valid(target, player, 10)) {
            // 0.1.9.1: escort scores only the threats at the owner (attackers, hunters, creepers within 10 blocks)
            target = WingmanTactics.select(this, player, player.position(), 10, valid(target, player, 10) ? target : null,
                e -> valid(e, player, 10) && (e instanceof Mob m && m.getTarget() == player || e == player.getLastHurtByMob()
                    || e instanceof net.minecraft.world.entity.monster.Creeper && e.distanceTo(player) < 7));
            // 0.1.8.4: body-block a hostile that closes in on the owner, even before it strikes
            if (target == null) target = hunterOf(player, 6);
        }
        if (target != null) engage(target, player);
        else steer(formation(player), CRUISE, player);
    }

    private void tickFulton(ServerLevel world, ServerPlayer player) {
        if (tickCount % 10 == 0 && !fultonCandidate(target, player)) target = pickFulton(player);
        if (target == null) { steer(formation(player), CRUISE, player); return; }
        steer(target.position().add(0, target.getBbHeight() + 0.7, 0), COMBAT, player);
        face(target.position());
        if (distanceToSqr(target.position().add(0, target.getBbHeight(), 0)) < 1.9 * 1.9) {
            mio_icif_armory home = fultonArmory(world);
            if (home == null) {
                player.displayClientMessage(Component.translatable("message.mio_icif.wingman.fulton_no_armory"), true);
                target = null;
                return;
            }
            var payment = home.getEnergyStorageInternal().scexReserveInternal(FULTON_EU);
            if (payment == null) {
                player.displayClientMessage(Component.translatable("message.mio_icif.armory.no_energy", FULTON_EU, home.storedEnergy()), true);
                target = null;
                return;
            }
            payment.commit();
            lastFulton = target.getId();
            target.startRiding(this, true);
            world.playSound(null, getX(), getY(), getZ(), mio_icif_sounds.ARMORY_LATCH.get(), SoundSource.NEUTRAL, 1.0F, 0.8F);
            setState(LIFTING);
        }
    }

    /** The paired Armory for a Fulton recovery: same dimension as this Wingman, loaded or not. */
    @Nullable
    private mio_icif_armory fultonArmory(ServerLevel world) {
        if (armory == null || armory.dimension() != world.dimension()) return null;
        return world.getBlockEntity(armory.pos()) instanceof mio_icif_armory a ? a : null;
    }

    private void tickLift(ServerLevel world, @Nullable ServerPlayer player) {
        Entity cargo = getFirstPassenger();
        if (cargo == null) { setState(ACTIVE); return; }
        setDeltaMovement(getDeltaMovement().scale(0.6).add(0, 0.18, 0));
        if (stateTicks % 3 == 0) world.sendParticles(ParticleTypes.CLOUD, getX(), getY() - 0.4, getZ(), 2, 0.2, 0.1, 0.2, 0.02);
        if (stateTicks < 30) return;
        mio_icif_armory home = fultonArmory(world);
        Vec3 drop = home == null ? null : dropPoint(world, home.getBlockPos(), cargo);
        ejectPassengers();
        if (drop != null) {
            cargo.teleportTo(drop.x, drop.y, drop.z);
            cargo.fallDistance = 0;
            if (cargo instanceof Mob mob) mob.setPersistenceRequired();
            world.playSound(null, drop.x, drop.y, drop.z, mio_icif_sounds.ARMORY_SEAL.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            if (player != null) player.displayClientMessage(Component.translatable("message.mio_icif.wingman.fulton_done", cargo.getDisplayName()), true);
        } else if (player != null) {
            cargo.teleportTo(player.getX(), player.getY(), player.getZ());
        }
        target = null;
        if (player != null && player.level() == world) catchUp(player);
        setState(ACTIVE);
    }

    /** A free place next to the Armory for a recovered mob: two-high air on solid ground. */
    @Nullable
    private static Vec3 dropPoint(ServerLevel world, BlockPos home, Entity cargo) {
        int height = Math.max(1, Mth.ceil(cargo.getBbHeight()));
        for (int r = 2; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    for (int dy = -1; dy <= 2; dy++) {
                        BlockPos feet = home.offset(dx, dy, dz);
                        if (!world.getBlockState(feet.below()).isFaceSturdy(world, feet.below(), net.minecraft.core.Direction.UP)) continue;
                        boolean free = true;
                        for (int h = 0; h < height && free; h++) free = world.getBlockState(feet.above(h)).getCollisionShape(world, feet.above(h)).isEmpty();
                        if (free) return Vec3.atBottomCenterOf(feet);
                    }
                }
            }
        }
        return Vec3.atCenterOf(home).add(0, 1.2, 0);
    }

    // ---- orders (0.1.10.2)

    /** Height of the hold point above the order point, and the spread of the squad around it. */
    private static final double ORDER_HEIGHT = 2.6, ORDER_SPREAD = 2.4;

    /**
     * An order: FOCUS chases its target anywhere in the dimension (within the focus range) and
     * ends when the target is down. MOVE flies to the point (no fighting on the way), then guards it
     * like HOLD: the squad spreads on a small ring above the point and engages the hostile creatures
     * within the hold radius. SUPPRESS patrols a ring above the area and engages everything in the
     * suppression radius. The owner leash and the catch-up jump do not apply.
     */
    private void tickOrder(ServerLevel world, ServerPlayer player) {
        orderAge++;
        if (order == WingmanOrder.FOCUS) {
            Entity e = world.getEntity(orderTarget);
            if (!(e instanceof LivingEntity focus) || !focusValid(focus, player)) {
                clearOrder();
                return;
            }
            target = focus;
            engage(focus, player);
            return;
        }
        Vec3 anchor = orderPos != null ? orderPos : position();
        boolean suppress = order == WingmanOrder.SUPPRESS;
        double radius = suppress ? com.miophas.singularity_iteration.common.suit.quantum.QuantumConfig.suppressRadius()
            : com.miophas.singularity_iteration.common.suit.quantum.QuantumConfig.holdRadius();
        Vec3 post;
        if (suppress) {
            double a = orderAge * 0.025 + slot * Math.PI * 2 / slotCount;
            post = anchor.add(Math.cos(a) * radius * 0.65, 5.0, Math.sin(a) * radius * 0.65);
        } else {
            double a = slot * Math.PI * 2 / slotCount;
            double spread = slotCount > 1 ? ORDER_SPREAD : 0;
            post = anchor.add(Math.cos(a) * spread, ORDER_HEIGHT, Math.sin(a) * spread);
        }
        boolean travelling = order == WingmanOrder.MOVE && target == null && position().distanceToSqr(post) > 6 * 6;
        if (!travelling && (tickCount % 5 == 0 || !guards(target, player, anchor, radius + 6))) {
            target = WingmanTactics.select(this, player, anchor, radius, guards(target, player, anchor, radius + 6) ? target : null,
                e -> guards(e, player, anchor, radius + 6), anchor);
        }
        if (target != null && !travelling) engage(target, player);
        else steer(post, travelling ? COMBAT : CRUISE, player);
    }

    /** A valid target near the order point (the owner leash does not apply). */
    private boolean guards(@Nullable LivingEntity e, ServerPlayer player, Vec3 anchor, double radius) {
        return e != null && e.isAlive() && e.level() == level() && !e.isRemoved() && e != player
            && !(e instanceof Player) && !(e instanceof WingmanEntity) && e.distanceToSqr(anchor) < radius * radius
            && !player.isAlliedTo(e) && !(e instanceof net.minecraft.world.entity.OwnableEntity own && player.getUUID().equals(own.getOwnerUUID()));
    }

    /** A focus target: alive, in this dimension, within the focus range of this Wingman; never a player, a Wingman or an ally. */
    boolean focusValid(LivingEntity e, ServerPlayer player) {
        double range = com.miophas.singularity_iteration.common.suit.quantum.QuantumConfig.focusRange();
        return e.isAlive() && !e.isRemoved() && e.level() == level() && !(e instanceof Player) && !(e instanceof WingmanEntity)
            && e.distanceToSqr(this) < range * range && !player.isAlliedTo(e)
            && !(e instanceof net.minecraft.world.entity.OwnableEntity own && player.getUUID().equals(own.getOwnerUUID()));
    }

    // ---- targets
    private boolean valid(@Nullable LivingEntity e, ServerPlayer player, double leash) {
        return e != null && e.isAlive() && e.level() == level() && !e.isRemoved() && e != player
            && !(e instanceof Player) && !(e instanceof WingmanEntity) && e.distanceToSqr(player) < leash * leash
            && !player.isAlliedTo(e) && !(e instanceof net.minecraft.world.entity.OwnableEntity own && player.getUUID().equals(own.getOwnerUUID()));
    }

    @Nullable
    private LivingEntity pickOffensive(ServerPlayer player) {
        LivingEntity marked = player.getLastHurtMob();
        if (marked != null && player.tickCount - player.getLastHurtMobTimestamp() < 200 && valid(marked, player, LEASH)) return marked;
        LivingEntity attacker = player.getLastHurtByMob();
        if (attacker != null && player.tickCount - player.getLastHurtByMobTimestamp() < 200 && valid(attacker, player, LEASH)) return attacker;
        return nearestHostile(player.position(), SCAN_RANGE, player);
    }

    @Nullable
    private LivingEntity nearestHostile(Vec3 around, double range, ServerPlayer player) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob mob : level().getEntitiesOfClass(Mob.class, new AABB(around, around).inflate(range), m -> m instanceof Enemy)) {
            if (!valid(mob, player, LEASH)) continue;
            double d = mob.distanceToSqr(around);
            if (d < bestD) { bestD = d; best = mob; }
        }
        return best;
    }

    /** 0.1.10.8: the projectile scan runs every 2nd tick; in between the last threat is kept while it still flies at the owner. */
    @Nullable private Projectile lastThreat;
    private int threatScanAt = -10;

    @Nullable
    private Projectile nearestThreat(ServerPlayer player) {
        if (tickCount >= threatScanAt && tickCount - threatScanAt < 2) {
            Projectile kept = lastThreat;
            return kept != null && !kept.isRemoved() && kept.level() == level() && incoming(kept, player) ? kept : null;
        }
        threatScanAt = tickCount;
        Projectile best = null;
        double bestD = Double.MAX_VALUE;
        for (Projectile p : level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(8))) {
            if (!incoming(p, player)) continue;
            double d = p.distanceToSqr(this);
            if (d < bestD) { bestD = d; best = p; }
        }
        lastThreat = best;
        return best;
    }

    /** A projectile of somebody else that passes within 2.2 blocks of the owner in the next 20 ticks. */
    private static boolean incoming(Projectile p, ServerPlayer player) {
        Entity shooter = p.getOwner();
        if (shooter == player || shooter instanceof WingmanEntity) return false;
        Vec3 c = player.getBoundingBox().getCenter();
        Vec3 v = p.getDeltaMovement();
        double v2 = v.lengthSqr();
        if (v2 < 0.01) return false;                                   // lying still (an arrow in the ground)
        Vec3 rel = c.subtract(p.position());
        double t = rel.dot(v) / v2;                                    // ticks to the closest approach
        if (t <= 0 || t > 20) return false;
        return p.position().add(v.scale(t)).distanceToSqr(c) <= 2.2 * 2.2;
    }

    private boolean fultonCandidate(@Nullable LivingEntity e, ServerPlayer player) {
        if (e == null || !e.isAlive() || e.level() != level() || e instanceof Player || e instanceof WingmanEntity) return false;
        if (e.isPassenger() || e.isVehicle() || e.getId() == lastFulton) return false;
        if (e.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES) || e.getType().is(FULTON_IMMUNE)) return false;
        if (e.getBbWidth() > 1.6F || e.getBbHeight() > 2.8F || e.distanceToSqr(player) > LEASH * LEASH) return false;
        if (e instanceof net.minecraft.world.entity.OwnableEntity own && own.getOwnerUUID() != null && !player.getUUID().equals(own.getOwnerUUID())) return false;
        return !(e instanceof Enemy) || e.getHealth() <= e.getMaxHealth() * 0.5F;   // a hostile mob must be weakened first
    }

    @Nullable
    private LivingEntity pickFulton(ServerPlayer player) {
        LivingEntity marked = player.getLastHurtMob();
        if (marked != null && player.tickCount - player.getLastHurtMobTimestamp() < 100 && fultonCandidate(marked, player)) return marked;
        if (!player.isCrouching()) return null;
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getViewVector(1.0F).scale(24));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
            e -> e instanceof LivingEntity && !e.isSpectator(), 24 * 24);
        return hit != null && hit.getEntity() instanceof LivingEntity l && fultonCandidate(l, player) ? l : null;
    }

    // ---- combat (0.1.8.4 rework)
    //
    // Weapon kinds: a SlashBlade: Resharped blade runs its real combos and Slash Arts
    // (SlashBladeCombat); a ranged weapon (bow, crossbow, trident, any projectile weapon, and the
    // energy guns through IWeaponItem#fireFrom) keeps a stand-off band around the target, circles
    // it and fires with a lead on the target's motion; anything else is a melee weapon used with
    // hit-and-run passes. All kinds turn smoothly, keep apart from the other Wingmen, and never
    // shoot through the owner.

    enum WeaponKind { MELEE, BLADE, RANGED }

    /** Weapon kind of the main hand. */
    WeaponKind weaponKind() {
        ItemStack w = getMainHandItem();
        if (SlashBladeCombat.isBlade(w)) return WeaponKind.BLADE;
        var item = w.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun && gun.isRanged()) return WeaponKind.RANGED;
        if (item instanceof net.minecraft.world.item.ProjectileWeaponItem || item instanceof net.minecraft.world.item.TridentItem) return WeaponKind.RANGED;
        return WeaponKind.MELEE;
    }

    private void engage(LivingEntity enemy, ServerPlayer player) {
        switch (weaponKind()) {
            case RANGED -> engageRanged(enemy, player);
            case BLADE -> engageBlade(enemy, player);
            default -> engageMelee(enemy, player);
        }
    }

    /** Melee: close in on the predicted position, strike, then peel off sideways and come back. */
    private void engageMelee(LivingEntity enemy, ServerPlayer player) {
        Vec3 aim = enemy.position().add(0, enemy.getBbHeight() * 0.5, 0);
        faceSmooth(enemy.getEyePosition(), 35);
        if (retreatTicks > 0) {                                   // peel off: sideways and up, then return
            retreatTicks--;
            Vec3 side = sideOf(enemy).scale(3.2).add(0, 1.6, 0);
            steer(aim.add(side), COMBAT, player);
            return;
        }
        // 0.1.9.1: come in from the flank / behind the target's back while far, straight in the last blocks
        Vec3 approach = aim.add(enemy.getDeltaMovement().scale(4));
        if (distanceToSqr(enemy) > 4.5 * 4.5) {
            Vec3 look = enemy.getViewVector(1.0F);
            approach = approach.add(new Vec3(-look.x, 0, -look.z).scale(1.5)).add(sideOf(enemy).scale(1.5));
        }
        steer(approach, COMBAT, player);
        armElectric();                                          // 0.1.10.4: switch the nanosaber on
        if (attackCooldown == 0 && isWithinMeleeAttackRange(enemy)) {
            swing(InteractionHand.MAIN_HAND);
            doHurtTarget(enemy);
            double speed = Math.max(0.5, getAttributeValue(Attributes.ATTACK_SPEED));
            attackCooldown = Mth.ceil(20.0 / speed);
            // a creeper is thrown away from the owner
            if (enemy instanceof net.minecraft.world.entity.monster.Creeper) {
                Vec3 away = enemy.position().subtract(player.position());
                enemy.knockback(1.6, -away.x, -away.z);
            }
            // 0.1.9.1 hit and away: a thruster boost out of reach, then a new pass from the other side
            boost(position().subtract(enemy.position()).add(sideOf(enemy).scale(0.8)));
            retreatTicks = 10;
            strafeSign = -strafeSign;
        }
    }

    // ---- electric weapons (0.1.10.4)

    /** Ticks without a target; the nanosaber switches off after {@link #DISARM_TICKS}. */
    private int idleTicks;
    private static final int DISARM_TICKS = 60;

    /**
     * Melee hit with the held weapon. Mob#doHurtTarget only reads the attack damage attribute and
     * never calls the item. The electric melee weapons add their damage in
     * {@code Item#getAttackDamageBonus} (nanosaber 20 when on, chainsaw 9, quantum sword +12.5 in
     * hyper mode) and pay their charge in {@code Item#hurtEnemy}, so in a Wingman's hand they hit
     * for 1 and never drew power. For an SI electric tool the hit now works as for a player: the
     * bonus is added (enchantments still apply), and after a hit the weapon's hurtEnemy runs (it pays
     * the charge and switches off when empty). Other weapons keep the vanilla mob attack.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        ItemStack weapon = getMainHandItem();
        if (!(weapon.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_tool_elc tool)) return super.doHurtTarget(target);
        float base = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource source = damageSources().mobAttack(this);
        float damage = base + tool.getAttackDamageBonus(target, base, source);
        if (level() instanceof ServerLevel world) damage = net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(world, weapon, target, source, damage);
        boolean hit = target.hurt(source, damage);
        if (hit) {
            float knock = getKnockback(target, source);
            if (knock > 0.0F && target instanceof LivingEntity living) {
                living.knockback(knock * 0.5F, Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
                setDeltaMovement(getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }
            if (level() instanceof ServerLevel world) net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(world, target, source);
            if (target instanceof LivingEntity living) tool.hurtEnemy(weapon, living, this);
            setLastHurtMob(target);
        }
        return hit;
    }

    /** Switches a held nanosaber on for a fight (it needs its activation charge, as for a player). */
    private void armElectric() {
        idleTicks = 0;
        ItemStack weapon = getMainHandItem();
        if (weapon.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber saber
                && !com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber.isActive(weapon)
                && saber.hasEnoughEnergy(weapon, com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber.ACTIVATE_COST)) {
            com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber.setActive(weapon, true);
            playSound(mio_icif_sounds.NANO_HF_SYNC.get(), 0.6F, 1.3F);
        }
    }

    /** Server tick: a nanosaber goes off again {@link #DISARM_TICKS} ticks after the last fight. */
    private void disarmIdle() {
        if (target != null) { idleTicks = 0; return; }
        if (++idleTicks != DISARM_TICKS) return;
        ItemStack weapon = getMainHandItem();
        if (weapon.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber
                && com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber.isActive(weapon)) {
            com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber.setActive(weapon, false);
        }
    }

    /**
     * SlashBlade: dash in with the rapid slash from 3.5 to 9 blocks, chain the combo in reach
     * (one input every 5 ticks, the timing window of the combo), release a Slash Art every
     * {@link #ARTS_INTERVAL} ticks of contact. The blade's own tick actions do the cutting.
     */
    private void engageBlade(LivingEntity enemy, ServerPlayer player) {
        Vec3 aim = enemy.position().add(0, enemy.getBbHeight() * 0.5, 0);
        faceSmooth(enemy.getEyePosition(), 45);
        double d = distanceTo(enemy);
        if (dashTicks > 0) {                                      // rapid slash: a straight burst through the target
            dashTicks--;
            Vec3 to = aim.subtract(position());
            if (to.lengthSqr() > 1.0E-4) setDeltaMovement(to.normalize().scale(1.6));
        } else {
            steer(aim.add(enemy.getDeltaMovement().scale(3)).add(sideOf(enemy).scale(0.8)), COMBAT, player);
        }
        if (attackCooldown > 0) return;
        if (artsCooldown == 0 && d < 7 && hasLineOfSight(enemy)) {
            if (SlashBladeCombat.slashArts(this, enemy) != null) {
                artsCooldown = ARTS_INTERVAL;
                attackCooldown = 20;
                boost(position().subtract(enemy.position()));     // 0.1.9.1: break away after the art
                return;
            }
        }
        if (d > 3.5 && d < 9 && hasLineOfSight(enemy)) {
            if (SlashBladeCombat.attack(this, enemy, SlashBladeCombat.Command.FORWARD, SlashBladeCombat.Command.SPRINT) != null) {
                dashTicks = 5;
                attackCooldown = 12;
                return;
            }
        }
        if (d <= 3.5) {
            if (SlashBladeCombat.attack(this, enemy) != null) {
                swing(InteractionHand.MAIN_HAND);
                attackCooldown = 5;
            } else if (isWithinMeleeAttackRange(enemy)) {        // bridge off: plain sword
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(enemy);
                attackCooldown = Mth.ceil(20.0 / Math.max(0.5, getAttributeValue(Attributes.ATTACK_SPEED)));
            }
        }
    }

    /**
     * Ranged: hold the stand-off band (from the weapon, 8 to 16 blocks for a bow), 3 to 5 blocks
     * above the target, circling it; fire with a lead when the line is clear and the owner is not
     * in it. A blocked line makes the Wingman climb and swap the circling direction.
     */
    private void engageRanged(LivingEntity enemy, ServerPlayer player) {
        ItemStack weapon = getMainHandItem();
        double[] band = rangeBand(weapon);
        // 0.1.9.1: kiting - a hostile inside the danger radius triggers a boost away
        Mob danger = WingmanTactics.danger(this, Math.max(3.5, band[0] * 0.55));
        if (danger != null && boostCooldown == 0) {
            boost(position().subtract(danger.position()));
            station = null;
        }
        // 0.1.9.1: tactical firing station (line of fire, flank, height, owner clear), re-planned every 8 ticks
        if (station == null || --stationTicks <= 0 || blockedTicks > 10) {
            station = WingmanTactics.station(this, enemy, player, band, blockedTicks > 10 ? null : station);
            stationTicks = STATION_TICKS;
            if (blockedTicks > 10) blockedTicks = 0;
        }
        // drift around the station so the drone never hangs still
        Vec3 stand = station.add(sideOf(enemy).scale(0.9 * Mth.sin(tickCount * 0.09F)));
        steer(stand, CRUISE * 1.15, player);
        Vec3 muzzle = getEyePosition();
        Vec3 aim = lead(enemy, muzzle, projectileSpeed(weapon));
        faceSmooth(aim, 40);
        boolean clear = hasLineOfSight(enemy) && !ownerInLine(player, muzzle, aim);
        if (!clear) {
            if (++blockedTicks > 20) { strafeSign = -strafeSign; blockedTicks = 0; }
            return;
        }
        blockedTicks = 0;
        if (attackCooldown > 0 || facingError(aim) > 12) return;
        if (splash(weapon) && enemy.distanceToSqr(player) < 5.5 * 5.5) return;   // no blast next to the owner
        int cooldown = fire(weapon, aim.subtract(muzzle), enemy);
        if (cooldown >= 0) entityData.set(DATA_SHOTS, entityData.get(DATA_SHOTS) + 1);   // muzzle flash on the clients
        if (cooldown >= 0) attackCooldown = Math.max(2, cooldown);
        else attackCooldown = 20;                                  // could not fire (no charge): try again later
    }

    /** Fires the held ranged weapon along {@code dir}. Returns the cooldown, or -1. */
    private int fire(ItemStack weapon, Vec3 dir, LivingEntity enemy) {
        var item = weapon.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun) {
            int cd = gun.fireFrom(weapon, this, dir, true);
            if (cd >= 0) swing(InteractionHand.MAIN_HAND);
            return cd;
        }
        if (item instanceof net.minecraft.world.item.TridentItem) {
            var trident = new net.minecraft.world.entity.projectile.ThrownTrident(level(), this, weapon.copyWithCount(1));
            trident.shoot(dir.x, dir.y, dir.z, 2.5F, 1.0F);
            trident.pickup = AbstractArrow.Pickup.DISALLOWED;
            level().addFreshEntity(trident);
            playSound(SoundEvents.TRIDENT_THROW.value(), 1.0F, 1.0F);
            return 30;
        }
        // bows, crossbows and other projectile weapons: an arrow from the weapon, gravity-compensated
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), 1.0F, weapon);
        double flat = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float speed = item instanceof CrossbowItem ? 3.15F : 2.4F;
        arrow.shoot(dir.x, dir.y + flat * (item instanceof CrossbowItem ? 0.05 : 0.11), dir.z, speed, 1.0F);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        level().addFreshEntity(arrow);
        playSound(item instanceof CrossbowItem ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT, 1.0F,
            1.0F / (getRandom().nextFloat() * 0.4F + 0.8F));
        return item instanceof CrossbowItem ? 22 : 16;
    }

    private static double[] rangeBand(ItemStack weapon) {
        var item = weapon.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun) return gun.engagementRange();
        if (item instanceof CrossbowItem) return new double[]{9, 16};
        if (item instanceof net.minecraft.world.item.TridentItem) return new double[]{6, 12};
        return new double[]{8, 16};
    }

    private static double projectileSpeed(ItemStack weapon) {
        var item = weapon.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun) return gun.projectileSpeed();
        if (item instanceof CrossbowItem) return 3.15;
        if (item instanceof net.minecraft.world.item.TridentItem) return 2.5;
        return 2.4;
    }

    /** True for a weapon whose shot explodes (rockets, the plasma gun). */
    private static boolean splash(ItemStack weapon) {
        return weapon.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_rocket_launcher
            || weapon.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_electric_plasma_gun;
    }

    /** Aim point with a lead on the target's motion (speed 0: the current position). */
    private static Vec3 lead(LivingEntity enemy, Vec3 from, double speed) {
        Vec3 at = enemy.position().add(0, enemy.getBbHeight() * 0.55, 0);
        if (speed <= 0) return at;
        Vec3 v = enemy.getDeltaMovement();
        double t = 0;
        for (int i = 0; i < 3; i++) t = at.add(v.scale(t)).distanceTo(from) / speed;   // fixed-point on the flight time
        return at.add(v.x * t, Math.max(-0.5, v.y) * t, v.z * t);
    }

    /** True if the owner stands near the shot line from {@code from} to {@code to}. */
    private static boolean ownerInLine(ServerPlayer owner, Vec3 from, Vec3 to) {
        Vec3 seg = to.subtract(from);
        double len2 = seg.lengthSqr();
        if (len2 < 1.0E-4) return false;
        Vec3 c = owner.getBoundingBox().getCenter();
        double t = Mth.clamp(c.subtract(from).dot(seg) / len2, 0, 1);
        return from.add(seg.scale(t)).distanceToSqr(c) < 1.3 * 1.3;
    }

    /** Horizontal unit vector to the side of the target the Wingman circles on. */
    private Vec3 sideOf(LivingEntity enemy) {
        Vec3 to = enemy.position().subtract(position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        if (flat.lengthSqr() < 1.0E-4) return new Vec3(strafeSign, 0, 0);
        flat = flat.normalize();
        return new Vec3(-flat.z * strafeSign, 0, flat.x * strafeSign);
    }

    /** Degrees between the current look and the direction to {@code at}. */
    private double facingError(Vec3 at) {
        Vec3 d = at.subtract(getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        return Math.abs(Mth.wrapDegrees(yaw - getYRot())) + Math.abs(pitch - getXRot());
    }

    /** Turns toward {@code at} by at most {@code maxStep} degrees per tick (no snapping). */
    private void faceSmooth(Vec3 at, float maxStep) {
        Vec3 d = at.subtract(getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        float dy = Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -maxStep, maxStep);
        setYaw(getYRot() + dy);
        setXRot(getXRot() + Mth.clamp(pitch - getXRot(), -maxStep, maxStep));
    }

    /** The mob the owner hit in the last 2 s, if it is a valid target (0.1.8.4: owner focus). */
    @Nullable
    private LivingEntity ownerFocus(ServerPlayer player) {
        LivingEntity marked = player.getLastHurtMob();
        if (marked != null && player.tickCount - player.getLastHurtMobTimestamp() < 40 && valid(marked, player, LEASH)) return marked;
        // 0.1.10.1 Fleet Datalink synergy: the creature in the owner's sights
        LivingEntity sighted = com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergies.mark(player);
        return sighted != null && valid(sighted, player, LEASH * 2) ? sighted : null;
    }

    /** Nearest hostile mob within {@code range} of the owner that hunts the owner. */
    @Nullable
    private LivingEntity hunterOf(ServerPlayer player, double range) {
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Mob mob : level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range), m -> m instanceof Enemy && m.getTarget() == player)) {
            if (!valid(mob, player, range + 2)) continue;
            double d = mob.distanceToSqr(player);
            if (d < bestD) { bestD = d; best = mob; }
        }
        return best;
    }

    /** 0.1.10.8: the separation push, measured every 2nd tick and reused in between. */
    private Vec3 separationPush = Vec3.ZERO;
    private int separationAt = -10;

    /** Push away from the other Wingmen closer than 2.2 blocks, so the squad does not stack up. */
    private Vec3 separation() {
        if (tickCount >= separationAt && tickCount - separationAt < 2) return separationPush;
        separationAt = tickCount;
        separationPush = measureSeparation();
        return separationPush;
    }

    private Vec3 measureSeparation() {
        Vec3 push = Vec3.ZERO;
        for (WingmanEntity other : level().getEntitiesOfClass(WingmanEntity.class, getBoundingBox().inflate(2.2), w -> w != this)) {
            Vec3 away = position().subtract(other.position());
            double d = away.length();
            if (d < 1.0E-3) away = new Vec3(getId() % 2 == 0 ? 1 : -1, 0, 0);
            push = push.add(away.normalize().scale((2.2 - Math.min(2.2, d)) * 0.08));
        }
        return push;
    }

    /** Escort place of this Wingman: behind and to the side of the owner, at shoulder height. */
    private Vec3 formation(ServerPlayer player) {
        double[][] offsets = {{-1.6, 1.2, -1.8}, {1.6, 1.2, -1.8}, {-2.9, 1.7, -0.6}, {2.9, 1.7, -0.6}};
        double[] o = offsets[Math.floorMod(slot, offsets.length)];
        // 0.1.8.4: tighter guard when the owner is below 40 % health
        double k = player.getHealth() < player.getMaxHealth() * 0.4F ? 0.6 : 1.0;
        o = new double[]{o[0] * k, o[1], o[2] * k};
        double yaw = Math.toRadians(player.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);       // forward
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);      // right
        return player.position().add(rx * o[0] + fx * o[2], o[1], rz * o[0] + fz * o[2]);
    }

    /** Steers the velocity to {@code goal} with a limited acceleration; climbs over obstacles. */
    private void steer(Vec3 goal, double speed, @Nullable ServerPlayer player) {
        Vec3 to = goal.subtract(position());
        double dist = to.length();
        Vec3 want = dist < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(speed, dist * 0.3) / dist);
        want = WingmanTactics.avoid(this, want.add(separation()));   // 0.1.9.1: look ahead, climb or slide past walls
        Vec3 v = getDeltaMovement();
        Vec3 dv = want.subtract(v);
        if (dv.lengthSqr() > ACCEL * ACCEL) dv = dv.normalize().scale(ACCEL);
        v = v.add(dv);
        if (horizontalCollision) v = v.add(0, 0.3, 0);
        setDeltaMovement(v);
        if (target == null) {
            if (v.horizontalDistanceSqr() > 0.02) faceSmooth(getEyePosition().add(v.x, 0, v.z), 20);
            else if (player != null) setYaw(getYRot() + Mth.clamp(Mth.wrapDegrees(player.getYRot() - getYRot()), -15, 15));
        }
    }

    private void face(Vec3 at) {
        Vec3 d = at.subtract(getEyePosition());
        setYaw((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
    }

    private void setYaw(float yaw) {
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    /**
     * 0.1.9.1 thruster boost: an impulse along {@code dir} (flattened, a little upwards), with a
     * flare on the clients and a short burst sound. Has a cooldown.
     */
    private void boost(Vec3 dir) {
        if (boostCooldown > 0) return;
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() < 1.0E-4) flat = new Vec3(strafeSign, 0, 0);
        Vec3 impulse = WingmanTactics.avoid(this, flat.normalize().scale(1.1).add(0, 0.38, 0));
        setDeltaMovement(getDeltaMovement().scale(0.3).add(impulse));
        boostCooldown = BOOST_COOLDOWN;
        entityData.set(DATA_BOOSTS, entityData.get(DATA_BOOSTS) + 1);
        level().playSound(null, getX(), getY(), getZ(), mio_icif_sounds.WINGMAN_BOOST.get(), SoundSource.NEUTRAL, 0.7F,
            0.95F + getRandom().nextFloat() * 0.1F);
    }

    /** Publishes the target for the lock-on effect; a new lock beeps once. */
    private void syncTarget(ServerLevel world) {
        int id = target != null && target.isAlive() && state() == ACTIVE ? target.getId() : -1;
        if (id == entityData.get(DATA_TARGET)) return;
        entityData.set(DATA_TARGET, id);
        station = null;
        if (id >= 0) world.playSound(null, getX(), getY(), getZ(), mio_icif_sounds.WINGMAN_LOCK.get(), SoundSource.NEUTRAL, 0.5F, 1.0F);
    }

    private void catchUp(ServerPlayer player) {
        Vec3 at = formation(player).add(0, 2, 0);
        teleportTo(at.x, at.y, at.z);
        setDeltaMovement(Vec3.ZERO);
    }

    // ---- return home
    /** Starts the flight home. A message, if any, goes to the owner. */
    void startReturn(@Nullable Component message) {
        if (state() == RETURNING || secured) return;
        dropCargo();
        clearOrder();
        setState(RETURNING);
        if (message != null && level() instanceof ServerLevel world && owner != null) {
            ServerPlayer player = world.getServer().getPlayerList().getPlayer(owner);
            if (player != null) player.displayClientMessage(message, true);
        }
    }

    private void tickReturn(ServerLevel world) {
        mio_icif_armory home = loadedArmory(world);
        boolean near = home != null && home.getLevel() == world && home.launchTop().distanceToSqr(position()) < 160 * 160;
        if (near) {
            Vec3 top = home.launchTop();
            steer(top, CRUISE, null);
            if (top.distanceToSqr(position()) < 2.0 * 2.0 || stateTicks > 400) {
                world.playSound(null, top.x, top.y, top.z, mio_icif_sounds.ARMORY_LATCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                secure();
                discard();
            }
            return;
        }
        // far away or in a different dimension: climb out, then the Armory recalls the suit
        setDeltaMovement(getDeltaMovement().scale(0.8).add(0, 0.12, 0));
        if (stateTicks > 30) {
            world.sendParticles(ParticleTypes.FLASH, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
            world.playSound(null, getX(), getY(), getZ(), mio_icif_sounds.ARMORY_LAUNCH.get(), SoundSource.NEUTRAL, 1.0F, 1.2F);
            secure();
            discard();
        }
    }

    /** Lets go of a Fulton cargo in the air; the cargo gets 10 s of Slow Falling. */
    private void dropCargo() {
        for (Entity cargo : getPassengers()) {
            if (cargo instanceof LivingEntity living) {
                living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 200));
            }
        }
        ejectPassengers();
    }

    /**
     * Puts every item back: the Armory first (suit {@link #set}), then the owner inventory, then
     * the ground at this Wingman. Runs at most once. The Armory chunk can load for it.
     * 0.1.10.5: closes the sortie, so a saved copy of this Wingman cannot bring the items back.
     */
    void secure() {
        if (secured || !(level() instanceof ServerLevel world)) return;
        secured = true;
        WingmanSorties ledger = WingmanSorties.get(world.getServer());
        if (ledger != null) ledger.close(sortie);
        try {
            dropCargo();
        } catch (RuntimeException e) {
            com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.error("Wingman {}: dropping the cargo failed", getUUID(), e);
        }
        mio_icif_armory home = null;
        try {
            if (armory != null) {
                ServerLevel level = world.getServer().getLevel(armory.dimension());
                if (level != null && level.isInWorldBounds(armory.pos()) && level.getBlockEntity(armory.pos()) instanceof mio_icif_armory a) home = a;
            }
        } catch (RuntimeException e) {
            // the items go to the owner or the ground instead
            com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.error("Wingman {}: the Armory at {} cannot be reached", getUUID(), armory, e);
        }
        ServerPlayer player = owner == null ? null : world.getServer().getPlayerList().getPlayer(owner);
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            ItemStack stack = getItemBySlot(p.slot);
            if (stack.isEmpty()) continue;
            setItemSlot(p.slot, ItemStack.EMPTY);
            ItemStack rest = home != null ? home.store(set, p, stack) : stack;
            if (!rest.isEmpty() && player != null) {
                player.getInventory().add(rest);
            }
            if (!rest.isEmpty()) spawnAtLocation(rest);
        }
        if (home != null) home.markStorageChanged();
    }

    /**
     * 0.1.10.5: the sortie check when this Wingman enters a level (server; see
     * {@link WingmanSorties}). False for a copy: a captured Wingman released after its suit went
     * home, or a second Wingman of a sortie that is in flight. A copy loses its items without a
     * drop and must not enter. A Wingman saved before 0.1.10.5 gets a token now.
     */
    boolean admit() {
        if (secured || !(level() instanceof ServerLevel world)) return !secured;
        WingmanSorties ledger = WingmanSorties.get(world.getServer());
        if (ledger == null) return true;
        if (sortie == null) {
            sortie = UUID.randomUUID();
            ledger.adopt(sortie, getUUID());
            return true;
        }
        WingmanSorties.Status status = ledger.status(sortie);
        boolean twin = WingmanService.flying(owner, sortie, this);
        if (status == WingmanSorties.Status.CLOSED || twin) {
            voidCopy(world, twin ? "a Wingman of this sortie is in flight" : "the suit of this sortie went home");
            return false;
        }
        if (status == WingmanSorties.Status.UNKNOWN) ledger.adopt(sortie, getUUID());
        else ledger.rebind(sortie, getUUID());
        return true;
    }

    /** Empties a copy without a drop: its items exist elsewhere (the Armory, the first Wingman). */
    private void voidCopy(ServerLevel world, String reason) {
        secured = true;                 // remove() and die() must not send these items home
        setSilent(true);
        for (EquipmentSlot s : EquipmentSlot.values()) setItemSlot(s, ItemStack.EMPTY);
        com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.warn(
            "Wingman {} (owner {}, sortie {}) is a copy and was turned away without its items: {}", getUUID(), owner, sortie, reason);
        ServerPlayer player = owner == null ? null : world.getServer().getPlayerList().getPlayer(owner);
        if (player != null) player.displayClientMessage(Component.translatable("message.mio_icif.wingman.copy_removed"), false);
    }

    /**
     * 0.1.10.5: a tick of this Wingman threw ({@link WingmanTickGuard}). On the server the suit
     * goes home and the Wingman leaves, instead of a crash that the world would hit again at
     * every load. On a client nothing happens (the server decides).
     */
    void fault(Throwable error) {
        if (level().isClientSide || isRemoved()) return;
        if (level() instanceof ServerLevel world && owner != null) {
            ServerPlayer player = world.getServer().getPlayerList().getPlayer(owner);
            if (player != null) player.displayClientMessage(Component.translatable("message.mio_icif.wingman.fault"), false);
        }
        discard();   // remove() sends the suit home
    }

    // ------------------------------------------------------------------ damage, death, removal
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (protectedReturn && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        Entity attacker = source.getEntity();
        if (attacker != null && owner != null && (owner.equals(attacker.getUUID())
                || attacker instanceof WingmanEntity w && owner.equals(w.owner))) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !secured) {
            // a fatal hit is not fatal: the suit breaks off and limps home
            setHealth(1.0F);
            protectedReturn = true;
            startReturn(Component.translatable("message.mio_icif.wingman.damaged"));
            return;
        }
        secure();
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        try {
            if (!level().isClientSide && reason.shouldDestroy()) secure();
        } catch (RuntimeException e) {
            // 0.1.10.5: the removal itself must go through, or the error comes back every tick
            com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.error("Wingman {}: sending the suit home failed", getUUID(), e);
        } finally {
            super.remove(reason);
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) { }

    // ------------------------------------------------------------------ movement
    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance()) move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    public boolean isPushable() { return false; }

    /** No portals: the owner registry and the item return work in one dimension. */
    @Override
    public boolean canUsePortal(boolean allowPassengers) { return false; }

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean canTakeItem(ItemStack stack) { return false; }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() { return null; }

    @Override
    protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty() && state() == ACTIVE; }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0, -passenger.getBbHeight() - 0.15, 0);   // the cargo hangs below
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (owner != null && (owner.equals(other.getUUID()) || other instanceof WingmanEntity w && owner.equals(w.owner))) return true;
        return super.isAlliedTo(other);
    }

    /**
     * The owner crouches and uses an empty hand on the Wingman: it returns home. 0.1.10.5: an item
     * used on a Wingman does nothing and goes no further (no {@code interactLivingEntity}), so mob
     * capture tools cannot take it (see also {@link WingmanService#onInteract}).
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (owner != null && owner.equals(player.getUUID()) && player.isSecondaryUseActive() && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide) startReturn(Component.translatable("message.mio_icif.wingman.recalled", 1));
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.CONSUME;
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------ client effects
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && isAlive()) {
            // boot thrusters: two jets, stronger when the drone climbs or moves fast
            Vec3 v = getDeltaMovement();
            double yaw = Math.toRadians(yBodyRot);
            double rx = -Math.cos(yaw) * 0.13, rz = -Math.sin(yaw) * 0.13;
            for (int side = -1; side <= 1; side += 2) {
                level().addParticle(ArmoryRegistry.THRUST.get(), getX() + rx * side, getY() + 0.05, getZ() + rz * side,
                    -v.x * 0.3, -0.22 - Math.max(0, v.y) * 0.4, -v.z * 0.3);
            }
            if (tickCount % 3 == 0 && v.lengthSqr() > 0.3) level().addParticle(ArmoryRegistry.TRAIL.get(), getX(), getY() + 0.9, getZ(), 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ save / load
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
        if (armory != null) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, armory).result().ifPresent(t -> tag.put("Armory", t));
        tag.putInt("Set", set);
        if (sortie != null) tag.putUUID("Sortie", sortie);   // 0.1.10.5
        tag.putByte("Mode", entityData.get(DATA_MODE));
        tag.putByte("State", state());
        tag.putBoolean("Protected", protectedReturn);
        // 0.1.10.2 (absent in older saves: squad mode, no order); a focus order is not saved
        tag.putBoolean("Individual", individual);
        if (order.anchored() && orderPos != null) {
            tag.putByte("Order", (byte) order.ordinal());
            tag.putDouble("OrderX", orderPos.x);
            tag.putDouble("OrderY", orderPos.y);
            tag.putDouble("OrderZ", orderPos.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        armory = tag.contains("Armory") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("Armory")).result().orElse(null) : null;
        set = tag.getInt("Set");
        sortie = tag.hasUUID("Sortie") ? tag.getUUID("Sortie") : null;
        entityData.set(DATA_MODE, tag.getByte("Mode"));
        entityData.set(DATA_STATE, tag.getByte("State"));
        protectedReturn = tag.getBoolean("Protected");
        individual = tag.getBoolean("Individual");
        WingmanOrder saved = tag.contains("Order") ? WingmanOrder.byId(tag.getByte("Order")) : WingmanOrder.NONE;
        if (saved.anchored() && tag.contains("OrderX")) {
            order = saved;
            orderPos = new Vec3(tag.getDouble("OrderX"), tag.getDouble("OrderY"), tag.getDouble("OrderZ"));
        } else {
            order = WingmanOrder.NONE;
            orderPos = null;
        }
        setNoGravity(true);
    }

    // ------------------------------------------------------------------ owner registry
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!level().isClientSide) WingmanService.track(this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        if (!level().isClientSide) WingmanService.untrack(this);
    }

    /** Live list of the pieces this Wingman wears, in Armory column order (for the console). */
    List<ItemStack> items() {
        List<ItemStack> out = new java.util.ArrayList<>();
        for (ArmoryPiece p : ArmoryPiece.COLUMNS) out.add(getItemBySlot(p.slot));
        return out;
    }
}
