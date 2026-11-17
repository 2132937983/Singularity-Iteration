---
navigation:
  title: "Solar Generator"
  icon: mio_icif:generator/block_solar_generator
  parent: generators.md
  position: 28
item_ids:
  - mio_icif:generator/block_solar_generator
---

# Solar Generator

<Row>
  <BlockImage id="mio_icif:generator/block_solar_generator" scale="3" />
</Row>

## Function

The machine makes EU from the sunlight on it. The sun is weak in the morning and evening and strongest at noon; the machine reaches its rated output at 700 W/m² and stays there above it.
Rain keeps about 70 % of the light and a thunderstorm about 37 %. A hot panel makes less (−0.4 % per °C above 25 °C), so deserts are sunny but hot.
The machine needs open sky straight above it; under cover it gets only the diffuse light, in the dark nothing.
The screen shows today's record: the sunlight on the panel, a clear day for comparison, and the output.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | 1 EU/t |
| Energy storage | 2 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator in a dimension with a sky, for example the Overworld.
2. Remove all opaque blocks above the generator.
3. Connect an LV cable to the generator.
4. Put a battery into the battery slot to charge it.

## Recipe

<RecipesFor id="mio_icif:generator/block_solar_generator" fallbackText="-" />
