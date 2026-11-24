---
navigation:
  title: "Scanners and Multimeter"
  icon: mio_icif:item_tool_od_scanner
  parent: tools.md
  position: 4
item_ids:
  - mio_icif:item_tool_od_scanner
  - mio_icif:item_tool_ov_scanner
  - mio_icif:item_tool_meter
  - mio_icif:item_tool_windmeter
  - mio_icif:item_geomagnetic_detector
  - mio_icif:item_tool_area_scanner
---

# Scanners and Multimeter

## Ore scanners

| Scanner | Area | EU per scan | Capacity |
|---|---|---|---|
| <ItemLink id="mio_icif:item_tool_od_scanner" /> | 13×13×13 | 48 EU | 100,000 EU |
| <ItemLink id="mio_icif:item_tool_ov_scanner" /> | 25×13×25 | 96 EU | 1,000,000 EU |

- Right-click to scan. The screen lists each ore kind with its count and the direction and distance of its nearest block.
- Only blocks in `c:ores` count, grouped by their `c:ores/<kind>` tag.
- The OV scanner is crafted from an OD scanner; the charge carries over. They are also the Miner's two scanner tiers.

## Multimeter

The <ItemLink id="mio_icif:item_tool_meter" /> needs no energy and has three probes (G switches):

- **Energy.** Right-click a grid device to open its data screen; sneak + right-click switches the reading (input, output, throughput and others).
- **Wind.** Right-click a wind kinetic generator for its effective wind; right-click elsewhere for the wind at your height. The wind direction and weather factor come with it.
- **Geomagnetic.** Right-click a block for the geomagnetic strength there and the current Kp index.

The <ItemLink id="mio_icif:item_tool_windmeter" /> and the <ItemLink id="mio_icif:item_geomagnetic_detector" /> are replaced: put one alone in a crafting grid to get a multimeter.

## Area scanner

The <ItemLink id="mio_icif:item_tool_area_scanner" /> shows a machine's working area in the world when you right-click the machine. Sneak + right-click clears it.
