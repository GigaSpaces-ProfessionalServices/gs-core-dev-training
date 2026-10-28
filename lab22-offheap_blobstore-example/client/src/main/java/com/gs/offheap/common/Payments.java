package com.gs.offheap.common;

import com.gs.offheap.model.Payment;
import org.openspaces.core.GigaSpace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The data every example works on - payments with amounts 1..COUNT, currencies in turn - and the
 * expected answers, computed here on the client from the same rules. Knowing the answer in advance
 * matters: a wrong aggregation returns a plausible number, and without the expected one next to it
 * nothing on the screen looks wrong.
 */
public final class Payments {

    public static final int COUNT = 1000;

    private static final String[] CURRENCIES = {"USD", "EUR", "ILS"};

    /** The amount above which a payment counts as large - different for every currency. */
    public static final Map<String, Double> LARGE_PAYMENT_LIMITS = largePaymentLimits();

    private static Map<String, Double> largePaymentLimits() {
        // A fixed order keeps the output the same from run to run.
        Map<String, Double> limits = new LinkedHashMap<>();
        limits.put("USD", 500.0);
        limits.put("EUR", 450.0);
        limits.put("ILS", 1750.0);
        return limits;
    }

    private Payments() {
    }

    /** Replaces whatever payments the space holds with a fresh set, and says so. */
    public static void write(GigaSpace gigaSpace) {
        writeQuietly(gigaSpace);
        System.out.printf("Wrote %d payments to %s: amounts 1..%d, currencies %s%n",
                COUNT, gigaSpace.getSpaceName(), COUNT, String.join("/", CURRENCIES));
    }

    /** The same as {@link #write}, without printing anything. */
    public static void writeQuietly(GigaSpace gigaSpace) {
        gigaSpace.clear(new Payment());
        List<Payment> payments = new ArrayList<>(COUNT);
        for (int i = 1; i <= COUNT; i++)
            payments.add(new Payment(i, (double) i, currencyOf(i)));
        gigaSpace.writeMultiple(payments.toArray());
    }

    public static String currencyOf(int id) {
        return CURRENCIES[id % CURRENCIES.length];
    }

    public static long expectedSum() {
        return (long) COUNT * (COUNT + 1) / 2;
    }

    public static long expectedCountAbove(double amount) {
        return Math.max(0, COUNT - (long) Math.floor(amount));
    }

    public static long expectedLargePayments() {
        long expected = 0;
        for (int i = 1; i <= COUNT; i++)
            if (i > LARGE_PAYMENT_LIMITS.get(currencyOf(i)))
                expected++;
        return expected;
    }

    /** The same selection as {@link #expectedLargePayments()}, written as a query condition. */
    public static String largePaymentsCondition() {
        StringBuilder sql = new StringBuilder();
        for (Map.Entry<String, Double> limit : LARGE_PAYMENT_LIMITS.entrySet()) {
            if (sql.length() > 0)
                sql.append(" OR ");
            sql.append("(currency = '").append(limit.getKey())
               .append("' AND amount > ").append(limit.getValue()).append(")");
        }
        return sql.toString();
    }
}
