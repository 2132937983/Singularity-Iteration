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
 *       player who wears it (0.1.11.39: the suit modules, such as the deflector, work only for players). The base values are the player base values (20 health, attack
 *       damage 1, attack speed 4).</li>
 *   <li>The items never disappear. Recall, owner logout, owner in a different dimension, low
 *       power and a removal (for example {@code /kill}) put the items back into the Armory. If
 *       the Armory has no free position, the items go to the owner inventory, else they drop
 *       at the Wingman.</li>
 *   <li>A hit that would kill the Wingman sets it to 1 health. It then returns to the Armory
 *       and is invulnerable on the way. Only void damage and {@code /kill} remove it at once.</li>
 *   <li>The owner and the other Wingmen of the owner cannot hurt it.</li>
 *   <li>Upkeep: {@link WingmanConfig#upkeepEuPerSecond()} EU per second (default 160). The electric pieces of the suit pay
 *       first (the full-set pool applies), then (0.1.11.41) the reserve the Armory charged at deployment, then the Armory
 *       when it is loaded; the unused reserve goes back to the Armory. After
 *       {@link WingmanConfig#lowPowerSeconds()} seconds without power (default 5), the Wingman returns.</li>
 * </ul>
 */
@SuppressWarnings("null")
public class WingmanEntity extends Mob {

    /** States; 2 was LIFTING (the Fulton lift, removed in 0.1.11.43): a saved one loads as ACTIVE. */
    public static final byte ACTIVE = 0, RETURNING = 1;

    private static final double CRUISE = 0.9, COMBAT = 1.15, ACCEL = 0.14;
    /** Follow-range attribute (the live target ranges are {@link WingmanConfig#scanRange()} and {@link WingmanConfig#leash()}). */
    private static final double SCAN_RANGE = 24;
    /**
     * Distance to the owner beyond which the Wingman jumps to its place (0.1.11.50, decision four A: 96, was 64; at high
     * speed it flies the owner's velocity and stays close, the jump is the last resort, and it lands with that velocity).
     */
    static final double CATCH_UP = 96;
    /** 0.1.11.50: farther than this from its place and not closing in for STUCK_TICKS: stuck behind terrain, it jumps. */
    static final double STUCK_DISTANCE = 16;
    static final int STUCK_TICKS = 60;

    private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.BYTE);
    /** 0.1.9.1 client effects: locked target (entity id, -1 none), shot counter, thruster boost counter. */
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHOTS = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_BOOSTS = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.INT);
    /** 0.1.10.11: standing on the ground while idle (no thrusters, standing pose). */
    private static final EntityDataAccessor<Boolean> DATA_LANDED = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.BOOLEAN);
    /** 0.1.10.11: blade slash counter (high bits) and kind (low 2 bits: {@link #SLASH_COMBO}, {@link #SLASH_DASH}, {@link #SLASH_ARTS}). */
    private static final EntityDataAccessor<Integer> DATA_SLASH = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.INT);
    /** 0.1.11.46 (motion audit M2): ticks until the held ranged weapon fires again, set at each shot (the draw, the reload). */
    private static final EntityDataAccessor<Integer> DATA_FIRE_CD = SynchedEntityData.defineId(WingmanEntity.class, EntityDataSerializers.INT);
    public static final int SLASH_COMBO = 0, SLASH_DASH = 1, SLASH_ARTS = 2;

    // server state (saved)
    @Nullable private UUID owner;
    @Nullable private GlobalPos armory;
    private int set;
    /** 0.1.10.5: the token of this sortie ({@link WingmanSorties}); null in a save of an older version. */
    @Nullable private UUID sortie;
    /** 0.1.11.41 (Wingman audit W2, decision 1 C): EU of upkeep charged by the Armory at deployment (saved); see {@link #upkeep}. */
    private long reserve;
    /** 0.1.11.41: the Armory paid (part of) the last upkeep (not saved). */
    private boolean armoryPaid;
    // server state (not saved)
    @Nullable private LivingEntity target;
    private int attackCooldown, deflectCooldown, lowPowerSeconds, stateTicks, slot, slotCount = 1;
    // 0.1.8.4 combat state: circling direction (+1/-1), melee peel-off, blade dash, Slash Art timer, blocked line
    private int strafeSign = 1, retreatTicks, artsCooldown, blockedTicks;
    /** Ticks between two Slash Arts of a blade-wielding Wingman. */
    private static final int ARTS_INTERVAL = 200;
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
        // 0.1.11.52 (gaze and Iron Man phase P0, decision one A): the Wingman turns its head itself. The vanilla look control
        // ran after its AI every tick, pulled the head 10 degrees back to the body and set the pitch to 0: in a fight the
        // head swung between the enemy and the body (a "look around") and never looked up or down at it.
        this.lookControl = new net.minecraft.world.entity.ai.control.LookControl(this) {
            @Override
            public void tick() { }
        };
    }

    /**
     * 0.1.11.52 (P0): the body is turned by the Wingman ({@link #setYaw}: it follows the head by at most {@link #BODY_TURN}
     * degrees per tick, the neck at most {@link #NECK} degrees); the vanilla body control (which turned the body to the
     * head after 10 still ticks and to the motion when moving) is not used. On the client the body keeps the synced yaw.
     */
    @Override
    protected float tickHeadTurn(float yRot, float animStep) {
        yBodyRot = getYRot();
        return animStep;
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
        builder.define(DATA_LANDED, false);
        builder.define(DATA_SLASH, 0);
        builder.define(DATA_FIRE_CD, 20);
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
    /** 0.1.10.11: true while the Wingman stands on the ground (idle landing). */
    public boolean landed() { return entityData.get(DATA_LANDED); }
    public int set() { return set; }

    /** 0.1.11.58: the tick of this Wingman's last callout ({@link WingmanCallouts}). */
    long lastCallout = Long.MIN_VALUE / 2;
    private boolean toldReserveLow;

    /** 0.1.11.58: the owner when online (server). */
    @Nullable
    ServerPlayer ownerPlayer() {
        return owner == null || !(level() instanceof ServerLevel world) ? null : world.getServer().getPlayerList().getPlayer(owner);
    }
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
        deathGuard = false;   // 0.1.11.43: an order of the owner ends the guard of the place of death
        this.order = order;
        this.orderPos = order.anchored() ? (pos != null ? pos : position()) : null;
        this.orderTarget = order == WingmanOrder.FOCUS ? focus.getId() : -1;
        orderAge = 0;
        target = order == WingmanOrder.FOCUS ? focus : null;
        station = null;
    }

    /** 0.1.10.2 "form up": the order ends and the tactical mode applies again. */
    public void clearOrder() {
        deathGuard = false;
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
        // 0.1.10.13: no arm swing per shot (it restarted every few ticks: the jittering arm); the shot
        // counter drives the muzzle flash and the recoil of the arms on the clients
        entityData.set(DATA_SHOTS, entityData.get(DATA_SHOTS) + 1);
    }

    /**
     * 0.1.10.13: a swing that is still running is not restarted. Vanilla restarts a swing after half
     * of it, and the arm snapped back mid-cut; the next swing now waits for the end of the current one.
     * Runs on both sides (the client gets the swing as an animation packet).
     */
    @Override
    public void swing(InteractionHand hand, boolean updateSelf) {
        if (swinging) return;
        super.swing(hand, updateSelf);
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

    // ------------------------------------------------------------------ wear (0.1.11.42, Wingman audit W3, decision 2 C)
    //
    // A Wingman wears its gear as a player does: a hit on it wears the armor (a quarter of the damage, at least 1, as for a
    // player), a melee hit or a shot wears the weapon by 1. A piece is never worn out: the last point of durability is kept,
    // and when any piece is below WORN_FRACTION the Wingman flies home, where the Armory repairs it. Arrows stay free.
    // Before, a Wingman (a Mob) wore nothing: NeoForge wears the armor of players only.

    /** A piece below this fraction of its durability sends the Wingman home. */
    public static final float WORN_FRACTION = 0.10F;

    /**
     * True for a piece that wears: damageable, and not an electric item (an electric item spends charge instead, and
     * some show their charge as durability: an empty one would count as worn).
     */
    static boolean wears(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) return false;
        var item = stack.getItem();
        return !(item instanceof com.miophas.singularity_iteration.core.api.item.IBatteryItem
            || item instanceof com.miophas.singularity_iteration.core.api.item.IElectricArmorItem
            || item instanceof com.miophas.singularity_iteration.core.api.item.IElectricToolItem);
    }

    /** Wears {@code stack} (in {@code slot}) by {@code amount}, keeping its last point of durability. */
    void wear(ItemStack stack, EquipmentSlot slot, int amount) {
        if (amount <= 0 || !wears(stack) || level().isClientSide) return;
        int left = stack.getMaxDamage() - stack.getDamageValue();
        int dmg = Math.min(amount, left - 1);
        if (dmg > 0) stack.hurtAndBreak(dmg, this, slot);
        // 0.1.11.58 (P6): a piece that falls below DAMAGED_FRACTION is called
        float max = stack.getMaxDamage();
        if (left >= max * WingmanPose.DAMAGED_FRACTION && left - dmg < max * WingmanPose.DAMAGED_FRACTION)
            WingmanCallouts.say(this, WingmanCallouts.Kind.DAMAGED, stack.getHoverName());
        checkWorn();
    }

    /** The first worn piece (below {@link #WORN_FRACTION}), or empty. */
    static ItemStack wornPiece(Iterable<ItemStack> pieces) {
        for (ItemStack s : pieces) {
            if (!wears(s)) continue;
            if ((s.getMaxDamage() - s.getDamageValue()) < s.getMaxDamage() * WORN_FRACTION) return s;
        }
        return ItemStack.EMPTY;
    }

    /** A worn piece sends the Wingman home for repairs (once). */
    private void checkWorn() {
        if (state() == RETURNING) return;
        ItemStack worn = wornPiece(items());
        if (!worn.isEmpty()) {
            WingmanCallouts.say(this, WingmanCallouts.Kind.REPAIR, worn.getHoverName());
            startReturn(Component.translatable("message.mio_icif.wingman.worn", worn.getHoverName()));
        }
    }

    @Override
    protected void hurtArmor(DamageSource source, float damage) {
        wearArmor(source, damage, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    }

    @Override
    protected void hurtHelmet(DamageSource source, float damage) {
        wearArmor(source, damage, EquipmentSlot.HEAD);
    }

    private void wearArmor(DamageSource source, float damage, EquipmentSlot... slots) {
        if (damage <= 0) return;
        int amount = (int) Math.max(1.0F, damage / 4.0F);
        for (EquipmentSlot slot : slots) {
            ItemStack piece = getItemBySlot(slot);
            if (piece.getItem() instanceof net.minecraft.world.item.ArmorItem && piece.canBeHurtBy(source)) wear(piece, slot, amount);
        }
    }

    // ---- an energy weapon out of charge (0.1.11.42, decision 2 C)
    /** Tick until which a ranged Wingman whose energy weapon could not fire fights in melee. */
    private int dryUntil = -1;
    /** The owner was told about the empty weapon (once per empty spell). */
    private boolean toldEmpty;

    /** Tells the owner once that the weapon of this Wingman is out of charge. */
    private void tellEmpty(ItemStack weapon) {
        if (toldEmpty || !(level() instanceof ServerLevel world) || owner == null) return;
        toldEmpty = true;
        ServerPlayer player = world.getServer().getPlayerList().getPlayer(owner);
        if (player != null) player.displayClientMessage(Component.translatable("message.mio_icif.wingman.no_charge", weapon.getHoverName()), true);
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
        if (!getPassengers().isEmpty()) dropCargo();   // 0.1.11.43: a Fulton cargo of an older save is let go (slow falling)
        if (attackCooldown > 0) attackCooldown--;
        if (deflectCooldown > 0) deflectCooldown--;
        if (boostCooldown > 0) boostCooldown--;
        syncTarget(world);
        if (artsCooldown > 0) artsCooldown--;
        if (state() == ACTIVE) SlashBladeCombat.tick(this);   // a blade's combo actions run on the wielder's tick
        disarmIdle();                                          // 0.1.10.4: the nanosaber goes off after a fight
        ServerPlayer player = owner == null ? null : world.getServer().getPlayerList().getPlayer(owner);
        boolean ownerHere = player != null && player.isAlive() && player.level() == world && !player.isSpectator();
        // 0.1.11.43 (W4, decision 3 B): the owner died here: the squad guards the place instead of flying home
        boolean ownerDead = player != null && !player.isAlive() && player.level() == world && !player.isSpectator();
        if (ownerDead && state() == ACTIVE && !deathGuard) startDeathGuard(world, player);
        if (state() != RETURNING && !ownerHere && !(deathGuard && player != null && player.level() == world)) {
            startReturn(null);
        }
        if (tickCount % 20 == 0 && state() != RETURNING) upkeep(world, player);
        if (ownerHere) trackOwner(player);
        idleThisTick = false;
        try {
            switch (state()) {
                case RETURNING -> tickReturn(world);
                default -> {
                    if (deathGuard && tickDeathGuard(world, player, ownerHere)) return;
                    if (!ownerHere && swapPhase == SWAP_NONE && !rescuing()) return;
                    if (rescuing()) { tickRescue(player, ownerHere); return; }             // 0.1.11.56: an emergency rescue
                    if (swapPhase != SWAP_NONE) { tickSwap(player, ownerHere); return; }   // 0.1.11.54: a suit swap
                    // 0.1.10.2 quantum command: an order replaces the mode and the owner leash
                    if (order != WingmanOrder.NONE) { tickOrder(world, player); return; }
                    if (distanceToSqr(player) > CATCH_UP * CATCH_UP) catchUp(player);
                    switch (mode()) {
                        case OFFENSIVE -> tickOffensive(player);
                        case DISPERSAL -> tickDispersal(player);
                        case ESCORT -> tickEscort(world, player);
                    }
                }
            }
        } finally {
            // 0.1.10.11: anything but idling (a target, an order on the move, the way home) takes off
            if (!idleThisTick) {
                quietTicks = 0;
                if (landed()) takeOff();
            }
        }
    }

    // ---- the owner's death (0.1.11.43, Wingman audit W4, decision 3 B)
    //
    // When the owner dies in the dimension of the squad, each Wingman holds the place of death (a HOLD order there: it
    // engages what comes near, the dropped items included) instead of flying home. The guard ends when the owner, alive
    // again, comes within DEATH_GUARD_END blocks (the Wingman forms up), when the owner gives an order, or after
    // DEATH_GUARD_TICKS (the Wingman flies home). A respawn in another dimension or a logout sends it home as before.

    /** Ticks a Wingman guards the place of its owner's death (2 minutes). */
    static final int DEATH_GUARD_TICKS = 2400;
    /** Blocks from the place of death within which the returning owner ends the guard. */
    static final double DEATH_GUARD_END = 24;
    private boolean deathGuard;
    private long guardUntil;

    /** 0.1.11.43: true while this Wingman guards the place of its owner's death. */
    public boolean deathGuard() { return deathGuard; }

    private void startDeathGuard(ServerLevel world, ServerPlayer player) {
        order(WingmanOrder.HOLD, player.position(), null);
        deathGuard = true;
        guardUntil = world.getGameTime() + DEATH_GUARD_TICKS;
        if (slot == 0) player.displayClientMessage(Component.translatable("message.mio_icif.wingman.death_guard", DEATH_GUARD_TICKS / 1200), false);
    }

    /** One tick of the guard; true when it took the tick (the guard goes on or ended with the flight home). */
    private boolean tickDeathGuard(ServerLevel world, @Nullable ServerPlayer player, boolean ownerHere) {
        if (world.getGameTime() > guardUntil) {
            deathGuard = false;
            startReturn(Component.translatable("message.mio_icif.wingman.death_guard_over"));
            return true;
        }
        if (ownerHere && orderPos != null && player.position().distanceToSqr(orderPos) < DEATH_GUARD_END * DEATH_GUARD_END) {
            clearOrder();   // the owner is back: form up
            return false;
        }
        if (player == null || order == WingmanOrder.NONE) return !ownerHere;
        tickOrder(world, player);
        return true;
    }

    // ---- idle landing (0.1.10.11)
    //
    // With nothing to do for LAND_DELAY ticks (no target, no cargo) while the owner stands on the
    // ground (or at the post of a HOLD / arrived MOVE order), the Wingman sets down on the ground
    // below its post: thrusters off, standing pose, no upkeep change. It stays while the owner
    // stays within LAND_STAY blocks of where they stood; a target, an order, the owner walking off,
    // jumping into flight or gliding make it take off at once.

    private static final int LAND_DELAY = 40;
    /** Distance to the landing spot at which the Wingman stands on it (0.1.11.44: 4 cm, was 25 cm). */
    static final double TOUCHDOWN = 0.04;
    private static final double LAND_STAY = 3.5;
    private boolean idleThisTick;
    private int quietTicks, ownerStill;
    @Nullable private Vec3 ownerLast, landSpot, landPost, landOwner;
    /** 0.1.11.50: the owner's smoothed velocity (blocks per tick), measured from its positions. */
    private Vec3 ownerVelocity = Vec3.ZERO;
    /** 0.1.11.50: EU of escort speed flown since the last upkeep; true while it could not be paid (cruise speed only). */
    private double speedDue;
    private boolean speedStarved;
    private int stuckTicks;
    private double stuckBest = Double.MAX_VALUE;

    /** 0.1.11.50: the owner's velocity as this Wingman measures it. */
    Vec3 ownerVelocity() { return ownerVelocity; }

    /** 0.1.11.50: true while the escort speed above cruise could not be paid. */
    public boolean speedStarved() { return speedStarved; }

    private void trackOwner(ServerPlayer player) {
        Vec3 p = player.position();
        // 0.1.11.50 (F1): the owner's velocity from its positions (the server's velocity of a player is not kept up to date
        // for elytra or maneuver flight)
        ownerVelocity = ownerLast == null ? Vec3.ZERO : WingmanFlight.ownerVelocity(ownerVelocity, p.subtract(ownerLast));
        boolean still = ownerLast != null && player.onGround() && !player.isFallFlying() && !player.getAbilities().flying
            && p.subtract(ownerLast).horizontalDistanceSqr() < 0.06 * 0.06 && Math.abs(p.y - ownerLast.y) < 0.1;
        ownerStill = still ? ownerStill + 1 : 0;
        ownerLast = p;
    }

    /** Idle at {@code post}: hover there, or land below it (see above). {@code anchored}: a post of an order. */
    private void idle(Vec3 post, ServerPlayer player, boolean anchored) {
        idleThisTick = true;
        quietTicks++;
        if (landed()) {
            boolean stay = anchored ? landPost != null && post.distanceToSqr(landPost) < 2.0 * 2.0
                : landOwner != null && player.position().distanceToSqr(landOwner) < LAND_STAY * LAND_STAY
                    && !player.isFallFlying() && !player.getAbilities().flying;
            if (stay) { holdLanded(player); return; }
            takeOff();
        }
        boolean wantLand = quietTicks >= LAND_DELAY && getPassengers().isEmpty() && (anchored || ownerStill >= LAND_DELAY);
        if (wantLand) {
            if (landSpot == null || landPost == null || landPost.distanceToSqr(post) > 1.0) {
                landSpot = findLanding(post);
                landPost = post;
                landOwner = player.position();
            }
            if (landSpot != null) {
                Vec3 to = landSpot.subtract(position());
                // 0.1.11.44 (motion audit M0): touch down within 4 cm (the sink below gets it there); it was 25 cm, a visible jump
                if (to.lengthSqr() < TOUCHDOWN * TOUCHDOWN) { touchDown(); holdLanded(player); return; }
                if (to.horizontalDistanceSqr() > 0.4 * 0.4) {
                    // line up above the spot first (with the usual obstacle avoidance) ...
                    steer(new Vec3(landSpot.x, Math.max(getY(), landSpot.y + 1.2), landSpot.z), CRUISE * 0.7, player);
                } else {
                    // ... then sink straight down: the column above the spot was checked free
                    double down = Math.min(0.14, Math.max(Math.min(0.03, -to.y), -to.y * 0.35));
                    setDeltaMovement(to.x * 0.4, to.y < 0 ? -down : Math.min(0.1, to.y), to.z * 0.4);
                }
                return;
            }
        }
        if (anchored) steer(post, CRUISE, player);
        else follow(post, player);
    }

    /**
     * 0.1.11.50 (escort and open Armory phase F1, decision three B): flies to a place that moves with the owner. The
     * Wingman flies the owner's velocity plus the correction to the place, with a speed limit and an
     * acceleration that grow with the owner's speed ({@link WingmanFlight}); the speed above cruise is charged with the next
     * upkeep. When it could not be paid, or the owner stands, this is the old flight to the place at cruise speed.
     */
    private void follow(Vec3 post, ServerPlayer player) {
        Vec3 carry = speedStarved ? Vec3.ZERO : ownerVelocity;
        double ownerSpeed = carry.length();
        double cap = WingmanFlight.cap(ownerSpeed, CRUISE), accel = WingmanFlight.accel(ownerSpeed, ACCEL);
        Vec3 to = post.subtract(position());   // with the owner's velocity flown this tick, it is at the next place
        Vec3 want = WingmanTactics.avoid(this, WingmanFlight.want(to, carry, cap).add(separation()));
        Vec3 v = WingmanFlight.accelerate(getDeltaMovement(), want, accel);
        if (horizontalCollision) v = v.add(0, 0.3, 0);
        setDeltaMovement(v);
        if (WingmanConfig.upkeepEuPerSecond() > 0) speedDue += WingmanFlight.speedCost(v.length(), CRUISE) * WingmanConfig.speedCostFactor();
        faceFlight(v, player);
        // stuck behind terrain: far from the place and not closing in for STUCK_TICKS
        double dist = to.length();
        if (dist <= STUCK_DISTANCE || speedStarved) { stuckTicks = 0; stuckBest = Double.MAX_VALUE; return; }
        if (dist < stuckBest - 0.5) { stuckBest = dist; stuckTicks = 0; }
        else if (++stuckTicks >= STUCK_TICKS) catchUp(player);
    }

    /** Ground below {@code post} within 8 blocks with room for the suit, or null (water, lava, blocked, too deep). */
    @Nullable
    private Vec3 findLanding(Vec3 post) {
        Level level = level();
        BlockPos start = BlockPos.containing(post);
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos feet = start.below(dy), below = feet.below();
            if (!level.isLoaded(below) || !free(level, feet) || !free(level, feet.above())) return null;
            var floor = level.getBlockState(below).getCollisionShape(level, below);
            if (!floor.isEmpty()) {
                double top = below.getY() + floor.max(net.minecraft.core.Direction.Axis.Y);
                return top > feet.getY() + 0.01 ? null : new Vec3(post.x, top, post.z);
            }
            if (!level.getFluidState(below).isEmpty()) return null;
        }
        return null;
    }

    private static boolean free(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty();
    }

    private void touchDown() {
        if (landSpot != null) setPos(landSpot.x, landSpot.y, landSpot.z);
        setDeltaMovement(Vec3.ZERO);
        entityData.set(DATA_LANDED, true);
        if (level() instanceof ServerLevel world) {
            world.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.05, getZ(), 6, 0.25, 0.02, 0.25, 0.02);
            world.playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 0.5F, 0.8F);
        }
    }

    private void holdLanded(ServerPlayer player) {
        setDeltaMovement(Vec3.ZERO);
        // face the way the owner looks, slowly; head level
        setYaw(facing() + Mth.clamp(Mth.wrapDegrees(player.getYRot() - facing()), -6, 6));
        setXRot(getXRot() * 0.8F);
    }

    private void takeOff() {
        entityData.set(DATA_LANDED, false);
        landSpot = null;
        landPost = null;
        quietTicks = 0;
        setDeltaMovement(getDeltaMovement().add(0, 0.22, 0));
    }

    private void upkeep(ServerLevel world, @Nullable ServerPlayer player) {
        // 0.1.10.5: count the nominal EU that is covered. Energy Saving makes a piece pay less EU
        // for the same share; before, the saved EU counted as unpaid (the Armory paid it again, or
        // the Wingman flew home for low power).
        long base = WingmanConfig.upkeepEuPerSecond();
        // 0.1.11.50 (decision three B): the escort speed above cruise flown since the last upkeep is paid with it
        long speed = (long) Math.ceil(speedDue);
        speedDue = 0;
        if (base <= 0) { lowPowerSeconds = 0; speedStarved = false; return; }
        long due = base + speed;
        var api = MioIcifAPI.instance().getItemAPI();
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            ItemStack piece = getItemBySlot(s);
            if (piece.isEmpty() || !api.isElectricArmor(piece) || SuitEnergyPool.available(this, piece) <= 0) continue;
            due -= SuitEnergyPool.payPart(this, piece, due);
            if (due <= 0) break;
        }
        // 0.1.11.41 (decision 1 C): the reserve charged at deployment pays next, so a Wingman flies on away from a loaded Armory
        if (due > 0 && reserve > 0) {
            long take = Math.min(reserve, due);
            reserve -= take;
            due -= take;
        }
        armoryPaid = false;
        if (due > 0) {
            mio_icif_armory home = loadedArmory(world);
            var payment = home == null ? null : home.getEnergyStorageInternal().scexReserveInternal(due);
            if (payment != null) { payment.commit(); due = 0; armoryPaid = true; }
            else if (home != null && due > speed) {
                // 0.1.11.50: the Armory cannot pay the escort speed too: it pays the upkeep alone (the Wingman slows to cruise)
                var upkeepOnly = home.getEnergyStorageInternal().scexReserveInternal(due - speed);
                if (upkeepOnly != null) { upkeepOnly.commit(); due = speed; armoryPaid = true; }
            }
        }
        boolean starvedBefore = speedStarved;
        speedStarved = due > 0;   // what is left unpaid: no escort speed above cruise until a full payment
        if (speedStarved && !starvedBefore && speed > 0) WingmanCallouts.say(this, WingmanCallouts.Kind.SPEED_STARVED);
        // 0.1.11.58 (P6): the reserve is down to its last minute (once until it is refilled)
        if (reserve > 0 && reserve < base * 60) {
            if (!toldReserveLow) toldReserveLow = WingmanCallouts.say(this, WingmanCallouts.Kind.RESERVE_LOW);
        } else if (reserve >= base * 60) toldReserveLow = false;
        if (due <= speed) { lowPowerSeconds = 0; return; }   // only the speed went unpaid: the upkeep itself is covered
        if (++lowPowerSeconds >= WingmanConfig.lowPowerSeconds()) {
            WingmanCallouts.say(this, WingmanCallouts.Kind.LOW_POWER);
            startReturn(player == null ? null : Component.translatable("message.mio_icif.wingman.low_power"));
        }
    }

    /** 0.1.11.41: EU of the upkeep reserve left. */
    public long reserve() { return reserve; }

    /** 0.1.11.41: sets the upkeep reserve (deployment, tests). */
    void setReserve(long eu) { reserve = Math.max(0, eu); }

    /** EU stored in the electric pieces of the suit (0 without). */
    long suitStored() {
        var api = MioIcifAPI.instance().getItemAPI();
        long stored = 0;
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            ItemStack piece = getItemBySlot(s);
            if (!piece.isEmpty() && api.isElectricArmor(piece)) stored += Math.max(0, api.getElectricArmorStored(piece));
        }
        return stored;
    }

    /** {@link #enduranceSeconds()}: no upkeep (free flight). */
    public static final int ENDURANCE_FREE = -1;
    /** {@link #enduranceSeconds()}: suit and reserve are empty and the Armory pays (it flies as long as the Armory has energy). */
    public static final int ENDURANCE_ARMORY = -2;

    /**
     * 0.1.11.41: seconds of flight the suit charge and the reserve pay for (Energy Saving makes it a little longer), or
     * {@link #ENDURANCE_FREE} / {@link #ENDURANCE_ARMORY}.
     */
    public int enduranceSeconds() {
        long upkeep = WingmanConfig.upkeepEuPerSecond();
        if (upkeep <= 0) return ENDURANCE_FREE;
        long own = suitStored() + reserve;
        if (own < upkeep && armoryPaid) return ENDURANCE_ARMORY;
        return (int) Math.min(Integer.MAX_VALUE, own / upkeep);
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
        double leash = WingmanConfig.leash();
        LivingEntity focus = ownerFocus(player);
        if (focus != null && focus != target) target = focus;          // the owner's new target first, at once
        else if (tickCount % 5 == 0 || !valid(target, player, leash)) {
            // 0.1.9.1: threat scoring (owner's call, fusing creepers, attackers, hunters, focus fire)
            target = WingmanTactics.select(this, player, player.position(), WingmanConfig.scanRange(), valid(target, player, leash) ? target : null,
                e -> valid(e, player, leash));
        }
        if (target != null) engage(target, player);
        else idle(formation(player), player, false);
    }

    private void tickDispersal(ServerPlayer player) {
        double angle = Math.toRadians(player.getYRot()) + slot * (Math.PI * 2 / slotCount);
        Vec3 station = player.position().add(-Math.sin(angle) * 10, 3.5, Math.cos(angle) * 10);
        double leash = WingmanConfig.leash();
        if (tickCount % 10 == 0 || !valid(target, player, leash)) {
            target = WingmanTactics.select(this, player, station, 14, valid(target, player, leash) ? target : null, e -> valid(e, player, leash));
            // decoy: the mobs near the station that hunt the owner turn to this Wingman
            for (Mob mob : level().getEntitiesOfClass(Mob.class, new AABB(station, station).inflate(12),
                    m -> m instanceof Enemy && m.getTarget() == player)) mob.setTarget(this);
        }
        if (target != null) engage(target, player);
        else idle(station, player, false);
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
                WingmanCallouts.say(this, WingmanCallouts.Kind.INTERCEPT);
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
        else idle(formation(player), player, false);
    }

    // ---- emergency rescue (0.1.11.56, gaze and Iron Man phase P4; WingmanRescue picks the Wingman)

    /** Ticks a falling owner is held after the catch, and the most ticks a rescue flight lasts. */
    static final int CATCH_HOLD = 10, RESCUE_TICKS = 100;
    /** Distance at which the owner is caught. */
    static final double CATCH_REACH = 1.8;
    private WingmanRescue.Danger rescue = WingmanRescue.Danger.NONE;
    private int rescueTicks, holdTicks;
    private long lastRescue = Long.MIN_VALUE / 2;

    /** 0.1.11.56: true while this Wingman flies a rescue. */
    public boolean rescuing() { return rescue != WingmanRescue.Danger.NONE; }

    /** 0.1.11.56: true when this Wingman can be sent on a rescue for {@code danger} now. */
    boolean canRescue(long now, WingmanRescue.Danger danger) {
        if (state() != ACTIVE || secured || rescuing() || swapPhase != SWAP_NONE || now - lastRescue < WingmanRescue.COOLDOWN) return false;
        return danger != WingmanRescue.Danger.LOW_HEALTH || armourPieces() >= 2;   // near death it brings a suit
    }

    private int armourPieces() {
        int n = 0;
        for (ArmoryPiece p : ArmoryPiece.VALUES) if (p.isArmor() && !getItemBySlot(p.slot).isEmpty()) n++;
        return n;
    }

    /** 0.1.11.56: starts a rescue of the owner. */
    void startRescue(WingmanRescue.Danger danger, ServerPlayer player) {
        lastRescue = level().getGameTime();
        clearOrder();
        deathGuard = false;
        target = null;
        if (danger == WingmanRescue.Danger.LOW_HEALTH) {
            // near death: the suit comes to the owner at once
            if (suitUp(player) == null) WingmanCallouts.say(this, WingmanCallouts.Kind.RESCUE_SUIT);
            return;
        }
        rescue = danger;
        rescueTicks = 0;
        holdTicks = 0;
        level().playSound(null, getX(), getY(), getZ(), mio_icif_sounds.WINGMAN_BOOST.get(), SoundSource.NEUTRAL, 1.0F, 0.8F);
    }

    /** One tick of a fall rescue: race to the owner, catch them, hold them, then swap suits when they wear less armour. */
    private void tickRescue(@Nullable ServerPlayer player, boolean ownerHere) {
        if (!ownerHere || ++rescueTicks > RESCUE_TICKS || holdTicks == 0 && player.onGround()) { rescue = WingmanRescue.Danger.NONE; return; }
        face(player.getEyePosition());
        Vec3 pv = player.getDeltaMovement();
        if (holdTicks == 0) {
            Vec3 to = player.position().add(pv).add(0, -0.4, 0).subtract(position());
            double cap = Math.max(3.0, pv.length() + 1.5);
            Vec3 want = pv.add(to.scale(0.6));
            if (want.length() > cap) want = want.normalize().scale(cap);
            setDeltaMovement(WingmanFlight.accelerate(getDeltaMovement(), want, 0.9));
            if (distanceTo(player) <= CATCH_REACH) {
                holdTicks = CATCH_HOLD;
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 100, 0, false, false, true));
                WingmanCallouts.say(this, WingmanCallouts.Kind.CAUGHT);
                level().playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.ARMORY_SEAL.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
                holdOwner(player);
            }
            return;
        }
        holdOwner(player);
        if (--holdTicks > 0) return;
        rescue = WingmanRescue.Danger.NONE;
        // caught: the owner gets the suit when they wear less armour than the Wingman
        if (WingmanRescue.armourPieces(player) < armourPieces() && armourPieces() >= 2 && suitUp(player) == null)
            WingmanCallouts.say(this, WingmanCallouts.Kind.RESCUE_SUIT);
    }

    /** Holds the caught owner: no fall, no fall distance; the Wingman under them. */
    private void holdOwner(ServerPlayer player) {
        player.setDeltaMovement(player.getDeltaMovement().multiply(0.3, 0, 0.3).add(0, 0.02, 0));
        player.hurtMarked = true;
        player.resetFallDistance();
        Vec3 under = player.position().add(0, -1.2, 0).subtract(position());
        setDeltaMovement(under.scale(0.5));
    }

    // ---- suit swap (0.1.11.54, gaze and Iron Man phase P2, decision four A)
    //
    // The owner calls a Wingman to swap suits (the command wheel, squad control). It flies to a place in front of the
    // owner and faces them; its pieces come off (the suit frame has no body: it is gone) and fly onto the owner, who is
    // suited up when they arrive (the seal of the Armory); then the owner's old pieces fly the other way and the Wingman
    // stands up in them, in its old place of the squad (same sortie, same Armory set). While pieces fly they are held by
    // the Wingman (saved with it): a cancel (the owner gone, dead, far, or the Wingman removed) puts them back.
    //
    // A slot is swapped when the Wingman has a piece there or the owner has gear there (hands: an unstackable item);
    // a slot with Curse of Binding is left alone; the owner's hand items that are not gear, the Armory remote and items
    // the Armory does not take go into the owner's inventory instead. With fewer than two armour pieces after the swap
    // the Wingman flies home (none at all: it is dismissed and its sortie closes).

    static final byte SWAP_NONE = 0, SWAP_APPROACH = 1, SWAP_TO_OWNER = 2, SWAP_TO_WINGMAN = 3, SWAP_RESUME = 4;
    /** Ticks the Wingman tries to reach the owner, and the farthest the owner may be when the pieces arrive. */
    static final int SWAP_APPROACH_TICKS = 200;
    static final double SWAP_REACH = 12;
    private byte swapPhase = SWAP_NONE;
    private int swapWait;
    private final ItemStack[] swapOut = emptyPieces(), swapIn = emptyPieces();
    private final boolean[] swapTake = new boolean[ArmoryPiece.VALUES.length];

    private static ItemStack[] emptyPieces() {
        ItemStack[] a = new ItemStack[ArmoryPiece.VALUES.length];
        java.util.Arrays.fill(a, ItemStack.EMPTY);
        return a;
    }

    /** 0.1.11.54: the swap phase (0 none). */
    public byte swapPhase() { return swapPhase; }

    /** 0.1.11.54: starts a suit swap with the owner; null when it starts, else the reason (a message key). */
    @Nullable
    public String suitUp(ServerPlayer player) { return suitUp(player, null); }

    /** 0.1.11.57 (P5): a swap of one piece ({@code only}; null: the whole suit). */
    @Nullable
    public String suitUp(ServerPlayer player, @Nullable ArmoryPiece only) {
        if (state() != ACTIVE || secured) return "message.mio_icif.wingman.swap.busy";
        if (swapPhase != SWAP_NONE) return "message.mio_icif.wingman.swap.busy";
        if (player.level() != level()) return "message.mio_icif.wingman.swap.far";
        clearOrder();
        deathGuard = false;
        target = null;
        swapPhase = SWAP_APPROACH;
        swapWait = 0;
        swapOnly = only;
        return null;
    }

    /** 0.1.11.57: the one piece a swap hands over (null: the whole suit). */
    @Nullable private ArmoryPiece swapOnly;

    /** An item the owner holds that counts as gear (it goes to the Wingman): a tool, weapon, shield or totem (unstackable). */
    static boolean handGear(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() == 1;
    }

    /** True when the owner's {@code stack} in {@code piece} goes to the Wingman. */
    public static boolean givesToWingman(ItemStack stack, ArmoryPiece piece) {
        if (stack.isEmpty() || stack.getItem() instanceof ArmoryRemoteItem || !ArmoryRules.isSummonable(stack)) return false;
        if (!piece.isArmor() && !handGear(stack)) return false;
        return ArmoryRules.fits(stack, piece);
    }

    private static boolean bound(ItemStack stack, ServerPlayer player) {
        return !stack.isEmpty() && !player.isCreative()
            && net.minecraft.world.item.enchantment.EnchantmentHelper.has(stack, net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }

    /** Where the Wingman stands for the swap: in front of the owner, at the owner's height, facing them. */
    private static Vec3 swapPlace(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        return player.position().add(look.scale(1.8)).add(0, 0.05, 0);
    }

    private void tickSwap(@Nullable ServerPlayer player, boolean ownerHere) {
        if (swapPhase == SWAP_RESUME) { flushSwap(); return; }
        if (player != null) face(player.getEyePosition());
        switch (swapPhase) {
            case SWAP_APPROACH -> {
                if (!ownerHere || ++swapWait > SWAP_APPROACH_TICKS) { cancelSwap(player); return; }
                Vec3 place = swapPlace(player);
                double d = position().distanceTo(place);
                if (d > 0.7) {
                    steer(place, Math.max(COMBAT, ownerVelocity.length() + 0.6), player);
                    face(player.getEyePosition());
                    return;
                }
                setDeltaMovement(Vec3.ZERO);
                beginSwap(player);
            }
            case SWAP_TO_OWNER -> {
                setDeltaMovement(getDeltaMovement().scale(0.5));
                if (--swapWait > 0) return;
                if (!ownerHere || player.distanceTo(this) > SWAP_REACH) { cancelSwap(player); return; }
                suitOwner(player);
            }
            case SWAP_TO_WINGMAN -> {
                setDeltaMovement(getDeltaMovement().scale(0.5));
                if (--swapWait > 0) return;
                standUp(player);
            }
            default -> swapPhase = SWAP_NONE;
        }
    }

    /** The Wingman's pieces come off and fly to the owner. */
    private void beginSwap(ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        int longest = 1;
        boolean any = false;
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            int i = p.ordinal();
            ItemStack mine = getItemBySlot(p.slot), theirs = player.getItemBySlot(p.slot);
            swapTake[i] = (swapOnly == null || p == swapOnly) && !bound(theirs, player) && (!mine.isEmpty() || givesToWingman(theirs, p));
            if (!swapTake[i]) continue;
            any = true;
            swapOut[i] = mine.copy();
            setItemSlot(p.slot, ItemStack.EMPTY);
            if (swapOut[i].isEmpty()) continue;
            Vec3 from = ArmoryFlight.dockPoint(this, 1.0F, p), to = ArmoryFlight.dockPoint(player, 1.0F, p);
            ArmoryPieceEntity echo = ArmoryPieceEntity.echo(level, this, swapOut[i].copy(), p, from, to);
            level.addFreshEntity(echo);
            longest = Math.max(longest, ArmoryFlight.PURGE_TICKS + ArmoryFlight.returnTicksFor(ArmoryFlight.length(from, to)));
        }
        if (!any) { cancelSwap(player); return; }
        level.playSound(null, getX(), getY(), getZ(), mio_icif_sounds.ARMORY_UNLOCK.get(), SoundSource.NEUTRAL, 0.9F, 1.0F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.0, getZ(), 14, 0.3, 0.5, 0.3, 0.08);
        swapPhase = SWAP_TO_OWNER;
        swapWait = longest;
    }

    /**
     * 0.1.11.55 (P3): a new Wingman takes the owner's ejected pieces: they fly from the owner's body to it, and it stands
     * up in them when they arrive ({@link #standUp}); it holds them meanwhile (saved with it).
     */
    void receiveSuit(ServerPlayer player, ItemStack[] items) {
        ServerLevel level = (ServerLevel) level();
        int longest = 1;
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            int i = p.ordinal();
            if (items[i].isEmpty()) continue;
            swapIn[i] = items[i];
            swapTake[i] = true;
            Vec3 from = ArmoryFlight.dockPoint(player, 1.0F, p), to = ArmoryFlight.dockPoint(this, 1.0F, p);
            level.addFreshEntity(ArmoryPieceEntity.echo(level, player, items[i].copy(), p, from, to));
            longest = Math.max(longest, ArmoryFlight.PURGE_TICKS + ArmoryFlight.returnTicksFor(ArmoryFlight.length(from, to)));
        }
        swapPhase = SWAP_TO_WINGMAN;
        swapWait = longest;
        ejected = true;
    }

    /** 0.1.11.55: true while the pieces of an eject fly to this new Wingman (no "back in formation" message). */
    private boolean ejected;

    /** The pieces arrive: the owner is suited up, their old pieces fly to the Wingman. */
    private void suitOwner(ServerPlayer player) {
        ServerLevel level = (ServerLevel) level();
        int longest = 1;
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            int i = p.ordinal();
            if (!swapTake[i]) continue;
            ItemStack theirs = player.getItemBySlot(p.slot).copy();
            if (bound(theirs, player)) {   // bound since the start: this slot stays as it is, the piece goes back to the suit
                swapIn[i] = swapOut[i];
                swapOut[i] = ItemStack.EMPTY;
                continue;
            }
            ArmoryPieceEntity.equipSilently(player, p.slot, swapOut[i]);
            swapOut[i] = ItemStack.EMPTY;
            if (theirs.isEmpty()) continue;
            if (givesToWingman(theirs, p)) {
                swapIn[i] = theirs;
                Vec3 from = ArmoryFlight.dockPoint(player, 1.0F, p), to = ArmoryFlight.dockPoint(this, 1.0F, p);
                level.addFreshEntity(ArmoryPieceEntity.echo(level, player, theirs.copy(), p, from, to));
                longest = Math.max(longest, ArmoryFlight.PURGE_TICKS + ArmoryFlight.returnTicksFor(ArmoryFlight.length(from, to)));
            } else {
                ArmoryPieceEntity.stash(player, theirs);
            }
        }
        player.inventoryMenu.broadcastChanges();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.ARMORY_SEAL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), mio_icif_sounds.ARMORY_COMPLETE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.3, 0.6, 0.3, 0.06);
        WingmanCallouts.say(this, WingmanCallouts.Kind.SUITED);
        swapPhase = SWAP_TO_WINGMAN;
        swapWait = longest;
    }

    /** The owner's old pieces arrive: the Wingman stands up in them (or flies home / is dismissed with too few). */
    private void standUp(@Nullable ServerPlayer player) {
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            int i = p.ordinal();
            if (swapIn[i].isEmpty()) continue;
            if (getItemBySlot(p.slot).isEmpty()) setItemSlot(p.slot, swapIn[i]);
            else spawnAtLocation(swapIn[i]);
            swapIn[i] = ItemStack.EMPTY;
        }
        swapPhase = SWAP_NONE;
        swapOnly = null;
        java.util.Arrays.fill(swapTake, false);
        level().playSound(null, getX(), getY(), getZ(), mio_icif_sounds.ARMORY_SEAL.get(), SoundSource.NEUTRAL, 0.9F, 1.1F);
        int armour = 0;
        boolean anything = false;
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            if (getItemBySlot(p.slot).isEmpty()) continue;
            anything = true;
            if (p.isArmor()) armour++;
        }
        if (!anything) {
            // nothing to fly in: dismissed, its sortie closes (the suit is on the owner)
            if (player != null) player.displayClientMessage(Component.translatable("message.mio_icif.wingman.swap.dismissed", getName()), false);
            secure();
            discard();
            return;
        }
        if (armour < 2) {
            startReturn(Component.translatable("message.mio_icif.wingman.swap.home", getName()));
            return;
        }
        WingmanCallouts.say(this, ejected ? WingmanCallouts.Kind.EJECTED : WingmanCallouts.Kind.REJOINED);
        ejected = false;
    }

    /** Ends a swap before the owner was suited up: the pieces go back on the Wingman. */
    private void cancelSwap(@Nullable ServerPlayer player) {
        flushSwap();
        if (player != null) player.displayClientMessage(Component.translatable("message.mio_icif.wingman.swap.far", getName()), true);
    }

    /** Puts every held swap piece back on the Wingman (into an empty slot), else home or on the ground. */
    private void flushSwap() {
        mio_icif_armory home = level() instanceof ServerLevel world ? loadedArmory(world) : null;
        for (ArmoryPiece p : ArmoryPiece.VALUES) {
            int i = p.ordinal();
            for (ItemStack[] held : new ItemStack[][]{swapOut, swapIn}) {
                ItemStack stack = held[i];
                if (stack.isEmpty()) continue;
                held[i] = ItemStack.EMPTY;
                if (getItemBySlot(p.slot).isEmpty()) { setItemSlot(p.slot, stack); continue; }
                ItemStack rest = home != null ? home.store(set, p, stack) : stack;
                if (!rest.isEmpty()) spawnAtLocation(rest);
            }
        }
        java.util.Arrays.fill(swapTake, false);
        swapPhase = SWAP_NONE;
        swapWait = 0;
        swapOnly = null;
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
        else if (travelling || suppress) steer(post, travelling ? COMBAT : CRUISE, player);
        else idle(post, player, true);   // 0.1.10.11: a quiet HOLD / arrived MOVE post sets down
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

    /**
     * A hostile projectile that passes within 2.2 blocks of the owner in the next 20 ticks. 0.1.11.43 (G-8): hostile means
     * from a hostile creature, a creature that hunts the owner, a player of another team (or none), or nobody (a
     * dispenser). Potions and ender pearls are never stopped (a healing potion thrown to the owner, a teammate's pearl);
     * before, every projectile of somebody else was destroyed.
     */
    static boolean incoming(Projectile p, ServerPlayer player) {
        if (p instanceof net.minecraft.world.entity.projectile.ThrownPotion || p instanceof net.minecraft.world.entity.projectile.ThrownEnderpearl) return false;
        Entity shooter = p.getOwner();
        if (shooter == player || shooter instanceof WingmanEntity) return false;
        if (shooter instanceof Player other && player.isAlliedTo(other)) return false;
        if (shooter instanceof Mob mob && !(mob instanceof Enemy) && mob.getTarget() != player) return false;
        if (shooter != null && !(shooter instanceof Player) && !(shooter instanceof Mob)) return false;
        Vec3 c = player.getBoundingBox().getCenter();
        Vec3 v = p.getDeltaMovement();
        double v2 = v.lengthSqr();
        if (v2 < 0.01) return false;                                   // lying still (an arrow in the ground)
        Vec3 rel = c.subtract(p.position());
        double t = rel.dot(v) / v2;                                    // ticks to the closest approach
        if (t <= 0 || t > 20) return false;
        return p.position().add(v.scale(t)).distanceToSqr(c) <= 2.2 * 2.2;
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
        return kindOf(getMainHandItem());
    }

    /** Weapon kind of {@code w} (0.1.10.13: the ranged test is shared with the arm pose, {@link WingmanPose#ranged}). */
    static WeaponKind kindOf(ItemStack w) {
        if (SlashBladeCombat.isBlade(w)) return WeaponKind.BLADE;
        return WingmanPose.ranged(w) ? WeaponKind.RANGED : WeaponKind.MELEE;
    }

    /** 0.1.11.52 (P0): ticks of the stare at a newly locked enemy, the rest between two stares, the head turn in a fight. */
    static final int GLARE = 6, GLARE_REST = 40;
    static final float GAZE_TURN = 45.0F;
    private int glareTicks, lastGlare = -GLARE_REST;

    /** 0.1.11.52: ticks of the stare left (0 when none). */
    int glareTicks() { return glareTicks; }

    private void engage(LivingEntity enemy, ServerPlayer player) {
        if (glareTicks > 0) {
            // the stare: brake to a hover, the head (and the body after it) on the enemy's eyes
            glareTicks--;
            faceSmooth(enemy.getEyePosition(), GAZE_TURN);
            setDeltaMovement(getDeltaMovement().scale(0.6));
            return;
        }
        switch (weaponKind()) {
            case RANGED -> {
                if (tickCount < dryUntil) engageMelee(enemy, player);   // 0.1.11.42: an empty energy weapon: melee until it may fire again
                else engageRanged(enemy, player);
            }
            case BLADE -> engageBlade(enemy, player);
            default -> engageMelee(enemy, player);
        }
    }

    /** Melee: close in on the predicted position, strike, then peel off sideways and come back. */
    private void engageMelee(LivingEntity enemy, ServerPlayer player) {
        Vec3 aim = enemy.position().add(0, enemy.getBbHeight() * 0.5, 0);
        faceSmooth(enemy.getEyePosition(), 35);
        // 0.1.11.46 (M2): after a hit the Wingman stays a moment (the follow-through of the cut), then boosts away
        if (hitPause > 0) {
            setDeltaMovement(getDeltaMovement().scale(0.45));
            if (--hitPause == 0) boost(position().subtract(enemy.position()).add(sideOf(enemy).scale(0.8)));
            return;
        }
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
            hitPause = HIT_PAUSE;   // the boost away follows the pause (0.1.11.46; it was at once, in the tick of the hit)
            retreatTicks = 10;
            strafeSign = -strafeSign;
        }
    }

    /** Ticks a melee Wingman stays at its target after a hit before it boosts away (0.1.11.46). */
    static final int HIT_PAUSE = 3;
    private int hitPause;

    // ---- electric weapons (0.1.10.4)

    /** Ticks without a target; the nanosaber switches off after {@link #DISARM_TICKS}. */
    private int idleTicks;
    private static final int DISARM_TICKS = 60;

    /**
     * Melee hit with the held weapon. Mob#doHurtTarget only reads the attack damage attribute and
     * never calls the item. The electric melee weapons pay their charge in {@code Item#hurtEnemy}, and
     * some add damage in {@code Item#getAttackDamageBonus} (quantum sword +12.5 in hyper mode; the
     * nanosaber, chainsaw and drills give theirs as an attack damage attribute since 0.1.10.14). For an
     * SI electric tool the hit works as for a player: the weapon's attribute counts as the weapon is
     * now (a nanosaber switched on in this tick is on), the bonus is added (enchantments still apply),
     * and after a hit the weapon's hurtEnemy runs (it pays the charge and switches off when empty).
     * Other weapons keep the vanilla mob attack.
     */
    @Override
    public boolean doHurtTarget(Entity target) {
        ItemStack weapon = getMainHandItem();
        if (!(weapon.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_tool_elc tool)) {
            boolean hit = super.doHurtTarget(target);
            if (hit && !SlashBladeCombat.isBlade(weapon)) wear(weapon, EquipmentSlot.MAINHAND, 1);   // 0.1.11.42: as a player's hit
            return hit;
        }
        // the equipment check applies a changed weapon's modifiers only at the next tick: apply them now
        // (same ids as the held item's, so they replace what is there)
        weapon.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            var instance = getAttribute(attribute);
            if (instance != null) instance.addOrUpdateTransientModifier(modifier);
        });
        float base = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource source = damageSources().mobAttack(this);
        float damage = base + tool.getAttackDamageBonus(target, base, source);
        if (level() instanceof ServerLevel world) damage = net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(world, weapon, target, source, damage);
        boolean hit = target.hurt(source, damage);
        if (hit) {
            float knock = getKnockback(target, source);
            if (knock > 0.0F && target instanceof LivingEntity living) {
                living.knockback(knock * 0.5F, Mth.sin(facing() * Mth.DEG_TO_RAD), -Mth.cos(facing() * Mth.DEG_TO_RAD));
                setDeltaMovement(getDeltaMovement().multiply(0.6, 1.0, 0.6));
            }
            if (level() instanceof ServerLevel world) net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(world, target, source);
            if (target instanceof LivingEntity living) tool.hurtEnemy(weapon, living, this);
            wear(weapon, EquipmentSlot.MAINHAND, 1);                  // 0.1.11.42 (most SI electric tools have no durability)
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
            toldEmpty = false;
        } else if (weapon.getItem() instanceof com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber
                && !com.miophas.singularity_iteration.common.item.tools.mio_icif_nanosaber.isActive(weapon)) {
            tellEmpty(weapon);   // 0.1.11.42: a nanosaber without its activation charge fights switched off
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

    /** 0.1.10.11: ticks of the arm cut of one combo input (the client key frames; 0.1.11.53: the chains time the inputs). */
    static final int BLADE_COMBO_TICKS = 8;

    /**
     * 0.1.10.11: a slash for the clients: the arm plays the cut ({@code WingmanRenderer}). The blade
     * combos no longer call {@code swing}: the vanilla arm swing restarted every few ticks and
     * fought the blade's own motion (the twitching hand).
     */
    private void slash(int kind) {
        entityData.set(DATA_SLASH, (((entityData.get(DATA_SLASH) >>> 2) + 1) << 2) | (kind & 3));
    }

    /** Client: the slash counter and kind ({@link #slash}). */
    public int slashData() { return entityData.get(DATA_SLASH); }

    /**
     * SlashBlade: dash in with the rapid slash from 3.5 to 9 blocks, chain the combo in reach
     * (0.1.10.11: one input every {@link #BLADE_COMBO_TICKS} ticks, so every cut is seen; before,
     * every 5 ticks cut each motion short), release a Slash Art every {@link #ARTS_INTERVAL} ticks
     * of contact. The blade's own tick actions do the cutting.
     */
    // ---- SlashBlade combos (0.1.11.53, gaze and Iron Man phase P1, decision two A)
    //
    // The Wingman plays whole chains ({@link BladeChain}): in the air the aerial chains A and B by turns, opening a fight
    // from above with the aerial cleave (a dive onto the target, the landing slash); against a target on the ground with
    // room beside it, it lands there (onGround picks the ground chains) for the chains A (a4, or a4_ex and a5ex when
    // powered), C and the B flurry, stays down until the finisher has played and takes off. During a chain it stays at
    // the target (no circling, which took it out of reach and broke the chain), and a Slash Art comes only between chains.

    /** Most blocks to the target at which a chain starts or goes on (SlashBlade's reach). */
    static final double BLADE_REACH = 3.5;
    /** Chance that a chain against a target on the ground is played on the ground. */
    static final float GROUND_CHANCE = 0.6F;
    /** Ticks a Wingman tries to reach its ground spot or the cleave perch before it plays an aerial chain instead. */
    static final int SETUP_TICKS = 40;
    /** Ticks after the kick of aerial_rave_b3 in which the Wingman does not steer (the blade lifts it). */
    static final int KICK_TICKS = 6;

    private final BladeChain chain = new BladeChain();
    /** The chain chosen next and where it is set up (the ground spot or the cleave perch), and since when. */
    @Nullable private BladeChain.Kind planned;
    @Nullable private Vec3 setupAt;
    private int setupTicks, kickTicks;
    private boolean aerialB, firstChain = true;
    @Nullable private LivingEntity chainTarget;

    /** 0.1.11.53: the chain scheduler (tests). */
    BladeChain bladeChain() { return chain; }

    private void engageBlade(LivingEntity enemy, ServerPlayer player) {
        faceSmooth(enemy.getEyePosition(), GAZE_TURN);
        long now = level().getGameTime();
        if (enemy != chainTarget) { chainTarget = enemy; firstChain = true; planned = null; chain.end(now, 0); }
        double d = distanceTo(enemy);
        BladeChain.Kind running = chain.running(now);
        if (running != null) {
            playChain(enemy, player, running, now, d);
            return;
        }
        if (!chain.free(now)) { holdAtBlade(enemy, player, false); return; }
        // between chains: a Slash Art when it is ready
        if (artsCooldown == 0 && d < 7 && hasLineOfSight(enemy)) {
            if (SlashBladeCombat.slashArts(this, enemy) != null) {
                slash(SLASH_ARTS);
                artsCooldown = ARTS_INTERVAL;
                chain.end(now, 20);
                boost(position().subtract(enemy.position()));     // 0.1.9.1: break away after the art
                return;
            }
        }
        if (planned == null) plan(enemy, now);
        if (planned == null) { engageMelee(enemy, player); return; }   // the bridge went off: a plain sword
        switch (planned) {
            case CLEAVE -> {
                // above the target, a little to the side; then the dive
                if (setupAt == null || position().distanceTo(setupAt) > 1.2) {
                    if (setupAt == null || ++setupTicks > SETUP_TICKS) { planned = BladeChain.Kind.AERIAL_A; setupAt = null; return; }
                    steer(setupAt, COMBAT, player);
                    return;
                }
                startChain(BladeChain.Kind.CLEAVE, now);
                playChain(enemy, player, BladeChain.Kind.CLEAVE, now, d);
            }
            case GROUND_A, GROUND_B, GROUND_C -> {
                if (setupAt == null || ++setupTicks > SETUP_TICKS || setupAt.distanceTo(enemy.position()) > BLADE_REACH) {
                    planned = aerialB ? BladeChain.Kind.AERIAL_B : BladeChain.Kind.AERIAL_A;
                    setupAt = null;
                    return;
                }
                if (!descendTo(setupAt, player)) return;
                BladeChain.Kind k = planned;
                startChain(k, now);
                playChain(enemy, player, k, now, d);
            }
            default -> {
                if (d > BLADE_REACH) { steer(enemy.position().add(0, enemy.getBbHeight() * 0.5, 0).add(enemy.getDeltaMovement().scale(3)), COMBAT, player); return; }
                BladeChain.Kind k = planned;
                startChain(k, now);
                playChain(enemy, player, k, now, d);
            }
        }
    }

    /** Chooses the next chain and where to set it up. */
    private void plan(LivingEntity enemy, long now) {
        if (!SlashBladeCombat.isBlade(getMainHandItem())) return;
        setupTicks = 0;
        setupAt = null;
        Vec3 side = position().subtract(enemy.position()).multiply(1, 0, 1);
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 spot = standsOnFloor(enemy) ? findLanding(enemy.position().add(side.scale(1.8)).add(0, 1.0, 0)) : null;
        if (spot != null && Math.abs(spot.y - enemy.getY()) > 1.1) spot = null;
        Vec3 perch = enemy.position().add(side.scale(1.2)).add(0, 4.0, 0);
        if (firstChain && spot != null && level().noCollision(this, getBoundingBox().move(perch.subtract(position())).expandTowards(0, -3.0, 0))) {
            planned = BladeChain.Kind.CLEAVE;   // the opener: up to the perch above the target, then the dive
            setupAt = perch;
        } else if (spot != null && getRandom().nextFloat() < GROUND_CHANCE) {
            BladeChain.Kind[] ground = {BladeChain.Kind.GROUND_A, BladeChain.Kind.GROUND_C, BladeChain.Kind.GROUND_B};
            planned = ground[getRandom().nextInt(ground.length)];
            setupAt = spot;
        } else {
            planned = aerialB ? BladeChain.Kind.AERIAL_B : BladeChain.Kind.AERIAL_A;
        }
        firstChain = false;
    }

    /** True when {@code e} stands on a floor (its onGround, or a solid block right under its feet: a still mob never updates onGround). */
    private boolean standsOnFloor(LivingEntity e) {
        if (e.onGround()) return true;
        BlockPos under = BlockPos.containing(e.getX(), e.getY() - 0.05, e.getZ());
        var shape = level().getBlockState(under).getCollisionShape(level(), under);
        return !shape.isEmpty() && under.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) >= e.getY() - 0.06;
    }

    private void startChain(BladeChain.Kind kind, long now) {
        chain.start(kind, now);
        planned = null;
        setupAt = null;
        if (kind == BladeChain.Kind.AERIAL_A || kind == BladeChain.Kind.AERIAL_B) aerialB = !aerialB;
    }

    /** Flies down onto {@code spot} (a floor point); true when the Wingman stands on it. */
    private boolean descendTo(Vec3 spot, ServerPlayer player) {
        Vec3 to = spot.subtract(position());
        if (onGround() && to.horizontalDistanceSqr() < 0.6 * 0.6) return true;
        if (to.horizontalDistanceSqr() > 0.5 * 0.5) steer(new Vec3(spot.x, Math.max(spot.y + 0.6, Math.min(getY(), spot.y + 2.5)), spot.z), COMBAT, player);
        else setDeltaMovement(to.x * 0.4, Math.max(-0.4, Math.min(-0.08, to.y * 0.5)), to.z * 0.4);
        return false;
    }

    /** One tick of a running chain: stay at the target (on the ground, or in the air), click when due. */
    private void playChain(LivingEntity enemy, ServerPlayer player, BladeChain.Kind kind, long now, double d) {
        if (kind.grounded) {
            if (!onGround() && chain.clicks() > 0 && !chain.resting(now)) { chain.end(now, BladeChain.MISS_REST); return; }   // knocked off the ground: the chain is lost
            setDeltaMovement(0, -0.1, 0);   // stand (the suit flies without gravity)
            if (!chain.resting(now) && d > BLADE_REACH + 0.5) { chain.end(now, BladeChain.MISS_REST); return; }   // the target left
        } else if (kind == BladeChain.Kind.CLEAVE) {
            // the dive: the blade drives the Wingman down (3 blocks per tick) until it lands; no steering against it
            if (chain.resting(now) && onGround()) setDeltaMovement(0, -0.1, 0);
        } else if (kickTicks > 0) {
            kickTicks--;
        } else {
            holdAtBlade(enemy, player, true);
            if (onGround() && chain.clicks() == 0) return;   // an aerial chain starts in the air (onGround picks the chain)
        }
        if (!chain.due(now)) return;
        if (kind != BladeChain.Kind.CLEAVE && d > BLADE_REACH + 0.5) return;   // wait for reach (the window may still hold)
        net.minecraft.resources.ResourceLocation combo = kind == BladeChain.Kind.CLEAVE
            ? SlashBladeCombat.input(this, enemy, SlashBladeCombat.Command.SNEAK, SlashBladeCombat.Command.BACK, SlashBladeCombat.Command.R_CLICK)
            : SlashBladeCombat.attack(this, enemy);
        String path = combo == null ? null : combo.getPath();
        if (chain.clicked(now, path)) {
            slash(SLASH_COMBO);
            if ("aerial_rave_b3".equals(path)) kickTicks = KICK_TICKS;
        }
        if (BLADE_LOG) com.miophas.singularity_iteration.common.Singularity_Iteration.LOGGER.info("Wingman {} blade {} click -> {} (chain {}, clicks {})", getId(), kind, path,
            chain.running(now), chain.clicks());
    }

    /** Debug log of the blade clicks (-Dsi.wingman.bladeLog=true; the capture scene and the tests read it). */
    static final boolean BLADE_LOG = Boolean.getBoolean("si.wingman.bladeLog") || "wingman_blade".equals(System.getProperty("si.capture.scene"));

    /** Hover at the target within reach, on the side the Wingman is on (no circling). */
    private void holdAtBlade(LivingEntity enemy, ServerPlayer player, boolean close) {
        if (onGround() && chain.resting(level().getGameTime())) { setDeltaMovement(0, -0.1, 0); return; }
        Vec3 side = position().subtract(enemy.position()).multiply(1, 0, 1);
        side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 at = enemy.position().add(side.scale(close ? 2.0 : 2.6)).add(0, enemy.getBbHeight() * 0.35, 0);
        steer(at, COMBAT, player);
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
        if (cooldown >= 0) {
            attackCooldown = Math.max(2, cooldown);
            entityData.set(DATA_FIRE_CD, attackCooldown);           // 0.1.11.46: the clients time the draw and the reload by it
            toldEmpty = false;
            wear(weapon, EquipmentSlot.MAINHAND, 1);                // 0.1.11.42: a shot wears the weapon (arrows stay free)
        } else {
            // 0.1.11.42 (decision 2 C): could not fire (no charge): melee for 10 s, then try again; the owner is told once.
            // Before, the Wingman hovered at its firing station and tried again every second without a word.
            attackCooldown = 10;
            dryUntil = tickCount + 200;
            tellEmpty(weapon);
        }
    }

    /** Fires the held ranged weapon along {@code dir}. Returns the cooldown, or -1. */
    private int fire(ItemStack weapon, Vec3 dir, LivingEntity enemy) {
        var item = weapon.getItem();
        if (item instanceof com.miophas.singularity_iteration.core.api.item.IWeaponItem gun) {
            // 0.1.10.13: no arm swing per shot (see entangledShot); the clients show the recoil
            return gun.fireFrom(weapon, this, dir, true);
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
        return Math.abs(Mth.wrapDegrees(yaw - facing())) + Math.abs(pitch - getXRot());
    }

    /** Turns toward {@code at} by at most {@code maxStep} degrees per tick (no snapping). */
    private void faceSmooth(Vec3 at, float maxStep) {
        Vec3 d = at.subtract(getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        float dy = Mth.clamp(Mth.wrapDegrees(yaw - facing()), -maxStep, maxStep);
        setYaw(facing() + dy);
        setXRot(getXRot() + Mth.clamp(pitch - getXRot(), -maxStep, maxStep));
    }

    /** The mob the owner hit in the last 2 s, if it is a valid target (0.1.8.4: owner focus). */
    @Nullable
    private LivingEntity ownerFocus(ServerPlayer player) {
        LivingEntity marked = player.getLastHurtMob();
        double leash = WingmanConfig.leash();
        // 0.1.11.43 (G-7): the owner's hit calls the squad only on a hostile creature (a stray hit on a villager, a golem or a
        // friend's horse no longer turns every Wingman on it); a neutral one is a target when it fights the owner
        if (marked != null && marked instanceof Enemy && player.tickCount - player.getLastHurtMobTimestamp() < 40 && valid(marked, player, leash)) return marked;
        // 0.1.10.1 Fleet Datalink synergy: the creature in the owner's sights
        LivingEntity sighted = com.miophas.singularity_iteration.common.suit.quantum.QuantumSynergies.mark(player);
        return sighted instanceof Enemy && valid(sighted, player, leash * 2) ? sighted : null;
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

    /**
     * Escort place of this Wingman: behind and to the side of the owner, at shoulder height; 0.1.11.51 (F2): with speed it
     * follows the flight direction and opens into a V ({@link WingmanFlight#place}).
     */
    private Vec3 formation(ServerPlayer player) {
        // 0.1.8.4: tighter guard when the owner is below 40 % health
        double k = player.getHealth() < player.getMaxHealth() * 0.4F ? 0.6 : 1.0;
        return WingmanFlight.place(player.position(), player.getYRot(), speedStarved ? Vec3.ZERO : ownerVelocity, slot, k);
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
        faceFlight(v, player);
    }

    /** Without a target the Wingman looks where it flies, or slowly the way its owner looks when it hovers. */
    private void faceFlight(Vec3 v, @Nullable ServerPlayer player) {
        if (target != null) return;
        if (v.horizontalDistanceSqr() > 0.02) faceSmooth(getEyePosition().add(v.x, 0, v.z), 20);
        else if (player != null) setYaw(facing() + Mth.clamp(Mth.wrapDegrees(player.getYRot() - facing()), -15, 15));
    }

    private void face(Vec3 at) {
        Vec3 d = at.subtract(getEyePosition());
        setYaw((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        setXRot((float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z))));
    }

    /**
     * The way the Wingman looks and aims (its head). 0.1.11.47 (motion audit M4): the head turns first, the body follows at
     * {@link #BODY_TURN} degrees per tick (before, both snapped together, up to 45 degrees in a tick of combat).
     */
    float facing() { return yHeadRot; }

    /** Most degrees per tick the body turns after the head. */
    static final float BODY_TURN = 18.0F;

    /** 0.1.11.52 (P0): most degrees between the head and the body (the body is pulled along beyond it). */
    static final float NECK = 75.0F;

    private void setYaw(float yaw) {
        yHeadRot = yaw;
        float body = getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - getYRot()), -BODY_TURN, BODY_TURN);
        float neck = Mth.wrapDegrees(yaw - body);
        if (Math.abs(neck) > NECK) body = yaw - Math.copySign(NECK, neck);
        setYRot(body);
        yBodyRot = body;
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
        boolean fresh = id >= 0 && tickCount - lastGlare >= GLARE_REST;
        // 0.1.11.58 (P6): the kill of the last target and a new lock are called
        int before = entityData.get(DATA_TARGET);
        if (before >= 0 && world.getEntity(before) instanceof LivingEntity old && old.isDeadOrDying()) WingmanCallouts.say(this, WingmanCallouts.Kind.KILL, old.getName());
        if (id >= 0 && target != null) WingmanCallouts.say(this, WingmanCallouts.Kind.LOCK, target.getName());
        entityData.set(DATA_TARGET, id);
        // 0.1.11.52 (P0, decision one A): a new lock starts with a stare: the Wingman stops, turns its head on the enemy and
        // holds it there for GLARE ticks before the first attack (at most once per GLARE_REST ticks, so a fight that
        // switches targets does not stall)
        if (fresh) { glareTicks = GLARE; lastGlare = tickCount; }
        if (id < 0) glareTicks = 0;
        station = null;
        hitPause = 0;   // 0.1.11.46: a new target starts without the pause of the last hit
        if (id >= 0) world.playSound(null, getX(), getY(), getZ(), mio_icif_sounds.WINGMAN_LOCK.get(), SoundSource.NEUTRAL, 0.5F, 1.0F);
    }

    private void catchUp(ServerPlayer player) {
        Vec3 at = formation(player).add(0, 2, 0);
        teleportTo(at.x, at.y, at.z);
        setDeltaMovement(ownerVelocity);   // 0.1.11.50: it arrives at the owner's speed (it stopped dead before and fell behind again)
        stuckTicks = 0;
        stuckBest = Double.MAX_VALUE;
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
        flushSwap();   // 0.1.11.54: pieces of a swap in flight go back to the suit (or home) with the rest
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
        if (home != null && reserve > 0) {
            // 0.1.11.41: the reserve that was not flown goes back (as far as the Armory has room; without the Armory it is lost)
            home.getEnergyStorageInternal().generateEnergyInternal(reserve, false);
        }
        reserve = 0;
        if (home != null) {
            home.releaseDeployed(set, sortie);   // 0.1.11.39 (G-1)
            home.markStorageChanged();
        }
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
        reserve = 0;                    // 0.1.11.41: nor its reserve (the real Wingman has it)
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
    protected boolean canAddPassenger(Entity passenger) { return false; }   // 0.1.11.43: no Fulton cargo any more

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

    // 0.1.10.11 client animation state: the flight factor and the landing blend are filtered per
    // tick and read with the partial tick (before, the lean and the arms jumped at every tick with
    // the raw speed of the last tick); the slash counter starts the arm cut.
    private float flight, flightO, land, landO;
    private int seenSlash = Integer.MIN_VALUE, slashStart = -100, slashKind;
    // 0.1.10.13: combat (a target is locked: the weapon guard, the aim) and the recoil of firing, both
    // filtered per tick like the flight factor; the recoil holds while shots keep coming, so a fast gun
    // gives a steady braced pose instead of a kick every 2 ticks
    private float combat, combatO, recoil, recoilO;
    // 0.1.11.44 (motion audit M0, M-4): the smoothed horizontal velocity in the body frame (forward, right), for the lean
    private float moveF, moveFO, moveR, moveRO;
    // 0.1.11.45 (M1): the smoothed climb, turn rate and braking, and the tick of the last thruster boost (the leg tuck)
    private float moveU, moveUO, turn, turnO, brake, brakeO;
    private int seenBoosts = Integer.MIN_VALUE, boostAt = -100;
    /** 0.1.11.51 (F2): the smoothed weight of the high-speed pose, and its smoothing per tick. */
    private float prone, proneO;
    static final float PRONE_SMOOTH = 0.12F;
    // 0.1.11.47 (M4): the ticks of the last touchdown and take-off (the cushion, the push-off)
    private boolean wasDown;
    private int landedAt = -100, tookOffAt = -100;
    /** Smoothing per tick of the turn rate and the braking (the attitude test checks them as the entity uses them). */
    public static final float TURN_SMOOTH = 0.10F, BRAKE_SMOOTH = 0.15F;
    private int seenShots = Integer.MIN_VALUE, lastShot = -100, shotCooldown = 20;
    /** The recoil holds this many ticks after a shot (a gun firing faster keeps a steady braced pose). */
    private static final int RECOIL_HOLD = 8;

    // 0.1.11.52 (P0, decision three A): the idle glances of this Wingman (client), spaced from the other Wingmen's
    private final WingmanGlance glance = new WingmanGlance();
    private static final WingmanGlance.Shared GLANCES = new WingmanGlance.Shared();

    /** Client: the idle glance as a head offset {yaw, pitch}, radians (0.1.11.52). */
    public float[] glance(float partialTick) { return glance.offset(partialTick); }

    /** Client: what an idle glance looks at: a creature just hurt, else the nearest one (not a Wingman), or null. */
    @Nullable
    private float[] glancePick() {
        if (getRandom().nextFloat() < 0.3F) return null;   // now and then just a look round
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12),
                e -> e != this && e.isAlive() && !(e instanceof WingmanEntity))) {
            double score = distanceToSqr(e) - (e.hurtTime > 0 ? 1000 : 0);
            if (score < bestScore) { bestScore = score; best = e; }
        }
        if (best == null) return null;
        Vec3 d = best.getEyePosition().subtract(getEyePosition());
        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-d.x, d.z)) - yBodyRot);
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        return new float[]{yaw * Mth.DEG_TO_RAD, pitch * Mth.DEG_TO_RAD};
    }

    // 0.1.11.58 (P6): battle damage on the client: sparks below half durability, smoke and an unsteady flight below a quarter
    private int damageLevel;

    /** Client: the damage level of the worst worn armour piece ({@link WingmanPose#damageLevel}). */
    public int damageLevel() { return damageLevel; }

    /** Client: the unsteady flight of a badly damaged suit, {pitch, roll} degrees. */
    public float[] damageShake(float partialTick) {
        return WingmanPose.damageShake(tickCount + partialTick, damageLevel, 1 - landBlend(partialTick));
    }

    private void tickDamageFx() {
        if (tickCount % 10 == 0) {
            float worst = 1;
            for (ArmoryPiece p : ArmoryPiece.VALUES) {
                ItemStack s = getItemBySlot(p.slot);
                if (!p.isArmor() || !wears(s)) continue;
                worst = Math.min(worst, (s.getMaxDamage() - s.getDamageValue()) / (float) s.getMaxDamage());
            }
            damageLevel = WingmanPose.damageLevel(worst);
        }
        if (damageLevel == 0) return;
        var r = getRandom();
        if (r.nextFloat() < (damageLevel == 2 ? 0.2F : 0.08F)) {
            double x = getX() + (r.nextDouble() - 0.5) * 0.6, y = getY() + 0.6 + r.nextDouble(), z = getZ() + (r.nextDouble() - 0.5) * 0.6;
            for (int i = 0; i < 3; i++)
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, (r.nextDouble() - 0.5) * 0.3, r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.3);
        }
        if (damageLevel < 2) return;
        Vec3 v = getDeltaMovement();
        if (r.nextBoolean())
            level().addParticle(ParticleTypes.SMOKE, getX() - v.x * 0.5, getY() + 1.1, getZ() - v.z * 0.5, -v.x * 0.1, 0.03, -v.z * 0.1);
        if (r.nextFloat() < 0.03F) level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 0, -0.05, 0);   // a sputtering thruster
    }

    private void tickAnimation() {
        flightO = flight;
        landO = land;
        moveFO = moveF;
        proneO = prone;
        moveRO = moveR;
        moveUO = moveU;
        turnO = turn;
        brakeO = brake;
        combatO = combat;
        recoilO = recoil;
        boolean down = landed();
        if (down != wasDown) {
            if (down) landedAt = tickCount;
            else tookOffAt = tickCount;
            wasDown = down;
        }
        double dx = getX() - xo, dz = getZ() - zo;
        float want = down ? 0 : (float) Mth.clamp(Math.sqrt(dx * dx + dz * dz) * 1.6, 0, 1);
        flight += (want - flight) * 0.2F;
        float[] frame = WingmanPose.bodyFrame(down ? 0 : dx, down ? 0 : dz, yBodyRot);
        moveF += (frame[0] - moveF) * 0.2F;
        moveR += (frame[1] - moveR) * 0.2F;
        moveU += ((down ? 0 : (float) (getY() - yo)) - moveU) * 0.2F;
        // 0.1.11.51 (F2): the high-speed pose comes and goes over about a second
        prone += (WingmanPose.proneTarget(moveF, down ? 0 : 1) - prone) * PRONE_SMOOTH;
        turn += (Mth.wrapDegrees(yBodyRot - yBodyRotO) - turn) * TURN_SMOOTH;   // slow: a snap turn in combat must not snap the bank
        // lost forward speed per tick (moveF is smoothed by 0.2), at most a full brake: a reversal must not snap the flare
        brake += (Math.min(WingmanPose.FULL_BRAKE, Math.max(0, moveFO - moveF) * 5) - brake) * BRAKE_SMOOTH;
        int boosts = entityData.get(DATA_BOOSTS);
        if (boosts != seenBoosts) {
            if (seenBoosts != Integer.MIN_VALUE) boostAt = tickCount;
            seenBoosts = boosts;
        }
        land += ((down ? 1 : 0) - land) * 0.2F;
        combat += ((lockedTargetId() >= 0 ? 1 : 0) - combat) * 0.25F;
        int shots = entityData.get(DATA_SHOTS);
        if (shots != seenShots) {
            if (seenShots != Integer.MIN_VALUE) lastShot = tickCount;
            seenShots = shots;
            shotCooldown = Math.max(2, entityData.get(DATA_FIRE_CD));
        }
        recoil += ((tickCount - lastShot <= RECOIL_HOLD ? 1 : 0) - recoil) * 0.35F;
        glance.tick(level().getGameTime(), (1 - combat) * (1 - flight) * (1 - prone), getRandom(), GLANCES, this::glancePick);
        int s = entityData.get(DATA_SLASH);
        if (s != seenSlash) {
            if (seenSlash != Integer.MIN_VALUE) { slashStart = tickCount; slashKind = s & 3; }
            seenSlash = s;
        }
    }

    /** Client: ticks since the last shot, with the partial tick (0.1.11.46: the draw, the reload, the throw). */
    public float shotAge(float partialTick) { return tickCount - lastShot + partialTick; }
    /** Client: the weapon's cooldown at the last shot, ticks. */
    public int shotCooldown() { return shotCooldown; }

    /** Client: the distance to the locked target, or a large value without one (0.1.11.46: the melee wind-up). */
    public float targetDistance() {
        int id = lockedTargetId();
        Entity e = id < 0 ? null : level().getEntity(id);
        return e == null ? 99 : distanceTo(e);
    }

    /** Client: 0 (no target) .. 1 (target locked), smoothed (0.1.10.13). */
    public float combat(float partialTick) { return Mth.lerp(partialTick, combatO, combat); }
    /** Client: 0 .. 1 while firing, smoothed (0.1.10.13). */
    public float recoil(float partialTick) { return Mth.lerp(partialTick, recoilO, recoil); }

    /** Client: the tilt {pitch, roll} in degrees from the smoothed flight direction (0.1.11.44, {@link WingmanPose#lean}). */
    public float[] lean(float partialTick) {
        // 0.1.11.45 (M1): with the bank of a turn, the climb or dive and the flare of a brake; 0.1.11.51 (F2): lying into
        // the flight at high speed
        return WingmanPose.prone(attitude(partialTick), proneBlend(partialTick));
    }

    /** Client: weight 0..1 of the high-speed pose (0.1.11.51), interpolated. */
    public float proneBlend(float partialTick) { return Mth.lerp(partialTick, proneO, prone) * (1 - landBlend(partialTick)); }

    private float[] attitude(float partialTick) {
        return WingmanPose.attitude(Mth.lerp(partialTick, moveFO, moveF), Mth.lerp(partialTick, moveRO, moveR),
            Mth.lerp(partialTick, moveUO, moveU), Mth.lerp(partialTick, turnO, turn), Mth.lerp(partialTick, brakeO, brake),
            1 - landBlend(partialTick));
    }

    /** Client: ticks since the last touchdown, with the partial tick (0.1.11.47: the cushion). */
    public float sinceLanding(float partialTick) { return tickCount - landedAt + partialTick; }
    /** Client: ticks since the last take-off, with the partial tick (0.1.11.47: the push-off). */
    public float sinceTakeoff(float partialTick) { return tickCount - tookOffAt + partialTick; }

    /** Client: ticks since the last thruster boost (with the partial tick), for the leg tuck (0.1.11.45). */
    public float boostAge(float partialTick) {
        return tickCount - boostAt + partialTick;
    }

    /** Client: 0 (hover) .. 1 (full speed), smoothed. */
    public float flight(float partialTick) { return Mth.lerp(partialTick, flightO, flight); }
    /** Client: 0 (flying) .. 1 (standing on the ground), smoothed. */
    public float landBlend(float partialTick) { return Mth.lerp(partialTick, landO, land); }
    /** Client: progress 0..1 of the current blade slash, or -1 without one. */
    public float slashProgress(float partialTick) {
        int ticks = slashKind == SLASH_ARTS ? 14 : slashKind == SLASH_DASH ? 7 : BLADE_COMBO_TICKS;
        float t = (tickCount - slashStart + partialTick) / ticks;
        return t < 0 || t > 1 ? -1 : t;
    }
    /** Client: kind of the current slash ({@link #SLASH_COMBO}, {@link #SLASH_DASH}, {@link #SLASH_ARTS}). */
    public int slashKind() { return slashKind; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && isAlive()) {
            tickAnimation();
            tickDamageFx();
            if (landed()) return;   // 0.1.10.11: thrusters off on the ground
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
        tag.putLong("Reserve", reserve);                       // 0.1.11.41
        tag.putByte("Mode", entityData.get(DATA_MODE));
        tag.putByte("State", state());
        tag.putBoolean("Protected", protectedReturn);
        // 0.1.10.2 (absent in older saves: squad mode, no order); a focus order is not saved
        tag.putBoolean("Individual", individual);
        // 0.1.11.54: the pieces of a swap in flight (they go back to the suit when the Wingman loads)
        if (swapPhase != SWAP_NONE) {
            var provider = registryAccess();
            net.minecraft.nbt.ListTag out = new net.minecraft.nbt.ListTag(), in = new net.minecraft.nbt.ListTag();
            for (int i = 0; i < ArmoryPiece.VALUES.length; i++) {
                out.add(swapOut[i].isEmpty() ? new CompoundTag() : swapOut[i].save(provider));
                in.add(swapIn[i].isEmpty() ? new CompoundTag() : swapIn[i].save(provider));
            }
            tag.put("SwapOut", out);
            tag.put("SwapIn", in);
        }
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
        reserve = Math.max(0, tag.getLong("Reserve"));   // 0.1.11.41 (absent in older saves: no reserve)
        entityData.set(DATA_MODE, (byte) WingmanMode.byId(tag.getByte("Mode")).ordinal());   // 0.1.11.43: Fulton (3) loads as Escort
        byte state = tag.getByte("State");
        entityData.set(DATA_STATE, state == RETURNING ? RETURNING : ACTIVE);                   // 0.1.11.43: a Fulton lift (2) is over
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
        if (tag.contains("SwapOut")) {
            var provider = registryAccess();
            net.minecraft.nbt.ListTag out = tag.getList("SwapOut", net.minecraft.nbt.Tag.TAG_COMPOUND), in = tag.getList("SwapIn", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < ArmoryPiece.VALUES.length; i++) {
                swapOut[i] = i < out.size() ? ItemStack.parseOptional(provider, out.getCompound(i)) : ItemStack.EMPTY;
                swapIn[i] = i < in.size() ? ItemStack.parseOptional(provider, in.getCompound(i)) : ItemStack.EMPTY;
            }
            swapPhase = SWAP_RESUME;   // flushed on the first tick
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
