package com.gs.offheap.examples;

import com.gigaspaces.client.iterator.SpaceIterator;
import com.gigaspaces.client.iterator.SpaceIteratorConfiguration;
import com.gigaspaces.client.iterator.SpaceIteratorType;
import com.gs.offheap.common.BuiltEntries;
import com.gs.offheap.common.Payments;
import com.gs.offheap.common.Spaces;
import com.gs.offheap.model.Payment;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;

/**
 * Step 4: a space iterator honours the query projection, and builds no entry to do it.
 * <p>
 * Two changes are visible here.
 * <ul>
 *   <li><b>The projection is applied.</b> An iterator now returns only the properties the query
 *       projected, the way {@code read} and {@code readMultiple} always did: {@code currency}, not
 *       projected below, comes back {@code null}. Before, a {@code CURSOR} iterator ignored the
 *       projection altogether, and a {@code PREFETCH_UIDS} iterator ignored it for the first batch.</li>
 *   <li><b>No entry is built.</b> With a projection of plain properties, both iterator types assemble
 *       what they return straight from the values in the off-heap block.</li>
 * </ul>
 * A {@code PREFETCH_UIDS} iterator works in two steps: when it is created it collects the ids of all
 * matching entries, receiving the first batch of entries with them; then it reads the rest by id,
 * batch by batch. The first step is counted separately, right after the iterator is created.
 * <p>
 * Without a projection every entry is still built: the client asked for whole entries. The control
 * run uses exactly that to prove the counter sees what the space does.
 */
public class SpaceIterators {

    /** currency is left out on purpose: it is how the projection shows. */
    private static final String[] PROJECTION = {"id", "amount"};

    /** Smaller than the data, so that PREFETCH_UIDS has entries to read by id. */
    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) {
        GigaSpace gigaSpace = Spaces.connect(Spaces.OFF_HEAP);
        Payments.write(gigaSpace);

        try (BuiltEntries built = BuiltEntries.watch(gigaSpace)) {
            System.out.println();
            long control = iterate(gigaSpace, built, SpaceIteratorType.CURSOR, false);
            long cursor = iterate(gigaSpace, built, SpaceIteratorType.CURSOR, true);
            long prefetchUids = iterate(gigaSpace, built, SpaceIteratorType.PREFETCH_UIDS, true);

            System.out.println();
            if (control != Payments.COUNT)
                System.out.println("UNEXPECTED: the control built " + control + " of " + Payments.COUNT
                        + " entries, so the counter does not see what the space does. Check that the space "
                        + "was deployed with space.mx.cache-entries-percentage=0.");
            else if (cursor != 0 || prefetchUids != 0)
                System.out.println("UNEXPECTED: an iterator with a projection built entries.");
            else
                System.out.println("As expected: with a projection neither iterator type builds an entry.");
        }
        Spaces.exit();
    }

    private static long iterate(GigaSpace gigaSpace, BuiltEntries built, SpaceIteratorType type, boolean projected) {
        SQLQuery<Payment> query = new SQLQuery<>(Payment.class, "");
        if (projected)
            query.setProjections(PROJECTION);
        String what = type + (projected ? " with projection " + String.join("+", PROJECTION) : " without projection");

        built.restart();
        long start = System.nanoTime();
        int read = 0;
        int currencyNull = 0;
        long builtOnCreation;
        try (SpaceIterator<Payment> iterator = gigaSpace.iterator(query,
                new SpaceIteratorConfiguration().setIteratorType(type).setBatchSize(BATCH_SIZE))) {
            builtOnCreation = built.count();
            while (iterator.hasNext()) {
                if (iterator.next().getCurrency() == null)
                    currencyNull++;
                read++;
            }
        }
        long tookMs = (System.nanoTime() - start) / 1_000_000;
        long total = built.count();

        System.out.printf("%-48s read: %4d  currency null: %4d  built: %5d   took: %4d ms%n",
                what, read, currencyNull, total, tookMs);
        if (type == SpaceIteratorType.PREFETCH_UIDS)
            System.out.printf("  of which built while collecting the ids: %d, while reading by id: %d%n",
                    builtOnCreation, total - builtOnCreation);
        return total;
    }
}
