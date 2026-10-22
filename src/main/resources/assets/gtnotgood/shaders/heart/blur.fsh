// SPDX-License-Identifier: GPL-3.0-only
#version 120

uniform sampler2D Sampler0;
uniform vec2 TexelStep;

varying vec2 texCoord;



void main() {
    vec4 color = texture2D(Sampler0, texCoord) * 0.22702703;
    color += texture2D(Sampler0, texCoord + TexelStep * 1.38461538) * 0.31621622;
    color += texture2D(Sampler0, texCoord - TexelStep * 1.38461538) * 0.31621622;
    color += texture2D(Sampler0, texCoord + TexelStep * 3.23076923) * 0.07027027;
    color += texture2D(Sampler0, texCoord - TexelStep * 3.23076923) * 0.07027027;
    gl_FragColor = color;
}
