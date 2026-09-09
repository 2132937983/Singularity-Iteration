package com.singularity_iteration.mio_icif.Items.Build;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.api.item.IPainterItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

/**
 * 刷子（Painter�? IC2风格的建筑工�?
 * 
 * 功能量?
 * - 右键给以下方块上色（16种颜色）�?
 *   建筑泡沫、羊毛、混凝土粉末、混凝土、玻璃、玻璃板、旗帜、蜡烛、地毯、陶�?
 * - 陶瓦刷色后变成普通带色陶瓦（不上釉）
 * - 如果方块已经是刷子的颜色，会提示颜色相同
 */
@SuppressWarnings("null")
public class PainterItem extends Item implements IPainterItem {

    // 刷子颜色枚举 - 对应Minecraft�?6种染料颜�?
    @SuppressWarnings("null")
public enum PainterColor {
        WHITE("white", 0xFFFFFF),
        ORANGE("orange", 0xD87F33),
        MAGENTA("magenta", 0xB24CD8),
        LIGHT_BLUE("light_blue", 0x6689D3),
        YELLOW("yellow", 0xE5C33A),
        LIME("lime", 0x7FCC19),
        PINK("pink", 0xF27FA5),
        GRAY("gray", 0x4C4C4C),
        LIGHT_GRAY("light_gray", 0x999999),
        CYAN("cyan", 0x4C7F99),
        PURPLE("purple", 0x7F3FB2),
        BLUE("blue", 0x334CB2),
        BROWN("brown", 0x664C33),
        GREEN("green", 0x337F33),
        RED("red", 0x993333),
        BLACK("black", 0x191919);

        public final String name;
        public final int rgb;

        PainterColor(String name, int rgb) {
            this.name = name;
            this.rgb = rgb;
        }
    }

    private final PainterColor color;

    public PainterItem(PainterColor color, Properties properties) {
        super(properties.stacksTo(1));
        this.color = color;
    }

    public PainterColor getColor() {
        return color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        @SuppressWarnings("unused")
        ItemStack itemStack = context.getItemInHand();

        if (level.isClientSide()) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockState clickedState = level.getBlockState(pos);
        Block clickedBlock = clickedState.getBlock();

        // 尝试给原版方块上色（羊毛、地毯、陶瓦、混凝土、玻璃、旗帜、蜡烛等�?
        Block targetBlock = getVanillaColoredBlock(clickedBlock, color);
        if (targetBlock != null && targetBlock != clickedBlock) {
            BlockState newState = targetBlock.defaultBlockState();
            copyBlockStateProperties(clickedState, newState, level, pos);
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("message.mio_icif.painter.painted",
                        Component.translatable("item.mio_icif.build.item_painter_" + color.name))
                        .withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(false);
        } else if (targetBlock == clickedBlock) {
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("message.mio_icif.painter.same_color")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResult.CONSUME;
        }

        // 建筑泡沫 �?对应颜色的混凝土
        if (clickedBlock == mio_icif_blocks.CONSTRUCTION_FOAM.get()) {
            level.setBlockAndUpdate(pos, getConcreteForColor(color).defaultBlockState());
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player != null) {
                player.displayClientMessage(
                    Component.translatable("message.mio_icif.painter.painted",
                        Component.translatable("item.mio_icif.build.item_painter_" + color.name))
                        .withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(false);
        }

        return InteractionResult.PASS;
    }

    /**
     * 获取原版方块对应颜色的版�?
     * @return 对应颜色的方块，如果不可上色返回null，如果已经是目标颜色返回原方法?
     */
    private Block getVanillaColoredBlock(Block clickedBlock, PainterColor painterColor) {
        // 羊毛
        if (isWool(clickedBlock)) {
            Block target = getWoolForColor(painterColor);
            return clickedBlock == target ? clickedBlock : target;
        }
        // 混凝土粉�?
        if (isConcretePowder(clickedBlock)) {
            Block target = getConcretePowderForColor(painterColor);
            return clickedBlock == target ? clickedBlock : target;
        }
        // 混凝�?
        if (isConcrete(clickedBlock)) {
            Block target = getConcreteForColor(painterColor);
            return clickedBlock == target ? clickedBlock : target;
        }
        // 玻璃 �?彩色玻璃
        if (clickedBlock == Blocks.GLASS) {
            return getStainedGlassForColor(painterColor);
        }
        // 彩色玻璃 �?彩色玻璃
        if (isStainedGlass(clickedBlock)) {
            Block target = getStainedGlassForColor(painterColor);
            return clickedBlock == target ? clickedBlock : target;
        }
        // 玻璃�?�?彩色玻璃�?
        if (clickedBlock == Blocks.GLASS_PANE) {
            return getStainedGlassPaneForColor(painterColor);
        }
        // 彩色玻璃�?�?彩色玻璃�?
        if (isStainedGlassPane(clickedBlock)) {
            Block target = getStainedGlassPaneForColor(painterColor);
            return clickedBlock == target ? clickedBlock : target;
        }
        // 旗帜
        if (isBanner(clickedBlock)) {
            Block target = getBannerForColor(painterColor);
            return clickedBlock == target ? clickedBlock : target;
        }
        // 蜡烛
        if (isCandle(clickedBlock)) {
            Block target = getCandleForColor(painterColor);
            return clickedBlock == target ? clickedBlock : target;
        }
        // 地毯（不支持床，因为床是双方块结构，替换会破坏）
        if (isCarpet(clickedBlock)) {
            Block target = getCarpetForColor(painterColor);
            return clickedBlock == target ? clickedBlock : target;
        }
        // 陶瓦 �?普通带色陶瓦（不上釉！�?
        if (clickedBlock == Blocks.TERRACOTTA) {
            return getTerracottaForColor(painterColor);
        }
        // 带色陶瓦 �?带色陶瓦（不上釉�?
        if (isColoredTerracotta(clickedBlock)) {
            Block target = getTerracottaForColor(painterColor);
            return clickedBlock == target ? clickedBlock : target;
        }

        return null;
    }

    /**
     * 尝试复制方块状态属性（如朝向等�?
     */
    private void copyBlockStateProperties(BlockState oldState, BlockState newState, Level level, BlockPos pos) {
        BlockState finalState = newState;
        // 复制朝向属性（facing�?
        if (oldState.hasProperty(BlockStateProperties.FACING) && finalState.hasProperty(BlockStateProperties.FACING)) {
            finalState = finalState.setValue(BlockStateProperties.FACING, oldState.getValue(BlockStateProperties.FACING));
        }
        // 复制水平朝向属性?
        if (oldState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && finalState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            finalState = finalState.setValue(BlockStateProperties.HORIZONTAL_FACING, oldState.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        // 复制蜡烛点亮状态?
        if (oldState.hasProperty(BlockStateProperties.LIT) && finalState.hasProperty(BlockStateProperties.LIT)) {
            finalState = finalState.setValue(BlockStateProperties.LIT, oldState.getValue(BlockStateProperties.LIT));
        }
        // 复制水位属性?
        if (oldState.hasProperty(BlockStateProperties.WATERLOGGED) && finalState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            finalState = finalState.setValue(BlockStateProperties.WATERLOGGED, oldState.getValue(BlockStateProperties.WATERLOGGED));
        }
        level.setBlockAndUpdate(pos, finalState);
    }

    // ===== 检查方法?=====

    private boolean isWool(Block block) {
        return block == Blocks.WHITE_WOOL || block == Blocks.ORANGE_WOOL
            || block == Blocks.MAGENTA_WOOL || block == Blocks.LIGHT_BLUE_WOOL
            || block == Blocks.YELLOW_WOOL || block == Blocks.LIME_WOOL
            || block == Blocks.PINK_WOOL || block == Blocks.GRAY_WOOL
            || block == Blocks.LIGHT_GRAY_WOOL || block == Blocks.CYAN_WOOL
            || block == Blocks.PURPLE_WOOL || block == Blocks.BLUE_WOOL
            || block == Blocks.BROWN_WOOL || block == Blocks.GREEN_WOOL
            || block == Blocks.RED_WOOL || block == Blocks.BLACK_WOOL;
    }

    private boolean isConcretePowder(Block block) {
        return block == Blocks.WHITE_CONCRETE_POWDER || block == Blocks.ORANGE_CONCRETE_POWDER
            || block == Blocks.MAGENTA_CONCRETE_POWDER || block == Blocks.LIGHT_BLUE_CONCRETE_POWDER
            || block == Blocks.YELLOW_CONCRETE_POWDER || block == Blocks.LIME_CONCRETE_POWDER
            || block == Blocks.PINK_CONCRETE_POWDER || block == Blocks.GRAY_CONCRETE_POWDER
            || block == Blocks.LIGHT_GRAY_CONCRETE_POWDER || block == Blocks.CYAN_CONCRETE_POWDER
            || block == Blocks.PURPLE_CONCRETE_POWDER || block == Blocks.BLUE_CONCRETE_POWDER
            || block == Blocks.BROWN_CONCRETE_POWDER || block == Blocks.GREEN_CONCRETE_POWDER
            || block == Blocks.RED_CONCRETE_POWDER || block == Blocks.BLACK_CONCRETE_POWDER;
    }

    private boolean isConcrete(Block block) {
        return block == Blocks.WHITE_CONCRETE || block == Blocks.ORANGE_CONCRETE
            || block == Blocks.MAGENTA_CONCRETE || block == Blocks.LIGHT_BLUE_CONCRETE
            || block == Blocks.YELLOW_CONCRETE || block == Blocks.LIME_CONCRETE
            || block == Blocks.PINK_CONCRETE || block == Blocks.GRAY_CONCRETE
            || block == Blocks.LIGHT_GRAY_CONCRETE || block == Blocks.CYAN_CONCRETE
            || block == Blocks.PURPLE_CONCRETE || block == Blocks.BLUE_CONCRETE
            || block == Blocks.BROWN_CONCRETE || block == Blocks.GREEN_CONCRETE
            || block == Blocks.RED_CONCRETE || block == Blocks.BLACK_CONCRETE;
    }

    private boolean isStainedGlass(Block block) {
        return block == Blocks.WHITE_STAINED_GLASS || block == Blocks.ORANGE_STAINED_GLASS
            || block == Blocks.MAGENTA_STAINED_GLASS || block == Blocks.LIGHT_BLUE_STAINED_GLASS
            || block == Blocks.YELLOW_STAINED_GLASS || block == Blocks.LIME_STAINED_GLASS
            || block == Blocks.PINK_STAINED_GLASS || block == Blocks.GRAY_STAINED_GLASS
            || block == Blocks.LIGHT_GRAY_STAINED_GLASS || block == Blocks.CYAN_STAINED_GLASS
            || block == Blocks.PURPLE_STAINED_GLASS || block == Blocks.BLUE_STAINED_GLASS
            || block == Blocks.BROWN_STAINED_GLASS || block == Blocks.GREEN_STAINED_GLASS
            || block == Blocks.RED_STAINED_GLASS || block == Blocks.BLACK_STAINED_GLASS;
    }

    private boolean isStainedGlassPane(Block block) {
        return block == Blocks.WHITE_STAINED_GLASS_PANE || block == Blocks.ORANGE_STAINED_GLASS_PANE
            || block == Blocks.MAGENTA_STAINED_GLASS_PANE || block == Blocks.LIGHT_BLUE_STAINED_GLASS_PANE
            || block == Blocks.YELLOW_STAINED_GLASS_PANE || block == Blocks.LIME_STAINED_GLASS_PANE
            || block == Blocks.PINK_STAINED_GLASS_PANE || block == Blocks.GRAY_STAINED_GLASS_PANE
            || block == Blocks.LIGHT_GRAY_STAINED_GLASS_PANE || block == Blocks.CYAN_STAINED_GLASS_PANE
            || block == Blocks.PURPLE_STAINED_GLASS_PANE || block == Blocks.BLUE_STAINED_GLASS_PANE
            || block == Blocks.BROWN_STAINED_GLASS_PANE || block == Blocks.GREEN_STAINED_GLASS_PANE
            || block == Blocks.RED_STAINED_GLASS_PANE || block == Blocks.BLACK_STAINED_GLASS_PANE;
    }

    private boolean isBanner(Block block) {
        return block == Blocks.WHITE_BANNER || block == Blocks.ORANGE_BANNER
            || block == Blocks.MAGENTA_BANNER || block == Blocks.LIGHT_BLUE_BANNER
            || block == Blocks.YELLOW_BANNER || block == Blocks.LIME_BANNER
            || block == Blocks.PINK_BANNER || block == Blocks.GRAY_BANNER
            || block == Blocks.LIGHT_GRAY_BANNER || block == Blocks.CYAN_BANNER
            || block == Blocks.PURPLE_BANNER || block == Blocks.BLUE_BANNER
            || block == Blocks.BROWN_BANNER || block == Blocks.GREEN_BANNER
            || block == Blocks.RED_BANNER || block == Blocks.BLACK_BANNER;
    }

    private boolean isCandle(Block block) {
        return block == Blocks.WHITE_CANDLE || block == Blocks.ORANGE_CANDLE
            || block == Blocks.MAGENTA_CANDLE || block == Blocks.LIGHT_BLUE_CANDLE
            || block == Blocks.YELLOW_CANDLE || block == Blocks.LIME_CANDLE
            || block == Blocks.PINK_CANDLE || block == Blocks.GRAY_CANDLE
            || block == Blocks.LIGHT_GRAY_CANDLE || block == Blocks.CYAN_CANDLE
            || block == Blocks.PURPLE_CANDLE || block == Blocks.BLUE_CANDLE
            || block == Blocks.BROWN_CANDLE || block == Blocks.GREEN_CANDLE
            || block == Blocks.RED_CANDLE || block == Blocks.BLACK_CANDLE;
    }

    private boolean isCarpet(Block block) {
        return block == Blocks.WHITE_CARPET || block == Blocks.ORANGE_CARPET
            || block == Blocks.MAGENTA_CARPET || block == Blocks.LIGHT_BLUE_CARPET
            || block == Blocks.YELLOW_CARPET || block == Blocks.LIME_CARPET
            || block == Blocks.PINK_CARPET || block == Blocks.GRAY_CARPET
            || block == Blocks.LIGHT_GRAY_CARPET || block == Blocks.CYAN_CARPET
            || block == Blocks.PURPLE_CARPET || block == Blocks.BLUE_CARPET
            || block == Blocks.BROWN_CARPET || block == Blocks.GREEN_CARPET
            || block == Blocks.RED_CARPET || block == Blocks.BLACK_CARPET;
    }

    private boolean isColoredTerracotta(Block block) {
        return block == Blocks.WHITE_TERRACOTTA || block == Blocks.ORANGE_TERRACOTTA
            || block == Blocks.MAGENTA_TERRACOTTA || block == Blocks.LIGHT_BLUE_TERRACOTTA
            || block == Blocks.YELLOW_TERRACOTTA || block == Blocks.LIME_TERRACOTTA
            || block == Blocks.PINK_TERRACOTTA || block == Blocks.GRAY_TERRACOTTA
            || block == Blocks.LIGHT_GRAY_TERRACOTTA || block == Blocks.CYAN_TERRACOTTA
            || block == Blocks.PURPLE_TERRACOTTA || block == Blocks.BLUE_TERRACOTTA
            || block == Blocks.BROWN_TERRACOTTA || block == Blocks.GREEN_TERRACOTTA
            || block == Blocks.RED_TERRACOTTA || block == Blocks.BLACK_TERRACOTTA;
    }

    // ===== 获取对应颜色方块的方法?=====

    private Block getWoolForColor(PainterColor c) {
        return switch (c) {
            case WHITE -> Blocks.WHITE_WOOL; case ORANGE -> Blocks.ORANGE_WOOL;
            case MAGENTA -> Blocks.MAGENTA_WOOL; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL;
            case YELLOW -> Blocks.YELLOW_WOOL; case LIME -> Blocks.LIME_WOOL;
            case PINK -> Blocks.PINK_WOOL; case GRAY -> Blocks.GRAY_WOOL;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL; case CYAN -> Blocks.CYAN_WOOL;
            case PURPLE -> Blocks.PURPLE_WOOL; case BLUE -> Blocks.BLUE_WOOL;
            case BROWN -> Blocks.BROWN_WOOL; case GREEN -> Blocks.GREEN_WOOL;
            case RED -> Blocks.RED_WOOL; case BLACK -> Blocks.BLACK_WOOL;
        };
    }

    private Block getConcretePowderForColor(PainterColor c) {
        return switch (c) {
            case WHITE -> Blocks.WHITE_CONCRETE_POWDER; case ORANGE -> Blocks.ORANGE_CONCRETE_POWDER;
            case MAGENTA -> Blocks.MAGENTA_CONCRETE_POWDER; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE_POWDER;
            case YELLOW -> Blocks.YELLOW_CONCRETE_POWDER; case LIME -> Blocks.LIME_CONCRETE_POWDER;
            case PINK -> Blocks.PINK_CONCRETE_POWDER; case GRAY -> Blocks.GRAY_CONCRETE_POWDER;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE_POWDER; case CYAN -> Blocks.CYAN_CONCRETE_POWDER;
            case PURPLE -> Blocks.PURPLE_CONCRETE_POWDER; case BLUE -> Blocks.BLUE_CONCRETE_POWDER;
            case BROWN -> Blocks.BROWN_CONCRETE_POWDER; case GREEN -> Blocks.GREEN_CONCRETE_POWDER;
            case RED -> Blocks.RED_CONCRETE_POWDER; case BLACK -> Blocks.BLACK_CONCRETE_POWDER;
        };
    }

    private Block getConcreteForColor(PainterColor c) {
        return switch (c) {
            case WHITE -> Blocks.WHITE_CONCRETE; case ORANGE -> Blocks.ORANGE_CONCRETE;
            case MAGENTA -> Blocks.MAGENTA_CONCRETE; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE;
            case YELLOW -> Blocks.YELLOW_CONCRETE; case LIME -> Blocks.LIME_CONCRETE;
            case PINK -> Blocks.PINK_CONCRETE; case GRAY -> Blocks.GRAY_CONCRETE;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE; case CYAN -> Blocks.CYAN_CONCRETE;
            case PURPLE -> Blocks.PURPLE_CONCRETE; case BLUE -> Blocks.BLUE_CONCRETE;
            case BROWN -> Blocks.BROWN_CONCRETE; case GREEN -> Blocks.GREEN_CONCRETE;
            case RED -> Blocks.RED_CONCRETE; case BLACK -> Blocks.BLACK_CONCRETE;
        };
    }

    private Block getStainedGlassForColor(PainterColor c) {
        return switch (c) {
            case WHITE -> Blocks.WHITE_STAINED_GLASS; case ORANGE -> Blocks.ORANGE_STAINED_GLASS;
            case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS;
            case YELLOW -> Blocks.YELLOW_STAINED_GLASS; case LIME -> Blocks.LIME_STAINED_GLASS;
            case PINK -> Blocks.PINK_STAINED_GLASS; case GRAY -> Blocks.GRAY_STAINED_GLASS;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS; case CYAN -> Blocks.CYAN_STAINED_GLASS;
            case PURPLE -> Blocks.PURPLE_STAINED_GLASS; case BLUE -> Blocks.BLUE_STAINED_GLASS;
            case BROWN -> Blocks.BROWN_STAINED_GLASS; case GREEN -> Blocks.GREEN_STAINED_GLASS;
            case RED -> Blocks.RED_STAINED_GLASS; case BLACK -> Blocks.BLACK_STAINED_GLASS;
        };
    }

    private Block getStainedGlassPaneForColor(PainterColor c) {
        return switch (c) {
            case WHITE -> Blocks.WHITE_STAINED_GLASS_PANE; case ORANGE -> Blocks.ORANGE_STAINED_GLASS_PANE;
            case MAGENTA -> Blocks.MAGENTA_STAINED_GLASS_PANE; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_STAINED_GLASS_PANE;
            case YELLOW -> Blocks.YELLOW_STAINED_GLASS_PANE; case LIME -> Blocks.LIME_STAINED_GLASS_PANE;
            case PINK -> Blocks.PINK_STAINED_GLASS_PANE; case GRAY -> Blocks.GRAY_STAINED_GLASS_PANE;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_STAINED_GLASS_PANE; case CYAN -> Blocks.CYAN_STAINED_GLASS_PANE;
            case PURPLE -> Blocks.PURPLE_STAINED_GLASS_PANE; case BLUE -> Blocks.BLUE_STAINED_GLASS_PANE;
            case BROWN -> Blocks.BROWN_STAINED_GLASS_PANE; case GREEN -> Blocks.GREEN_STAINED_GLASS_PANE;
            case RED -> Blocks.RED_STAINED_GLASS_PANE; case BLACK -> Blocks.BLACK_STAINED_GLASS_PANE;
        };
    }

    private Block getBannerForColor(PainterColor c) {
        return switch (c) {
            case WHITE -> Blocks.WHITE_BANNER; case ORANGE -> Blocks.ORANGE_BANNER;
            case MAGENTA -> Blocks.MAGENTA_BANNER; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_BANNER;
            case YELLOW -> Blocks.YELLOW_BANNER; case LIME -> Blocks.LIME_BANNER;
            case PINK -> Blocks.PINK_BANNER; case GRAY -> Blocks.GRAY_BANNER;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_BANNER; case CYAN -> Blocks.CYAN_BANNER;
            case PURPLE -> Blocks.PURPLE_BANNER; case BLUE -> Blocks.BLUE_BANNER;
            case BROWN -> Blocks.BROWN_BANNER; case GREEN -> Blocks.GREEN_BANNER;
            case RED -> Blocks.RED_BANNER; case BLACK -> Blocks.BLACK_BANNER;
        };
    }

    private Block getCandleForColor(PainterColor c) {
        return switch (c) {
            case WHITE -> Blocks.WHITE_CANDLE; case ORANGE -> Blocks.ORANGE_CANDLE;
            case MAGENTA -> Blocks.MAGENTA_CANDLE; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CANDLE;
            case YELLOW -> Blocks.YELLOW_CANDLE; case LIME -> Blocks.LIME_CANDLE;
            case PINK -> Blocks.PINK_CANDLE; case GRAY -> Blocks.GRAY_CANDLE;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CANDLE; case CYAN -> Blocks.CYAN_CANDLE;
            case PURPLE -> Blocks.PURPLE_CANDLE; case BLUE -> Blocks.BLUE_CANDLE;
            case BROWN -> Blocks.BROWN_CANDLE; case GREEN -> Blocks.GREEN_CANDLE;
            case RED -> Blocks.RED_CANDLE; case BLACK -> Blocks.BLACK_CANDLE;
        };
    }

    private Block getCarpetForColor(PainterColor c) {
        return switch (c) {
            case WHITE -> Blocks.WHITE_CARPET; case ORANGE -> Blocks.ORANGE_CARPET;
            case MAGENTA -> Blocks.MAGENTA_CARPET; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CARPET;
            case YELLOW -> Blocks.YELLOW_CARPET; case LIME -> Blocks.LIME_CARPET;
            case PINK -> Blocks.PINK_CARPET; case GRAY -> Blocks.GRAY_CARPET;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CARPET; case CYAN -> Blocks.CYAN_CARPET;
            case PURPLE -> Blocks.PURPLE_CARPET; case BLUE -> Blocks.BLUE_CARPET;
            case BROWN -> Blocks.BROWN_CARPET; case GREEN -> Blocks.GREEN_CARPET;
            case RED -> Blocks.RED_CARPET; case BLACK -> Blocks.BLACK_CARPET;
        };
    }

    /**
     * 陶瓦刷色 �?普通带色陶瓦（不上釉！�?
     */
    private Block getTerracottaForColor(PainterColor c) {
        return switch (c) {
            case WHITE -> Blocks.WHITE_TERRACOTTA; case ORANGE -> Blocks.ORANGE_TERRACOTTA;
            case MAGENTA -> Blocks.MAGENTA_TERRACOTTA; case LIGHT_BLUE -> Blocks.LIGHT_BLUE_TERRACOTTA;
            case YELLOW -> Blocks.YELLOW_TERRACOTTA; case LIME -> Blocks.LIME_TERRACOTTA;
            case PINK -> Blocks.PINK_TERRACOTTA; case GRAY -> Blocks.GRAY_TERRACOTTA;
            case LIGHT_GRAY -> Blocks.LIGHT_GRAY_TERRACOTTA; case CYAN -> Blocks.CYAN_TERRACOTTA;
            case PURPLE -> Blocks.PURPLE_TERRACOTTA; case BLUE -> Blocks.BLUE_TERRACOTTA;
            case BROWN -> Blocks.BROWN_TERRACOTTA; case GREEN -> Blocks.GREEN_TERRACOTTA;
            case RED -> Blocks.RED_TERRACOTTA; case BLACK -> Blocks.BLACK_TERRACOTTA;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mio_icif.painter.color",
            Component.translatable("item.mio_icif.build.item_painter_" + color.name))
            .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.mio_icif.painter.usage")
            .withStyle(ChatFormatting.GRAY));
    }
}

