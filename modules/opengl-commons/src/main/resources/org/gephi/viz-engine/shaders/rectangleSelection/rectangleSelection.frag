#version 100

#ifdef GL_ES
precision lowp float;
#endif

void main() {
    float q = clamp(asin(sin((gl_FragCoord.x + gl_FragCoord.y) * 0.5)), .0, .1);
    gl_FragColor = vec4(0, 0.47, 0.843, 0.2);
}
