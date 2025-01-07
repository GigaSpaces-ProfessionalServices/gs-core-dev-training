package com.gs;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.AsyncFutureListener;
import com.gigaspaces.async.AsyncResult;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.UUID;



public class TestLongTask {
    public static long WAIT_TIME = 3000;
    public static void main(String[] args) throws Exception {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).gigaSpace();
        TestLongTask test = new TestLongTask();
        test.runDurable(gs);
    }

    public void runDurable(GigaSpace gs) throws Exception{
        UUID taskId = gs.registerDurableTask(new MyDurableTask());
        AsyncFuture<Long> future = gs.executeDurable(taskId);
        future.setListener(new MyTaskListner());
        Thread.sleep(WAIT_TIME);
        gs.unregisterDurableTask(taskId);
        // This task should be unregisterd when we want to stop it or when its ended
        // The only reason to register this task is to have the ability to stop it gracefuly when we want.
    }

    class MyTaskListner implements AsyncFutureListener {

        @Override
        public void onResult(AsyncResult result) {
            System.out.println("got results! " + result.getResult().toString());
        }
    }
}
