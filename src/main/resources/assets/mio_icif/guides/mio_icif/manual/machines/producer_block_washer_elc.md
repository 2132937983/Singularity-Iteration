---
navigation:
  title: "Ore Washer"
  icon: mio_icif:producer/block_washer_elc
  parent: machines.md
  position: 49
item_ids:
  - mio_icif:producer/block_washer_elc
---

# Ore Washer

<Row>
  <BlockImage id="mio_icif:producer/block_washer_elc" scale="3" />
</Row>

## Function

The Ore Washer uses EU and water to wash crushed ores into purified crushed ores and by-products. The machine has three output slots.

**Principle:** Gravity separation needs a steady flow of water. Each wash passes the amount of water set by the recipe (crushed ore 1,000 mB). The settling tank recovers 80%, so the net use is one fifth of the flow (crushed ore 200 mB). The net water is taken tick by tick over the whole operation time. With too little water the machine slows down in proportion, and with an empty tank it shows "No water".

**Time and power:** The default is 400 tick × 16 EU/t. When a recipe sets a time and a power, the machine uses the recipe: crushed niobium ore and crushed titanium ore 200 tick × 10 EU/t, with 200 mB water (net use 40 mB).

The GUI draws the washing drum, the settling tank and the return pipe, and shows the tank content, the flow and the net use.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 8,000 EU |
| Use while working | By recipe (default 16 EU/t) |
| Operation time | By recipe (default 400 tick) |
| Energy per operation | By recipe (default 6,400 EU) |
| Water use | Flow by recipe (crushed ore 1,000 mB), 80% recovered |
| Net water for crushed ore | 200 mB per operation (0.5 mB/t) |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an MV cable to the machine.
2. Put a Water Bucket or a Water Cell into the water slot, or connect a water pipe.
3. Put the crushed ore into the input slot.
4. Take the products from the three output slots.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_washer_elc" fallbackText="-" />
