#version 410

#define FACTOR 10

uniform vec4 palette[64];
uniform float time = 0.0;
uniform mat4 transform;

layout(location = 0) in vec3 x;
layout(location = 1) in vec3 v;
layout(location = 2) in int type;

out vec4 vColor;

void main(void) {
    vColor = vec4(1.0, 1.0, 1.0, 1.0);
    gl_Position = vec4(x, 1.0);
}