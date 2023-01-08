//#if with_selection
//#outname "edge-line-directed_with_selection.vert"
//#endif
#version 100
#define ARROW_HEIGHT 1.1

uniform mat4 mvp;
//#if with_selection
uniform vec4 backgroundColor;
uniform float colorBias;
uniform float colorMultiplier;
uniform float colorLightenFactor;
//#endif
uniform float minWeight;
uniform float weightDifferenceDivisor;
uniform float edgeScaleMin;
uniform float edgeScaleMax;

attribute vec3 vert;
attribute vec2 position;
attribute vec2 targetPosition;
attribute float size;//It's the weight
attribute vec4 sourceColor;
attribute vec4 elementColor;
attribute float targetSize;

varying vec4 fragColor;

void main() {
    float thickness = mix(edgeScaleMin, edgeScaleMax, (size - minWeight) / weightDifferenceDivisor);

    vec2 direction = targetPosition - position;
    vec2 directionNormalized = normalize(direction);

    vec2 sideVector = vec2(-directionNormalized.y, directionNormalized.x) * thickness * 0.5;
    vec2 arrowHeight = directionNormalized * thickness * ARROW_HEIGHT * 2.0;

    vec2 lineEnd = direction - directionNormalized * targetSize;

    vec2 edgeVert = lineEnd * vert.x + sideVector * vert.y + arrowHeight * vert.z;

    gl_Position = mvp * vec4(edgeVert + position, 0.0, 1.0);

    //bgra -> rgba because Java color is argb big-endian
    vec4 color;
    if(elementColor.a <= 0.0) {
        color = sourceColor.bgra;
    } else {
        color = elementColor.bgra;
    }
    color = color / 255.0;

    //#if with_selection
    color.rgb = colorBias + color.rgb * colorMultiplier;
    color.rgb = mix(color.rgb, backgroundColor.rgb, colorLightenFactor);
    //#endif

    fragColor = color;
}
