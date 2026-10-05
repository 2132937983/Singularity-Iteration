---
navigation:
  title: "Electric Nano Blast Furnace"
  icon: mio_icif:producer/block_blast_furnace_elc
  parent: machines.md
  position: 21
item_ids:
  - mio_icif:producer/block_blast_furnace_elc
---

# Electric Nano Blast Furnace

<Row>
  <BlockImage id="mio_icif:producer/block_blast_furnace_elc" scale="3" />
</Row>

## Function

The Electric Nano Blast Furnace uses EU to process the Blast Furnace recipes. The machine needs no heat and no Compressed Air.
The machine gives only the main product. The machine gives no Slag.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | IV (8,192 EU) |
| Maximum input | 8,192 EU/t |
| Energy storage | 320,000 EU |
| Use while working | 8,000 EU/t |
| Operation time | 40 tick (2 s) |
| Energy per operation | 320,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an IV supply to the machine through transformers.
2. Put the iron into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above IV.


## Recipe

<RecipesFor id="mio_icif:producer/block_blast_furnace_elc" fallbackText="-" />
