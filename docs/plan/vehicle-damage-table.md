# Vehicle damage table

Every vehicle weapon in the pack now reads its damage from
`io.github.littlebird.entity.weapon.VehicleDamageTable`. The table is the single place these
numbers live; weapons no longer carry their own constants. Config-backed rows are rebuilt per
lookup, so a config edit takes effect without a restart.

## Policy: which price a target uses

| Target | Armor class | Price used |
|---|---|---|
| Player / mob | UNARMORED | flesh |
| Little Bird, Apache, jet | AIRFRAME | aircraft (Apache has its own price) |
| Wheeled light armour (reserved) | LIGHT_ARMOR | heavy |
| M1A5, Wildcat, Bradley | HEAVY_ARMOR | heavy |

A **bullet** row is zero against `HEAVY_ARMOR` and `LIGHT_ARMOR` no matter what its numbers say.
A **shell** row always prices armour.

## Rows (defaults)

| Weapon | Type | Flesh | Aircraft | Apache | Heavy |
|---|---|---:|---:|---:|---:|
| LITTLE_BIRD_GUN | bullet | 14 | 1 | 0.5 | 0 |
| LITTLE_BIRD_ROCKET | shell | 40 | 10 | 7 | 8 |
| APACHE_CHAIN_GUN | shell | 8 | 3 | 3 | 2 |
| APACHE_MISSILE | shell | 60 | 20 | 20 | 45 |
| JET_CANNON | shell | 8 | 4 | 4 | 0 |
| TANK_MAIN_GUN_AP | shell | 80 | 40 | 40 | 40 |
| TANK_MAIN_GUN_HE | shell | 80 | 25 | 25 | 30 |
| TANK_MAIN_GUN_CANISTER | shell | 80 | 40 | 40 | 0 |
| TANK_CANISTER_PELLET | shell | 40 | 40 | 40 | 0 |
| TANK_COAX | bullet | 8 | 3 | 3 | 0 |
| IFV_AUTOCANNON | shell | 8 | 3 | 3 | 3 |
| IFV_ATGM | shell | 60 | 20 | 20 | 45 |
| BRADLEY_AUTOCANNON | shell | 8 | 3 | 3 | 1 |
| BRADLEY_M2 | bullet | 6 | 1.5 | 1.5 | 0 |
| BRADLEY_TOW | shell | 90 | 40 | 40 | 45 |
| PHL191_ROCKET | shell | 14 | 40 | 40 | 40 |
| PLZ05_SHELL | shell | 22 | 60 | 60 | 60 |
| JDAM | shell | 240 | 240 | 240 | 60 |
| INCENDIARY | shell | 90 | 90 | 90 | 0 |

The defaults are the config defaults (`config/battlefield-common.toml`); artillery rows are the
ballistics constants and are asserted equal in `VehicleDamageTableTest`.

## Rules added after the first table pass

- The two launchers are **heavy armour**: a 155mm shell prices any hull at **60**, a 370mm rocket
  at **40** (aircraft included).
- **Other vehicles' cannons and rockets** (any shell row except the launchers' own) deal **+20%**
  against the PHL-191 / PLZ-05: a tank gun or a TOW hits a howitzer harder than it hits a tank.
- **Machine guns do nothing to the launchers**: they are heavy armour now, and every bullet row is
  zero against armour.

## Reading order: what a vehicle duel looks like

- **Rifle/machine guns** (little bird gun, tank coax, Bradley M2): kill infantry, chip aircraft,
  and do exactly nothing to the three ground hulls. An aircraft duel needs a long burst.
- **Cannons** (Apache chain gun, jet cannon, IFV/Bradley autocannon): small armour prices (1–3),
  so a ground hull still needs many hits, while an aircraft dies in a burst.
- **Rockets / ATGMs / TOW** (7–45 on armour): the real anti-vehicle tools; a Bradley TOW (45) is
  the hardest hitting vehicle weapon against a tracked hull short of the howitzer.
- **Direct-fire tank guns** (AP 40 / HE 30 against armour): two to three hits to kill a hull.
- **Artillery** (PHL 10, PLZ 16) is deliberately the lowest armour price of the big guns: both
  are area weapons, their strength is the blast and the crater, not armour penetration.
- **Bombs** (JDAM 60, incendiary 0) are anti-structure/anti-infantry; the JDAM still hurts hulls.
