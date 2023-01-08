![cover](./assets/cover.png)

<p align="right">
  <img src="https://github.com/XiaMoZhiShi/MorphPluginClient/actions/workflows/gradle.yml/badge.svg">
</p>

<!-- [Wiki](https://github.com/XiaMoZhiShi/MorphPlugin/wiki) -->

# (WIP) FeatherMorphClient

[FeatherMorph](https://github.com/XiaMoZhiShi/MorphPlugin)的客户端模组，可以提供一些增强功能。

### 功能
- [x] 在客户端显示自身伪装
- [x] 伪装选择界面
- [x] 技能快捷键
- [x] 一键切换自身可见

### 依赖关系
FeatherMorphClient至少需要下面这些依赖才能运行：
- Fabric 或 Quilt 1.19.3
- [Fabric API](https://modrinth.com/mod/fabric-api)

### 构建
```bash
#!/usr/bin/env bash
git clone https://github.com/XiaMoZhiShi/MorphPluginClient
cd MorphPluginClient

./gradlew build --no-daemon
```

生成的文件将位于`build/libs`中，`client-x.x.x.jar`就是构建出来的插件。