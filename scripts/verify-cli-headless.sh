#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT_DIR"

MAVEN_IMAGE=${MAVEN_IMAGE:-maven:3.9.9-eclipse-temurin-21}
JRE_IMAGE=${JRE_IMAGE:-eclipse-temurin:21-jre}
M2_CACHE=${M2_CACHE:-/root/.m2}
CLI_JAR=${CLI_JAR:-target/OrzRepacker-cli.jar}

if command -v sudo >/dev/null 2>&1 && sudo -n true >/dev/null 2>&1; then
  SUDO=(sudo -n)
else
  SUDO=()
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required for headless verification" >&2
  exit 1
fi

run() {
  echo
  echo ">>> $*"
  "$@"
}

run_docker_maven() {
  "${SUDO[@]}" docker run --rm \
    -v "$ROOT_DIR:/workspace" \
    -v "$M2_CACHE:/root/.m2" \
    -w /workspace \
    "$MAVEN_IMAGE" \
    "$@"
}

run_docker_jre() {
  "${SUDO[@]}" docker run --rm \
    -v "$ROOT_DIR:/workspace" \
    -w /workspace \
    "$JRE_IMAGE" \
    "$@"
}

echo "OrangeWz CLI/headless verification"
echo "root: $ROOT_DIR"
echo "maven image: $MAVEN_IMAGE"
echo "jre image: $JRE_IMAGE"
echo "m2 cache: $M2_CACHE"

cleanup_build_outputs() {
  if [ -d target ] || [ -e dependency-reduced-pom.xml ]; then
    "${SUDO[@]}" rm -rf target dependency-reduced-pom.xml
  fi
}

run cleanup_build_outputs

run run_docker_maven bash -lc 'mvn -q test 2>&1 | tee /tmp/orange-wz-full-test.log; status=${PIPESTATUS[0]}; if grep -q "MainFrame\|HeadlessException" /tmp/orange-wz-full-test.log; then echo "HEADLESS_GUI_LOG_FOUND" >&2; exit 2; fi; exit $status'

run run_docker_maven mvn -q -DskipTests package

run python3 scripts/check-cli-jar.py "$CLI_JAR"

run run_docker_jre bash -lc 'java -jar target/OrzRepacker-cli.jar --help >/tmp/orange-wz-help.txt && java -jar target/OrzRepacker-cli.jar keys >/tmp/orange-wz-keys.json && grep -q "OrangeWz headless CLI" /tmp/orange-wz-help.txt && grep -q "img-to-xml" /tmp/orange-wz-help.txt && grep -q "imgs-to-xml" /tmp/orange-wz-help.txt && grep -q "xml-to-img" /tmp/orange-wz-help.txt && grep -q "wz-to-xml" /tmp/orange-wz-help.txt && grep -q "ms-to-xml" /tmp/orange-wz-help.txt && grep -q "zlib-mode" /tmp/orange-wz-help.txt && grep -q "memory-mode" /tmp/orange-wz-help.txt && grep -q "\"gms\"" /tmp/orange-wz-keys.json && grep -q "\"cms\"" /tmp/orange-wz-keys.json && grep -q "\"latest\"" /tmp/orange-wz-keys.json && grep -q "\"empty\"" /tmp/orange-wz-keys.json'

provider_scan() {
  set +e
  git grep -n "orange\.wz\.gui\|MainFrame\|ExportXmlData\|ExportXmlDialog\|javax\.swing\|System\.load\|Runtime\.getRuntime\|ProcessBuilder\|org\.pngquant\|libimagequant" -- \
    src/main/java/orange/wz/provider \
    src/main/java/orange/wz/provider/tools \
    src/main/java/orange/wz/provider/properties \
    src/main/java/orange/wz/provider/ms
  local status=$?
  set -e
  if [ "$status" -eq 0 ]; then
    echo "Provider/headless layer has forbidden GUI/native/process dependency" >&2
    return 1
  fi
  if [ "$status" -eq 1 ]; then
    return 0
  fi
  echo "Provider/headless dependency scan failed with git grep status $status" >&2
  return "$status"
}

run provider_scan

run bash -lc 'python3 - <<'"'"'PY'"'"'
import re
import subprocess
text = subprocess.run(["git", "diff", "HEAD"], text=True, capture_output=True, check=True).stdout
checks = {
    "hardcoded_secret": r"^\+.*(?:api_key|secret|password|token|credential|jdbc:|mongodb:|redis:)",
    "shell_injection": r"^\+.*(?:Runtime\.getRuntime\(\)\.exec|ProcessBuilder\()",
    "eval_exec": r"^\+.*(?:eval\(|exec\()",
    "unsafe_deser": r"^\+.*(?:ObjectInputStream|readObject\()",
}
failed = False
for name, pattern in checks.items():
    matches = re.findall(pattern, text, re.IGNORECASE | re.MULTILINE)
    print(f"{name}: {len(matches)}")
    if matches:
        failed = True
if failed:
    raise SystemExit(1)
PY'

run git diff --check

run cleanup_build_outputs

echo
echo "OK: CLI/headless verification passed"
