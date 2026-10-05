---
navigation:
  title: "Wind Generator"
  icon: mio_icif:generator/block_wind_generator
  parent: generators.md
  position: 39
item_ids:
  - mio_icif:generator/block_wind_generator
---

# Wind Generator

<Row>
  <BlockImage id="mio_icif:generator/block_wind_generator" scale="3" />
</Row>

## Function

The machine makes EU from wind.
The output increases with height above Y 64. Rain and thunder also increase the output.
Blocks in a 9x9x7 zone around the machine decrease the output.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | LV (32 EU) |
| Output | 32 EU/t |
| Energy storage | 64,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator high above Y 64.
2. Remove the blocks around the generator.
3. Connect an LV cable to the generator.
4. Put a battery into the battery slot to charge it.

## Notes

- Below Y 64, the machine makes no EU.
- The wind strength changes at random over time.

## Recipe

<RecipesFor id="mio_icif:generator/block_wind_generator" fallbackText="-" />
