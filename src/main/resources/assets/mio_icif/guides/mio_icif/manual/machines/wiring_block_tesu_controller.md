---
navigation:
  title: "TESU Controller"
  icon: mio_icif:wiring/block_tesu_controller
  parent: power.md
  position: 17
item_ids:
  - mio_icif:wiring/block_tesu_controller
---

# TESU Controller

<Row>
  <BlockImage id="mio_icif:wiring/block_tesu_controller" scale="3" />
</Row>

## Function

The TESU Controller is the core of the TESU (Tera Energy Storage Unit), a 7×7×13 storage box. It stores the energy;
the input and output ports in the base ring move it in and out. It faces out of the front of the structure, and the structure
turns with it. Its own faces take and give nothing: connect cables to the ports.

## Power data

| Item | Value |
|---|---|
| Energy storage | 227,000,000,000 EU |

## Procedure

1. Build the structure (see below) and place the controller in the middle of the front of the base ring.
2. Put TESU Input Ports, TESU Output Ports and TESU Wireless Output Ports in the port slots; close the others with TESU Casing.
3. Open the GUI: it shows the stored energy, the ports and their output, or the first wrong block and what belongs there.

## Notes

- The controller checks the structure every second.
- The GUI has a slot to charge an item and a slot to take EU from a battery.

## Structure

| Layer | Blocks |
|---|---|
| 1 | Base: a 7×7 layer of TESU Casing |
| 2 | The controller in the middle of the front, 8 port slots (2 on each side), the rest casing |
| 3–11 | Shell: casing walls with a Reinforced Glass window three blocks wide in the middle of each side; inside, a 3-5-5-5-3 disc of TESU Energy Cells (21 a layer, 189 in all) and casing in the four inner corners |
| 12 | Lid: a 7×7 layer of casing |
| 13 | Top: a 7×7 layer of casing |

Drag the view to turn it. The list gives the blocks and their rules. JEI shows the same plan in the category "Multiblock Structures".

<GameScene zoom="2" interactive={true}>
  <SiStructure id="mio_icif:tesu" />
</GameScene>

<SiStructureInfo id="mio_icif:tesu" />

## Recipe

<RecipesFor id="mio_icif:wiring/block_tesu_controller" fallbackText="-" />
