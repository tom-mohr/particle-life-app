#version 410

#define FACTOR 10

uniform vec4 palette[256];
uniform float time = 0.0;
uniform mat4 transform;

layout(location = 0) in vec3 x;
layout(location = 1) in vec3 v;
layout(location = 2) in int type;

out vec4 vColor;

void main(void) {
    float speed = sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
    vColor = vec4(0.5 + 0.5 * cos(vec3(2.5 * exp(- FACTOR * speed)) + vec3(0, 2, 4)), 1.0);
    gl_Position = vec4(x, 1.0);
}