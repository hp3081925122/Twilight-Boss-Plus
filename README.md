# Twilight Boss Plus

暮色森林 Boss 技能与特效增强附属模组。本分支面向 **Minecraft 26.1.2 / NeoForge 26.1.2.112 及以上**，开发和构建使用 Java 25。

覆盖娜迦、巫妖、米诺菇、九头蛇、幻影骑士、暮初恶魂、雪怪首领和冰雪女王，保留独立的技能时间轴、阶段、配置和客户端图集特效。

需要安装暮色森林；本次验证使用 `twilightforest-26.1.2-4.9.3817-universal.jar`。配置文件生成在 `config/twilightbossplus/`。

## 构建

```powershell
$env:JAVA_HOME = 'C:/Users/30819/.jdks/jdk-25.0.4+7'
.\gradlew.bat build
```

输出文件：`build/libs/twilightbossplus-26.1.2-neoforge-0.0.1.jar`。

## 分支

- `main`：Minecraft 1.20.1 Forge。
- `1211`：Minecraft 1.21.1 NeoForge。
- `2612`：Minecraft 26.1.2 NeoForge。

迁移内容、依赖和测试范围见 [26.1.2 迁移记录](docs/迁移-2612.md)。
