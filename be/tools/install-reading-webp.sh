#!/bin/sh
# Resmi libwebp arşivini SHA ile doğrular; yalnız seçili iki aracı kurar.
set -eu
platform="${1:?platform required: mac-arm64/linux-aarch64/linux-x86-64}"
destination="${2:?destination required}"
case "$platform" in
  mac-arm64) checksum=bc6bf84cc70f3f8574fba797d1e4a7dea4feebe9fa4be919f202413ea2b3b8f2 ;;
  linux-aarch64) checksum=69f5eebe203e0f3942fe37986209a1725741be19c152950a4283b376c95ec798 ;;
  linux-x86-64) checksum=1c5ffab71efecefa0e3c23516c3a3a1dccb45cc310ae1095c6f14ae268e38067 ;;
  *) exit 2 ;;
esac
working="$(mktemp -d)"
trap 'rm -rf "$working"' EXIT HUP INT TERM
archive="libwebp-1.6.0-$platform.tar.gz"
curl --fail --location --proto '=https' --tlsv1.2 "https://storage.googleapis.com/downloads.webmproject.org/releases/webp/$archive" --output "$working/archive.tar.gz"
if command -v sha256sum >/dev/null 2>&1; then
  printf '%s  %s\n' "$checksum" "$working/archive.tar.gz" | sha256sum -c -
else
  printf '%s  %s\n' "$checksum" "$working/archive.tar.gz" | shasum -a 256 -c -
fi
mkdir -p "$destination"
for name in cwebp webpmux; do
  tar -xzOf "$working/archive.tar.gz" "libwebp-1.6.0-$platform/bin/$name" > "$destination/$name"
done
chmod 755 "$destination/cwebp" "$destination/webpmux"
"$destination/cwebp" -version
"$destination/webpmux" -version
