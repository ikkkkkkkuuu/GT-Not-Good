// SPDX-License-Identifier: GPL-3.0-only
#version 120

uniform sampler2D Sampler0;
uniform float Intensity;
uniform vec3 CameraPosition;
uniform vec3 BeamColor;

varying vec2 texCoord;
varying vec2 localPosition;
varying float transparency;



float luminanceTransform(vec3 color) {
    return 0.2126 * color.r + 0.7152 * color.g + 0.0722 * color.b;
}

void main() {
    float viewEffect = dot(normalize(CameraPosition.xy), normalize(localPosition));
    float angleAlpha = max((viewEffect - 0.5) * 2.0, 0.0);
    angleAlpha = pow(angleAlpha, Intensity);

    vec4 texColor = texture2D(Sampler0, texCoord);
    float luminance = 1.0 - luminanceTransform(texColor.xyz);
    luminance = mix(luminance, 1.0, 1.0 - pow(angleAlpha, 6.0));
    float alpha = clamp(angleAlpha * transparency * luminance, 0.0, 1.0);
    gl_FragColor = vec4(BeamColor, alpha);
}
