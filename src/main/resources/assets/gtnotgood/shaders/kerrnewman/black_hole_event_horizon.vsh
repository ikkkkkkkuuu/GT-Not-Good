// SPDX-License-Identifier: LGPL-3.0-only
#version 120

varying vec4 vertexColor;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    vertexColor = gl_Color;
}
