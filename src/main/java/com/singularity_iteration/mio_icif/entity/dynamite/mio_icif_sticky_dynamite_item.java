package com.singularity_iteration.mio_icif.entity.dynamite;

import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

@SuppressWarnings("null")
public class mio_icif_sticky_dynamite_item extends Item {

    public mio_icif_sticky_dynamite_item(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            mio_icif_sticky_dynamite_entity dynamite = new mio_icif_sticky_dynamite_entity(level, player.getX(), player.getY(0.5), player.getZ(), player, true);
            float xRot = player.getXRot();
            float yRot = player.getYRot();
            float rad = (float) (Math.PI / 180.0F);
            double dx = (double) (-Math.sin(yRot * rad) * Math.cos(xRot * rad));
            double dy = (double) (-Math.sin(xRot * rad));
            double dz = (double) (Math.cos(yRot * rad) * Math.cos(xRot * rad));
            double speed = 0.8;
            dynamite.setDeltaMovement(dx * speed, dy * speed + 0.1, dz * speed);
            level.addFreshEntity(dynamite);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static void registerDispenserBehavior() {
        DispenserBlock.registerBehavior(mio_icif_normal.STICKY_DYNAMITE.get(), new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                Level level = source.level();
                Direction direction = source.state().getValue(DispenserBlock.FACING);
                Position position = DispenserBlock.getDispensePosition(source);

                mio_icif_sticky_dynamite_entity dynamite = new mio_icif_sticky_dynamite_entity(level, position.x(), position.y(), position.z(), null, true);
                double speed = 0.8;
                dynamite.setDeltaMovement(
                    direction.getStepX() * speed,
                    direction.getStepY() * speed + 0.1,
                    direction.getStepZ() * speed
                );
                level.addFreshEntity(dynamite);

                stack.shrink(1);
                return stack;
            }

            @Override
            protected void playSound(BlockSource source) {
                source.level().levelEvent(1002, source.pos(), 0);
            }
        });
    }
}

