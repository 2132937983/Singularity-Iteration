---
navigation:
  title: "Pattern Storage"
  icon: mio_icif:producer/block_pattern_storage
  parent: machines.md
  position: 52
item_ids:
  - mio_icif:producer/block_pattern_storage
---

# Pattern Storage

<Row>
  <BlockImage id="mio_icif:producer/block_pattern_storage" scale="3" />
</Row>

## Function

The Pattern Storage holds up to 64 item patterns for UU-Matter replication.
The machine imports and exports patterns with a Pattern Storage Crystal. Each operation costs EU.
An adjacent Pattern Scanner saves new patterns here. An adjacent Replicator uses the selected pattern.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | LV (32 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 100,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the Pattern Storage between a Pattern Scanner and a Replicator.
2. Connect an LV cable to the machine.
3. Use the arrow buttons in the GUI to select a pattern.
4. Put a Pattern Storage Crystal into the slot to import or export a pattern.

## Notes


> **WARNING:** The machine explodes on overvoltage above LV.


## Recipe

<RecipesFor id="mio_icif:producer/block_pattern_storage" fallbackText="-" />
