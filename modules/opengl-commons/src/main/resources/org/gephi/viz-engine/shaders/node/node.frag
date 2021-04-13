#version 100

#ifdef GL_ES
precision lowp float;
#endif

uniform float fGlobalTime;

varying vec4 fragColor;

mat2 rot(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}
void main() {
    vec4 c = fragColor;
    c.rg *=rot(fGlobalTime);
    gl_FragColor = c;
}
