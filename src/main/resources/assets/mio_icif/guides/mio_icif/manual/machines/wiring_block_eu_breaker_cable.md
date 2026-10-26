---
navigation:
  title: "EU Breaker Cable"
  icon: mio_icif:wiring/block_eu_breaker_cable
  parent: power.md
  position: 10
item_ids:
  - mio_icif:wiring/block_eu_breaker_cable
---

# EU Breaker Cable

<Row>
  <BlockImage id="mio_icif:wiring/block_eu_breaker_cable" scale="3" />
</Row>

## Function

The EU Breaker Cable is an IV cable with a circuit breaker. It protects the line behind it.

- **Overload**: above the rated flow the breaker opens after a delay. At twice the rated flow it opens after the set delay; a larger overload opens it sooner, ten times the rated flow at once.
- **Overvoltage**: a packet above the rated voltage opens it at once, before the packet can melt cables or explode machines behind it.
- **Reclosing**: the breaker closes again after 3 s. After the fourth trip in a row it stays open (locked out) until you reset it.
- A redstone signal holds it open, like the splitter cable.

Set a shorter delay on the breakers near the machines than on the breaker of the main line. Then a fault opens only the nearest breaker.

## Power data

This block uses no EU.

## Procedure

1. Replace one cable block of the line with the breaker cable.
2. Right-click it with an empty hand to open its settings.
3. Set the rated flow (EU/t), the rated voltage, the delay and the automatic reclosing. Click Apply.
4. After a lockout, click Reset in the settings or in the monitoring panel of the energy management terminal.

## Recipe

<RecipesFor id="mio_icif:wiring/block_eu_breaker_cable" fallbackText="-" />
