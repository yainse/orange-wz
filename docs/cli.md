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

CLI jar output:

```bash
target/OrzRepacker-cli.jar
```

If the final GUI packaging plugins fail in your environment, compile-only verification is still useful:

```bash
./mvnw -DskipTests compile
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

Parse `.img` or `.wz` and print a JSON summary.

```bash
java -jar target/OrzRepacker-cli.jar info /path/to/Skill.img --key gms
java -jar target/OrzRepacker-cli.jar info /path/to/Skill.wz --key gms
```

Fields include:
- `path`
- `type`
- `name`
- `parsed`
- `status`
- child/image/directory counts where applicable

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

### xml-to-img

Convert XML back to `.img`. By default, output overwrite is refused.

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml \
  -o /tmp/Skill.modified.img \
  --key gms
```

Overwrite explicitly:

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /tmp/Skill.img.xml \
  -o /tmp/Skill.modified.img \
  --key gms \
  --force
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

## Notes

- This is a minimal prototype for automation and AI workflows.
- WZ version is auto-detected by `WzFile` using file version `-1`.
- Avoid running this on the only copy of a game file. Work in a temporary directory and keep backups.
