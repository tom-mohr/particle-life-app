#version 410

in vec3 in_pos;
uniform mat4 transform;

void main(void) {
    gl_Position = transform  * vec4(in_pos, 1.0);
}