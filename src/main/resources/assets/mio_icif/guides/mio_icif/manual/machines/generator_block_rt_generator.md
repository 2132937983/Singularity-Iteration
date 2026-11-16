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
Each added pellet doubles the EU output (1, 2, 4, 8, 16, 32 EU/t with the air of a temperate biome around it). The pellets never deplete.
It is a thermoelectric generator: the colder its cold side the more it makes. The best of its six faces sets the cold side: ice, packed ice, blue ice, snow or powder snow ×1.25, water ×1.15; otherwise the air, ×0.9 in a hot biome (35 °C or more) and ×0.7 in the Nether. Lava touching any face costs a further ×0.6.
The screen is a recorder: the hot junctions' and the cold side's thermometers, the output of the last 12 seconds against the pellet table, and the cold side's factor.

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
