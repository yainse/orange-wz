# Orange WZ Linux CLI / AI Docs Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Make `orange-wz` usable on Linux/headless servers as a command-line WZ/IMG/XML tool and generate AI-friendly documentation for MapleStory/BeiDou client asset workflows.

**Architecture:** Keep the existing Swing/Spring GUI entrypoint intact, but add a separate lightweight CLI entrypoint under `orange.wz.cli`. The CLI should call existing provider classes (`WzFile`, `WzImageFile`, `WzXmlFile`, `XmlExport`, `XmlImport`) directly without starting Spring or Swing. Documentation should describe safe read-only export first, then controlled write operations with backups and Git/version-management guidance.

**Tech Stack:** Java 21, Maven Wrapper, existing `orange.wz.provider` parser/writer classes, plain argument parsing for MVP. Optional later upgrade: picocli.

---

## Current Findings

- Repository cloned at: `/home/edam/orange-wz`
- Remote: `https://github.com/yainse/orange-wz.git`
- Current HEAD: `b9eddfc`
- Main project: Java 21 Maven/Spring Boot + Swing GUI.
- Frontend: Vue 3 under `vue/`.
- Existing GUI startup is not Linux/headless friendly because `OrangeWzApplication` starts Spring with `.headless(false)` and `ServerManager` opens `MainFrame` via Swing.
- Existing reusable WZ APIs already support most needed CLI operations:
  - `.wz` parse: `orange.wz.provider.WzFile`
  - standalone `.img` parse/save: `orange.wz.provider.WzImageFile`
  - XML import/export: `orange.wz.provider.WzXmlFile`, `XmlImport`, `XmlExport`
  - default keys/IVs: `orange.wz.provider.WzAESConstant`
- Host initially lacked Java; OpenJDK 21 installation was started for build verification.

## CLI MVP Scope

### Command shape

Executable jar examples:

```bash
java -jar target/orange-wz-cli.jar --help
java -jar target/orange-wz-cli.jar keys
java -jar target/orange-wz-cli.jar info /home/edam/BeiDou-Client/Data/String/Skill.img --key gms
java -jar target/orange-wz-cli.jar img-to-xml /home/edam/BeiDou-Client/Data/String/Skill.img -o /tmp/Skill.img.xml --key gms --indent 2 --media none
java -jar target/orange-wz-cli.jar xml-to-img /tmp/Skill.img.xml -o /tmp/Skill.img --key gms
java -jar target/orange-wz-cli.jar wz-to-xml /home/edam/BeiDou-Client/Data/String.wz -o /tmp/String.xml.dir --key gms --indent 2 --media none
```

### Supported key aliases

Use existing constants:

- `gms`: `WZ_GMS_IV + DEFAULT_KEY`
- `cms`: `WZ_CMS_IV + DEFAULT_KEY`
- `latest`: `WZ_LATEST_IV + DEFAULT_KEY`
- `empty`: `WZ_EMPTY_IV + DEFAULT_KEY`

Also support explicit values later:

```bash
--iv-base64 <base64>
--user-key-base64 <base64>
```

### MVP commands

1. `keys`
   - Print built-in aliases and short descriptions.
   - No file access.

2. `info <path>`
   - If `<path>` ends with `.img`: instantiate `WzImageFile`, parse, print JSON summary.
   - If `<path>` ends with `.wz`: instantiate `WzFile`, parse, print JSON summary: file version, root child count, top-level directories/images.
   - Output should be valid JSON by default for AI/tool consumption.

3. `img-to-xml <input.img> -o <output.xml>`
   - Instantiate `WzImageFile(input, keyAlias, iv, key)`.
   - Call `exportToXml(output, indent, mediaExportType, linux=true)`.
   - Default `--media none` for text/string assets.
   - Support `--media base64|file|none`.

4. `xml-to-img <input.xml> -o <output.img>`
   - Use `XmlImport.importXml(input, keyAlias, iv, key)` or `WzXmlFile`.
   - Call `saveFromXml(output)`.
   - Never overwrite unless `--force` is present.

5. `wz-to-xml <input.wz> -o <output-dir>`
   - Parse `WzFile`.
   - Use `exportFileToXml(outputDir, collector)` then loop collector and call `image.exportToXml(path, indent, media, linux=true)`.
   - Useful for full WZ documentation/search/index generation.

## Files to Create / Modify

### Create: `src/main/java/orange/wz/cli/OrangeWzCli.java`

Responsibilities:

- `public static void main(String[] args)` separate from Spring/Swing.
- Parse command and options.
- Dispatch to command handlers.
- Exit non-zero on invalid input or parse failure.
- Print machine-readable JSON for `info`, and concise logs for conversion commands.

Suggested structure:

```java
package orange.wz.cli;

public final class OrangeWzCli {
    public static void main(String[] args) {
        int code = new OrangeWzCli().run(args);
        System.exit(code);
    }

    int run(String[] args) {
        if (args.length == 0 || has(args, "--help")) return help();
        return switch (args[0]) {
            case "keys" -> keys(args);
            case "info" -> info(args);
            case "img-to-xml" -> imgToXml(args);
            case "xml-to-img" -> xmlToImg(args);
            case "wz-to-xml" -> wzToXml(args);
            default -> error("unknown command: " + args[0]);
        };
    }
}
```

### Create: `src/main/java/orange/wz/cli/WzCliKeys.java`

Responsibilities:

- Map aliases to key name, IV and user key.
- Avoid dependency on GUI `WzKeyStorage`, because that uses encrypted `keys.dat` tied to `ServerManager.getSlog()`.

### Create: `src/main/java/orange/wz/cli/WzCliJson.java`

Responsibilities:

- Small JSON escaping/writing helper or use existing Gson/Jackson dependency.
- Keep `info` output valid JSON.

### Modify: `pom.xml`

Add a non-Spring CLI jar build profile or an additional jar artifact.

Preferred minimal option:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.5.3</version>
  <executions>
    <execution>
      <id>shade-cli</id>
      <phase>package</phase>
      <goals><goal>shade</goal></goals>
      <configuration>
        <shadedArtifactAttached>true</shadedArtifactAttached>
        <shadedClassifierName>cli</shadedClassifierName>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <mainClass>orange.wz.cli.OrangeWzCli</mainClass>
          </transformer>
        </transformers>
      </configuration>
    </execution>
  </executions>
</plugin>
```

This produces:

```text
target/OrzRepacker-cli.jar
```

### Create: `docs/cli.md`

Document:

- Linux prerequisites: Java 21, Maven wrapper.
- Build:

```bash
chmod +x mvnw
./mvnw -DskipTests package
```

- CLI examples for BeiDou client:

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml \
  /home/edam/BeiDou-Client/Data/String/Skill.img \
  -o /tmp/Skill.img.xml \
  --key gms --indent 2 --media none
```

- Safe replacement workflow:

```bash
cd /home/edam/BeiDou-Client
git status --short
cp Data/String/Skill.img Data/String/Skill.img.bak.$(date +%Y%m%d-%H%M%S)
# replace or generate new Skill.img
git add Data/String/Skill.img
git commit -m "fix(client): update Skill.img skill descriptions"
```

### Create: `docs/ai-usage.md`

Document AI-oriented workflows:

- Convert `.img`/`.wz` to XML before asking AI to inspect it.
- Prefer `--media none` for text-like resources (`String`, `Quest`, `Skill` descriptions).
- Use `--media file` for image/sound assets to keep XML readable.
- Keep generated XML under temp/output dirs, not directly inside client repo unless intentionally versioned.
- Suggested prompts for AI:
  - “Compare two Skill.img.xml files and list changed skill IDs/descriptions.”
  - “Generate a CSV of skill id, name, desc, h, pdesc from Skill.img.xml.”

## Implementation Tasks

### Task 1: Add CLI key alias helper

**Files:**
- Create: `src/main/java/orange/wz/cli/WzCliKeys.java`

**Verification:**
- `./mvnw -DskipTests compile`

### Task 2: Add minimal command parser and help

**Files:**
- Create: `src/main/java/orange/wz/cli/OrangeWzCli.java`

**Verification:**
- Build jar or run class.
- `java -cp target/classes:$(dependency classpath) orange.wz.cli.OrangeWzCli --help`

### Task 3: Implement `keys` and `info`

**Files:**
- Modify: `OrangeWzCli.java`
- Create optional: `WzCliJson.java`

**Verification:**

```bash
java -jar target/OrzRepacker-cli.jar keys
java -jar target/OrzRepacker-cli.jar info /home/edam/BeiDou-Client/Data/String/Skill.img --key gms
```

Expected: valid JSON with parse status and child counts.

### Task 4: Implement `img-to-xml`

**Files:**
- Modify: `OrangeWzCli.java`

**Verification:**

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml \
  /home/edam/BeiDou-Client/Data/String/Skill.img \
  -o /tmp/Skill.img.xml --key gms --indent 2 --media none

test -s /tmp/Skill.img.xml
python3 - <<'PY'
import xml.etree.ElementTree as ET
ET.parse('/tmp/Skill.img.xml')
print('xml ok')
PY
```

### Task 5: Implement `xml-to-img`

**Files:**
- Modify: `OrangeWzCli.java`

**Verification:**

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml -o /tmp/Skill.roundtrip.img --key gms
java -jar target/OrzRepacker-cli.jar info /tmp/Skill.roundtrip.img --key gms
```

### Task 6: Implement `wz-to-xml`

**Files:**
- Modify: `OrangeWzCli.java`

**Verification:**

```bash
java -jar target/OrzRepacker-cli.jar wz-to-xml /path/to/String.wz -o /tmp/String.wz.xml --key gms --indent 2 --media none
find /tmp/String.wz.xml -name '*.xml' | head
```

### Task 7: Add CLI jar packaging

**Files:**
- Modify: `pom.xml`

**Verification:**

```bash
./mvnw -DskipTests package
java -jar target/OrzRepacker-cli.jar --help
```

### Task 8: Add documentation

**Files:**
- Create: `docs/cli.md`
- Create: `docs/ai-usage.md`

**Verification:**
- `grep -n "Skill.img" docs/cli.md docs/ai-usage.md`
- Confirm examples use Linux paths and no Windows-only `.bat` workflow.

## Linux Notes

- The parser/writer code itself is portable Java and should work on Linux.
- GUI startup is not suitable for headless Linux; keep CLI entrypoint separate from Spring Boot/Swing.
- Avoid relying on `keys.dat` for CLI because it is encrypted using `ServerManager.getSlog()` and is GUI/server-configuration coupled.
- The existing `XmlExport` already has a `linux` boolean for LF line endings.
- The repo includes `libcrypto-3-x64.dll`, which is Windows-only and should not be required by the Java CLI path.

## AI-Friendly Output Recommendation

After XML export, add optional helper commands later:

```bash
orange-wz-cli skill-summary Skill.img.xml -o Skill.summary.json
orange-wz-cli xml-find Skill.img.xml --path 1001004/desc
orange-wz-cli xml-diff old.Skill.img.xml new.Skill.img.xml -o skill-desc-diff.json
```

These can be implemented in a second phase after the basic WZ/IMG/XML conversions are stable.
