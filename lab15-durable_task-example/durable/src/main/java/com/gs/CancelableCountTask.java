package com.gs;
import com.gigaspaces.annotation.SupportCodeChange;
import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.client.iterator.SpaceIterator;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DurableTask;
import org.openspaces.core.executor.TaskGigaSpace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/*
   This task iterates over all objects, does a time-consuming job, and returns the number of objects processed.
 */

@SupportCodeChange(id="1")
public class CancelableCountTask implements  DurableTask<Integer, Long> {

    private static final Logger logger = LoggerFactory.getLogger(CancelableCountTask.class);

    @Override
    public boolean isAutoStart() {
        return false;
    }

    @TaskGigaSpace
    private transient GigaSpace gigaSpace;

    private transient volatile boolean cancel = false;


    public Integer execute() throws Exception {
        SQLQuery<Object> query = new SQLQuery<>(Object.class, "");
        AtomicInteger counter = new AtomicInteger();
        try (SpaceIterator<Object> spaceIterator = gigaSpace.iterator(query)) {
            while (spaceIterator.hasNext() && !cancel) {
                // spaceIterator.next();
                logger.info("counting {}", counter.incrementAndGet());
                Thread.sleep(500);
            }
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
        return "CancelableCountTask";
    }

    public String description(){ return "Iterates over all entries in the partition, counting them with a simulated delay. Supports cancellation mid-run.";}
}
