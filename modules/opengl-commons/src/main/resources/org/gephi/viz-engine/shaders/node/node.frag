#version 100

#ifdef GL_ES
precision lowp float;
#endif

uniform float fGlobalTime;

varying vec4 fragColor;

void main() {
    gl_FragColor = fragColor;
}
