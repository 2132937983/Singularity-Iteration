---
navigation:
  title: "Oil Refinery"
  icon: mio_icif:producer/block_oil_refinery_elc
  parent: machines.md
  position: 48
item_ids:
  - mio_icif:producer/block_oil_refinery_elc
---

# Oil Refinery

<Row>
  <BlockImage id="mio_icif:producer/block_oil_refinery_elc" scale="3" />
</Row>

## Function

The Oil Refinery uses EU to refine fluids. Crude Oil becomes Diesel Oil.
The machine also turns Water or Steam into Distilled Water.
Each Overclocker Upgrade increases the fluid per cycle and the EU cost.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 2,000 EU |
| Use while working | 8 EU/t |
| Operation time | 10 tick (0.5 s) |
| Energy per operation | 80 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Pump the input fluid into the machine, or put a filled cell into the input slot.
3. Put Empty Cells into the output cell slot, or connect a pipe to take the product.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_oil_refinery_elc" fallbackText="-" />
