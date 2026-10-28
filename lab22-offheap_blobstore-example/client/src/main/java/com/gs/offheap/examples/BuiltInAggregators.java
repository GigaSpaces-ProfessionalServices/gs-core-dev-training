package com.gs.offheap.examples;

import com.gigaspaces.query.aggregators.AggregationSet;
import com.gs.offheap.common.BuiltEntries;
import com.gs.offheap.common.Payments;
import com.gs.offheap.common.Spaces;
import com.gs.offheap.model.Payment;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;

/**
 * Step 2: built-in aggregators read their values straight from the off-heap block.
 * <p>
 * {@code count}, {@code sum}, {@code average}, {@code minValue}, {@code maxValue} and the other
 * built-in aggregators that fold one property declare which property they read. So an aggregation
 * made of them reads just those values from the off-heap block and builds no entry at all. Nothing
 * needs to change in the client.
 * <p>
 * Two lines show where this stops:
 * <ul>
 *   <li>{@code maxEntry} returns an entry, so it needs one, and every entry is built;</li>
 *   <li>a condition of a more complex form - an OR of (currency, amount) pairs here - is not decided
 *       from the block, and every entry it looks at is built.</li>
 * </ul>
 */
public class BuiltInAggregators {

    public static void main(String[] args) {
        GigaSpace gigaSpace = Spaces.connect(Spaces.OFF_HEAP);
        Payments.write(gigaSpace);
        SQLQuery<Payment> all = new SQLQuery<>(Payment.class, "");

        try (BuiltEntries built = BuiltEntries.watch(gigaSpace)) {
            System.out.println();
            built.measure("count()",
                    () -> gigaSpace.aggregate(all, new AggregationSet().count()).get(0));
            built.measure("sum(amount)",
                    () -> gigaSpace.aggregate(all, new AggregationSet().sum("amount")).get(0));
            built.measure("average(amount)",
                    () -> gigaSpace.aggregate(all, new AggregationSet().average("amount")).get(0));
            built.measure("maxValue(amount)",
                    () -> gigaSpace.aggregate(all, new AggregationSet().maxValue("amount")).get(0));
            built.measure("count() where amount > 500",
                    () -> gigaSpace.aggregate(new SQLQuery<>(Payment.class, "amount > 500"),
                            new AggregationSet().count()).get(0));
            built.measure("count() where (currency, amount) pairs",
                    () -> gigaSpace.aggregate(new SQLQuery<>(Payment.class, Payments.largePaymentsCondition()),
                            new AggregationSet().count()).get(0));
            built.measure("maxEntry(amount) - needs the entry",
                    () -> gigaSpace.aggregate(all, new AggregationSet().maxEntry("amount")).get(0));
        }
        System.out.printf("%nExpected: sum %d, average %.1f, %d payments above 500, %d large payments.%n",
                Payments.expectedSum(), Payments.expectedSum() / (double) Payments.COUNT,
                Payments.expectedCountAbove(500), Payments.expectedLargePayments());
        Spaces.exit();
    }
}
