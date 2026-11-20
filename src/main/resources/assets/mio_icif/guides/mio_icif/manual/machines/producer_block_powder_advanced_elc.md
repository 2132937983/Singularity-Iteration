---
navigation:
  title: "Advanced Cyclone Macerator"
  icon: mio_icif:producer/block_powder_advanced_elc
  parent: machines.md
  position: 0
item_ids:
  - mio_icif:producer/block_powder_advanced_elc
---

# Advanced Cyclone Macerator

<Row>
  <BlockImage id="mio_icif:producer/block_powder_advanced_elc" scale="3" />
</Row>

## Function

The Advanced Cyclone Macerator uses the Macerator recipes at MV.

**Principle:** A jet mill and a cyclone separator form a closed grinding circuit. The separator takes away the fine powder and sends the coarse particles back to the mill. Nothing is over-ground, so each operation uses only 80% of the Macerator's energy for the same material (Iron Ore 480 EU).

**Rotor:** While the machine has work and power, the rotor speed rises by 10% per second. It takes about 10 seconds from standstill to full speed. Without work or power, the speed falls by 5% per second. Below full speed, the machine runs in proportion to the speed, so the average power and the processing speed drop together. At full speed it uses the full power of 12 EU/t and makes one Iron Ore in about 40 tick.

**Hold speed:** When the "Hold speed" button in the GUI is on, the machine pays 1 EU per tick while idle to keep the rotor speed, so the next item starts at full speed at once. The speed is not held while a redstone signal turns the machine off.

The GUI draws the grinding roller and the classifier wheel, and shows the speed, the cut size d50 (about 75 μm at full speed) and the circulating load (about 250% at full speed).

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 1,710 EU |
| Use while working | 12 EU/t at full speed |
| Operation time | By material (Iron Ore 40 tick) |
| Energy per operation | 80% of the Macerator (Iron Ore 480 EU) |
| Rotor | Speeds up 10% and slows down 5% per second |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an MV cable to the machine.
2. Put the ore or the item to grind into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_powder_advanced_elc" fallbackText="-" />
