# GUI 回归清单与 guji 移植边界

> 目标：Phase 7 以后开始接触 GUI 代码前，先固定当前 `yainse/orange-wz:temp` 的 GUI 能力与 CLI/headless 边界，避免直接套用 `gujichu/orange-wz:guji` 的大范围 Swing 改动造成回退。

## 当前结论

- **不要整分支 merge `gujichu/guji`。** 当前全量 `HEAD..gujichu/guji` 约 142 个文件变更、12469 行新增、4202 行删除；其中 GUI/资源子集约 75 个文件、9000+ 行新增，并包含 CLI 文档/测试删除、`application.properties` 回退、native `libimagequant` 资源新增等非 GUI 基础接入内容。
- Phase 7 应从小步开始：先保护菜单与表单能力，再单项接入 GUI 功能。
- GUI 可以依赖 provider API；provider/core 仍禁止依赖 `orange.wz.gui.*`、`MainFrame`、Swing/AWT UI 类型。
- `.hermes/` 计划文件仍为本地流程产物，默认不提交。

## 必须保护的现有 GUI 功能

### WzListProperty 右键菜单

`src/main/java/orange/wz/gui/component/menu/WzListPropertyMenu.java` 当前必须保留这些菜单与动作：

- `复制` / `粘贴` / `删除节点`
- `汉化`
- `图片对比`
- `图片嗅探`
- `Outlink`
- `排序并改名`
- `批量删除`
- `图片格式`
- `图片缩放`
- `修改节点名`
- `修改int值`
- `RawToIcon`
- `修改图片origin`

尤其不能被 guji 的 `WzListPropertyMenu.java` 局部改动覆盖掉以下能力：

- `editPane.scaleImage()`
- `editPane.changeNodeName()`
- `editPane.changeIntNodeValue()`
- `editPane.rawToIcon()`
- `editPane.changeOriginValue()`

### WzNodeUtil 批量工具

`src/main/java/orange/wz/gui/utils/WzNodeUtil.java` 当前必须保留：

- `changeNodeName(WzObject, oldName, newName, degree)`
- `changeIntNodeValue(WzObject, nodeName, value)`
- `rawToIcon(WzObject)`
- `changeOriginValue(WzObject, nodeName, x, y)`

这些方法会递归处理 `WzImage` 与 list property，并标记 `tempChanged` / `wzImage.changed`。后续 guji GUI 功能如批量删除、预览缓存、节点转移修复不得删除或改坏这些方法。

### 图片缩放支持指定节点

必须保留：

- `ScaleDialog` 中 `匹配名称(留空=全部)` 输入框
- `CanvasUtil.scaleImage(..., nodeName, scale)` 的按节点名过滤语义
- `editPane.scaleImage()` 调用链

回归验证：

- 留空时处理所有 Canvas；
- 填入节点名时只处理同名 Canvas；
- 保存后重新打开不丢失修改。

### 批量修改图片 origin

必须保留：

- `WzListPropertyMenu` 菜单 `修改图片origin`
- `editPane.changeOriginValue()` 调用链
- `WzNodeUtil.changeOriginValue(...)`

回归验证：

- 留空节点名时修改所有 Canvas 的 `origin`；
- 填入节点名时只修改同名 Canvas；
- 缺失 `origin` 的 Canvas 不崩；
- 修改后 `origin`、Canvas、所属 `WzImage` 均被标记为 changed/tempChanged。

### 批量修改 int 节点值

必须保留：

- `WzListPropertyMenu` 菜单 `修改int值`
- `editPane.changeIntNodeValue()` 调用链
- `WzNodeUtil.changeIntNodeValue(...)`

回归验证：

- 递归修改指定名称的 `WzIntProperty`；
- 不影响其他名称/类型；
- 修改后属性和所属 `WzImage` 被标记 changed/tempChanged。

### RawToIcon

必须保留：

- `WzListPropertyMenu` 菜单 `RawToIcon`
- `editPane.rawToIcon()` 调用链
- `WzNodeUtil.rawToIcon(...)`

回归验证：

- 递归查找名为 `iconRaw` 的 Canvas；
- 将同父节点下 `icon` 的图片内容替换为 `iconRaw`；
- 父节点没有 `icon` 时安全跳过。

### XML 导入/导出

必须保留：

- `WzFileMenu` / `WzFolderMenu` / `WzDirectoryMenu` / `WzImageFileMenu` / `WzImageMenu` / `WzXmlFileMenu` 的 XML 导入导出入口；
- `ExportXmlDialog` 的 media 选项：`NONE` / `FILE` / `BASE64`，对应 UI `不输出` / `文件` / `Base64`，对应 CLI 术语 `none` / `path` / `base64`；
- 当前 provider `XmlExportVersion` / CLI `--xml-version` 能力；
- `WzImage.saveFromXml(Path)` 与 CLI `xml-to-img` 所需链路。

后续若 GUI 增加 XML 版本选择，只能让 GUI 引用 provider enum `XmlExportVersion`，不能把 GUI enum 传入 provider。

### CLI/headless 不受 GUI 改动影响

每次 GUI 改动后必须确认：

- `target/OrzRepacker-cli.jar` 仍是 plain shaded CLI jar；
- `java -jar target/OrzRepacker-cli.jar --help` 正常；
- `keys` 正常输出 `gms/cms/latest/empty`；
- headless Docker 中 `mvn test` 不再打印 `MainFrame HeadlessException`；
- provider 层没有新增 GUI/native/process 依赖。

## guji GUI diff 风险分区

### 禁止直接覆盖/merge 的内容

这些内容必须手工 cherry-pick 或重写，不可整文件覆盖：

- `src/main/java/orange/wz/gui/component/panel/EditPane.java`
  - guji 改动 1000+ 行，极易覆盖当前 RawToIcon、origin、int、scale 等菜单调用链。
- `src/main/java/orange/wz/gui/utils/CanvasUtil.java`
  - guji 改动 1000+ 行，可能影响当前按节点名缩放与图片处理语义。
- `src/main/java/orange/wz/gui/utils/WzNodeUtil.java`
  - guji diff 删除/替换当前批量工具风险高。
- `src/main/resources/application.properties`
  - 不得回退版本或恢复任何 key/token/credential/连接串。
- `docs/cli.md`、`docs/ai-usage.md`、`docs/cli-linux-ai-plan.md`
  - guji 中删除这些 CLI/headless 文档，不得接受。
- `src/test/java/orange/wz/cli/*` 与 Phase 1-6 新增 provider/CLI 测试
  - guji 中删除多项 CLI/provider 测试，不得接受。
- `src/main/resources/libimagequant/*` 与 `LibimagequantStrongCompress*`
  - native 强压缩必须独立确认，不作为 Phase 7 基础接入默认内容。
- guji 中 provider 反向引用 GUI 的写法
  - 例如 `WzImage` / `WzXmlFile` / `XmlExport` 引用 `ExportXmlData.ExportVersion`，或 `WzPngProperty` 读取 `MainFrame.getInstance()` 的 GUI 选项。
  - 必须重写为 provider 自有 enum/value object/显式参数注入，不能接受 provider -> GUI 依赖。

### 可作为后续小步移植候选

按风险从低到高：

1. `ExportXmlDialog` / `ExportXmlData` 接入 provider `XmlExportVersion`。
   - GUI -> provider 单向依赖；provider 不引用 GUI。
   - 不影响 CLI 默认行为。
2. `BatchDeleteNodeDialog` / `BatchDeleteNodeFormData` + `WzListPropertyMenu` 小范围菜单接入。
   - 只做批量删除奇数/偶数数字子节点；不要同时改预览/视频/压缩。
3. 节点转移视图 bug 修复。
   - 只从 guji 最新提交 `3a85e21` 精确提取修复点，不整文件覆盖 `EditPane`。
4. 图片/动画预览缓存。
   - 进入 Phase 8 后单独做；必须复用 provider `WzMemoryReclaimer`，GUI cache 不得放入 provider。
5. 视频/FFmpeg。
   - 进入 Phase 9；Linux 下必须支持 PATH `ffmpeg`，找不到时禁用导出而不是崩溃。
6. libimagequant/native 强压缩。
   - 进入 Phase 10 且可选；默认不影响 CLI jar，不允许加载失败导致启动失败。

## 每次 GUI 改动后的验证命令

本机缺少可用 Java/JAVA_HOME 时，继续使用 Docker：

```bash
sudo -n docker run --rm \
  -v "$PWD":/workspace -v /root/.m2:/root/.m2 -w /workspace \
  maven:3.9.9-eclipse-temurin-21 \
  bash -lc 'mvn -q test 2>&1 | tee /tmp/full.log; status=${PIPESTATUS[0]}; if grep -q "MainFrame\\|HeadlessException" /tmp/full.log; then echo "HEADLESS_GUI_EXCEPTION_FOUND"; exit 2; fi; exit $status'

sudo -n docker run --rm \
  -v "$PWD":/workspace -v /root/.m2:/root/.m2 -w /workspace \
  maven:3.9.9-eclipse-temurin-21 \
  mvn -q -DskipTests package

python3 scripts/check-cli-jar.py target/OrzRepacker-cli.jar

sudo -n docker run --rm -v "$PWD":/workspace -w /workspace eclipse-temurin:21-jre \
  bash -lc 'java -jar target/OrzRepacker-cli.jar --help | grep -E -- "img-to-xml|xml-to-img|imgs-to-xml|ms-to-xml|--threads|--zlib-level" && java -jar target/OrzRepacker-cli.jar keys | grep -E "\\\"gms\\\"|\\\"cms\\\"|\\\"latest\\\"|\\\"empty\\\""'
```

Provider/headless leak 检查分两类执行。

严格检查 GUI/native/process 依赖，预期无输出：

```bash
git grep -n "orange.wz.gui\|MainFrame\|ExportXmlData\|ExportXmlDialog\|javax.swing\|System.load\|Runtime.getRuntime\|ProcessBuilder\|org.pngquant\|libimagequant" -- \
  src/main/java/orange/wz/provider \
  src/main/java/orange/wz/provider/tools \
  src/main/java/orange/wz/provider/properties \
  src/main/java/orange/wz/provider/ms
```

`java.awt` / `java.awt.image` 是当前 provider 图像处理 baseline（例如 `BufferedImage`、`ImgTool`），不作为 GUI 泄漏直接失败；如有 GUI 改动触碰 provider，应单独对比是否新增 AWT UI/Swing 依赖：

```bash
git grep -n "java.awt" -- \
  src/main/java/orange/wz/provider \
  src/main/java/orange/wz/provider/tools \
  src/main/java/orange/wz/provider/properties \
  src/main/java/orange/wz/provider/ms
```

只允许既有图像处理类命中，不允许新增 Swing/UI、窗口、对话框、桌面交互或 native/process 加载。

## 手工 GUI 回归清单

有可用桌面环境时，按以下顺序人工验证：

1. 启动 GUI，打开一个 `.wz` 和一个独立 `.img`。
2. 右键 list property，确认菜单项完整：`RawToIcon`、`修改图片origin`、`修改int值`、`图片缩放` 等均存在。
3. 对样本节点执行 `图片缩放`：
   - 留空名称缩放全部；
   - 指定名称只缩放匹配 Canvas；
   - 保存、关闭、重新打开后确认缩放结果落盘。
4. 执行 `修改图片origin`：
   - 缺失 origin 的 Canvas 不崩；
   - 匹配 Canvas 的 origin 变化；
   - 保存、关闭、重新打开后确认 origin 结果落盘。
5. 执行 `修改int值`：
   - 只改指定名称 int 节点；
   - 保存、关闭、重新打开后确认 int 结果落盘。
6. 执行 `RawToIcon`：
   - `iconRaw` 能复制到同级 `icon`；
   - 保存、关闭、重新打开后确认 icon 结果落盘。
7. 执行 XML 导出：
   - `none`、`path`、`base64` 三种 media 模式可用；
   - 视频节点只导出 metadata。
8. 执行 XML 导入/保存并重新打开。
9. 执行复制、粘贴、删除、排序并改名、汉化、图片嗅探、图片对比、Outlink。
10. 关闭文件/窗口，不出现未处理异常。

## Phase 7 下一步建议

完成本清单后，下一步优先做 **Phase 7.2：GUI 使用 provider `XmlExportVersion`**，因为它范围小、方向正确、能复用 Phase 1 的 provider 能力。暂缓 Phase 8/9/10，直到 GUI 基础接入和回归清单稳定。
