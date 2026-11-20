---
navigation:
  title: "Thermal Centrifuge"
  icon: mio_icif:producer/block_centrifuge_elc
  parent: machines.md
  position: 67
item_ids:
  - mio_icif:producer/block_centrifuge_elc
---

# Thermal Centrifuge

<Row>
  <BlockImage id="mio_icif:producer/block_centrifuge_elc" scale="3" />
</Row>

## Function

The Thermal Centrifuge uses EU to heat up and then separates items into up to three products.

**Heating:** Each recipe sets a minimum heat (Cobblestone 100 HU, clay 250 HU, crushed ores 500 to 4,000 HU, recipes without a value 5,000 HU). The machine starts when it has heated to what the recipe needs. It heats only when it has material, free output space, and is not turned off: +4 HU per tick for 4 EU, up to 5,000 HU. Otherwise it loses 2 HU per tick.

**Rotor:** While working, the rotor speed rises by 10% per second. When not working, it falls by 5% per second. Below full speed, the machine runs in proportion to the speed. Full speed is 12,000 rpm, with a centrifugal force of about 16,000 g.

The GUI draws a top view of the heating ring and the rotor. The large number is the rotor speed.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 24,000 EU |
| Use while working | 48 EU/t |
| Operation time | 500 tick (25 s) |
| Energy per operation | 24,000 EU |
| Heating | +4 HU and 4 EU per tick, up to 5,000 HU |

The operation time and energy in the table are the values without overclocking. Each Overclocker Upgrade multiplies the operation time by 0.7 and the energy use by 1.6. The GUI and JEI show the actual values for the current input.

## Procedure

1. Connect an MV cable to the machine.
2. Put the crushed ore into the input slot.
3. Wait until the machine reaches the working heat.
4. Take the products from the three output slots.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_centrifuge_elc" fallbackText="-" />
