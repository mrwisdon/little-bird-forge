# models/ - rebuildable ground vehicle model files

Zero external dependencies: pure Python 3 standard library. No CAD/3D package is installed in
this environment, and none is required to rebuild these files.

## What this is
- The two ground vehicles of the `battlefield` mod (M1A5 main battle tank, EBAA Wildcat 6x6 IFV)
  expressed as a **parameterised box list** in `src/ground_units.py`.
- Exported as binary **STL** plus a schematic **left-side PNG** per vehicle, so the geometry can be
  inspected and verified outside Minecraft.
- The in-game model remains the Java `LayerDefinition` code under
  `src/main/java/io/github/littlebird/client/`; this directory is an **additional** deliverable and
  does not replace or modify it.

## Units and axes
- 16 model units = 1 block; 1 block = 1 m is an export-only assumption.
- Y up, nose along -Z, +X is the vehicle's right - identical to the Java renderer, so no transform
  is needed to compare the STL with the in-game model.
- STL itself carries no unit declaration; treat the numbers as model units.

## Rebuild
```sh
python3 models/src/build_models.py          # writes models/build/*.stl, *_side.png, box_manifest.json
python3 models/src/verify_models.py         # re-reads the STL files and checks triangle count + bbox
```
`verify_models.py` imports nothing from the builder except the parameter module, so a bad export
cannot pass silently. It exits non-zero on mismatch.

## Verification performed
- `main_battle_tank.stl`: 1404 triangles, bbox `[-33, 0, -78] .. [33, 72, 58]` model units.
- `infantry_fighting_vehicle.stl`: 732 triangles, bbox `[-31, 0, -60.5] .. [31, 62, 50]` model units.
- Both match the parameters exactly (tolerance 1e-3); see `params/ground_units.json` and
  `build/box_manifest.json`.

## Known limitations (read before use)
1. **Box approximation.** Every part is an axis-aligned box; there are no bevels, curves or true
   chamfers. This mirrors the Minecraft model's own representation, so the STL is geometrically
   identical to the in-game shape but is **not** a CAD-grade or print-ready part.
2. **Reference coverage.** The references (see `docs/plan/ground-units-reference-images.md`) have
   no orthographic front/back/top views, so width and roof layout are estimated from 3/4 renders.
3. **Inferred values.** `params/ground_units.json` marks each value as `project`, `reference` or
   `assumption`; the missile tube count is the only `assumption`.
4. **Preview PNG** is a flat painter's-algorithm silhouette for shape checking, not a render.

## Provenance
Generated from the box list that was built up in `docs/plan/ground-units-model-log.md`
(stage 1-3), whose boxes were themselves measured against the reference images recorded in
`docs/plan/ground-units-reference-images.md`.
