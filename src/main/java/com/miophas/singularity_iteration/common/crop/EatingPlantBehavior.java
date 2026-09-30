// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.common.crop;

import com.miophas.singularity_iteration.core.api.crop.IPlanter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

final class EatingPlantBehavior {
    private EatingPlantBehavior() {}
    static void tick(IPlanter crop, BuiltinCrop plant) {
        if (crop.getGrowthStage() == 1) return;
        var world = crop.getPlanterWorld();
        var pos = crop.getPlanterPos();
        if (crop.getCustomData().getBoolean("eaten")) {
            Block.popResource(world, pos, new ItemStack(Items.ROTTEN_FLESH));
            crop.getCustomData().putBoolean("eaten", false);
        }
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
        var entities = world.getEntitiesOfClass(LivingEntity.class, new AABB(x - 1, pos.getY(), z - 1, x + 1, pos.getY() + 2, z + 1));
        for (int i = entities.size() - 1; i > 0; i--) java.util.Collections.swap(entities, i, world.random.nextInt(i + 1));
        for (LivingEntity entity : entities) {
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) continue;
            entity.setDeltaMovement((x - entity.getX()) * 0.5, Math.min(-0.05, entity.getDeltaMovement().y), (z - entity.getZ()) * 0.5);
            entity.hurtMarked = true;
            entity.hurt(new net.minecraft.world.damagesource.DamageSource(world.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(
                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "crop_eating")))), crop.getGrowthStage() * 2);
            boolean metal = false;
            if (entity instanceof Player) for (ItemStack armor : entity.getArmorSlots()) {
                if (armor.getItem() instanceof com.miophas.singularity_iteration.core.api.armor.IMetalArmor metalArmor
                        && metalArmor.isMetalArmor(armor, entity)) metal = true;
                if (armor.getItem() instanceof com.miophas.singularity_iteration.core.api.item.IMetalArmor metalArmor
                        && metalArmor.isMetalArmor(armor, (Player) entity)) metal = true;
                if (armor.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mio_icif", "metal_armor")))) metal = true;
            }
            if (!metal) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 64, 50));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 64));
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 64));
            }
            if (plant.canGrow(crop)) crop.setProgress(crop.getProgress() + 100);
            world.playSound(null, pos, SoundEvents.PLAYER_BURP, SoundSource.BLOCKS, 1, 0.9f + world.random.nextFloat() * 0.1f);
            crop.getCustomData().putBoolean("eaten", true);
        }
    }
}
