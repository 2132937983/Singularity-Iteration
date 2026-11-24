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
public class mio_icif_iron_chainsaw extends mio_icif_tool_elc implements com.miophas.singularity_iteration.core.api.tool.IToolModeProvider {

    // Default maximum energy of the iron chainsaw (IC2 original: 30000 EU).
    public static final int IRON_CHAINSAW_MAX_ENERGY = 30000;

    // Energy transfer limit of the iron chainsaw (IC2 original: 100 EU/t).
    public static final int IRON_CHAINSAW_TRANSFER_LIMIT = 100;

    // Energy that the iron chainsaw uses for each operation (IC2 original: 100 EU).
    public static final int IRON_CHAINSAW_ENERGY_PER_USE = 100;

    /**
     * Added to the wielder's base damage. 0.1.11.196 (T3, D4): a player hits for 6, an iron sword (it was 10 and cost
     * nothing); a hit costs a use and needs energy.
     */
    public static final float IRON_CHAINSAW_ATTACK_DAMAGE = 5.0F;
    /** 0.1.11.196 (T3): a leaf, wool, cobweb or vine costs this much instead of a full use (100 EU per leaf was too dear). */
    public static final int IRON_CHAINSAW_SHEAR_COST = 25;
    /** 0.1.11.196 (T3): most logs one cut fells in felling mode. */
    public static final int FELLING_MAX_LOGS = 128;
    /** 0.1.11.196 (T3): felling reaches at most this many blocks sideways from the cut log. */
    public static final int FELLING_RADIUS = 8;
    private static final String FELLING_KEY = "ChainsawFelling";
    private static final ThreadLocal<Boolean> FELLING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    // Mining efficiency of the iron chainsaw (IC2 original: 8.0).
    public static final float IRON_CHAINSAW_EFFICIENCY = 8.0F;
    /** 0.1.10.39: the dig speed for crop sticks: they break at once, also in the air or under water (where speed counts a fifth). */
    public static final float CROP_STICK_SPEED = 1000.0F;

    // Mining tier of the iron axe.
    private static final Tier IRON_TIER = Tiers.IRON;

    /**
     * Default constructor. The tool starts with no energy.
     * @param properties the item properties
     */
    public mio_icif_iron_chainsaw(Properties properties) {
        super(properties, IRON_CHAINSAW_MAX_ENERGY, 0L, "iron_chainsaw", IRON_CHAINSAW_TRANSFER_LIMIT, IRON_CHAINSAW_ENERGY_PER_USE, 1);
    }

    /**
     * Constructor with an initial energy value.
     * @param properties the item properties
     * @param initialEnergy the initial energy
     */
    public mio_icif_iron_chainsaw(Properties properties, int initialEnergy) {
        super(properties, IRON_CHAINSAW_MAX_ENERGY, IRON_CHAINSAW_MAX_ENERGY - initialEnergy, "iron_chainsaw", IRON_CHAINSAW_TRANSFER_LIMIT, IRON_CHAINSAW_ENERGY_PER_USE, 1);
    }

    private static final net.minecraft.world.item.component.ItemAttributeModifiers ATTACK = attackDamage(IRON_CHAINSAW_ATTACK_DAMAGE);


    // 0.1.11.196 (T3, M-04): no canAttackBlock override; a flat chainsaw cuts like a hand (speed 1, no drops) instead of
    // refusing the block (it could not even break a torch).

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

        // 0.1.10.39: crop sticks break at once (by hand they take 1.5 s, and a slip breaks the wrong one)
        if (state.is(com.miophas.singularity_iteration.common.util.mio_icif_tags.CROP_STICKS)) return CROP_STICK_SPEED;

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
        if (level.isClientSide || state.getDestroySpeed(level, pos) == 0.0F) return true;
        boolean shearing = state.is(Blocks.COBWEB) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL)
            || state.is(Blocks.VINE) || state.is(Blocks.GLOW_LICHEN);
        boolean paid = consumeEnergy(stack, shearing ? IRON_CHAINSAW_SHEAR_COST : IRON_CHAINSAW_ENERGY_PER_USE);
        if (paid && isFelling(stack) && state.is(BlockTags.LOGS) && !FELLING.get()
                && miningEntity instanceof net.minecraft.server.level.ServerPlayer player) {
            fell(player, stack, pos);
        }
        return true;
    }

    /**
     * 0.1.11.196 (T3): felling mode cuts the logs joined to the cut one (diagonals count, so branches go too), level with it
     * or above, within {@link #FELLING_RADIUS} sideways, at most {@link #FELLING_MAX_LOGS}; each log is cut as the player
     * cuts it (protection, break event, drops, a use of energy).
     */
    private void fell(net.minecraft.server.level.ServerPlayer player, ItemStack stack, BlockPos start) {
        Level level = player.level();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        queue.add(start);
        seen.add(start);
        int cut = 0;
        FELLING.set(Boolean.TRUE);
        try {
            while (!queue.isEmpty() && cut < FELLING_MAX_LOGS) {
                BlockPos at = queue.poll();
                for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                    BlockPos next = at.offset(dx, dy, dz);
                    if (!seen.add(next) || Math.abs(next.getX() - start.getX()) > FELLING_RADIUS
                            || Math.abs(next.getZ() - start.getZ()) > FELLING_RADIUS || !level.isLoaded(next)
                            || !level.getBlockState(next).is(BlockTags.LOGS)) continue;
                    if (!hasEnoughEnergy(stack) || player.getMainHandItem() != stack || cut >= FELLING_MAX_LOGS) return;
                    if (player.gameMode.destroyBlock(next)) {
                        cut++;
                        queue.add(next);
                    }
                }
            }
        } finally {
            FELLING.set(Boolean.FALSE);
        }
    }

    public static boolean isFelling(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(FELLING_KEY);
    }

    /** G switches felling mode (0.1.11.196, T3). */
    @Override
    public void toggleActive(ItemStack stack, Player player) {
        boolean on = !isFelling(stack);
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putBoolean(FELLING_KEY, on));
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.mio_icif.chainsaw.mode",
            net.minecraft.network.chat.Component.translatable(on ? "tooltip.mio_icif.chainsaw.felling" : "tooltip.mio_icif.chainsaw.single")), true);
    }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> toolModes(ItemStack stack) {
        return java.util.List.of(net.minecraft.network.chat.Component.translatable("tooltip.mio_icif.chainsaw.single"),
            net.minecraft.network.chat.Component.translatable("tooltip.mio_icif.chainsaw.felling"));
    }

    @Override
    public int toolModeIndex(ItemStack stack) {
        return isFelling(stack) ? 1 : 0;
    }

    /** 0.1.11.196 (T3, D4): the attack damage while a hit can be paid, none without energy. */
    @Override
    public net.minecraft.world.item.component.ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return hasEnoughEnergy(stack) ? ATTACK : net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) consumeEnergy(stack);
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
            return !state.is(BlockTags.INCORRECT_FOR_IRON_TOOL);   // 0.1.11.196 (T3, M-07): an iron tool's tag, as the drills
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