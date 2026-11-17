#version 150

// 0.1.11.73 (phase warp W3): the screen of the own warp. Strength (0..1) blurs the picture outwards from the centre,
// splits its colours at the edges (red out, blue in), darkens the edge violet and lights the centre a little; Gather
// (0..1, before the barrier) splits the colours a little and closes the dark edge in.

uniform sampler2D DiffuseSampler;
uniform float Strength;
uniform float Gather;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main() {
    vec2 d = texCoord - vec2(0.5);
    float r = length(d);
    float edge = smoothstep(0.12, 0.72, r);
    // radial blur: samples back towards the centre, longer at the edge
    float blur = Strength * 0.1 * edge;
    vec3 acc = vec3(0.0);
    for (int i = 0; i < 8; i++) {
        float k = float(i) / 7.0;
        acc += texture(DiffuseSampler, texCoord - d * blur * k).rgb;
    }
    acc /= 8.0;
    // colour split at the edge
    float ca = (Strength * 0.014 + Gather * 0.005) * edge;
    float red = texture(DiffuseSampler, texCoord + d * ca).r;
    float blue = texture(DiffuseSampler, texCoord - d * ca).b;
    vec3 col = vec3(mix(acc.r, red, 0.65), acc.g, mix(acc.b, blue, 0.65));
    // dark violet edge and a lit centre
    float v = smoothstep(0.28, 0.85, r) * clamp(0.85 * Strength + 0.5 * Gather, 0.0, 1.0);
    col = mix(col, vec3(0.03, 0.015, 0.09), v);
    col += vec3(0.55, 0.85, 1.0) * Strength * 0.12 * (1.0 - smoothstep(0.0, 0.32, r));
    fragColor = vec4(col, 1.0);
}
