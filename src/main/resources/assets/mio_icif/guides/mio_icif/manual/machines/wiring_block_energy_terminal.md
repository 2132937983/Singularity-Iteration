---
navigation:
  title: "Energy Management Terminal"
  icon: mio_icif:wiring/block_energy_terminal
  parent: power.md
  position: 11
item_ids:
  - mio_icif:wiring/block_energy_terminal
---

# Energy Management Terminal

<Row>
  <BlockImage id="mio_icif:wiring/block_energy_terminal" scale="3" />
</Row>

## Function

The terminal monitors generation, consumption and storage of the attached cable network.
The terminal uses no EU and adds no load to the network.
The GUI can switch off a device. A switched-off device has no input and no output.

Since 0.1.10.22 the Panel button opens the grid monitoring panel (full screen, four tabs):

- **Overview**: generation, consumption, storage and balance, the trends, the supply by source class (must-run, fuel, storage), the critical loads and the engine time of this network.
- **Diagram**: one bus per sub-network, with transformers, storage blocks and open breakers between the buses.
- **Devices**: every device. Set the priority of a machine or storage block here: critical, normal or deferrable. When the energy is too little, deferrable devices stop first and critical devices last.
- **Protection**: breaker and fuse cables with their state and settings (Reset for a breaker), and the alarm log: trips, lockouts, blown fuses, melted cables, exploded machines and critical loads without energy.

## Power data

This block uses no EU.

## Procedure

1. Place the terminal next to a cable of the network.
2. Right-click the terminal to open the GUI.
3. Select Local to show this network or Global to include networks behind transformers.
4. Click the switch of a device to cut the device from the network.
5. Click Panel for the monitoring panel. Click the priority of a device to change it.

## Recipe

<RecipesFor id="mio_icif:wiring/block_energy_terminal" fallbackText="-" />
