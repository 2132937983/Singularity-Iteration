---
navigation:
  title: "Frame cores"
  icon: mio_icif:module/item_core_propulsion
  parent: suit/index.md
  position: 2
item_ids:
  - mio_icif:module/item_core_solar_array_1
  - mio_icif:module/item_core_solar_array_2
  - mio_icif:module/item_core_solar_array_3
  - mio_icif:module/item_core_graviton
  - mio_icif:module/item_core_propulsion
---

# Frame cores

A frame core sits in the **core socket** of a suit helmet or chestplate. The socket is separate from the unit slots (quantum) and the lattice (nano), so a core never takes the place of a unit.

| Core | Socket | Effect |
|---|---|---|
| <ItemLink id="mio_icif:module/item_core_solar_array_1" /> | Nano / quantum helmet | 8 EU/t day, 1 EU/t night (open sky) |
| <ItemLink id="mio_icif:module/item_core_solar_array_2" /> | Nano / quantum helmet | 64 EU/t day, 8 EU/t night |
| <ItemLink id="mio_icif:module/item_core_solar_array_3" /> | Quantum helmet | 512 EU/t day, 64 EU/t night |
| <ItemLink id="mio_icif:module/item_core_graviton" /> | Quantum chestplate | Gravity anchor (no fall damage, no knockback, no blast push), armour +1, emergency repair below 20 % health |
| <ItemLink id="mio_icif:module/item_core_propulsion" /> | Quantum chestplate | +90,000,000 EU, no flight ceiling, thrust +15 %, sharper maneuver vectoring, armour +1, knockback resistance +0.5, repair 2.0 health/s, arc discharge |

## Install, swap, remove

Put the piece and the core into the <ItemLink id="mio_icif:producer/block_quantum_modification_station" /> (the core goes into the unit slot). Installing takes the usual install time and power.
A core in a piece that already has one swaps them: the old core comes back into the unit slot and waits there until you take it.
The core socket box of the station screen shows the core; click the small eject button next to it to take the core out (it goes to you).
A piece with a core shows a small diamond in the core colour in the corner of its icon.

## Exterior

Each core adds its exterior to the worn piece: a sun panel on a mast, a winged panel, the sun crown with a fan, the graviton ring with a gyro and shoulder pads, or twin boosters with a chest reactor.
The solar wings open in daylight under open sky and fold in the dark; the graviton ring and gyro turn while the suit has energy; the propulsion nozzles glow with the thrust.
Turn it off for one piece with its "Core exterior (3D)" switch in the Singularity Management Panel, or for everything in the **Look** tab (or the client option core_attachments).

## Core burst

With the Advanced Propulsion core, press **R** (core burst): a high-voltage arc discharge hits every creature around you and a shock wave throws them back. It costs 1,500,000 EU and has an 8 s cooldown (server config section FrameCores).

## Retired items

0.1.9.0 retired the solar helmets, the advanced quantum chestplate and the heavy quantum chestplate. Your old items are not lost: they turn into the standard piece with the matching core as soon as you carry them, in the modification station, on a Wingman, or when you craft one alone.

| Old item | Becomes |
|---|---|
| Solar Helmet | a Solar Skin unit |
| Advanced Solar Helmet | nano helmet + Solar Array I |
| Hybrid Solar Helmet | quantum helmet + Solar Array II |
| Ultimate Hybrid Solar Helmet | quantum helmet + Solar Array III |
| Heavy Quantum Chestplate | quantum chestplate + Graviton Bulwark |
| Advanced Quantum Chestplate | quantum chestplate + Advanced Propulsion |

The stored energy, units, switches, enchantments and name stay on the piece. The old 10 health/s repair of the advanced quantum chestplate is 2.0 health/s on the Advanced Propulsion core.
