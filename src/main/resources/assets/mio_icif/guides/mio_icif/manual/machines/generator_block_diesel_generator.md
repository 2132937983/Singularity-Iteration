---
navigation:
  title: "Diesel Generator"
  icon: mio_icif:generator/block_diesel_generator
  parent: generators.md
  position: 5
item_ids:
  - mio_icif:generator/block_diesel_generator
---

# Diesel Generator

<Row>
  <BlockImage id="mio_icif:generator/block_diesel_generator" scale="3" />
</Row>

## Function

The machine burns diesel oil and makes EU.
The machine accepts diesel oil buckets and diesel oil cells, and diesel oil from pipes or a Fluid Ejector Upgrade on any side.
The machine is an engine that follows the grid: it makes what is taken, plus a top-up while its storage is below 90 %. It stops when the storage is full (98 %) and starts again below 60 %, so a full storage burns no fuel.
Fuel per EU is lowest near 80 % load (5 % better than at full load) and rises at part load. A cold engine has 80 % of its efficiency until the coolant reaches about 80 °C.
The screen is an engine room monitor: exhaust temperature per cylinder, tachometer, coolant, fuel tank and a record of the load and exhaust temperature.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | EV (2,048 EU) |
| Output | 120 EU/t |
| Energy storage | 1,000,000 EU |
| Fuel: Diesel Oil | 0.5 mB/t → 120 EU/t (240 EU/mB) |

The game measured these values from the block entity of this version.

With a generator output multiplier other than 1.0 (config GeneratorOutput), the EU values change. The fuel use stays the same.

## Procedure

1. Connect an HV cable to the generator.
2. Put a diesel oil bucket into the input slot.
3. Remove the empty containers from the output slot.
4. Put a battery into the battery slot to charge it.

## Recipe

<RecipesFor id="mio_icif:generator/block_diesel_generator" fallbackText="-" />
