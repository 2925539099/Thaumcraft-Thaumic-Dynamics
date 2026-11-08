# libs —— 编译期第三方依赖（不提交、不发布）

本工程只发布「神秘动力学」本体。下列前置 jar 仅用于编译，不会进入构建产物、仓库或 Release。
`build.gradle` 按**文件名前缀**查找，升级前置版本时直接替换 jar 即可，不用改构建脚本。

| 文件名前缀 | 来源 | 已验证版本 |
|---|---|---|
| `create-*.jar` | Create（1.21.1 / NeoForge） | `create-1.21.1-6.0.10.jar` |
| `ponder-neoforge-*.jar` | Create jar 内 `META-INF/jarjar/` | `1.0.82+mc1.21.1` |
| `flywheel-neoforge-*.jar` | 同上 | `1.21.1-1.0.6` |
| `Registrate-*.jar` | 同上 | `MC1.21-1.3.0+67` |
| `curios-neoforge-*.jar` | Curios | `9.5.1+1.21.1` |
| `thaumcraft-*.jar` | 神秘时代移植版（必需前置） | `0.3.0-port.279-scex.compat.1` |

Ponder / Flywheel / Registrate 不用单独下载：jar 就是 zip，从 Create jar 的 `META-INF/jarjar/` 里解出来即可。

## 放置位置

- 默认读取项目的 `libs/`；
- 也可以放在项目外，用 `-PdepsDir=<路径>` 指定；
- `build.gradle` 会优先找 `../.tools/deps`，找不到再退回 `libs/`；
- 缺任何一个前置时，构建会直接报出缺的是哪个文件名前缀。

`libs/*.jar` 已被 `.gitignore` 排除；仓库和 Release 里只有「神秘动力学」自己的 jar。
