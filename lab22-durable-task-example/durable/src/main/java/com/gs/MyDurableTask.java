package com.gs;
import com.gigaspaces.annotation.SupportCodeChange;
import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.client.iterator.SpaceIterator;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DurableTask;
import org.openspaces.core.executor.TaskGigaSpace;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
This task  iterate over all objects and do some time consuming job, and return number of objects processed
 */

@SupportCodeChange(id="1")
public class MyDurableTask implements  DurableTask<Integer, Long> {
    @Override
    public boolean isAutoStart() {
        return false;
    }

    @TaskGigaSpace
    private transient GigaSpace gigaSpace;

    transient boolean cancel = false;


    public Integer execute() throws Exception {
        SQLQuery<Object> query = new SQLQuery(Object.class,"");
        AtomicInteger counter = new AtomicInteger();
        SpaceIterator<Object> spaceIterator = gigaSpace.iterator(query);
        while (spaceIterator.hasNext() && !cancel) {
            System.out.println("counting " + counter.incrementAndGet());
            Thread.sleep(500);
        }
        return counter.get();
    }

    public Long reduce(List<AsyncResult<Integer>> results) throws Exception {
        long sum = 0;
        for (AsyncResult<Integer> result : results) {
            if (result.getException() != null) {
                throw result.getException();
            }
            sum += result.getResult();
        }
        return sum;
    }


    public boolean cancel() throws Exception{
        cancel = true;
        return true;
    }


    public String name(){
        return "longRunningJob";
    }

    public String description(){ return "this task is responsble of iterating over all objects and gather statitics related to ...";}
}
