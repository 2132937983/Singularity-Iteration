---
navigation:
  title: "Batch Crafter"
  icon: mio_icif:producer/block_batch_crafter
  parent: machines.md
  position: 12
item_ids:
  - mio_icif:producer/block_batch_crafter
---

# Batch Crafter

<Row>
  <BlockImage id="mio_icif:producer/block_batch_crafter" scale="3" />
</Row>

## Function

The Batch Crafter uses EU to craft a crafting table recipe automatically.
The GUI holds the recipe pattern. The machine takes the ingredients from nine ingredient slots.
Container remainders, for example empty buckets, go to separate output slots.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 20,000 EU |
| Use while working | 2 EU/t |
| Operation time | 40 tick (2 s) |
| Energy per operation | 80 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Place the recipe pattern in the crafting grid of the GUI.
3. Put the ingredients into the ingredient slots, or pipe them in.
4. Install Overclocker Upgrades to process faster.
5. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_batch_crafter" fallbackText="-" />
