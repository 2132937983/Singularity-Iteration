---
navigation:
  title: "Blast Furnace"
  icon: mio_icif:producer/block_blast_furnace
  parent: machines.md
  position: 13
item_ids:
  - mio_icif:producer/block_blast_furnace
---

# Blast Furnace

<Row>
  <BlockImage id="mio_icif:producer/block_blast_furnace" scale="3" />
</Row>

## Function

The Blast Furnace uses heat (HU) and Compressed Air to make Refined Iron Ingots from iron ore, iron dust or iron ingots.
Slag is a by-product. The furnace takes heat only from a heat source on its front face.

## Power data

This block uses no EU.

## Procedure

1. Place a heat generator against the front face of the furnace.
2. Make Compressed Air Cells from Empty Cells in a Compressor.
3. Put the Compressed Air Cells into the air slot.
4. Put the iron into the input slot and wait until the furnace is hot.
5. Take the ingots, the Slag and the Empty Cells from the output slots.

## Notes

- The furnace accepts heat only while the input slot holds a valid item.
- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

## Recipe

<RecipesFor id="mio_icif:producer/block_blast_furnace" fallbackText="-" />
