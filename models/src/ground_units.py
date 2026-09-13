"""Rebuildable box definition of the two ground vehicles.

Single source of truth for the exported model files. Coordinates are Minecraft model units
(16 units = 1 block), Y up, nose along -Z, +X to the vehicle's right - exactly the unit space the
Java renderers use, so the STL export lines up with the in-game model.

Parts listed under a vehicle's TURRET group are offset by TURRET_OFFSET_Y at build time, because
in the Java models they are children of the animated turret part.
"""

TURRET_OFFSET_Y = {"main_battle_tank": 34.0, "infantry_fighting_vehicle": 36.0}

UNITS_PER_BLOCK = 16.0


def _box(part, turret, x, y, z, w, h, d):
    return {"part": part, "turret": turret, "min": (x, y, z), "size": (w, h, d)}


def main_battle_tank():
    boxes = []
    # ---- hull ----
    boxes.append(_box("hull_lower", False, -26, 8, -50, 52, 18, 100))
    boxes.append(_box("hull_upper", False, -28, 26, -46, 56, 8, 92))
    boxes.append(_box("glacis_lower", False, -26, 14, -58, 52, 12, 8))
    boxes.append(_box("glacis_upper", False, -24, 22, -62, 48, 10, 8))
    boxes.append(_box("engine_deck", False, -26, 34, 16, 52, 6, 34))
    boxes.append(_box("rear_plate", False, -26, 10, 50, 52, 24, 4))
    # ---- running gear: 7 road wheels a side ----
    boxes.append(_box("track_left", False, -31, 0, -54, 5, 14, 108))
    boxes.append(_box("track_right", False, 26, 0, -54, 5, 14, 108))
    for i in range(9):
        z = -50.0 + i * 12.0
        boxes.append(_box("tread_left_%d" % i, False, -31, 14, z, 5, 2, 8))
        boxes.append(_box("tread_right_%d" % i, False, 26, 14, z, 5, 2, 8))
    for i in range(7):
        z = -42.0 + i * 14.0 - 5.0
        boxes.append(_box("wheel_left_%d" % i, False, -30, 2, z, 5, 10, 10))
        boxes.append(_box("wheel_right_%d" % i, False, 25, 2, z, 5, 10, 10))
        boxes.append(_box("hub_left_%d" % i, False, -32, 6, z + 4, 1, 2, 2))
        boxes.append(_box("hub_right_%d" % i, False, 31, 6, z + 4, 1, 2, 2))
    boxes.append(_box("sprocket_left", False, -30, 3, 44, 5, 12, 9))
    boxes.append(_box("sprocket_right", False, 25, 3, 44, 5, 12, 9))
    boxes.append(_box("idler_left", False, -30, 3, -53, 5, 12, 9))
    boxes.append(_box("idler_right", False, 25, 3, -53, 5, 12, 9))
    # ---- fittings ----
    for i in range(6):
        z = -50.0 + i * 17.0
        boxes.append(_box("skirt_left_%d" % i, False, -32, 10, z, 3, 16, 15))
        boxes.append(_box("skirt_left_bolt_%d" % i, False, -33, 12, z + 3, 1, 12, 1))
        boxes.append(_box("skirt_right_%d" % i, False, 29, 10, z, 3, 16, 15))
        boxes.append(_box("skirt_right_bolt_%d" % i, False, 32, 12, z + 3, 1, 12, 1))
    boxes.append(_box("headlight_left", False, -27, 24, -60, 6, 6, 4))
    boxes.append(_box("headlight_right", False, 21, 24, -60, 6, 6, 4))
    boxes.append(_box("tow_hook_left", False, -10, 18, -64, 4, 4, 4))
    boxes.append(_box("tow_hook_right", False, 6, 18, -64, 4, 4, 4))
    for i in range(8):
        boxes.append(_box("grille_slat_%d" % i, False, -22, 40, 18.0 + i * 4.0, 44, 1, 2))
    boxes.append(_box("exhaust", False, -32, 14, 52, 6, 10, 6))
    # ---- turret (offset at build time) ----
    boxes.append(_box("turret", True, -16, 0, -20, 32, 16, 40))
    boxes.append(_box("turret_seam_front", True, -14, 16, -18, 28, 1, 1))
    boxes.append(_box("turret_seam_rear", True, -14, 16, 14, 28, 1, 1))
    boxes.append(_box("bustle", True, -14, 2, 20, 28, 12, 14))
    boxes.append(_box("mantlet", True, -8, 2, -26, 16, 10, 6))
    boxes.append(_box("barrel", True, -3, 4, -74, 6, 6, 48))
    boxes.append(_box("bore_evacuator", True, -4, 3, -66, 8, 8, 4))
    boxes.append(_box("thermal_sleeve", True, -4, 3, -60, 8, 8, 20))
    boxes.append(_box("muzzle", True, -4, 3, -78, 8, 8, 4))
    boxes.append(_box("cupola", True, -8, 16, -8, 14, 8, 14))
    for i, x in enumerate((-6, -2, 2)):
        boxes.append(_box("periscope_%d" % i, True, x, 24, -10, 2, 2, 2))
    boxes.append(_box("loader_hatch", True, 6, 16, 0, 10, 3, 12))
    boxes.append(_box("smoke_left", True, -18, 8, -18, 4, 6, 10))
    boxes.append(_box("smoke_right", True, 14, 8, -18, 4, 6, 10))
    boxes.append(_box("commander_gun_mount", True, -2, 24, -8, 4, 6, 6))
    boxes.append(_box("commander_gun_barrel", True, -1, 26, -24, 2, 2, 16))
    boxes.append(_box("gunner_sight", True, -10, 16, -16, 8, 6, 8))
    boxes.append(_box("wind_sensor", True, -2, 16, 30, 4, 6, 4))
    boxes.append(_box("antenna_left", True, -12, 16, 26, 1, 22, 1))
    boxes.append(_box("antenna_right", True, 10, 16, 28, 1, 18, 1))
    return boxes


def infantry_fighting_vehicle():
    boxes = []
    # ---- hull ----
    boxes.append(_box("hull_lower", False, -24, 10, -46, 48, 16, 92))
    boxes.append(_box("hull_upper", False, -24, 26, -42, 48, 10, 86))
    boxes.append(_box("bow", False, -24, 12, -54, 48, 14, 8))
    boxes.append(_box("windscreen", False, -20, 30, -44, 40, 10, 4))
    boxes.append(_box("windscreen_frame", False, -21, 40, -45, 42, 2, 1))
    boxes.append(_box("rear_doors", False, -22, 12, 44, 44, 20, 4))
    boxes.append(_box("rear_door_seam", False, -1, 12, 48, 2, 20, 1))
    # ---- 6x6 running gear ----
    for i in range(3):
        z = -30.0 + i * 30.0 - 7.0
        boxes.append(_box("wheel_left_%d" % i, False, -30, 0, z, 5, 14, 14))
        boxes.append(_box("wheel_left_hub_%d" % i, False, -31, 5, z + 6, 1, 4, 2))
        boxes.append(_box("wheel_right_%d" % i, False, 25, 0, z, 5, 14, 14))
        boxes.append(_box("wheel_right_hub_%d" % i, False, 30, 5, z + 6, 1, 4, 2))
    boxes.append(_box("bull_bar", False, -22, 0, -60, 44, 12, 4))
    for i in range(4):
        boxes.append(_box("bull_bar_bar_%d" % i, False, -18.0 + i * 12.0, 0, -60.5, 2, 18, 1))
    # ---- stowage ----
    for i, z in enumerate((-30.0, -10.0, 10.0)):
        boxes.append(_box("stowage_left_%d" % i, False, -28, 14, z, 4, 12, 16))
        boxes.append(_box("stowage_left_hinge_%d" % i, False, -29, 16, z + 2, 1, 8, 1))
        boxes.append(_box("stowage_right_%d" % i, False, 24, 14, z, 4, 12, 16))
        boxes.append(_box("stowage_right_hinge_%d" % i, False, 28, 16, z + 2, 1, 8, 1))
    boxes.append(_box("side_window_left", False, -25, 30, -42, 1, 10, 12))
    boxes.append(_box("side_window_right", False, 24, 30, -42, 1, 10, 12))
    boxes.append(_box("mirror_left", False, -30, 36, -44, 4, 4, 2))
    boxes.append(_box("mirror_right", False, 26, 36, -44, 4, 4, 2))
    boxes.append(_box("headlight_left", False, -22, 16, -58, 6, 4, 2))
    boxes.append(_box("headlight_right", False, 16, 16, -58, 6, 4, 2))
    for i, x in enumerate((-16.0, 4.0)):
        boxes.append(_box("roof_hatch_%d" % i, False, x, 36, 8, 12, 2, 12))
        boxes.append(_box("roof_hatch_hinge_%d" % i, False, x + 1, 38, 9, 2, 1, 2))
    boxes.append(_box("engine_grille", False, -20, 36, 30, 40, 1, 12))
    boxes.append(_box("handle_left", False, -10, 18, 48, 4, 4, 2))
    boxes.append(_box("handle_right", False, 6, 18, 48, 4, 4, 2))
    boxes.append(_box("antenna", False, -14, 36, 28, 1, 16, 1))
    # ---- roof station (offset at build time) ----
    boxes.append(_box("turret", True, -14, 0, -14, 28, 10, 30))
    boxes.append(_box("missile_box", True, -20, 8, -4, 10, 18, 19))
    for t in range(4):
        boxes.append(_box("missile_tube_%d" % t, True,
                          -19.0 + (t % 2) * 5.0, 11.0 + (t // 2) * 8.0, -6, 4, 6, 2))
    boxes.append(_box("rws_base", True, 4, 10, -10, 12, 10, 16))
    boxes.append(_box("rws_gun", True, 8, 14, -30, 4, 4, 20))
    boxes.append(_box("rws_ammo", True, 4, 14, 6, 8, 6, 6))
    boxes.append(_box("sight", True, -6, 10, -22, 12, 6, 8))
    boxes.append(_box("sight_lens", True, -2, 12, -23, 4, 3, 1))
    return boxes


VEHICLES = {
    "main_battle_tank": main_battle_tank,
    "infantry_fighting_vehicle": infantry_fighting_vehicle,
}


def world_boxes(name):
    """Resolved boxes in model units, turret children lifted onto the ring."""
    offset = TURRET_OFFSET_Y[name]
    resolved = []
    for box in VEHICLES[name]():
        x, y, z = box["min"]
        w, h, d = box["size"]
        if box["turret"]:
            y += offset
        resolved.append((box["part"], x, y, z, x + w, y + h, z + d))
    return resolved


def bounds(name):
    boxes = world_boxes(name)
    return (
        min(b[1] for b in boxes), min(b[2] for b in boxes), min(b[3] for b in boxes),
        max(b[4] for b in boxes), max(b[5] for b in boxes), max(b[6] for b in boxes),
    )
