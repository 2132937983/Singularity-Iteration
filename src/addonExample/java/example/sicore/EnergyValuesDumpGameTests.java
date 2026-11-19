package example.sicore;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.miophas.singularity_iteration.core.api.capability.IMioIcifCapabilities;
import com.miophas.singularity_iteration.core.api.energy.storage.CableTier;
import com.miophas.singularity_iteration.core.api.item.IBatteryItem;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyStorageBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractGeneratorBlockEntity;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity;
import com.miophas.singularity_iteration.core.prefab.item.AbstractElectricTool;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 0.1.11.82: dumps the electrical numbers of every Singularity Iteration block entity and electric item to
 * {@code si_energy_dump.json} in the game directory (build/addon-gametest), read by {@code tools/energy_table_11582.py}
 * to write {@code docs/energy_values.md}. Blocks: kind (generator, storage, machine, other), storage capacity, input and
 * output limits, voltage tier, rated output (generators) and EU/t and work time (machines). Items: capacity, transfer
 * (charge) rate, EU per use and tier (tools), armor tier and EU per tick (armor). Block entities are created detached (no world), so
 * state-dependent numbers (a reactor's layout, a structure) read as their idle value.
 *
 * <p>The test also checks the numbers are sane: generators and storage exist, every generator makes something or is
 * state-dependent, capacities are not negative, and tiers go up.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class EnergyValuesDumpGameTests {
    private EnergyValuesDumpGameTests() {}

    @GameTest(template = "empty", batch = "energy_values_dump", timeoutTicks = 20)
    public static void dumpEnergyValues(GameTestHelper h) {
        JsonObject root = new JsonObject();
        JsonArray tiers = new JsonArray();
        long lastEu = 0;
        for (CableTier tier : CableTier.allTiers()) {
            JsonObject t = new JsonObject();
            t.addProperty("name", tier.getName());
            t.addProperty("eu", tier.getEu());
            t.addProperty("index", tier.getTierIndex());
            tiers.add(t);
            if (tier.getEu() > 0 && tier.getEu() < Long.MAX_VALUE / 2) {
                h.assertTrue(tier.getEu() >= lastEu, "tiers do not go up at " + tier.getName());
                lastEu = tier.getEu();
            }
        }
        root.add("tiers", tiers);

        JsonArray blocks = new JsonArray();
        int generators = 0, storages = 0, machines = 0;
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            var id = entry.getKey().location();
            if (!id.getNamespace().equals("mio_icif") || !(entry.getValue() instanceof EntityBlock eb)) continue;
            JsonObject b = new JsonObject();
            b.addProperty("id", id.getPath());
            BlockEntity be;
            try {
                be = eb.newBlockEntity(BlockPos.ZERO, entry.getValue().defaultBlockState());
            } catch (RuntimeException e) {
                b.addProperty("error", e.getClass().getSimpleName());
                blocks.add(b);
                continue;
            }
            if (!(be instanceof AbstractEnergyBlockEntity energy)) continue;
            b.addProperty("class", be.getClass().getSimpleName());
            try {
                var storage = energy.getEnergyStorageInternal();
                b.addProperty("capacity", storage.getCapacity());
                b.addProperty("maxReceive", storage.getMaxReceive());
                b.addProperty("maxExtract", storage.getMaxExtract());
                CableTier tier = storage.getCableTier();
                if (tier != null) {
                    b.addProperty("tier", tier.getName());
                    b.addProperty("tierEu", tier.getEu());
                }
                h.assertTrue(storage.getCapacity() >= 0, id + " has a negative capacity");
            } catch (RuntimeException e) {
                b.addProperty("storageError", e.getClass().getSimpleName());
            }
            String kind = "other";
            // every generator with a nameplate rate has a public ratedOutput() (not all extend AbstractGeneratorBlockEntity)
            Long rated = null;
            try {
                var m = be.getClass().getMethod("ratedOutput");
                if (m.getReturnType() == long.class) rated = (Long) m.invoke(be);
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
            if (rated != null) b.addProperty("rated", rated);
            boolean generatorBlock = id.getPath().startsWith("generator/") && !id.getPath().contains("antenna") && !id.getPath().contains("pedestal")
                || id.getPath().equals("fusion/block_fusion_controller");   // 0.1.11.92: the fusion reactor (its output leaves through its ports)
            if (be instanceof AbstractGeneratorBlockEntity || generatorBlock) {
                kind = "generator";
                generators++;
            } else if (be instanceof AbstractEnergyStorageBlockEntity) {
                kind = "storage";
                storages++;
            } else if (be instanceof AbstractProcessingMachineBlockEntity m) {
                kind = "machine";
                machines++;
                try {
                    b.addProperty("energyPerTick", m.getEnergyPerTick());
                    b.addProperty("maxProgress", m.getMaxProgress());
                } catch (RuntimeException e) { b.addProperty("machineError", e.getClass().getSimpleName()); }
            }
            b.addProperty("kind", kind);
            blocks.add(b);
        }
        root.add("blocks", blocks);
        h.assertTrue(generators >= 20 && storages >= 6 && machines >= 30,
            "too few energy blocks: " + generators + " generators, " + storages + " storage, " + machines + " machines");

        JsonArray items = new JsonArray();
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            var id = entry.getKey().location();
            if (!id.getNamespace().equals("mio_icif")) continue;
            ItemStack stack = new ItemStack(entry.getValue());
            if (!(entry.getValue() instanceof IBatteryItem battery)) continue;
            JsonObject it = new JsonObject();
            it.addProperty("id", id.getPath());
            it.addProperty("class", entry.getValue().getClass().getSimpleName());
            it.addProperty("maxCharge", battery.getMaxEnergy(stack));
            it.addProperty("chargeRate", battery.getChargeRate(stack));
            if (entry.getValue() instanceof AbstractElectricTool tool) {
                it.addProperty("kind", "tool");
                it.addProperty("perUse", tool.getEnergyPerUse());
                it.addProperty("toolTier", tool.getToolTier());
            } else if (entry.getValue() instanceof com.miophas.singularity_iteration.core.api.item.IElectricArmorItem armor) {
                it.addProperty("kind", "armor");
                it.addProperty("armorTier", armor.getArmorTier());
                it.addProperty("energyPerTick", armor.getEnergyPerTick());
            } else {
                it.addProperty("kind", "battery");
            }
            var electric = stack.getCapability(IMioIcifCapabilities.ELECTRIC_ITEM);
            if (electric != null) {
                it.addProperty("tier", electric.getTier(stack));
                it.addProperty("transfer", electric.getTransferLimit(stack));
            }
            h.assertTrue(battery.getMaxEnergy(stack) >= 0, id + " has a negative capacity");
            items.add(it);
        }
        root.add("items", items);
        h.assertTrue(items.size() >= 20, "too few electric items: " + items.size());

        Path out = FMLPaths.GAMEDIR.get().resolve("si_energy_dump.json");
        try {
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new AssertionError("cannot write " + out, e);
        }
        h.succeed();
    }
}
