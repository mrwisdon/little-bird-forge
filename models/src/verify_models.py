#!/usr/bin/env python3
"""Independent verification: re-read the exported STL files and check them against the parameters.

Reads only the files on disk (no import of the builder), so a wrong export cannot pass silently.
Exits non-zero on any mismatch.

Usage:  python3 models/src/verify_models.py [--dir models/build]
"""
import argparse
import os
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ground_units  # noqa: E402


def read_stl(path):
    with open(path, "rb") as fh:
        fh.read(80)
        (count,) = struct.unpack("<I", fh.read(4))
        tri = []
        for _ in range(count):
            data = struct.unpack("<12fH", fh.read(50))
            tri.append(((data[3], data[4], data[5]), (data[6], data[7], data[8]),
                        (data[9], data[10], data[11])))
    return tri


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dir", default=os.path.join(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))), "build"))
    parser.add_argument("--tolerance", type=float, default=1e-3)
    args = parser.parse_args()
    failures = 0
    for name in ground_units.VEHICLES:
        path = os.path.join(args.dir, name + ".stl")
        if not os.path.isfile(path):
            print(f"[FAIL] {name}: missing {path}")
            failures += 1
            continue
        tri = read_stl(path)
        verts = [v for t in tri for v in t]
        got = (min(v[0] for v in verts), min(v[1] for v in verts), min(v[2] for v in verts),
               max(v[0] for v in verts), max(v[1] for v in verts), max(v[2] for v in verts))
        want = ground_units.bounds(name)
        expected_tri = len(ground_units.world_boxes(name)) * 12
        ok = (len(tri) == expected_tri
              and all(abs(got[i] - want[i]) <= args.tolerance for i in range(6)))
        print(f"[{'OK' if ok else 'FAIL'}] {name}: triangles={len(tri)} (expected {expected_tri})")
        print(f"       bbox got  {{'min': {got[:3]}, 'max': {got[3:]}}}")
        print(f"       bbox want {{'min': {want[:3]}, 'max': {want[3:]}}}")
        if not ok:
            failures += 1
    print("VERIFY", "FAILED" if failures else "PASSED")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
