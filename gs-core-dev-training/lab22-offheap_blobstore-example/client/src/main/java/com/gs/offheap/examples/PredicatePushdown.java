package com.gs.offheap.examples;

import com.gigaspaces.client.ChangeSet;
import com.gigaspaces.query.aggregators.AggregationSet;
import com.gs.offheap.common.BuiltEntries;
import com.gs.offheap.common.Payments;
import com.gs.offheap.common.Spaces;
import com.gs.offheap.model.Payment;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;

/**
 * Step 1: a query condition is evaluated against the off-heap block.
 * <p>
 * A space has to look at every entry a non-indexed condition might match. On an off-heap space,
 * looking used to mean building: every candidate was decoded into a full entry and only then tested.
 * Now the properties the condition names are read straight from the block, and an entry that does
 * not match is dropped without being built. Only the matching entries are built - because a read
 * without a projection returns them whole.
 * <p>
 * There is no API for this: every query gets it. The more selective the query, the larger the gain;
 * a query that matches almost everything gains almost nothing.
 * <p>
 * A change gets it too: the entries it does not match are not built. The ones it changes are - a
 * change needs the entry. So put the selection in the query: a custom change operation that is
 * handed every entry and skips most of them makes the space build them all.
 */
public class PredicatePushdown {

    public static void main(String[] args) {
        GigaSpace gigaSpace = Spaces.connect(Spaces.OFF_HEAP);
        Payments.write(gigaSpace);

        try (BuiltEntries built = BuiltEntries.watch(gigaSpace)) {
            System.out.println();
            Integer all = built.measure("control: readMultiple, no condition",
                    () -> gigaSpace.readMultiple(new SQLQuery<>(Payment.class, "")).length);
            long control = built.count();

            Integer selected = built.measure("readMultiple where amount > 990",
                    () -> gigaSpace.readMultiple(new SQLQuery<>(Payment.class, "amount > 990")).length);
            long selectedBuilt = built.count();

            built.measure("readMultiple where currency='USD' and amount>990",
                    () -> gigaSpace.readMultiple(new SQLQuery<>(Payment.class,
                            "currency = 'USD' AND amount > 990")).length);

            built.measure("count() where amount > 990",
                    () -> gigaSpace.aggregate(new SQLQuery<>(Payment.class, "amount > 990"),
                            new AggregationSet().count()).get(0));
            long countBuilt = built.count();

            // Last, because it changes the data. The payments it raises are rewritten by the next run.
            Integer changed = built.measure("change where amount > 990",
                    () -> gigaSpace.change(new SQLQuery<>(Payment.class, "amount > 990"),
                            new ChangeSet().increment("amount", 1.0)).getNumberOfChangedEntries());
            long changeBuilt = built.count();

            System.out.println();
            if (control != all)
                System.out.println("UNEXPECTED: the control built " + control + " of " + all + " entries, so the "
                        + "counter does not see what the space does. Check that the space was deployed with "
                        + "space.mx.cache-entries-percentage=0. Until then the numbers above mean nothing.");
            else if (selectedBuilt != selected || selected != Payments.expectedCountAbove(990))
                System.out.println("UNEXPECTED: amount > 990 returned " + selected + " and built " + selectedBuilt
                        + "; expected " + Payments.expectedCountAbove(990) + " of each.");
            else if (countBuilt != 0)
                System.out.println("UNEXPECTED: count() built " + countBuilt + " entries.");
            else if (changeBuilt != changed || changed != Payments.expectedCountAbove(990))
                System.out.println("UNEXPECTED: change where amount > 990 changed " + changed + " and built "
                        + changeBuilt + "; expected " + Payments.expectedCountAbove(990) + " of each.");
            else
                System.out.println("As expected: a read or a change builds only the entries it matches,"
                        + " and count() builds none.");
        }
        Spaces.exit();
    }
}
