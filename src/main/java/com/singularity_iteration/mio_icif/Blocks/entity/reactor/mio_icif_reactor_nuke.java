package com.singularity_iteration.mio_icif.Blocks.entity.reactor;

import com.singularity_iteration.mio_icif.api.item.IReactorChamber;

import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.MachineItemHandler;
import com.singularity_iteration.mio_icif.Blocks.entity.slot.SlotLayout;
import com.singularity_iteration.mio_icif.client.NuclearExplosionAnimationHandler;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_nuclear_material;
import com.singularity_iteration.mio_icif.Menu.Generator.NukeMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("null")
public class mio_icif_reactor_nuke extends BlockEntity implements WorldlyContainer, MenuProvider, IReactorChamber {

    private static final SlotLayout LAYOUT = SlotLayout.builder()
        .extra(8)
        .nuclear(1)
        .build();

    public static final int SLOT_COUNT = 9;
    public static final int ICTNT_SLOT_START = 0;   // IC-TNT槽(EXTRA)
    public static final int ICTNT_SLOT_COUNT = 8;
    public static final int NUCLEAR_SLOT = 8;        // 核材料槽 (NUCLEAR)
    
    // 引爆倒计时
    public static final int COUNTDOWN_TICKS = 600; // 30秒= 600 ticks
    private int countdownTicks = 0;
    private boolean isArmed = false;

    protected MachineItemHandler itemHandler;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> countdownTicks;
                case 1 -> isArmed ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> countdownTicks = value;
                case 1 -> isArmed = value != 0;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public mio_icif_reactor_nuke(BlockPos pos, BlockState state) {
        super(com.singularity_iteration.mio_icif.Blocks.entity.mio_icif_block_entities.NUKE_ENTITY_TYPE.get(), pos, state);
        this.itemHandler = new MachineItemHandler(LAYOUT) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        this.itemHandler.setValidator((slot, stack, slotType) -> mio_icif_reactor_nuke.this.canPlaceItem(slot, stack));
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        int toRemove = Math.min(amount, stack.getCount());
        ItemStack result = stack.copyWithCount(toRemove);
        stack.shrink(toRemove);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
        if (stack.getCount() > stack.getMaxStackSize()) {
            stack.setCount(stack.getMaxStackSize());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (index >= ICTNT_SLOT_START && index < ICTNT_SLOT_START + ICTNT_SLOT_COUNT) {
            return isIC_TNT(stack);
        } else if (index == NUCLEAR_SLOT) {
            return stack.getItem() instanceof mio_icif_nuclear_material;
        }
        return false;
    }

    private boolean isIC_TNT(ItemStack stack) {
        return stack.is(mio_icif_blocks.IC_TNT.get().asItem());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", itemHandler.serializeNBT(registries));
        tag.putInt("CountdownTicks", countdownTicks);
        tag.putBoolean("IsArmed", isArmed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("Items"));
        } else if (tag.contains("Items")) {
            // 旧存档兼容：用ContainerHelper 格式读取
            net.minecraft.core.NonNullList<ItemStack> oldItems = net.minecraft.core.NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, oldItems, registries);
            for (int i = 0; i < oldItems.size(); i++) {
                itemHandler.setStackInSlot(i, oldItems.get(i));
            }
        }
        if (tag.contains("CountdownTicks")) {
            countdownTicks = tag.getInt("CountdownTicks");
        }
        if (tag.contains("IsArmed")) {
            isArmed = tag.getBoolean("IsArmed");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("CountdownTicks", countdownTicks);
        tag.putBoolean("IsArmed", isArmed);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        if (tag.contains("CountdownTicks")) {
            countdownTicks = tag.getInt("CountdownTicks");
        }
        if (tag.contains("IsArmed")) {
            isArmed = tag.getBoolean("IsArmed");
        }
    }

    public int getICTNTCount() {
        int count = 0;
        for (int i = ICTNT_SLOT_START; i < ICTNT_SLOT_START + ICTNT_SLOT_COUNT; i++) {
            count += itemHandler.getStackInSlot(i).getCount();
        }
        return count;
    }

    public float getTotalRadioactivity() {
        float total = 0.0F;
        ItemStack nuclearStack = itemHandler.getStackInSlot(NUCLEAR_SLOT);
        if (!nuclearStack.isEmpty() && nuclearStack.getItem() instanceof mio_icif_nuclear_material nuclear) {
            total += nuclear.getRadioactivity() * nuclearStack.getCount();
        }
        return total;
    }

    public float calculateExplosionPower() {
        int tntCount = getICTNTCount();
        float radioactivity = getTotalRadioactivity();

        if (tntCount == 0 && radioactivity == 0) {
            return 0.0F;
        }

        float basePower = tntCount * 16.0F;
        float radiationBonus = radioactivity * 100.0F;

        float totalPower = basePower + radiationBonus;
        float maxPower = 1000000.0F;

        return Math.min(totalPower, maxPower);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, mio_icif_reactor_nuke blockEntity) {
        if (level.isClientSide) {
            return;
        }

        if (blockEntity.isEmpty()) {
            return;
        }

        boolean redstoneSignal = level.hasNeighborSignal(pos);
        
        if (redstoneSignal && !blockEntity.isArmed) {
            blockEntity.isArmed = true;
            blockEntity.countdownTicks = COUNTDOWN_TICKS;
            blockEntity.setChanged();
            
            // 播放激活音效
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(),
                net.minecraft.sounds.SoundSource.BLOCKS,
                2.0F, 0.5F);
            
            // 转换为实体模式（TNT实体）
            spawnNukeEntity(level, pos, blockEntity);
            return;
        }
        
        // 倒计时模式：实体模式由实体自己处理
        if (blockEntity.isArmed) {
            blockEntity.countdownTicks--;
            
            // 每秒播放一次滴答声，最后几秒加快
            int remainingSeconds = blockEntity.countdownTicks / 20;
            if (blockEntity.countdownTicks % 20 == 0) {
                float pitch = remainingSeconds <= 5 ? 2.0F : 1.0F;
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.5F, pitch);
            }
            
            // 最后5秒播放警告音效
            if (blockEntity.countdownTicks <= 100 && blockEntity.countdownTicks % 10 == 0) {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(),
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    2.0F, 1.5F);
            }
            
            // 服务端发送警告粒子（核弹方块使用sendParticles）
            spawnWarningParticlesServer(level, pos, blockEntity.countdownTicks);
            
            if (blockEntity.countdownTicks <= 0) {
                triggerExplosion(level, pos, state, blockEntity);
            } else {
                blockEntity.setChanged();
            }
        }
    }
    
    /**
     * 将核弹转换为TNT实体
     */
    private static void spawnNukeEntity(Level level, BlockPos pos, mio_icif_reactor_nuke blockEntity) {
        if (level.isClientSide) return;
        
        // 保存所有物品的副本
        java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < blockEntity.itemHandler.getSlots(); i++) {
            items.add(blockEntity.itemHandler.getStackInSlot(i).copy());
        }
        
        // 移除方块实体不触发掉落
        level.removeBlockEntity(pos);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        
        // 创建核弹实体
        com.singularity_iteration.mio_icif.entity.mio_icif_Entity_Nuke_Primed nukeEntity = 
            new com.singularity_iteration.mio_icif.entity.mio_icif_Entity_Nuke_Primed(
                level,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                null, // owner
                items
            );
        
        level.addFreshEntity(nukeEntity);
    }
    
    /**
     * 客户端生成警告粒子
     */
    @SuppressWarnings("unused")
    private static void spawnWarningParticles(Level level, BlockPos pos, int countdownTicks) {
        // 倒计时闪烁频率：模仿ICTNT闪烁越来越快
        int flashInterval;
        if (countdownTicks > 400) {
            flashInterval = 20; // 前10秒：每秒闪1次
        } else if (countdownTicks > 200) {
            flashInterval = 10; // 中间10秒：每秒闪2次
        } else if (countdownTicks > 100) {
            flashInterval = 5;  // 倒数5秒：每秒闪4次
        } else {
            flashInterval = 2;  // 最后5秒：每秒闪10次
        }
        
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        // ICTNT风格闪烁效果
        boolean shouldFlash = countdownTicks % flashInterval == 0;
        boolean isBrightFlash = countdownTicks % (flashInterval * 2) == 0;
        
        if (shouldFlash) {
            // 发光粒子：模仿ICTNT闪烁
            int particleCount = countdownTicks <= 100 ? 32 : 16;
            for (int i = 0; i < particleCount; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 1.5;
                double offsetY = (level.random.nextDouble() - 0.5) * 1.5;
                double offsetZ = (level.random.nextDouble() - 0.5) * 1.5;
                
                // 使用发光/亮闪粒子模拟闪烁
                level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                    x + offsetX, y + offsetY, z + offsetZ,
                    (level.random.nextDouble() - 0.5) * 0.02,
                    (level.random.nextDouble() - 0.5) * 0.02,
                    (level.random.nextDouble() - 0.5) * 0.02
                );
            }
            
            // 添加强烈闪光效果
            if (isBrightFlash) {
                for (int i = 0; i < 8; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 1.2;
                    double offsetY = (level.random.nextDouble() - 0.5) * 1.2;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 1.2;
                    
                    level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.FLASH,
                        x + offsetX, y + offsetY, z + offsetZ,
                        0, 0, 0
                    );
                }
            }
        }
        
        // 持续产生烟雾粒子表示核弹正在倒计时
        if (countdownTicks % 5 == 0) {
            level.addParticle(
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                x + (level.random.nextDouble() - 0.5) * 0.8,
                y + 0.3,
                z + (level.random.nextDouble() - 0.5) * 0.8,
                0, 0.05, 0
            );
        }
        
        // 表面产生红色粒子表示核反应活跃
        // 每2tick 产生一次让效果更明显
        if (countdownTicks % 2 == 0) {
            // 红石粒子 - 表示核反应活跃，表面散发辐射
            int redstoneCount = countdownTicks <= 100 ? 6 : 3;
            net.minecraft.core.particles.DustParticleOptions redstoneOptions = 
                new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(1.0F, 0.0F, 0.0F), // 红色
                    1.5F // 大小
                );
            for (int i = 0; i < redstoneCount; i++) {
                // 表面范围1.2x1.2x1.2 随机位置，让粒子更容纳可见
                double offsetX = (level.random.nextDouble() - 0.5) * 1.2;
                double offsetY = (level.random.nextDouble() - 0.5) * 1.2;
                double offsetZ = (level.random.nextDouble() - 0.5) * 1.2;
                
                level.addParticle(
                    redstoneOptions,
                    x + offsetX, y + offsetY, z + offsetZ,
                    0, 0.05, 0
                );
            }
            
            // 大烟雾粒子 - 表示核反应产生的灰烬向顶部扩散
            int ashCount = countdownTicks <= 100 ? 4 : 2;
            for (int i = 0; i < ashCount; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 0.8;
                double offsetY = 0.5 + level.random.nextDouble() * 0.5;
                double offsetZ = (level.random.nextDouble() - 0.5) * 0.8;
                
                level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    x + offsetX, y + offsetY, z + offsetZ,
                    (level.random.nextDouble() - 0.5) * 0.01,
                    0.02 + level.random.nextDouble() * 0.02,
                    (level.random.nextDouble() - 0.5) * 0.01
                );
            }
            
            // 灵魂火焰粒子 - 表示核反应产生的辐射向周围扩散
            if (countdownTicks <= 200) {
                for (int i = 0; i < 2; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 1.0;
                    double offsetY = (level.random.nextDouble() - 0.5) * 1.0;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 1.0;
                    
                    level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                        x + offsetX, y + offsetY, z + offsetZ,
                        (level.random.nextDouble() - 0.5) * 0.01,
                        0.02,
                        (level.random.nextDouble() - 0.5) * 0.01
                    );
                }
            }
        }
        
        // 最后5秒添加更多警告粒子
        if (countdownTicks <= 100 && countdownTicks % 5 == 0) {
            for (int i = 0; i < 12; i++) {
                double offsetX = (level.random.nextDouble() - 0.5) * 1.8;
                double offsetY = (level.random.nextDouble() - 0.5) * 1.8;
                double offsetZ = (level.random.nextDouble() - 0.5) * 1.8;
                
                level.addParticle(
                    net.minecraft.core.particles.ParticleTypes.LAVA,
                    x + offsetX, y + offsetY, z + offsetZ,
                    (level.random.nextDouble() - 0.5) * 0.05,
                    0.05,
                    (level.random.nextDouble() - 0.5) * 0.05
                );
            }
        }
    }
    
    /**
     * 服务端发送警告粒子（核弹方块使用sendParticles）
     */
    private static void spawnWarningParticlesServer(Level level, BlockPos pos, int countdownTicks) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        
        // 倒计时闪烁频率
        int flashInterval;
        if (countdownTicks > 400) {
            flashInterval = 20;
        } else if (countdownTicks > 200) {
            flashInterval = 10;
        } else if (countdownTicks > 100) {
            flashInterval = 5;
        } else {
            flashInterval = 2;
        }
        
        // ICTNT风格闪烁效果
        if (countdownTicks % flashInterval == 0) {
            int particleCount = countdownTicks <= 100 ? 32 : 16;
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.END_ROD,
                x, y, z,
                particleCount,
                0.75, 0.75, 0.75,
                0.02
            );
            
            // 添加强烈闪光效果
            if (countdownTicks % (flashInterval * 2) == 0) {
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.FLASH,
                    x, y, z,
                    8,
                    0.6, 0.6, 0.6,
                    0.0
                );
            }
        }
        
        // 持续产生烟雾粒子
        if (countdownTicks % 5 == 0) {
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.SMOKE,
                x, y + 0.3, z,
                1,
                0.4, 0.1, 0.4,
                0.05
            );
        }
        
        // 附近区域产生红色粒子
        if (countdownTicks % 2 == 0) {
            // 红石粒子 - 表示核反应活跃
            int redstoneCount = countdownTicks <= 100 ? 6 : 3;
            serverLevel.sendParticles(
                new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(1.0F, 0.0F, 0.0F),
                    1.5F
                ),
                x, y, z,
                redstoneCount,
                0.6, 0.6, 0.6,
                0.05
            );
            
            // 大烟雾粒子
            int ashCount = countdownTicks <= 100 ? 4 : 2;
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                x, y + 0.5, z,
                ashCount,
                0.4, 0.5, 0.4,
                0.02
            );
            
            // 灵魂火焰粒子 - 表示辐射扩散
            if (countdownTicks <= 200) {
                serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    x, y, z,
                    2,
                    0.5, 0.5, 0.5,
                    0.01
                );
            }
        }
        
        // 最后5秒添加更多警告粒子
        if (countdownTicks <= 100 && countdownTicks % 5 == 0) {
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.LAVA,
                x, y, z,
                12,
                0.9, 0.9, 0.9,
                0.05
            );
        }
    }

    public static void triggerExplosion(Level level, BlockPos pos, BlockState state, mio_icif_reactor_nuke blockEntity) {
        if (level.isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        float explosionPower = blockEntity.calculateExplosionPower();
        if (explosionPower <= 0) {
            return;
        }

        // 检查是否启用核爆炸
        if (!com.singularity_iteration.mio_icif.Singularity_Iteration_Config.ENABLE_NUCLEAR_EXPLOSION.get()) {
            // 未启用时仅产生小规模机械破坏
            blockEntity.clearContent();
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.5F, Level.ExplosionInteraction.BLOCK);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            level.removeBlockEntity(pos);
            return;
        }

        // 使用线性公式计算半径让误差更小
        // 基础半径20，每25威力增加1格，比例更大时半径更大
        int explosionRadius = (int) Math.ceil(20.0 + explosionPower / 25.0);
        explosionRadius = Math.max(20, Math.min(explosionRadius, 2000));

        Vec3 center = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        // 发送蘑菇云动画到客户端显示核爆炸视觉效果
        com.singularity_iteration.mio_icif.network.mio_icif_Network.sendNuclearExplosionAnimation(
            serverLevel, center.x, center.y, center.z, explosionRadius);

        // 启动客户端动画
        NuclearExplosionAnimationHandler.startAnimation(center.x, center.y, center.z, explosionRadius);

        // 创建24小时辐射区域（实时24小时 = 20×72×60游戏tick * 72 = 86400秒现实时间）
        // 使用辐射区域系统确保创建持续24小时的辐射
        NukeRadiationZoneManager.createRadiationZone(serverLevel, pos, explosionRadius, explosionPower);

        List<Entity> entities = level.getEntities(null, new AABB(
            center.x - explosionRadius, center.y - explosionRadius, center.z - explosionRadius,
            center.x + explosionRadius, center.y + explosionRadius, center.z + explosionRadius
        ));

        // 先清空物品防止爆炸时掉落
        blockEntity.clearContent();
        
        NukeExplosionTask task = new NukeExplosionTask(serverLevel, center, explosionPower, explosionRadius, entities);
        NukeExplosionScheduler.startExplosion(serverLevel, pos, task);
        
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, 
            net.minecraft.sounds.SoundSource.BLOCKS, 
            4.0F, 
            0.5F);
        
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.removeBlockEntity(pos);
    }

    // ==================== MenuProvider 接口实现 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.mio_icif.nuke");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NukeMenu(containerId, playerInventory, this);
    }

    public void drops() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack itemStack = itemHandler.getStackInSlot(i);
            if (!itemStack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), itemStack);
            }
        }
    }
}