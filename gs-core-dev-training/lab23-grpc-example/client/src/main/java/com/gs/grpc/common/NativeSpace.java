package com.gs.grpc.common;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

/**
 * The native client, for the Java side of step 3 and the comparison in step 5. Lookup locators and
 * groups come from the {@code -Dcom.gs.jini_lus.locators} and {@code -Dcom.gs.jini_lus.groups} VM
 * options, as in the other labs.
 */
public final class NativeSpace {

    private NativeSpace() {
    }

    public static GigaSpace connect() {
        return new GigaSpaceConfigurer(new SpaceProxyConfigurer(Grpc.SPACE)).gigaSpace();
    }

    /**
     * Ends the program. Closing the proxy does not stop every thread it started, and some of them are
     * not daemons, so without this a program that used the native client would hang at the end.
     */
    public static void exit() {
        System.exit(0);
    }
}
