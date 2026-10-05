---
navigation:
  title: "Magnetizer"
  icon: mio_icif:producer/block_magnetizer
  parent: machines.md
  position: 41
item_ids:
  - mio_icif:producer/block_magnetizer
---

# Magnetizer

<Row>
  <BlockImage id="mio_icif:producer/block_magnetizer" scale="3" />
</Row>

## Function

The Magnetizer uses EU to magnetize connected iron bars and Iron Fence blocks.
A player with iron, gold, netherite or electric boots moves up inside the magnetized bars.
The machine magnetizes bars up to 20 blocks above or below its own height.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 100 EU |
| Use while working | 5 EU/t |
| Operation time | 100 tick (5 s) |
| Energy per operation | 500 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Build a vertical column of iron bars.
2. Place the Magnetizer next to the lowest block of the column.
3. Connect an LV cable to the machine.
4. Put on iron boots or electric boots.
5. Walk into the bars to move up.

## Notes

- A redstone signal stops the machine.

> **WARNING:** The machine explodes on overvoltage above LV.


## Recipe

<RecipesFor id="mio_icif:producer/block_magnetizer" fallbackText="-" />
