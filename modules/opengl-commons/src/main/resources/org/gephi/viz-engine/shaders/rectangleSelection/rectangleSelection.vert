#version 100

uniform mat4 mvp;
uniform vec4 backgroundColor;
uniform float colorLightenFactor;

attribute vec2 vert;



void main() {
    gl_Position = mvp * vec4(vert, 0.0, 1.0);

}
