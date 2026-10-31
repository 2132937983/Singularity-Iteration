package example.sicore;

import com.miophas.singularity_iteration.core.api.item.BatteryEnergy;
import com.miophas.singularity_iteration.core.platform.neoforge.CoreDataComponents;
import com.miophas.singularity_iteration.core.prefab.blockentity.AbstractEnergyBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests of 0.1.9.1: machine contents as crafting remainders and the Recovered Parts Case,
 * the induction furnace heat hold and Thermal Retention Unit, charge-agnostic matching, and the
 * Wingman threat priority. SI common classes are reached by reflection.
 */
@GameTestHolder(CoreExampleMod.ID)
@PrefixGameTestTemplate(false)
public final class Round191GameTests {

    /** A machine item carrying {@code contents} the way the SI wrench stores a machine inventory. */
    static ItemStack machineWith(GameTestHelper h, String machine, ItemStack... contents) {
        ItemStack stack = Round33GameTests.item(machine);
        CompoundTag data = new CompoundTag();
        CompoundTag inv = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < contents.length; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", 3 + i);
            entry.put("Item", contents[i].save(h.getLevel().registryAccess()));
            list.add(entry);
        }
        inv.put("Items", list);
        inv.putInt("Size", 9);
        data.put("inventory", inv);
        data.putString("id", "mio_icif:producer/block_powder_elc");
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(data));
        return stack;
    }

    /** One stack comes back as it is; several stacks come back in a Recovered Parts Case; a plain machine leaves nothing. */
    @GameTest(template = "empty", batch = "round191_contents")
    public static void machineContentsAreRemainders(GameTestHelper h) {
        ItemStack one = machineWith(h, "producer/block_powder_elc", Round33GameTests.item("upgrade/overclocker_upgrade").copyWithCount(3));
        h.assertTrue(one.hasCraftingRemainingItem(), "a machine with an upgrade inside has no remainder");
        ItemStack back = one.getCraftingRemainingItem();
        h.assertTrue(BuiltInRegistries.ITEM.getKey(back.getItem()).getPath().equals("upgrade/overclocker_upgrade") && back.getCount() == 3,
            "single remainder " + back);
        ItemStack two = machineWith(h, "producer/block_powder_elc", Round33GameTests.item("upgrade/overclocker_upgrade").copyWithCount(2),
            Round33GameTests.item("upgrade/transformer_upgrade"));
        ItemStack box = two.getCraftingRemainingItem();
        h.assertTrue(BuiltInRegistries.ITEM.getKey(box.getItem()).getPath().equals("normal/item_recovered_parts"), "two stacks: " + box);
        ItemContainerContents contents = box.get(DataComponents.CONTAINER);
        // count the stacks (a filtered iterable reports no exact size: its spliterator size is always -1)
        h.assertTrue(contents != null && contents.nonEmptyStream().count() == 2, "case contents " + contents);
        h.assertTrue(!Round33GameTests.item("producer/block_powder_elc").hasCraftingRemainingItem(), "a plain machine item has a remainder");
        h.succeed();
    }

    /** The tier-up recipe of the macerator hands the old macerator's plugins back through getRemainingItems. */
    @GameTest(template = "empty", batch = "round191_contents")
    public static void tierUpRecipeReturnsPlugins(GameTestHelper h) {
        var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("mio_icif", "mets/shaped_advanced_macerator"));
        if (holder.isEmpty() || !(holder.get().value() instanceof ShapedRecipe recipe)) { h.fail("advanced macerator recipe missing"); return; }
        ItemStack old = machineWith(h, "producer/block_powder_elc", Round33GameTests.item("upgrade/overclocker_upgrade").copyWithCount(4));
        List<ItemStack> grid = new ArrayList<>();
        int machineSlot = -1;
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ing = ingredients.get(i);
            if (ing.isEmpty()) { grid.add(ItemStack.EMPTY); continue; }
            if (ing.test(old)) { grid.add(old.copy()); machineSlot = i; continue; }
            ItemStack[] options = ing.getItems();
            grid.add(options.length == 0 ? ItemStack.EMPTY : options[0].copy());
        }
        CraftingInput input = CraftingInput.of(recipe.getWidth(), recipe.getHeight(), grid);
        h.assertTrue(machineSlot >= 0 && recipe.matches(input, h.getLevel()), "the old macerator does not fit the recipe");
        var remaining = ((CraftingRecipe) recipe).getRemainingItems(input);
        boolean returned = remaining.stream().anyMatch(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().equals("upgrade/overclocker_upgrade")
            && s.getCount() == 4);
        h.assertTrue(returned, "the overclockers did not come back: " + remaining);
        h.succeed();
    }

    /** The case unpacks itself in a player's inventory. */
    @GameTest(template = "empty", batch = "round191_contents", timeoutTicks = 20)
    public static void recoveredPartsUnpack(GameTestHelper h) {
        ServerPlayer p = Round33GameTests.survivalPlayer(h);
        ItemStack box = machineWith(h, "producer/block_powder_elc", Round33GameTests.item("upgrade/overclocker_upgrade").copyWithCount(2),
            Round33GameTests.item("upgrade/ejector_upgrade")).getCraftingRemainingItem();
        p.getInventory().setItem(5, box);
        p.getInventory().tick();
        int over = 0, ejector = 0;
        boolean caseLeft = false;
        for (ItemStack s : p.getInventory().items) {
            String path = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
            if (path.equals("upgrade/overclocker_upgrade")) over += s.getCount();
            if (path.equals("upgrade/ejector_upgrade")) ejector += s.getCount();
            if (path.equals("normal/item_recovered_parts")) caseLeft = true;
        }
        Round33GameTests.remove(p);
        h.assertTrue(over == 2 && ejector == 1 && !caseLeft, "unpacked " + over + " overclockers, " + ejector + " ejector, case left " + caseLeft);
        h.succeed();
    }

    // ------------------------------------------------------------------ induction furnace

    private static final BlockPos FURNACE = new BlockPos(2, 2, 2);

    static BlockEntity furnace(GameTestHelper h) {
        h.setBlock(FURNACE, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "producer/block_induction_elc")));
        BlockEntity be = h.getBlockEntity(FURNACE);
        ((AbstractEnergyBlockEntity) be).getEnergyStorageInternal().setEnergy(10_000);
        return be;
    }

    static long heat(BlockEntity be) {
        Object storage = Round33GameTests.call(be, "getHeatStorage");
        return ((Number) Round33GameTests.call(storage, "getHeatStored")).longValue();
    }

    /** Heat hold: an idle furnace heats up; without it an idle furnace cools down. */
    @GameTest(template = "reactor_loop", batch = "round191_induction", timeoutTicks = 80)
    public static void heatHoldHeatsAnIdleFurnace(GameTestHelper h) {
        BlockEntity be = furnace(h);
        Round33GameTests.call(be, "toggleHeatHold");
        h.assertTrue((boolean) Round33GameTests.call(be, "heatHold"), "heat hold did not switch on");
        h.onEachTick(() -> ((AbstractEnergyBlockEntity) be).getEnergyStorageInternal().setEnergy(10_000));
        h.runAfterDelay(40, () -> {
            h.assertTrue(heat(be) >= 30, "an idle furnace with heat hold did not heat: " + heat(be));
            h.succeed();
        });
    }

    /** Thermal Retention Unit: an idle furnace keeps its heat; without the unit the heat drops. */
    @GameTest(template = "reactor_loop", batch = "round191_induction", timeoutTicks = 80)
    public static void thermalRetentionKeepsHeat(GameTestHelper h) {
        BlockEntity be = furnace(h);
        Object storage = Round33GameTests.call(be, "getHeatStorage");
        Round33GameTests.call(storage, "setHeat", 5000L);
        var slots = (net.neoforged.neoforge.items.IItemHandlerModifiable) ((com.miophas.singularity_iteration.core.prefab.blockentity.AbstractProcessingMachineBlockEntity) be).getItemHandler();
        slots.setStackInSlot(5, Round33GameTests.item("upgrade/thermal_retention_upgrade"));
        h.runAfterDelay(40, () -> {
            long kept = heat(be);
            slots.setStackInSlot(5, ItemStack.EMPTY);
            h.runAfterDelay(20, () -> {
                h.assertTrue(kept == 5000, "an insulated idle furnace lost heat: " + kept);
                h.assertTrue(heat(be) < kept, "an idle furnace without the unit kept its heat: " + heat(be));
                h.succeed();
            });
        });
    }

    // ------------------------------------------------------------------ charge-agnostic matching

    @GameTest(template = "empty", batch = "round191_charge")
    public static void chargeDoesNotChangeTheMaterial(GameTestHelper h) {
        Class<?> diff = Round33GameTests.common("item.EnergyStackDiff");
        ItemStack empty = Round33GameTests.item("normal/item_bat_lev0");
        ItemStack charged = empty.copy();
        charged.set(CoreDataComponents.BATTERY_ENERGY.get(), new BatteryEnergy(7_000, 10_000));
        h.assertTrue((boolean) Round33GameTests.call(diff, "sameIgnoringCharge", empty, charged), "a charged battery is a different material");
        ItemStack named = charged.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("x"));
        h.assertTrue(!(boolean) Round33GameTests.call(diff, "sameIgnoringCharge", empty, named), "a renamed battery matched");
        h.assertTrue(!(boolean) Round33GameTests.call(diff, "sameIgnoringCharge", empty, Round33GameTests.item("normal/item_bat_lev1")),
            "a different item matched");
        h.succeed();
    }

    // ------------------------------------------------------------------ Wingman threat priority

    /** The owner's call outranks everything; a fusing creeper next to the owner outranks a far zombie. */
    @GameTest(template = "empty", batch = "round191_wingman", timeoutTicks = 20)
    public static void wingmanThreatPriority(GameTestHelper h) {
        ServerPlayer owner = Round33GameTests.survivalPlayer(h);
        var type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("mio_icif", "wingman"));
        var wingman = type.create(h.getLevel());
        if (wingman == null) { Round33GameTests.remove(owner); h.fail("no wingman entity type"); return; }
        wingman.moveTo(h.absoluteVec(new Vec3(1.5, 4, 1.5)));
        Zombie zombie = EntityType.ZOMBIE.create(h.getLevel());
        zombie.moveTo(h.absoluteVec(new Vec3(8.5, 2, 8.5)));
        Creeper creeper = EntityType.CREEPER.create(h.getLevel());
        creeper.moveTo(h.absoluteVec(new Vec3(2.5, 2, 1.5)));
        h.getLevel().addFreshEntity(zombie);
        h.getLevel().addFreshEntity(creeper);
        Class<?> tactics = Round33GameTests.common("armory.WingmanTactics");
        creeper.setSwellDir(1);
        double zombieScore = (double) Round33GameTests.call(tactics, "threat", wingman, owner, zombie, (LivingEntity) null);
        double creeperScore = (double) Round33GameTests.call(tactics, "threat", wingman, owner, creeper, (LivingEntity) null);
        owner.setLastHurtMob(zombie);
        double calledScore = (double) Round33GameTests.call(tactics, "threat", wingman, owner, zombie, (LivingEntity) null);
        zombie.discard();
        creeper.discard();
        Round33GameTests.remove(owner);
        h.assertTrue(creeperScore > zombieScore, "fusing creeper " + creeperScore + " <= zombie " + zombieScore);
        h.assertTrue(calledScore > creeperScore, "owner's call " + calledScore + " <= creeper " + creeperScore);
        h.succeed();
    }
}
