package com.gs.offheap.aggregators;

import com.gigaspaces.query.aggregators.SpaceEntriesAggregator;
import com.gigaspaces.query.aggregators.SpaceEntriesAggregatorContext;
import com.gigaspaces.server.ServerEntry;

import java.util.Map;

/**
 * The same aggregator, breaking the promise it makes. It declares its paths but reads the values off
 * {@code context.getServerEntry()} instead of {@code context.getPathValue(path)} - a one-line
 * difference from {@link LargePaymentsAggregator}, and the kind of code that worked everywhere before
 * aggregators could declare their paths.
 * <p>
 * Having declared its paths, the aggregator is never given an entry: none was built, which is where
 * the speed-up comes from. So {@code getServerEntry()} - like {@code getRawEntry()} - throws an
 * {@link IllegalStateException} that names the entry and {@code getAggregatedPaths}, and the
 * aggregation fails instead of returning a wrong number.
 * <p>
 * Either of two one-line changes fixes it: read through {@code context.getPathValue(path)}, or drop
 * {@link #getAggregatedPaths()} - then every entry is built again, and this code works as it always
 * did, without the speed-up.
 */
public class LargePaymentsByServerEntryAggregator extends SpaceEntriesAggregator<Long> {

    private static final long serialVersionUID = 1L;

    private final Map<String, Double> limits;

    private long large;

    public LargePaymentsByServerEntryAggregator(Map<String, Double> limits) {
        this.limits = limits;
    }

    @Override
    public String getDefaultAlias() {
        return "largePaymentsByServerEntry";
    }

    @Override
    public String[] getAggregatedPaths() {
        return new String[]{"currency", "amount"};
    }

    @Override
    public void aggregate(SpaceEntriesAggregatorContext context) {
        ServerEntry entry = context.getServerEntry();
        Object currency = entry.getPropertyValue("currency");
        Object amount = entry.getPropertyValue("amount");
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
