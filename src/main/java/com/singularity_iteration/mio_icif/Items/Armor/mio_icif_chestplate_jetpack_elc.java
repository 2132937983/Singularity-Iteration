package com.singularity_iteration.mio_icif.Items.Armor;

import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.api.item.ArmorFeatureInfo;
import com.singularity_iteration.mio_icif.api.item.IJetpackItem;
import com.singularity_iteration.mio_icif.util.JetpackKeyHandler;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 电力喷气背包胸甲
 * 基于IC2 JetpackLogic重构，修复各种bug
 */
@SuppressWarnings({"null", "deprecation"})
public class mio_icif_chestplate_jetpack_elc extends mio_icif_armor_elc implements IJetpackItem, ICurioItem {

    // ========== IC2 标准属性 ==========
    // 最大电量 30,000 EU (IC2标准)
    public static final int JETPACK_MAX_ENERGY = 30000;
    
    // 传输限制 60 EU/t (IC2标准)
    public static final int TRANSFER_LIMIT = 60;
    
    // 等级 1 (IC2标准)
    public static final int TIER = 1;

    // 飞行每tick消耗的能量 (IC2标准: 2 EU/t)
    public static final int ENERGY_PER_TICK = 2;
    
    // 悬停模式每tick消耗的能量 (IC2标准: 1 EU/t)
    public static final int HOVER_ENERGY_PER_TICK = 1;

    // 喷气背包动力系数 (IC2标准: 0.7F)
    public static final float JETPACK_POWER = 0.7F;
    
    // 最大上升速度 (IC2标准: 0.6，削减为原来的三分之二)
    public static final double MAX_ASCENT_SPEED = 0.4D;
    
    // 悬浮模式上升速度 (IC2标准: 0.1F)
    public static final float HOVER_ASCENT_SPEED = 0.1F;
    
    // 悬浮模式下降速度 (IC2标准: -0.1F)
    public static final float HOVER_DESCENT_SPEED = -0.1F;
    
    // 世界高度除数 (IC2标准: 1.28F)
    public static final float WORLD_HEIGHT_DIVISOR = 1.28F;
    
    // 电量下降阈值 (IC2标准: 5%)
    public static final float DROP_PERCENTAGE = 0.05F;

    // NBT 键名
    private static final String MODE_KEY = "JetpackMode";
    private static final String TOGGLE_TIMER_KEY = "JetpackToggleTimer";

    // 模式常量
    public static final int MODE_JETPACK = 0;   // 喷气模式
    public static final int MODE_HOVER = 1;     // 悬浮模式

    // 音效播放间隔 (每20 ticks播放一次)
    private static final int SOUND_INTERVAL = 20;
    
    // 模式切换冷却时间 (10 ticks = 0.5秒)
    private static final int TOGGLE_COOLDOWN = 10;

    /**
     * 构造函数
     */
    public mio_icif_chestplate_jetpack_elc(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.CHESTPLATE, properties, JETPACK_MAX_ENERGY, 0, "armor_jetpack", TRANSFER_LIMIT, ENERGY_PER_TICK, TIER);
    }

    // ========== 模式管理 ==========

    public int toggleMode(ItemStack stack) {
        int currentMode = getModeInternal(stack);
        int newMode = (currentMode == MODE_JETPACK) ? MODE_HOVER : MODE_JETPACK;
        setModeInternal(stack, newMode);
        return newMode;
    }

    public int getToggleTimer(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(TOGGLE_TIMER_KEY)) {
                return tag.getInt(TOGGLE_TIMER_KEY);
            }
        }
        return 0;
    }
    
    public void setToggleTimer(ItemStack stack, int timer) {
        CompoundTag tag = new CompoundTag();
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putInt(TOGGLE_TIMER_KEY, timer);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }

    public Component getModeName(int mode) {
        return switch (mode) {
            case MODE_JETPACK -> Component.translatable("hud.mio_icif.jetpack.mode_jetpack");
            case MODE_HOVER -> Component.translatable("hud.mio_icif.jetpack.mode_hover");
            default -> Component.translatable("hud.mio_icif.jetpack.mode_jetpack");
        };
    }

    // ========== 飞行状态跟踪 ==========
    
    // 跟踪正在飞行的玩家
    private static final Map<Player, Boolean> flyingPlayers = new WeakHashMap<>();
    
    // 跟踪上次播放音效的时间
    private static final Map<Player, Long> lastSoundTime = new WeakHashMap<>();

    // ========== 核心飞行逻辑 ==========

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;

        ItemStack actualStack = player.getItemBySlot(EquipmentSlot.CHEST);
        boolean isWearingChestplate = !actualStack.isEmpty() && actualStack.getItem() == this;

        if (isWearingChestplate) {
            tickJetpack(player, actualStack, level);
        }
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player)) return;
        if (!slotContext.identifier().equals("back")) return;

        ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestStack.isEmpty() && chestStack.getItem() instanceof mio_icif_chestplate_jetpack_elc) return;

        tickJetpack(player, stack, player.level());
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return slotContext.identifier().equals("back");
    }

    private void tickJetpack(Player player, ItemStack actualStack, Level level) {
        int currentMode = getModeInternal(actualStack);
        long currentEnergy = getEnergy(actualStack);
        int toggleTimer = getToggleTimer(actualStack);

        if (level.isClientSide && player.tickCount % 20 == 0) {
            showModeInfo(player, actualStack, currentMode);
        }

        int requiredEnergy = (currentMode == MODE_HOVER) ? HOVER_ENERGY_PER_TICK : ENERGY_PER_TICK;
        if (currentEnergy < requiredEnergy) {
            flyingPlayers.remove(player);
            return;
        }

        boolean isJumping = JetpackKeyHandler.isJumpKeyDown(player);

        if (JetpackKeyHandler.isModeSwitchKeyDown(player) && toggleTimer == 0) {
            int newMode = toggleMode(actualStack);
            toggleTimer = TOGGLE_COOLDOWN;
            setToggleTimer(actualStack, toggleTimer);
            if (!level.isClientSide) {
                if (newMode == MODE_HOVER) {
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
                    player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_enabled"));
                } else {
                    player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_disabled"));
                }
            }
        }

        if (toggleTimer > 0) {
            toggleTimer--;
            setToggleTimer(actualStack, toggleTimer);
        }

        boolean jetpackUsed = false;

        if (currentMode == MODE_JETPACK) {
            if (isJumping) {
                jetpackUsed = useJetpack(player, actualStack, (int) currentEnergy);
            }
        } else if (currentMode == MODE_HOVER) {
            if (isJumping || !player.onGround()) {
                jetpackUsed = useJetpackHover(player, actualStack, (int) currentEnergy, isJumping);
            }
        }

        if (jetpackUsed) {
            flyingPlayers.put(player, true);
            player.fallDistance = 0.0F;
        } else {
            flyingPlayers.remove(player);
        }

        if (currentMode == MODE_HOVER && player.onGround() && !level.isClientSide) {
            Boolean wasFlying = flyingPlayers.get(player);
            if (wasFlying != null && wasFlying) {
                setModeInternal(actualStack, MODE_JETPACK);
                player.sendSystemMessage(Component.translatable("hud.mio_icif.jetpack.hover_disabled"));
            }
        }
    }

    /**
     * 喷气模式飞行逻辑 (基于IC2 JetpackLogic.useJetpack)
     */
    private boolean useJetpack(Player player, ItemStack stack, int currentEnergy) {
        // 计算动力（根据电量百分比衰减）
        float power = calculatePower(currentEnergy);
        
        // 应用前进推力
        applyForwardThrust(player, power, false);
        
        // 应用高度限制
        power = applyHeightLimit(player, power);
        
        // 计算垂直速度
        double currentY = player.getDeltaMovement().y;
        
        // 基础推力
        double thrust = power * 0.2F;
        
        // 关键修复：限制每tick的速度变化，让反冲更平滑
        // 最大速度变化 = 0.25，这样从-0.8到0需要至少4个tick，过渡更自然
        double maxDeltaV = 0.25;
        double targetY = Math.min(currentY + thrust, MAX_ASCENT_SPEED);
        
        // 如果目标速度比当前速度大很多（减速或转向），限制变化率
        if (targetY > currentY + maxDeltaV) {
            targetY = currentY + maxDeltaV;
        }
        
        double newY = targetY;
        
        // 应用速度
        player.setDeltaMovement(player.getDeltaMovement().x, newY, player.getDeltaMovement().z);
        player.hasImpulse = true;
        
        // 消耗能量（IC2逻辑：不在地面上时才消耗）
        if (!player.onGround()) {
            consumeEnergy(stack, ENERGY_PER_TICK);
        }
        
        return true;
    }
    
    /**
     * 悬停模式飞行逻辑 (基于IC2 JetpackLogic.useJetpackHover)
     */
    private boolean useJetpackHover(Player player, ItemStack stack, int currentEnergy, boolean isJumping) {
        // 使用IC2风格的独立按键状态系统检查潜行键
        boolean isDescending = JetpackKeyHandler.isSneakKeyDown(player);
        
        // 在地面上且不按空格时不激活
        if (player.onGround() && !isJumping) {
            return false;
        }
        
        // 计算动力
        float power = calculatePower(currentEnergy);
        
        // 应用前进推力（悬停模式下推力更大）
        applyForwardThrust(player, power, true);
        
        // 应用高度限制
        power = applyHeightLimit(player, power);
        
        // 获取当前速度（应用推力后的速度）
        double currentMotionY = player.getDeltaMovement().y;
        
        // 计算新速度
        double newMotionY = Math.min(currentMotionY + power * 0.2F, MAX_ASCENT_SPEED);
        
 // 悬停模式速度限制和冲
        float maxHoverY = 0.0F;
        if (isJumping) {
            maxHoverY += HOVER_ASCENT_SPEED;
        }
        if (isDescending) {
            maxHoverY += HOVER_DESCENT_SPEED;
        }
        
 // IC2关键冲逻辑：如果新速度超过限制，限制为最大速度
        // 但如果之前的速度大于新速度，保持之前的速度（防止突然下降）
        if (newMotionY > maxHoverY) {
            newMotionY = maxHoverY;
            if (currentMotionY > newMotionY) {
                newMotionY = currentMotionY;
            }
        }
        
        // 应用速度
        player.setDeltaMovement(player.getDeltaMovement().x, newMotionY, player.getDeltaMovement().z);
        player.hasImpulse = true;
        
        // 消耗能量（IC2逻辑：不在地面上时才消耗）
        if (!player.onGround()) {
            consumeEnergy(stack, HOVER_ENERGY_PER_TICK);
        }

        return true;
    }

    /**
     * 计算动力 (基于IC2 JetpackLogic.getPower)
     */
    private float calculatePower(int currentEnergy) {
        float power = JETPACK_POWER;
        float chargeLevel = (float) currentEnergy / JETPACK_MAX_ENERGY;
        
        // 电量低于5%时，动力按比例下降
        if (chargeLevel <= DROP_PERCENTAGE) {
            power = power * (chargeLevel / DROP_PERCENTAGE);
        }
        
        return power;
    }

    /**
     * 应用前进推力 (基于IC2 JetpackLogic.applyForwardThrust)
     */
    private void applyForwardThrust(Player player, float power, boolean hoverMode) {
        // 使用IC2风格的独立按键状态系统检查前进键
        if (!JetpackKeyHandler.isForwardKeyDown(player)) return;
        
        // 基础推力系数 (喷气模式0.15，悬停模式1.0)
        float thruster = hoverMode ? 1.0F : 0.15F;
        float forwardPower = power * thruster * 2.0F;
        
        if (forwardPower <= 0.0F) return;
        
        // 计算推力
        float thrust = 0.4F * forwardPower;
        float friction = 0.02F;
        
        // 获取玩家朝向
        float yaw = player.getYRot();
        float rad = (float) Math.toRadians(yaw);
        
        double mx = player.getDeltaMovement().x;
        double mz = player.getDeltaMovement().z;
        
        // 计算前进方向
        double forwardX = -Math.sin(rad);
        double forwardZ = Math.cos(rad);
        
        // 应用推力
        mx += forwardX * thrust * friction;
        mz += forwardZ * thrust * friction;
        
        player.setDeltaMovement(mx, player.getDeltaMovement().y, mz);
    }

    /**
     * 应用最大飞行高度限制 (基于IC2 JetpackLogic.applyHeightLimit)
     */
    private float applyHeightLimit(Player player, float power) {
        if (!hasHeightLimit()) {
            return power;
        }
        int maxFlightHeight = (int) getMaxHeight();
        double y = player.getY();

        if (y > (maxFlightHeight - 25)) {
            if (y > maxFlightHeight) {
                y = maxFlightHeight;
            }
            power = (float) (power * (maxFlightHeight - y) / 25.0D);
        }

        return power;
    }

    /**
     * 消耗能量
     */
    @SuppressWarnings("unused")
    private void consumeEnergy(Player player, ItemStack stack, int amount) {
        long currentEnergy = getEnergy(stack);
        long newEnergy = Math.max(0, currentEnergy - amount);
        setEnergy(stack, newEnergy);
        
        // 同步装备状态到客户端
        if (!player.level().isClientSide) {
            player.setItemSlot(EquipmentSlot.CHEST, stack);
        }
    }

    /**
     * 播放喷气背包音效
     */
    @SuppressWarnings("unused")
    private void playJetpackSound(Player player, Level level) {
        long currentTime = level.getGameTime();
        Long lastTime = lastSoundTime.get(player);
        
        if (lastTime == null || currentTime - lastTime >= SOUND_INTERVAL) {
            level.playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS,
                0.5F,
                1.0F,
                false
            );
            lastSoundTime.put(player, currentTime);
        }
    }

    /**
     * 显示模式信息
     */
    private void showModeInfo(Player player, ItemStack stack, int mode) {
        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();
        var api = MioIcifAPI.instance().getItemAPI();
        boolean holdingElectricTool = api.isElectricTool(mainHandStack) ||
                                      api.isElectricTool(offHandStack);
        
        if (!holdingElectricTool) {
            Component modeName = getModeName(mode);
            player.displayClientMessage(
                Component.translatable("hud.mio_icif.jetpack.display",
                    modeName, getEnergy(stack), JETPACK_MAX_ENERGY), true);
        }
    }

    // ========== 公共方法 ==========

    /**
     * 检查玩家是否正在喷气背包飞行中
     */
    public static boolean isPlayerFlying(Player player) {
        return flyingPlayers.containsKey(player);
    }

    /**
     * 获取电量百分比 (0.0 - 1.0)
     */
    public double getChargeLevel(ItemStack stack) {
        return (double) getEnergy(stack) / JETPACK_MAX_ENERGY;
    }

    // ========== 重写方法 ==========

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = new ItemStack(this);
        setEnergy(stack, 0);
        return stack;
    }

    private static final ResourceLocation JETPACK_ARMOR_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("mio_icif", "textures/armor/jetpack_1.png");

    @Override
    @Nullable
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return JETPACK_ARMOR_TEXTURE;
    }

    @Override
    public List<ArmorFeatureInfo> getFeatures(ItemStack stack) {
        return List.of(
            new ArmorFeatureInfo(EquipmentSlot.CHEST, "jetpack_mode", "tooltip.mio_icif.armor.feature_jetpack_mode", getModeName(getModeInternal(stack)))
        );
    }

    // ==================== IJetpackItem API ====================

    @Override
    public float getThrust() {
        return JETPACK_POWER;
    }

    @Override
    public long getEnergyPerTickFlying() {
        return ENERGY_PER_TICK;
    }

    @Override
    public float getMaxHeight() {
        return 310.0F;
    }

    @Override
    public JetpackMode getMode(ItemStack stack) {
        int internal = getModeInternal(stack);
        return internal == MODE_HOVER ? JetpackMode.HOVER : JetpackMode.FLIGHT;
    }

    @Override
    public void setMode(ItemStack stack, JetpackMode mode) {
        int internal = switch (mode) {
            case OFF -> MODE_JETPACK;
            case NORMAL -> MODE_JETPACK;
            case HOVER -> MODE_HOVER;
            case FLIGHT -> MODE_JETPACK;
        };
        setModeInternal(stack, internal);
    }

    public int getModeInternal(ItemStack stack) {
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(MODE_KEY)) {
                return tag.getInt(MODE_KEY);
            }
        }
        return MODE_JETPACK;
    }

    public void setModeInternal(ItemStack stack, int mode) {
        CompoundTag tag = new CompoundTag();
        net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            tag = customData.copyTag();
        }
        tag.putInt(MODE_KEY, mode);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
}