#version 410
#pragma optimize(on)

uniform mat4 transform;
uniform vec2 camTopLeft;
uniform bool wrap;
uniform float size;

layout (points) in;

/* Why are we setting max_vertices to that number?
 * Because we are drawing at most 4 particles for each input particle (the original one and its 3 periodic copies).
 * And each particle is drawn as a quad with 4 corners,
 * so we are never outputting more than 4 * 4 = 16 vertices.
 */
layout (triangle_strip, max_vertices = 16) out;

in vec4 vColor[];
out vec4 fColor;
out vec2 texCoord;

#define Pi 3.141592564

void quad(vec4 center) {
    float r = 0.5 * size;

    gl_Position = transform * (center + vec4(-r, -r, 0.0, 0.0));
    texCoord = vec2(-1.0, -1.0);
    EmitVertex();

    gl_Position = transform * (center + vec4(r, -r, 0.0, 0.0));
    texCoord = vec2(1.0, -1.0);
    EmitVertex();

    gl_Position = transform * (center + vec4(-r, r, 0.0, 0.0));
    texCoord = vec2(-1.0, 1.0);
    EmitVertex();

    gl_Position = transform * (center + vec4(r, r, 0.0, 0.0));
    texCoord = vec2(1.0, 1.0);
    EmitVertex();

    EndPrimitive();
}

void main() {
    fColor = vColor[0];
    vec4 center = gl_in[0].gl_Position;

    center -= vec4(camTopLeft, 0.0, 0.0);
    // wrap positions on [0, 1)
    if (wrap) {
        center.x = mod(center.x, 1.0);
        center.y = mod(center.y, 1.0);
    }

    quad(center);

    if (wrap) {
        // Some particles are so close to the edge of the world
        // that they already appear on the other side.
        // We need to draw them there as well.

        float r = 0.5 * size;
        int dx = 0;
        int dy = 0;

        if (center.x < r) {
            dx = 1;
        } else if (center.x > 1 - r) {
            dx = -1;
        }
        if (center.y < r) {
            dy = 1;
        } else if (center.y > 1 - r) {
            dy = -1;
        }
        if (dx != 0) {
            quad(center + vec4(float(dx), 0.0, 0.0, 0.0));
        }
        if (dy != 0) {
            quad(center + vec4(0.0, float(dy), 0.0, 0.0));
        }
        if ((dx != 0) && (dy != 0)) {
            quad(center + vec4(float(dx), float(dy), 0.0, 0.0));
        }
    }
}