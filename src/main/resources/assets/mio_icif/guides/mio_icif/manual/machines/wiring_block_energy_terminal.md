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

## Power data

This block uses no EU.

## Procedure

1. Place the terminal next to a cable of the network.
2. Right-click the terminal to open the GUI.
3. Select Local to show this network or Global to include networks behind transformers.
4. Click the switch of a device to cut the device from the network.

## Recipe

<RecipesFor id="mio_icif:wiring/block_energy_terminal" fallbackText="-" />
