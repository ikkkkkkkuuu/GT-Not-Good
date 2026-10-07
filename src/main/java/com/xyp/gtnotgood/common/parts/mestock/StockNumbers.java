package com.xyp.gtnotgood.common.parts.mestock;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.gtnewhorizon.gtnhlib.util.parsing.MathExpressionParser;

/** Exact native GTNH quantities: items and fluid liters use integral storage amounts without bucket scaling. */
final class StockNumbers {

    private static final ThreadLocal<MathExpressionParser.Context> expressionContext = ThreadLocal.withInitial(
        () -> new MathExpressionParser.Context().setEmptyValue(0)
            .setErrorValue(Double.NaN));

    private StockNumbers() {}

    static String format(long amount) {
        return Long.toString(amount);
    }

    static long parse(String text) {
        if (text == null || text.length() > 64) throw new NumberFormatException("Invalid stock quantity");
        BigDecimal value;
        try {
            value = new BigDecimal(text.isEmpty() ? "0" : text.trim());
        } catch (NumberFormatException error) {
            var context = expressionContext.get();
            double expression = MathExpressionParser.parse(text, context);
            if (!context.wasSuccessful() || !Double.isFinite(expression)) throw error;
            value = BigDecimal.valueOf(expression);
        }
        if (value.signum() < 0) throw new NumberFormatException("Negative stock");
        return value.setScale(0, RoundingMode.UNNECESSARY)
            .longValueExact();
    }
}
