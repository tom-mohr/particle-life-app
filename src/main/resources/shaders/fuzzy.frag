#version 410

in vec4 fColor;
in vec2 texCoord;  // normalized [-1, 1]
out vec4 FragColor;

void main(void) {

    if (length(texCoord) > 1.0) discard;

    FragColor = vec4(fColor.rgb, max(0, 1 - length(texCoord)));
}