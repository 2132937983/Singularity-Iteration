#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform float GameTime;

in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float t = GameTime * 1200.0;
    float s = vertexColor.a;
    vec2 wave = vec2(sin(gl_FragCoord.y * 0.09 + t * 11.0) + 0.5 * sin(gl_FragCoord.x * 0.21 - t * 7.0),
                     cos(gl_FragCoord.x * 0.08 - t * 9.0) + 0.5 * cos(gl_FragCoord.y * 0.17 + t * 6.0));
    vec2 off = wave * 0.0045 * s;
    vec3 col = texture(Sampler0, clamp(uv + off, 0.001, 0.999)).rgb;
    col += vertexColor.rgb * 0.12 * s;
    fragColor = vec4(col, 1.0) * ColorModulator;
}