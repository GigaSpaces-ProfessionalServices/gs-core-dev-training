package com.gs;

import com.gigaspaces.annotation.SupportCodeChange;
import com.gigaspaces.async.AsyncResult;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.DurableTask;
import org.openspaces.core.executor.TaskGigaSpace;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.SimplePollingContainerConfigurer;
import org.openspaces.events.polling.SimplePollingEventListenerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
@SupportCodeChange(id="1")
public class EmbeddedPollingDurableTask implements DurableTask<Boolean, Boolean> {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedPollingDurableTask.class);

    @TaskGigaSpace
        transient GigaSpace gigaSpace;
        transient SimplePollingEventListenerContainer pollingEventListenerContainer;

        @Override
        public boolean isAutoStart()  {
            return true;
        }
        @Override
        public Boolean execute() throws Exception {
            pollingEventListenerContainer = new SimplePollingContainerConfigurer(gigaSpace).template(new Purchase(PurchaseStatus.NEW))
                    .autoStart(true).eventListenerAnnotation(new Object() {
                        @SpaceDataEvent
                        public void eventHappened(Purchase data) {
                            try {
                                data.setPurchaseStatus(PurchaseStatus.PROCESSED);
                                logger.info("New event was Processed");
                                //Do some logic
                                Thread.sleep(100);
                                // gigaSpace.write(data);
                            } catch (InterruptedException e) {
                                logger.error("Event processing interrupted", e);
                                Thread.currentThread().interrupt();
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
            if (pollingEventListenerContainer != null) {
                pollingEventListenerContainer.destroy();
            }
            return true;
        }

    public String name(){
        return "Business Logic Task";
    }

    public String description(){ return "Runs an embedded polling container in each partition that processes unprocessed Purchase entries. Survives failover via auto-start.";}
}
