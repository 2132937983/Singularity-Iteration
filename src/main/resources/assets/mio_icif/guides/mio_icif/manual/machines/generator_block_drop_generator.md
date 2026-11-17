---
navigation:
  title: "Drop Generator"
  icon: mio_icif:generator/block_drop_generator
  parent: generators.md
  position: 6
item_ids:
  - mio_icif:generator/block_drop_generator
---

# Drop Generator

<Row>
  <BlockImage id="mio_icif:generator/block_drop_generator" scale="3" />
</Row>

## Function

The machine pulls dropped items within a short range and destroys them to make EU.
Rare items give more EU than common items. Common items count by their furnace burn time.
The machine is an incinerator with a chamber temperature. Burning items heat the chamber toward 1,000 °C; items that do not burn (stone, dirt) give no fuel and cool it by 25 °C each. The share of the fuel turned into EU follows the chamber: nothing below 300 °C, all of it at 850 °C. A cold machine burns its first fuel to warm up.
Every item leaves ash in the ash pit (one per burning item, two per other item). At 64 ash the machine stops taking items; take the ash out by hand, with a hopper or a pipe.
The screen is a recorder: the chamber temperature of the last 12 seconds with a mark for every item, the efficiency, the output and the ash.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | ULV (8 EU) |
| Output | 20 EU/t |
| Energy storage | 10,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator at the exit of an item collection line.
2. Connect an MV cable to the generator.
3. Drop unwanted items near the generator.

## Notes


> **WARNING:** The machine destroys every dropped item in its range, including your own items.


## Recipe

<RecipesFor id="mio_icif:generator/block_drop_generator" fallbackText="-" />
