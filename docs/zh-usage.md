# OrangeWz 中文使用说明

本文面向两类使用者：

1. **普通桌面使用者**：需要用 GUI 查看、导入、导出和编辑 WZ/IMG/XML 资源。
2. **Linux/headless/AI 自动化使用者**：需要在服务器、Docker、脚本或 AI Agent 中批量检查、导出、转换 MapleStory/冒险岛资源文件。

AI/自动化执行者可直接阅读：[AI Agent 专用中文说明](ai-agent-guide.zh.md)。

> 推荐：如果你的目标是自动化、批处理、AI 分析或服务器环境，优先使用 CLI/headless 模式；GUI 更适合人工浏览和少量编辑。

---

## 1. 项目定位

OrangeWz 是一个 MapleStory/冒险岛 WZ 资源查看、转换和编辑工具。本项目当前保留桌面 GUI，同时新增并强化了可在 Linux/headless 环境运行的 CLI。

主要能力：

- 打开和浏览 `.wz`、`.img`、XML 导出的资源结构（GUI/项目整体能力）。
- 将 `.img`、`.wz`、`.ms` 导出为 XML，方便审查、diff、AI 分析和批处理。
- 将 XML 重新生成 `.img`。
- 支持多种 WZ key alias。
- 支持 PNG 格式兼容、压缩参数、低内存模式和受控并发导出。
- 支持 `.ms` 包只读解析和导出。
- 提供 Docker/headless 一键验证脚本，适合 CI 或 AI Agent 在改动后自检。

---

## 2. 运行模式选择

### 2.1 推荐场景：CLI/headless

适合：

- Linux 服务器；
- 无显示器 / 无桌面环境；
- Docker；
- AI Agent 自动分析资源；
- 批量导出或转换；
- 希望用 Git 管理 XML 差异；
- 不希望启动 Spring Boot 或 Swing GUI。

CLI 主类：

```text
orange.wz.cli.OrangeWzCli
```

CLI jar：

```text
target/OrzRepacker-cli.jar
```

### 2.2 GUI 模式

适合：

- 人工浏览资源树；
- 少量编辑节点；
- 右键菜单导入/导出 XML；
- 图片预览、节点菜单操作等桌面交互。

GUI 仍然存在，但如果你的主要目标是 AI/脚本/headless，通常不需要继续依赖 GUI。

---

## 3. 构建与验证

### 3.1 本机 Java 构建

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

### 3.2 Docker/headless 一键验证

如果本机没有 Java/JAVA_HOME，或者希望用干净环境验证，推荐运行：

```bash
scripts/verify-cli-headless.sh
```

该脚本会执行：

- 清理 `target` 和 `dependency-reduced-pom.xml`；
- Docker Maven full test；
- 检查测试日志中是否出现 `MainFrame` 或 `HeadlessException`；
- Docker Maven package；
- 检查 `target/OrzRepacker-cli.jar` 是否是普通 runnable CLI jar；
- Docker JRE smoke：`--help`、`keys`；
- provider/headless 层 GUI/native/process 依赖泄漏检查；
- 基础 added-line 安全扫描，快速拦截常见问题，但不能替代人工审查；
- `git diff --check`；
- 结束后清理构建产物。

可选环境变量：

```bash
MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21 \
JRE_IMAGE=eclipse-temurin:21-jre \
M2_CACHE=/root/.m2 \
scripts/verify-cli-headless.sh
```

期望末尾输出：

```text
OK: CLI/headless verification passed
```

---

## 4. CLI 基础用法

通用格式：

```bash
java -jar target/OrzRepacker-cli.jar <command> [options]
```

查看帮助：

```bash
java -jar target/OrzRepacker-cli.jar --help
```

查看可用 key alias：

```bash
java -jar target/OrzRepacker-cli.jar keys
```

常见 key alias：

- `gms`
- `cms`
- `latest`
- `empty`

如果解析失败，可尝试按顺序切换：

```text
gms -> cms -> latest -> empty
```

---

## 5. CLI 命令详解

### 5.1 info：检查资源文件

用于在导出或转换前先确认文件是否能解析。

```bash
java -jar target/OrzRepacker-cli.jar info /path/to/Skill.img --key gms
```

也可用于 `.wz` 或 `.ms`：

```bash
java -jar target/OrzRepacker-cli.jar info /path/to/String.wz --key gms
java -jar target/OrzRepacker-cli.jar info /path/to/Data.ms --key gms
```

输出为适合脚本解析的 JSON。

### 5.2 img-to-xml：单个 `.img` 导出 XML

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml /path/to/Skill.img \
  -o /tmp/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

参数说明：

- `-o` / `--output`：输出 XML 路径；
- `--key`：WZ key alias；
- `--indent`：XML 缩进空格数；
- `--media none`：不导出图片/音频 payload，适合文本 diff 和 AI 分析；
- `--xml-version default`：默认 XML 版本。

### 5.3 imgs-to-xml：多个独立 `.img` 批量导出

```bash
java -jar target/OrzRepacker-cli.jar imgs-to-xml /path/to/Skill.img /path/to/Item.img \
  -o /tmp/img-xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --threads 2
```

注意：

- `imgs-to-xml` 只适合多个**独立 `.img` 文件**；
- 不要把它理解为 `.wz` 或 `.ms` 内部并发导出；
- `--threads` 默认 1，最大 4；
- 输出目录中不能有重复目标路径，否则会提前失败，避免覆盖。

### 5.4 wz-to-xml：完整 `.wz` 包导出 XML

```bash
java -jar target/OrzRepacker-cli.jar wz-to-xml /path/to/String.wz \
  -o /tmp/String.wz.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --memory-mode low
```

建议：

- 大型 `.wz` 使用 `--memory-mode low`；
- 如果需要图片/音频 payload，再考虑 `--media file` 或 `--media base64`；
- AI 分析或代码审查场景优先用 `--media none`。

### 5.5 ms-to-xml：`.ms` 只读导出 XML

```bash
java -jar target/OrzRepacker-cli.jar ms-to-xml /path/to/Data.ms \
  -o /tmp/Data.ms.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

重要限制：

- `.ms` 当前是 read-only 支持；
- 可以 `info` / 导出 XML；
- 不支持写回或重新打包 `.ms`；
- 内部 entry 输出路径会做安全检查，拒绝绝对路径或 `..` traversal。

### 5.6 xml-to-img：XML 重新生成 `.img`

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml \
  -o /tmp/Skill.modified.img \
  --key gms \
  --zlib-level -1 \
  --zlib-mode default
```

注意：如果 XML 中包含 metadata-only `<video .../>` 节点，当前不应视为完整可 round-trip 的视频输入。

覆盖已有输出需要显式加：

```bash
--force
```

压缩参数：

- `--zlib-level -1`：默认压缩级别；
- `--zlib-level 0..9`：指定 zlib 压缩级别；
- `--zlib-mode default`：默认模式；
- `--zlib-mode filtered`；
- `--zlib-mode huffman_only`；
- `--zlib-mode rle`；
- `--zlib-mode brute_smallest`：尝试更小输出，速度较慢。

---

## 6. 常用参数说明

### 6.1 `--xml-version`

可选：

```text
default
v125
```

用途：

- `default`：推荐默认值；
- `v125`：兼容旧版 XML 消费方或特定工具链。

### 6.2 `--memory-mode`

可选：

```text
normal
low
```

用途：

- `normal`：默认模式；
- `low`：导出大型 `.wz` 时更积极释放内存，适合服务器/headless 环境。

### 6.3 `--media`

常见值：

```text
none
file
base64
```

建议：

- AI/脚本分析：`none`
- 需要导出独立媒体文件：`file`
- 需要单文件 XML 且可接受体积膨胀：`base64`

### 6.4 `--threads`

用于 `imgs-to-xml`。

```text
默认：1
最大：4
```

只并发多个独立 `.img`，不要用于同一 `.wz` / `.ms` 包内部资源。

---

## 7. 推荐 AI/headless 工作流

### 7.1 检查优先

先用 `info`，不要直接改资源：

```bash
java -jar target/OrzRepacker-cli.jar info /work/Skill.img --key gms
```

### 7.2 导出为 XML

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml /work/Skill.img \
  -o /work/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

### 7.3 让 AI 修改 XML

建议：

- 保留原始 `.img` 备份；
- 用 Git 管理 XML 改动；
- 让 AI 只改明确目标节点；
- 大文件分段读取，避免上下文过大；
- 对图片/音频 payload 不需要分析时使用 `--media none`。

### 7.4 重新生成 `.img`

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /work/Skill.img.xml \
  -o /work/Skill.modified.img \
  --key gms \
  --zlib-level -1 \
  --zlib-mode default
```

### 7.5 验证新 `.img`

```bash
java -jar target/OrzRepacker-cli.jar info /work/Skill.modified.img --key gms
```

### 7.6 替换客户端资源

建议流程：

1. 备份原始客户端资源；
2. 将新 `.img` 放到临时位置；
3. 本地客户端 smoke test；
4. 再替换正式资源；
5. 资源文件和代码改动分开提交。

---

## 8. GUI 使用说明

### 8.1 启动 GUI

构建后可使用原 GUI/分发 jar，或通过项目原有启动方式运行桌面程序。

如果在无图形环境中运行，推荐改用 CLI；headless 环境下测试已避免自动启动 GUI。

### 8.2 GUI 常见操作

GUI 适合人工操作：

- 打开 `.wz` / `.img` / XML；
- 浏览资源树；
- 右键菜单导出 XML；
- 从 XML 导入；
- 修改节点名称和值；
- 批量修改图片 origin；
- 批量修改 int 节点值；
- 图片缩放；
- RawToIcon；
- 批量删除奇偶数字节点；
- 转移视图。

### 8.3 GUI XML 导出版本

GUI 的 XML 导出对话框支持：

- `默认`
- `125`

对应 CLI/provider：

```text
XmlExportVersion.DEFAULT
XmlExportVersion.V125
```

### 8.4 GUI 批量删除奇偶节点

批量删除对话框支持：

- 按名称删除；
- 删除奇数节点；
- 删除偶数节点。

奇偶删除规则：

- 只处理当前选中节点的直接子节点；
- 节点名必须是 ASCII 整数字符串；
- `0/2/10` 属于偶数；
- `1/3` 属于奇数；
- `foo`、`01`、`-1`、空字符串不处理；
- 不递归删除嵌套子节点，避免误伤。

### 8.5 GUI 转移视图

`转移视图` 已修复为迁移同一棵树节点子树：

- 目标视图保留已展开子节点；
- 源视图摘除节点但不释放对象；
- 普通删除仍保留释放语义。

---

## 9. 当前支持与限制

### 9.1 已支持

- `.img` inspect / XML export / XML import；
- `.wz` inspect / XML export；
- `.ms` inspect / read-only XML export；
- PNG FORMAT3 / FORMAT517 兼容；
- Canvas compressed PNG data 复用；
- XML 导出版本兼容；
- zlib 写入参数；
- 低内存导出；
- 多独立 `.img` 受控并发；
- Canvas#Video metadata-only XML 表示。

### 9.2 限制

- `.ms` 不支持写回或重新打包；
- Canvas#Video 当前是 metadata-only，不是完整视频 round-trip payload；
- `imgs-to-xml --threads` 只适合多个独立 `.img`；
- GUI 视频/FFmpeg、预览缓存、libimagequant/native 强压缩没有作为 headless 主线继续移植；
- native 强压缩不建议默认启用，避免破坏 Linux/headless 可移植性。

---

## 10. 维护者开发与合并前检查

### 10.1 每次提交前建议运行

```bash
scripts/verify-cli-headless.sh
```

### 10.2 关键保护项

合并前应确认：

- `mvnw` / `mvnw.cmd` 保留；
- `src/main/java/orange/wz/cli/OrangeWzCli.java` 保留；
- `src/main/java/orange/wz/cli/WzCliKeys.java` 保留；
- `docs/cli.md`、`docs/ai-usage.md`、`docs/cli-linux-ai-plan.md` 保留；
- `scripts/check-cli-jar.py` 保留；
- `scripts/verify-cli-headless.sh` 通过；
- `target/OrzRepacker-cli.jar` 是普通 runnable CLI jar，而不是 Spring Boot `BOOT-INF` layout；
- `application.properties` 版本不回退；
- provider/core 不依赖 GUI/Swing/MainFrame；
- 不提交 `.hermes/` 本地流程文件。

### 10.3 provider/headless 禁止依赖检查

```bash
git grep -n "orange\.wz\.gui\|MainFrame\|ExportXmlData\|ExportXmlDialog\|javax\.swing\|System\.load\|Runtime\.getRuntime\|ProcessBuilder\|org\.pngquant\|libimagequant" -- \
  src/main/java/orange/wz/provider \
  src/main/java/orange/wz/provider/tools \
  src/main/java/orange/wz/provider/properties \
  src/main/java/orange/wz/provider/ms
```

期望：无输出。

---

## 11. 常见问题

### Q1：我只在 Linux 服务器或 AI Agent 中用，还需要 GUI 吗？

不需要。优先使用 `target/OrzRepacker-cli.jar` 和本文 CLI 工作流即可。

### Q2：为什么不直接 merge guji 分支？

因为整分支 merge 会带来较高风险：

- 删除或回退现有 CLI/headless 资产；
- 回退 `application.properties`；
- 引入 GUI 大文件覆盖；
- 引入 native/libimagequant 等非必要依赖；
- 破坏 provider/headless 分层。

当前采用的是按能力拆分移植：core/provider 和 CLI 优先，GUI 只做低风险补齐。

### Q3：`.ms` 能不能修改后写回？

当前不能。`.ms` 只支持 read-only inspect/export。

### Q4：图片或音频会不会全部进 XML？

取决于 `--media`：

- `none`：不输出媒体 payload；
- `file`：媒体输出为外部文件路径；
- `base64`：媒体内嵌到 XML。

AI 分析通常推荐 `none`。

### Q5：生成 `.img` 会覆盖原文件吗？

不会默认覆盖。覆盖已有输出需要加：

```bash
--force
```

### Q6：大文件导出内存高怎么办？

使用：

```bash
--memory-mode low
```

并优先 `--media none`。

### Q7：如何确认 CLI jar 没被 Spring Boot 打包破坏？

运行：

```bash
python3 scripts/check-cli-jar.py target/OrzRepacker-cli.jar
```

或直接运行：

```bash
java -jar target/OrzRepacker-cli.jar --help
```

---

## 12. 推荐命令速查

```bash
# 构建
./mvnw -DskipTests package

# Docker/headless 全量验证
scripts/verify-cli-headless.sh

# 查看帮助
java -jar target/OrzRepacker-cli.jar --help

# 查看 key alias
java -jar target/OrzRepacker-cli.jar keys

# 检查 img
java -jar target/OrzRepacker-cli.jar info /path/to/Skill.img --key gms

# img -> xml
java -jar target/OrzRepacker-cli.jar img-to-xml /path/to/Skill.img -o /tmp/Skill.img.xml --key gms --media none --xml-version default

# 多 img -> xml
java -jar target/OrzRepacker-cli.jar imgs-to-xml /path/to/Skill.img /path/to/Item.img -o /tmp/img-xml --key gms --media none --threads 2

# wz -> xml
java -jar target/OrzRepacker-cli.jar wz-to-xml /path/to/String.wz -o /tmp/String.wz.xml.d --key gms --media none --memory-mode low

# ms -> xml，只读
java -jar target/OrzRepacker-cli.jar ms-to-xml /path/to/Data.ms -o /tmp/Data.ms.xml.d --key gms --media none

# xml -> img
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml -o /tmp/Skill.modified.img --key gms --zlib-level -1 --zlib-mode default
```
