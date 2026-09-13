"""CAD-flavoured mesh of the M1A5 tank: real cylinders and chamfers instead of Minecraft boxes.

Zero external dependencies. Primitive helpers return triangles (with normals) in blocks, Y up,
nose along -Z, so the result can be compared with the in-game model. This is a demonstration of
what route B looks like without installing a CAD kernel; a real B-rep (fillets, STEP) still needs
OpenSCAD or build123d.
"""
import math

Tri = tuple  # ((p0,p1,p2), normal)


def _norm(v):
    n = math.sqrt(sum(c * c for c in v)) or 1.0
    return (v[0] / n, v[1] / n, v[2] / n)


def _sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _tri(a, b, c):
    n = _norm(_cross(_sub(b, a), _sub(c, a)))
    return ((a, b, c), n)


def box(center, size):
    cx, cy, cz = center
    sx, sy, sz = (s / 2.0 for s in size)
    v = [(cx - sx, cy - sy, cz - sz), (cx + sx, cy - sy, cz - sz), (cx + sx, cy + sy, cz - sz),
         (cx - sx, cy + sy, cz - sz), (cx - sx, cy - sy, cz + sz), (cx + sx, cy - sy, cz + sz),
         (cx + sx, cy + sy, cz + sz), (cx - sx, cy + sy, cz + sz)]
    f = [(0, 2, 1), (0, 3, 2), (4, 5, 6), (4, 6, 7), (0, 1, 5), (0, 5, 4),
         (3, 7, 6), (3, 6, 2), (0, 4, 7), (0, 7, 3), (1, 2, 6), (1, 6, 5)]
    return [_tri(v[a], v[b], v[c]) for (a, b, c) in f]


def prism_x(profile, width):
    """profile: list of (y, z) going counter-clockwise; extruded along +/-X."""
    h = width / 2.0
    tris = []
    n = len(profile)
    for i in range(n):
        y0, z0 = profile[i]
        y1, z1 = profile[(i + 1) % n]
        a = (-h, y0, z0)
        b = (h, y0, z0)
        c = (h, y1, z1)
        d = (-h, y1, z1)
        tris.append(_tri(a, b, c))
        tris.append(_tri(a, c, d))
    # caps
    for side in (-h, h):
        for i in range(1, n - 1):
            p0 = (side, profile[0][0], profile[0][1])
            p1 = (side, profile[i][0], profile[i][1])
            p2 = (side, profile[i + 1][0], profile[i + 1][1])
            tris.append(_tri(p0, p1, p2))
    return tris

def cylinder(center, radius, length, axis="x", segments=24):
    cx, cy, cz = center
    tris = []

    def point(angle, side):
        if axis == "x":
            return (cx + side * length / 2.0, cy + radius * math.cos(angle),
                    cz + radius * math.sin(angle))
        if axis == "z":
            return (cx + radius * math.cos(angle), cy + radius * math.sin(angle),
                    cz + side * length / 2.0)
        return (cx + radius * math.cos(angle), cy + side * length / 2.0,
                cz + radius * math.sin(angle))

    for i in range(segments):
        a0 = 2.0 * math.pi * i / segments
        a1 = 2.0 * math.pi * (i + 1) / segments
        m0, p0 = point(a0, -1), point(a0, 1)
        m1, p1 = point(a1, -1), point(a1, 1)
        tris.append(_tri(m0, p0, p1))
        tris.append(_tri(m0, p1, m1))
        cap0 = (cx - length / 2.0, cy, cz) if axis == "x" else (
            (cx, cy, cz - length / 2.0) if axis == "z" else (cx, cy - length / 2.0, cz))
        cap1 = (cx + length / 2.0, cy, cz) if axis == "x" else (
            (cx, cy, cz + length / 2.0) if axis == "z" else (cx, cy + length / 2.0, cz))
        tris.append(_tri(cap0, m0, m1))
        tris.append(_tri(cap1, p1, p0))
    return tris


def turret_mesh():
    """Chamfered turret: a plan-view octagon prism with a bustle."""
    r = 1.15
    ch = 0.42
    plan = [(-r + ch, -r), (r - ch, -r), (r, -r + ch), (r, r - ch),
            (r - ch, r), (-r + ch, r), (-r, r - ch), (-r, -r + ch)]
    tris = []
    y0, y1 = 2.05, 2.75
    top = [(x, y1, z) for (x, z) in plan]
    bottom = [(x, y0, z) for (x, z) in plan]
    n = len(plan)
    for i in range(n):
        j = (i + 1) % n
        tris.append(_tri(bottom[i], top[i], top[j]))
        tris.append(_tri(bottom[i], top[j], bottom[j]))
    for i in range(1, n - 1):
        tris.append(_tri(top[0], top[i], top[i + 1]))
        tris.append(_tri(bottom[0], bottom[i + 1], bottom[i]))
    return tris


def main_battle_tank():
    tris = []
    # hull and sloped glacis (prism in X)
    tris += box((0.0, 1.45, 0.2), (3.5, 1.1, 6.4))
    tris += prism_x([(1.95, -3.0), (1.95, -3.3), (1.05, -4.15), (1.0, -3.9)], 3.5)
    # tracks
    tris += box((-1.62, 0.35, 0.0), (0.55, 0.7, 6.0))
    tris += box((1.62, 0.35, 0.0), (0.55, 0.7, 6.0))
    # road wheels, sprocket, idler
    for i in range(7):
        z = -2.45 + i * 0.82
        for x in (-1.58, 1.58):
            tris += cylinder((x, 0.35, z), 0.34, 0.28, "x", 20)
    for z in (-3.05, 3.05):
        for x in (-1.58, 1.58):
            tris += cylinder((x, 0.35, z), 0.44, 0.26, "x", 20)
    # turret, mantlet, barrel, thermal sleeve, muzzle
    tris += turret_mesh()
    tris += box((0.0, 2.35, -1.0), (1.0, 0.6, 0.5))
    tris += cylinder((0.0, 2.35, -3.1), 0.13, 4.2, "z", 24)
    tris += cylinder((0.0, 2.35, -2.2), 0.19, 1.7, "z", 24)
    tris += cylinder((0.0, 2.35, -5.35), 0.17, 0.4, "z", 24)
    # cupola and commander's gun
    tris += cylinder((0.05, 2.75, -0.15), 0.44, 0.42, "y", 20)
    tris += cylinder((0.05, 3.12, -0.8), 0.05, 1.1, "z", 12)
    # smoke discharger banks
    for i in range(6):
        x = -1.05 + (i % 2) * 0.25
        y = 2.5 + (i // 2) * 0.22
        for side in (-1, 1):
            tris += cylinder((side * x, y, -1.25), 0.06, 0.4, "z", 10)
    return tris