---
navigation:
  title: "Tesla Coil"
  icon: mio_icif:producer/block_tesla
  parent: machines.md
  position: 66
item_ids:
  - mio_icif:producer/block_tesla
---

# Tesla Coil

<Row>
  <BlockImage id="mio_icif:producer/block_tesla" scale="3" />
</Row>

## Function

The Tesla Coil uses EU to damage every living entity within 9 blocks of the coil.
The coil attacks at a fixed interval while it receives a redstone signal and holds enough EU.
The first hit on a target does more damage. Each hit also damages armor.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 10,000 EU |
| Use while working | 500 EU/t |
| Operation time | 100 tick (5 s) |
| Energy per operation | 50,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the Tesla Coil in the area to protect.
2. Connect an MV cable to the machine.
3. Let the coil store enough EU.
4. Apply a redstone signal to activate the coil.

## Notes


> **WARNING:** The coil also attacks players and tamed animals in range.


> **WARNING:** The machine explodes on overvoltage above MV.


## Recipe

<RecipesFor id="mio_icif:producer/block_tesla" fallbackText="-" />
