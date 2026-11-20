---
navigation:
  title: "Electric Furnace"
  icon: mio_icif:producer/block_furnace_elc
  parent: machines.md
  position: 20
item_ids:
  - mio_icif:producer/block_furnace_elc
---

# Electric Furnace

<Row>
  <BlockImage id="mio_icif:producer/block_furnace_elc" scale="3" />
</Row>

## Function

The Electric Furnace uses EU to smelt items with the furnace recipes.
The machine has one input slot, one output slot, one battery slot and four upgrade slots.

**Principle:** Resistance heating. The heating wire raises the chamber temperature by 10 °C per tick, up to 1,100 °C. When not working, the chamber cools toward 20 °C by Newton's law of cooling (time constant 30 seconds). This also applies while the machine sleeps.

**Required temperature:** Food can start at 250 °C. Ores, metals and other items need 1,000 °C. When the temperature is too low, the machine only heats up and does not progress, and the status shows "Heating". It also uses 3 EU/t while heating. Smelting ore from a cold furnace first needs about 100 tick of heating. In continuous work the furnace stays hot, so there is no extra cost. No external heat source is needed.

The GUI draws the chamber. The color of the heating wire changes with the furnace temperature, and the large number is the furnace temperature.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 600 EU |
| Use while working | 3 EU/t |
| Operation time | 100 tick (5 s), plus heating from a cold furnace |
| Energy per operation | 300 EU |
| Required temperature | Food 250 °C, others 1,000 °C |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an LV cable to the machine.
2. Put the items to smelt into the input slot.
3. Install Overclocker Upgrades to process faster.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_furnace_elc" fallbackText="-" />
