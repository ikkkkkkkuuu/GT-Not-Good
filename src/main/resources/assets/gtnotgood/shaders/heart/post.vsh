// SPDX-License-Identifier: GPL-3.0-only
#version 120




varying vec2 texCoord;

void main() {
    texCoord = gl_MultiTexCoord0.xy;
    gl_Position = vec4(gl_Vertex.xy, 0.0, 1.0);
}
