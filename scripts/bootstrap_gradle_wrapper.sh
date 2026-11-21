#!/usr/bin/env bash
# Generate the pinned wrapper in an isolated Gradle project, then copy it here.
# Requires Gradle on PATH. The generated distribution is pinned and checksum-verified.
set -euo pipefail
project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$project_dir/gradlew" && -f "$project_dir/gradle/wrapper/gradle-wrapper.jar" ]]; then
    exit 0
fi
command -v gradle >/dev/null || { echo 'Gradle 8.13 is required on PATH.' >&2; exit 1; }
wrapper_tmp="$(mktemp -d)"
trap 'rm -rf -- "$wrapper_tmp"' EXIT
printf 'rootProject.name = "dan-wrapper-bootstrap"\n' > "$wrapper_tmp/settings.gradle.kts"
(
    cd "$wrapper_tmp"
    distribution_sha256="$(curl --fail --silent --show-error --location \
        https://services.gradle.org/distributions/gradle-8.13-bin.zip.sha256)"
    [[ "$distribution_sha256" =~ ^[0-9a-f]{64}$ ]] || { echo 'Invalid Gradle checksum' >&2; exit 1; }
    gradle wrapper --gradle-version 8.13 --distribution-type bin \
        --gradle-distribution-sha256-sum "$distribution_sha256"
)
mkdir -p "$project_dir/gradle/wrapper"
cp "$wrapper_tmp/gradlew" "$wrapper_tmp/gradlew.bat" "$project_dir/"
cp "$wrapper_tmp/gradle/wrapper/gradle-wrapper.jar" "$wrapper_tmp/gradle/wrapper/gradle-wrapper.properties" "$project_dir/gradle/wrapper/"
chmod +x "$project_dir/gradlew"
