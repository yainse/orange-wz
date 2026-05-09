# OrangeWz AI Agent 使用说明

本文专门给 AI Agent / 自动化脚本使用。目标是在 Linux/headless 环境中安全、可重复地检查、导出、修改和验证 MapleStory/冒险岛 WZ/IMG/MS 资源。

> 如果你是人类用户，优先看 `README.md` 和 `docs/zh-usage.md`。如果你是 AI Agent，执行任务前先读本文。

---

## 0. 总原则

1. **不要直接修改原始客户端资源**。先复制到工作目录，再处理。
2. **不要默认覆盖输出**。`xml-to-img` 只有明确需要时才加 `--force`。
3. **先 `info`，再导出/转换**。先确认文件能解析。
4. **优先 `--media none`**。文本分析、diff、AI 修改场景不要导出图片/音频 payload。
5. **大 `.wz` 用 `--memory-mode low`**。
6. **`imgs-to-xml --threads` 只用于多个独立 `.img` 文件**，不要用于同一个 `.wz`/`.ms` 包内部。
7. **`.ms` 只读**。可以 `info` 和 `ms-to-xml`，不能写回、保存或重打包。
8. **Video 是 metadata-only**。`<video .../>` XML 只表示元数据，不是完整视频 round-trip payload。
9. **CLI/headless 优先**。不要在服务器/AI 自动化任务里启动 GUI。
10. **提交前运行 `scripts/verify-cli-headless.sh`**。

---

## 1. 固定入口

CLI jar：

```text
target/OrzRepacker-cli.jar
```

主类：

```text
orange.wz.cli.OrangeWzCli
```

通用命令格式：

```bash
java -jar target/OrzRepacker-cli.jar <command> [options]
```

查看帮助：

```bash
java -jar target/OrzRepacker-cli.jar --help
```

查看 key alias：

```bash
java -jar target/OrzRepacker-cli.jar keys
```

常用 key alias：

```text
gms
cms
latest
empty
```

解析失败时可按顺序尝试：

```text
gms -> cms -> latest -> empty
```

---

## 2. 构建与自检

### 2.1 本机 Java 可用时

```bash
./mvnw -DskipTests package
python3 scripts/check-cli-jar.py target/OrzRepacker-cli.jar
java -jar target/OrzRepacker-cli.jar --help
java -jar target/OrzRepacker-cli.jar keys
```

### 2.2 本机 Java/JAVA_HOME 不可用时

直接使用 Docker 验证脚本：

```bash
scripts/verify-cli-headless.sh
```

期望末尾输出：

```text
OK: CLI/headless verification passed
```

该脚本会覆盖：

- Docker Maven full test；
- 测试日志 `MainFrame` / `HeadlessException` 检查；
- Docker Maven package；
- CLI jar 结构检查；
- Docker JRE `--help` / `keys` smoke；
- provider/headless GUI/native/process 泄漏检查；
- 基础 added-line 安全扫描；
- `git diff --check`。

---

## 3. 标准 AI 工作流

### 3.1 建立工作目录

不要在客户端资源目录直接操作。推荐复制到临时目录：

```bash
mkdir -p /tmp/orange-wz-work
cp /path/to/Skill.img /tmp/orange-wz-work/Skill.img
```

### 3.2 先检查源文件

```bash
java -jar target/OrzRepacker-cli.jar info /tmp/orange-wz-work/Skill.img --key gms
```

如果失败，尝试其他 key：

```bash
java -jar target/OrzRepacker-cli.jar info /tmp/orange-wz-work/Skill.img --key cms
java -jar target/OrzRepacker-cli.jar info /tmp/orange-wz-work/Skill.img --key latest
java -jar target/OrzRepacker-cli.jar info /tmp/orange-wz-work/Skill.img --key empty
```

### 3.3 导出 XML

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml /tmp/orange-wz-work/Skill.img \
  -o /tmp/orange-wz-work/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

### 3.4 修改 XML

AI 修改 XML 时必须遵守：

- 只改用户明确要求的节点；
- 保留 XML 结构、节点名和类型；
- 不批量重排无关内容；
- 不改图片/音频 payload，除非用户明确要求；
- 大 XML 分段处理，避免上下文截断导致误改；
- 修改后用 diff 检查变更是否只包含目标内容。

### 3.5 生成新的 `.img`

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/orange-wz-work/Skill.img.xml \
  -o /tmp/orange-wz-work/Skill.modified.img \
  --key gms \
  --zlib-level -1 \
  --zlib-mode default
```

默认不覆盖已有输出。只有确认要覆盖临时输出时才使用：

```bash
--force
```

### 3.6 验证新 `.img`

```bash
java -jar target/OrzRepacker-cli.jar info /tmp/orange-wz-work/Skill.modified.img --key gms
```

如果有客户端可用，再让人类或外部流程做游戏客户端 smoke test。

---

## 4. 命令模板

### 4.1 `info`：检查文件

```bash
java -jar target/OrzRepacker-cli.jar info /path/to/Skill.img --key gms
java -jar target/OrzRepacker-cli.jar info /path/to/String.wz --key gms
java -jar target/OrzRepacker-cli.jar info /path/to/Data.ms --key gms
```

输出是 JSON，适合 AI/脚本解析。

### 4.2 `img-to-xml`：单 `.img` 导出

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml /path/to/Skill.img \
  -o /tmp/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

### 4.3 `imgs-to-xml`：多个独立 `.img` 导出

```bash
java -jar target/OrzRepacker-cli.jar imgs-to-xml /path/to/Skill.img /path/to/Item.img \
  -o /tmp/img-xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --threads 2
```

注意：只用于多个独立 `.img`。不要把 `.wz`/`.ms` 内部 entry 拆给这个命令并发处理。

### 4.4 `wz-to-xml`：完整 `.wz` 导出

```bash
java -jar target/OrzRepacker-cli.jar wz-to-xml /path/to/String.wz \
  -o /tmp/String.wz.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --memory-mode low
```

建议：大型 `.wz` 始终使用 `--memory-mode low`。

### 4.5 `ms-to-xml`：`.ms` 只读导出

```bash
java -jar target/OrzRepacker-cli.jar ms-to-xml /path/to/Data.ms \
  -o /tmp/Data.ms.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

重要：`.ms` 当前不能写回或重打包。

### 4.6 `xml-to-img`：XML 生成 `.img`

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml \
  -o /tmp/Skill.modified.img \
  --key gms \
  --zlib-level -1 \
  --zlib-mode default
```

可选压缩策略：

```text
--zlib-level -1|0..9
--zlib-mode default|filtered|huffman_only|rle|brute_smallest
```

`brute_smallest` 可能更慢，只在体积优先时使用。

---

## 5. 参数选择策略

### 5.1 `--media`

```text
none
file
base64
```

推荐：

- 文本分析 / AI diff：`none`
- 需要独立图片/音频文件：`file`
- 需要单 XML 文件但能接受体积膨胀：`base64`

默认建议 AI 使用：

```bash
--media none
```

### 5.2 `--xml-version`

```text
default
v125
```

推荐：

- 默认使用 `default`；
- 只有需要兼容旧工具链时才使用 `v125`。

### 5.3 `--memory-mode`

```text
normal
low
```

推荐：

- 小文件默认 `normal`；
- 大 `.wz` 导出使用 `low`。

### 5.4 `--threads`

```text
默认：1
最大：4
```

只用于 `imgs-to-xml`，且只处理多个独立 `.img`。

---

## 6. 不要做的事

AI Agent 不应执行以下操作：

- 不要直接编辑客户端原始 `.img` / `.wz` / `.ms`。
- 不要直接覆盖用户资源目录中的文件。
- 不要对 `.ms` 承诺写回、保存或重打包。
- 不要把 `<video .../>` 当作完整视频数据。
- 不要在 headless 环境启动 GUI。
- 不要把 `imgs-to-xml --threads` 用于同一个 `.wz`/`.ms` 包内部。
- 不要默认启用 native/libimagequant 或 FFmpeg 相关能力。
- 不要提交 `.hermes/` 本地流程文件。
- 不要在文档或日志中保留敏感配置、认证信息或连接串明文。

---

## 7. 任务类型示例

### 7.1 只读分析资源

目标：让 AI 分析 `Skill.img` 中的文本节点。

流程：

```bash
mkdir -p /tmp/orange-wz-work
cp /path/to/Skill.img /tmp/orange-wz-work/Skill.img
java -jar target/OrzRepacker-cli.jar info /tmp/orange-wz-work/Skill.img --key gms
java -jar target/OrzRepacker-cli.jar img-to-xml /tmp/orange-wz-work/Skill.img \
  -o /tmp/orange-wz-work/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

然后只读取 XML 中与任务相关的片段。

### 7.2 修改文本并生成新 `.img`

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/orange-wz-work/Skill.img.xml \
  -o /tmp/orange-wz-work/Skill.modified.img \
  --key gms \
  --zlib-level -1 \
  --zlib-mode default
java -jar target/OrzRepacker-cli.jar info /tmp/orange-wz-work/Skill.modified.img --key gms
```

### 7.3 批量导出多个 `.img`

```bash
java -jar target/OrzRepacker-cli.jar imgs-to-xml /tmp/work/Skill.img /tmp/work/Item.img /tmp/work/Quest.img \
  -o /tmp/work/xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --threads 3
```

### 7.4 大型 `.wz` 导出供搜索/索引

```bash
java -jar target/OrzRepacker-cli.jar wz-to-xml /path/to/String.wz \
  -o /tmp/String.wz.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --memory-mode low
```

### 7.5 `.ms` 只读审查

```bash
java -jar target/OrzRepacker-cli.jar info /path/to/Data.ms --key gms
java -jar target/OrzRepacker-cli.jar ms-to-xml /path/to/Data.ms \
  -o /tmp/Data.ms.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

---

## 8. 输出与汇报格式建议

AI Agent 完成资源任务后，建议向用户汇报：

```text
已完成：
- 源文件：...
- 工作目录：...
- 使用 key：...
- 导出 XML：...
- 生成文件：...
- 验证命令：...
- 验证结果：...

注意：
- 是否覆盖原文件：否/是
- 是否涉及 .ms 写回：否
- 是否涉及 video round-trip：否
- 是否需要用户做客户端 smoke test：是/否
```

如果无法确认解析 key 或缺少样本文件，应明确说明，不要猜测。

---

## 9. 提交前检查

如果 AI Agent 修改了仓库代码或文档，提交前运行：

```bash
git status --short --branch
git diff --check
scripts/verify-cli-headless.sh
```

确认 staged 文件：

```bash
git diff --cached --stat
git diff --cached --name-status
```

提交前应向用户展示：

- staged 文件列表；
- diff stat；
- 验证结果；
- 建议 commit message。

不要未经确认直接 commit/push。
