---
navigation:
  title: "Steam Tunnel Power Port"
  icon: mio_icif:generator/block_steam_tunnel_power_port
  parent: generators.md
  position: 33
item_ids:
  - mio_icif:generator/block_steam_tunnel_power_port
---

# Steam Tunnel Power Port

<Row>
  <BlockImage id="mio_icif:generator/block_steam_tunnel_power_port" scale="3" />
</Row>

## Function

The power port sends the EU of a [Steam Tunnel](generator_block_steam_tunnel.md). Put it in the middle of a long side or in the centre of the back of the tunnel.

## Power data

| Item | Value |
|---|---|
| Output voltage tier | HV (512 EU) |
| Output | up to 2,048 EU/t (4 packets a tick) |
| Energy storage | 4,096 EU |

- It sends EU only while its tunnel is formed. Several power ports share the tunnel's output.

## Recipe

<RecipesFor id="mio_icif:generator/block_steam_tunnel_power_port" fallbackText="-" />
