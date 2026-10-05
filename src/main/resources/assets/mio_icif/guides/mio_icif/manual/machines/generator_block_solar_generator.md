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

The machine makes EU from sunlight during the day.
The machine needs a clear view of the sky above it.
The machine stops at night and in rain.

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
