"""Player suit flight poses on the shared seven-bone humanoid rig."""
from dataclasses import replace
from math import pi, sin

from an1_locomotion import Pose, bake, staged


READY = Pose(torso=(-2, 0, 0), head=(2, 0, 0),
             arms=((-4, -3, -14), (-2, 4, 15)))


def hover_pose(time):
    phase = 2 * pi * time / 3
    lift = .19 * sin(phase)
    # The game moves the entity through the air. The local motion is a small
    # trim about a steady height, with the gloves following at half amplitude.
    trim = (sin(phase - .4) + sin(.4)) * .5
    return replace(READY, legs=(-3, 5), leg_roll=(-5, 6), feet=(.60 + lift, .63 + lift),
                   compress=.57, torso=(-2 - .45*trim, 0, .55*trim),
                   head=(2 + .2*trim, 0, -.35*trim),
                   arms=((-4 - 1.6*trim, -3, -14 - 1.7*trim),
                         (-2 - 1.3*trim, 4, 15 + 1.5*trim)))


def takeoff():
    load = Pose(legs=(34, -36), leg_roll=(-3, 4), compress=1.15, lean=-7,
                torso=(-8, 2, -2), head=(8, -1, 1), arms=((36, -5, -13), (30, 4, 16)))
    leaving = Pose(legs=(6, -9), leg_roll=(-3, 3), feet=(.22, .25),
                   torso=(3, 0, 0), head=(-1, 0, 0), arms=((-13, -2, -10), (-10, 3, 12)))
    return staged([
        (0, Pose()),
        (.09, load), (.11, load),
        (.14, Pose(legs=(14, -16), leg_roll=(-3, 3), compress=.2, lean=-2,
                   torso=(-1, 0, 0), head=(3, 0, 0), arms=((7, -2, -15), (8, 3, 16)))),
        (.15, leaving),
        (.21, Pose(legs=(-3, -10), leg_roll=(-5, 5), feet=(1.65, 1.55),
                   torso=(5, 0, 0), head=(-3, 0, 0), arms=((-19, -2, -8), (-17, 3, 10)))),
        (.29, Pose(legs=(-6, 7), leg_roll=(-5, 6), feet=(1.30, 1.36), compress=.25,
                   torso=(.5, 0, 0), head=(.5, 0, 0), arms=((-10, -3, -12), (-7, 4, 14)))),
        (.40, hover_pose(0)),
    ])


def land_heavy():
    # Opposing boot pitches and a hip hinge give the loaded silhouette.
    # Lead with the right boot and keep both gloves clear for the next action.
    plant = Pose(legs=(64, -71), leg_roll=(-4, 5), feet=(0, .15), compress=.85,
                 lean=-30, shift=(-.60, -.65), torso=(-11, 8, -6), head=(23, -7, 4),
                 arms=((82, -8, -18), (-12, 5, 35)))
    return staged([
        (0, Pose(legs=(13, -10), leg_roll=(-4, 4), torso=(-4, 0, 0),
                 head=(3, 0, 0), arms=((-6, -3, -20), (-3, 3, 21)))),
        (.05, Pose(legs=(41, -47), leg_roll=(-4, 5), compress=.5, lean=-17,
                   shift=(-.32, -.35), torso=(-8, 6, -3), head=(12, -2, 1),
                   arms=((31, -6, -26), (-10, 5, 29)))),
        (.12, plant), (.22, plant),
        (.34, replace(plant, legs=(54, -58), lean=-18, compress=.45,
                      torso=(-9, 5, -4), head=(18, -4, 3), arms=((67, -7, -15), (-18, 5, 27)))),
        (.47, Pose(legs=(24, -27), leg_roll=(-2, 3), compress=.35, lean=-6,
                   torso=(-4, 2, -1), head=(8, -2, 1), arms=((35, -5, -11), (-10, 2, 17)))),
        (.57, Pose(legs=(5, -6), torso=(2, -1, 0), head=(1, 0, 0),
                   arms=((6, -2, -6), (-4, 1, 9)))),
        (.67, Pose(legs=(1, -1), torso=(.7, 0, 0), head=(-1, 0, 0),
                   arms=((-3, 0, -3), (2, 0, 3)))),
        (.80, Pose()),
    ])


def boost():
    swept = Pose(torso=(-14, 0, 0), head=(12, 0, 0),
                 arms=((-51, -5, -9), (-45, 5, 10)))
    return staged([
        (0, READY),
        (.04, Pose(torso=(-2, 0, 0), head=(2, 0, 0), arms=((9, -3, -17), (8, 4, 18)))),
        (.09, replace(swept, head=(7, 0, 0), arms=((-34, -5, -13), (-30, 5, 14)))),
        (.12, swept), (.16, swept),
        (.23, Pose(torso=(-8, 0, 0), head=(8, 0, 0),
                   arms=((-33, -4, -10), (-29, 4, 12)))),
        (.30, READY),
    ])


def brake():
    brace = Pose(torso=(16, -2, 0), head=(-10, 2, 0),
                 arms=((61, 23, -44), (65, -20, 40)))
    return staged([
        (0, READY),
        (.05, Pose(torso=(5, 0, 0), head=(-2, 0, 0),
                   arms=((-7, -3, -19), (-5, 3, 20)))),
        (.11, replace(brace, head=(-5, 1, 0), arms=((45, 14, -38), (50, -14, 37)))),
        (.15, brace), (.20, brace),
        (.28, Pose(torso=(8, 0, 0), head=(-7, 0, 0),
                   arms=((38, 12, -32), (41, -10, 31)))),
        (.34, Pose(torso=(-3, 0, 0), head=(1, 0, 0),
                   arms=((9, 1, -21), (12, -1, 22)))),
        (.40, READY),
    ])


def build_flight():
    return {"hover": bake(3, hover_pose), "takeoff": takeoff(), "land_heavy": land_heavy(),
            "boost": boost(), "brake": brake()}
