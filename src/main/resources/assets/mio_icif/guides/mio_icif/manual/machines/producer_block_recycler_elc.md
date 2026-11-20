---
navigation:
  title: "Recycler"
  icon: mio_icif:producer/block_recycler_elc
  parent: machines.md
  position: 56
item_ids:
  - mio_icif:producer/block_recycler_elc
---

# Recycler

<Row>
  <BlockImage id="mio_icif:producer/block_recycler_elc" scale="3" />
</Row>

## Function

The Recycler uses EU to shred items and sort out Scrap. The machine consumes one input item in every operation.

**Principle:** Twin-shaft shredding. Two counter-rotating cutter shafts shred the item, and the usable part is sorted out of the shreds.

**Output by proportional accumulation:** For each recycled item, the output rate (Scrap 1/8; a recycling recipe can set its own product and rate) is added to an accumulated value. When it reaches 1, one product comes out. So exactly one Scrap comes out every 8 items, with no randomness. When the product changes to another one, the accumulated value starts again from 0. The GUI shows the output rate, the current accumulated value and how many items are still needed.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 45 EU |
| Use while working | 1 EU/t |
| Operation time | 45 tick |
| Energy per operation | 45 EU |
| Output | 1 Scrap per 8 items (proportional accumulation) |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an LV cable to the machine.
2. Put the unwanted items into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_recycler_elc" fallbackText="-" />
