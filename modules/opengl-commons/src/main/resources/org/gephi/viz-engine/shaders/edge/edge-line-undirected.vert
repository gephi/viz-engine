#version 100

uniform mat4 mvp;
uniform vec4 backgroundColor;
uniform float colorLightenFactor;
uniform float minWeight;
uniform float weightDifferenceDivisor;
uniform float edgeScaleMin;
uniform float edgeScaleMax;
uniform float fGlobalTime;

attribute vec2 vert;
attribute vec2 position;
attribute vec2 targetPosition;
attribute float size;//It's the weight
attribute vec4 sourceColor;
attribute vec4 targetColor;
attribute vec4 elementColor;
attribute float colorBias;
attribute float colorMultiplier;

varying vec4 fragColor;

void main() {
    float thickness = mix(edgeScaleMin, edgeScaleMax, (size - minWeight) / weightDifferenceDivisor);

    vec2 direction = targetPosition - position;
    vec2 directionNormalized = normalize(direction);

    vec2 sideVector = vec2(-directionNormalized.y, directionNormalized.x) * thickness * 0.5;

    vec2 lineEnd = direction;
    vec2 edgeVert = lineEnd * vert.x + sideVector * vert.y;

    gl_Position = mvp * vec4(edgeVert + position, 0.0, 1.0);

    //bgra -> rgba because Java color is argb big-endian
    vec4 color;
    if(elementColor.a <= 0.0) {
        color = (sourceColor.bgra + targetColor.bgra) * 0.5;//Average the colors
    } else {
        color = elementColor.bgra;
    }
    color = color / 255.0;

    color.rgb = colorBias + color.rgb * colorMultiplier;
    color.rgb = mix(color.rgb, backgroundColor.rgb, colorLightenFactor);

    fragColor = color;
}
