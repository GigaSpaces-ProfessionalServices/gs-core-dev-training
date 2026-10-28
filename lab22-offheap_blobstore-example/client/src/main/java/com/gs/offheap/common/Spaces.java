package com.gs.offheap.common;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

/**
 * The three spaces this lab deploys, and how to connect to them. Lookup locators and groups come
 * from the {@code -Dcom.gs.jini_lus.locators} and {@code -Dcom.gs.jini_lus.groups} VM options, as in
 * the other labs. See the README for the deploy commands.
 */
public final class Spaces {

    /** Off-heap blobstore, internal cache switched off: every read goes to the off-heap block. */
    public static final String OFF_HEAP = "offheapDemo";

    /** The same off-heap blobstore with the internal cache left at its default size. */
    public static final String OFF_HEAP_DEFAULT_CACHE = "offheapDefaultCache";

    /** The same as {@link #OFF_HEAP}, plus an after-read space filter (the filter-space module). */
    public static final String OFF_HEAP_WITH_FILTER = "offheapWithFilter";

    private Spaces() {
    }

    public static GigaSpace connect(String spaceName) {
        return new GigaSpaceConfigurer(new SpaceProxyConfigurer(spaceName)).gigaSpace();
    }

    /**
     * Ends the program. Closing the proxy does not stop every thread it started, and some of them are
     * not daemons, so without this the program would print everything and then hang.
     */
    public static void exit() {
        System.exit(0);
    }
}
