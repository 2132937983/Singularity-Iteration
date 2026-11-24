---
navigation:
  title: "Heavy Weapons"
  icon: mio_icif:item_tool_rocket_launcher
  parent: tools.md
  position: 6
item_ids:
  - mio_icif:item_tool_rocket_launcher
  - mio_icif:item_rocket
  - mio_icif:item_tool_plasma_launcher
  - mio_icif:item_tool_plasma_air_cannon
---

# Heavy Weapons

## Rocket launcher

The <ItemLink id="mio_icif:item_tool_rocket_launcher" /> uses one <ItemLink id="mio_icif:item_rocket" /> and 20,000 EU a shot, one shot a second. The rocket falls with gravity; its blast has power 3 (about 43 damage at the centre). Creative keeps its rockets.

## Plasma launcher

The <ItemLink id="mio_icif:item_tool_plasma_launcher" /> clears paths and areas. 2,000 EU a shot, one shot a second, 20 shots a full charge.

- The bolt smelts smeltable blocks on its way, clears water and lava and lights flammable blocks.
- On a hit it makes a heat blast that breaks only weak blocks; entities take about 24 at the centre, less farther out. The shooter is spared.

## Plasma air cannon

Hold the <ItemLink id="mio_icif:item_tool_plasma_air_cannon" /> to charge and release to fire a cone of air (15° half angle), mostly to knock back. Damage 4–10 by charge, halved at the far end. 1,000 EU a shot. Tamed animals, villagers, golems and team mates are spared.

## Terrain damage

The server setting `Weapons.weaponsDamageTerrain` (on by default) decides whether the electric plasma gun, rockets and the plasma launcher break blocks. Off, they only hurt entities. Claims always apply.
