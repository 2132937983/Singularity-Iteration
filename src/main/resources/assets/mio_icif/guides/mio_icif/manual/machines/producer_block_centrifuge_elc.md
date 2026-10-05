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
The machine starts to process only after it reaches the minimum heat. Without power, the heat drops.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 24,000 EU |
| Use while working | 48 EU/t |
| Operation time | 500 tick (25 s) |
| Energy per operation | 24,000 EU |

The game measured these values from the block entity of this version.

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
