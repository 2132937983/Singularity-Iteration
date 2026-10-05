---
navigation:
  title: "METS Advanced Solar Generator"
  icon: mio_icif:generator/block_mets_advanced_solar_generator
  parent: generators.md
  position: 19
item_ids:
  - mio_icif:generator/block_mets_advanced_solar_generator
---

# METS Advanced Solar Generator

<Row>
  <BlockImage id="mio_icif:generator/block_mets_advanced_solar_generator" scale="3" />
</Row>

## Function

The machine makes EU in proportion to the sky brightness above it.
The sun angle, rain and thunder change the brightness. Sandy biomes ignore rain and thunder.
At night and in dimensions without a sky, the machine keeps a minimum output.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | MV (128 EU) |
| Output | 128 EU/t |
| Energy storage | 200,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator under open sky.
2. Connect an MV cable to the generator.
3. Put a battery into the charge slot.

## Recipe

<RecipesFor id="mio_icif:generator/block_mets_advanced_solar_generator" fallbackText="-" />
