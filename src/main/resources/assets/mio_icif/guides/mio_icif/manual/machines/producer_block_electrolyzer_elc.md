---
navigation:
  title: "Electrolyzer"
  icon: mio_icif:producer/block_electrolyzer_elc
  parent: machines.md
  position: 23
item_ids:
  - mio_icif:producer/block_electrolyzer_elc
---

# Electrolyzer

<Row>
  <BlockImage id="mio_icif:producer/block_electrolyzer_elc" scale="3" />
</Row>

## Function

The Electrolyzer uses EU to split the water in Water Cells and stores the energy as chemical energy.
The machine returns the stored energy to adjacent blocks that are not full. The output slot receives Empty Cells.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 400 EU |
| Use while working | 10 EU/t |
| Operation time | 20 tick (1 s) |
| Energy per operation | 200 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the machine.
2. Put Water Cells into the input slot.
3. Place a storage block or a machine next to the Electrolyzer to take the stored energy.
4. Take the Empty Cells from the output slot.

## Notes


> **WARNING:** The machine explodes on overvoltage above LV.


## Recipe

<RecipesFor id="mio_icif:producer/block_electrolyzer_elc" fallbackText="-" />
