---
navigation:
  title: "Advanced Semifluid Generator"
  icon: mio_icif:generator/block_advanced_semifluid_generator
  parent: generators.md
  position: 2
item_ids:
  - mio_icif:generator/block_advanced_semifluid_generator
---

# Advanced Semifluid Generator

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_semifluid_generator" scale="3" />
</Row>

## Function

The machine burns semifluid fuels (biogas, biomass, crude oil, diesel oil) and makes EU.
Each fuel gives 1.25 times the EU per mB of the Semifluid Generator.
The machine accepts buckets, cells and other containers of these fuels.
The machine is an engine that follows the grid: it makes what is taken, plus a top-up while its storage is below 90 %. It stops when the storage is full (98 %) and starts again below 60 %, so a full storage burns no fuel.
Fuel per EU is lowest near 80 % load (5 % better than at full load) and rises at part load. A cold engine has 80 % of its efficiency until the coolant reaches about 80 °C.
The screen is an engine room monitor: exhaust temperature per cylinder, tachometer, coolant, fuel tank and a record of the load and exhaust temperature.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Output | 36 EU/t |
| Energy storage | 100,000 EU |
| Fuel: Biogas | 0.9 mB/t → 36 EU/t (40 EU/mB) |
| Fuel: Biomass | 3.6 mB/t → 36 EU/t (10 EU/mB) |
| Fuel: Crude Oil | 1.8 mB/t → 36 EU/t (20 EU/mB) |
| Fuel: Diesel Oil | 0.225 mB/t → 36 EU/t (160 EU/mB) |

The game measured these values from the block entity of this version.

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change. The fuel use stays the same.

## Procedure

1. Connect an MV cable to the generator.
2. Put a fuel bucket or cell into the input slot.
3. Remove the empty containers from the output slot.
4. Put a battery into the battery slot to charge it.

## Recipe

<RecipesFor id="mio_icif:generator/block_advanced_semifluid_generator" fallbackText="-" />
