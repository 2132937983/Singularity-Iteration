---
navigation:
  title: "Advanced Drop Generator"
  icon: mio_icif:generator/block_advanced_drop_generator
  parent: generators.md
  position: 0
item_ids:
  - mio_icif:generator/block_advanced_drop_generator
---

# Advanced Drop Generator

<Row>
  <BlockImage id="mio_icif:generator/block_advanced_drop_generator" scale="3" />
</Row>

## Function

The machine pulls dropped items and destroys them to make EU.
The machine has a larger range and a higher output than the Drop Generator.
Rare items give more EU than common items.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | ULV (8 EU) |
| Output | 40 EU/t |
| Energy storage | 100,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator at the exit of an item collection line.
2. Connect an HV cable to the generator.
3. Drop unwanted items near the generator.

## Notes


> **WARNING:** The machine destroys every dropped item in its range, including your own items.


## Recipe

<RecipesFor id="mio_icif:generator/block_advanced_drop_generator" fallbackText="-" />
