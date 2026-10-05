---
navigation:
  title: "Radioisotope Thermoelectric Generator"
  icon: mio_icif:generator/block_rt_generator
  parent: generators.md
  position: 26
item_ids:
  - mio_icif:generator/block_rt_generator
---

# Radioisotope Thermoelectric Generator

<Row>
  <BlockImage id="mio_icif:generator/block_rt_generator" scale="3" />
</Row>

## Function

The machine makes EU from Radioisotope Fuel Pellets.
Each added pellet doubles the EU output. The pellets never deplete.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | 32 EU/t |
| Energy storage | 20,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the generator.
2. Put up to six Radioisotope Fuel Pellets into the pellet slots.

## Recipe

<RecipesFor id="mio_icif:generator/block_rt_generator" fallbackText="-" />
