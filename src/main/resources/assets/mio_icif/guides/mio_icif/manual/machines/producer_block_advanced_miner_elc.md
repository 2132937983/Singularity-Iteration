---
navigation:
  title: "Advanced Miner"
  icon: mio_icif:producer/block_advanced_miner_elc
  parent: machines.md
  position: 6
item_ids:
  - mio_icif:producer/block_advanced_miner_elc
---

# Advanced Miner

<Row>
  <BlockImage id="mio_icif:producer/block_advanced_miner_elc" scale="3" />
</Row>

## Function

The Advanced Miner mines ores below the machine without Mining Pipe or a drill.
An OD Scanner gives a radius of 16 blocks. An OV Scanner gives 32 blocks. Without a scanner the machine mines straight down.
The machine puts the drops into adjacent inventories. When no inventory has space, the drops fall above the machine.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 4,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an HV cable to the machine.
2. Put an OD Scanner or an OV Scanner into the scanner slot.
3. Place a chest next to the machine to collect the ores.
4. Set the filter list, the whitelist or blacklist mode and Silk Touch in the GUI.

## Notes

- The Reset button starts the mining position again from the top.
- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above HV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_advanced_miner_elc" fallbackText="-" />
