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

The machine makes EU from the sunlight on it. The sun is weak in the morning and evening and strongest at noon; the machine reaches its rated output at 700 W/m² and stays there above it.
Rain keeps about 70 % of the light and a thunderstorm about 37 %. A hot panel makes less (−0.4 % per °C above 25 °C), so deserts are sunny but hot.
The machine needs open sky straight above it; under cover it gets only the diffuse light, in the dark nothing.
The screen shows today's record: the sunlight on the panel, a clear day for comparison, and the output.
At night, while it sees the sky, its night core keeps a lower output.

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
