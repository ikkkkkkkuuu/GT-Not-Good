// Ported from ABKQPO/GT-Not-Leisure @ 52345d2; LGPL-3.0. See META-INF/text-effects-port/.
package com.xyp.gtnotgood.client.text.compat;

/** Optional font-batch boundary without linking callers to Angelica classes. */
public interface FontBatchBridge {

    int gtng$suspendBatch();

    void gtng$resumeBatch(int depth);

    void gtng$flushBatch();
}
