// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.armory;

import com.miophas.singularity_iteration.common.Singularity_Iteration;
import com.miophas.singularity_iteration.common.suit.SuitEnergyPool;
import com.miophas.singularity_iteration.common.suit.SuitModuleType;
import com.miophas.singularity_iteration.common.suit.SuitModules;
import com.miophas.singularity_iteration.common.suit.core.FrameCore;
import com.miophas.singularity_iteration.common.suit.core.FrameCores;
import com.miophas.singularity_iteration.core.api.MioIcifAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Suit-module and Frame Core linkages for deployed Wingmen:
 * <ul>
 *   <li><b>Max Health scaling</b>: Living Nano Lining (+2.5 HP/piece) and Resonance Field (+4/+8 HP)
 *       raise the Wingman's durability above its base 20 HP.</li>
 *   <li><b>Out-of-combat self-repair</b>: Nano/Quantum Wingmen regenerate HP after 5 s out of combat
 *       at a modest EU cost (doubled with Living Nano Lining or Bio-Stim).</li>
 *   <li><b>Solar recharging</b>: Solar Array Frame Cores and Solar Skin recharge the Wingman's suit
 *       and sortie reserve under open sky.</li>
 *   <li><b>Weapon power direct feed</b>: Electric weapons draw charge directly from the Wingman's
 *       suit battery / sortie reserve instead of stalling out.</li>
 *   <li><b>Point-defense &amp; damage reduction</b>: Deflector widens Escort intercept range and
 *       reduces cooldown; Resonance Field adds flat damage reduction.</li>
 * </ul>
 */
public final class WingmanSuitSynergy {
    private WingmanSuitSynergy() {}

    private static final ResourceLocation HP_MOD_ID = ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "wingman_suit_hp");
    /** Out-of-combat delay before self-repair starts (5 seconds). */
    public static final int SELF_REPAIR_DELAY_TICKS = 100;
    /** EU paid per self-repair pulse (1/s). */
    public static final long SELF_REPAIR_EU = 160L;
    /** Base and Deflector-boosted Escort point-defense intercept radii (blocks). */
    public static final double BASE_INTERCEPT_RADIUS = 5.0;
    public static final double DEFLECTOR_INTERCEPT_RADIUS = 7.5;
    public static final double DEFLECTOR_MK2_INTERCEPT_RADIUS = 8.5;

    /** Updates transient Max Health modifier from the Wingman's worn suit modules and living lining. */
    public static void syncSuitAttributes(WingmanEntity w) {
        double bonus = 0.0;
        int resTier = 0;
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            ItemStack piece = w.getItemBySlot(s);
            if (piece.isEmpty() || !SuitModules.isSuitPiece(piece)) continue;
            if (SuitModules.hasLivingLining(piece)) bonus += 2.5;
            resTier = Math.max(resTier, SuitModules.tier(piece, SuitModuleType.RESONANCE_FIELD.id()));
        }
        if (resTier >= 2) bonus += 8.0;
        else if (resTier == 1) bonus += 4.0;

        var attr = w.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(HP_MOD_ID);
        if (bonus <= 0.0) {
            if (existing != null) attr.removeModifier(HP_MOD_ID);
        } else if (existing == null || Math.abs(existing.amount() - bonus) > 0.01) {
            boolean wasFull = w.getHealth() >= w.getMaxHealth() - 0.1F;
            attr.addOrUpdateTransientModifier(new AttributeModifier(HP_MOD_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
            if (wasFull) w.setHealth(w.getMaxHealth());
        }
        if (w.getHealth() > w.getMaxHealth()) w.setHealth(w.getMaxHealth());
    }

    /** Once-per-second server tick: solar charging, weapon power feed, and out-of-combat self-repair. */
    public static void tickSecond(WingmanEntity w, ServerLevel world, int outOfCombatTicks) {
        syncSuitAttributes(w);
        tickSolarRecharge(w, world);
        rechargeWeapon(w, 4_000L);
        if (outOfCombatTicks >= SELF_REPAIR_DELAY_TICKS) {
            tickSelfRepair(w, world);
        }
    }

    /** Recharges the Wingman's electric armor and sortie reserve from Solar Array Core / Solar Skin. */
    private static void tickSolarRecharge(WingmanEntity w, ServerLevel world) {
        BlockPos pos = w.blockPosition().above();
        if (!world.canSeeSky(pos)) return;
        boolean day = world.isDay() && !world.isRaining();
        long perTick = 0;
        ItemStack head = w.getItemBySlot(EquipmentSlot.HEAD);
        FrameCore core = FrameCores.core(head);
        if (core != null && core.isSolar()) {
            perTick += day ? core.solarDay() : core.solarNight();
        }
        if (day && hasModule(w, SuitModuleType.SOLAR_SKIN)) {
            perTick += 4;
        }
        if (perTick <= 0) return;
        long generated = perTick * 20L;
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            if (generated <= 0) break;
            ItemStack piece = w.getItemBySlot(s);
            if (piece.isEmpty()) continue;
            long took = ArmoryItemEnergy.charge(piece, generated, false);
            generated -= took;
        }
        if (generated > 0) {
            long cap = WingmanConfig.reserveEu();
            if (cap > 0 && w.reserve() < cap) {
                w.setReserve(Math.min(cap, w.reserve() + generated));
            }
        }
    }

    /** Self-repairs Nano/Quantum Wingmen out of combat using suit/reserve EU. */
    private static void tickSelfRepair(WingmanEntity w, ServerLevel world) {
        if (w.getHealth() >= w.getMaxHealth()) return;
        boolean hasSuit = false;
        boolean hasLining = false;
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            ItemStack piece = w.getItemBySlot(s);
            if (piece.isEmpty() || !SuitModules.isSuitPiece(piece)) continue;
            hasSuit = true;
            if (SuitModules.hasLivingLining(piece)) hasLining = true;
        }
        if (!hasSuit) return;
        if (!paySuitOrReserve(w, SELF_REPAIR_EU)) return;
        float heal = hasLining ? 2.0F : 1.0F;
        w.heal(heal);
        world.sendParticles(hasLining ? ParticleTypes.HAPPY_VILLAGER : ParticleTypes.ELECTRIC_SPARK,
            w.getX(), w.getY() + 1.0, w.getZ(), 3, 0.3, 0.4, 0.3, 0.02);
    }

    /** Feeds EU from a Nano/Quantum Wingman's electric armor / sortie reserve into its held electric weapon. */
    public static boolean rechargeWeapon(WingmanEntity w, long maxEu) {
        if (!hasSuitPiece(w)) return false;
        ItemStack weapon = w.getMainHandItem();
        if (weapon.isEmpty()) return false;
        long room = ArmoryItemEnergy.room(weapon);
        if (room <= 0) return false;
        long want = Math.min(room, maxEu);
        long drawn = drawSuitOrReserve(w, want);
        if (drawn <= 0) return false;
        long charged = ArmoryItemEnergy.charge(weapon, drawn, false);
        if (charged < drawn) {
            // Return any unused EU to reserve so nothing is wasted
            w.setReserve(w.reserve() + (drawn - charged));
        }
        return charged > 0;
    }

    public static boolean hasSuitPiece(WingmanEntity w) {
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            ItemStack piece = w.getItemBySlot(s);
            if (!piece.isEmpty() && SuitModules.isSuitPiece(piece)) return true;
        }
        return false;
    }

    /** Point-defense intercept radius in Escort mode. */
    public static double interceptRadius(WingmanEntity w) {
        int tier = maxModuleTier(w, SuitModuleType.DEFLECTOR);
        if (tier >= 2) return DEFLECTOR_MK2_INTERCEPT_RADIUS;
        if (tier == 1) return DEFLECTOR_INTERCEPT_RADIUS;
        return BASE_INTERCEPT_RADIUS;
    }

    /** Point-defense intercept cooldown in Escort mode (ticks). */
    public static int interceptCooldown(WingmanEntity w) {
        return maxModuleTier(w, SuitModuleType.DEFLECTOR) > 0 ? 2 : 3;
    }

    /** Incoming damage multiplier from Resonance Field module (1.0 without). */
    public static float damageMultiplier(WingmanEntity w) {
        int tier = maxModuleTier(w, SuitModuleType.RESONANCE_FIELD);
        if (tier >= 2) return 0.75F;
        if (tier == 1) return 0.85F;
        return 1.0F;
    }

    public static boolean hasModule(WingmanEntity w, SuitModuleType type) {
        return maxModuleTier(w, type) > 0;
    }

    public static int maxModuleTier(WingmanEntity w, SuitModuleType type) {
        int best = 0;
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            ItemStack piece = w.getItemBySlot(s);
            if (!piece.isEmpty()) best = Math.max(best, SuitModules.tier(piece, type.id()));
        }
        return best;
    }

    /** Draws up to {@code want} EU from the Wingman's electric armor pool, then its sortie reserve. */
    static long drawSuitOrReserve(WingmanEntity w, long want) {
        if (want <= 0) return 0;
        long drawn = 0;
        var api = MioIcifAPI.instance().getItemAPI();
        for (EquipmentSlot s : SuitEnergyPool.ARMOR_SLOTS) {
            if (drawn >= want) break;
            ItemStack piece = w.getItemBySlot(s);
            if (piece.isEmpty() || !api.isElectricArmor(piece) || SuitEnergyPool.available(w, piece) <= 0) continue;
            drawn += SuitEnergyPool.payPart(w, piece, want - drawn);
        }
        if (drawn < want && w.reserve() > 0) {
            long take = Math.min(w.reserve(), want - drawn);
            w.setReserve(w.reserve() - take);
            drawn += take;
        }
        return drawn;
    }

    /** Pays exactly {@code cost} EU if available across suit + reserve, returning true on success. */
    static boolean paySuitOrReserve(WingmanEntity w, long cost) {
        if (cost <= 0) return true;
        if (w.suitStored() + w.reserve() < cost) return false;
        return drawSuitOrReserve(w, cost) >= cost;
    }
}
