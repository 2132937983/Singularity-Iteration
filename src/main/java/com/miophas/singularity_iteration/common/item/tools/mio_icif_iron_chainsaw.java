package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import java.util.List;

/**
 * Iron chainsaw.
 * It extends the electric tool base class and uses energy.
 * It has all the properties of an iron axe. It does not break when the durability reaches 0.
 * It also has shear properties: it shears sheep and collects cobwebs and leaves intact.
 * With energy, it breaks blocks as fast as an iron axe. Without energy, it breaks blocks at hand speed.
 */
@SuppressWarnings("null")
public class mio_icif_iron_chainsaw extends mio_icif_tool_elc {

    // Default maximum energy of the iron chainsaw (IC2 original: 30000 EU).
    public static final int IRON_CHAINSAW_MAX_ENERGY = 30000;

    // Energy transfer limit of the iron chainsaw (IC2 original: 100 EU/t).
    public static final int IRON_CHAINSAW_TRANSFER_LIMIT = 100;

    // Energy that the iron chainsaw uses for each operation (IC2 original: 100 EU).
    public static final int IRON_CHAINSAW_ENERGY_PER_USE = 100;

    // Attack damage of the iron chainsaw (IC2 original: 9.0).
    public static final float IRON_CHAINSAW_ATTACK_DAMAGE = 9.0F;

    // Mining efficiency of the iron chainsaw (IC2 original: 8.0).
    public static final float IRON_CHAINSAW_EFFICIENCY = 8.0F;

    // Mining tier of the iron axe.
    private static final Tier IRON_TIER = Tiers.IRON;

    /**
     * Default constructor. The tool starts with no energy.
     * @param properties the item properties
     */
    public mio_icif_iron_chainsaw(Properties properties) {
        super(properties, IRON_CHAINSAW_MAX_ENERGY, IRON_CHAINSAW_MAX_ENERGY, "iron_chainsaw", IRON_CHAINSAW_TRANSFER_LIMIT, IRON_CHAINSAW_ENERGY_PER_USE, 1);
    }

    /**
     * Constructor with an initial energy value.
     * @param properties the item properties
     * @param initialEnergy the initial energy
     */
    public mio_icif_iron_chainsaw(Properties properties, int initialEnergy) {
        super(properties, IRON_CHAINSAW_MAX_ENERGY, IRON_CHAINSAW_MAX_ENERGY - initialEnergy, "iron_chainsaw", IRON_CHAINSAW_TRANSFER_LIMIT, IRON_CHAINSAW_ENERGY_PER_USE, 1);
    }

    /**
     * Returns the attack damage bonus.
     * The iron chainsaw adds 9 points of damage (IC2 original value).
     */
    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        return IRON_CHAINSAW_ATTACK_DAMAGE;
    }

    /**
     * Checks whether the tool can mine the specified block.
     * The iron chainsaw can mine all blocks that an iron axe can mine. It can also process all blocks that shears can process.
     */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        // Check that the tool has sufficient energy.
        ItemStack stack = player.getMainHandItem();
        if (!hasEnoughEnergy(stack)) {
            // Without energy, the tool can mine only the blocks that a hand can mine.
            return state.getDestroySpeed(level, pos) > 0 && !state.requiresCorrectToolForDrops();
        }
        // With energy, the chainsaw can mine the block.
        return true;
    }

    /**
     * Returns the mining speed.
     * With energy, the speed matches an iron axe or shears. Without energy, the speed matches the hand.
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Check that the tool has sufficient energy.
        if (!hasEnoughEnergy(stack)) {
            // Without energy, the speed is very slow (hand speed).
            return 1.0F;
        }

        // Shear blocks have high priority.
        if (state.is(Blocks.COBWEB)) {
            return 15.0F; // Cobweb: shear speed
        }
        if (state.is(BlockTags.LEAVES)) {
            return 15.0F; // Leaves: shear speed
        }
        if (state.is(BlockTags.WOOL)) {
            return 5.0F; // Wool: shear speed
        }
        if (state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN)) {
            return 2.0F; // Vines and glow lichen
        }

        // Blocks for axes
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            return IRON_CHAINSAW_EFFICIENCY;
        }

        // For all other blocks, return the base speed.
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
     * Checks if the chainsaw is the correct tool for drops.
     */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // Check that the tool has sufficient energy.
        if (!hasEnoughEnergy(stack)) {
            return false;
        }

        // Blocks for shears: the tool can always process these blocks.
        if (state.is(Blocks.COBWEB) ||
            state.is(BlockTags.LEAVES) ||
            state.is(BlockTags.WOOL) ||
            state.is(Blocks.VINE) ||
            state.is(Blocks.GLOW_LICHEN) ||
            state.is(Blocks.TRIPWIRE)) {
            return true;
        }

        // Blocks for axes
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) {
            // Check the mining level that the block needs.
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) {
                return false; // Needs a diamond tool.
            }
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) {
                return true; // Needs an iron tool.
            }
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) {
                return true; // Needs a stone tool.
            }
            // Blocks that do not need a specific tool
            return true;
        }

        return false;
    }

    /**
     * Handles a right click on an entity (for example, to shear a sheep).
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, net.minecraft.world.InteractionHand hand) {
        // Check that the tool has sufficient energy.
        if (!hasEnoughEnergy(stack)) {
            return InteractionResult.PASS;
        }

        // Check whether the entity can be sheared.
        if (entity instanceof net.neoforged.neoforge.common.IShearable target) {
            BlockPos pos = entity.blockPosition();
            boolean isClient = entity.level().isClientSide();

            // Check if the target can be sheared.
            if (target.isShearable(player, stack, entity.level(), pos)) {
                // Shear the entity on the server.
                if (!isClient) {
                    List<ItemStack> drops = target.onSheared(player, stack, entity.level(), pos);
                    for (ItemStack drop : drops) {
                        target.spawnShearedDrop(entity.level(), pos, drop);
                    }
                }

                // Send the shear game event.
                entity.gameEvent(GameEvent.SHEAR, player);

                // Use energy on the server.
                if (!isClient) {
                    consumeEnergy(stack);
                }

                return InteractionResult.sidedSuccess(isClient);
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Handles right-click on a block (strips logs and removes wax).
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        // Check that the tool has sufficient energy.
        if (!hasEnoughEnergy(stack)) {
            return InteractionResult.PASS;
        }

        // Try the axe right-click actions (log stripping and wax removal).
        // Use the NeoForge ItemAbilities system.
        if (player != null) {
            // Check whether the block is wood that the tool can strip.
            BlockState modifiedState = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
            if (modifiedState != null) {
                if (!level.isClientSide) {
                    level.setBlock(pos, modifiedState, 11);
                    level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, modifiedState));
                    consumeEnergy(stack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Check whether the block is a waxed copper block that the tool can scrape the wax from.
            modifiedState = state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false);
            if (modifiedState != null) {
                if (!level.isClientSide) {
                    level.setBlock(pos, modifiedState, 11);
                    level.levelEvent(player, 3005, pos, 0);
                    consumeEnergy(stack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }

            // Check whether the block is an oxidized copper block that the tool can scrape.
            modifiedState = state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false);
            if (modifiedState != null) {
                if (!level.isClientSide) {
                    level.setBlock(pos, modifiedState, 11);
                    level.levelEvent(player, 3004, pos, 0);
                    consumeEnergy(stack);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Checks whether the tool can do a specific action (shear actions and axe actions).
     */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        // Check that the tool has sufficient energy.
        if (!hasEnoughEnergy(stack)) {
            return false;
        }

        // Support all shear actions.
        if (ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility)) {
            return true;
        }

        // Support all axe actions.
        if (ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility)) {
            return true;
        }

        return super.canPerformAction(stack, itemAbility);
    }

    /**
     * Override: prevents damage to the chainsaw.
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        // The electric chainsaw does not take damage through damageItem. It uses energy instead.
        return 0;
    }

    /**
     * Override: the chainsaw does not wear out.
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
     * Gets the enchantment value (the same as an iron axe).
     */
    @Override
    public int getEnchantmentValue() {
        return IRON_TIER.getEnchantmentValue();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

}