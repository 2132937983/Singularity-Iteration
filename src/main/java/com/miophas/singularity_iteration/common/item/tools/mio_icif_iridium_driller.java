package com.miophas.singularity_iteration.common.item.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.miophas.singularity_iteration.core.api.item.IEquipmentHudProvider;
import com.miophas.singularity_iteration.core.api.tool.IMiningDrill;
import com.miophas.singularity_iteration.core.api.tool.ToggleableElectricTool;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * Iridium drill.
 * It extends the electric tool base class and uses energy.
 * It has all the properties of a diamond pickaxe. It does not break when the durability reaches 0.
 * It mines twice as fast as a diamond pickaxe and has a built-in Fortune III effect.
 * Sneak and right-click to switch to Silk Touch mode.
 */
@SuppressWarnings("null")
public class mio_icif_iridium_driller extends mio_icif_tool_elc implements com.miophas.singularity_iteration.core.api.tool.IToolModeProvider, IMiningDrill, IEquipmentHudProvider {

    // Default maximum energy of the iridium drill (original IC2: 1000000 EU).
    public static final int IRIDIUM_DRILLER_MAX_ENERGY = 1000000;

    // Transfer limit of the iridium drill (original IC2: 2000 EU/t).
    public static final int IRIDIUM_DRILLER_TRANSFER_LIMIT = 2000;

    // Energy that the iridium drill uses each time (original IC2: 800 EU).
    public static final int IRIDIUM_DRILLER_ENERGY_PER_USE = 800;

    // 铱钻头的攻击伤害 (IC2原版: 20.0)
    public static final float IRIDIUM_DRILLER_ATTACK_DAMAGE = 20.0F;

    // 铱钻头的挖掘效率 (IC2原版: 25.0)
    public static final float IRIDIUM_DRILLER_EFFICIENCY = 25.0F;

    // 钻石镐的挖掘等级（继承材质）
    private static final Tier DIAMOND_TIER = Tiers.DIAMOND;

    // NBT key and mode values
private static final String MODE_KEY = "iridium_driller_mode";
    private static final int MODE_FORTUNE = 0;  // 时运模式
    private static final int MODE_SILK_TOUCH = 1;  // 精准采集模式

    /**
     * Default constructor. The drill has no energy and starts in Fortune mode.
 * @param properties the item properties
 */
    public mio_icif_iridium_driller(Properties properties) {
        super(properties, IRIDIUM_DRILLER_MAX_ENERGY, IRIDIUM_DRILLER_MAX_ENERGY, "iridium_driller", IRIDIUM_DRILLER_TRANSFER_LIMIT, IRIDIUM_DRILLER_ENERGY_PER_USE, 3);
    }

    /**
     * 带初始能量的构造函数
 * @param properties 物品属性
 * @param initialEnergy 初始能量
     */
    public mio_icif_iridium_driller(Properties properties, int initialEnergy) {
        super(properties, IRIDIUM_DRILLER_MAX_ENERGY, IRIDIUM_DRILLER_MAX_ENERGY - initialEnergy, "iridium_driller", IRIDIUM_DRILLER_TRANSFER_LIMIT, IRIDIUM_DRILLER_ENERGY_PER_USE, 3);
    }

    /**
     * 获取当前模式
     * @param stack 物品栈
 * @return 0=时运模式, 1=精准采集模式
     */
    private int getMode(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(MODE_KEY)) {
                return tag.getInt(MODE_KEY);
            }
        }
        return MODE_FORTUNE; // 默认时运模式
    }

    /**
     * 设置模式
     * @param stack 物品栈
 * @param mode 0=时运模式, 1=精准采集模式
     */
    private void setMode(ItemStack stack, int mode) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag;
        if (customData != null) {
            tag = customData.copyTag();
        } else {
            tag = new CompoundTag();
        }
        tag.putInt(MODE_KEY, mode);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 切换模式
     * @param stack 物品栈
 * @return 切换后的模式
     */
    private int toggleMode(ItemStack stack) {
        int currentMode = getMode(stack);
        int newMode = (currentMode == MODE_FORTUNE) ? MODE_SILK_TOUCH : MODE_FORTUNE;
        setMode(stack, newMode);
        return newMode;
    }

    @Override
    public void toggleActive(ItemStack stack, Player player) {
        int newMode = toggleMode(stack);
        Component modeName = (newMode == MODE_FORTUNE) ?
            Component.translatable("hud.mio_icif.driller.mode_fortune") :
            Component.translatable("hud.mio_icif.driller.mode_silk");
        player.sendSystemMessage(
            Component.translatable("hud.mio_icif.driller.switch_mode", modeName)
        );
    }

    /**
     * 右键使用（已移除蹲下切换模式，改用G键）
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    /**
     * 处理附魔等级事件
     * 这是NeoForge提供的机制，用于动态修改物品的附魔等级
     */
    public static void onGetEnchantmentLevel(GetEnchantmentLevelEvent event) {
        ItemStack stack = event.getStack();
        
        // Check that the item is an iridium drill.
    if (!(stack.getItem() instanceof mio_icif_iridium_driller driller)) {
            return;
        }
        
        int currentMode = driller.getMode(stack);
        ItemEnchantments.Mutable enchantments = event.getEnchantments();
        HolderLookup.RegistryLookup<Enchantment> lookup = event.getLookup();
        
        // Add the enchantment for the current mode.
    if (currentMode == MODE_FORTUNE) {
            // Fortune mode: add Fortune.
            lookup.get(Enchantments.FORTUNE).ifPresent(holder -> {
                if (event.isTargetting(holder)) {
                    enchantments.set(holder, 3);
                }
            });
        } else if (currentMode == MODE_SILK_TOUCH) {
            // Silk Touch mode: add Silk Touch.
        lookup.get(Enchantments.SILK_TOUCH).ifPresent(holder -> {
                if (event.isTargetting(holder)) {
                    enchantments.set(holder, 1);
                }
            });
        }
    }

    /**
     * 检查是否可以挖掘指定方法
 */
    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!hasEnoughEnergy(stack)) {
            // 没电时只能挖掘手可以挖的方块
            return state.getDestroySpeed(level, pos) > 0 && !state.requiresCorrectToolForDrops();
        }
        // 有电时可以挖掘任何可破坏的方法
    return state.getDestroySpeed(level, pos) >= 0;
    }

    /**
     * Checks if the tool can perform an action (pickaxe and shovel actions).
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

        // 保留斧头动作支持，防止挖掘木头时进度重置
        // 但getDestroySpeed中不会给予快速挖掘速度
        // isCorrectToolForDrops确保木头不会掉落
        if (ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility)) {
            return true;
        }

        return super.canPerformAction(stack, itemAbility);
    }

    /**
     * 获取挖掘速度
     * 钻头只能快速挖掘镐子和铲子类方法
 */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) {
            return 1.0F;
        }

        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) {
            return IRIDIUM_DRILLER_EFFICIENCY;
        }

        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)) {
            return IRIDIUM_DRILLER_EFFICIENCY;
        }

        return 1.0F;
    }

    /**
     * 挖掘方块时消耗能量
     * 对齐IC2原版：只在方块实际被破坏后消耗能量，且只在硬度不为0时消耗
     */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        // 只在服务端、方块硬度不为0时消耗能量
        if (!level.isClientSide && state.getDestroySpeed(level, pos) != 0.0F) {
            consumeEnergy(stack);
        }
        return true;
    }

    /**
     * Checks if the drill is the correct tool for drops.
 */
    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) {
            return false;
        }
        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) {
            return true;
        }
        if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)) {
            return true;
        }
        return false;
    }

    /**
     * 重写：防止钻头损坏
 */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, java.util.function.Consumer<net.minecraft.world.item.Item> onBroken) {
        return 0;
    }

    /**
     * 重写：钻头永远不会消耗
 */
    @Override
    public boolean isDamaged(ItemStack stack) {
        return getEnergy(stack) < getMaxEnergy();
    }

    /**
     * 是否可以附魔
     */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    /**
     * Returns the enchantability.
 */
    @Override
    public int getEnchantmentValue() {
        return DIAMOND_TIER.getEnchantmentValue();
    }

    /**
     * Shows the mode and battery information above the hotbar, with colors.
 */
    @Override
    public Component getEquipmentHudText(ItemStack stack) {
        Component modeName = (getMode(stack) == MODE_FORTUNE) ?
            Component.translatable("hud.mio_icif.driller.mode_fortune") :
            Component.translatable("hud.mio_icif.driller.mode_silk");
        return Component.translatable("hud.mio_icif.driller.display",
            modeName, getEnergy(stack), getMaxEnergy());
    }

    /**
     * Returns the attack damage bonus.
     * The iridium drill adds 20 points of damage.
 */
    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource damageSource) {
        return IRIDIUM_DRILLER_ATTACK_DAMAGE;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    // ==================== IMiningDrill 接口实现 ====================

    @Override
    public long energyUse(ItemStack stack, BlockGetter world, BlockPos pos, BlockState state) {
        return IRIDIUM_DRILLER_ENERGY_PER_USE;
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


    // ---- equipment console (IToolModeProvider)
    @Override public java.util.List<net.minecraft.network.chat.Component> toolModes(ItemStack stack) {
        return java.util.List.of(net.minecraft.network.chat.Component.translatable("hud.mio_icif.driller.mode_fortune"), net.minecraft.network.chat.Component.translatable("hud.mio_icif.driller.mode_silk"));
    }
    @Override public int toolModeIndex(ItemStack stack) { return getMode(stack) == MODE_FORTUNE ? 0 : 1; }
}
