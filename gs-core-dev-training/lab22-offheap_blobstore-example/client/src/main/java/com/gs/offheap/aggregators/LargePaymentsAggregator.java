package com.gs.offheap.aggregators;

import com.gigaspaces.query.aggregators.SpaceEntriesAggregator;
import com.gigaspaces.query.aggregators.SpaceEntriesAggregatorContext;

import java.util.Map;

/**
 * A custom aggregator, written the way that gets the off-heap speed-up. It counts the payments above
 * a limit, and the limit differs per currency - something a built-in aggregator, which folds one
 * path with one fixed operation, cannot do.
 * <p>
 * How an aggregator runs: the object is sent to every partition, {@link #aggregate} is called there
 * once for every matching entry, {@link #getIntermediateResult} hands back the partition's answer,
 * and {@link #aggregateIntermediateResult} combines the partitions' answers on the client. Only the
 * partial results travel; the entries stay where they are.
 * <p>
 * Two things make it fast on an off-heap space:
 * <ul>
 *   <li>{@link #getAggregatedPaths()} declares which properties it reads - a promise that nothing
 *       else will be needed from the entry;</li>
 *   <li>it reads them through {@link SpaceEntriesAggregatorContext#getPathValue(String)}, the only
 *       supported way once the paths are declared.</li>
 * </ul>
 * Keep the promise and the values are read straight from the off-heap block: no entry is built.
 * Compare {@link LargePaymentsByServerEntryAggregator}, which differs in one line.
 */
public class LargePaymentsAggregator extends SpaceEntriesAggregator<Long> {

    private static final long serialVersionUID = 1L;

    /** The limits travel to the partitions together with the aggregator. */
    private final Map<String, Double> limits;

    private long large;

    public LargePaymentsAggregator(Map<String, Double> limits) {
        this.limits = limits;
    }

    @Override
    public String getDefaultAlias() {
        return "largePayments";
    }

    /** Declares exactly what {@link #aggregate} reads. Returning null would mean "I need the whole entry". */
    @Override
    public String[] getAggregatedPaths() {
        return new String[]{"currency", "amount"};
    }

    @Override
    public void aggregate(SpaceEntriesAggregatorContext context) {
        Object currency = context.getPathValue("currency");
        Object amount = context.getPathValue("amount");
        Double limit = limits.get(String.valueOf(currency));
        if (limit != null && amount instanceof Number && ((Number) amount).doubleValue() > limit)
            large++;
    }

    @Override
    public Long getIntermediateResult() {
        return large;
    }

    @Override
    public void aggregateIntermediateResult(Long partitionResult) {
        if (partitionResult != null)
            large += partitionResult;
    }
}
