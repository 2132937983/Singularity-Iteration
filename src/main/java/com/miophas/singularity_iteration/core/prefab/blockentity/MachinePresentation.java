package com.miophas.singularity_iteration.core.prefab.blockentity;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

/** Optional host defaults; addon machines can override their own menu and work-sound hooks. */
public final class MachinePresentation {
    @FunctionalInterface
    public interface StorageMenuFactory {
        @Nullable AbstractContainerMenu create(int id, Inventory inventory, AbstractEnergyStorageBlockEntity storage);
    }

    private static volatile StorageMenuFactory storageMenu = (id, inventory, storage) -> null;
    private static volatile Supplier<SoundEvent> workSound = () -> null;
    private static volatile Supplier<SoundEvent> cableBreakSound = () -> null;
    private MachinePresentation() {}

    public static void configure(StorageMenuFactory menu, Supplier<SoundEvent> sound, Supplier<SoundEvent> cableBreak) {
        storageMenu = Objects.requireNonNull(menu);
        workSound = Objects.requireNonNull(sound);
        cableBreakSound = Objects.requireNonNull(cableBreak);
    }

    public static void configure(StorageMenuFactory menu, Supplier<SoundEvent> sound) {
        configure(menu, sound, () -> null);
    }

    public static @Nullable AbstractContainerMenu storageMenu(int id, Inventory inventory, AbstractEnergyStorageBlockEntity storage) {
        return storageMenu.create(id, inventory, storage);
    }

    public static @Nullable SoundEvent workSound() { return workSound.get(); }

    public static @Nullable SoundEvent cableBreakSound() { return cableBreakSound.get(); }
}