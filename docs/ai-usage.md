# AI Usage Guide for OrangeWz CLI

This document is for AI agents using OrangeWz in Linux/headless environments.

## Goal

Use a stable CLI entry point without launching Spring Boot or Swing:

```bash
java -jar target/OrzRepacker-cli.jar ...
```

Main class:

```text
orange.wz.cli.OrangeWzCli
```

## Build

```bash
cd /home/edam/orange-wz
./mvnw -DskipTests compile
./mvnw -DskipTests package
```

When host Java is unavailable, use Docker verification instead:

```bash
scripts/verify-cli-headless.sh
```

Expected CLI artifact:

```text
target/OrzRepacker-cli.jar
```

## Recommended AI Workflow

1. Inspect available key aliases:

```bash
java -jar target/OrzRepacker-cli.jar keys
```

2. Check parseability before exporting:

```bash
java -jar target/OrzRepacker-cli.jar info /data/Skill.img --key gms
```

3. Export XML with deterministic Linux line endings:

```bash
java -jar target/OrzRepacker-cli.jar img-to-xml /data/Skill.img \
  -o /work/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

4. Edit XML using text tools or an AI model.

5. Repack to a new `.img` path first:

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /work/Skill.img.xml \
  -o /work/Skill.modified.img \
  --key gms \
  --zlib-level -1 \
  --zlib-mode default
```

6. Re-run `info` on the new image if needed:

```bash
java -jar target/OrzRepacker-cli.jar info /work/Skill.modified.img --key gms
```

## BeiDou Skill.img Example

Assume the target file is copied to `/work/BeiDou/Skill.img`.

```bash
cd /home/edam/orange-wz
./mvnw -DskipTests package

java -jar target/OrzRepacker-cli.jar info /work/BeiDou/Skill.img --key gms

java -jar target/OrzRepacker-cli.jar img-to-xml /work/BeiDou/Skill.img \
  -o /work/BeiDou/Skill.img.xml \
  --key gms \
  --indent 2 \
  --media none

# AI/text edit /work/BeiDou/Skill.img.xml here.

java -jar target/OrzRepacker-cli.jar xml-to-img /work/BeiDou/Skill.img.xml \
  -o /work/BeiDou/Skill.modified.img \
  --key gms
```

Use `--force` only when overwriting a disposable output file:

```bash
java -jar target/OrzRepacker-cli.jar xml-to-img /work/BeiDou/Skill.img.xml \
  -o /work/BeiDou/Skill.modified.img \
  --key gms \
  --force
```

## Batch IMG Export

For multiple independent `.img` files, use bounded concurrency. Do not use this command for entries inside the same `.wz` or `.ms` file.

```bash
java -jar target/OrzRepacker-cli.jar imgs-to-xml /data/Skill.img /data/Item.img \
  -o /work/img-xml \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --threads 2
```

## Full WZ Export

```bash
java -jar target/OrzRepacker-cli.jar wz-to-xml /data/Skill.wz \
  -o /work/Skill.wz.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default \
  --memory-mode low
```

The output layout follows the internal WZ directory/image names. Each image is exported as `<image>.xml`.

## Read-only MS Export

`.ms` files are supported for inspection/export only. The CLI does not write or repack `.ms` files.

```bash
java -jar target/OrzRepacker-cli.jar info /data/Data.ms --key gms
java -jar target/OrzRepacker-cli.jar ms-to-xml /data/Data.ms \
  -o /work/Data.ms.xml.d \
  --key gms \
  --indent 2 \
  --media none \
  --xml-version default
```

The exporter rejects unsafe internal paths such as absolute paths or `..` traversal before writing output files.

## Machine-Readable Info Output

`info` prints JSON suitable for parsing by scripts/agents. Example shape:

```json
{
  "path": "/work/BeiDou/Skill.img",
  "name": "Skill.img",
  "type": "img",
  "keyAlias": "gms",
  "parsed": true,
  "status": "PARSE_SUCCESS",
  "children": 10,
  "propertyCount": 1234
}
```

For `.wz`, the JSON includes directory/image counts. For `.ms`, it also includes entry/property counts and `readOnly: true`.

## Safety Rules for Agents

- Never edit original game files in place.
- Copy source files to a work directory first.
- Use `xml-to-img` without `--force` by default.
- Use `--media none` for smaller, text-focused XML diffs unless media payloads are required.
- If parsing fails, try the key aliases in this order: `gms`, `cms`, `latest`, `empty`.
- Use `--memory-mode low` for large `.wz` exports.
- Use `--threads` only for separate `.img` input files, not for one `.wz`/`.ms` package.
- Treat `<video .../>` XML entries as metadata-only inspection output; they are not full video round-trip payloads.
