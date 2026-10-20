package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.miophas.singularity_iteration.core.api.tool.IMiningDrill;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Iron drill.
 * Extends the electric tool base class and runs on electric power.
 * It has all properties of an iron pickaxe. It does not break when its durability reaches 0.
 * With power, it mines as fast as an iron pickaxe. Without power, it mines at hand speed.
 */
@SuppressWarnings("null")
public class mio_icif_iron_driller extends mio_icif_tool_elc implements IMiningDrill {

    // Default maximum energy of the iron drill (IC2 original: 30000 EU).
    public static final int IRON_DRILLER_MAX_ENERGY = 30000;

    // Energy transfer limit of the iron drill (IC2 original: 100 EU/t).
    public static final int IRON_DRILLER_TRANSFER_LIMIT = 100;

    // Energy that the iron drill uses for each operation (IC2 original: 50 EU).
    public static final int IRON_DRILLER_ENERGY_PER_USE = 50;

    // Attack damage of the iron drill (SI value; IC2 gives drills only their material's damage).
    public static final float IRON_DRILLER_ATTACK_DAMAGE = 8.0F;

    // Mining efficiency of the iron drill (IC2 original: 8.0).
    public static final float IRON_DRILLER_EFFICIENCY = 8.0F;

    // Mining tier of an iron pickaxe.
    private static final Tier IRON_TIER = Tiers.IRON;

    /**
     * Default constructor. The tool starts with no energy.
     * @param properties the item properties
     */
    public mio_icif_iron_driller(Properties properties) {
        super(properties, IRON_DRILLER_MAX_ENERGY, IRON_DRILLER_MAX_ENERGY, "iron_driller", IRON_DRILLER_TRANSFER_LIMIT, IRON_DRILLER_ENERGY_PER_USE, 1);
    }

    /**
     * Constructor with an initial energy value.
     * @param properties the item properties
     * @param initialEnergy the initial energy
     */
    public mio_icif_iron_driller(Properties properties, int initialEnergy) {
        super(properties, IRON_DRILLER_MAX_ENERGY, IRON_DRILLER_MAX_ENERGY - initialEnergy, "iron_driller", IRON_DRILLER_TRANSFER_LIMIT, IRON_DRILLER_ENERGY_PER_USE, 1);
    }

    private static final net.minecraft.world.item.component.ItemAttributeModifiers ATTACK = attackDamage(IRON_DRILLER_ATTACK_DAMAGE);

    /** 8 attack damage (9 in a player's hand), as an attribute: shown and used by mobs too. */
    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return ATTACK;
    }

    /**
     * Checks whether the tool can mine the specified block.
     * The iron drill can mine all blocks that an iron pickaxe can mine.
     */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        // Check that the tool has sufficient energy.
        ItemStack stack = player.getMainHandItem();
        if (!hasEnoughEnergy(stack)) {
            // Without energy, the tool can mine only the blocks that a hand can mine.
            return state.getDestroySpeed(level, pos) > 0 && !state.requiresCorrectToolForDrops();
        }
        // With energy, the tool can mine the blocks that an iron pickaxe can mine.
        return true;
    }

    /**
     * Checks whether the tool can do a given action (pickaxe and shovel actions).
     * This method prevents the mining progress reset.
     * NeoForge calls this method during mining to check the tool energy.
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (!hasEnoughEnergy(stack)) {
            return false;
        }

        if (ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility)) {
            return true;
        }

        if (ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility)) {
            return true;
        }

        // Keep support for axe actions. This prevents a reset of the mining progress on wood.
        // getDestroySpeed does not give a high mining speed for wood.
        // isCorrectToolForDrops makes sure that wood does not drop.
        if (ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility)) {
            return true;
        }

        return super.canPerformAction(stack, itemAbility);
    }

    /**
     * Returns the mining speed.
     * With power, the speed equals an iron pickaxe. Without power, the speed equals mining by hand.
     * The drill mines only pickaxe blocks and shovel blocks fast.
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) {
            return 1.0F;
        }

        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) {
            return IRON_DRILLER_EFFICIENCY;
        }

        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)) {
            return IRON_DRILLER_EFFICIENCY;
        }

        return 1.0F;
    }

    /**
     * Uses energy when the tool mines a block.
     * This matches the original IC2. The tool uses energy only after the block breaks. It uses energy only when the block hardness is not 0.
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        // Use energy only on the server, and only when the block hardness is not 0.
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            consumeEnergy(stack);
        }
        return true;
    }

    /**
     * Checks whether the tool is the correct tool to get drops from the block.
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) {
            return false;
        }
        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) {
            if (state.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)) {
                return false;
            }
            return true;
        }
        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)) {
            if (state.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL)) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Override: prevents damage to the drill.
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        // The electric drill does not take damage through damageItem. It uses energy instead.
        return 0;
    }

    /**
     * Override: the drill does not wear out.
     */
    @Override
    public boolean isDamaged(ItemStack stack) {
        // Return true when the energy is not full. This shows the durability bar.
        return getEnergy(stack) < getMaxEnergy();
    }

    /**
     * Checks whether the tool can be enchanted.
     */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    /**
     * Gets the enchantment value (the same as an iron pickaxe).
     */
    @Override
    public int getEnchantmentValue() {
        return IRON_TIER.getEnchantmentValue();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    // ==================== IMiningDrill interface implementation ====================

    @Override
    public long energyUse(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        return IRON_DRILLER_ENERGY_PER_USE;
    }

    @Override
    public int breakTime(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness < 0.0F) return -1;
        if (hardness == 0.0F) return 0;
        float speed = getDestroySpeed(stack, state);
        if (speed <= 0.0F) speed = 1.0F;
        return (int) Math.ceil((hardness * 1.5F / speed) * 20.0F);
    }

    @Override
    public boolean breakBlock(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        if (!(world instanceof Level level) || level.isClientSide) return false;
        if (!hasEnoughEnergy(stack)) return false;
        if (state.getDestroySpeed(level, pos) < 0.0F) return false;
        // IMiningDrill is public API: an addon call must respect protection like the diamond driller
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            var actor = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(server);
            if (!level.mayInteract(actor, pos) || net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
                    new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level, pos, state, actor)).isCanceled()) return false;
        }
        boolean removed = level.destroyBlock(pos, true);
        if (removed) consumeEnergy(stack);
        return removed;
    }

}