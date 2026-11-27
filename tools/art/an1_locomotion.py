"""AN1 wingman locomotion, in the rig's right-handed, Y-up authoring space."""
from dataclasses import dataclass, fields
from math import cos, floor, pi, radians, sin


ZERO = (0, 0, 0)


@dataclass(frozen=True)
class Pose:
    # Pitches of the straight right and left legs in world space. Positive
    # swings a foot forward (-Z). Arm/torso/head angles are relative to parent.
    legs: tuple = (0, 0)
    leg_yaw: tuple = (0, 0)
    leg_roll: tuple = (0, 0)
    arms: tuple = (ZERO, ZERO)
    torso: tuple = ZERO
    head: tuple = ZERO
    lean: float = 0
    # Horizontal pelvis displacement, desired sole heights, suit compression.
    shift: tuple = (0, 0)
    feet: tuple = (0, 0)
    compress: float = 0
    palm: float = 0


def rotate(v, angles):
    """Intrinsic bone transform Rz Ry Rx, as seen in the reference rig."""
    x, y, z = v
    rx, ry, rz = map(radians, angles)
    y, z = cos(rx) * y - sin(rx) * z, sin(rx) * y + cos(rx) * z
    x, z = cos(ry) * x + sin(ry) * z, -sin(ry) * x + cos(ry) * z
    return (cos(rz) * x - sin(rz) * y, sin(rz) * x + cos(rz) * y, z)


def plus(a, b):
    return tuple(x + y for x, y in zip(a, b))


def lowest_sole(right, rotation, lean):
    # The leg pivots are at +/-1.9 but the reference boots centre at +/-2.
    eccentric = -0.1 if right else 0.1
    return min(rotate(plus((0, 12, 0), rotate((x + eccentric, -12, z), rotation)), lean)[1]
               for x in (-2, 2) for z in (-2, 2))


def bone_values(p):
    lean = (p.lean, 0, 0)
    leg_rot = [(p.legs[i] - p.lean, p.leg_yaw[i], p.leg_roll[i]) for i in (0, 1)]
    sole = [lowest_sole(i == 0, leg_rot[i], lean) for i in (0, 1)]
    required = [target - low for target, low in zip(p.feet, sole)]
    height = min(required) - p.compress
    # Root pitch hinges about the hips, despite the root bone's floor pivot.
    hip = rotate((0, 12, 0), lean)
    root_pos = (p.shift[0], height, p.shift[1] - hip[2])
    # Body rotation is authored about the pelvis, but the vanilla body bone
    # pivots at the neck. Translate the torso so the waist still meets the legs.
    waist = rotate((0, -12, 0), p.torso)
    body_pos = (-waist[0], -12 - waist[1], -waist[2])
    pose = {"root": (lean, root_pos), "body": (p.torso, body_pos),
            "head": (p.head, ZERO), "right_arm": (p.arms[0], ZERO),
            "left_arm": (p.arms[1], ZERO)}
    for i, name in enumerate(("right_leg", "left_leg")):
        rise = required[i] - height
        local = rotate((0, rise, 0), (-p.lean, 0, 0))
        pose[name] = (leg_rot[i], local)

    if p.palm:
        # The right palm lies ten units below its shoulder pivot at rest.
        # Touch the lowest fingertip corner to the floor through the held beat.
        def shoulder_frame(v):
            return rotate(rotate(v, p.torso), lean)

        wrist_y = []
        for x in (-3, 1):
            for z in (-2, 2):
                hand = rotate((x, -10, z), p.arms[0])
                below_neck = plus((-5, -2, 0), hand)
                body_point = plus(plus((0, 24, 0), body_pos), rotate(below_neck, p.torso))
                wrist_y.append(root_pos[1] + rotate(body_point, lean)[1])
        bottom = min(wrist_y)
        shift = -bottom if bottom < 0 else -bottom * p.palm
        vertical_local = tuple(shift * shoulder_frame(axis)[1] for axis in ((1,0,0), (0,1,0), (0,0,1)))
        pose["right_arm"] = (p.arms[0], vertical_local)
    return pose


def blend(a, b, t):
    if isinstance(a, tuple):
        return tuple(blend(x, y, t) for x, y in zip(a, b))
    return a + (b - a) * t


def tick(t):
    """Blockbench's positive-time Math.round on a 100 Hz project grid."""
    return floor(t * 100 + .5 + 1e-9)


def staged(keys):
    keys = [(tick(t) / 100, pose) for t, pose in keys]
    def at(t):
        for (t0, a), (t1, b) in zip(keys, keys[1:]):
            if t <= t1 + 1e-8:
                u = min(1, max(0, (t - t0) / (t1 - t0)))
                u = u * u * (3 - 2 * u)
                return Pose(**{f.name: blend(getattr(a, f.name), getattr(b, f.name), u)
                               for f in fields(Pose)})
        return keys[-1][1]
    return bake(keys[-1][0], at, [t for t, _ in keys])


def simplify(samples, tolerance):
    # Retain the hand-shaped acceleration while omitting collinear samples.
    if len(samples) <= 2:
        return samples
    start, first = samples[0]
    end, last = samples[-1]
    worst, index = 0, 0
    for i, (t, vec) in enumerate(samples[1:-1], 1):
        predicted = blend(first, last, (t - start) / (end - start))
        error = max(abs(a - b) for a, b in zip(vec, predicted))
        if error > worst:
            worst, index = error, i
    if worst <= tolerance:
        return [samples[0], samples[-1]]
    return simplify(samples[:index+1], tolerance)[:-1] + simplify(samples[index:], tolerance)


def bake(length, pose_at, accents=()):
    frames = set(range(tick(length) + 1)) | {tick(t) for t in accents}
    times = [frame / 100 for frame in sorted(frames)]
    channels = {}
    for t in times:
        for bone, transforms in bone_values(pose_at(t)).items():
            for prop, vec in zip(("rotation", "position"), transforms):
                channels.setdefault((bone, prop), []).append((t, vec))
    result = {}
    for (bone, prop), samples in channels.items():
        if all(abs(c) < 0.0001 for _, vec in samples for c in vec):
            continue
        reduced = simplify(samples, 0.055 if prop == "rotation" else 0.008)
        result.setdefault(bone, {})[prop] = {t: [round(c, 4) for c in vec] for t, vec in reduced}
    return result


def gait(run):
    length, angle, stance, swing = (.6, 43, .32, 2.80) if run else (1., 26, .56, 2.05)

    def pose(t):
        p = t / length
        phases = (p % 1, (p + .5) % 1)
        def pitch(u):
            return angle * cos(2*pi*u) + (4 if run else 2) * sin(4*pi*u)
        def lift(u):
            return 0 if u <= stance else swing * sin(pi * (u-stance)/(1-stance)) ** 1.6
        legs = tuple(pitch(u) for u in phases)
        phase = p % .5
        flight = .90 * sin(pi * (phase - stance) / (.5 - stance)) ** 2 if run and phase > stance else 0
        feet = tuple(lift(u) + flight for u in phases)
        load = max(0, cos(4*pi*(p-.069))) ** 3
        twist = (8 if run else 4.7) * cos(2*pi*(p-.025))
        sway = sin(2*pi*(p-.035))
        arms = tuple((4 - (.94 if run else .76) * pitch(u-.033),
                      sign * (4 if run else 2), sign * (8 if run else 5))
                     for u, sign in zip(phases, (-1, 1)))
        return Pose(legs=legs, arms=arms, feet=feet, leg_roll=(-1,1),
                    torso=(-(11.5 if run else 3.8) - 1.5*load, twist, 1.4*sway),
                    head=(7 if run else 2, -.71*twist, -.65*sway),
                    shift=(-(0.72 if run else .52)*sway, -.18*load),
                    compress=(.70 if run else .50)*load)
    return bake(length, pose, (0, length/2, length))


def skate(t):
    u = sin(2*pi*t/.8)
    return Pose(legs=(34 + .7*u, -34 + .6*u), leg_roll=(-8, 6),
                torso=(-16 + .6*u, -4, 2), head=(11, 3, -1),
                arms=((-32 + 1.4*u,-8,-24), (-26 - 1.3*u,6,21)),
                shift=(-.45 + .12*u,-.5), compress=1.45)


def cover(t):
    u = sin(2*pi*t/1.5)
    return Pose(legs=(-53 + .55*u, 56 - .4*u), leg_roll=(-4,4),
                torso=(-12 - .65*u, -10, 2), head=(8 + .25*u, 8, -1),
                arms=((72 + .6*u,-10,-3), (82 - .5*u,19,6)), compress=2.4 - .1*u,
                shift=(.35,-.55))


def ascending(t):
    u = sin(2*pi*t/.5)
    return Pose(legs=(26 + 1.3*u, -20 + 1.3*u), feet=(2.6,2.2),
                leg_roll=(-3,3), arms=((-16 + 1.5*u,-4,-11),(-20 - 1.1*u,4,12)),
                torso=(1.5 + .7*u,0,0), head=(2,0,0), shift=(0,-.45), compress=.2)


def falling(t):
    u = sin(2*pi*t/.5)
    return Pose(legs=(-12 + .65*u, 15 - .8*u), feet=(1.6,1.15),
                leg_roll=(-4,5), arms=((7 + 1.2*u,-6,-24),(1 - .9*u,6,22)),
                torso=(-7 + .6*u,1,0), head=(3,0,0), shift=(0,-.25))


def jumping(thrust):
    if thrust:
        return staged([
            (0, Pose()),
            (.065, Pose(legs=(-27,32), compress=1.2, shift=(-.2,.3),
                        torso=(-8,1,0), arms=((14,0,-8),(19,0,8)), head=(4,0,0))),
            (.108, Pose(legs=(-42,45), compress=1.9, shift=(-.3,.6), torso=(-14,2,0),
                        arms=((33,-5,-12),(38,5,13)), head=(8,-1,0))),
            (.15, Pose(legs=(-11,12), compress=.1, torso=(-5,0,0),
                       arms=((8,-4,-10),(12,4,10)), head=(8,0,0))),
            (.205, Pose(legs=(2,-7), feet=(1.5,1.4), torso=(6,0,0),
                        arms=((-26,-5,-12),(-30,5,14)), head=(-2,0,0))),
            (.285, Pose(legs=(20,-17), feet=(2.7,2.35), torso=(3.5,0,0),
                        arms=((-22,-5,-13),(-26,5,13)), shift=(0,-.42))),
            (.4, ascending(0)),
        ])
    return staged([
        (0, Pose()),
        (.052, Pose(legs=(-23,25), compress=.85, torso=(-7,0,0),
                    arms=((-14,0,-5),(-10,0,5)), head=(3,0,0))),
        (.078, Pose(legs=(-27,29), compress=1.05, torso=(-8,0,0),
                    arms=((-18,0,-6),(-14,0,6)), head=(4,0,0))),
        (.1, Pose(legs=(-7,9), torso=(-4,0,0), arms=((1,0,-5),(4,0,6)))),
        (.155, Pose(legs=(10,-7), feet=(1.05,.9), arms=((15,0,-8),(18,0,8)), torso=(1,0,0))),
        (.25, ascending(0)),
    ])


def landing(heavy):
    arrival = Pose(legs=(-13,16), leg_roll=(-4,5), arms=((8,-6,-22),(1,6,21)),
                   torso=(-6,1,0), head=(4,0,0))
    if not heavy:
        return staged([
            (0, arrival),
            (.047, Pose(legs=(-25,29), compress=.8, torso=(-9,1,1),
                        arms=((18,-7,-20),(11,7,19)), head=(5,0,0))),
            (.09, Pose(legs=(-28,32), compress=1.1, torso=(-12,1,1),
                       arms=((24,-8,-19),(16,7,17)), head=(6,0,0))),
            (.16, Pose(legs=(-18,22), compress=.5, torso=(-6,1,0),
                       arms=((14,-6,-13),(11,5,12)), head=(5,0,0))),
            (.24, Pose(legs=(-3,4), torso=(1,0,0), arms=((-3,0,-4),(-4,0,4)))),
            (.30, Pose()),
        ])

    # Front (left) boot and the opposite palm form a diagonal brace. Lean the
    # entire torso forward from the hips; the trailing right leg skims the mat.
    plant = Pose(legs=(-78,66), leg_yaw=(-4,6), leg_roll=(-3,5),
                 feet=(.4,0), lean=-46, torso=(-12,2,12), head=(31,-6,-7),
                 arms=((59,-9,-8),(13,-14,32)), shift=(-.55,-1.45), palm=1)
    return staged([
        (0, arrival),
        (.045, Pose(legs=(-34,38), lean=-13, torso=(-8,1,4),
                    arms=((24,-6,-13),(12,5,23)), head=(9,0,-1), compress=.35)),
        (.091, Pose(legs=(-65,54), lean=-34, torso=(-12,2,8), head=(22,-3,-5),
                    arms=((49,-9,-9),(16,-8,28)), shift=(-.4,-1.1))),
        (.135, plant), (.375, plant),
        (.46, Pose(legs=(-65,56), lean=-34, torso=(-12,2,8), head=(27,-4,-3),
                   arms=((44,-8,-10),(18,-6,23)), shift=(-.3,-1.0))),
        (.565, Pose(legs=(-44,37), lean=-16, torso=(-9,1,3), head=(16,-1,-1),
                    arms=((28,-6,-13),(13,-4,17)), shift=(0,-.65))),
        (.69, Pose(legs=(-9,11), torso=(2,0,-1), head=(3,0,0),
                   arms=((-6,-2,-6),(-8,2,6)), shift=(.2,.12))),
        (.8, Pose()),
    ])


def step_up():
    return staged([
        (0, Pose(legs=(10,-8), torso=(-3,0,0), arms=((-9,0,-4),(8,0,4)))),
        (.08, Pose(legs=(4,14), compress=.65, shift=(-.55,.2),
                   torso=(-7,1,-1), arms=((-3,0,-5),(3,0,5)))),
        (.13, Pose(legs=(-8,43), feet=(0,3.1), shift=(-.6,-.15),
                   torso=(-10,3,-2), arms=((13,0,-6),(-10,0,6)), head=(5,0,0))),
        (.183, Pose(legs=(-16,66), feet=(0,6.4), shift=(-.4,-.65),
                    torso=(-11,4,-1), arms=((20,-2,-8),(-18,2,7)), head=(7,-2,0))),
        (.23, Pose(legs=(-23,50), feet=(0,4.1), shift=(.15,-1.25),
                   torso=(-7,3,1), arms=((16,0,-6),(-13,0,6)), head=(5,-1,0))),
        (.30, Pose(legs=(-14,19), feet=(0,0), shift=(.25,-1.55),
                   torso=(-3,1,0), arms=((9,0,-4),(-7,0,4)))),
    ])


def turn(left):
    direction = -1 if left else 1
    def footstep(p, yaw, leading, other, shift):
        r, l = ((1,0) if left else (0,1))
        legs = (other, leading) if left else (leading, other)
        heights = (0, p) if left else (p, 0)
        if p < 0:
            heights = (-p, 0) if left else (0, -p)
        return Pose(legs=legs, leg_yaw=(direction*yaw[r], direction*yaw[l]),
                    feet=heights, shift=(direction*shift,0))
    return staged([
        (0, Pose()), (.075, footstep(1.15,(16,0),12,-4,.40)),
        (.153, footstep(0,(13,3),8,-6,.05)), (.24, footstep(-1.05,(6,17),-4,10,-.42)),
        (.323, footstep(0,(2,9),-3,5,-.1)), (.4, Pose()),
    ])


def tracks():
    return {"walk": gait(False), "run": gait(True), "skate": bake(.8, skate),
            "jump_takeoff": jumping(False), "thrust_hop": jumping(True),
            "air_rise": bake(.5, ascending), "air_fall": bake(.5, falling),
            "land_light": landing(False), "land_heavy": landing(True),
            "step_up": step_up(), "turn_l": turn(True), "turn_r": turn(False),
            "cover_crouch": bake(1.5, cover)}
