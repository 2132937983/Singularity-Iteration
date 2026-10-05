---
navigation:
  title: "Replicator"
  icon: mio_icif:producer/block_replicator_elc
  parent: machines.md
  position: 57
item_ids:
  - mio_icif:producer/block_replicator_elc
---

# Replicator

<Row>
  <BlockImage id="mio_icif:producer/block_replicator_elc" scale="3" />
</Row>

## Function

The Replicator uses UU-Matter and EU to make a copy of an item from a stored pattern.
The pattern comes from a Pattern Storage Crystal in the memory slot or from an adjacent Pattern Storage.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | EV (2,048 EU) |
| Maximum input | 8,192 EU/t |
| Energy storage | 2,000,000 EU |
| Use while working | 512 EU/t |
| Operation time | 100 tick (5 s) |
| Energy per operation | 51,200 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an EV supply to the machine through transformers.
2. Supply UU-Matter with UU-Matter Cells or with a pipe.
3. Put a Pattern Storage Crystal into the memory slot, or place a Pattern Storage next to the machine.
4. Click Single or Loop in the GUI to start.
5. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above EV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_replicator_elc" fallbackText="-" />
