#version 110

uniform mat4 mvp;




void main() {
    gl_Position =mvp* vec4( gl_Vertex.xy, 0.0, 1.0);

}
