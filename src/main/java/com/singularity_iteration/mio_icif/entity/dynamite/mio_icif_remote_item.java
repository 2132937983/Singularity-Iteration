package com.singularity_iteration.mio_icif.entity.dynamite;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_remote_item extends Item {

    public mio_icif_remote_item(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            AABB searchBox = player.getBoundingBox().inflate(64.0);
            List<mio_icif_sticky_dynamite_entity> dynamites = level.getEntitiesOfClass(mio_icif_sticky_dynamite_entity.class, searchBox,
                d -> d.isRemote() && d.isAlive());

            for (mio_icif_sticky_dynamite_entity dynamite : dynamites) {
                dynamite.detonate();
            }

            if (!dynamites.isEmpty()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0F, 1.5F);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

