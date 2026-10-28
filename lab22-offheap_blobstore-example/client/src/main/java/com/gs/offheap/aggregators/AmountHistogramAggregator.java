package com.gs.offheap.aggregators;

import com.gigaspaces.query.aggregators.AbstractPathAggregator;
import com.gigaspaces.query.aggregators.SpaceEntriesAggregatorContext;

import java.util.Arrays;

/**
 * A custom aggregator over a single path, and so written the short way, on top of
 * {@link AbstractPathAggregator} - the way every built-in aggregator is written.
 * <p>
 * It counts how many payments fall into each amount bucket. The result is an array, so no built-in
 * aggregator does it, but it reads one property only, and {@link AbstractPathAggregator} takes care
 * of it: the path is set once with {@code setPath("amount")}, the declaration comes from it through
 * {@code declaredPath()}, and {@code getPathValue(context)} reads it.
 * <p>
 * The declaration still has to be made by this class: a subclass of {@code AbstractPathAggregator}
 * that does not override {@link #getAggregatedPaths()} declares nothing and is handed the entry, as
 * before.
 */
public class AmountHistogramAggregator extends AbstractPathAggregator<int[]> {

    private static final long serialVersionUID = 1L;

    public static final int BUCKET = 250;
    public static final int BUCKETS = 4;

    private transient int[] histogram;

    @Override
    public String getDefaultAlias() {
        return "amountHistogram";
    }

    @Override
    public String getName() {
        return "HISTOGRAM";
    }

    @Override
    public String[] getAggregatedPaths() {
        return declaredPath();
    }

    @Override
    public void aggregate(SpaceEntriesAggregatorContext context) {
        Object value = getPathValue(context);
        if (!(value instanceof Number))
            return;
        if (histogram == null)
            histogram = new int[BUCKETS];
        histogram[bucketOf(((Number) value).doubleValue())]++;
    }

    /** Buckets of {@link #BUCKET}; the last one also takes everything above. */
    public static int bucketOf(double amount) {
        int bucket = (int) ((amount - 1) / BUCKET);
        return Math.min(Math.max(bucket, 0), BUCKETS - 1);
    }

    @Override
    public int[] getIntermediateResult() {
        return histogram;
    }

    @Override
    public void aggregateIntermediateResult(int[] partitionResult) {
        if (partitionResult == null)
            return;
        if (histogram == null)
            histogram = new int[BUCKETS];
        for (int i = 0; i < histogram.length && i < partitionResult.length; i++)
            histogram[i] += partitionResult[i];
    }

    @Override
    public int[] getFinalResult() {
        return histogram;
    }

    public static String render(int[] histogram) {
        return histogram == null ? "empty" : Arrays.toString(histogram);
    }
}
