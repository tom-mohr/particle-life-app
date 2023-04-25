#version 130

//layout(location = 0) in dvec3 x;
//layout(location = 1) in dvec3 v;
//layout(location = 1) in int type;

uniform float time = 0.0;
uniform vec4 palette[64];
uniform int paletteSize;

in int type;

varying vec4 vColor;

void main(void)
{
   vec3 x = gl_Vertex.xyz;
   //double type = gl_Vertex.w;

   vColor = gl_Color;//palette[0];

   gl_Position = gl_ModelViewProjectionMatrix * vec4(x, 1.0);
}