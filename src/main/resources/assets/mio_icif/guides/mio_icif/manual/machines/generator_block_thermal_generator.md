---
navigation:
  title: "Thermal Generator"
  icon: mio_icif:generator/block_thermal_generator
  parent: generators.md
  position: 32
item_ids:
  - mio_icif:generator/block_thermal_generator
---

# Thermal Generator

<Row>
  <BlockImage id="mio_icif:generator/block_thermal_generator" scale="3" />
</Row>

## Function

The machine burns furnace fuel and makes EU.
The machine charges a battery in its battery slot.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | 10 EU/t |
| Energy storage | 4,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the generator.
2. Put coal into the fuel slot.
3. Put a battery into the battery slot to charge it.

## Recipe

<RecipesFor id="mio_icif:generator/block_thermal_generator" fallbackText="-" />
