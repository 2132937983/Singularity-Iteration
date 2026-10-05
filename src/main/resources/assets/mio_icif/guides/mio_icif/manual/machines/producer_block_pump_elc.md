---
navigation:
  title: "Pump"
  icon: mio_icif:producer/block_pump_elc
  parent: machines.md
  position: 53
item_ids:
  - mio_icif:producer/block_pump_elc
---

# Pump

<Row>
  <BlockImage id="mio_icif:producer/block_pump_elc" scale="3" />
</Row>

## Function

The Pump removes fluid source blocks in front of the machine and stores the fluid in its tank.
The pump area is one layer high, 8 blocks long and 8 blocks to each side.
Next to a Miner, the Pump also removes the fluids that the Miner finds.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 20 EU |
| Use while working | 1 EU/t |
| Operation time | 20 tick (1 s) |
| Energy per operation | 20 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the Pump with its front face toward the fluid.
2. Connect an LV cable to the machine.
3. Put empty buckets or Empty Cells into the container slot, or connect a pipe.
4. Take the filled containers from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_pump_elc" fallbackText="-" />
