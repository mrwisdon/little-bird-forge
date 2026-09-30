#!/usr/bin/env python3
"""mc-verify: insert-only evidence for the registration files (read-only git)."""
import os
import subprocess

ROOT = "/root/projects/little-bird-forge"
FILES = ["src/main/java/io/github/littlebird/registry/ModEntities.java",
         "src/main/java/io/github/littlebird/registry/ModItems.java",
         "src/main/java/io/github/littlebird/registry/ModCreativeTabs.java",
         "src/main/java/io/github/littlebird/client/ClientModEvents.java",
         "src/main/java/io/github/littlebird/entity/flight/JetType.java"]
for f in FILES:
    out = subprocess.run(["git", "-C", ROOT, "diff", "-U0", "--", f],
                         capture_output=True, text=True).stdout
    lines = out.splitlines()
    add = [l for l in lines if l.startswith("+") and not l.startswith("+++")]
    rem = [l for l in lines if l.startswith("-") and not l.startswith("---")]
    b2a = [l for l in add if "b2" in l.lower()]
    b2r = [l for l in rem if "b2" in l.lower()]
    print("%-20s added=%2d removed=%2d b2_added=%2d b2_removed=%d" % (
        os.path.basename(f), len(add), len(rem), len(b2a), len(b2r)))
    for l in b2a:
        print("      + %s" % l[1:].strip()[:96])
    for l in rem[:3]:
        print("      - (pre-existing, not B2) %s" % l[1:].strip()[:70])
st = subprocess.run(["git", "-C", ROOT, "status", "--short"], capture_output=True,
                    text=True).stdout.splitlines()
b2new = [l for l in st if l.startswith("??") and "b2" in l.lower()]
b2mod = [l for l in st if l.startswith(" M") and "b2" in l.lower()]
print("untracked B2 files=%d ; modified-tracked B2-named files=%d" % (len(b2new), len(b2mod)))
print("ALL b2-removed-lines across the 5 files =",
      sum(1 for f in FILES for l in subprocess.run(
          ["git", "-C", ROOT, "diff", "-U0", "--", f], capture_output=True, text=True
      ).stdout.splitlines() if l.startswith("-") and "b2" in l.lower()))
