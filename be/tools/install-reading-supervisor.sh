#!/bin/sh
# Yerel kaynak kodunu aynı platform için derler; source kimliğini binary'ye bağlar.
set -eu
destination="${1:?destination required}"
source_directory=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
source_file="$source_directory/reading-process-supervisor.c"
if command -v sha256sum >/dev/null 2>&1; then
  source_sha=$(sha256sum "$source_file" | cut -d ' ' -f 1)
else
  source_sha=$(shasum -a 256 "$source_file" | cut -d ' ' -f 1)
fi
mkdir -p "$destination"
"${CC:-cc}" -std=c11 -O2 -Wall -Wextra -Werror "-DSOURCE_SHA256=\"$source_sha\"" "$source_file" -o "$destination/reading-process-supervisor"
"$destination/reading-process-supervisor" --version
