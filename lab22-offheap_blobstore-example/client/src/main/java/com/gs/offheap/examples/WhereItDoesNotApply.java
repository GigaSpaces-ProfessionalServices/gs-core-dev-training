package com.gs.offheap.examples;

import com.gigaspaces.client.iterator.SpaceIterator;
import com.gigaspaces.client.iterator.SpaceIteratorConfiguration;
import com.gigaspaces.client.iterator.SpaceIteratorType;
import com.gigaspaces.query.aggregators.AggregationSet;
import com.gs.offheap.common.BuiltEntries;
import com.gs.offheap.common.Payments;
import com.gs.offheap.common.Spaces;
import com.gs.offheap.model.Payment;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;

/**
 * Step 6: the same operations on three spaces, to see where reading from the off-heap block applies
 * and where it does not - and how to tell the two apart.
 * <ul>
 *   <li><b>offheapDemo</b> - internal cache off. Every read goes to the off-heap block: the control
 *       builds every entry, the others build none.</li>
 *   <li><b>offheapDefaultCache</b> - the blobstore internal cache at its default size. Entries it
 *       holds are already built objects on the heap and are served from there; the block is not read
 *       at all, so the counter shows zero even for the control. Zero in the control means "served from
 *       the cache", not "fast". This is not something to tune away: a cached entry needs no
 *       optimization, the block-reading paths serve the entries that are not in the cache.</li>
 *   <li><b>offheapWithFilter</b> - an after-read space filter is registered. A filter is handed whole
 *       entries, before the projection is applied, so a batch read with a projection takes the regular
 *       path and builds its entries. Aggregation is not affected: aggregators are not passed through
 *       read filters.</li>
 * </ul>
 * One number on the filtered space needs explaining: a {@code PREFETCH_UIDS} iterator with a
 * projection builds every entry but its first batch. The first batch arrives with the ids, as part
 * of an aggregation, and an aggregation result is not passed through read filters - with or without
 * a projection, on any version. The remaining entries are read by id with a regular read, which is
 * filtered, and so built.
 */
public class WhereItDoesNotApply {

    private static final String[] PROJECTION = {"id", "amount"};
    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) {
        System.out.println();
        System.out.printf("%-22s %12s %8s %18s %24s%n", "space", "maxEntry", "sum",
                "CURSOR projected", "PREFETCH_UIDS projected");
        for (String spaceName : new String[]{Spaces.OFF_HEAP, Spaces.OFF_HEAP_DEFAULT_CACHE, Spaces.OFF_HEAP_WITH_FILTER})
            run(spaceName);
        System.out.printf("%nEach column is the number of entries the operation built, out of %d.%n", Payments.COUNT);
        System.out.println("Expected: offheapDemo 1000 / 0 / 0 / 0, offheapDefaultCache 0 / 0 / 0 / 0, "
                + "offheapWithFilter 1000 / 0 / 1000 / 900.");
        Spaces.exit();
    }

    private static void run(String spaceName) {
        GigaSpace gigaSpace = Spaces.connect(spaceName);
        Payments.writeQuietly(gigaSpace);
        SQLQuery<Payment> all = new SQLQuery<>(Payment.class, "");

        try (BuiltEntries built = BuiltEntries.watch(gigaSpace)) {
            gigaSpace.aggregate(all, new AggregationSet().maxEntry("amount"));
            long maxEntry = built.count();

            built.restart();
            gigaSpace.aggregate(all, new AggregationSet().sum("amount"));
            long sum = built.count();

            built.restart();
            drain(gigaSpace, SpaceIteratorType.CURSOR);
            long cursor = built.count();

            built.restart();
            drain(gigaSpace, SpaceIteratorType.PREFETCH_UIDS);
            long prefetchUids = built.count();

            System.out.printf("%-22s %12d %8d %18d %24d%n", spaceName, maxEntry, sum, cursor, prefetchUids);
        }
    }

    private static void drain(GigaSpace gigaSpace, SpaceIteratorType type) {
        SQLQuery<Payment> query = new SQLQuery<>(Payment.class, "").setProjections(PROJECTION);
        try (SpaceIterator<Payment> iterator = gigaSpace.iterator(query,
                new SpaceIteratorConfiguration().setIteratorType(type).setBatchSize(BATCH_SIZE))) {
            while (iterator.hasNext())
                iterator.next();
        }
    }
}
