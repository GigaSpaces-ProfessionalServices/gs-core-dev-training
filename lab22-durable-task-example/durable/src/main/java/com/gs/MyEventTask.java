package com.gs;

import com.gigaspaces.annotation.SupportCodeChange;
import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.client.iterator.SpaceIterator;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DurableTask;
import org.openspaces.core.executor.TaskGigaSpace;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.SimplePollingContainerConfigurer;
import org.openspaces.events.polling.SimplePollingEventListenerContainer;

import java.util.List;
@SupportCodeChange(id="1")
public class MyEventTask implements DurableTask<Boolean, Boolean> {

        @TaskGigaSpace
        transient GigaSpace gigaSpace;
        transient SimplePollingEventListenerContainer pollingEventListenerContainer;

        @Override
        public boolean isAutoStart()  {
            return true;
        }
        @Override
        public Boolean execute() throws Exception {
            pollingEventListenerContainer = new SimplePollingContainerConfigurer(gigaSpace).template(new Purchase(false))
                    .autoStart(true).eventListenerAnnotation(new Object() {
                        @SpaceDataEvent
                        public void eventHappened(Purchase data) {
                            try {
                                data.setProccessed(true);
                                System.out.println("new event was Proccessed");
                                //Do some logic
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).pollingContainer();
            return true;
        }
        @Override
        public Boolean reduce(List<AsyncResult<Boolean>> list) throws Exception {
            for (AsyncResult<Boolean> result : list) {
                if (result.getException() != null) {
                    throw result.getException();
                }
            }
            return true;
        }

        @Override
        public boolean cancel() throws Exception {
            pollingEventListenerContainer.destroy();
            return true;
        }





    public String name(){
        return "Buisness Logic Task";
    }

    public String description(){ return "this task is responsble of starting embedded logic as event containers ...";}
}
