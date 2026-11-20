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
Logs give the planks of their own wood: every log, wood, stem and stripped variant, bamboo blocks, and the woods of other mods that have a crafting recipe to planks. A log gives 6 planks (4 by hand), a bamboo block 3 (2 by hand).

**Operation time:** Harder materials take longer to saw. Metal blocks and obsidian take 150 tick × hardness at 16 EU/t: copper, gold, tin and lead blocks 150 tick (2,400 EU); iron, bronze, lapis and Refined Iron blocks 750 tick (12,000 EU); obsidian 1,200 tick (19,200 EU). Logs and planks take 100 tick × 2 EU/t. Recipes without values take 900 tick × 48 EU/t.

**Blade wear:** Each recipe needs a minimum blade hardness. The Iron Cutting Blade has hardness 2 and durability 64. The Diamond Cutting Blade has hardness 8 and durability 1,024. Each cut block wears the blade by (material hardness ÷ blade hardness)². Wear below 1 point accumulates, and the blade breaks when it is worn through. Example: an Iron Cutting Blade can cut 64 blocks of hardness 2, or 256 blocks of hardness 1. When there is no blade or the blade is too soft, the status shows "Blade too soft or missing".

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 43,200 EU |
| Use while working | By recipe (metal blocks 16 EU/t) |
| Operation time | By recipe (metal blocks 150 tick × hardness) |
| Energy per operation | By recipe (iron block 12,000 EU) |
| Blade wear | (material hardness ÷ blade hardness)² per block |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

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
