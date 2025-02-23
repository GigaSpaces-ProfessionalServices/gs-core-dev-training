# gs-core-dev-training - lab22-durable tasks example

---

## Durable tasks 
###### What is a durable task
A Durable Task is a Distributed Task that can be canceled and recovered.
Use cases :
1. Task that does a job that takes long time and needs the ability to cancel.
2. Business logic - Adds the ability to change business logic of the space without redeploy of the space.

Durable tasks extends distributed tasks
See: https://docs.gigaspaces.com/17.0/dev-java/task-execution-overview.html DistributedTask API section
As task it supports : @SupportCodeChange annotation
As explained in: https://docs.gigaspaces.com/17.0/dev-java/the-space-no-restart.html
For Business logic tasks tasks this annotation must be used to allow change task code without restarts.
For Business logic tasks, (autoStart=true) related classes will be kept in zookeeper to allow automatic re-execution in case of failover, in order not to store too much data it is recommended to consider task dependencies and decide what should be dynamically loaded and what is in the common class loader.


####### GigaSpace API to support DurableTask
UUID registerDurableTask(DurableTask<T, R> eventTask)
AsyncFuture executeDurable(UUID uuid) 
Set<DurableTaskInfo> listDurableTasks()
Boolean  UnRegisterDurableTask(UUID task)

List & unregister are available in Rest API
Durable task extends distributed task including support code change. execute method should be implemented instructing what to do in each partition.
Unlike regular task don’t call execute method directly and instead use 
executeDurable  for tasks that return false to isAutoStart in order to maintain task status. For auto started tasks execute will be called automatically when needed.


######## Interface
interface DurableTask<T extends  Serializable, R> extends DistributedTask<T, R>

 String name()
Name of the durable task

String description()
Describe What is this task doing

boolean isAutoStart()
should be true for business logic task and will allow automatic  start upon register and rerun after failover, 
for long running task should be false and execute should be called explicitly

boolean cancel() 
add logic what to do to terminate job or cancel business logic


Methods From DistributedTask

T execute() throws Exception 
Code that will be executed in each primary parttion

R reduce(List<AsyncResult<T>> results) throws Exception
code that will run in the client and will gather results from all parttions and return final results