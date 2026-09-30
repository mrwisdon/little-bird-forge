#!/usr/bin/env python3
"""mc-verify: dump independent Blender-side facts for a .blend (read-only, never saves).

    blender -b <file.blend> --python tools/_b2verify_blendfacts.py
Prints one B2FACTS line and writes /tmp/b2verify/blend_facts.json.
"""
import json
import os
import sys

import bpy
from mathutils import Vector

MESHES = [o for o in bpy.data.objects if o.type == "MESH"]
lo = Vector((1e18, 1e18, 1e18))
hi = Vector((-1e18, -1e18, -1e18))
vis_lo = Vector((1e18, 1e18, 1e18))
vis_hi = Vector((-1e18, -1e18, -1e18))
tris = 0
for o in MESHES:
    me = o.data
    me.calc_loop_triangles()
    tris += len(me.loop_triangles)
    vis = not o.hide_render
    for corner in o.bound_box:
        w = o.matrix_world @ Vector(corner)
        for i in range(3):
            lo[i] = min(lo[i], w[i]); hi[i] = max(hi[i], w[i])
            if vis:
                vis_lo[i] = min(vis_lo[i], w[i]); vis_hi[i] = max(vis_hi[i], w[i])
prefix = {}
for o in MESHES:
    key = o.name.split("_")[0].split(".")[0]
    prefix[key] = prefix.get(key, 0) + 1
facts = {
    "blend": bpy.data.filepath,
    "objects_mesh": len(MESHES),
    "objects_all": len(bpy.data.objects),
    "triangles": tris,
    "bbox_m": [round(v, 4) for v in list(lo) + list(hi)],
    "dims_m": [round(hi[i] - lo[i], 4) for i in range(3)],
    "visible_bbox_m": [round(v, 4) for v in list(vis_lo) + list(vis_hi)],
    "visible_dims_m": [round(vis_hi[i] - vis_lo[i], 4) for i in range(3)],
    "hidden_render_meshes": sum(1 for o in MESHES if o.hide_render),
    "objects_by_prefix": dict(sorted(prefix.items(), key=lambda kv: -kv[1])[:14]),
    "unit_settings_scale": bpy.context.scene.unit_settings.scale_length,
    "collections": sorted({c.name for o in MESHES for c in o.users_collection}),
}
os.makedirs("/tmp/b2verify", exist_ok=True)
json.dump(facts, open("/tmp/b2verify/blend_facts.json", "w"), indent=1)
print("B2FACTS " + json.dumps(facts))
sys.stdout.flush()
