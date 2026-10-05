---
navigation:
  title: "Metal Former"
  icon: mio_icif:producer/block_metal_former
  parent: machines.md
  position: 43
item_ids:
  - mio_icif:producer/block_metal_former
---

# Metal Former

<Row>
  <BlockImage id="mio_icif:producer/block_metal_former" scale="3" />
</Row>

## Function

The Metal Former uses EU to shape metal in three modes.
Rolling mode makes plates and casings. Cutting mode makes cables from plates. Extruding mode makes cables, shafts and cans.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 32 EU/t |
| Energy storage | 2,000 EU |
| Use while working | 10 EU/t |
| Operation time | 200 tick (10 s) |
| Energy per operation | 2,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an LV cable to the machine.
2. Click the mode button in the GUI to select Rolling, Cutting or Extruding.
3. Put the ingot or the plate into the input slot.
4. Take the result from the output slot.

## Notes

- A redstone signal stops the machine. A Redstone Signal Inverter Upgrade reverses this.

> **WARNING:** The machine explodes on overvoltage above LV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_metal_former" fallbackText="-" />
