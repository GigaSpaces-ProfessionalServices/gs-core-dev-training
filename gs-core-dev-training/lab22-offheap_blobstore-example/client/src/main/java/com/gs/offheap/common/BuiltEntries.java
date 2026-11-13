package com.gs.offheap.common;

import org.openspaces.core.GigaSpace;

import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * The client side of {@link BuiltEntriesTask}: how many entries the space built while an operation
 * ran, summed over all partitions.
 * <p>
 * This is the number to watch in every example. A regular read or aggregation over an off-heap
 * space decodes every entry it looks at into a full object, even to read a single property; the
 * optimized paths read the needed values straight from the off-heap block and build nothing. With a
 * thousand entries the time hardly differs, the count does.
 * <p>
 * Zero means success only if the counter can see anything at all, so the examples start with a
 * control: an operation that must build every entry.
 */
public final class BuiltEntries implements AutoCloseable {

    private final GigaSpace gigaSpace;

    private BuiltEntries(GigaSpace gigaSpace) {
        this.gigaSpace = gigaSpace;
    }

    public static BuiltEntries watch(GigaSpace gigaSpace) {
        BuiltEntries built = new BuiltEntries(gigaSpace);
        built.restart();
        return built;
    }

    /** Starts counting from zero. */
    public void restart() {
        run(BuiltEntriesTask.Action.RESTART);
    }

    /** Entries built since the last {@link #restart()}. */
    public long count() {
        return run(BuiltEntriesTask.Action.READ);
    }

    /**
     * Runs one operation and prints what it returned, how many entries it built and how long it took.
     * The time includes the network round trip and, with this little data, says little on its own.
     */
    public <T> T measure(String what, Callable<T> operation) {
        restart();
        long start = System.nanoTime();
        T result;
        try {
            result = operation.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        long tookMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("%-48s result: %-14s built: %5d   took: %4d ms%n",
                what, abbreviate(result), count(), tookMs);
        return result;
    }

    @Override
    public void close() {
        run(BuiltEntriesTask.Action.STOP);
    }

    private long run(BuiltEntriesTask.Action action) {
        try {
            return gigaSpace.execute(new BuiltEntriesTask(action)).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not count the built entries in " + gigaSpace.getSpaceName(), e);
        }
    }

    private static String abbreviate(Object result) {
        String text = result instanceof int[] array ? Arrays.toString(array) : String.valueOf(result);
        return text.length() <= 14 ? text : text.substring(0, 11) + "...";
    }
}
