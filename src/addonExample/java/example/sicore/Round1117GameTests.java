package example.sicore;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.10.17 (E-2, sync diet): the update tag of a block entity (sent with every chunk load
 * and with every block entity packet) carries no inventory and no owner, the 15 block entities that used
 * to send their whole save send their client data only, and a tag without an inventory never clears the
 * items of a client copy. The sizes of every SI update tag are written to the log.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round1117GameTests {
    private Round1117GameTests() {}

    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static final BlockPos AT = new BlockPos(1, 2, 1);
    /** Inventory keys that no update tag may carry (except the rotor of the wind and water generators). */
    private static final Set<String> INVENTORY_KEYS = Set.of("inventory", "Items", "ReactorItems");
    private static final Set<String> ROTOR_BLOCKS = Set.of("kugenerator/block_wind_kinetic_generator", "kugenerator/block_water_kinetic_generator");

    private static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath("mio_icif", path); }

    private static int bytes(CompoundTag tag) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.writeAnyTag(tag, new DataOutputStream(out));
            return out.size();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static BlockEntity place(GameTestHelper h, String path) {
        Block block = BuiltInRegistries.BLOCK.get(rl(path));
        h.assertTrue(block != Blocks.AIR, path + " not registered");
        h.setBlock(AT, block);
        BlockEntity be = h.getBlockEntity(AT);
        h.assertTrue(be != null, path + " has no block entity");
        return be;
    }

    private static void fill(BlockEntity be) {
        if (!(be instanceof Container container)) return;
        for (int i = 0; i < container.getContainerSize(); i++) {
            try {
                container.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
            } catch (RuntimeException ignored) {
                // a slot that refuses the item stays empty; the test only needs some contents
            }
        }
    }

    // ------------------------------------------------------------------ every SI block entity

    /** Every SI block with a block entity: no inventory in the update tag; logs update tag and save sizes. */
    @GameTest(batch = "round1117_tags", template = "empty", timeoutTicks = 40)
    public static void updateTagsCarryNoInventory(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        List<String> offenders = new ArrayList<>(), failed = new ArrayList<>();
        long saveTotal = 0, updateTotal = 0;
        int checked = 0;
        StringBuilder table = new StringBuilder("SI update tags (bytes: update / full save):");
        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!id.getNamespace().equals("mio_icif") || !(entry.getValue() instanceof EntityBlock)) continue;
            try {
                h.setBlock(AT, entry.getValue());
                BlockEntity be = h.getBlockEntity(AT);
                if (be == null) continue;
                fill(be);
                CompoundTag update = be.getUpdateTag(registries);
                CompoundTag save = be.saveWithoutMetadata(registries);
                checked++;
                int u = bytes(update), s = bytes(save);
                updateTotal += u;
                saveTotal += s;
                table.append("\n  ").append(id.getPath()).append(": ").append(u).append(" / ").append(s);
                for (String key : INVENTORY_KEYS) {
                    if (update.contains(key) && !(key.equals("Items") && ROTOR_BLOCKS.contains(id.getPath()))) {
                        offenders.add(id.getPath() + "." + key);
                    }
                }
            } catch (RuntimeException e) {
                failed.add(id.getPath() + ": " + e);
            } finally {
                h.setBlock(AT, Blocks.AIR);
            }
        }
        LOG.info("{}\n  total: {} / {} bytes in {} block entities", table, updateTotal, saveTotal, checked);
        h.assertTrue(checked > 100, "only " + checked + " block entities checked");
        h.assertTrue(failed.isEmpty(), "update tag failed: " + failed);
        h.assertTrue(offenders.isEmpty(), "inventory in update tags: " + offenders);
        h.succeed();
    }

    // ------------------------------------------------------------------ the 15 former full-save tags

    private static void lacks(GameTestHelper h, String path, String... keys) {
        var registries = h.getLevel().registryAccess();
        BlockEntity be = place(h, path);
        fill(be);
        CompoundTag update = be.getUpdateTag(registries);
        CompoundTag save = be.saveWithoutMetadata(registries);
        for (String key : keys) h.assertTrue(!update.contains(key), path + ": update tag has " + key);
        h.assertTrue(bytes(update) < bytes(save), path + ": update tag " + bytes(update) + " not smaller than the save " + bytes(save));
        h.setBlock(AT, Blocks.AIR);
    }

    /** The former full-save tags leave out the server data: tanks, credits, reactor parts and owners. */
    @GameTest(batch = "round1117_trimmed", template = "empty", timeoutTicks = 40)
    public static void formerFullSaveTagsAreTrimmed(GameTestHelper h) {
        lacks(h, "producer/block_condenser", "Steam", "Distilled", "SteamCredit");
        lacks(h, "producer/block_electrolyzer_elc", "ChemicalEnergy", "EnergyAccumulator");
        lacks(h, "producer/block_fermenter_elc", "biomassTank", "biogasTank", "inventory");
        lacks(h, "hugenerator/block_fluid_heat_generator", "fuelTank", "Items");
        lacks(h, "hugenerator/block_heat_source_fluid", "InputTank", "OutputTank", "Items");
        lacks(h, "generator/block_nuclear_reactor_generator", "ReactorItems", "FluidHandler", "LiquidCooling", "AuditOwner", "AuditOwnerName");
        lacks(h, "producer/block_oil_refinery_elc", "InputTank", "OutputTank", "scex_paid_recipe");
        lacks(h, "producer/block_pump_elc", "Tank", "ActionOwner", "PaidWork");
        lacks(h, "producer/block_replicator_elc", "tank", "credit", "pending_uu");
        lacks(h, "producer/block_solar_distiller", "waterTank", "distilledTank", "inventory");
        lacks(h, "hugenerator/block_solid_heat_generator", "Items", "scex_ash_pending");
        lacks(h, "energy_converter/energy_converter", "FEStored");
        var registries = h.getLevel().registryAccess();
        h.assertTrue(place(h, "energy_converter/energy_converter").getUpdateTag(registries).contains("Mode"), "converter: no mode");
        h.setBlock(AT, Blocks.AIR);
        CompoundTag cable = place(h, "wiring/block_eu_splitter_cable").getUpdateTag(registries);
        h.assertTrue(cable.getAllKeys().equals(Set.of("active")), "special cable keys: " + cable.getAllKeys());
        h.setBlock(AT, Blocks.AIR);
        CompoundTag transformer = place(h, "wiring/transformer_lv_mv").getUpdateTag(registries);
        h.assertTrue(transformer.getAllKeys().equals(Set.of("mode", "active")), "transformer keys: " + transformer.getAllKeys());
        h.setBlock(AT, Blocks.AIR);
        CompoundTag crop = place(h, "crop/stick").getUpdateTag(registries);
        for (String key : List.of("Progress", "CropTicker", "FertilizerCooldown", "TendedUntil", "CropCycleVersion")) {
            h.assertTrue(!crop.contains(key), "crop: update tag has " + key);
        }
        h.assertTrue(crop.contains("GrowthStage") && crop.contains("ScanLevel"), "crop: visible keys missing: " + crop.getAllKeys());
        h.setBlock(AT, Blocks.AIR);
        h.succeed();
    }

    // ------------------------------------------------------------------ client copies keep their items

    /**
     * A copy of a machine (as the client holds it) keeps its items when an update tag without the
     * inventory arrives, on the chunk path (handleUpdateTag) and on the packet path (onDataPacket).
     */
    @GameTest(batch = "round1117_client", template = "empty", timeoutTicks = 40)
    public static void updateTagKeepsClientItems(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        for (String path : List.of("producer/block_powder_elc", "wiring/block_mfsu")) {
            BlockEntity be = place(h, path);
            h.assertTrue(be instanceof Container, path + " is not a container");
            Container container = (Container) be;
            container.setItem(0, new ItemStack(Items.COPPER_ORE, 48));
            BlockEntity copy = BlockEntity.loadStatic(be.getBlockPos(), be.getBlockState(), be.saveWithFullMetadata(registries), registries);
            h.assertTrue(copy instanceof Container c && c.getItem(0).is(Items.COPPER_ORE), path + ": the copy lost its items on load");
            copy.handleUpdateTag(be.getUpdateTag(registries), registries);
            h.assertTrue(((Container) copy).getItem(0).getCount() == 48, path + ": items cleared by the chunk update tag");
            copy.onDataPacket(null, ClientboundBlockEntityDataPacket.create(be), registries);
            h.assertTrue(((Container) copy).getItem(0).getCount() == 48, path + ": items cleared by the update packet");
            h.setBlock(AT, Blocks.AIR);
        }
        h.succeed();
    }
}
