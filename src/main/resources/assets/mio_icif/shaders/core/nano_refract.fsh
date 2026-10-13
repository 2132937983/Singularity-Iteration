#version 150

// Samples the scene behind the model (Sampler0 = copy of the frame) with an offset from the
// surface normal and a slow ripple: the body bends the light instead of hiding it. The vertex
// alpha is the strength; the vertex colour tints the rim.

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform float GameTime;

in vec4 vertexColor;
in vec3 viewNormal;
in vec3 viewPos;

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float t = GameTime * 1200.0;
    vec3 n = normalize(viewNormal);
    float strength = vertexColor.a;
    vec2 ripple = vec2(sin(viewPos.y * 11.0 + t * 3.1), cos(viewPos.x * 11.0 - t * 2.7)) * 0.003;
    vec2 off = (n.xy * 0.014 + ripple) * strength;
    float rim = pow(1.0 - clamp(abs(n.z), 0.0, 1.0), 2.5);
    vec3 base = texture(Sampler0, clamp(uv + off, 0.001, 0.999)).rgb;
    float r = texture(Sampler0, clamp(uv + off * 1.7, 0.001, 0.999)).r;
    float b = texture(Sampler0, clamp(uv + off * 0.5, 0.001, 0.999)).b;
    vec3 col = vec3(mix(base.r, r, rim), base.g, mix(base.b, b, rim));
    col += vertexColor.rgb * rim * 0.28 * strength;
    fragColor = vec4(col, 1.0) * ColorModulator;
}