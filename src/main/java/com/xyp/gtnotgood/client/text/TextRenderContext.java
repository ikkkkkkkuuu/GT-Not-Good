// Ported from ABKQPO/GT-Not-Leisure @ 52345d2; LGPL-3.0. See META-INF/text-effects-port/.
package com.xyp.gtnotgood.client.text;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtnotgood.client.text.TextMaskCache.Mask;
import com.xyp.gtnotgood.utils.text.effect.TextEffectStyle;

/** Immutable input for one shader pass; its mask and screen coordinates are owned by the render thread. */
@Desugar
public record TextRenderContext(Mask mask, TextEffectStyle style, float x, float y, int color, double seconds,
    boolean shadow) {}
