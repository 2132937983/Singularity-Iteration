# 旧版纳米与量子套装资源（封存）

0.1.11.124（装甲与飞行升级 E1）封存。这些文件是 0.1.11.121 及以前游戏里用的纳米、量子套装模型和贴图，0.1.11.123 合并上游 PR 37 后已被定稿模型覆盖。这里只作留档，不会打包进模组，游戏也不会读取。

| 目录 | 内容 |
|---|---|
| `armor_models/` | 8 件穿戴模型（烘焙网格，`BakedArmorModel` 的格式） |
| `armor_models/attachments/` | 量子头盔的 3 种太阳能阵列、量子胸甲的引力核心和推进核心外挂；定稿量子套把核心做成了自身模型的挂件（`quantum_mounts.json`） |
| `models/item/armor/` | 8 件物品模型 |
| `textures/models/armor/` | 穿戴贴图（底色、镀层、强调色、发光层） |

要临时恢复旧外观：把对应文件复制回 `src/main/resources/assets/mio_icif/` 的同名路径。量子外挂还需要在 `CoreAttachmentLayer.baseName` 里恢复量子头盔和量子胸甲两行。旧的推进特效代码在 `common.client.legacy.flight`，由 `LegacyFlightFx.ENABLED` 控制。
