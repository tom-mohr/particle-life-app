#version 410

in vec4 fColor;
in vec2 texCoord;  // normalized [-1, 1]
out vec4 FragColor;

const int MAX_ITERS = 100;

vec3 hue_shift(vec3 color, float dhue) {
    float s = sin(dhue);
    float c = cos(dhue);
    return (color * c) + (color * s) * mat3(
    vec3(0.167444, 0.329213, -0.496657),
    vec3(-0.327948, 0.035669, 0.292279),
    vec3(1.250268, -1.047561, -0.202707)
    ) + dot(vec3(0.299, 0.587, 0.114), color) * (1.0 - c);
}

void main(void) {

    dvec2 c = dvec2(texCoord);
    c.x = c.x * 1.5;
    c.y = c.y * 1.5;

    dvec2 z = c;
    int iters = 0;
    while (iters < MAX_ITERS && z.x * z.x + z.y * z.y < 2) {
        dvec2 z_next = dvec2(z.x * z.x - z.y * z.y, 2 * z.x * z.y) + c;
        z = z_next;
        iters++;
    }

    float hue = float(iters) / MAX_ITERS;
    hue = pow(hue, 0.2);
    float ang = hue * 6.28318530718;
    FragColor = vec4(hue_shift(fColor.rgb, ang), hue);
    //    FragColor = vec4(fColor.rgb, hue);
}
