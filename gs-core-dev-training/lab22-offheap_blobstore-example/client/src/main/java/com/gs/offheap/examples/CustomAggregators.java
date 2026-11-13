package com.gs.offheap.examples;

import com.gigaspaces.query.aggregators.AggregationSet;
import com.gs.offheap.aggregators.AmountHistogramAggregator;
import com.gs.offheap.aggregators.LargePaymentsAggregator;
import com.gs.offheap.aggregators.LargePaymentsByServerEntryAggregator;
import com.gs.offheap.common.BuiltEntries;
import com.gs.offheap.common.Payments;
import com.gs.offheap.common.Spaces;
import com.gs.offheap.model.Payment;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;

/**
 * Step 3: a custom aggregator gets the same speed-up by declaring the paths it reads.
 * <p>
 * The rule, in one line: declare your paths with {@code getAggregatedPaths()} and read only them,
 * only through {@code getPathValue}; if you need the entry itself, do not declare paths.
 * <p>
 * Three aggregations:
 * <ol>
 *   <li>{@link LargePaymentsAggregator} - two paths, declared and read correctly: right answer, no
 *       entry built;</li>
 *   <li>{@link AmountHistogramAggregator} - one path, written on top of {@code AbstractPathAggregator}:
 *       the same, with less code;</li>
 *   <li>{@link LargePaymentsByServerEntryAggregator} - declares its paths but reaches for the entry.
 *       The aggregation fails with an {@code IllegalStateException}, printed below in full - that
 *       exception is what to look at.</li>
 * </ol>
 */
public class CustomAggregators {

    public static void main(String[] args) {
        GigaSpace gigaSpace = Spaces.connect(Spaces.OFF_HEAP);
        Payments.write(gigaSpace);
        SQLQuery<Payment> all = new SQLQuery<>(Payment.class, "");

        try (BuiltEntries built = BuiltEntries.watch(gigaSpace)) {
            System.out.println();
            built.measure("largePayments (custom, two paths)",
                    () -> gigaSpace.aggregate(all, new AggregationSet()
                            .add(new LargePaymentsAggregator(Payments.LARGE_PAYMENT_LIMITS))).get(0));
            System.out.printf("  expected: %d%n", Payments.expectedLargePayments());

            AmountHistogramAggregator histogram = new AmountHistogramAggregator();
            histogram.setPath("amount");
            int[] buckets = built.measure("amountHistogram (custom, one path)",
                    () -> (int[]) gigaSpace.aggregate(all, new AggregationSet().add(histogram)).get(0));
            System.out.printf("  buckets of %d: %s, expected: [250, 250, 250, 250]%n",
                    AmountHistogramAggregator.BUCKET, AmountHistogramAggregator.render(buckets));

            System.out.println();
            System.out.println("largePaymentsByServerEntry (custom, breaks the promise):");
            try {
                Object result = gigaSpace.aggregate(all, new AggregationSet()
                        .add(new LargePaymentsByServerEntryAggregator(Payments.LARGE_PAYMENT_LIMITS))).get(0);
                System.out.println("  UNEXPECTED: the aggregation returned " + result + " instead of failing.");
            } catch (RuntimeException e) {
                System.out.println("  failed, as it should: " + rootCause(e));
            }
        }
        Spaces.exit();
    }

    private static Throwable rootCause(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause)
            cause = cause.getCause();
        return cause;
    }
}
