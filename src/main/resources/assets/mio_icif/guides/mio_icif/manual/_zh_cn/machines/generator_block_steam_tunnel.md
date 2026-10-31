---
navigation:
  title: "蒸汽隧道涡轮机"
  icon: mio_icif:generator/block_steam_tunnel
  parent: generators.md
  position: 32
item_ids:
  - mio_icif:generator/block_steam_tunnel
  - mio_icif:generator/block_steam_tunnel_casing
  - mio_icif:generator/block_steam_tunnel_fluid_port
  - mio_icif:generator/block_steam_turbine_rotor_bronze
  - mio_icif:generator/block_steam_turbine_rotor_iron
  - mio_icif:generator/block_steam_turbine_rotor_adviron
  - mio_icif:generator/block_steam_turbine_rotor_titanium
  - mio_icif:generator/block_steam_turbine_rotor_iridium
  - mio_icif:generator/block_steam_turbine_rotor_netherite
---

# 蒸汽隧道涡轮机

<Row>
  <BlockImage id="mio_icif:generator/block_steam_tunnel" scale="3" />
  <BlockImage id="mio_icif:generator/block_steam_tunnel_casing" scale="3" />
  <BlockImage id="mio_icif:generator/block_steam_tunnel_fluid_port" scale="3" />
  <BlockImage id="mio_icif:generator/block_steam_tunnel_power_port" scale="3" />
</Row>

## 功能

IC2 经典版的大型蒸汽涡轮：3 x 3 x 5 的空心结构，在一处把大量蒸汽转为 EU。

- 每 tick 把 1,024 mB 蒸汽转为 512 EU，最多 4 次（4,096 mB 转为 2,048 EU），即与 <ItemLink id="mio_icif:generator/block_steam_turbine" /> 相同的每 EU 2 mB。与 IC2 经典版一样，只要 10,000 EU 缓存还放得下一次（512 EU）就运行，并按蒸汽量做满所有次数，因此缓存最多可超出 1,536 EU。内部放入蒸汽涡轮转子可提高每步发电量（见“转子”）。
- 每 100 mB 蒸汽返还 1 mB 水，与蒸汽反应堆煮水的比例相同，因此反应堆循环不损失水（IC2 经典版按它自己的 1:160 比例做同样的事）。水箱容量 2,000 mB，放不下的水会损失。
- 控制器没有界面。右键它可查看状态（运行、待机，或缺少什么、在哪里），也可用 Jade 查看（蒸汽、水、EU 缓存、输出）。

## 电力数据

| 项目 | 数值 |
|---|---|
| 输出电压等级 | HV（512 EU），由电力端口输出 |
| 输出 | 无转子 2,048 EU/t，3 个下界合金转子时最多 3,968 EU/t（每 tick 4～8 个 512 EU 的 HV 数据包） |
| 蒸汽箱 | 16,000 mB |
| 水箱 | 2,000 mB |
| 能量缓存 | 10,000 EU |

## 结构

- 控制器位于前端中心并朝外，隧道在它后面。
- 28 个棱方块为外壳。其余 13 个外壳位置（四个长侧面的中间与后端中心）可放外壳、流体端口或电力端口。内部 3 格为空气或蒸汽涡轮转子。
- 拆除任一部件立即停机；结构完整后自动恢复（每秒检查一次）。
- 相邻的外壳与端口会连成一整块面板。结构成形时，前端显示一个完整的大风扇：控制器是中心的轴，周围 8 个外壳组成扇叶。运行时风扇转动并吹出蒸汽；风扇消失说明结构已不完整。

拖动视图可旋转。

<GameScene zoom="4" interactive={true}>
  <SiStructure id="mio_icif:steam_tunnel" />
</GameScene>

<SiStructureInfo id="mio_icif:steam_tunnel" />

## 转子

隧道内部的 3 格最多可放 3 个蒸汽涡轮转子。每个转子使每一步多产 EU，每步消耗的蒸汽与返还的水不变。不同等级可以混用，也可以留空：不放转子时与以前相同，每 1,024 mB 产生 512 EU。

<Row>
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_bronze" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_iron" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_adviron" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_titanium" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_iridium" scale="2" />
  <BlockImage id="mio_icif:generator/block_steam_turbine_rotor_netherite" scale="2" />
</Row>

| 转子 | 每步增加 | 放满 3 个时 |
|---|---|---|
| 青铜 | +32 (+6.25%) | 608 EU / 1,024 mB |
| 铁 | +64 (+12.5%) | 704 EU / 1,024 mB |
| 精炼铁 | +80 (+15.625%) | 752 EU / 1,024 mB |
| 钛 | +96 (+18.75%) | 800 EU / 1,024 mB |
| 铱 | +128 (+25%) | 896 EU / 1,024 mB |
| 下界合金 | +160 (+31.25%) | 992 EU / 1,024 mB（3,968 EU/t） |

- 一个电力端口即可送出全部输出：按转子自动每 tick 发送 4～8 个 512 EU 的 HV 数据包（最多 3,968 EU/t）。
- 放入或拆除转子立即生效，转子可朝 6 个方向放置。Jade 显示内部的转子、蒸汽用量（满功率需 4,096 mB/t）与最大输出，并在等待蒸汽或缓存已满时说明；右键控制器同样会说明，并显示每步 EU。
- 转子由 4 个该材质的转子扇叶放在合成格四角、一根轴放在中间制成（X 形；十字形做出的是动能转子）。下界合金转子由铱转子在锻造台用下界合金锭与下界合金升级锻造模板升级而成。

## 使用步骤

1. 用外壳搭建结构（切石机用 1 个基础机械外壳切出 1 个蒸汽隧道外壳）。
2. 放置至少一个电力端口，并连接 HV 电缆。
3. 把蒸汽通入流体端口或控制器。流体端口也会把水推入相邻的管道与储罐。
4. 右键流体端口打开界面。与流体核反应堆的流体端口相同，可放入流体弹出升级（按升级设置的方向与速率输出水）或流体抽入升级（从相邻的储罐抽入蒸汽）。

## 注意事项

- 一座装有四联燃料棒与 4 个超频蒸汽散热片的蒸汽反应堆每 tick 约产 192 mB 蒸汽，而本机每 tick 最多处理 4,096 mB。它适合把多座反应堆的蒸汽集中到一处，并不更省蒸汽。
- 多个电力端口共享输出。

## 配方

<RecipesFor id="mio_icif:generator/block_steam_tunnel" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_casing" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_fluid_port" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_tunnel_power_port" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_bronze" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_iron" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_adviron" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_titanium" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_iridium" fallbackText="-" />
<RecipesFor id="mio_icif:generator/block_steam_turbine_rotor_netherite" fallbackText="-" />
