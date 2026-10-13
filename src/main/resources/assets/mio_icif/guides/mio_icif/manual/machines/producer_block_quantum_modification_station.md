---
navigation:
  title: "Universal Suit Modification Station"
  icon: mio_icif:producer/block_quantum_modification_station
  parent: suit/index.md
  position: 0
item_ids:
  - mio_icif:producer/block_quantum_modification_station
---

# Universal Suit Modification Station

<Row>
  <BlockImage id="mio_icif:producer/block_quantum_modification_station" scale="3" />
</Row>

## Function

The station installs upgrade units into quantum and nano suit pieces. Each installation uses EU.
A quantum piece counts units: helmet 4, chestplate 3, leggings 2, boots 2.
A nano piece has a nano lattice and counts lattice points (LP): helmet 4, chestplate 6, leggings 4, boots 4.
Each nano unit takes 1 to 3 LP. A Nano Lattice Densifier adds 2 LP to a nano piece, at most twice per piece.
Some units fit only one suit class. The tooltip of each unit shows its class and its LP.

## Power data

| Item | Value |
| Input voltage tier | HV (512 EU) |
| Maximum input | 512 EU/t |
| Energy storage | 40,000 EU |
| Use while working | 64 EU/t |
| Operation time | 100 tick (5 s) |
| Energy per operation | 6,400 EU |

## Procedure

1. Connect an HV cable to the station.
2. Put a quantum or nano suit piece in the top slot.
3. Put a unit or a lattice densifier in the lower slot.
4. Wait until the station finishes the installation.
5. Click the x next to an installed unit to remove the unit.
6. Switch the unit on in the Singularity Management Panel.

## Notes

- For a nano piece, the screen shows the lattice bar. Each cell is one LP in the colour of the unit that uses it. The unit in the lower slot blinks where it would go.
- Sensor units show data only through a suit visor.
- The removal of a unit costs no EU and gives the unit back. A densifier stays in the piece.

## Recipe

<RecipesFor id="mio_icif:producer/block_quantum_modification_station" fallbackText="-" />
