---
navigation:
  title: "EU Detector Cable"
  icon: mio_icif:wiring/block_eu_detector_cable
  parent: power.md
  position: 9
item_ids:
  - mio_icif:wiring/block_eu_detector_cable
---

# EU Detector Cable

<Row>
  <BlockImage id="mio_icif:wiring/block_eu_detector_cable" scale="3" />
</Row>

## Function

The EU Detector Cable is an IV cable that measures the EU flow through the cable.
The cable emits a full redstone signal while EU flows.
A comparator reads a signal strength that increases with the flow.

## Power data

This block uses no EU.

## Procedure

1. Replace one cable block in the line with the EU Detector Cable.
2. Place a redstone lamp next to the detector cable.
3. Place a comparator next to the detector cable to read the flow level.

## Notes

- The signal updates with a short delay.

## Recipe

<RecipesFor id="mio_icif:wiring/block_eu_detector_cable" fallbackText="-" />
