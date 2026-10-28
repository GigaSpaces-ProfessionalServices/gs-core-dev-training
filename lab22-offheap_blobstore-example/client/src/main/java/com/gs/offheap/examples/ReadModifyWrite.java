package com.gs.offheap.examples;

import com.gigaspaces.client.WriteModifiers;
import com.gigaspaces.client.iterator.SpaceIterator;
import com.gigaspaces.client.iterator.SpaceIteratorConfiguration;
import com.gigaspaces.client.iterator.SpaceIteratorType;
import com.gs.offheap.common.Payments;
import com.gs.offheap.common.Spaces;
import com.gs.offheap.model.Payment;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;

/**
 * Step 5: the one change that can break existing code silently - a read-modify-write loop over an
 * iterator with a projection.
 * <p>
 * Such a loop reads every payment with a projection of {@code id} and {@code amount}, raises the
 * amount, and writes the payment back. Before the iterator honoured the projection it was handed
 * whole payments and stored them whole. Now it is handed payments whose {@code currency} is
 * {@code null}, and a plain {@code write} stores that {@code null} over the currency that was there.
 * No exception, no failure - the data is just gone.
 * <p>
 * Two ways out: drop the projection where the objects are written back, or write them back as a
 * partial update - {@link WriteModifiers#PARTIAL_UPDATE} updates the non-null properties and leaves
 * the rest alone. (The {@code change} API does the same, without reading the entry first.)
 */
public class ReadModifyWrite {

    private static final String[] PROJECTION = {"id", "amount"};

    public static void main(String[] args) {
        GigaSpace gigaSpace = Spaces.connect(Spaces.OFF_HEAP);

        System.out.println();
        Payments.write(gigaSpace);
        raiseAmounts(gigaSpace, WriteModifiers.UPDATE_OR_WRITE);
        long lostWithWrite = paymentsWithoutCurrency(gigaSpace);
        System.out.printf("plain write:   payments that lost their currency: %d of %d%n",
                lostWithWrite, Payments.COUNT);

        Payments.write(gigaSpace);
        raiseAmounts(gigaSpace, WriteModifiers.PARTIAL_UPDATE);
        long lostWithPartialUpdate = paymentsWithoutCurrency(gigaSpace);
        System.out.printf("PARTIAL_UPDATE: payments that lost their currency: %d of %d, amounts raised: %s%n",
                lostWithPartialUpdate, Payments.COUNT, amountsRaised(gigaSpace) ? "yes" : "NO");

        System.out.println();
        if (lostWithWrite == Payments.COUNT && lostWithPartialUpdate == 0)
            System.out.println("As expected: writing back projected objects erases what was not projected; "
                    + "a partial update does not.");
        else if (lostWithWrite == 0)
            System.out.println("The plain write lost nothing: this grid does not apply the projection to "
                    + "iterators, so the loop was handed whole payments - the behaviour before 18.0.1.");
        else
            System.out.println("UNEXPECTED: see the numbers above.");
        Spaces.exit();
    }

    /** The loop that a client may already have: read with a projection, modify, write back. */
    private static void raiseAmounts(GigaSpace gigaSpace, WriteModifiers modifiers) {
        SQLQuery<Payment> query = new SQLQuery<>(Payment.class, "").setProjections(PROJECTION);
        try (SpaceIterator<Payment> iterator = gigaSpace.iterator(query,
                new SpaceIteratorConfiguration().setIteratorType(SpaceIteratorType.CURSOR))) {
            while (iterator.hasNext()) {
                Payment payment = iterator.next();
                payment.setAmount(payment.getAmount() + 1);
                gigaSpace.write(payment, modifiers);
            }
        }
    }

    private static long paymentsWithoutCurrency(GigaSpace gigaSpace) {
        return gigaSpace.count(new SQLQuery<>(Payment.class, "currency is null"));
    }

    private static boolean amountsRaised(GigaSpace gigaSpace) {
        Payment first = gigaSpace.readById(Payment.class, 1);
        return first != null && first.getAmount() == 2.0;
    }
}
