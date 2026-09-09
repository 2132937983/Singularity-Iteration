package com.singularity_iteration.mio_icif.Items;

import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.Singularity_Iteration;
import com.singularity_iteration.mio_icif.Items.Rotors.mio_icif_rotor;
import com.singularity_iteration.mio_icif.Items.EnvTemplate.mio_icif_env_templates;
import com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrades;
import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class mio_icif_items {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Singularity_Iteration.MOD_ID);

    @SuppressWarnings("null")
public enum Bronze_Tier implements Tier {
        BRONZE(BlockTags.INCORRECT_FOR_STONE_TOOL, 200, 5.0F, 1.5F, 10, () -> Ingredient.of(mio_icif_resources.INGOT_BRONZE.get()));
        
        private final TagKey<Block> incorrectBlocksForDrops;
        private final int uses;
        private final float speed;
        private final float damage;
        private final int enchantmentValue;
        private final Supplier<Ingredient> repairIngredient;
        
        private Bronze_Tier(TagKey<Block> incorrectBlockForDrops, int uses, float speed, float damage, int enchantmentValue, Supplier<Ingredient> repairIngredient) {
            this.incorrectBlocksForDrops = incorrectBlockForDrops;
            this.uses = uses;
            this.speed = speed;
            this.damage = damage;
            this.enchantmentValue = enchantmentValue;
            this.repairIngredient = Suppliers.memoize(repairIngredient::get);
        }
        
        @Override
        public int getUses() {
            return this.uses;
        }
        
        @Override
        public float getSpeed() {
            return this.speed;
        }
        
        @Override
        public float getAttackDamageBonus() {
            return this.damage;
        }
        
        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return this.incorrectBlocksForDrops;
        }
        
        @Override
        public int getEnchantmentValue() {
            return this.enchantmentValue;
        }
        
        @Override
        public Ingredient getRepairIngredient() {
            return this.repairIngredient.get();
        }
    }

    //BronzeSword
    public static final DeferredItem<Item> TOOL_BRONZE_SWORD = ITEMS.register("item_tool_bronze_sword", () -> new SwordItem(Bronze_Tier.BRONZE, new Item.Properties().attributes(SwordItem.createAttributes(Bronze_Tier.BRONZE, 3.0F, -2.4F))));

    //Rotors (IC2 1.12.2 compatible stats)
    public static final DeferredItem<mio_icif_rotor> ROTOR_WOOD = ITEMS.register("rotors/item_wood_rotor", () -> new mio_icif_rotor(new Item.Properties(), 10800, 5, 0.25f, 10, 60, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/item/rotors/rotor_wood_model.png"), false));
    public static final DeferredItem<mio_icif_rotor> ROTOR_IRON = ITEMS.register("rotors/item_iron_rotor", () -> new mio_icif_rotor(new Item.Properties(), 86400, 7, 0.50f, 14, 75, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/item/rotors/rotor_iron_model.png"), true));
    public static final DeferredItem<mio_icif_rotor> ROTOR_CARBON = ITEMS.register("rotors/item_carbon_rotor", () -> new mio_icif_rotor(new Item.Properties(), 604800, 11, 1.00f, 20, 110, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/item/rotors/rotor_carbon_model.png"), true));
    public static final DeferredItem<mio_icif_rotor> ROTOR_ADVIRON = ITEMS.register("rotors/item_adviron_rotor", () -> new mio_icif_rotor(new Item.Properties(), 172800, 9, 0.75f, 17, 90, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/item/rotors/rotor_steel_model.png"), true));
    public static final DeferredItem<mio_icif_rotor> ROTOR_TITANIUM_IRON = ITEMS.register("rotors/item_titanium_iron_alloy_rotor", () -> new mio_icif_rotor(new Item.Properties(), 192800, 9, 0.91f, 12, 100, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/item/rotors/rotor_titanium_iron_alloy_model.png"), true));
    public static final DeferredItem<mio_icif_rotor> ROTOR_SUPER_IRIDIUM = ITEMS.register("rotors/item_super_iridium_alloy_rotor", () -> new mio_icif_rotor(new Item.Properties(), 2147483647, 11, 1.00f, 18, 155, ResourceLocation.fromNamespaceAndPath(Singularity_Iteration.MOD_ID, "textures/item/rotors/rotor_super_iridium_alloy_model.png"), true));

    //CarryEvent
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);

        // 注册地形转换模板
        mio_icif_env_templates.register(eventBus);

        // 注册升级插件
        mio_icif_upgrades.register(eventBus);
    }
}

