---
navigation:
  title: "Semifluid Generator"
  icon: mio_icif:generator/block_semifluid_generator
  parent: generators.md
  position: 27
item_ids:
  - mio_icif:generator/block_semifluid_generator
---

# Semifluid Generator

<Row>
  <BlockImage id="mio_icif:generator/block_semifluid_generator" scale="3" />
</Row>

## Function

The machine burns semifluid fuel and makes EU.
The machine accepts biogas, biomass, crude oil and diesel oil.
Each fuel gives a different EU output.
The machine is an engine that follows the grid: it makes what is taken, plus a top-up while its storage is below 90 %. It stops when the storage is full (98 %) and starts again below 60 %, so a full storage burns no fuel.
Fuel per EU is lowest near 80 % load (5 % better than at full load) and rises at part load. A cold engine has 80 % of its efficiency until the coolant reaches about 80 °C.
The screen is an engine room monitor: exhaust temperature per cylinder, tachometer, coolant, fuel tank and a record of the load and exhaust temperature.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | 8 to 32 EU/t, by fuel |
| Energy storage | 32,000 EU |
| Fuel: Biogas | 0.5 mB/t → 16 EU/t (32 EU/mB) |
| Fuel: Biomass | 1 mB/t → 8 EU/t (8 EU/mB) |
| Fuel: Crude Oil | 0.5 mB/t → 8 EU/t (16 EU/mB) |
| Fuel: Diesel Oil | 0.25 mB/t → 32 EU/t (128 EU/mB) |

The game measured these values from the block entity of this version.

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change. The fuel use stays the same.

## Procedure

1. Connect an LV cable to the generator.
2. Put a fuel bucket or a fuel cell into the input slot.
3. Remove the empty containers from the output slot.
4. Put a battery into the battery slot to charge it.

## Recipe

<RecipesFor id="mio_icif:generator/block_semifluid_generator" fallbackText="-" />
