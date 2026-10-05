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
Rare items give more EU than common items.

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
