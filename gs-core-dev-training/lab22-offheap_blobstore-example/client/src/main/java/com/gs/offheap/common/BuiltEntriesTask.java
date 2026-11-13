package com.gs.offheap.common;

import com.gigaspaces.async.AsyncResult;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Runs inside every primary partition and counts how many entries the space builds - decodes from
 * the off-heap block into a full entry. It is sent to the grid like any other task: no jar needs to
 * be deployed for it.
 * <p>
 * How it counts. The space logs one debug line per entry it builds from the off-heap block, under
 * the {@code com.gigaspaces.cache} logger. The task turns that logger up to debug, stops its lines
 * from reaching the container log, and counts them instead. This is a diagnostic aid for this lab,
 * not a product API: it relies on the text of an internal debug line, and it watches the whole
 * container, so two programs counting the same space at the same time would reset each other.
 * <p>
 * The counter has to survive from one task to the next, and every task may arrive in a class loader
 * of its own - so it is found again by its class name and read through a JDK interface, never by
 * its class.
 */
public class BuiltEntriesTask implements DistributedTask<Long, Long> {

    private static final long serialVersionUID = 1L;

    private static final String ENGINE_LOGGER = "com.gigaspaces.cache";
    private static final String BUILT_ENTRY_LINE = "Blobstore- entry loaded from off heap";

    public enum Action {
        /** Start counting from zero; answers what was counted before. */
        RESTART,
        /** Answer what was counted so far and keep counting. */
        READ,
        /** Stop counting and put the logger back as it was; answers what was counted. */
        STOP
    }

    private final Action action;

    @TaskGigaSpace
    private transient GigaSpace gigaSpace;

    public BuiltEntriesTask(Action action) {
        this.action = action;
    }

    @Override
    public Long execute() {
        Logger logger = Logger.getLogger(ENGINE_LOGGER);
        Handler counter = findCounter(logger);
        long counted = counter == null ? 0 : ((LongSupplier) counter).getAsLong();
        if (action == Action.READ)
            return counted;
        if (counter != null)
            logger.removeHandler(counter);
        if (action == Action.RESTART) {
            logger.addHandler(new Counter(gigaSpace.getSpaceName()));
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.FINE);
        } else {
            logger.setLevel(null);
            logger.setUseParentHandlers(true);
        }
        return counted;
    }

    @Override
    public Long reduce(List<AsyncResult<Long>> results) throws Exception {
        long total = 0;
        for (AsyncResult<Long> result : results) {
            if (result.getException() != null)
                throw result.getException();
            total += result.getResult();
        }
        return total;
    }

    private static Handler findCounter(Logger logger) {
        for (Handler handler : logger.getHandlers())
            if (handler instanceof LongSupplier && handler.getClass().getName().equals(Counter.class.getName()))
                return handler;
        return null;
    }

    /** Counts the built-entry lines of one space; the logger is shared by every space in the container. */
    static final class Counter extends Handler implements LongSupplier {

        private final AtomicLong counted = new AtomicLong();
        private final String spaceMarker;

        Counter(String spaceName) {
            // The line names the space as "container [<container>:<space>]".
            this.spaceMarker = ":" + spaceName + "]";
        }

        @Override
        public void publish(LogRecord record) {
            String message = record == null ? null : record.getMessage();
            if (message != null && message.contains(BUILT_ENTRY_LINE) && message.contains(spaceMarker))
                counted.incrementAndGet();
        }

        @Override
        public long getAsLong() {
            return counted.get();
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
