---
navigation:
  title: "Miner"
  icon: mio_icif:producer/block_miner_elc
  parent: machines.md
  position: 44
item_ids:
  - mio_icif:producer/block_miner_elc
---

# Miner

<Row>
  <BlockImage id="mio_icif:producer/block_miner_elc" scale="3" />
</Row>

## Function

The Miner pushes Mining Pipe down into the ground and mines the ores on each layer.
The machine needs a drill, Mining Pipe and an OD Scanner or OV Scanner.
The OD Scanner gives a radius of 3 blocks around the pipe. The OV Scanner gives 6 blocks.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 10,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the machine.
2. Put an Iron Drill, a Diamond Drill, an Iridium Drill or a Mining Laser into the drill slot. The Mining Laser must be in Mining mode or Super Heat mode.
3. Put Mining Pipe into the pipe slot.
4. Put an OD Scanner or an OV Scanner into the scanner slot.
5. Take the ores from the storage slots.

## Notes

- Place a Pump next to the Miner to remove the fluids in the shaft.
- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.
- To get the Mining Pipe back, push the Retract button or take the drill out. The machine pulls up one pipe every 5 ticks for 5 EU.
- The Mining Laser mines 5 times faster than a drill and uses 250 EU of its charge for each route. In Super Heat mode, the machine smelts the ores.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_miner_elc" fallbackText="-" />
