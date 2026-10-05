---
navigation:
  title: "Infinite Generator"
  icon: mio_icif:generator/block_unlimit_generator
  parent: generators.md
  position: 17
item_ids:
  - mio_icif:generator/block_unlimit_generator
---

# Infinite Generator

<Row>
  <BlockImage id="mio_icif:generator/block_unlimit_generator" scale="3" />
</Row>

## Function

The machine makes EU continuously without fuel.
The machine charges a battery in its charge slot. The machine is a creative-mode block.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | IV (8,192 EU) |
| Output | 8,192 EU/t |
| Energy storage | 100,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the generator next to a machine or a cable.
2. Put a battery into the charge slot to charge it.

## Notes

- Check the voltage tier of each connected machine. A machine explodes on overvoltage.

## Recipe

<RecipesFor id="mio_icif:generator/block_unlimit_generator" fallbackText="-" />
