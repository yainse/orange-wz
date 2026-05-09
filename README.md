## OrzRepacker / OrangeWz

OrzRepacker / OrangeWz 是一个 MapleStory / 冒险岛 WZ、IMG、XML 资源查看、导入导出与重打包工具。

本仓库保留原有桌面 GUI，同时重点增强了 **Linux / headless CLI**，适合服务器环境、Docker、脚本和 AI Agent 批量处理资源。

> 如果你的目标是自动化、批处理、AI 分析或服务器环境，优先使用 CLI/headless；GUI 适合人工浏览、少量编辑和桌面交互。

## 目录

- [推荐使用方式](#推荐使用方式)
- [快速开始：CLI/headless](#快速开始cliheadless)
- [常用 CLI 命令](#常用-cli-命令)
- [关键能力](#关键能力)
- [重要限制](#重要限制)
- [GUI 使用入口](#gui-使用入口)
- [文档导航](#文档导航)
- [维护者验证](#维护者验证)
- [打包流程](#打包流程)

## 推荐使用方式

### Linux / headless / AI 自动化

推荐使用：

```text
target/OrzRepacker-cli.jar
```

CLI 入口不会启动 Spring Boot 或 Swing，适合：

- Linux 服务器；
- 无显示器或无桌面环境；
- Docker / CI；
- AI Agent 分析和修改资源；
- 批量导出 `.img`、`.wz`、`.ms` 为 XML；
- 将 XML 重新生成 `.img`。

### 桌面 GUI

GUI 仍可用于：

- 打开和浏览 `.wz` / `.img` / XML；
- 右键菜单导入/导出 XML；
- 人工编辑节点；
- 批量修改图片 origin；
- 批量修改 int 节点值；
- 图片缩放；
- RawToIcon；
- 批量删除奇偶数字节点；
- 转移视图。

如果只给 AI/脚本/headless 使用，通常不需要依赖 GUI。

## 快速开始：CLI/headless

需要 Java 21。

```bash
cd /path/to/orange-wz
./mvnw -DskipTests package
```

生成的关键产物：

```text
target/OrzRepacker-cli.jar   # headless CLI jar
target/OrzRepacker-boot.jar  # Spring Boot artifact
target/OrzRepacker.jar       # 原 GUI/分发路径产物
```

如果本机没有可用 Java/JAVA_HOME，但有 Docker，推荐直接运行：

```bash
scripts/verify-cli-headless.sh
```

验证通过时末尾应输出：

```text
OK: CLI/headless verification passed
```

## 常用 CLI 命令

通用格式：

```bash
java -jar target/OrzRepacker-cli.jar <command> [options]
```

查看帮助：

```bash
java -jar target/OrzRepacker-cli.jar --help
```

查看内置 WZ key alias：

```bash
java -jar target/OrzRepacker-cli.jar keys
```

常见 key alias：

- `gms`
- `cms`
- `latest`
- `empty`

### 检查资源文件

`info` 会输出适合脚本/AI 读取的 JSON。

```bash
java -jar target/OrzRepacker-cli.jar info /path/to/Skill.img --key gms
java -jar target/OrzRepacker-cli.jar info /path/to/String.wz --key gms
java -jar target/OrzRepacker-cli.jar info /path/to/Data.ms --key gms
```

### 单个 `.img` 导出 XML

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml /path/to/Skill.img \
  -o /tmp/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

### 多个独立 `.img` 批量导出 XML

```bash
java -jar target/OrzRepacker-cli.jar imgs-to-xml /path/to/Skill.img /path/to/Item.img \
  -o /tmp/img-xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --threads 2
```

注意：`imgs-to-xml --threads` 只适合多个独立 `.img` 文件，不用于同一 `.wz` 或 `.ms` 包内部并发。

### 完整 `.wz` 导出 XML

```bash
java -jar target/OrzRepacker-cli.jar wz-to-xml /path/to/String.wz \
  -o /tmp/String.wz.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --memory-mode low
```

大型 `.wz` 推荐使用 `--memory-mode low`。

### `.ms` 只读导出 XML

```bash
java -jar target/OrzRepacker-cli.jar ms-to-xml /path/to/Data.ms \
  -o /tmp/Data.ms.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

`.ms` 当前只支持 inspect/export，不支持写回或重打包。

### XML 重新生成 `.img`

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml \
  -o /tmp/Skill.modified.img \
  --key gms \
  --zlib-level -1 \
  --zlib-mode default
```

默认不会覆盖已有输出。需要覆盖时显式添加：

```bash
--force
```

## 关键能力

### CLI/headless

- CLI 不启动 Spring Boot 或 Swing。
- `target/OrzRepacker-cli.jar` 是普通 runnable shaded jar，不是 Spring Boot `BOOT-INF` layout。
- 支持 `keys`、`info`、`img-to-xml`、`imgs-to-xml`、`wz-to-xml`、`ms-to-xml`、`xml-to-img`。
- `--xml-version default|v125`：XML 导出版本兼容。
- `--memory-mode normal|low`：大型 `.wz` 低内存导出。
- `--media none|file|base64`：控制媒体 payload 输出方式。
- `--threads 1..4`：多个独立 `.img` 受控并发导出。
- `--zlib-level -1|0..9`：XML 导入 `.img` 时的 PNG zlib 压缩级别。
- `--zlib-mode default|filtered|huffman_only|rle|brute_smallest`：XML 导入 `.img` 时的 PNG zlib 策略。
- `.ms` read-only 支持：`info` 和 `ms-to-xml`。
- Canvas#Video headless metadata-only XML 表示。
- PNG FORMAT3 / FORMAT517 兼容。
- Canvas compressed PNG data 复用。
- Docker/headless 一键验证脚本。

### GUI

- GUI XML 导出支持 `默认` / `125` 版本选择。
- 批量删除支持原按名称删除，以及直接子节点数字名奇偶删除。
- 转移视图已修复为移动同一棵树节点子树，避免目标视图丢失子节点。
- 保留 RawToIcon、批量修改图片 origin、批量修改 int 节点值、图片缩放等既有功能。

## 重要限制

- 不建议直接整分支 merge `gujichu/orange-wz:guji`；当前分支采用按能力拆分移植，避免回退 CLI/headless 和引入 GUI/provider 污染。
- `.ms` 是 read-only 支持，不支持保存、写回或重打包。
- Canvas#Video 当前只导出 metadata-only XML，不是完整视频 round-trip payload。
- `imgs-to-xml --threads` 只并发多个独立 `.img` 文件。
- GUI 视频/FFmpeg、预览缓存、libimagequant/native 强压缩没有作为 headless 主线继续迁移。
- native/libimagequant 不建议默认启用，避免破坏 Linux/headless 可移植性。
- `.hermes/` 是本地流程/计划文件，默认不提交。

## AI/headless 推荐流程

1. 用 `keys` 确认可用 key alias。
2. 用 `info` 检查源 `.img` / `.wz` / `.ms` 是否能解析。
3. 用 `img-to-xml`、`imgs-to-xml`、`wz-to-xml` 或 `ms-to-xml` 导出 XML。
4. 文本分析时优先 `--media none`，减少 XML 体积。
5. 让 AI 或脚本修改 XML。
6. 用 `xml-to-img` 输出到新路径。
7. 对新 `.img` 再运行 `info`。
8. 备份客户端原始资源，再替换并进行游戏客户端 smoke test。

## GUI 使用入口

有桌面环境时，可以继续使用原 GUI/分发 jar 或项目原有启动方式。

GUI 更适合人工检查与少量编辑；在无图形环境或服务器环境中，请使用 CLI/headless。

详细 GUI 回归与移植边界见：

```text
docs/gui-regression-checklist.md
```

## 文档导航

建议阅读顺序：

1. [中文使用说明](docs/zh-usage.md)：完整中文指南，覆盖 GUI、CLI、AI/headless、限制和 FAQ。
2. [AI Agent 专用中文说明](docs/ai-agent-guide.zh.md)：给 AI/自动化执行者看的安全工作流、命令模板和提交前检查。
3. [CLI 使用说明](docs/cli.md)：CLI/headless 参数和命令细节。
4. [AI 使用指南](docs/ai-usage.md)：AI Agent 修改资源时的英文安全流程。
5. [GUI 回归清单与移植边界](docs/gui-regression-checklist.md)：GUI 改动保护项和风险边界。
6. [Linux CLI / AI 文档实施计划](docs/cli-linux-ai-plan.md)：早期 CLI 实施计划，主要作为历史设计记录。

## 维护者验证

在提交 CLI/headless、provider、打包或文档入口相关改动前，推荐运行：

```bash
scripts/verify-cli-headless.sh
```

该脚本会执行：

- Docker Maven full test；
- 检查测试日志中是否出现 `MainFrame` 或 `HeadlessException`；
- Docker Maven package；
- CLI jar 结构检查；
- Docker JRE `--help` / `keys` smoke；
- provider/headless GUI/native/process 泄漏检查；
- 基础 added-line 安全扫描；
- `git diff --check`。

基础安全扫描只用于快速拦截常见问题，不能替代人工审查。

可选环境变量：

```bash
MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21 \
JRE_IMAGE=eclipse-temurin:21-jre \
M2_CACHE=/root/.m2 \
scripts/verify-cli-headless.sh
```

单独检查 CLI jar：

```bash
python3 scripts/check-cli-jar.py target/OrzRepacker-cli.jar
```

## 打包流程

原 GUI/分发打包流程保留如下：

1. 更新 `application.properties` 里新版本的 key。
2. 提交所有记录。
3. 执行 `UpdateVersionNumber` 更新版本号。
4. 将新版本的 key 提交到更新服务器。
5. 执行 Maven 面板的 `package` 进行打包。
6. 修改 Launcher 的版本号（最新版本号在 `application.properties` 里）。
7. 重新编译 Launcher。
8. 将 Launcher 放入解压后的目录中，执行 Launcher 将 Jar 加密成 `data.bin`。
