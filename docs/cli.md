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
  --media none
```

Media modes:
- `none`
- `base64`
- `file`

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
  --media none
```

## Notes

- This is a minimal prototype for automation and AI workflows.
- WZ version is auto-detected by `WzFile` using file version `-1`.
- Avoid running this on the only copy of a game file. Work in a temporary directory and keep backups.
