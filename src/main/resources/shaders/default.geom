#version 410
#pragma optimize(on)

uniform mat4 transform;
uniform vec2 camTopLeft;
uniform bool wrap;
uniform float size;
uniform int detail;

layout (points) in;
layout (triangle_strip,
        max_vertices = 24  // = 2 * (MAX_DETAIL + 1)
       ) out;

in vec4 vColor[];
out vec4 fColor;

#define Pi 3.141592564

void circle(vec4 center) {
    center -= vec4(camTopLeft, 0.0, 0.0);
    // wrap positions to [0, 1)
    if (wrap) {
        center.x = mod(center.x, 1.0);
        center.y = mod(center.y, 1.0);
    }

    float r = 0.5 * size;
    for (int i = 0; i <= detail; i++) {
        gl_Position = transform * center;
        EmitVertex();

        float angle = 0.25 * Pi + 2.0 * Pi * float(i) / float(detail);
        gl_Position = transform * (center + vec4(r * cos(angle), r * sin(angle), 0.0, 0.0));
        EmitVertex();
    }

    EndPrimitive();
}

void main() {
    fColor = vColor[0];
    circle(gl_in[0].gl_Position);
}