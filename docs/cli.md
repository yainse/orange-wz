# OrangeWz Headless CLI

Minimal Linux/headless command line entry point for OrangeWz. It does **not** start Spring Boot or Swing.

## Build on Linux

Requirements:
- Java 21
- Maven wrapper from this repo

```bash
cd /home/edam/orange-wz
./mvnw -DskipTests package
```

If the host does not have a working Java/JAVA_HOME, use the Docker-based verification script instead:

```bash
scripts/verify-cli-headless.sh
```

CLI jar output:

```bash
target/OrzRepacker-cli.jar
```

If the final GUI packaging plugins fail in your environment, compile-only verification is still useful:

```bash
./mvnw -DskipTests compile
```

## Verify the headless path

For maintainer or CI-style verification, run:

```bash
scripts/verify-cli-headless.sh
```

The script performs:
- Docker Maven full test and fails if `MainFrame` / `HeadlessException` appears in test logs.
- Docker Maven package.
- Plain CLI jar structural check via `scripts/check-cli-jar.py`.
- Docker JRE smoke for `--help` and `keys`.
- Provider/headless forbidden dependency grep for GUI/native/process APIs.
- 基础 added-line 安全扫描和 `git diff --check`。基础安全扫描用于快速拦截常见问题，不能替代人工审查。

Optional environment overrides:

```bash
MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21 \
JRE_IMAGE=eclipse-temurin:21-jre \
M2_CACHE=/root/.m2 \
scripts/verify-cli-headless.sh
```

## Commands

Use the CLI with:

```bash
java -jar target/OrzRepacker-cli.jar <command> [args]
```

### Help

```bash
java -jar target/OrzRepacker-cli.jar --help
```

### Key aliases

```bash
java -jar target/OrzRepacker-cli.jar keys
```

Supported aliases:
- `gms`
- `cms`
- `latest`
- `empty`

Default: `gms`

### Info JSON

Parse `.img`, `.wz`, or `.ms` and print a JSON summary. `.ms` support is read-only.

```bash
java -jar target/OrzRepacker-cli.jar info /path/to/Skill.img --key gms
java -jar target/OrzRepacker-cli.jar info /path/to/Skill.wz --key gms
java -jar target/OrzRepacker-cli.jar info /path/to/Data.ms --key gms
```

Fields include:
- `path`
- `type`
- `name`
- `parsed`
- `status`
- child/image/directory counts where applicable
- `.ms` summaries also include `entries`, `propertyCount`, and `readOnly: true`

### img-to-xml

Export one `.img` file to XML. Output uses Linux LF line endings.

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml /path/to/Skill.img \
  -o /tmp/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

Media modes:
- `none`
- `base64`
- `file`

XML export versions:
- `default`: current OrangeWz CLI XML format. Canvas nodes include `width`, `height`, `format`, and `scale`.
- `v125`: v125-compatible export shape. Root omits CLI metadata attributes, canvas nodes keep `width`/`height`, and empty `imgdir` nodes use explicit closing tags. This is an export compatibility option; `xml-to-img` still needs media data for image round-trip.

Canvas#Video nodes are parsed in headless mode and exported as metadata-only `<video .../>` entries in this phase; video playback/media extraction is reserved for later GUI/video work. The metadata-only `<video>` tag is not importable by `xml-to-img` yet, so XML files containing these entries should be treated as inspection/export artifacts rather than full round-trip sources.

### imgs-to-xml

Export multiple independent `.img` files to XML files with bounded concurrency. This command is intentionally limited to separate `.img` files; `wz-to-xml` and `ms-to-xml` still export sequentially because shared WZ/MS reader safety is not assumed.

```bash
java -jar target/OrzRepacker-cli.jar imgs-to-xml /path/to/Skill.img /path/to/Item.img \
  -o /tmp/img-xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --threads 2
```

Threading rules:
- `--threads` defaults to `1` and accepts `1..4`.
- It only applies to `imgs-to-xml`.
- Each worker creates an independent `WzImageFile`; no single `.wz` package or `.ms` package is parsed in parallel.
- Output files are named `<input-file-name>.xml`; duplicate output names are rejected before workers start.
- Batch export is not transactional: successfully written XML files are not rolled back if another input fails.

### xml-to-img

Convert XML back to `.img`. By default, output overwrite is refused. PNG zlib options only apply to canvas data imported from XML; they do not affect `img-to-xml`, `wz-to-xml`, `.ms` read-only export, or native/libimagequant compression.

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml \
  -o /tmp/Skill.modified.img \
  --key gms \
  --zlib-level -1 \
  --zlib-mode default
```

Supported PNG zlib options for `xml-to-img`:
- `--zlib-level -1|0..9`: `-1` uses the JDK zlib default compression level; `0` stores without compression; `9` is best compression. Default: `-1`.
- `--zlib-mode default|filtered|huffman_only|rle|brute_smallest`: chooses the zlib strategy for WZ PNG payloads. `brute_smallest` tries several Java zlib strategies and keeps the smallest output, but can be slower. Default: `default`.
- `legacy-skill-effect` and native strong compression are intentionally not enabled in the CLI baseline.

Overwrite explicitly:

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml \
  -o /tmp/Skill.modified.img \
  --key gms \
  --force \
  --zlib-level 9 \
  --zlib-mode brute_smallest
```

### wz-to-xml

Export every image in a `.wz` package to XML files.

```bash
java -jar target/OrzRepacker-cli.jar wz-to-xml /path/to/Skill.wz \
  -o /tmp/skill-xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --memory-mode low
```

Memory modes for `wz-to-xml`:
- `normal`: default behavior; keeps parsed image state and caches according to existing provider semantics.
- `low`: after each image XML export succeeds, flushes decoded canvas images, drops compressed copies that can be safely reloaded from the original WZ reader, and unparses unchanged images when safe. This mode is intended for large batch exports and does not apply to `xml-to-img`, because XML-origin image data may not be reloadable.

### ms-to-xml

Export every readable image entry in a `.ms` package to XML files. This command is read-only: it parses/decrypts entries and writes XML output, but it never writes back to the `.ms` source file and does not support `.ms` repacking.

```bash
java -jar target/OrzRepacker-cli.jar ms-to-xml /path/to/Data.ms \
  -o /tmp/data-ms-xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

Output paths preserve the internal entry structure and append `.xml` to each entry name. Unsafe entry paths such as absolute paths or `..` traversal are rejected before writing.

## Recommended headless workflows

### Inspect-first flow for AI agents

1. Run `keys` to confirm the key aliases available in this build.
2. Run `info` on the source `.img`, `.wz`, or `.ms` before attempting export.
3. Prefer `--media none` for script/AI text inspection when image/audio payloads are not needed.
4. Use `img-to-xml` for one `.img`, `imgs-to-xml --threads N` for multiple independent `.img` files, `wz-to-xml --memory-mode low` for large WZ packages, and `ms-to-xml` for read-only MS package inspection.
5. Use `xml-to-img` to write a new output path first; only add `--force` for disposable outputs.
6. Keep original game/client files backed up and commit generated resources separately from code when possible.

### Large export recommendations

- Use `--memory-mode low` with `wz-to-xml` on large packages.
- Keep `--threads` at `1` unless exporting multiple independent `.img` files; never assume one `.wz`/`.ms` package can be parsed concurrently.
- Use `--xml-version v125` only when a downstream legacy tool requires the v125-compatible XML shape; otherwise keep `default`.
- Use `--zlib-mode brute_smallest` only when output size matters more than conversion speed.

## Notes

- This CLI is intended for automation and AI workflows.
- WZ version is auto-detected by `WzFile` using file version `-1`.
- Avoid running this on the only copy of a game file. Work in a temporary directory and keep backups.
