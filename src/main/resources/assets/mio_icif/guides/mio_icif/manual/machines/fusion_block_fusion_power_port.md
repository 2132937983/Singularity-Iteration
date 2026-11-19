---
navigation:
  title: "Fusion Power Output Port"
  icon: mio_icif:fusion/block_fusion_power_port
  parent: heavy.md
  position: 41
item_ids:
  - mio_icif:fusion/block_fusion_power_port
---

# Fusion Power Output Port

<Row><BlockImage id="mio_icif:fusion/block_fusion_power_port" scale="3" /></Row>

## Function

Sends the [fusion reactor's](fusion_block_fusion_controller.md) output into the energy network. Put it in a port slot of the reactor; it serves the console while the structure is formed. With several power output ports the output is shared evenly among them.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | the lowest tier whose 64 packets carry its share: IV (8,192 EU) up to 524,288 EU/t, then LuV, ZPMV, UV, UHV (2,097,152 EU) |
| Packets per tick | up to 64 |
| Energy storage | 39,200,000 EU |

## Notes

- One port at the full 39,200,000 EU/t sends UHV packets: only the Superconducting Cable and machines of that tier take them. Two or four ports send UV packets, eight ZPMV.
- An unformed port still sends out the EU it holds.

## Recipe

<RecipesFor id="mio_icif:fusion/block_fusion_power_port" fallbackText="-" />
