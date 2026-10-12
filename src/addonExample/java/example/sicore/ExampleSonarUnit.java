package example.sicore;

import com.miophas.singularity_iteration.core.api.armory.ArmoryEvent;
import com.miophas.singularity_iteration.core.api.suit.ISuitModuleItem;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleRegistry;
import com.miophas.singularity_iteration.core.api.suit.SuitModuleSpec;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Example of the 0.1.8.0 public suit unit API: a sonar unit for the quantum helmet. While it is
 * on, it pings every 40 ticks and makes the hostile mobs within 16 blocks glow for 3 seconds.
 * Uses only the published core JAR.
 */
public final class ExampleSonarUnit extends Item implements ISuitModuleItem {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "sonar");
    public static final SuitModuleSpec SPEC = new SuitModuleSpec(ID, Set.of(EquipmentSlot.HEAD), 3, 0x33CCAA, true,
        ResourceLocation.fromNamespaceAndPath(CoreExampleMod.ID, "sonar_unit"));

    public ExampleSonarUnit() { super(new Item.Properties().stacksTo(16)); }

    @Override
    public SuitModuleSpec suitModule(ItemStack stack) { return SPEC; }

    /** Called from the example mod constructor. */
    static void register() {
        SuitModuleRegistry.register(SPEC, (player, piece) -> {
            if (player.tickCount % 40 != 0) return;
            for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> m instanceof Enemy)) {
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            }
        });
        // Armory hook: event.setCanceled(true) stops a summon before energy or items move.
        // This example only stops an empty summon (it cannot happen; it shows the call).
        NeoForge.EVENT_BUS.addListener(ArmoryEvent.Summon.class, event -> {
            if (event.getSlots().isEmpty()) event.setCanceled(true);
        });
    }
}
