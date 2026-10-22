// SPDX-License-Identifier: GPL-3.0-only
#version 120

varying vec2 texCoord;
varying vec2 localPosition;
varying float transparency;

void main() {
    texCoord = gl_MultiTexCoord0.xy;
    localPosition = gl_Vertex.xy;
    transparency = gl_MultiTexCoord1.x;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
