#version 150

// Singularity Iteration 0.1.8.5: heat-haze distortion of the dash streak and the EMP shock wave
// (POSITION_COLOR quads; the vertex alpha is the distortion strength).

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
}