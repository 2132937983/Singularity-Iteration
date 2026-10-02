package vazkii.botania.api.mana;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Test-only stand-in mirroring Botania's public ManaReceiver API (compat GameTests only). */
public interface ManaReceiver {
    Level getManaReceiverLevel();
    BlockPos getManaReceiverPos();
    int getCurrentMana();
    boolean isFull();
    void receiveMana(int mana);
    boolean canReceiveManaFromBursts();
}
