---
navigation:
  title: "Chunk Loader"
  icon: mio_icif:producer/block_chunk_loader
  parent: machines.md
  position: 16
item_ids:
  - mio_icif:producer/block_chunk_loader
---

# Chunk Loader

<Row>
  <BlockImage id="mio_icif:producer/block_chunk_loader" scale="3" />
</Row>

## Function

The Chunk Loader keeps selected chunks loaded. The machine loads up to 25 chunks in a 9 x 9 chunk area around itself.
Each loaded chunk costs EU. The machine stops when the EU runs out.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 2,500 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Open the GUI and click the chunks to load.
3. Keep a steady EU supply to the machine.

## Notes

- The chunk that holds the Chunk Loader is part of the selection grid.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_chunk_loader" fallbackText="-" />
