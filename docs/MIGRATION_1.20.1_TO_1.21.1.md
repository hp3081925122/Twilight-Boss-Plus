# Twilight Boss Plus 1.20.1 Forge → 1.21.1 NeoForge 迁移

## 范围

- 源版本：Minecraft 1.20.1 Forge 47.x。
- 目标版本：Minecraft 1.21.1 NeoForge 21.1.x。
- 目标依赖：Twilight Forest 4.7.3196（1.21.1 NeoForge）、GeckoLib 4.9.x。

## 迁移原则

- 保留 Boss 技能、伤害倍率、阶段阈值、配置项、特效和 Mixin 行为。
- 只替换加载器 API、Minecraft 1.21.1 API、依赖版本和资源格式。
- 迁移完成后执行 clean build、服务端启动检查和客户端资源加载检查。

## 已知差异

- 1.21.1 的 Twilight Forest 发布文件仅提供 NeoForge，因此目标加载器改为 NeoForge。
- 模型 Mixin 使用映射后的 `setupAnim` 方法名，移除旧版混入的 `m_6973_` 候选，避免目标方法检查失败。
- 1.21.1 使用新的 `pack_format` 和 NeoForge 模组描述文件名。
- NeoForge 网络改用 `CustomPacketPayload`，保留原有客户端 VFX 广播语义。
- Twilight Forest 4.7 的 Boss 继承层级和部分方法名已变化，Mixin 已按实际字节码调整；不存在的旧版 `spitFireball` 目标移除，UrGhast 的现有阶段逻辑仍由 `doTantrumDamageEffects` 与脑逻辑接管。

## 验收

- [ ] `clean build` 成功。
- [x] `build` 成功。
- [x] 服务端可启动并完成模组加载，日志到达 `Done`，配方加载 1726 条、进度加载 1864 条。
- [x] 客户端可启动并完成资源重载，Boss 模型 Mixin 与 VFX 资源加载无阻断错误。
- [ ] 逐项检查技能伤害、阶段触发、击退和配置项未被简化。
