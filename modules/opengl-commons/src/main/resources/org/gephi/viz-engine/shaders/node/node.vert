//#if with_selection
//#outname "node_with_selection.vert"
//#endif
#version 100

uniform mat4 mvp;
uniform float sizeMultiplier;
uniform float colorMultiplier;
//#if with_selection
uniform vec4 backgroundColor;
uniform float colorBias;
uniform float colorLightenFactor;
//#endif

attribute vec2 vert;
attribute vec2 position;
attribute vec4 elementColor;
attribute float size;

varying vec4 fragColor;

void main() {	
    vec2 instancePosition = size * sizeMultiplier * vert + position;
    gl_Position = mvp * vec4(instancePosition, 0.0, 1.0);

    //bgra -> rgba because Java color is argb big-endian
    vec4 color = elementColor.bgra / 255.0;

    //#if with_selection
    color.rgb = colorBias + color.rgb * colorMultiplier;
    color.rgb = mix(color.rgb, backgroundColor.rgb, colorLightenFactor);
    //#else
    color.rgb = color.rgb * colorMultiplier;
    //#endif

    fragColor = color;
}
