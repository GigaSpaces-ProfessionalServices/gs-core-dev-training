package com.gs;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.UUID;

public class RegisterDurableTask {
    public static void main(String[] args) {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).gigaSpace();
        RegisterDurableTask test = new RegisterDurableTask();
        test.runDurable(gs);
    }

    public void runDurable(GigaSpace gs) {
        UUID taskId = gs.registerDurableTask(new EmbeddedPollingDurableTask());
        System.out.println("Event task started embedded event container: " + taskId);
        /* This task should be unregistered only if we want to change business logic.
         * In that case we will unregister, change code, update supportcodechange version, and re-register. */
    }
}
