package com.singularity_iteration.mio_icif.entity.boat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class mio_icif_boat_item extends Item {

    private final Supplier<EntityType<? extends mio_icif_boat>> boatEntityType;

    public mio_icif_boat_item(Properties properties, Supplier<EntityType<? extends mio_icif_boat>> boatEntityType) {
        super(properties);
        this.boatEntityType = boatEntityType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hitResult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(stack);
        }
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            BlockPos hitPos = hitResult.getBlockPos();
            Direction hitDir = hitResult.getDirection();
            BlockPos spawnPos = hitDir != null ? hitPos.relative(hitDir) : hitPos;

            mio_icif_boat boat = boatEntityType.get().create(serverLevel, null, spawnPos, MobSpawnType.SPAWN_EGG, true, false);
            if (boat == null) {
                return InteractionResultHolder.fail(stack);
            }

            boat.setYRot(player.getYRot());
            if (!level.noCollision(boat, boat.getBoundingBox().inflate(-0.1))) {
                return InteractionResultHolder.fail(stack);
            }

            if (!level.addFreshEntity(boat)) {
                return InteractionResultHolder.fail(stack);
            }

            level.gameEvent(player, GameEvent.ENTITY_PLACE, hitPos);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            player.awardStat(Stats.ITEM_USED.get(this));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}

