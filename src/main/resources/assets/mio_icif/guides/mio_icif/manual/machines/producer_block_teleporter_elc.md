---
navigation:
  title: "Teleporter"
  icon: mio_icif:producer/block_teleporter_elc
  parent: machines.md
  position: 64
item_ids:
  - mio_icif:producer/block_teleporter_elc
---

# Teleporter

<Row>
  <BlockImage id="mio_icif:producer/block_teleporter_elc" scale="3" />
</Row>

## Function

The Teleporter sends the entity on top of the pad to the linked Teleporter.
Each teleport costs EU. A longer distance costs more EU.
The Teleporter has no GUI. The machine works only while it receives a redstone signal.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 8,192 EU/t |
| Energy storage | 100,000,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place two Teleporters and connect an HV cable to each.
2. Right-click the first Teleporter with a Frequency Transmitter.
3. Right-click the second Teleporter to complete the link.
4. Apply a redstone signal to the Teleporter.
5. Stand on the Teleporter to travel to the linked Teleporter.

## Notes

- The link works in both directions.

> **WARNING:** The machine explodes on overvoltage above HV.


## Recipe

<RecipesFor id="mio_icif:producer/block_teleporter_elc" fallbackText="-" />
