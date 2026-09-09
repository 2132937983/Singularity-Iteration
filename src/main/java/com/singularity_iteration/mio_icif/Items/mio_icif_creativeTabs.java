package com.singularity_iteration.mio_icif.Items;

import java.util.function.Supplier;

import com.singularity_iteration.mio_icif.Items.Armor.mio_icif_items_armors;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_cells;
import com.singularity_iteration.mio_icif.Items.Cell.mio_icif_dynamic_cell;
import com.singularity_iteration.mio_icif.Items.Reactor.mio_icif_reactors;
import com.singularity_iteration.mio_icif.Blocks.Environment.fluid.mio_icif_fluids;
import com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources;
import com.singularity_iteration.mio_icif.Items.Crop.CropSeedItem;
import com.singularity_iteration.mio_icif.Items.Normal.mio_icif_normal;
import com.singularity_iteration.mio_icif.Items.Tools.mio_icif_items_tools;
import com.singularity_iteration.mio_icif.api.MioIcifAPI;
import com.singularity_iteration.mio_icif.Items.EnvTemplate.mio_icif_env_templates;
import com.singularity_iteration.mio_icif.Items.Upgrade.mio_icif_upgrades;
import com.singularity_iteration.mio_icif.Blocks.mio_icif_blocks;
import com.singularity_iteration.mio_icif.Singularity_Iteration;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

@SuppressWarnings("null")
public class mio_icif_creativeTabs {
    
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Singularity_Iteration.MOD_ID);

    public static final Supplier<CreativeModeTab> ICIF_TAB = TABS.register("icif_tab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(mio_icif_blocks.THERMAL_GENERATOR.get()))
            .title(Component.translatable("itemGroup.mio_icif"))
            .displayItems(((parameters, output) -> {

                // ============================================================
                //  1. TOOLS
                // ============================================================
                output.accept(mio_icif_items_tools.TOOL_BRONZE_PICKAXE.get());
                output.accept(mio_icif_items_tools.TOOL_BRONZE_AXE.get());
                output.accept(mio_icif_items_tools.TOOL_BRONZE_SHOVEL.get());
                output.accept(mio_icif_items_tools.TOOL_BRONZE_HOE.get());
                output.accept(mio_icif_items.TOOL_BRONZE_SWORD.get());
                output.accept(mio_icif_items_tools.TOOL_HAMMER.get());
                output.accept(mio_icif_items_tools.TOOL_CUTTER.get());

                output.accept(mio_icif_items_tools.IRON_DRILLER.get());
                { ItemStack fullIronDriller = new ItemStack(mio_icif_items_tools.IRON_DRILLER.get()); fullIronDriller.setDamageValue(0); output.accept(fullIronDriller); }
                output.accept(mio_icif_items_tools.DIAMOND_DRILLER.get());
                { ItemStack fullDiamondDriller = new ItemStack(mio_icif_items_tools.DIAMOND_DRILLER.get()); fullDiamondDriller.setDamageValue(0); output.accept(fullDiamondDriller); }
                output.accept(mio_icif_items_tools.IRIDIUM_DRILLER.get());
                { ItemStack fullIridiumDriller = new ItemStack(mio_icif_items_tools.IRIDIUM_DRILLER.get()); fullIridiumDriller.setDamageValue(0); output.accept(fullIridiumDriller); }
                output.accept(mio_icif_items_tools.IRON_CHAINSAW.get());
                { ItemStack fullIronChainsaw = new ItemStack(mio_icif_items_tools.IRON_CHAINSAW.get()); fullIronChainsaw.setDamageValue(0); output.accept(fullIronChainsaw); }
                output.accept(mio_icif_items_tools.NANO_SABER.get());
                { ItemStack fullNanoSaber = new ItemStack(mio_icif_items_tools.NANO_SABER.get()); fullNanoSaber.setDamageValue(0); output.accept(fullNanoSaber); }
                output.accept(mio_icif_items_tools.PLASMA_LAUNCHER.get());
                { ItemStack fullPlasmaLauncher = new ItemStack(mio_icif_items_tools.PLASMA_LAUNCHER.get()); fullPlasmaLauncher.setDamageValue(0); output.accept(fullPlasmaLauncher); }
                output.accept(mio_icif_items_tools.TOOL_LASER_MINER.get());
                { ItemStack fullLaserMiner = new ItemStack(mio_icif_items_tools.TOOL_LASER_MINER.get()); fullLaserMiner.setDamageValue(0); output.accept(fullLaserMiner); }

                output.accept(mio_icif_items_tools.WRENCH.get());
                output.accept(mio_icif_items_tools.WRENCH_ELC.get());
                // 电动扳手 - 空电版本
                { ItemStack emptyWrenchElc = new ItemStack(mio_icif_items_tools.WRENCH_ELC.get());
                  emptyWrenchElc.setDamageValue(mio_icif_items_tools.WRENCH_ELC.get().getDefaultInstance().getMaxDamage());
                  output.accept(emptyWrenchElc); }
                // 电动扳手 - 满电版本
                { ItemStack fullWrenchElc = new ItemStack(mio_icif_items_tools.WRENCH_ELC.get()); fullWrenchElc.setDamageValue(0); output.accept(fullWrenchElc); }
                output.accept(mio_icif_items_tools.TOOL_TREE_TAP.get());
                output.accept(mio_icif_items_tools.TREETAP_ELC.get());
                { ItemStack fullTreetapElc = new ItemStack(mio_icif_items_tools.TREETAP_ELC.get()); fullTreetapElc.setDamageValue(0); output.accept(fullTreetapElc); }
                output.accept(mio_icif_items_tools.OD_SCANNER.get());
                { ItemStack fullOdScanner = new ItemStack(mio_icif_items_tools.OD_SCANNER.get()); fullOdScanner.setDamageValue(0); output.accept(fullOdScanner); }
                output.accept(mio_icif_items_tools.OV_SCANNER.get());
                { ItemStack fullOvScanner = new ItemStack(mio_icif_items_tools.OV_SCANNER.get()); fullOvScanner.setDamageValue(0); output.accept(fullOvScanner); }
                output.accept(mio_icif_items_tools.EU_METER.get());
                output.accept(mio_icif_items_tools.WINDMETER.get());
                { ItemStack fullWindmeter = new ItemStack(mio_icif_items_tools.WINDMETER.get()); fullWindmeter.setDamageValue(0); output.accept(fullWindmeter); }
                output.accept(mio_icif_items_tools.POWER_UNIT.get());
                { ItemStack fullPowerUnit = new ItemStack(mio_icif_items_tools.POWER_UNIT.get()); fullPowerUnit.setDamageValue(0); output.accept(fullPowerUnit); }
                output.accept(mio_icif_items_tools.POWER_UNIT_SMALL.get());
                { ItemStack fullPowerUnitSmall = new ItemStack(mio_icif_items_tools.POWER_UNIT_SMALL.get()); fullPowerUnitSmall.setDamageValue(0); output.accept(fullPowerUnitSmall); }

                // METS 移植电力工具

                // 电力营养供应器 - 空电和满电
                output.accept(mio_icif_items_tools.ELECTRIC_NUTRITION_SUPPLY.get());
                { ItemStack fullNutrition = new ItemStack(mio_icif_items_tools.ELECTRIC_NUTRITION_SUPPLY.get());
                  MioIcifAPI.instance().getItemAPI().setBatteryEnergy(fullNutrition, MioIcifAPI.instance().getItemAPI().getBatteryCapacity(fullNutrition));
                  output.accept(fullNutrition); }

                // 电力生命维护仪 - 空电和满电
                output.accept(mio_icif_items_tools.ELECTRIC_FIRST_AID_LIFE_SUPPORT.get());
                { ItemStack fullFirstAid = new ItemStack(mio_icif_items_tools.ELECTRIC_FIRST_AID_LIFE_SUPPORT.get());
                  MioIcifAPI.instance().getItemAPI().setBatteryEnergy(fullFirstAid, MioIcifAPI.instance().getItemAPI().getBatteryCapacity(fullFirstAid));
                  output.accept(fullFirstAid); }

                // 电力伤害吸收仪 - 空电和满电
                output.accept(mio_icif_items_tools.ELECTRIC_FORCE_FIELD_GENERATOR.get());
                { ItemStack fullForceField = new ItemStack(mio_icif_items_tools.ELECTRIC_FORCE_FIELD_GENERATOR.get());
                  MioIcifAPI.instance().getItemAPI().setBatteryEnergy(fullForceField, MioIcifAPI.instance().getItemAPI().getBatteryCapacity(fullForceField));
                  output.accept(fullForceField); }

                // 电动鱼竿 - 空电和满电
                output.accept(mio_icif_items_tools.ELECTRIC_FISHING_ROD.get());
                { ItemStack fullFishingRod = new ItemStack(mio_icif_items_tools.ELECTRIC_FISHING_ROD.get());
                  MioIcifAPI.instance().getItemAPI().setBatteryEnergy(fullFishingRod, MioIcifAPI.instance().getItemAPI().getBatteryCapacity(fullFishingRod));
                  output.accept(fullFishingRod); }

                // 电力光源产生器 - 空电和满电
                output.accept(mio_icif_items_tools.ELECTRIC_LIGHTER.get());
                { ItemStack fullLighter = new ItemStack(mio_icif_items_tools.ELECTRIC_LIGHTER.get());
                  MioIcifAPI.instance().getItemAPI().setBatteryEnergy(fullLighter, MioIcifAPI.instance().getItemAPI().getBatteryCapacity(fullLighter));
                  output.accept(fullLighter); }

                // 无线管理器 - 空电和满电
                output.accept(mio_icif_items_tools.ELECTRIC_WIRELESS_MANAGER.get());
                { ItemStack fullWireless = new ItemStack(mio_icif_items_tools.ELECTRIC_WIRELESS_MANAGER.get());
                  MioIcifAPI.instance().getItemAPI().setBatteryEnergy(fullWireless, MioIcifAPI.instance().getItemAPI().getBatteryCapacity(fullWireless));
                  output.accept(fullWireless); }

                // 地磁探测器 - 空电和满电
                output.accept(mio_icif_items_tools.GEOMAGNETIC_DETECTOR.get());
                { ItemStack fullGeomagnetic = new ItemStack(mio_icif_items_tools.GEOMAGNETIC_DETECTOR.get());
                  MioIcifAPI.instance().getItemAPI().setBatteryEnergy(fullGeomagnetic, MioIcifAPI.instance().getItemAPI().getBatteryCapacity(fullGeomagnetic));
                  output.accept(fullGeomagnetic); }

                // ============================================================
                //  METS 移植武器
                // ============================================================
                output.accept(mio_icif_items_tools.QUANTUM_SWORD.get());
                { ItemStack fullQuantumSword = new ItemStack(mio_icif_items_tools.QUANTUM_SWORD.get());
                  MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullQuantumSword, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullQuantumSword));
                  output.accept(fullQuantumSword); }
                output.accept(mio_icif_items_tools.ELECTRIC_RIFLE.get());
                { ItemStack fullRifle = new ItemStack(mio_icif_items_tools.ELECTRIC_RIFLE.get());
                  MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullRifle, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullRifle));
                  output.accept(fullRifle); }
                output.accept(mio_icif_items_tools.ADVANCED_ELECTRIC_RIFLE.get());
                { ItemStack fullAdvRifle = new ItemStack(mio_icif_items_tools.ADVANCED_ELECTRIC_RIFLE.get());
                  MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullAdvRifle, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullAdvRifle));
                  output.accept(fullAdvRifle); }
                output.accept(mio_icif_items_tools.TACTICAL_LASER_RIFLE.get());
                { ItemStack fullTacticalRifle = new ItemStack(mio_icif_items_tools.TACTICAL_LASER_RIFLE.get());
                  MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullTacticalRifle, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullTacticalRifle));
                  output.accept(fullTacticalRifle); }
                output.accept(mio_icif_items_tools.PLASMA_AIR_CANNON.get());
                { ItemStack fullPlasmaCannon = new ItemStack(mio_icif_items_tools.PLASMA_AIR_CANNON.get());
                  MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullPlasmaCannon, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullPlasmaCannon));
                  output.accept(fullPlasmaCannon); }
                output.accept(mio_icif_items_tools.ELECTRIC_PLASMA_GUN.get());
                { ItemStack fullPlasmaGun = new ItemStack(mio_icif_items_tools.ELECTRIC_PLASMA_GUN.get());
                  MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullPlasmaGun, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullPlasmaGun));
                  output.accept(fullPlasmaGun); }
                output.accept(mio_icif_items_tools.TACHYON_DISRUPTOR.get());
                { ItemStack fullTachyon = new ItemStack(mio_icif_items_tools.TACHYON_DISRUPTOR.get());
                  MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullTachyon, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullTachyon));
                  output.accept(fullTachyon); }
                output.accept(mio_icif_items_tools.STEEL_SHIELD.get());
                { ItemStack fullShield = new ItemStack(mio_icif_items_tools.STEEL_SHIELD.get());
                  fullShield.setDamageValue(0); output.accept(fullShield); }
                output.accept(mio_icif_items_tools.NANO_BOW.get());
                { ItemStack fullBow = new ItemStack(mio_icif_items_tools.NANO_BOW.get());
                  fullBow.setDamageValue(0); output.accept(fullBow); }
                output.accept(mio_icif_items_tools.ROCKET_LAUNCHER.get());
                { ItemStack fullRocketLauncher = new ItemStack(mio_icif_items_tools.ROCKET_LAUNCHER.get());
                  MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullRocketLauncher, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullRocketLauncher));
                  output.accept(fullRocketLauncher); }
                output.accept(mio_icif_items_tools.ROCKET.get());

                // ============================================================
                //  METS 移植饰品 (Curios)
                // ============================================================
                if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
                    try {
                        Class<?> curiosIntegrationClass = Class.forName("com.singularity_iteration.mio_icif.integration.CuriosIntegration");
                        java.lang.reflect.Field fireproofNecklaceField = curiosIntegrationClass.getField("TRINKET_FIREPROOF_NECKLACE");
                        java.lang.reflect.Field energyCrystalBeltField = curiosIntegrationClass.getField("TRINKET_ENERGY_CRYSTAL_BELT");
                        java.lang.reflect.Field laportonCrystalBeltField = curiosIntegrationClass.getField("TRINKET_LAPORTON_CRYSTAL_BELT");
                        java.lang.reflect.Field lifeSupportRingField = curiosIntegrationClass.getField("TRINKET_LIFE_SUPPORT_RING");
                        java.lang.reflect.Field flightRingField = curiosIntegrationClass.getField("TRINKET_FLIGHT_RING");

                        net.neoforged.neoforge.registries.DeferredItem<?> fireproofNecklace = (net.neoforged.neoforge.registries.DeferredItem<?>) fireproofNecklaceField.get(null);
                        net.neoforged.neoforge.registries.DeferredItem<?> energyCrystalBelt = (net.neoforged.neoforge.registries.DeferredItem<?>) energyCrystalBeltField.get(null);
                        net.neoforged.neoforge.registries.DeferredItem<?> laportonCrystalBelt = (net.neoforged.neoforge.registries.DeferredItem<?>) laportonCrystalBeltField.get(null);
                        net.neoforged.neoforge.registries.DeferredItem<?> lifeSupportRing = (net.neoforged.neoforge.registries.DeferredItem<?>) lifeSupportRingField.get(null);
                        net.neoforged.neoforge.registries.DeferredItem<?> flightRing = (net.neoforged.neoforge.registries.DeferredItem<?>) flightRingField.get(null);

                        // 防火项链 - 空电版本
                        { ItemStack emptyFireNecklace = new ItemStack(fireproofNecklace.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(emptyFireNecklace, 0);
                          output.accept(emptyFireNecklace); }
                        // 防火项链 - 满电版本
                        { ItemStack fullFireNecklace = new ItemStack(fireproofNecklace.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullFireNecklace, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullFireNecklace));
                          output.accept(fullFireNecklace); }
                        // 能量水晶腰带 - 空电版本
                        { ItemStack emptyEnergyBelt = new ItemStack(energyCrystalBelt.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(emptyEnergyBelt, 0);
                          output.accept(emptyEnergyBelt); }
                        // 能量水晶腰带 - 满电版本
                        { ItemStack fullEnergyBelt = new ItemStack(energyCrystalBelt.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullEnergyBelt, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullEnergyBelt));
                          output.accept(fullEnergyBelt); }
                        // 拉普顿水晶腰带 - 空电版本
                        { ItemStack emptyLapotronBelt = new ItemStack(laportonCrystalBelt.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(emptyLapotronBelt, 0);
                          output.accept(emptyLapotronBelt); }
                        // 拉普顿水晶腰带 - 满电版本
                        { ItemStack fullLapotronBelt = new ItemStack(laportonCrystalBelt.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullLapotronBelt, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullLapotronBelt));
                          output.accept(fullLapotronBelt); }
                        // 生命维持指环 - 空电版本
                        { ItemStack emptyLifeRing = new ItemStack(lifeSupportRing.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(emptyLifeRing, 0);
                          output.accept(emptyLifeRing); }
                        // 生命维持指环 - 满电版本
                        { ItemStack fullLifeRing = new ItemStack(lifeSupportRing.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullLifeRing, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullLifeRing));
                          output.accept(fullLifeRing); }
                        // 电力飞行指环 - 空电版本
                        { ItemStack emptyFlightRing = new ItemStack(flightRing.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(emptyFlightRing, 0);
                          output.accept(emptyFlightRing); }
                        // 电力飞行指环 - 满电版本
                        { ItemStack fullFlightRing = new ItemStack(flightRing.get());
                          MioIcifAPI.instance().getItemAPI().setElectricToolEnergy(fullFlightRing, MioIcifAPI.instance().getItemAPI().getElectricToolMaxEnergy(fullFlightRing));
                          output.accept(fullFlightRing); }
                    } catch (Exception e) {
                    }
                }

                // ============================================================
                //  2. ARMOR
                // ============================================================
                // Bronze Armor
                output.accept(mio_icif_items_armors.ARMOR_BRONZE_HELMET.get());
                output.accept(mio_icif_items_armors.ARMOR_BRONZE_CHESTPLATE.get());
                output.accept(mio_icif_items_armors.ARMOR_BRONZE_LEGGINGS.get());
                output.accept(mio_icif_items_armors.ARMOR_BRONZE_BOOTS.get());
                // Hazmat Suit
                output.accept(mio_icif_items_armors.HAZMAT_HELMET.get());
                output.accept(mio_icif_items_armors.HAZMAT_CHESTPLATE.get());
                output.accept(mio_icif_items_armors.HAZMAT_LEGGINGS.get());
                output.accept(mio_icif_items_armors.HAZMAT_BOOTS.get());
                // Battery Packs
                output.accept(mio_icif_items_armors.ARMOR_BATPACK.get());
                { ItemStack fullBatpack = new ItemStack(mio_icif_items_armors.ARMOR_BATPACK.get()); fullBatpack.setDamageValue(0); output.accept(fullBatpack); }
                output.accept(mio_icif_items_armors.ARMOR_ADV_BATPACK.get());
                { ItemStack fullAdvBatpack = new ItemStack(mio_icif_items_armors.ARMOR_ADV_BATPACK.get()); fullAdvBatpack.setDamageValue(0); output.accept(fullAdvBatpack); }
                output.accept(mio_icif_items_armors.ARMOR_ENERGYPACK.get());
                { ItemStack fullEnergypack = new ItemStack(mio_icif_items_armors.ARMOR_ENERGYPACK.get()); fullEnergypack.setDamageValue(0); output.accept(fullEnergypack); }
                output.accept(mio_icif_items_armors.ARMOR_LAPPACK.get());
                { ItemStack fullLappack = new ItemStack(mio_icif_items_armors.ARMOR_LAPPACK.get()); fullLappack.setDamageValue(0); output.accept(fullLappack); }
                // Nightvision Goggles
                output.accept(mio_icif_items_armors.ARMOR_NIGHTVISION_GOGGLES.get());
                { ItemStack fullNightvision = new ItemStack(mio_icif_items_armors.ARMOR_NIGHTVISION_GOGGLES.get()); fullNightvision.setDamageValue(0); output.accept(fullNightvision); }
                // NanoSuit
                output.accept(mio_icif_items_armors.ARMOR_NANO_HELMET.get());
                { ItemStack fullNanoHelmet = new ItemStack(mio_icif_items_armors.ARMOR_NANO_HELMET.get()); fullNanoHelmet.setDamageValue(0); output.accept(fullNanoHelmet); }
                output.accept(mio_icif_items_armors.ARMOR_NANO_CHESTPLATE.get());
                { ItemStack fullNanoChestplate = new ItemStack(mio_icif_items_armors.ARMOR_NANO_CHESTPLATE.get()); fullNanoChestplate.setDamageValue(0); output.accept(fullNanoChestplate); }
                output.accept(mio_icif_items_armors.ARMOR_NANO_LEGGINGS.get());
                { ItemStack fullNanoLeggings = new ItemStack(mio_icif_items_armors.ARMOR_NANO_LEGGINGS.get()); fullNanoLeggings.setDamageValue(0); output.accept(fullNanoLeggings); }
                output.accept(mio_icif_items_armors.ARMOR_NANO_BOOTS.get());
                { ItemStack fullNanoBoots = new ItemStack(mio_icif_items_armors.ARMOR_NANO_BOOTS.get()); fullNanoBoots.setDamageValue(0); output.accept(fullNanoBoots); }
                // QuantumSuit
                output.accept(mio_icif_items_armors.ARMOR_QUANTUM_HELMET.get());
                { ItemStack fullQuantumHelmet = new ItemStack(mio_icif_items_armors.ARMOR_QUANTUM_HELMET.get()); fullQuantumHelmet.setDamageValue(0); output.accept(fullQuantumHelmet); }
                output.accept(mio_icif_items_armors.ARMOR_QUANTUM_CHESTPLATE.get());
                { ItemStack fullQuantumChestplate = new ItemStack(mio_icif_items_armors.ARMOR_QUANTUM_CHESTPLATE.get()); fullQuantumChestplate.setDamageValue(0); output.accept(fullQuantumChestplate); }
                output.accept(mio_icif_items_armors.ARMOR_QUANTUM_LEGGINGS.get());
                { ItemStack fullQuantumLeggings = new ItemStack(mio_icif_items_armors.ARMOR_QUANTUM_LEGGINGS.get()); fullQuantumLeggings.setDamageValue(0); output.accept(fullQuantumLeggings); }
                output.accept(mio_icif_items_armors.ARMOR_QUANTUM_BOOTS.get());
                { ItemStack fullQuantumBoots = new ItemStack(mio_icif_items_armors.ARMOR_QUANTUM_BOOTS.get()); fullQuantumBoots.setDamageValue(0); output.accept(fullQuantumBoots); }
                // Jetpack
                output.accept(mio_icif_items_armors.ARMOR_JETPACK_ELECTRIC.get());
                { ItemStack fullJetpack = new ItemStack(mio_icif_items_armors.ARMOR_JETPACK_ELECTRIC.get()); fullJetpack.setDamageValue(0); output.accept(fullJetpack); }
                // METS 移植装备
                output.accept(mio_icif_items_armors.ARMOR_DIVING_MASK.get());
                { ItemStack fullDivingMask = new ItemStack(mio_icif_items_armors.ARMOR_DIVING_MASK.get()); fullDivingMask.setDamageValue(0); output.accept(fullDivingMask); }
                output.accept(mio_icif_items_armors.ARMOR_ADVANCED_JETPACK.get());
                { ItemStack fullAdvJetpack = new ItemStack(mio_icif_items_armors.ARMOR_ADVANCED_JETPACK.get()); fullAdvJetpack.setDamageValue(0); output.accept(fullAdvJetpack); }
                output.accept(mio_icif_items_armors.ARMOR_HEAVY_QUANTUM_CHESTPLATE.get());
                { ItemStack fullHeavyQuantum = new ItemStack(mio_icif_items_armors.ARMOR_HEAVY_QUANTUM_CHESTPLATE.get()); fullHeavyQuantum.setDamageValue(0); output.accept(fullHeavyQuantum); }
                output.accept(mio_icif_items_armors.ARMOR_ADVANCED_QUANTUM_CHESTPLATE.get());
                { ItemStack fullAdvQuantum = new ItemStack(mio_icif_items_armors.ARMOR_ADVANCED_QUANTUM_CHESTPLATE.get()); fullAdvQuantum.setDamageValue(0); output.accept(fullAdvQuantum); }
                // Solar Helmet
                output.accept(mio_icif_normal.SOLAR_HELMET.get());
                { ItemStack fullSolarHelmet = new ItemStack(mio_icif_normal.SOLAR_HELMET.get()); fullSolarHelmet.setDamageValue(0); output.accept(fullSolarHelmet); }
                // ASP Advanced Solar Helmet
                output.accept(mio_icif_items_armors.ARMOR_ADVANCED_SOLAR_HELMET.get());
                { ItemStack fullAdvSolarHelmet = new ItemStack(mio_icif_items_armors.ARMOR_ADVANCED_SOLAR_HELMET.get()); fullAdvSolarHelmet.setDamageValue(0); output.accept(fullAdvSolarHelmet); }
                // ASP Hybrid Solar Helmet
                output.accept(mio_icif_items_armors.ARMOR_HYBRID_SOLAR_HELMET.get());
                { ItemStack fullHybridSolarHelmet = new ItemStack(mio_icif_items_armors.ARMOR_HYBRID_SOLAR_HELMET.get()); fullHybridSolarHelmet.setDamageValue(0); output.accept(fullHybridSolarHelmet); }
                // ASP Ultimate Solar Helmet
                output.accept(mio_icif_items_armors.ARMOR_ULTIMATE_SOLAR_HELMET.get());
                { ItemStack fullUltimateSolarHelmet = new ItemStack(mio_icif_items_armors.ARMOR_ULTIMATE_SOLAR_HELMET.get()); fullUltimateSolarHelmet.setDamageValue(0); output.accept(fullUltimateSolarHelmet); }
                // Static Boots
                output.accept(mio_icif_normal.STATIC_BOOTS.get());

                // ============================================================
                //  3. RAW ORE BLOCKS
                // ============================================================
                output.accept(mio_icif_blocks.BLOCK_ORE_TIN.get());
                output.accept(mio_icif_blocks.BLOCK_ORE_TIN_IN_DEEP.get());
                output.accept(mio_icif_blocks.BLOCK_TIN.get());
                output.accept(mio_icif_blocks.BLOCK_ORE_LEAD.get());
                output.accept(mio_icif_blocks.BLOCK_ORE_LEAD_IN_DEEP.get());
                output.accept(mio_icif_blocks.BLOCK_LEAD.get());
                output.accept(mio_icif_blocks.BLOCK_ORE_URAN.get());
                output.accept(mio_icif_blocks.BLOCK_ORE_URAN_IN_DEEP.get());
                output.accept(mio_icif_blocks.BLOCK_URAN.get());
                output.accept(mio_icif_blocks.BLOCK_ORE_NIOBIUM.get());
                output.accept(mio_icif_blocks.BLOCK_ORE_NIOBIUM_IN_DEEP.get());
                output.accept(mio_icif_blocks.BLOCK_NIOBIUM.get());
                output.accept(mio_icif_blocks.BLOCK_ORE_TITANIUM.get());
                output.accept(mio_icif_blocks.BLOCK_ORE_TITANIUM_IN_DEEP.get());
                output.accept(mio_icif_blocks.BLOCK_TITANIUM.get());
                output.accept(mio_icif_blocks.BLOCK_RAW_TIN.get());
                output.accept(mio_icif_blocks.BLOCK_RAW_LEAD.get());
                output.accept(mio_icif_blocks.BLOCK_RAW_URAN.get());
                output.accept(mio_icif_blocks.BLOCK_RAW_NIOBIUM.get());
                output.accept(mio_icif_blocks.BLOCK_RAW_TITANIUM.get());
                output.accept(mio_icif_resources.ZIP_COAL_BLOCK.get());

                // ============================================================
                //  4. PROCESSED MATERIALS - BRONZE
                // ============================================================
                output.accept(mio_icif_resources.INGOT_BRONZE.get());
                output.accept(mio_icif_resources.BRONZE_DUST.get());
                output.accept(mio_icif_resources.BRONZE_DUST_SMALL.get());
                output.accept(mio_icif_resources.BRONZE_PLATE.get());
                output.accept(mio_icif_resources.BRONZE_DENSEPLATE.get());
                output.accept(mio_icif_resources.BRONZE_CASING.get());

                // PROCESSED MATERIALS - TIN
                output.accept(mio_icif_resources.INGOT_TIN.get());
                output.accept(mio_icif_resources.TIN_DUST.get());
                output.accept(mio_icif_resources.TIN_DUST_SMALL.get());
                output.accept(mio_icif_resources.TIN_PLATE.get());
                output.accept(mio_icif_resources.TIN_DENSEPLATE.get());
                output.accept(mio_icif_resources.TIN_CASING.get());
                output.accept(mio_icif_resources.TIN_ORE_CRUSHED.get());
                output.accept(mio_icif_resources.TIN_ORE_CRUSHED_PURIFIED.get());
                output.accept(mio_icif_resources.RAW_TIN.get());
                output.accept(mio_icif_resources.TIN_NUGGET.get());

                // PROCESSED MATERIALS - COPPER
                output.accept(mio_icif_resources.COPPER_DUST.get());
                output.accept(mio_icif_resources.COPPER_DUST_SMALL.get());
                output.accept(mio_icif_resources.COPPER_PLATE.get());
                output.accept(mio_icif_resources.COPPER_DENSEPLATE.get());
                output.accept(mio_icif_resources.COPPER_CASING.get());
                output.accept(mio_icif_resources.COPPER_ORE_CRUSHED.get());
                output.accept(mio_icif_resources.COPPER_ORE_CRUSHED_PURIFIED.get());
                output.accept(mio_icif_resources.COPPER_BOILER.get());
                output.accept(mio_icif_resources.COPPER_NUGGET.get());

                // PROCESSED MATERIALS - IRON
                output.accept(mio_icif_resources.IRON_DUST.get());
                output.accept(mio_icif_resources.IRON_DUST_SMALL.get());
                output.accept(mio_icif_resources.IRON_PLATE.get());
                output.accept(mio_icif_resources.IRON_DENSEPLATE.get());
                output.accept(mio_icif_resources.IRON_CASING.get());
                output.accept(mio_icif_resources.IRON_ORE_CRUSHED.get());
                output.accept(mio_icif_resources.IRON_ORE_CRUSHED_PURIFIED.get());
                output.accept(mio_icif_resources.IRON_CUT_BLADE.get());
                output.accept(mio_icif_resources.IRON_ROTOR_BLADE.get());
                output.accept(mio_icif_resources.IRON_SHAFT.get());

                // PROCESSED MATERIALS - GOLD
                output.accept(mio_icif_resources.GOLDEN_DUST.get());
                output.accept(mio_icif_resources.GOLDEN_DUST_SMALL.get());
                output.accept(mio_icif_resources.GOLDEN_PLATE.get());
                output.accept(mio_icif_resources.GOLDEN_DENSEPLATE.get());
                output.accept(mio_icif_resources.GOLDEN_CASING.get());
                output.accept(mio_icif_resources.GOLD_ORE_CRUSHED.get());
                output.accept(mio_icif_resources.GOLD_ORE_CRUSHED_PURIFIED.get());

                // PROCESSED MATERIALS - LEAD
                output.accept(mio_icif_resources.LEAD_INGOT.get());
                output.accept(mio_icif_resources.LEAD_DUST.get());
                output.accept(mio_icif_resources.LEAD_DUST_SMALL.get());
                output.accept(mio_icif_resources.LEAD_PLATE.get());
                output.accept(mio_icif_resources.LEAD_DENSEPLATE.get());
                output.accept(mio_icif_resources.LEAD_CASING.get());
                output.accept(mio_icif_resources.LEAD_ORE_CRUSHED.get());
                output.accept(mio_icif_resources.LEAD_ORE_CRUSHED_PURIFIED.get());
                output.accept(mio_icif_resources.RAW_LEAD.get());
                output.accept(mio_icif_resources.LEAD_NUGGET.get());

                // PROCESSED MATERIALS - NIOBIUM
                output.accept(mio_icif_resources.NIOBIUM_INGOT.get());
                output.accept(mio_icif_resources.NIOBIUM_DUST.get());
                output.accept(mio_icif_resources.NIOBIUM_DUST_SMALL.get());
                output.accept(mio_icif_resources.NIOBIUM_PLATE.get());
                output.accept(mio_icif_resources.NIOBIUM_DENSEPLATE.get());
                output.accept(mio_icif_resources.NIOBIUM_CASING.get());
                output.accept(mio_icif_resources.NIOBIUM_ORE_CRUSHED.get());
                output.accept(mio_icif_resources.NIOBIUM_ORE_CRUSHED_PURIFIED.get());
                output.accept(mio_icif_resources.RAW_NIOBIUM.get());

                // PROCESSED MATERIALS - TITANIUM
                output.accept(mio_icif_resources.TITANIUM_INGOT.get());
                output.accept(mio_icif_resources.TITANIUM_DUST.get());
                output.accept(mio_icif_resources.TITANIUM_DUST_SMALL.get());
                output.accept(mio_icif_resources.TITANIUM_PLATE.get());
                output.accept(mio_icif_resources.TITANIUM_DENSEPLATE.get());
                output.accept(mio_icif_resources.TITANIUM_CASING.get());
                output.accept(mio_icif_resources.TITANIUM_ORE_CRUSHED.get());
                output.accept(mio_icif_resources.TITANIUM_ORE_CRUSHED_PURIFIED.get());
                output.accept(mio_icif_resources.RAW_TITANIUM.get());
                output.accept(mio_icif_resources.TITANIUM_NUGGET.get());

                // PROCESSED MATERIALS - NIOBIUM-TITANIUM ALLOY
                output.accept(mio_icif_resources.NIOBIUM_TITANIUM_INGOT.get());
                output.accept(mio_icif_resources.NIOBIUM_TITANIUM_DUST.get());
                output.accept(mio_icif_resources.NIOBIUM_TITANIUM_PLATE.get());

                // PROCESSED MATERIALS - THORIUM
                output.accept(mio_icif_resources.THORIUM_DUST.get());
                output.accept(mio_icif_resources.THORIUM_DUST_SMALL.get());
                output.accept(mio_icif_resources.THORIUM_SCRAP.get());

                // ============================================================
                //  METS (MoreElectricTools) 移植材料
                // ============================================================
                output.accept(mio_icif_resources.METS_TITANIUM_SHAFT.get());

                output.accept(mio_icif_normal.METS_SUPER_CIRCUIT.get());
                output.accept(mio_icif_normal.METS_LIVING_CIRCUIT.get());
                output.accept(mio_icif_normal.METS_LENS.get());
                output.accept(mio_icif_normal.METS_DIAMOND_LENS.get());
                output.accept(mio_icif_resources.METS_SUPER_IRIDIUM_ALLOY.get());
                output.accept(mio_icif_resources.METS_SUPER_IRIDIUM_COMPRESS_PLATE.get());
                output.accept(mio_icif_resources.METS_PLANT_EXTRACT.get());
                output.accept(mio_icif_resources.METS_NANO_LIVING_METAL.get());
                output.accept(mio_icif_resources.METS_NEUTRON_PLATE.get());
                output.accept(mio_icif_normal.METS_FIELD_GENERATOR.get());

                // PROCESSED MATERIALS - SILVER
                output.accept(mio_icif_resources.SILVER_INGOT.get());
                output.accept(mio_icif_resources.SILVER_DUST.get());
                output.accept(mio_icif_resources.SILVER_DUST_SMALL.get());
                output.accept(mio_icif_resources.SILVER_CASING.get());
                output.accept(mio_icif_resources.SILVER_ORE_CRUSHED.get());
                output.accept(mio_icif_resources.SILVER_ORE_CRUSHED_PURIFIED.get());
                output.accept(mio_icif_resources.SILVER_NUGGET.get());

                // PROCESSED MATERIALS - LAPIS
                output.accept(mio_icif_resources.LAPI_DUST.get());
                output.accept(mio_icif_resources.LAPI_DUST_SMALL.get());
                output.accept(mio_icif_resources.LAPI_PLATE.get());
                output.accept(mio_icif_resources.LAPI_DENSEPLATE.get());

                // PROCESSED MATERIALS - ADVIRON
                output.accept(mio_icif_resources.ADVIRON_INGOT.get());
                output.accept(mio_icif_resources.ADVIRON_PLATE.get());
                output.accept(mio_icif_resources.ADVIRON_DENSEPLATE.get());
                output.accept(mio_icif_resources.ADVIRON_CASING.get());
                output.accept(mio_icif_resources.ADVIRON_CUTBLADE.get());
                output.accept(mio_icif_resources.ADVIRON_ROTOR_BLADE.get());
                output.accept(mio_icif_resources.ADVIRON_SHAFT.get());

                // PROCESSED MATERIALS - URANIUM / NUCLEAR
                output.accept(mio_icif_resources.URAN.get());
                output.accept(mio_icif_resources.URAN_235.get());
                output.accept(mio_icif_resources.URAN_235_SMALL.get());
                output.accept(mio_icif_resources.URAN_238.get());
                output.accept(mio_icif_resources.URAN_238_SMALL.get());
                output.accept(mio_icif_resources.URAN_ORE_CRUSHED.get());
                output.accept(mio_icif_resources.URAN_ORE_CRUSHED_PURIFIED.get());
                output.accept(mio_icif_resources.RAW_URAN.get());
                output.accept(mio_icif_resources.URAN_PELLET.get());
                output.accept(mio_icif_resources.PLUTONIUM.get());
                output.accept(mio_icif_resources.PLUTONIUM_SMALL.get());
                output.accept(mio_icif_resources.MOX.get());
                output.accept(mio_icif_resources.PELLET.get());
                output.accept(mio_icif_resources.RTG_PELLET.get());
                output.accept(mio_icif_resources.LITHIUM_DUST.get());
                output.accept(mio_icif_resources.LITHIUM_DUST_SMALL.get());

                // PROCESSED MATERIALS - ALLOY
                output.accept(mio_icif_resources.ALLOY_INGOT.get());
                output.accept(mio_icif_resources.ALLOY_PLATE.get());
                output.accept(mio_icif_resources.DCP_PLATE.get());

                // PROCESSED MATERIALS - CARBON
                output.accept(mio_icif_resources.CARBON_PLATE.get());
                output.accept(mio_icif_resources.CARBON_ROTORBLADE.get());
                output.accept(mio_icif_resources.CF_DUST.get());

                // PROCESSED MATERIALS - OBSIDIAN
                output.accept(mio_icif_resources.OBSIDIAN_DUST.get());
                output.accept(mio_icif_resources.OBSIDIAN_DUST_SMALL.get());
                output.accept(mio_icif_resources.OBSIDIAN_PLATE.get());
                output.accept(mio_icif_resources.OBSIDIAN_DENSEPLATE.get());

                // PROCESSED MATERIALS - IRIDIUM / DIAMOND
                output.accept(mio_icif_resources.IRIDIUM.get());
                output.accept(mio_icif_resources.IRIDIUM_INGOT.get());
                output.accept(mio_icif_resources.IRIDIUM_PLATE.get());
                output.accept(mio_icif_resources.SHARD_IRIDIUM.get());
                output.accept(mio_icif_resources.DIAMOND_DUST.get());
                output.accept(mio_icif_resources.DIAMOND_CUT_BLADE.get());
                output.accept(mio_icif_resources.ENERGIUM_DUST.get());

                // PROCESSED MATERIALS - ASP (Advanced Solar Panels)
                output.accept(mio_icif_resources.SUNNARIUM.get());
                output.accept(mio_icif_resources.SUNNARIUM_SMALL.get());
                output.accept(mio_icif_resources.SUNNARIUM_ALLOY.get());
                output.accept(mio_icif_resources.ENRICHED_SUNNARIUM.get());
                output.accept(mio_icif_resources.ENRICHED_SUNNARIUM_ALLOY.get());
                output.accept(mio_icif_resources.URANIUM_INGOT.get());
                output.accept(mio_icif_resources.IRRADIANT_URANIUM_INGOT.get());
                output.accept(mio_icif_resources.IRIDIUMIRON_PLATE.get());
                output.accept(mio_icif_resources.IRIDIUMIRON_REINFORCED_PLATE.get());
                output.accept(mio_icif_resources.IRRADIANT_REINFORCED_PLATE.get());
                output.accept(mio_icif_resources.QUANTUM_CORE.get());
                output.accept(mio_icif_resources.MOLECULAR_TRANSFORMER_CORE.get());

                // PROCESSED MATERIALS - MISCELLANEOUS DUSTS
                output.accept(mio_icif_resources.COAL_DUST.get());
                output.accept(mio_icif_resources.COAL_BALL.get());
                output.accept(mio_icif_resources.COAL_CHUNK.get());
                output.accept(mio_icif_resources.COAL_FABRE.get());
                output.accept(mio_icif_resources.COAL_MESH.get());
                output.accept(mio_icif_resources.CLAY_DUST.get());
                output.accept(mio_icif_resources.GRIN_DUST.get());
                output.accept(mio_icif_resources.STONE_DUST.get());
                output.accept(mio_icif_resources.ASH.get());
                output.accept(mio_icif_resources.SILICONDIOXIDE_DUST.get());
                output.accept(mio_icif_resources.SULFUR_DUST.get());
                output.accept(mio_icif_resources.SULFUR_DUST_SMALL.get());
                output.accept(mio_icif_resources.NETHERRACK_DUST.get());
                // 新增粉末物品
                output.accept(mio_icif_resources.FLINT_DUST.get());
                output.accept(mio_icif_resources.CHARCOAL_DUST.get());
                output.accept(mio_icif_resources.NITRE_DUST.get());

                // ============================================================
                //  5. COMPONENTS & PARTS
                // ============================================================
                // Rotors
                output.accept(mio_icif_items.ROTOR_WOOD.get());
                output.accept(mio_icif_items.ROTOR_IRON.get());
                output.accept(mio_icif_items.ROTOR_CARBON.get());
                output.accept(mio_icif_items.ROTOR_ADVIRON.get());
                output.accept(mio_icif_items.ROTOR_TITANIUM_IRON.get());
                output.accept(mio_icif_items.ROTOR_SUPER_IRIDIUM.get());
                output.accept(mio_icif_resources.WOOD_ROTOR_BLADE.get());
                output.accept(mio_icif_resources.TITANIUM_IRON_ROTOR_BLADE.get());
                output.accept(mio_icif_resources.SUPER_IRIDIUM_ROTOR_BLADE.get());

                // Circuits
                output.accept(mio_icif_normal.CIRCUIT.get());
                output.accept(mio_icif_normal.ADCIRCUIT.get());

                // Mechanical Parts
                output.accept(mio_icif_resources.MOTOR.get());
                output.accept(mio_icif_resources.COIL.get());
                output.accept(mio_icif_resources.HEATCONDUCTOR.get());
                output.accept(mio_icif_resources.HARZ.get());

                // Crystals
                output.accept(mio_icif_normal.CRYSTAL_LEV0.get());
                {
                    ItemStack fullCrystalLev4 = new ItemStack(mio_icif_normal.CRYSTAL_LEV0.get());
                    if (fullCrystalLev4.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat bat) bat.setEnergy(fullCrystalLev4, bat.getMaxEnergy());
                    output.accept(fullCrystalLev4);
                }
                output.accept(mio_icif_normal.LAPOTRON_CRYSTAL_LEV0.get());
                {
                    ItemStack fullLapotronCrystalLev4 = new ItemStack(mio_icif_normal.LAPOTRON_CRYSTAL_LEV0.get());
                    if (fullLapotronCrystalLev4.getItem() instanceof com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat bat) bat.setEnergy(fullLapotronCrystalLev4, bat.getMaxEnergy());
                    output.accept(fullLapotronCrystalLev4);
                }
                output.accept(mio_icif_resources.CRYSTAL_MEMORY.get());
                output.accept(mio_icif_resources.CRYSTAL_MEMORY_RAW.get());

                // ============================================================
                //  6. BATTERIES & POWER ITEMS
                // ============================================================
                // Re-Battery - 空电状态
                { ItemStack emptyBat = new ItemStack(mio_icif_normal.BAT_LEV0.get()); ((com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat)emptyBat.getItem()).setEnergy(emptyBat, 0); output.accept(emptyBat); }
                // Re-Battery - 满电状态
                { ItemStack fullBat = new ItemStack(mio_icif_normal.BAT_LEV0.get()); ((com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat)fullBat.getItem()).setEnergy(fullBat, 10000); output.accept(fullBat); }

                // Advanced Battery - 空电状态
                { ItemStack emptyAdvBat = new ItemStack(mio_icif_normal.ADVBAT_LEV0.get()); ((com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat)emptyAdvBat.getItem()).setEnergy(emptyAdvBat, 0); output.accept(emptyAdvBat); }
                // Advanced Battery - 满电状态
                { ItemStack fullAdvBat = new ItemStack(mio_icif_normal.ADVBAT_LEV0.get()); ((com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat)fullAdvBat.getItem()).setEnergy(fullAdvBat, 100000); output.accept(fullAdvBat); }

                // Charged Battery - 空电状态
                { ItemStack emptyChargeBat = new ItemStack(mio_icif_normal.CHARGEBAT_LEV0.get()); emptyChargeBat.setDamageValue(40000); output.accept(emptyChargeBat); }
                // Charged Battery - 满电状态
                { ItemStack fullChargeBat = new ItemStack(mio_icif_normal.CHARGEBAT_LEV0.get()); fullChargeBat.setDamageValue(0); output.accept(fullChargeBat); }

                // Advanced Charged Battery - 空电状态
                { ItemStack emptyAdvChargeBat = new ItemStack(mio_icif_normal.ADVCHARGEBAT_0.get()); emptyAdvChargeBat.setDamageValue(400000); output.accept(emptyAdvChargeBat); }
                // Advanced Charged Battery - 满电状态
                { ItemStack fullAdvChargeBat = new ItemStack(mio_icif_normal.ADVCHARGEBAT_0.get()); fullAdvChargeBat.setDamageValue(0); output.accept(fullAdvChargeBat); }

                // Crystal Battery - 空电状态
                { ItemStack emptyCrystalChargeBat = new ItemStack(mio_icif_normal.CRYSTAL_CHARGEBAT_LEV0.get()); emptyCrystalChargeBat.setDamageValue(4000000); output.accept(emptyCrystalChargeBat); }
                // Crystal Battery - 满电状态
                { ItemStack fullCrystalChargeBat = new ItemStack(mio_icif_normal.CRYSTAL_CHARGEBAT_LEV0.get()); fullCrystalChargeBat.setDamageValue(0); output.accept(fullCrystalChargeBat); }

                // LapCrystal Battery - 空电状态
                { ItemStack emptyLamaCrystalChargeBat = new ItemStack(mio_icif_normal.LAMACRYSTAL_CHARGEBAT_LEV0.get()); emptyLamaCrystalChargeBat.setDamageValue(40000000); output.accept(emptyLamaCrystalChargeBat); }
                // LapCrystal Battery - 满电状态
                { ItemStack fullLamaCrystalChargeBat = new ItemStack(mio_icif_normal.LAMACRYSTAL_CHARGEBAT_LEV0.get()); fullLamaCrystalChargeBat.setDamageValue(0); output.accept(fullLamaCrystalChargeBat); }

                // METS 超级兰波顿水晶 - 空电状态
                { ItemStack emptySuperLapotron = new ItemStack(mio_icif_normal.SUPER_LAPOTRON_CRYSTAL.get()); emptySuperLapotron.setDamageValue(100000000); output.accept(emptySuperLapotron); }
                // METS 超级兰波顿水晶 - 满电状态
                { ItemStack fullSuperLapotron = new ItemStack(mio_icif_normal.SUPER_LAPOTRON_CRYSTAL.get()); fullSuperLapotron.setDamageValue(0); output.accept(fullSuperLapotron); }

                // METS 充电超级兰波顿水晶 - 空电状态
                { ItemStack emptyChargingSuperLapotron = new ItemStack(mio_icif_normal.CHARGING_SUPER_LAPOTRON_CRYSTAL.get()); emptyChargingSuperLapotron.setDamageValue(400000000); output.accept(emptyChargingSuperLapotron); }
                // METS 充电超级兰波顿水晶 - 满电状态
                { ItemStack fullChargingSuperLapotron = new ItemStack(mio_icif_normal.CHARGING_SUPER_LAPOTRON_CRYSTAL.get()); fullChargingSuperLapotron.setDamageValue(0); output.accept(fullChargingSuperLapotron); }



                // METS 锂电池 - 空电状态
                { ItemStack emptyLithiumBat = new ItemStack(mio_icif_normal.LITHIUM_BATTERY.get()); ((com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat)emptyLithiumBat.getItem()).setEnergy(emptyLithiumBat, 0); output.accept(emptyLithiumBat); }
                // METS 锂电池 - 满电状态
                { ItemStack fullLithiumBat = new ItemStack(mio_icif_normal.LITHIUM_BATTERY.get()); ((com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat)fullLithiumBat.getItem()).setEnergy(fullLithiumBat, 50000); output.accept(fullLithiumBat); }

                // METS 高级锂电池 - 空电状态
                { ItemStack emptyAdvLithiumBat = new ItemStack(mio_icif_normal.ADV_LITHIUM_BATTERY.get()); ((com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat)emptyAdvLithiumBat.getItem()).setEnergy(emptyAdvLithiumBat, 0); output.accept(emptyAdvLithiumBat); }
                // METS 高级锂电池 - 满电状态
                { ItemStack fullAdvLithiumBat = new ItemStack(mio_icif_normal.ADV_LITHIUM_BATTERY.get()); ((com.singularity_iteration.mio_icif.Items.Normal.mio_icif_bat)fullAdvLithiumBat.getItem()).setEnergy(fullAdvLithiumBat, 200000); output.accept(fullAdvLithiumBat); }

                // METS 钍电池 - 空电状态
                { ItemStack emptyThoriumBat = new ItemStack(mio_icif_normal.THORIUM_BATTERY.get()); emptyThoriumBat.setDamageValue(Integer.MAX_VALUE); output.accept(emptyThoriumBat); }
                // METS 钍电池 - 满电状态
                { ItemStack fullThoriumBat = new ItemStack(mio_icif_normal.THORIUM_BATTERY.get()); fullThoriumBat.setDamageValue(0); output.accept(fullThoriumBat); }

                // Energy Crystal
                output.accept(mio_icif_normal.ALE.get());

                // Fuel Items
                output.accept(mio_icif_resources.FUEL_PLANT_BALL.get());

                // ============================================================
                //  7. CROP SEEDS
                // ============================================================
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "wheat", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "carrots", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "potato", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "cocoa", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "coffee", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "hops", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "melon", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "pumpkin", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "dandelion", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "rose", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "tulip", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "cyazint", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "ferru", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "cyprium", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "stagnium", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "plumbiscus", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "redwheat", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "netherWart", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "redMushroom", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "brownMushroom", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "reed", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "stickreed", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "aurelia", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "shining", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "blackthorn", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "venomilia", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "eatingplant", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "titanium", 0, 0, 0));
                output.accept(CropSeedItem.createSeedStack(mio_icif_normal.CROP_SEED.get(), "mio_icif", "uranium", 0, 0, 0));

                // 富集作物种子 (原版耕地种植)
                output.accept(mio_icif_normal.SEED_IRON_RICH.get());
                output.accept(mio_icif_normal.SEED_COPPER_RICH.get());
                output.accept(mio_icif_normal.SEED_TIN_RICH.get());
                output.accept(mio_icif_normal.SEED_TITANIUM_RICH.get());
                output.accept(mio_icif_normal.SEED_LEAD_RICH.get());
                output.accept(mio_icif_normal.SEED_URANIUM_RICH.get());

                // ============================================================
                //  8. MISCELLANEOUS ITEMS
                // ============================================================
                // Rubber
                output.accept(mio_icif_resources.RUBBER.get());

                // Fertilizer & Crops
                output.accept(mio_icif_resources.FERTILIZER.get());
                output.accept(mio_icif_resources.TERRA_WART.get());
                output.accept(mio_icif_resources.COFFEE_BEAN.get());
                output.accept(mio_icif_resources.COFFEE_DUST.get());
                output.accept(mio_icif_normal.WEEDEX.get());
                output.accept(mio_icif_normal.WEEDING_TROWEL.get());

                // Food & Drinks
                output.accept(mio_icif_normal.COFFEE_0.get());
                output.accept(mio_icif_normal.COFFEE_1.get());
                output.accept(mio_icif_normal.COFFEE_2.get());
                output.accept(mio_icif_normal.BEER.get());
                output.accept(mio_icif_normal.BREW.get());
                output.accept(mio_icif_normal.RUM.get());
                output.accept(mio_icif_normal.HOPS.get());

                // Cans & Containers
                output.accept(mio_icif_normal.CAN.get());
                output.accept(mio_icif_normal.TIN_EMPTY_CAN.get());
                output.accept(mio_icif_normal.TIN_FILLED_CAN.get());
                output.accept(mio_icif_normal.EMPTY_MUG.get());
                output.accept(mio_icif_normal.RED_MUG.get());

                // Boats
                output.accept(mio_icif_normal.ENTITY_COAL_BOAT.get());
                output.accept(mio_icif_normal.ENTITY_ELECTRIC_BOAT.get());
                output.accept(mio_icif_normal.ENTITY_RUBBER_BOAT.get());
                output.accept(mio_icif_normal.BROKEN_RUBBER_BOAT.get());

                // Explosives
                output.accept(mio_icif_normal.DYNAMITE.get());
                output.accept(mio_icif_normal.STICKY_DYNAMITE.get());

                // Special Items
                output.accept(mio_icif_normal.REMOTE.get());
                output.accept(mio_icif_normal.TOOLBOX.get());
                output.accept(mio_icif_normal.TOOLBOX_X.get());
                output.accept(mio_icif_normal.CONTAIN_MENT_BOX.get());
                output.accept(mio_icif_normal.CROP_ANALYZER.get());
                { ItemStack fullCropAnalyzer = new ItemStack(mio_icif_normal.CROP_ANALYZER.get()); fullCropAnalyzer.setDamageValue(0); output.accept(fullCropAnalyzer); }
                output.accept(mio_icif_normal.INTELLIGENCE.get());
                output.accept(mio_icif_normal.MODIFY.get());
                output.accept(mio_icif_normal.YOUNGSTER.get());
                output.accept(mio_icif_normal.FREQ.get());
                output.accept(mio_icif_normal.COIN.get());
                output.accept(mio_icif_normal.STUFF.get());

                // Industrial Diamond
                output.accept(mio_icif_normal.INDUSTRIAL_DIAMOND.get());

                // Scrap
                output.accept(mio_icif_normal.SCRAP.get());
                output.accept(mio_icif_normal.SCRAPBOX.get());
                output.accept(com.singularity_iteration.mio_icif.Items.Resource.mio_icif_resources.THORIUM_SCRAP.get());
                output.accept(mio_icif_normal.SLAG.get());

                // Lathe Items
                output.accept(mio_icif_normal.IRON_TURNING_BLANKS.get());
                output.accept(mio_icif_normal.WOODEN_TURNING_BLANKS.get());
                output.accept(com.singularity_iteration.mio_icif.Items.Lathe.mio_icif_lathe_items.IRON_LATHING_TOOL.get());
                output.accept(com.singularity_iteration.mio_icif.Items.Lathe.mio_icif_lathe_items.DIAMOND_LATHING_TOOL.get());

                // Steam Turbine
                output.accept(mio_icif_normal.STEAM_TURBIN.get());
                output.accept(mio_icif_normal.STEAM_TURBINE_BLADE.get());

                // Fuel Rods & Cells
                output.accept(mio_icif_normal.FUEL_ROD.get());
                output.accept(mio_icif_normal.MOX_QUAD_DEPLETE.get());
                output.accept(mio_icif_normal.BIOCHAFF.get());
                output.accept(mio_icif_normal.DRAGONBLOOD.get());

                // Painters
                output.accept(mio_icif_normal.PAINTER.get());
                output.accept(mio_icif_normal.PAINTER_WHITE.get());
                output.accept(mio_icif_normal.PAINTER_ORANGE.get());
                output.accept(mio_icif_normal.PAINTER_MAGENTA.get());
                output.accept(mio_icif_normal.PAINTER_LIGHT_BLUE.get());
                output.accept(mio_icif_normal.PAINTER_YELLOW.get());
                output.accept(mio_icif_normal.PAINTER_LIME.get());
                output.accept(mio_icif_normal.PAINTER_PINK.get());
                output.accept(mio_icif_normal.PAINTER_GRAY.get());
                output.accept(mio_icif_normal.PAINTER_LIGHT_GRAY.get());
                output.accept(mio_icif_normal.PAINTER_CYAN.get());
                output.accept(mio_icif_normal.PAINTER_PURPLE.get());
                output.accept(mio_icif_normal.PAINTER_BLUE.get());
                output.accept(mio_icif_normal.PAINTER_BROWN.get());
                output.accept(mio_icif_normal.PAINTER_GREEN.get());
                output.accept(mio_icif_normal.PAINTER_RED.get());
                output.accept(mio_icif_normal.PAINTER_BLACK.get());

                // Construction Tools
                // CF喷枪 - 空状态
                output.accept(mio_icif_normal.CF_SPRAYER.get());
                // CF喷枪 - 满装载状态
                { ItemStack fullCfSprayer = new ItemStack(mio_icif_normal.CF_SPRAYER.get()); fullCfSprayer.setDamageValue(0); output.accept(fullCfSprayer); }
                output.accept(mio_icif_normal.OBSCURATOR.get());

                // ============================================================
                //  9. REACTOR COMPONENTS
                // ============================================================
                // Coolant
                output.accept(mio_icif_reactors.COLLANT_SIMPLE.get());
                output.accept(mio_icif_reactors.COLLANT_TRIPLE.get());
                output.accept(mio_icif_reactors.COOLANT_SIX.get());

                // Condensators
                output.accept(mio_icif_reactors.CONDENSATOR.get());
                output.accept(mio_icif_reactors.CONDENSATOR_LAP.get());

                // Heat Vents
                output.accept(mio_icif_reactors.VENT.get());
                output.accept(mio_icif_reactors.VENT_CORE.get());
                output.accept(mio_icif_reactors.VENT_SPREAD.get());
                output.accept(mio_icif_reactors.OVERCLOCKED_HEAT_VENT.get());
                output.accept(mio_icif_reactors.DIAMOND_VENT.get());
                output.accept(mio_icif_reactors.IRIDIUM_HEAT_VENT.get());
                output.accept(mio_icif_reactors.IRIDIUM_OVERCLOCKED_HEAT_VENT.get());

                // Heat Switches
                output.accept(mio_icif_reactors.HEAT_SWITCH.get());
                output.accept(mio_icif_reactors.HEAT_SWITCH_CORE.get());
                output.accept(mio_icif_reactors.HEAT_SWITCH_SPREAD.get());
                output.accept(mio_icif_reactors.DIAMOND_HEAT_SWITCH.get());

                // Heat Plates & Exchangers
                output.accept(mio_icif_reactors.HEAT_PLATE.get());

                // Protection Plates
                output.accept(mio_icif_reactors.PLATE.get());
                output.accept(mio_icif_reactors.EXPLOSIVE_PLATE.get());

                // Reflectors
                output.accept(mio_icif_reactors.REFLECTOR.get());
                output.accept(mio_icif_reactors.THICK_REFLECTOR.get());

                // Uranium Fuel Rods
                output.accept(mio_icif_reactors.URANIUM_SIMPLE.get());
                output.accept(mio_icif_reactors.URANIUM_SIMPLE_DEPLETED.get());
                output.accept(mio_icif_reactors.URANIUM_DUAL.get());
                output.accept(mio_icif_reactors.URANIUM_DUAL_DEPLETED.get());
                output.accept(mio_icif_reactors.URANIUM_QUAD.get());
                output.accept(mio_icif_reactors.URANIUM_QUAD_DEPLETED.get());

                // MOX Fuel Rods
                output.accept(mio_icif_reactors.MOX_SIMPLE.get());
                output.accept(mio_icif_reactors.MOX_SIMPLE_DEPLETED.get());
                output.accept(mio_icif_reactors.MOX_DUAL.get());
                output.accept(mio_icif_reactors.MOX_DUAL_DEPLETED.get());
                output.accept(mio_icif_reactors.MOX_QUAD.get());
                output.accept(mio_icif_reactors.MOX_QUAD_DEPLETED.get());

                // ============================================================
                //  10. FLUID BUCKETS
                // ============================================================
                output.accept(mio_icif_fluids.BIOGAS_BUCKET.get());
                output.accept(mio_icif_fluids.BIOMASS_BUCKET.get());
                output.accept(mio_icif_fluids.CONSTRUCTIONFOAM_BUCKET.get());
                output.accept(mio_icif_fluids.COOLANT_BUCKET.get());
                output.accept(mio_icif_fluids.DISTILLEDWATER_BUCKET.get());
                output.accept(mio_icif_fluids.HOTCOOLANT_BUCKET.get());
                output.accept(mio_icif_fluids.HOTWATER_BUCKET.get());
                output.accept(mio_icif_fluids.PAHOEHOELAVA_BUCKET.get());
                output.accept(mio_icif_fluids.STEAM_BUCKET.get());
                output.accept(mio_icif_fluids.SUPERHEATEDSTEAM_BUCKET.get());
                output.accept(mio_icif_fluids.UUMATTER_BUCKET.get());
                output.accept(mio_icif_fluids.CRUDEOIL_BUCKET.get());
                output.accept(mio_icif_fluids.DIESELOIL_BUCKET.get());

                // ============================================================
                //  11. CELLS (Dynamic)
                // ============================================================
                output.accept(mio_icif_cells.CELL_EMPTY.get());
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(Fluids.WATER, 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(Fluids.LAVA, 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.BIOGAS.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.HOTWATER.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.BIOMASS.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.CONSTRUCTIONFOAM.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.COOLANT.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.DISTILLEDWATER.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.HOTCOOLANT.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.PAHOEHOELAVA.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.STEAM.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.SUPERHEATEDSTEAM.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.UUMATTER.get(), 1000));
                  output.accept(cell); }
                { ItemStack cell = new ItemStack(mio_icif_cells.CELL_EMPTY.get());
                  ((mio_icif_dynamic_cell) cell.getItem()).writeFluidToNBT(cell, new FluidStack(mio_icif_fluids.AIR.get(), 1000));
                  output.accept(cell); }

                // ============================================================
                //  12. ENVIRONMENT TEMPLATES
                // ============================================================
                output.accept(mio_icif_env_templates.EVT_EMPTY.get());
                output.accept(mio_icif_env_templates.EVT_CULTIVATION.get());
                output.accept(mio_icif_env_templates.EVT_DESERT.get());
                output.accept(mio_icif_env_templates.EVT_IRRIGATION.get());
                output.accept(mio_icif_env_templates.EVT_CHILLING.get());
                output.accept(mio_icif_env_templates.EVT_FLATIFICATION.get());
                output.accept(mio_icif_env_templates.EVT_MUSHROOM.get());

                // ============================================================
                //  13. UPGRADE MODULES
                // ============================================================
                output.accept(mio_icif_upgrades.OVERCLOCKER_UPGRADE.get());
                output.accept(mio_icif_upgrades.ENERGY_STORAGE_UPGRADE.get());
                output.accept(mio_icif_upgrades.TRANSFORMER_UPGRADE.get());
                output.accept(mio_icif_upgrades.EJECTOR_UPGRADE.get());
                output.accept(mio_icif_upgrades.PULLING_UPGRADE.get());
                output.accept(mio_icif_upgrades.FLUID_EJECTOR_UPGRADE.get());
                output.accept(mio_icif_upgrades.FLUID_PULLING_UPGRADE.get());
                output.accept(mio_icif_upgrades.REDSTONE_INVERTER_UPGRADE.get());

                // ============================================================
                //  14. BLOCKS - WIRING & CABLES
                // ============================================================
                // Bare Wires (by voltage tier)
                output.accept(mio_icif_blocks.WIRE_LV.get());
                output.accept(mio_icif_blocks.WIRE_MV.get());
                output.accept(mio_icif_blocks.WIRE_HV.get());
                output.accept(mio_icif_blocks.WIRE_EV.get());
                output.accept(mio_icif_blocks.WIRE_IV.get());
                // Insulated Wires (by voltage tier)
                output.accept(mio_icif_blocks.WIRE_ISOLATION_LV.get());
                output.accept(mio_icif_blocks.WIRE_ISOLATION_MV.get());
                output.accept(mio_icif_blocks.WIRE_ISOLATION_HV.get());
                output.accept(mio_icif_blocks.WIRE_ISOLATION_EV.get());
                // Superconducting Cable (METS)
                output.accept(mio_icif_blocks.SUPERCONDUCTING_CABLE.get());
                // EU Detector Cable & EU Splitter Cable
                output.accept(mio_icif_blocks.WIRE_DETECTOR.get());
                output.accept(mio_icif_blocks.WIRE_SPLITTER.get());
                // Transformers
                output.accept(mio_icif_blocks.TRANSFORMER_LV_MV.get());
                output.accept(mio_icif_blocks.TRANSFORMER_MV_HV.get());
                output.accept(mio_icif_blocks.TRANSFORMER_HV_EV.get());
                output.accept(mio_icif_blocks.TRANSFORMER_EV_SC.get());
                output.accept(mio_icif_blocks.TRANSFORMER_IV_LUV.get());
                output.accept(mio_icif_blocks.TRANSFORMER_LUV_ZPMV.get());
                // Wireless Power Transmission Node
                output.accept(mio_icif_blocks.WIRELESS_POWER_TRANSMISSION_NODE.get());

                // ============================================================
                //  15. BLOCKS - ENERGY STORAGE
                // ============================================================
                // BatBox (40000 EU)
                output.accept(mio_icif_blocks.BAT_BOX.get());
                {
                    ItemStack fullBatBox = new ItemStack(mio_icif_blocks.BAT_BOX.get());
                    fullBatBox.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(40000L, "mio_icif:batbox")));
                    output.accept(fullBatBox);
                }
                output.accept(mio_icif_blocks.BATBOX_CHARGER.get());
                {
                    ItemStack fullBatboxCharger = new ItemStack(mio_icif_blocks.BATBOX_CHARGER.get());
                    fullBatboxCharger.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(40000L, "mio_icif:batbox_charger")));
                    output.accept(fullBatboxCharger);
                }
                // CESU (300000 EU)
                output.accept(mio_icif_blocks.CESU.get());
                {
                    ItemStack fullCesu = new ItemStack(mio_icif_blocks.CESU.get());
                    fullCesu.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(300000L, "mio_icif:cesu")));
                    output.accept(fullCesu);
                }
                output.accept(mio_icif_blocks.CESU_CHARGER.get());
                {
                    ItemStack fullCesuCharger = new ItemStack(mio_icif_blocks.CESU_CHARGER.get());
                    fullCesuCharger.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(300000L, "mio_icif:cesu_charger")));
                    output.accept(fullCesuCharger);
                }
                // MFE (4000000 EU)
                output.accept(mio_icif_blocks.MFE.get());
                {
                    ItemStack fullMfe = new ItemStack(mio_icif_blocks.MFE.get());
                    fullMfe.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(4000000L, "mio_icif:mfe")));
                    output.accept(fullMfe);
                }
                output.accept(mio_icif_blocks.MFE_CHARGER.get());
                {
                    ItemStack fullMfeCharger = new ItemStack(mio_icif_blocks.MFE_CHARGER.get());
                    fullMfeCharger.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(4000000L, "mio_icif:mfe_charger")));
                    output.accept(fullMfeCharger);
                }
                // MFSU (40000000 EU)
                output.accept(mio_icif_blocks.MFSU.get());
                {
                    ItemStack fullMfsu = new ItemStack(mio_icif_blocks.MFSU.get());
                    fullMfsu.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(40000000L, "mio_icif:mfsu")));
                    output.accept(fullMfsu);
                }
                output.accept(mio_icif_blocks.MFSU_CHARGER.get());
                {
                    ItemStack fullMfsuCharger = new ItemStack(mio_icif_blocks.MFSU_CHARGER.get());
                    fullMfsuCharger.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(40000000L, "mio_icif:mfsu_charger")));
                    output.accept(fullMfsuCharger);
                }
                // LESU (1000000 EU)
                output.accept(mio_icif_blocks.LESU.get());
                {
                    ItemStack fullLesu = new ItemStack(mio_icif_blocks.LESU.get());
                    fullLesu.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(1000000L, "mio_icif:lesu")));
                    output.accept(fullLesu);
                }
                output.accept(mio_icif_blocks.LESU_CHARGER.get());
                {
                    ItemStack fullLesuCharger = new ItemStack(mio_icif_blocks.LESU_CHARGER.get());
                    fullLesuCharger.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(1000000L, "mio_icif:lesu_charger")));
                    output.accept(fullLesuCharger);
                }
                // EESU (400000000 EU)
                output.accept(mio_icif_blocks.EESU.get());
                {
                    ItemStack fullEesu = new ItemStack(mio_icif_blocks.EESU.get());
                    fullEesu.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(400000000L, "mio_icif:eesu")));
                    output.accept(fullEesu);
                }
                output.accept(mio_icif_blocks.EESU_CHARGER.get());
                {
                    ItemStack fullEesuCharger = new ItemStack(mio_icif_blocks.EESU_CHARGER.get());
                    fullEesuCharger.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(400000000L, "mio_icif:eesu_charger")));
                    output.accept(fullEesuCharger);
                }

                // GESU Core (2147483647 EU)
                output.accept(mio_icif_blocks.GESU_CORE.get());
                {
                    ItemStack fullGesu = new ItemStack(mio_icif_blocks.GESU_CORE.get());
                    fullGesu.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(createEnergyTag(2147483647L, "mio_icif:gesu_core")));
                    output.accept(fullGesu);
                }
                // GESU Input Module IV
                output.accept(mio_icif_blocks.GESU_INPUT_IV.get());
                // GESU Output Module IV
                output.accept(mio_icif_blocks.GESU_OUTPUT_IV.get());
                // GESU Output Module LuV
                output.accept(mio_icif_blocks.GESU_OUTPUT_LUV.get());

                // Oil Rig Series (石油钻机系列)
                output.accept(mio_icif_blocks.OIL_RIG_CORE.get());
                output.accept(mio_icif_blocks.OIL_RIG_BASE.get());
                output.accept(mio_icif_blocks.OIL_RIG_INPUT.get());
                output.accept(mio_icif_blocks.OIL_RIG_OUTPUT.get());
                output.accept(mio_icif_blocks.OIL_RIG_PANEL.get());
                output.accept(mio_icif_blocks.DIMENSION_OIL_RIG_CORE.get());

                // ============================================================
                //  16. BLOCKS - EU GENERATORS
                // ============================================================
                output.accept(mio_icif_blocks.SOLAR_GENERATOR.get());
                output.accept(mio_icif_blocks.ADVANCED_SOLAR_PANEL.get());
                output.accept(mio_icif_blocks.HYBRID_SOLAR_PANEL.get());
                output.accept(mio_icif_blocks.ULTIMATE_HYBRID_SOLAR_PANEL.get());
                output.accept(mio_icif_blocks.QUANTUM_SOLAR_PANEL.get());
                output.accept(mio_icif_blocks.METS_ADVANCED_SOLAR_GENERATOR.get());
                output.accept(mio_icif_blocks.PHOTON_RESONANCE_SOLAR_GENERATOR.get());
                output.accept(mio_icif_blocks.ULTIMATE_PHOTON_RESONANCE_SOLAR_GENERATOR.get());
                // 量子发电机不在创造模式物品栏中显示
                // output.accept(mio_icif_blocks.QUANTUM_GENERATOR.get());
                output.accept(mio_icif_blocks.WIND_GENERATOR.get());
                output.accept(mio_icif_blocks.WATER_GENERATOR.get());
                output.accept(mio_icif_blocks.GEO_GENERATOR.get());
                output.accept(mio_icif_blocks.GEOMAGNETIC_GENERATOR.get());
                output.accept(mio_icif_blocks.GEOMAGNETIC_ANTENNA.get());
                output.accept(mio_icif_blocks.GEOMAGNETIC_PEDESTAL.get());
                output.accept(mio_icif_blocks.THERMAL_GENERATOR.get());
                output.accept(mio_icif_blocks.SEMIFLUID_GENERATOR.get());
                output.accept(mio_icif_blocks.DIESEL_GENERATOR.get());
                output.accept(mio_icif_blocks.RT_GENERATOR.get());
                output.accept(mio_icif_blocks.NUCLEAR_REACTOR_GENERATOR.get());
                output.accept(mio_icif_blocks.UNLIMIT_GENERATOR.get());

                // ============================================================
                //  17. BLOCKS - KINETIC GENERATORS
                // ============================================================
                output.accept(mio_icif_blocks.MANUAL_KINETIC_GENERATOR.get());
                output.accept(mio_icif_blocks.STIRLING_KINETIC_GENERATOR.get());
                output.accept(mio_icif_blocks.WIND_KINETIC_GENERATOR.get());
                output.accept(mio_icif_blocks.WATER_KINETIC_GENERATOR.get());
                output.accept(mio_icif_blocks.KINETIC_GENERATOR_ELC.get());
                output.accept(mio_icif_blocks.KINETIC_GENERATOR.get());
                output.accept(mio_icif_blocks.TURBO_KINETIC_GENERATOR.get());
                output.accept(mio_icif_blocks.TWIN_TURBO_KINETIC_GENERATOR.get());

                // ============================================================
                //  18. BLOCKS - HEAT SOURCES
                // ============================================================
                output.accept(mio_icif_blocks.SOLID_HEAT_GENERATOR.get());
                output.accept(mio_icif_blocks.FLUID_HEAT_GENERATOR.get());
                output.accept(mio_icif_blocks.RT_HEAT_GENERATOR.get());
                output.accept(mio_icif_blocks.HEAT_SOURCE_FLUID.get());
                output.accept(mio_icif_blocks.HEAT_GENERATOR_ELC.get());
                output.accept(mio_icif_blocks.STIRLING_GENERATOR.get());
                output.accept(mio_icif_blocks.DROP_GENERATOR.get());
                output.accept(mio_icif_blocks.ADVANCED_DROP_GENERATOR.get());
                output.accept(mio_icif_blocks.ADVANCED_STIRLING_GENERATOR.get());
                output.accept(mio_icif_blocks.ADVANCED_SEMIFLUID_GENERATOR.get());
                output.accept(mio_icif_blocks.EXPERIENCE_GENERATOR.get());
                output.accept(mio_icif_blocks.ADVANCED_EXPERIENCE_GENERATOR.get());

                // ============================================================
                //  19. BLOCKS - STEAM SYSTEM
                // ============================================================
                output.accept(mio_icif_blocks.STEAM_GENERATOR.get());
                output.accept(mio_icif_blocks.STEAM_KINETIC_GENERATOR.get());
                output.accept(mio_icif_blocks.SOLAR_DISTILLER.get());
                output.accept(mio_icif_blocks.STEAM_REPRESSURIZER.get());

                // ============================================================
                //  20. BLOCKS - BASIC PROCESSING MACHINES
                // ============================================================
                output.accept(mio_icif_blocks.FURNACE_ELC.get());
                output.accept(mio_icif_blocks.POWDER_ELC.get());
                output.accept(mio_icif_blocks.POWDER_ADVANCED_ELC.get());
                output.accept(mio_icif_blocks.COMPRESSOR_ELC.get());
                output.accept(mio_icif_blocks.COMPRESSOR_ADVANCED_ELC.get());
                output.accept(mio_icif_blocks.EXTRACTOR_ELC.get());
                output.accept(mio_icif_blocks.WASHER_ELC.get());
                output.accept(mio_icif_blocks.METAL_FORMER.get());
                output.accept(mio_icif_blocks.METAL_FORMER_ADVANCED.get());
                output.accept(mio_icif_blocks.BLOCK_CUTTER.get());
                output.accept(mio_icif_blocks.LATHE.get());
                output.accept(mio_icif_blocks.CANNER_ELC.get());
                output.accept(mio_icif_blocks.RECYCLER_ELC.get());
                output.accept(mio_icif_blocks.BLAST_FURNACE.get());
                output.accept(mio_icif_blocks.BLAST_FURNACE_ELC.get());
                output.accept(mio_icif_blocks.BLAST_FURNACE_ADVANCED.get());

                // ============================================================
                //  21. BLOCKS - INTERMEDIATE MACHINES
                // ============================================================
                output.accept(mio_icif_blocks.CENTRIFUGE_ELC.get());
                output.accept(mio_icif_blocks.ELECTROLYZER.get());
                output.accept(mio_icif_blocks.PUMP_ELC.get());
                output.accept(mio_icif_blocks.CONDENSER.get());
                output.accept(mio_icif_blocks.FERMENTER_ELC.get());
                output.accept(mio_icif_blocks.OIL_REFINERY_ELC.get());
                output.accept(mio_icif_blocks.MAGNETIZER.get());

                // ============================================================
                //  22. BLOCKS - ADVANCED MACHINES
                // ============================================================
                output.accept(mio_icif_blocks.INDUCTION_ELC.get());
                output.accept(mio_icif_blocks.MATTER_ELC.get());
                output.accept(mio_icif_blocks.LARGE_FABRICATOR_CORE.get());
                output.accept(mio_icif_blocks.LARGE_FABRICATOR_INPUT_IV.get());
                output.accept(mio_icif_blocks.LARGE_FABRICATOR_TANK.get());
                output.accept(mio_icif_blocks.LARGE_FABRICATOR_SCRAP.get());
                output.accept(mio_icif_blocks.NEUTRON_POLYMERIZER.get());
                output.accept(mio_icif_blocks.REPLICATOR_ELC.get());
                output.accept(mio_icif_blocks.SCANNER_ELC.get());
                output.accept(mio_icif_blocks.MOLECULAR_TRANSFORMER.get());
                output.accept(mio_icif_blocks.TELEPORTER_ELC.get());
                output.accept(mio_icif_blocks.TERRA_ELC.get());
                output.accept(mio_icif_blocks.FUTURE_ELC.get());
                output.accept(mio_icif_blocks.TESLA.get());

                // 工业工作台               
                output.accept(mio_icif_blocks.INDUSTRIAL_WORKBENCH.get());
                output.accept(mio_icif_blocks.ENERGY_CONVERTER.get());
                // ============================================================
                //  22.5 BLOCKS - ITEM & FLUID TRANSPORT
                // ============================================================
                output.accept(mio_icif_blocks.ITEM_BUFFER_ELC.get());
                output.accept(mio_icif_blocks.SORTER_ELC.get());
                output.accept(mio_icif_blocks.ITEM_DISTRIBUTOR_ELC.get());
                output.accept(mio_icif_blocks.FLUID_DISTRIBUTOR_ELC.get());
                output.accept(mio_icif_blocks.WEIGHTED_FLUID_DISTRIBUTOR_ELC.get());
                output.accept(mio_icif_blocks.FLUID_REGULATOR_ELC.get());
                output.accept(mio_icif_blocks.BATCH_CRAFTER.get());
                output.accept(mio_icif_blocks.CHUNK_LOADER.get());

                // ============================================================
                //  23. BLOCKS - AGRICULTURE
                // ============================================================
                output.accept(mio_icif_blocks.CROP_STICK.get());
                output.accept(mio_icif_blocks.CROP_STICK_UPGRADED.get());
                output.accept(mio_icif_blocks.HARVEST_ELC.get());
                output.accept(mio_icif_blocks.MATRON_ELC.get());

                // ============================================================
                //  24. BLOCKS - MINING
                // ============================================================
                output.accept(mio_icif_blocks.MINER_ELC.get());
                output.accept(mio_icif_blocks.ADVANCED_MINER_ELC.get());
                output.accept(mio_icif_blocks.BLOCK_MINING_PIPE.get());
                output.accept(mio_icif_blocks.BLOCK_MINING_TIP.get());

                // ============================================================
                //  25. BLOCKS - FLUID PIPES
                // ============================================================
                output.accept(mio_icif_blocks.PIPE_WATER.get());
                output.accept(mio_icif_blocks.PIPE_WATER_EXTRACT.get());
                output.accept(mio_icif_blocks.PIPE_ITEM_INPUT.get());
                output.accept(mio_icif_blocks.PIPE_ITEM_TRANSPORT.get());

                // ============================================================
                //  26. BLOCKS - MACHINE HULLS & MISC
                // ============================================================
                output.accept(mio_icif_blocks.MACHINE_HULL_BASIC.get());
                output.accept(mio_icif_blocks.MACHINE_HULL_ADVANCED.get());
                output.accept(mio_icif_blocks.CHECKER.get());

                // ============================================================
                //  27. BLOCKS - REACTOR STRUCTURE
                // ============================================================
                output.accept(mio_icif_blocks.REACTOR_CHAMBER.get());
                output.accept(mio_icif_blocks.REACTOR_VESSEL.get());
                output.accept(mio_icif_blocks.REACTOR_ACCESS_HATCH.get());
                output.accept(mio_icif_blocks.REACTOR_FLUID_PORT.get());
                output.accept(mio_icif_blocks.REACTOR_REDSTONE_PORT.get());
                output.accept(mio_icif_blocks.REDSTONE_REACTOR_COOLANT_INJECTOR.get());
                output.accept(mio_icif_blocks.LAPIS_REACTOR_COOLANT_INJECTOR.get());
                output.accept(mio_icif_blocks.IC_TNT.get());
                output.accept(mio_icif_blocks.NUKE.get());

                // ============================================================
                //  28. BLOCKS - RADIATION
                // ============================================================
                output.accept(mio_icif_blocks.BLOCK_RADIATING_STONE.get());
                output.accept(mio_icif_blocks.BLOCK_RADIATING_DIRT.get());
                output.accept(mio_icif_blocks.BLOCK_RADIATING_DEEPSLATE.get());

                // ============================================================
                //  29. BLOCKS - SCAFFOLDING
                // ============================================================
                output.accept(mio_icif_blocks.SCAFFOLD_WOOD.get());
                output.accept(mio_icif_blocks.SCAFFOLD_IRON.get());
                output.accept(mio_icif_blocks.SCAFFOLD_STEEL.get());
                output.accept(mio_icif_blocks.SCAFFOLD_CARBON.get());
                output.accept(mio_icif_blocks.TITANIUM_DRILL_FRAME.get());

                // ============================================================
                //  30. BLOCKS - CONSTRUCTION
                // ============================================================
                output.accept(mio_icif_blocks.BLOCK_BRONZE.get());
                output.accept(mio_icif_blocks.BLOCK_ADVIRON.get());
                output.accept(mio_icif_blocks.BLOCK_ALLOY_GLASS.get());
                output.accept(mio_icif_blocks.BLOCK_IRRADIANT_GLASS_PANE.get());
                output.accept(mio_icif_blocks.BLOCK_ALLOY_DOOR.get());
                output.accept(mio_icif_blocks.CONSTRUCTION_FOAM.get());
                output.accept(mio_icif_blocks.CONSTRUCTION_WALL.get());

                // ============================================================
                //  30.5. BLOCKS - STORAGE BOXES
                // ============================================================
                output.accept(mio_icif_blocks.STORAGE_BOX_WOOD.get());
                output.accept(mio_icif_blocks.STORAGE_BOX_BRONZE.get());
                output.accept(mio_icif_blocks.STORAGE_BOX_IRON.get());
                output.accept(mio_icif_blocks.STORAGE_BOX_ADVIRON.get());
                output.accept(mio_icif_blocks.STORAGE_BOX_IRIDIUM.get());
                output.accept(mio_icif_blocks.STORAGE_BOX_TITANIUM.get());
                output.accept(mio_icif_blocks.BRONZE_TANK.get());
                output.accept(mio_icif_blocks.IRON_TANK.get());
                output.accept(mio_icif_blocks.TITANIUM_TANK.get());
                output.accept(mio_icif_blocks.ADVIRON_TANK.get());
                output.accept(mio_icif_blocks.IRIDIUM_TANK.get());

                // ============================================================
                //  31. BLOCKS - RUBBER TREE & RUBBER WOOD SERIES
                // ============================================================
                output.accept(mio_icif_blocks.BLOCK_RUBBER_SAPLING.get());
                output.accept(mio_icif_blocks.BLOCK_RUBBER_TREE.get());
                output.accept(mio_icif_blocks.BLOCK_STRIPPED_RUBBER_WOOD.get());
                output.accept(mio_icif_blocks.BLOCK_RUBBER_LEAF.get());
                output.accept(mio_icif_blocks.RUBBER_PLANKS.get());
                output.accept(mio_icif_blocks.RUBBER_STAIRS.get());
                output.accept(mio_icif_blocks.RUBBER_SLAB.get());
                output.accept(mio_icif_blocks.RUBBER_FENCE.get());
                output.accept(mio_icif_blocks.RUBBER_FENCE_GATE.get());
                output.accept(mio_icif_blocks.RUBBER_DOOR.get());
                output.accept(mio_icif_blocks.RUBBER_TRAPDOOR.get());
                output.accept(mio_icif_blocks.RUBBER_PRESSURE_PLATE.get());
                output.accept(mio_icif_blocks.RUBBER_BUTTON.get());
                output.accept(mio_icif_blocks.RUBBER_SIGN.get());
                output.accept(mio_icif_blocks.RUBBER_HANGING_SIGN.get());
                output.accept(mio_icif_blocks.BLOCK_FENCE_IRON.get());

            })).build());

    /**
     * 创建包含能量数据的BlockEntityTag NBT 标签
     * 用于给储电方块物品添加满电状态
     * @param energy 能量值（EU）
     * @param blockEntityId BlockEntity 的注册ID（如 "mio_icif:batbox"）
     * @return 包含能量数据的CompoundTag
     */
    private static net.minecraft.nbt.CompoundTag createEnergyTag(long energy, String blockEntityId) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", blockEntityId);
        tag.putLong("energy", energy);
        return tag;
    }

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}