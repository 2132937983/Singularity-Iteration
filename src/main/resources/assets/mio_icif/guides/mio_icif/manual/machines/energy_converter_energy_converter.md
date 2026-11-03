---
navigation:
  title: "Grid-Tie Interface"
  icon: mio_icif:energy_converter/energy_converter
  parent: machines.md
  position: 24
item_ids:
  - mio_icif:energy_converter/energy_converter
---

# Grid-Tie Interface

> **0.1.11.1.** The Energy Converter is now the grid-tie interface (same block). A placed converter keeps its direction and its mode (export or import); its FE buffer joins the EU buffer.

<Row>
  <BlockImage id="mio_icif:energy_converter/energy_converter" scale="3" />
</Row>

## Function

The grid-tie interface connects the SI grid to the FE network of another mod. The front face connects to the other network (FE). The other five faces connect to the SI grid (EU). The rate is 4 FE = 1 EU, with no loss.
SI cables do not take FE and do not connect to the cables of other mods. The grid-tie interface is the connection between the two networks.

- Export: the interface takes the SI surplus after the loads, and gives it as FE at the front.
- Import: the interface takes FE at the front, and supplies the SI grid as a generator.
- Bidirectional: the interface keeps its buffer near half charge. Above the dead band it gives FE at the front. Below the dead band it takes FE at the front. On the SI side it takes only the surplus of generators and supplies only loads. Thus energy does not go back and forth through SI storage.
- The limit (32, 128, 512 or 2,048 EU/t) applies to each direction. The SI input and the FE input share one limit per tick. The SI output and the FE output share one limit per tick.

## Power data

| Item | Value |
|---|---|
| Input voltage tier | EV (2,048 EU) |
| Maximum input and output | 32, 128, 512 or 2,048 EU/t (limit) |
| Energy storage | 200,000 EU |
| Dead band (bidirectional) | 10, 25 or 50 % of the buffer, around half charge |

## Procedure

1. Put the interface with the front face against the cable or the block of the other mod.
2. Connect SI cables to the other faces.
3. Sneak and right-click the interface to select the mode: export, import, bidirectional 10 %, 25 % or 50 %.
4. Right-click the interface to select the limit.

## Notes


> **WARNING:** The machine explodes on overvoltage above EV.

- The front face does not connect to SI cables or SI machines.
- `/mio_icif perf` shows the FE per tick that grid-tie interfaces move.


## Recipe

<RecipesFor id="mio_icif:energy_converter/energy_converter" fallbackText="-" />
