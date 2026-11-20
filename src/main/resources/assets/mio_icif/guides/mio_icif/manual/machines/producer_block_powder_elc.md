---
navigation:
  title: "Macerator"
  icon: mio_icif:producer/block_powder_elc
  parent: machines.md
  position: 40
item_ids:
  - mio_icif:producer/block_powder_elc
---

# Macerator

<Row>
  <BlockImage id="mio_icif:producer/block_powder_elc" scale="3" />
</Row>

## Function

The Macerator uses EU to grind ores and other items into crushed ores and dusts.
The machine has one input slot, one output slot, one battery slot and four upgrade slots.

**Principle:** The work needed for grinding depends on the Bond work index of the material. Harder and tougher materials need more work. The Macerator has a fixed power, so hard materials take longer and soft materials are faster. Energy per operation = 47.2 EU × work index (Iron Ore 12.7 → 600 EU).

| Input | Work index | Energy per operation | Operation time |
|---|---|---|---|
| Clay block | 7.1 | 335 EU | 168 tick |
| Coal | 11.4 | 539 EU | 270 tick |
| Iron Ore | 12.7 | 600 EU | 300 tick |
| Cobblestone | 14.4 | 680 EU | 340 tick |
| Gravel | 25.2 | 1,191 EU | 596 tick |

Items not in the material table count as 12.7. When a recipe sets an operation time, the machine uses the recipe's time. The GUI draws a hammer rotor and shows the energy of the current operation and the work index.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 1,800 EU |
| Use while working | 2 EU/t |
| Operation time | By material (Iron Ore 300 tick) |
| Energy per operation | 47.2 EU × work index (Iron Ore 600 EU) |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an LV cable to the machine.
2. Put the ore or the item to grind into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_powder_elc" fallbackText="-" />
