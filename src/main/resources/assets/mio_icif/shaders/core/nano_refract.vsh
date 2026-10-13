#version 150

// Singularity Iteration 0.1.8.5: optical camouflage refraction (entity models, NEW_ENTITY format).

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec3 viewNormal;
out vec3 viewPos;

void main() {
    vec4 vp = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * vp;
    viewPos = vp.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    vertexColor = Color;
}