---
navigation:
  title: "Quantum Modification Station"
  icon: mio_icif:producer/block_quantum_modification_station
  parent: suit/index.md
  position: 0
item_ids:
  - mio_icif:producer/block_quantum_modification_station
---

# Quantum Modification Station

<Row>
  <BlockImage id="mio_icif:producer/block_quantum_modification_station" scale="3" />
</Row>

## Function

The Quantum Modification Station installs upgrade units into quantum suit pieces. The station uses EU for each installation.
Available units: Seismic Ore Scanner, Grid Telemetry Sensor, Biometric Scanner, Ballistic Computer, Blast Warning Timer.
More units: Behavior Predictor, Tactical 3D Holomap, Threat Sensor, Quantum Deflector.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 40,000 EU |
| Use while working | 64 EU/t |
| Operation time | 100 tick (5 s) |
| Energy per operation | 6,400 EU |

The game measured these values from the block entity of this version.

## Procedure

1. Connect an HV cable to the station.
2. Put a quantum suit piece in the top slot.
3. Put an upgrade unit in the lower slot.
4. Wait until the station finishes the installation.
5. Click the x next to an installed unit to remove the unit.
6. Switch the unit on in the Equipment Console.

## Notes

- The helmet takes four units, the chestplate three, the leggings two and the boots two. Each unit fits only specific pieces.
- Sensor units show data only through a quantum helmet visor. Each active unit uses EU from its suit piece.
- The removal of a unit costs no EU and gives the unit back.

## Recipe

<RecipesFor id="mio_icif:producer/block_quantum_modification_station" fallbackText="-" />
