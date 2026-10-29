---
navigation:
  title: "Nano suit units"
  icon: mio_icif:armor/item_armor_nano_chestplate
  parent: suit/index.md
  position: 1
item_ids:
  - mio_icif:module/item_module_myofiber
  - mio_icif:module/item_module_thermoregulator
  - mio_icif:module/item_module_emp_emitter
  - mio_icif:module/item_module_optical_camo
  - mio_icif:module/item_module_kinetic_mesh
  - mio_icif:module/item_module_recon_cloud
  - mio_icif:module/item_module_jammer
  - mio_icif:module/item_module_thermal_camo
  - mio_icif:module/item_module_gecko_setae
  - mio_icif:module/item_module_shock_gel
  - mio_icif:module/item_module_heat_sink_fins
  - mio_icif:module/item_module_micro_capacitor
  - mio_icif:module/item_module_solar_skin
  - mio_icif:module/item_module_toxin_filter
  - mio_icif:module/item_module_self_repair
  - mio_icif:module/item_module_hydrophobic
  - mio_icif:module/item_module_kinetic_generator
  - mio_icif:module/item_nano_lattice_densifier
---

# Nano suit units

The nano suit takes upgrade units. Its units are light: a fully loaded nano suit draws about 10 EU/t.
Its strong effects are bursts. A burst uses the nano reserve and adds heat.

## Nano core

- **Reserve** (0 to 100): bursts use it. It refills by 2 points per second while you wear a nano piece with units. Each point costs 400 EU from the suit.
- **Heat** (0 to 100, more with heat-sink fins): each burst adds heat. The suit sheds 4 heat per second.
- **Overheat**: at the cap the suit overheats. Bursts lock until the heat falls below 40 % of the cap. Without fins you also move 20 % slower.

## Burst keys

Press **V** (primary) or **B** (secondary). The Singularity Management Panel assigns a burst unit to each key. "auto" picks: Kinetic Dash, Optical Camouflage, Kinetic Armor Mesh, EMP, Recon Cloud.
The Kinetic Armor Mesh and the Optical Camouflage cannot be on the two keys at the same time, and they never run together.

## Units

| Unit | Fits | LP | Power | Burst | Class |
|---|---|---|---|---|---|
| <ItemLink id="mio_icif:module/item_module_myofiber" /> | Leggings | 2 | 3 EU/t | Dash | Nano |
| <ItemLink id="mio_icif:module/item_module_thermoregulator" /> | Chestplate | 2 | 2 EU/t | - | Nano |
| <ItemLink id="mio_icif:module/item_module_emp_emitter" /> | Chestplate | 3 | - | EMP | Nano |
| <ItemLink id="mio_icif:module/item_module_optical_camo" /> | Chestplate | 3 | - | Camo | Nano |
| <ItemLink id="mio_icif:module/item_module_kinetic_mesh" /> | Chestplate | 2 | - | Mesh | Nano |
| <ItemLink id="mio_icif:module/item_module_recon_cloud" /> | Helmet | 2 | - | Recon | Nano |
| <ItemLink id="mio_icif:module/item_module_jammer" /> | Helmet | 2 | 4 EU/t | - | Nano |
| <ItemLink id="mio_icif:module/item_module_thermal_camo" /> | Chestplate | 2 | 2 EU/t | - | Nano |
| <ItemLink id="mio_icif:module/item_module_gecko_setae" /> | Boots | 2 | 1 EU/t | - | Nano |
| <ItemLink id="mio_icif:module/item_module_shock_gel" /> | Boots | 1 | - | - | Nano |
| <ItemLink id="mio_icif:module/item_module_heat_sink_fins" /> | Chest / Legs | 1 | - | - | Nano |
| <ItemLink id="mio_icif:module/item_module_micro_capacitor" /> | Any | 1 | - | - | Nano |
| <ItemLink id="mio_icif:module/item_module_solar_skin" /> | Helmet / Chest | 1 | +2 EU/t | - | Nano |
| <ItemLink id="mio_icif:module/item_module_toxin_filter" /> | Helmet | 1 | 1 EU/t | - | Shared |
| <ItemLink id="mio_icif:module/item_module_self_repair" /> | Any | 1 | 2000 EU / heal | - | Shared |
| <ItemLink id="mio_icif:module/item_module_hydrophobic" /> | Chest / Legs | 1 | - | - | Shared |
| <ItemLink id="mio_icif:module/item_module_kinetic_generator" /> | Leggings | 1 | +1 EU / block | - | Shared |
| <ItemLink id="mio_icif:module/item_module_threat_sensor" /> | Chestplate | 1 | 6 EU/t | - | Shared |
| <ItemLink id="mio_icif:module/item_module_blast_warning" /> | Helmet / Chest | 1 | 2 EU/t | - | Shared |

## Mk.II

Every unit made for the nano lattice has a Mk.II item: the Mk.I unit, a Living Circuit, four Degenerate Zero Matter and three family materials.
Mk.II is stronger (the item tooltip shows how) and draws 20 % less power. It takes the same lattice points.
To upgrade, put the piece in the station and one Mk.II item in the unit slot. The station replaces the Mk.I unit and gives it back.

## HF sync

An active nanosaber in the main hand syncs with the Nano Myofiber and the Kinetic Armor Mesh weave. Each Mk.I source gives one level, each Mk.II source two, at most three.
Level 1: attack speed +12 %, armour penetration 15 %. Level 2: +15 %, 22 %. Level 3: +18 %, 30 %. The blade trembles harder at each level.

## Nano to quantum

When you craft a quantum piece from a nano piece, shared units move onto the quantum piece. Nano-only units and the lattice densifiers come back as items.

## Server settings

The common config section NanoSuit has a switch for each unit, PvP switches (all on by default) and the numbers.

<ItemLink id="mio_icif:producer/block_quantum_modification_station" />
