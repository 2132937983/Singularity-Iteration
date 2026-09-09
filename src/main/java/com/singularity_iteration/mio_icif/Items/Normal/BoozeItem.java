package com.singularity_iteration.mio_icif.Items.Normal;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * IC2饮品物品基类
 * 实现啤酒、朗姆酒等饮品的饮用效果
 */
@SuppressWarnings("null")
public class BoozeItem extends Item {

    // IC2原版属性名称
    public static final String[] SOLID_RATIO_NAMES = { "Watery ", "Clear ", "Lite ", "", "Strong ", "Thick ", "Stodge " };
    public static final String[] HOPS_RATIO_NAMES = { "Soup ", "Alcfree ", "White ", "", "Dark ", "Full ", "Black " };
    public static final String[] TIME_RATIO_NAMES = { "Brew", "Youngster", "Beer", "Ale", "Dragonblood", "Black Stuff" };

    // 基础持续时间
    public static final int[] BASE_DURATION = { 300, 600, 900, 1200, 1600, 2000, 2400 };
    // 基础强度
    public static final float[] BASE_INTENSITY = { 0.4F, 0.75F, 1.0F, 1.5F, 2.0F };

    private final BoozeType type;

    public BoozeItem(BoozeType type, Properties properties) {
        super(properties.food(createFoodProperties(type)));
        this.type = type;
    }

    @Nonnull
    private static FoodProperties createFoodProperties(BoozeType type) {
        FoodProperties.Builder builder = new FoodProperties.Builder();

        // 所有酒水在饱食度满时也能饮用
        builder.alwaysEdible();

        switch (type) {
            case BREW:
                // 酿造液 - 几乎没有营养
                builder.nutrition(1).saturationModifier(0.1f);
                break;
            case YOUNGSTER:
                // 新手酒 - 少量营养
                builder.nutrition(2).saturationModifier(0.2f);
                break;
            case BEER:
                // 啤酒 - 标准营养
                builder.nutrition(4).saturationModifier(0.6f);
                break;
            case ALE:
                // 麦酒 - 更多营养
                builder.nutrition(6).saturationModifier(0.8f);
                break;
            case RUM:
                // 朗姆酒 - 高热量
                builder.nutrition(3).saturationModifier(0.5f);
                break;
        }

        return builder.build();
    }

    @Override
    @Nonnull
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player player) {
            applyEffects(player, stack);
        }

        // 饮用后返还空杯子
        ItemStack result = super.finishUsingItem(stack, level, entity);

        if (entity instanceof Player player) {
            // 尝试给玩家空杯子
            ItemStack emptyMug = new ItemStack(mio_icif_normal.EMPTY_MUG.get());
            if (!player.getInventory().add(emptyMug)) {
                player.drop(emptyMug, false);
            }
        }

        return result;
    }

    @Override
    @Nonnull
    public UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.DRINK;
    }

    /**
     * 从ItemStack获取元数据（优先从CUSTOM_DATA读取，兼容旧版damage value）
     */
    public static int getMetaFromStack(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("BoozeMeta")) {
                return tag.getInt("BoozeMeta");
            }
        }
        // 兼容旧版damage value
        return stack.getDamageValue();
    }

    /**
     * 应用饮品效果
     */
    private void applyEffects(Player player, ItemStack stack) {
        int meta = getMetaFromStack(stack);

        switch (type) {
            case BREW:
                // 酿造液 - 轻微恶心效果
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                break;

            case YOUNGSTER:
                // 新手酒 - 轻微恢复
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
                break;

            case BEER:
                // 啤酒 - 根据元数据计算效果
                applyBeerEffects(player, meta);
                break;

            case ALE:
                // 麦酒 - 更强的啤酒效果
                applyAleEffects(player, meta);
                break;

            case RUM:
                // 朗姆酒 - 防火效果（如果完全酿造）
                applyRumEffects(player, meta);
                break;
        }
    }

    /**
     * 应用啤酒效果（基于IC2原版）
     */
    private void applyBeerEffects(Player player, int meta) {
        // 从元数据解析属性
        int solidRatio = getSolidRatioOfBeerValue(meta);
        int hopsRatio = getHopsRatioOfBeerValue(meta);
        int timeRatio = getTimeRatioOfBeerValue(meta);

        // 如果是Black Stuff，应用随机负面效果
        if (timeRatio >= 5) {
            applyBlackStuffEffects(player);
            return;
        }

        // 基础效果
        int duration = BASE_DURATION[Math.min(solidRatio, BASE_DURATION.length - 1)];
        float intensity = BASE_INTENSITY[Math.min(timeRatio, BASE_INTENSITY.length - 1)];

        // 饱和度恢复（IC2原版逻辑）
        float saturation = 6 - hopsRatio;
        float food = solidRatio * 0.15F;
        player.getFoodData().eat((int) Math.max(0, saturation), food);

        // 计算最大效果等级
        int maxLevel = (int) (intensity * hopsRatio * 0.5F);

 // 应用慢效果（基础值）
        amplifyEffect(player, MobEffects.MOVEMENT_SLOWDOWN, maxLevel, intensity, duration);

 // 如果已有慢效果，则叠加更多效果
        if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            MobEffectInstance existingSlow = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            int slowLevel = existingSlow != null ? existingSlow.getAmplifier() : 0;

            if (slowLevel > -1) {
                amplifyEffect(player, MobEffects.MOVEMENT_SPEED, maxLevel, intensity, duration);
            }
            if (slowLevel > 0) {
                amplifyEffect(player, MobEffects.DIG_SLOWDOWN, maxLevel / 2, intensity, duration);
            }
            if (slowLevel > 1) {
                amplifyEffect(player, MobEffects.CONFUSION, maxLevel - 1, intensity, duration);
            }
            if (slowLevel > 2) {
                amplifyEffect(player, MobEffects.BLINDNESS, 0, intensity, duration);
            }
            if (slowLevel > 3) {
                // 极高概率即死
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, player.getRandom().nextInt(3)));
            }
        }
    }

    /**
     * 应用麦酒效果（比啤酒更强）
     */
    private void applyAleEffects(Player player, int meta) {
        // 麦酒效果更强，持续时间更长
        int solidRatio = getSolidRatioOfBeerValue(meta);
        int hopsRatio = getHopsRatioOfBeerValue(meta);

        int duration = (int) (BASE_DURATION[Math.min(solidRatio, BASE_DURATION.length - 1)] * 1.5F);
        float intensity = 1.5F;

        // 更多饱和度
        player.getFoodData().eat(8, solidRatio * 0.2F);

        // 更强的效果
        int maxLevel = (int) (intensity * hopsRatio * 0.5F);
        amplifyEffect(player, MobEffects.MOVEMENT_SLOWDOWN, maxLevel, intensity, duration);
        amplifyEffect(player, MobEffects.DAMAGE_BOOST, maxLevel / 2, intensity, duration / 2);

        // 如果已有慢效果
        if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            MobEffectInstance existingSlow = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            int slowLevel = existingSlow != null ? existingSlow.getAmplifier() : 0;
            if (slowLevel > 0) {
                amplifyEffect(player, MobEffects.MOVEMENT_SPEED, maxLevel, intensity, duration);
            }
            if (slowLevel > 1) {
                amplifyEffect(player, MobEffects.DIG_SPEED, maxLevel / 2, intensity, duration / 2);
            }
        }
    }

    /**
     * 应用朗姆酒效果
     */
    private void applyRumEffects(Player player, int meta) {
        int progress = getProgressOfRumValue(meta);

        if (progress < 100) {
            // 未完全发酵 - 随机负面效果（Black Stuff）
            applyBlackStuffEffects(player);
        } else {
            // 完全发酵 - 防火效果
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3600, 0));
            // 额外恢复
            player.getFoodData().eat(4, 0.5F);
        }
    }

    /**
     * Black Stuff效果（随机负面效果）
     */
    private void applyBlackStuffEffects(Player player) {
        int rand = player.getRandom().nextInt(6);
        switch (rand) {
            case 0:
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0));
                break;
            case 1:
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 2400, 0));
                break;
            case 2:
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 2400, 0));
                break;
            case 3:
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 2));
                break;
            case 4:
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 1200, 1));
                break;
            case 5:
                // 即死效果
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, player.getRandom().nextInt(4)));
                break;
        }
    }

    /**
     * 增强效果（IC2原版逻辑）
     */
    private void amplifyEffect(Player player, Holder<MobEffect> effectHolder, int maxLevel, float intensity, int duration) {
        MobEffectInstance existing = player.getEffect(effectHolder);
        if (existing == null) {
            player.addEffect(new MobEffectInstance(effectHolder, duration, 0));
        } else {
            int newDuration = existing.getDuration();
            int maxNewDuration = (int) (duration * (1.0F + intensity * 2.0F) - newDuration) / 2;
            if (maxNewDuration < 0) maxNewDuration = 0;
            if (maxNewDuration < duration) duration = maxNewDuration;
            newDuration += duration;

            int newAmplifier = existing.getAmplifier();
            if (newAmplifier < maxLevel) newAmplifier++;

            player.addEffect(new MobEffectInstance(effectHolder, newDuration, newAmplifier));
        }
    }

    // ============ 元数据解析方法 ============

    public static int getTypeOfValue(int value) {
        return skipGetOfValue(value, 0, 2);
    }

    public static int getAmountOfValue(int value) {
        if (getTypeOfValue(value) == 0) return 0;
        return skipGetOfValue(value, 2, 5) + 1;
    }

    public static int getSolidRatioOfBeerValue(int value) {
        return skipGetOfValue(value, 7, 3);
    }

    public static int getHopsRatioOfBeerValue(int value) {
        return skipGetOfValue(value, 10, 3);
    }

    public static int getTimeRatioOfBeerValue(int value) {
        return skipGetOfValue(value, 13, 3);
    }

    public static int getProgressOfRumValue(int value) {
        return skipGetOfValue(value, 7, 7);
    }

    private static int skipGetOfValue(int value, int bitshift, int take) {
        value >>= bitshift;
        take = (int) Math.pow(2.0, take) - 1;
        return value & take;
    }

    // ============ 饮品类型枚举 ============

    public enum BoozeType {
        BREW,       // 酿造液
        YOUNGSTER,  // 新手酒
        BEER,       // 啤酒
        ALE,        // 麦酒
        RUM         // 朗姆酒
    }
}