#version 150

// 0.1.11.236 (SlashBlade collab B4): the singularity blade's event horizon. CenterX/Y is the singularity on the screen (0..1),
// Radius its horizon in screen heights. A point lensed at distance r shows the picture from r - E^2/r (the thin-lens
// image of a point mass, E the Einstein radius, 1.7 horizons): the scene wraps round the hole into a ring. Inside the
// horizon the picture is black; a thin magenta photon ring sits just outside it. Burst (0..1) is the Hawking flash:
// magenta light out from the centre, whitening the core.

uniform sampler2D DiffuseSampler;
uniform float CenterX;
uniform float CenterY;
uniform float Radius;
uniform float Strength;
uniform float Burst;
uniform vec2 OutSize;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 Center = vec2(CenterX, CenterY);
    float aspect = OutSize.x / OutSize.y;
    vec2 d = texCoord - Center;
    d.x *= aspect;
    float r = length(d);
    float rs = Radius * Strength;
    vec3 col;
    if (rs < 1e-5) {
        col = texture(DiffuseSampler, texCoord).rgb;
    } else {
        float e = rs * 1.7;
        vec2 dir = r > 1e-6 ? d / r : vec2(0.0);
        float src = r - e * e / max(r, 1e-4);
        // the bending fades out far from the hole, so the rest of the screen stays put
        src = mix(src, r, smoothstep(2.5 * e, 6.0 * e, r));
        vec2 s = dir * src;
        s.x /= aspect;
        col = texture(DiffuseSampler, clamp(Center + s, vec2(0.001), vec2(0.999))).rgb;
        // the horizon and the photon ring
        float ring = exp(-pow((r - rs * 1.12) / (rs * 0.09), 2.0));
        col = mix(col, vec3(0.0), 1.0 - smoothstep(rs * 0.96, rs * 1.04, r));
        col += vec3(1.0, 0.17, 0.84) * ring * 0.9 * Strength;
    }
    // the Hawking burst
    float rb = max(Radius, 0.02);
    float glow = Burst * exp(-r / (rb * 3.0));
    col = mix(col, vec3(1.0, 0.9, 0.98), Burst * exp(-r / (rb * 0.8)));
    col += vec3(1.0, 0.17, 0.84) * glow;
    fragColor = vec4(col, 1.0);
}
