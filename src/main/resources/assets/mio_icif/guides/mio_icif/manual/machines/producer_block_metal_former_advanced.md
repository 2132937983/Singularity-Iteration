---
navigation:
  title: "Advanced Metal Former"
  icon: mio_icif:producer/block_metal_former_advanced
  parent: machines.md
  position: 5
item_ids:
  - mio_icif:producer/block_metal_former_advanced
---

# Advanced Metal Former

<Row>
  <BlockImage id="mio_icif:producer/block_metal_former_advanced" scale="3" />
</Row>

## Function

The Advanced Metal Former uses the Metal Former recipes at MV and completes each operation faster.
Rolling mode makes plates and casings. Cutting mode makes cables from plates. Extruding mode makes cables, shafts and cans.
The block texture shows the current mode.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 1,000 EU |
| Use while working | 50 EU/t |
| Operation time | 20 tick (1 s) |
| Energy per operation | 1,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an MV cable to the machine.
2. Click the mode button in the GUI to select Rolling, Cutting or Extruding.
3. Put the ingot or the plate into the input slot.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_metal_former_advanced" fallbackText="-" />
