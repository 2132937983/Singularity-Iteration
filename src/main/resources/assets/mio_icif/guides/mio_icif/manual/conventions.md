---
navigation:
  title: "Units and voltage tiers"
  icon: mio_icif:item_tool_meter
  parent: index.md
  position: 1
---

# Units and voltage tiers

EU is the unit of electrical energy. EU/t is EU per game tick. One second has 20 ticks.
A cable carries EU in packets. The voltage tier limits the size of one packet.
A machine explodes when it receives a packet above its input voltage tier.
Use a transformer to change the voltage tier between a source and a machine.

| Voltage tier | Maximum packet |
|---|---|
| LV | 32 EU |
| MV | 128 EU |
| HV | 512 EU |
| EV | 2,048 EU |
| IV | 8,192 EU |
| LuV | 32,768 EU |
| ZPM | 131,072 EU |
