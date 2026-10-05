---
navigation:
  title: "Block Cutter"
  icon: mio_icif:producer/block_block_cutter
  parent: machines.md
  position: 14
item_ids:
  - mio_icif:producer/block_block_cutter
---

# Block Cutter

<Row>
  <BlockImage id="mio_icif:producer/block_block_cutter" scale="3" />
</Row>

## Function

The Block Cutter uses EU and a cutting blade to cut blocks. Metal blocks become plates. Logs become planks.
Each recipe needs a minimum blade hardness. The Diamond Cutting Blade is harder than the Iron Cutting Blade.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 43,200 EU |
| Use while working | 48 EU/t |
| Operation time | 900 tick (45 s) |
| Energy per operation | 43,200 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Put a cutting blade into the blade slot.
3. Put the block to cut into the input slot.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_block_cutter" fallbackText="-" />
