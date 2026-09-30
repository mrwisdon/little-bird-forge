#!/bin/bash
# Deploy the built mod jar into the PCL2 instance.
#
# Two rules, both learned the hard way:
#   1. Never write the mods jar while Minecraft is running. Forge loads classes lazily, so a client
#      that survives across a jar swap dies later with NoClassDefFoundError for classes that are
#      demonstrably inside the jar - exactly the crash chased at 23:27 on 2026-09-30.
#   2. Replace atomically (write a sibling temp file, then rename). A rename either lands whole or
#      not at all, so even a mistake here cannot leave a half written jar for a reader to trip on.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
JAR="$ROOT/build/libs/battlefield-0.1.0.jar"
MODS="/mnt/c/Users/19818/AppData/Roaming/.minecraft/versions/1.20.1-Forge_47.4.20/mods"
TARGET="$MODS/battlefield-0.1.0.jar"

[ -f "$JAR" ] || { echo "no jar at $JAR - build first"; exit 1; }
[ -d "$MODS" ] || { echo "no mods dir at $MODS"; exit 1; }

if tasklist.exe 2>/dev/null | grep -qiE "javaw|java\.exe"; then
  echo "REFUSING: Minecraft looks like it is running. Quit it completely, then deploy."
  echo "(the built jar is ready at $JAR)"
  exit 2
fi

cp "$JAR" "$MODS/.battlefield-0.1.0.jar.new"
mv -f "$MODS/.battlefield-0.1.0.jar.new" "$TARGET"
echo "deployed:"
sha256sum "$JAR" "$TARGET"
