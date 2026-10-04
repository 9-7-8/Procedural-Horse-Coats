#!/bin/sh
# Opens the Ixora's Horse Overhaul world converter. Put this file next to ixoras-converter.jar.
# Uses a Java it finds: first on the PATH, then the one the Minecraft launcher installed.
# UNVERIFIED: the launcher runtime folders below are where the official launcher usually puts Java; never tried.
cd "$(dirname "$0")" || exit 1
JAVA=java
if ! command -v java >/dev/null 2>&1; then
  for d in "$HOME/.minecraft/runtime"/*/*/*/bin "$HOME/Library/Application Support/minecraft/runtime"/*/*/*/jre.bundle/Contents/Home/bin; do
    if [ -x "$d/java" ]; then JAVA="$d/java"; break; fi
  done
fi
exec "$JAVA" -jar ixoras-converter.jar "$@"
