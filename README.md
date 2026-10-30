# Thaumcraft: 神秘动力学（Thaumic Dynamics）

[![Latest Release](https://img.shields.io/github/v/release/2925539099/Thaumcraft-Thaumic-Dynamics?label=release)](https://github.com/2925539099/Thaumcraft-Thaumic-Dynamics/releases/latest)

基于神秘时代，为机械动力额外增添一些设备。NeoForge 1.21.1 / Create 6.0.x / 神秘时代 port.265+（必需前置）。

## 傀儡动力系统

8 台不同材质的傀儡动力系统，都是应力源；另有 1 个纯装饰的空框架。转速 / 容量按「傀儡动力设计」配置：

| 方块 | 转速 | 应力容量 | 每 RPM 容量 |
|---|---|---|---|
| 稻草傀儡动力系统 | 160 RPM | 128 SU | 0.8 |
| 木头傀儡动力系统 | 128 RPM | 256 SU | 2.0 |
| 血肉傀儡动力系统 | 160 RPM | 384 SU | 2.4 |
| 粘土傀儡动力系统 | 128 RPM | 512 SU | 4.0 |
| 油脂傀儡动力系统 | 256 RPM | 128 SU | 0.5 |
| 石头傀儡动力系统 | 64 RPM | 1024 SU | 16.0 |
| 铁卫傀儡动力系统 | 32 RPM | 2048 SU | 64.0 |
| 神秘傀儡动力系统 | 128 RPM | 4096 SU | 32.0 |
| 空傀儡动力系统框架 | — | — | 纯装饰；挖掘参数与设备一致，扳手可旋转、潜行扳手可拆卸 |

输出轴与正面垂直：`facing=north/south` 时东、西两面是输出端，`facing=east/west` 时南、北两面是输出端。

![铁卫傀儡动力系统](preview_block.png)

## 红石发动机

新增的应力源，输出由红石信号强度决定：

| 红石信号 | 转速 | 应力 |
|---|---|---|
| 0 | 0 RPM（停转） | 0 SU |
| 1 | 32 RPM | 128 SU |
| 8 | 144 RPM | 4160 SU |
| 15 | 256 RPM | 8192 SU |

1~15 之间按线性插值；应力容量仍然按“每 RPM”登记，所以网络里显示的总容量就是上表的 SU。

- 方块模型直接采用 `红石发动机.bbmodel` 的不完整外形（非满方块），正面的输出轴用与创造马达相同的 `SHAFT_HALF` 半轴，由方块实体渲染器按转速旋转；
- 转向逻辑与傀儡动力系统一致：扳手顶/底面旋转朝向，侧面翻转方向，同网络内的红石发动机一起翻转，放置时也会自动对齐相邻转向；
- 方块是非满方块：碰撞/轮廓形状按 bbmodel 的 13 个部件并集生成，再用 `VoxelShaper.forHorizontal` 按朝向旋转（参考 Create 应力表/转速表的做法），不再是整格碰撞；
- 方块属性与神秘方块（`thaumcraft:thaumium_block`）一致：金属地图色、硬度 4、爆炸抗性 20、石质音效、需要正确工具才掉落（镐）；
- 奥术工作台配方：
  ```
  神秘锭 / 红石块 / 神秘锭
  炼金构材 / 精密构件 / 传动杆
  神秘锭 / 神秘方块 / 神秘锭
  ```
  消耗风 10、秩序 20、水 30、地 30、火 10、混沌 10；
- 性能：信号缓存在方块实体里，只有红石信号真的变化时才重算；没有每 tick 逻辑，区块加载不会触发转速/网络重算。
## 操作

| 操作 | 效果 |
|---|---|
| **手持扳手**右键顶面/底面 | 旋转整机朝向 90°，应力输出端跟着转到另外两面 |
| **手持扳手**右键侧面 | 翻转输出转向（顺时针 ⇄ 逆时针），动作栏提示当前方向 |
| 空手右键 | 不改变转向 |
| 潜行 + 扳手 | 拆除 |

**放置对齐**：放下设备时自动检查输出轴两端相邻传动部件的转向；如果对面是逆时针，新设备直接以相同转向接入，避免一正一反的冲突。

**转向联动**：同一传动网络里所有傀儡动力系统会一起翻转转向（参考蒸汽引擎的 `movementDirection`）。

## 挖掘与掉落

- 硬度与石头一致（1.5），爆炸抗性 6.0；
- 同时登记在 `mineable/axe` 和 `mineable/pickaxe` 里，**斧头或镐**都能正常挖掘并掉落方块本身；
- 潜行 + 机械动力扳手仍然可以直接拆卸（保留原有行为）。

## 神秘时代研究书

同时安装神秘时代 4 移植版（port.265+）时，研究书会多出一个「神秘动力学」分类：

- 分类图标：`textures/research/tab_icon.png`（32×32）
- 分类背景：`textures/research/background.png`（512×512）
- 分类顺序 6，排在「神秘」之后
- 设备研究链：

| 研究 | 坐标 | 前置 | 解锁方式 |
|---|---|---|---|
| STRAW_GOLEM_POWER 稻草傀儡动力系统 | (0, 2) | GOLEMSTRAW | `secondary`，点击直接扣 motus 6 / instrumentum 3 / machina 3 |
| FLESH_GOLEM_POWER 血肉傀儡动力系统 | (-2, 4) | GOLEMFLESH + WOOD_GOLEM_POWER | 研究笔记（血肉笔记 + fabrico 3），warp 3 |
| WOOD_GOLEM_POWER 木头傀儡动力系统 | (0, 4) | GOLEMWOOD + STRAW_GOLEM_POWER | 研究笔记（木头笔记 + fabrico 3） |
| TALLOW_GOLEM_POWER 油脂傀儡动力系统 | (-2, 6) | GOLEMTALLOW + CLAY_GOLEM_POWER | 研究笔记（油脂笔记 + fabrico 3） |
| CLAY_GOLEM_POWER 粘土傀儡动力系统 | (0, 6) | GOLEMCLAY + WOOD_GOLEM_POWER | 研究笔记（粘土笔记 + fabrico 3） |
| STONE_GOLEM_POWER 石头傀儡动力系统 | (0, 8) | GOLEMSTONE + CLAY_GOLEM_POWER | 研究笔记（石头笔记 + fabrico 3） |
| IRON_GOLEM_POWER 铁卫傀儡动力系统 | (0, 10) | GOLEMIRON + STONE_GOLEM_POWER | 研究笔记（铁卫笔记 + fabrico 3） |
| MYSTERY_GOLEM_POWER 神秘傀儡动力系统 | (0, 12) | GOLEMTHAUMIUM + IRON_GOLEM_POWER | 研究笔记（神秘笔记 + fabrico 3） |

- 只有稻草带 `secondary`（默认难度 `research_difficulty = 0` 时点击直接扣点）；其余研究不带 `secondary`，点击会生成研究笔记，要素 = 对应傀儡研究要素 + `fabrico` 3
- 血肉研究的 `warp` 与傀儡学血肉研究一致，为 3
- (0, 14) 与 (0, 16) 仍为空位
研究内容全部是数据包 JSON，不需要编译期依赖神秘时代：

- `data/mystery_dynamics/research_category/mystery_dynamics.json`
- `data/mystery_dynamics/research/mystery_dynamics/mystery_test.json`

本地化键：`tc.research_category.MYSTERY_DYNAMICS`、`tc.research_name.MYSTERY_TEST`、`tc.research_text.MYSTERY_TEST`、`mystery_dynamics.research.test.text`。
## 奥术工作台配方
## 奥术工作台配方

以下 9 个配方会注册到奥术工作台，并作为 ARCANE 配方页显示在对应研究里：

| 产物 | 配方 | Vis 消耗 | 研究页 |
|---|---|---|---|
| 空傀儡动力系统框架 | 神秘锭 ×3 / 齿轮 · 安山机壳 · 齿轮 | 六种元要素各 5 | STRAW_GOLEM_POWER |
| 稻草傀儡动力系统 | 空框架 / 稻草傀儡 / 木棍 | 水 3 + 地 6 | STRAW_GOLEM_POWER |
| 木头傀儡动力系统 | 空框架 / 木头傀儡 | 水 3 + 地 6 + 风 3 | WOOD_GOLEM_POWER |
| 血肉傀儡动力系统 | 空框架 / 骨头 / 血肉傀儡 | 水 20 + 地 20 + 秩序 20 | FLESH_GOLEM_POWER |
| 粘土傀儡动力系统 | 空框架居中 / 粘土 · 粘土傀儡 · 粘土 | 火 30 | CLAY_GOLEM_POWER |
| 油脂傀儡动力系统 | 魔力油脂 · 框架 · 魔力油脂 / 木棍 · 油脂傀儡 · 木棍 | 风/秩序/混沌/地各 10 + 水 20 | TALLOW_GOLEM_POWER |
| 石头傀儡动力系统 | 空框架居中 / 石砖 · 石头傀儡 · 石砖 | 风/秩序/混沌/地各 10 + 水 20 + 火 10 | STONE_GOLEM_POWER |
| 铁卫傀儡动力系统 | 空框架 / 铁卫傀儡 | 地 20 + 秩序 10 | IRON_GOLEM_POWER |
| 神秘傀儡动力系统 | 空框架 / 神秘傀儡 / 风之魔晶 | 地 20 + 秩序 10 + 风 30 | MYSTERY_GOLEM_POWER |

神秘时代与机械动力都是必需前置。每个设备研究 2 页（文字 + 配方），稻草研究 3 页（文字 + 框架配方 + 稻草配方）。竖排配方在书页里居中显示，合成时材料放在左/中/右都能匹配（`CenteredArcaneRecipe`）。

需要 JDK 21，以及 5 个编译期前置 jar（Create 6.0.10 与神秘时代 port.265）。前置**只用于编译，不随本模组发布**；默认从 `libs/` 读取，也可用 `-PdepsDir=<路径>` 指到项目外（本机放在 `../.tools/deps`）。详见 [libs/README.md](libs/README.md)。

```powershell
.\gradlew.bat build
```

产物：`build/libs/Thaumcraft-神秘动力学-<version>.jar`。

> 如果访问不了 Mojang / NeoForge 官方源，`gradle/mojang-mirror.gradle` 会把仓库切到镜像；网络正常时可以删掉 `build.gradle` 里的 `apply from` 一行。

## 项目结构

```
src/main/java/com/mysterydynamics/
  MysteryDynamics.java                 模组入口、9 个方块的注册、应力值注册
  block/GolemPowerBlock.java           应力源方块（朝向、输出面、扳手交互、放置对齐）
  block/GolemPowerBlockEntity.java     转速 / 容量读取、转向联动
  block/GolemPowerFrameBlock.java      空框架（纯装饰）
src/main/resources/assets/mystery_dynamics/
  blockstates/  models/  textures/  lang/   9 套方块 / 物品模型与贴图
src/main/resources/data/
  mystery_dynamics/loot_table/blocks/  9 个掉落表
  minecraft/tags/block/mineable/pickaxe.json
model/
  *.bbmodel                          9 个 Blockbench 源文件
  textures/                          8 种傀儡材质 + 安山机壳 + 占位符
tools/
  preview_block.py                   方块模型预览渲染
  rcon_test.py                       开发服务器 RCON 冒烟测试
  research_layout.py                 扫描研究 JSON，列出坐标并画研究书网格
  render_research_grid.py            把研究坐标渲染成 PNG（research_grid.png）
```

## 已做的验证

开发服务器 + RCON 实测（每台单独放置）：

| 方块 | Speed | Network.Capacity |
|---|---|---|
| 稻草 | 160.0 | 128.0 |
| 木头 | 128.0 | 256.0 |
| 血肉 | 160.0 | 384.0 |
| 粘土 | 128.0 | 512.0 |
| 油脂 | 256.0 | 128.0 |
| 石头 | 64.0 | 1024.0 |
| 铁卫 | 32.0 | 2048.0 |
| 神秘 | 128.0 | 4096.0 |

空框架放置后没有方块实体（纯装饰），符合设计。

## 应力与性能

- 应力容量只通过 Create 的 `BlockStressValues.CAPACITIES` 注册（与水车一致），不再额外覆盖 `calculateAddedStressCapacity`；
- `initialize` 只在“刚放置需要对齐”或“尚未接入网络”时重算转速，区块重新加载时不会重复触发网络更新；
- 实测 500 台铁卫傀儡动力系统连成同一网络：20 TPS、约 3.7 ms/tick，网络容量 1024000 SU，容量不随 tick 增长。
## 说明

- 方块模型由 9 个 bbmodel 转换：读取元素的 `inflate`（负值内缩），UV 按 128 工程分辨率除以 8。
- 物品显示使用原版 `minecraft:block/block` 父模型提供的标准变换（三分之四视角、手持缩放），不再使用自定义 `display`，避免手持时模型贴到镜头前。
- 设备与框架都是非满方块模型（前后开口），方块属性注册为 `noOcclusion`：贴近其它方块时不会剔除邻居朝向设备的面，避免出现“透视”破洞；模型同时使用 `cutout` 渲染类型处理贴图透明区域。
- 「神秘傀儡动力系统」使用其专属 bbmodel 与 `golem_thaumium` 贴图。
- **空傀儡动力系统框架**的傀儡几何体已在 bbmodel 里隐藏，转换时跳过不可见元素，只保留机架部分（不会再出现黑色傀儡）。
- 目前没有为输出轴注册旋转渲染（BER），模型本身是静态方块；相邻的 Create 传动轴会自己渲染到方块表面。
- 大型奥法离心机暂未添加 BlockItem 与奥术工作台配方。



## 涅里伊德之庇

头盔位护甲：

- 护甲值：3；
- 耐久与神秘头盔一致；
- 佩戴时可在水下呼吸；
- 水下视野雾取消，可视距离设为潜水头盔的两倍（12.5）。

当前没有奥术工作台配方，只能在创造模式或通过命令获取。

## 大型奥法离心机

3×3×3 多方块结构：

```text
顶层：空 / 空 / 空
      空 / 离合器 / 空
      空 / 空 / 空

中层：空 / 空 / 空
      空 / 炼金构材 / 空
      空 / 空 / 空

底层：奥术石砖 / 奥术石砖 / 奥术石砖
      奥术石砖 / 动力轴承 / 奥术石砖
      奥术石砖 / 炼金构材 / 奥术石砖
```

### 研究与手册

- 研究：`LARGE_CENTRIFUGE`，位于神秘动力学研究页 `(5, 4)`；
- 前置：神秘时代原版 **奥法离心机**（`CENTRIFUGE`）；
- 研究笔记需求：机械 8、移动 8、魔力 6、工具 6、合成 4、交换 4；
- 结构页为 3×3×3 搭建图，底部显示法杖激活需求；
- 法杖激活需求：火、地、风、混沌、水、秩序各 50 Vis；
- 未完成“大型奥法离心机”研究时，法杖激活与原版多方块一样直接失败，不额外提示；
- 手册结构页中底层炼金构材显示在橙色位置；实际成型判定仍按正面中央的炼金构材。

### 运行

- 顶部动力输入口接入 Create 旋转，最低 32 RPM，最高 256 RPM；
- 内部容量 24 点源质，输入口在模型西侧，输出口在东侧；
- 同一批只能输入一种源质，处理结果在开始时确定一次；
- 输出未排空前不接受新源质；
- 存满或正面强启方块收到红石信号后开始离心；强启只检测“强启控制器”模型对应的单一方块。

### 旋转区伤害

- 仅旋转组 AABB 内的生物会受到伤害；
- 伤害公式沿用动力锯/钻头：`4 × DrillBlock.getDamage(speed)`，256 RPM 时最高 40 点；
- 伤害类型带 `bypasses_armor`，无视护甲；
- 以旋转区几何中心为原点向外击退，最高约等于击退 X；
- 死亡提示：`XXX 被离心机抽飞了`。

> 当前大型离心机的控制器、外壳、输入/输出端口没有 BlockItem 与奥术工作台配方，生存模式获取方式仍需后续补齐。
