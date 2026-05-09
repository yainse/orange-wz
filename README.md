## OrzRepacker

OrzRepacker / OrangeWz 是一个 MapleStory WZ/IMG 资源查看、导入导出与重打包工具。原项目以桌面 GUI 为主；本仓库现在额外提供一个 Linux/headless CLI 入口，方便服务器环境、脚本和 AI Agent 使用。

## Linux / Headless CLI

CLI 入口不会启动 Spring Boot 或 Swing，适合在 Linux 服务器上直接处理 `.img`、`.wz`、`.xml` 文件；`.ms` 目前提供 read-only 读取/导出能力。

### 构建

需要 Java 21。

```bash
cd /home/edam/orange-wz
./mvnw -DskipTests package
```

生成的 CLI jar：

```text
target/OrzRepacker-cli.jar
```

### 常用命令

查看帮助：

```bash
java -jar target/OrzRepacker-cli.jar --help
```

查看内置 WZ key alias：

```bash
java -jar target/OrzRepacker-cli.jar keys
```

解析 `.img` 或 `.wz`，输出适合脚本/AI 读取的 JSON：

```bash
java -jar target/OrzRepacker-cli.jar info /path/to/Skill.img --key gms
```

导出 `.img` 为 XML：

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml /path/to/Skill.img \
  -o /tmp/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

从 XML 重新生成 `.img`：

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml \
  -o /tmp/Skill.modified.img \
  --key gms \
  --zlib-level -1 \
  --zlib-mode default
```

导出完整 `.wz` 包中的 image XML：

```bash
java -jar target/OrzRepacker-cli.jar wz-to-xml /path/to/String.wz \
  -o /tmp/String.wz.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --memory-mode low
```

### CLI/headless 增强能力

- XML 导出支持 `--xml-version default|v125`：
  - `default` 保持当前 OrangeWz CLI XML 格式；
  - `v125` 输出更接近 v125 兼容 XML 结构，便于与旧工具或脚本对接。
- `.wz` 批量导出支持 `--memory-mode normal|low`：
  - `normal` 为默认模式；
  - `low` 会在每个 image XML 成功导出后释放可重载的图像缓存，适合服务器或大 WZ 包导出。
- 新增 `imgs-to-xml` 受控并发导出：仅并发多个独立 `.img` 文件，`--threads` 默认 `1`、最大 `4`；`.wz`/`.ms` 内部导出仍保持顺序，避免共享 reader 风险。
- `xml-to-img` 支持显式 PNG zlib 写入参数：`--zlib-level -1|0..9` 与 `--zlib-mode default|filtered|huffman_only|rle|brute_smallest`，只影响 XML 导入的 canvas/png payload；默认仍使用 JDK zlib 默认压缩，不启用 native 或 legacy skill 行为。
- 已支持 `Canvas#Video` 的 headless 解析占位，XML 只导出视频元数据，例如类型和长度；目前不做播放、FFmpeg 导出，也不支持从 `<video>` XML 反向还原二进制视频。
- 新增 `.ms` read-only 支持：`info` 可识别 `.ms`，`ms-to-xml` 可将内部可读 `.img` entry 导出为 XML；输出路径会校验并拒绝绝对路径或 `..` 穿越，当前不支持 `.ms` 保存/重打包。
- `target/OrzRepacker-cli.jar` 现在是可直接 `java -jar` 运行的普通 CLI shaded jar；可用 `scripts/check-cli-jar.py` 检查打包结果，避免被 Spring Boot 重新打包成 `BOOT-INF` 布局。

### 支持的 key alias

- `gms`，默认
- `cms`
- `latest`
- `empty`

### AI 使用建议

推荐流程：

1. 先用 `info` 检查 `.img` / `.wz` 是否能解析；
2. 用 `img-to-xml` 或 `wz-to-xml` 导出 XML；
3. 让 AI 分析或修改 XML；
4. 用 `xml-to-img` 生成新的 `.img` 文件；
5. 手动备份并替换客户端资源文件；
6. 用 Git 提交资源变更。

处理文字类资源，例如 `String/Skill.img`，建议使用：

```bash
--media none
```

这样导出的 XML 更小、更适合 AI 阅读和 diff。

更多说明见：

- [CLI 使用文档](docs/cli.md)
- [AI 使用指南](docs/ai-usage.md)
- [Linux CLI / AI 文档实施计划](docs/cli-linux-ai-plan.md)

## 打包流程

1. 更新 application.properties 里新版本的 key
2. 提交所有记录
3. 执行 UpdateVersionNumber 更新版本号
4. 将新版本的 key 提交到更新服务器
5. 执行 maven 面板的 package 进行打包
6. 修改 Luncher 的版本号（最新版本号在 application.properties 里）
7. 重新编译 Luncher
8. 将 Luncher 放入解压后的目录中，执行 Luncher 将 Jar 加密成 data.bin
