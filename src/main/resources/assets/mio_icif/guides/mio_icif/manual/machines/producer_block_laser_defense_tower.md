---
navigation:
  title: "Laser Defense Tower"
  icon: mio_icif:producer/block_laser_defense_tower
  parent: machines.md
  position: 38
item_ids:
  - mio_icif:producer/block_laser_defense_tower
---

# Laser Defense Tower

<Row>
  <BlockImage id="mio_icif:producer/block_laser_defense_tower" scale="3" />
</Row>

## Function

The Laser Defense Tower fires laser beams at up to 5 targets in line of sight. Each hit costs EU.
The default scan box reaches 16 blocks in each direction. The GUI extends the box up to 32 blocks.
Blacklist mode attacks hostile mobs except listed ones. Whitelist mode attacks only listed mobs, tags and players.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | MV (128 EU) |
| Maximum input | 128 EU/t |
| Energy storage | 250,000 EU |
| Use while working | 1,250 EU/t |
| Operation time | 100 tick (5 s) |
| Energy per operation | 125,000 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Place the tower at a position with a clear view of the area.
2. Connect an MV cable to the machine.
3. Open the GUI and set the horizontal and vertical range.
4. Select the targeting mode and add entries to the list.

## Notes

- The upgrade bay accepts Overclocker, Transformer, Energy Storage and Redstone Signal Inverter Upgrades.
- A redstone signal switches the tower off.

> **WARNING:** The machine explodes on overvoltage above MV. Install a Transformer Upgrade for a higher voltage tier.


## Recipe

<RecipesFor id="mio_icif:producer/block_laser_defense_tower" fallbackText="-" />
