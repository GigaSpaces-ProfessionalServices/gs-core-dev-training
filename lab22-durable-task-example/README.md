# gs-core-dev-training - lab22-durable tasks example

---

## Durable tasks example
###### Goals
1. Understand 2 use cases of durable tasks.
2. Learn the related Java and REST API.

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
  

###### Instructions

1. Start the demo:  
   Go to `$GS_HOME/bin`  
   Run `./gs.sh demo` or `gs.bat demo`

   *This starts an agent, manager, 4 gscs and the webui. It also deploys a space named 'demo' with 2 partitions and 1 backup apiece.*
2. Change the gigaspaces.version in the `pom.xml` and correct the space name in all main() programs, if needed.
3. Copy the entire `runConfigurations` folder and its contents from this project, into the `.idea` directory. You may need to restart Intellij.
4. Make sure the `GS_LOOKUP_LOCATORS` and `GS_LOOKUP_GROUPS` environment variables are set correctly.  
   For example, the `GS_LOOKUP_LOCATORS=localhost` and `GS_LOOKUP_GROUPS=xap-17.1.0`.
   
   In each of the Intellij run configurations, there will be VM options that will reference these environment variables.     

   For example,  
   `-Dcom.gs.jini_lus.locators=${GS_LOOKUP_LOCATORS}` and `-Dcom.gs.jini_lus.groups=${GS_LOOKUP_GROUPS}`.

   Please see the main [README.md](https://github.com/GigaSpaces-ProfessionalServices/gs-core-dev-training/blob/main/README.md) for setting path values (environment variables in File | Settings | Appearance & Behavior | Path Values) used when running within Intellij.  
5. Build the project: `mvn package`
6. Run TestEventTask - This will trigger embedded logic to run in each partition
7. Use Rest API at `http://localhost:8090/v2`.to see the list of durableTasks
8. Restart both primary (can be done using gs-ui copied from previous version) ** ms14
9. Validate EvenTask is still in register tasks list via Rest API.
8. Run DataGen to write data to the space.
9. See console log - events are processed
10. Run TestLongTask
11. Change wait time to longer period at TestLongTask and see rest api while and after test ends.

