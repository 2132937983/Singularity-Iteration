// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.item.tools;

import com.miophas.singularity_iteration.core.api.tool.IMiningDrill;
import com.miophas.singularity_iteration.core.api.tool.IToolModeProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * The three mining drills, 0.1.11.196 (tool overhaul T3). The iron and the iridium drill were two copies of about 250
 * lines each and the diamond drill a third, shorter version, and they disagreed: different drop tiers, axe abilities on two
 * of them, a dead drill that could not break a torch, and an attack that cost nothing and hit hard without energy.
 *
 * <p>Rules shared by every drill:
 * <ul>
 *   <li>Mines pickaxe and shovel blocks below its tier ({@code incorrect} tag) fast and with drops while charged; with no
 *       energy it mines like a hand (speed 1, no drops for blocks that need a tool) instead of refusing the block.</li>
 *   <li>Area modes by tier (decision D3): G cycles the area, a mined block of the area costs a use, an area mines slower,
 *       and every extra block goes through the player's own break (protection, events, enchantments, drops).</li>
 *   <li>Attack (decision D4): a hit costs a use; with no energy the drill has no attack damage. The damage stays at or
 *       under a sword of its tier.</li>
 *   <li>{@link IMiningDrill} for machines: the block is checked, protected, broken with the drill's drops.</li>
 * </ul>
 */
public abstract class AbstractDrill extends mio_icif_tool_elc implements IMiningDrill, IToolModeProvider {

    /** Custom-data key of the area index (0 = 1×1, 1 = 3×3, 2 = 5×5). */
    public static final String AREA_KEY = "DrillArea";
    /** Mining speed factor of an area mode, by radius: a 3×3 mines at half speed, a 5×5 at 0.3. */
    private static final float[] AREA_SPEED = {1.0F, 0.5F, 0.3F};
    /** An extra block harder than {@code centre × 2 + 1} is left (an ore under obsidian, a reinforced wall). */
    private static final ThreadLocal<Boolean> AREA_BREAKING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final TagKey<Block> incorrect;
    private final float efficiency;
    private final int maxRadius;
    private final int enchantmentValue;
    private final ItemAttributeModifiers attack;

    /**
     * @param incorrect   blocks this drill gets no drops from (the vanilla {@code incorrect_for_*_tool} tag of its tier)
     * @param attackBonus attack damage added to the wielder's base (a player hits for this + 1)
     * @param maxRadius   largest area radius (0 = 1×1 only, 1 = up to 3×3, 2 = up to 5×5)
     */
    protected AbstractDrill(Properties properties, long maxEnergy, long chargeRate, long energyPerUse, int tier,
                            TagKey<Block> incorrect, float efficiency, double attackBonus, int maxRadius, int enchantmentValue) {
        super(properties, maxEnergy, 0L, "drill", chargeRate, energyPerUse, tier);
        this.incorrect = incorrect;
        this.efficiency = efficiency;
        this.maxRadius = maxRadius;
        this.enchantmentValue = enchantmentValue;
        this.attack = attackDamage(attackBonus);
    }

    // ==================== mining ====================

    /** A pickaxe or shovel block this drill's tier reaches. */
    public boolean mineable(BlockState state) {
        return (state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL)) && !state.is(incorrect);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack) || !mineable(state)) return 1.0F;
        return efficiency * AREA_SPEED[Math.min(getRadius(stack), AREA_SPEED.length - 1)];
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return hasEnoughEnergy(stack) && mineable(state);
    }

    /** Pickaxe and shovel actions only (two drills claimed axe actions, so other mods used them as axes). */
    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility ability) {
        return hasEnoughEnergy(stack)
            && (ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(ability) || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(ability));
    }

    @Override
    public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (level.isClientSide || state.getDestroySpeed(level, pos) == 0.0F) return true;
        boolean paid = consumeEnergy(stack);
        int radius = getRadius(stack);
        if (paid && radius > 0 && !AREA_BREAKING.get() && miner instanceof ServerPlayer player && mineable(state)) {
            mineArea(player, stack, pos, state, radius);
        }
        return true;
    }

    /** The plane round {@code centre} facing the player, broken block by block as the player would break it. */
    private void mineArea(ServerPlayer player, ItemStack stack, BlockPos centre, BlockState centreState, int radius) {
        ServerLevel level = player.serverLevel();
        float limit = centreState.getDestroySpeed(level, centre) * 2.0F + 1.0F;
        Direction.Axis normal = facingAxis(player);
        AREA_BREAKING.set(Boolean.TRUE);
        try {
            for (int a = -radius; a <= radius; a++) {
                for (int b = -radius; b <= radius; b++) {
                    if (a == 0 && b == 0) continue;
                    if (!hasEnoughEnergy(stack) || player.getMainHandItem() != stack) return;
                    BlockPos pos = switch (normal) {
                        case X -> centre.offset(0, a, b);
                        case Y -> centre.offset(a, 0, b);
                        case Z -> centre.offset(a, b, 0);
                    };
                    if (!level.isLoaded(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    float hardness = state.getDestroySpeed(level, pos);
                    if (state.isAir() || hardness < 0 || hardness > limit || !mineable(state)) continue;
                    player.gameMode.destroyBlock(pos);   // protection, break event, drops with this drill, mineBlock (the cost)
                }
            }
        } finally {
            AREA_BREAKING.set(Boolean.FALSE);
        }
    }

    /** The axis the player looks along: down or up when the view is steep, else the horizontal facing. */
    private static Direction.Axis facingAxis(Player player) {
        float pitch = player.getXRot();
        if (pitch > 45.0F || pitch < -45.0F) return Direction.Axis.Y;
        return player.getDirection().getAxis();
    }

    // ==================== attack (D4) ====================

    /** The attack damage while a hit can be paid; none without energy. */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return hasEnoughEnergy(stack) ? attack : ItemAttributeModifiers.EMPTY;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) consumeEnergy(stack);
        return true;
    }

    // ==================== area modes (D3) ====================

    public int getMaxRadius() {
        return maxRadius;
    }

    public static int getRadius(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        int r = data == null ? 0 : data.copyTag().getInt(AREA_KEY);
        return stack.getItem() instanceof AbstractDrill drill ? Math.max(0, Math.min(drill.maxRadius, r)) : 0;
    }

    public static void setRadius(ItemStack stack, int radius) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(AREA_KEY, radius));
    }

    private static Component areaName(int radius) {
        int side = radius * 2 + 1;
        return Component.translatable("tooltip.mio_icif.drill.area", side, side);
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        if (maxRadius == 0) return;
        int next = (getRadius(stack) + 1) % (maxRadius + 1);
        setRadius(stack, next);
        player.displayClientMessage(Component.translatable("message.mio_icif.drill.area", areaName(next)), true);
    }

    @Override
    public List<Component> toolModes(ItemStack stack) {
        List<Component> modes = new ArrayList<>();
        for (int r = 0; r <= maxRadius; r++) modes.add(areaName(r));
        return modes;
    }

    @Override
    public int toolModeIndex(ItemStack stack) {
        return getRadius(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (maxRadius > 0) {
            tooltip.add(Component.translatable("tooltip.mio_icif.drill.area_line", areaName(getRadius(stack))).withStyle(ChatFormatting.GRAY));
        }
    }

    // ==================== machines (IMiningDrill) ====================

    @Override
    public long energyUse(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        return state.getDestroySpeed(world, pos) == 0 ? 0 : getEnergyPerUse();
    }

    @Override
    public int breakTime(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(world, pos);
        if (hardness < 0) return -1;
        if (hardness == 0) return 0;
        return (int) Math.ceil(hardness * 30.0D / efficiency);
    }

    /**
     * A machine breaks {@code expected} with this drill: only the block it saw, only one the drill gets drops from, under
     * protection and the break event, with the drill's drops (its enchantments count). The energy is paid first.
     */
    @Override
    public boolean breakBlock(ItemStack stack, BlockGetter world, BlockPos pos, BlockState expected) {
        if (!(world instanceof ServerLevel level) || !level.isLoaded(pos)
                || !level.getBlockState(pos).equals(expected) || expected.getDestroySpeed(level, pos) < 0
                || !isCorrectToolForDrops(stack, expected)) return false;
        var actor = FakePlayerFactory.getMinecraft(level);
        if (!level.mayInteract(actor, pos) || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, expected, actor)).isCanceled()) return false;
        if (!level.getBlockState(pos).equals(expected)) return false;
        long cost = energyUse(stack, world, pos, expected);
        var blockEntity = level.getBlockEntity(pos);
        var drops = Block.getDrops(expected, level, pos, blockEntity, actor, stack);
        if (!consumeEnergy(stack, cost)) return false;
        if (!level.destroyBlock(pos, false, actor)) { addEnergy(stack, cost); return false; }
        for (ItemStack drop : drops) Block.popResource(level, pos, drop);
        return true;
    }

    // ==================== misc ====================

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    /** Unused here; kept so a subclass may read its own saved data next to the area. */
    protected static CompoundTag data(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
}
