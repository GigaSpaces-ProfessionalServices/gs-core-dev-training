package com.gs;

import com.gigaspaces.async.AsyncFuture;
import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.UUID;

public class CancelDurableTask {

    private static final Logger logger = LoggerFactory.getLogger(CancelDurableTask.class);
    public static long WAIT_TIME = 3000;
    public static void main(String[] args) throws Exception {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).gigaSpace();
        CancelDurableTask cancelDurableTask = new CancelDurableTask();
        cancelDurableTask.registerAndUnRegisterDurable(gs);
    }

    public void registerAndUnRegisterDurable(GigaSpace gs) throws Exception{
        UUID taskId = gs.registerDurableTask(new CancelableCountTask());
        AsyncFuture<Long> future = gs.executeDurable(taskId);
        future.setListener(result -> {
            if (result.getException() != null) {
                logger.error("Task failed", result.getException());
            } else {
                logger.info("Got results: {}", result.getResult());
            }
        });
        Thread.sleep(WAIT_TIME);
        gs.unregisterDurableTask(taskId);
        // This task should be unregistered when we want to stop it or when it has ended.
        // The only reason to register this task is to have the ability to stop it gracefully when we want.
    }
}
