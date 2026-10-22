// SPDX-License-Identifier: LGPL-3.0-only
#version 120

uniform sampler2D DiffuseSampler;
uniform vec2 ScreenSize;
uniform vec2 ScreenOrigin;
uniform vec2 BlackHoleCenterScreen;
uniform float BlackHoleRadiusScreen;
uniform float EventHorizonRadiusScreen;
uniform float DistortionStrength;

varying vec4 vertexColor;

void main() {
    if (!gl_FrontFacing) {
        discard;
    }

    vec2 fragPos = gl_FragCoord.xy - ScreenOrigin;
    vec2 fromCenter = fragPos - BlackHoleCenterScreen;
    float distanceToCenter = length(fromCenter);

    if (distanceToCenter >= EventHorizonRadiusScreen) {
        discard;
    }

    vec2 direction = distanceToCenter > 0.0 ? normalize(fromCenter) : vec2(0.0, 0.0);
    float band = max(EventHorizonRadiusScreen - BlackHoleRadiusScreen, 0.0001);
    float pull = 1.0 - clamp((distanceToCenter - BlackHoleRadiusScreen) / band, 0.0, 1.0);
    float distortion = pull * pull * pull;
    float offsetPixels = distortion * DistortionStrength * EventHorizonRadiusScreen;
    vec2 sampleUv = clamp((fragPos - direction * offsetPixels) / ScreenSize, vec2(0.0), vec2(1.0));

    vec4 sampled = texture2D(DiffuseSampler, sampleUv);
    gl_FragColor = vec4(sampled.rgb * vertexColor.rgb, distortion * vertexColor.a);
}
