#version 100

uniform mat4 mvp;
uniform vec4 backgroundColor;
uniform float colorLightenFactor;

attribute vec2 vert;
attribute vec2 position;
attribute vec4 elementColor;
attribute float colorBias;
attribute float colorMultiplier;
attribute float size;

varying vec4 fragColor;

void main() {	
    vec2 instancePosition = size * vert + position;
    gl_Position = mvp * vec4(instancePosition, 0.0, 1.0);

    //bgra -> rgba because Java color is argb big-endian
    vec4 color = elementColor.bgra / 255.0;
    color.rgb = colorBias + color.rgb * colorMultiplier;
    color.rgb = mix(color.rgb, backgroundColor.rgb, colorLightenFactor);

    fragColor = color;
}
