#!/usr/bin/env python3
import sys
import zipfile
from pathlib import Path

jar = Path(sys.argv[1] if len(sys.argv) > 1 else "target/OrzRepacker-cli.jar")
if not jar.exists():
    print(f"missing: {jar}", file=sys.stderr)
    sys.exit(1)

with zipfile.ZipFile(jar) as zf:
    names = set(zf.namelist())
    manifest = zf.read("META-INF/MANIFEST.MF").decode("utf-8", errors="replace")

required = "orange/wz/cli/OrangeWzCli.class"
boot_layout = "BOOT-INF/classes/orange/wz/cli/OrangeWzCli.class"
errors = []
if required not in names:
    errors.append(f"missing plain CLI class: {required}")
if boot_layout in names:
    errors.append(f"unexpected Spring Boot layout CLI class: {boot_layout}")
if "Main-Class: orange.wz.cli.OrangeWzCli" not in manifest:
    errors.append("manifest Main-Class is not orange.wz.cli.OrangeWzCli")
if "Spring-Boot-Classes:" in manifest or "Spring-Boot-Lib:" in manifest:
    errors.append("manifest contains Spring Boot repackage metadata")

if errors:
    print(f"{jar} is not a runnable plain CLI jar:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print(f"{jar} is a runnable plain CLI jar")
