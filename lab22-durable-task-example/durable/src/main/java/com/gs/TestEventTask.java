package com.gs;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.AsyncFutureListener;
import com.gigaspaces.async.AsyncResult;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TestEventTask {
    public static void main(String[] args) {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).gigaSpace();
        TestEventTask test = new TestEventTask();
        test.runDurable(gs);
    }

    public void runDurable(GigaSpace gs) {
        UUID taskId = gs.registerDurableTask(new MyEventTask());
        System.out.println("Event task started embedded event container:" +taskId);
        //This task should be unregistered only if we want to change business logic, in that case we will unregister, change code and supportcodechange version and register again

    }


}
