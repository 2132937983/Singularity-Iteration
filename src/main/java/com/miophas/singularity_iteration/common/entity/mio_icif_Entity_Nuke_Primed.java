package com.miophas.singularity_iteration.common.entity;

import com.miophas.singularity_iteration.common.registry.mio_icif_blocks;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 核弹点燃实体
 * 激活后变成实体模式，像TNT一样可以被推动和受重力影响
 */
@SuppressWarnings("null")
public class mio_icif_Entity_Nuke_Primed extends Entity implements TraceableEntity {
    private static final int CONTAINED_SLOT_COUNT = 9;
    private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(mio_icif_Entity_Nuke_Primed.class, EntityDataSerializers.INT);
    private static final int DEFAULT_FUSE_TIME = 600; // 30�?= 600 ticks
    public static final String TAG_FUSE = "fuse";
    public static final String TAG_ITEMS = "Items";

    @Nullable
    private LivingEntity owner;

    // 存储核弹内的物品栈?个TNT槽位 + 1个核原料槽位置
private List<ItemStack> containedItems = new ArrayList<>();

    public mio_icif_Entity_Nuke_Primed(EntityType<? extends mio_icif_Entity_Nuke_Primed> entityType, Level level) {
        super(entityType, level);
        this.blocksBuilding = true;
    }

    public mio_icif_Entity_Nuke_Primed(Level level, double x, double y, double z, @Nullable LivingEntity owner, List<ItemStack> items) {
        this(mio_icif_entities.NUKE_PRIMED.get(), level);
        this.setPos(x, y, z);
        double d0 = level.random.nextDouble() * (float) (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(d0) * 0.02, 0.2F, -Math.cos(d0) * 0.02);
        this.setFuse(DEFAULT_FUSE_TIME);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = owner;
        this.containedItems = new ArrayList<>(CONTAINED_SLOT_COUNT);
        int itemCount = items == null ? 0 : items.size();
        for (int i = 0; i < CONTAINED_SLOT_COUNT; i++) {
            ItemStack item = i < itemCount ? items.get(i) : ItemStack.EMPTY;
            this.containedItems.add(item == null ? ItemStack.EMPTY : item.copy());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FUSE_ID, DEFAULT_FUSE_TIME);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
        }

        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);

        if (fuse <= 0) {
            this.discard();
            if (!this.level().isClientSide) {
                this.explode();
            }
        } else {
            this.updateInWaterStateAndDoFluidPushing();
            this.spawnParticles();
        }
    }

    /**
     * 生成倒计时粒子效果
 */
    private void spawnParticles() {
        if (!this.level().isClientSide) return;

        int fuse = this.getFuse();
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        // 根据剩余时间调整闪烁频率
        int flashInterval;
        if (fuse > 400) {
            flashInterval = 20;
        } else if (fuse > 200) {
            flashInterval = 10;
        } else if (fuse > 100) {
            flashInterval = 5;
        } else {
            flashInterval = 2;
        }

        // 白色闪烁效果
        if (fuse % flashInterval == 0) {
            int particleCount = fuse <= 100 ? 32 : 16;
            for (int i = 0; i < particleCount; i++) {
                double offsetX = (this.level().random.nextDouble() - 0.5) * 1.5;
                double offsetY = (this.level().random.nextDouble() - 0.5) * 1.5;
                double offsetZ = (this.level().random.nextDouble() - 0.5) * 1.5;

                this.level().addParticle(
                    ParticleTypes.END_ROD,
                    x + offsetX, y + offsetY, z + offsetZ,
                    (this.level().random.nextDouble() - 0.5) * 0.02,
                    (this.level().random.nextDouble() - 0.5) * 0.02,
                    (this.level().random.nextDouble() - 0.5) * 0.02
                );
            }

            // 闪光效果
            if (fuse % (flashInterval * 2) == 0) {
                for (int i = 0; i < 8; i++) {
                    double offsetX = (this.level().random.nextDouble() - 0.5) * 1.2;
                    double offsetY = (this.level().random.nextDouble() - 0.5) * 1.2;
                    double offsetZ = (this.level().random.nextDouble() - 0.5) * 1.2;

                    this.level().addParticle(
                        ParticleTypes.FLASH,
                        x + offsetX, y + offsetY, z + offsetZ,
                        0, 0, 0
                    );
                }
            }
        }

        // 持续产生烟雾
        if (fuse % 5 == 0) {
            this.level().addParticle(
                ParticleTypes.SMOKE,
                x + (this.level().random.nextDouble() - 0.5) * 0.8,
                y + 0.3,
                z + (this.level().random.nextDouble() - 0.5) * 0.8,
                0, 0.05, 0
            );
        }

        // 红石粒子和烟�
    if (fuse % 2 == 0) {
            // 红石粒子
            int redstoneCount = fuse <= 100 ? 6 : 3;
            for (int i = 0; i < redstoneCount; i++) {
                double offsetX = (this.level().random.nextDouble() - 0.5) * 1.2;
                double offsetY = (this.level().random.nextDouble() - 0.5) * 1.2;
                double offsetZ = (this.level().random.nextDouble() - 0.5) * 1.2;

                this.level().addParticle(
                    new net.minecraft.core.particles.DustParticleOptions(
                        new org.joml.Vector3f(1.0F, 0.0F, 0.0F),
                        1.5F
                    ),
                    x + offsetX, y + offsetY, z + offsetZ,
                    0, 0.05, 0
                );
            }

            // 烟灰粒子
            int ashCount = fuse <= 100 ? 4 : 2;
            for (int i = 0; i < ashCount; i++) {
                double offsetX = (this.level().random.nextDouble() - 0.5) * 0.8;
                double offsetY = 0.5 + this.level().random.nextDouble() * 0.5;
                double offsetZ = (this.level().random.nextDouble() - 0.5) * 0.8;

                this.level().addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    x + offsetX, y + offsetY, z + offsetZ,
                    (this.level().random.nextDouble() - 0.5) * 0.01,
                    0.02 + this.level().random.nextDouble() * 0.02,
                    (this.level().random.nextDouble() - 0.5) * 0.01
                );
            }

            // 灵魂火焰
            if (fuse <= 200) {
                for (int i = 0; i < 2; i++) {
                    double offsetX = (this.level().random.nextDouble() - 0.5) * 1.0;
                    double offsetY = (this.level().random.nextDouble() - 0.5) * 1.0;
                    double offsetZ = (this.level().random.nextDouble() - 0.5) * 1.0;

                    this.level().addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        x + offsetX, y + offsetY, z + offsetZ,
                        (this.level().random.nextDouble() - 0.5) * 0.01,
                        0.02,
                        (this.level().random.nextDouble() - 0.5) * 0.01
                    );
                }
            }
        }

        // 最终?秒添加更多警告效果
    if (fuse <= 100 && fuse % 5 == 0) {
            for (int i = 0; i < 12; i++) {
                double offsetX = (this.level().random.nextDouble() - 0.5) * 1.8;
                double offsetY = (this.level().random.nextDouble() - 0.5) * 1.8;
                double offsetZ = (this.level().random.nextDouble() - 0.5) * 1.8;

                this.level().addParticle(
                    ParticleTypes.LAVA,
                    x + offsetX, y + offsetY, z + offsetZ,
                    (this.level().random.nextDouble() - 0.5) * 0.05,
                    0.05,
                    (this.level().random.nextDouble() - 0.5) * 0.05
                );
            }
        }
    }

    /**
     * 执行核爆�
 */
    protected void explode() {
        // 创建临时方块实体来计算爆炸威�
    com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_nuke tempNuke =
            new com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_nuke(
                this.blockPosition(),
                com.miophas.singularity_iteration.common.registry.mio_icif_blocks.NUKE.get().defaultBlockState()
            );

        // 将存储的物品放入临时方块实体
        for (int i = 0; i < containedItems.size() && i < 9; i++) {
            tempNuke.setItem(i, containedItems.get(i));
        }

        // 使用现有的爆炸逻辑
        com.miophas.singularity_iteration.common.blockentity.reactor.mio_icif_reactor_nuke.triggerExplosion(
            this.level(),
            this.blockPosition(),
            com.miophas.singularity_iteration.common.registry.mio_icif_blocks.NUKE.get().defaultBlockState(),
            tempNuke
        );
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putShort(TAG_FUSE, (short) this.getFuse());

        // 保存物品
        ListTag itemsList = new ListTag();
        for (int slot = 0; slot < Math.min(CONTAINED_SLOT_COUNT, containedItems.size()); slot++) {
            ItemStack stack = containedItems.get(slot);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = (CompoundTag) stack.save(this.level().registryAccess());
                itemTag.putByte("Slot", (byte) slot);
                itemsList.add(itemTag);
            }
        }
        compound.put(TAG_ITEMS, itemsList);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.setFuse(compound.getShort(TAG_FUSE));

        // 读取物品
        containedItems.clear();
        for (int i = 0; i < CONTAINED_SLOT_COUNT; i++) {
            containedItems.add(ItemStack.EMPTY);
        }
        if (compound.contains(TAG_ITEMS, 9)) { // 9 = ListTag
            ListTag itemsList = compound.getList(TAG_ITEMS, 10); // 10 = CompoundTag
            int limit = Math.min(CONTAINED_SLOT_COUNT, itemsList.size());
            for (int i = 0; i < limit; i++) {
                CompoundTag itemTag = itemsList.getCompound(i);
                // Older versions emitted a compact list without slot metadata;
                // retain that format while preserving sparse slots for new saves.
                int slot = itemTag.contains("Slot", Tag.TAG_BYTE)
                    ? itemTag.getByte("Slot") & 0xff : i;
                if (slot < 0 || slot >= CONTAINED_SLOT_COUNT) continue;
                containedItems.set(slot,
                    ItemStack.parse(this.level().registryAccess(), itemTag).orElse(ItemStack.EMPTY));
            }
        }
    }

    @Nullable
    public LivingEntity getOwner() {
        return this.owner;
    }

    public void setFuse(int fuse) {
        this.entityData.set(DATA_FUSE_ID, fuse);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE_ID);
    }

    public List<ItemStack> getContainedItems() {
        List<ItemStack> copy = new ArrayList<>(containedItems.size());
        for (ItemStack item : containedItems) {
            copy.add(item == null ? ItemStack.EMPTY : item.copy());
        }
        return copy;
    }
}

