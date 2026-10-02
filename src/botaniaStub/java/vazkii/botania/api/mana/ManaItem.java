package vazkii.botania.api.mana;

/** Test-only stand-in mirroring Botania's public ManaItem API (compat GameTests only). */
public interface ManaItem {
    int getMana();
    int getMaxMana();
    void addMana(int mana);
    default boolean canReceiveManaFromPool(net.minecraft.world.level.block.entity.BlockEntity pool) { return true; }
    default boolean canReceiveManaFromItem(net.minecraft.world.item.ItemStack otherStack) { return true; }
    default boolean canExportManaToPool(net.minecraft.world.level.block.entity.BlockEntity pool) { return true; }
    default boolean canExportManaToItem(net.minecraft.world.item.ItemStack otherStack) { return true; }
    default boolean isNoExport() { return false; }
}
