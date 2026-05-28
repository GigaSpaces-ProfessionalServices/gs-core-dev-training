# gs-core-dev-training - lab15-durable_tasks-example

This lab explores GigaSpaces Durable Tasks - Distributed Tasks that can be canceled and recovered. Two use cases are covered: long-running jobs that need the ability to cancel mid-execution, and business logic tasks that run persistently in the Space and survive failover without redeployment.

## Goals
1. Understand two use cases of Durable Tasks.
2. Learn the related Java and REST API.

## What is a Durable Task?

A Durable Task is a Distributed Task that can be canceled and recovered.

**Use cases:**
1. A task that does a job that takes a long time and needs the ability to cancel.
2. Business logic - Adds the ability to change business logic of the Space without Space redeployment.

* Durable Tasks extend Distributed Tasks.  
  See: https://docs.gigaspaces.com/latest/dev-java/task-execution-overview.html DistributedTask API section

* As a task, it supports the `@SupportCodeChange` annotation.
  For business logic tasks, this annotation must be used to allow changes to task code without restarts.  
  See: https://docs.gigaspaces.com/latest/dev-java/the-space-no-restart.html

* For business logic tasks, when `isAutoStart()` returns `true` related classes will be kept in ZooKeeper to allow automatic re-execution in case of failover.  
  **Note:** In order not to store too much data it is recommended to consider task dependencies and decide what should be dynamically loaded and what is in the common class loader.  
  See: https://docs.gigaspaces.com/latest/dev-java/task-execution-overview.html

## API Reference

### GigaSpaces API
```
UUID registerDurableTask(DurableTask<T, R> eventTask)
AsyncFuture executeDurable(UUID uuid)
Set<DurableTaskInfo> listDurableTasks()
Boolean unregisterDurableTask(UUID task)
```

`listDurableTasks` and `unregisterDurableTask` are also available in the REST API.

### Durable Task Interface
```
interface DurableTask<T extends Serializable, R> extends DistributedTask<T, R>
```
`String name()` - name of the Durable Task.  
`String description()` - describe what this task does.  
`boolean isAutoStart()` - should be true for a business logic task and will allow automatic start upon registration and will rerun after failover.
For a long-running task, this should be false and `execute()` should be called explicitly.  
`boolean cancel()` - Add the logic to execute when a job terminates or is canceled.

### Methods From DistributedTask
`T execute() throws Exception` - Code that will be executed in each primary partition.  
`R reduce(List<AsyncResult<T>> results) throws Exception` - Code that will run in the client and will gather results from all partitions and return final results.

### Usage Notes
* Durable Task extends Distributed Task and includes support for code change.
* The `execute()` method should be implemented to instruct what to do in each partition.
* Unlike a regular task, do not call `execute()` directly. For tasks where `isAutoStart()` returns `false`, use `executeDurable(uuid)` instead — this allows the framework to track task status and support cancellation and recovery. For tasks where `isAutoStart()` returns `true`, `execute()` is called automatically by the framework upon registration and after failover.

## Build and IntelliJ Setup
1. Change the `gigaspaces.version` in the `pom.xml`. Correct the Space name in all `main()` programs, if needed.
2. Copy the entire `runConfigurations` folder and its contents from this project, into the `.idea` directory. You will need to restart Intellij.
3. Make sure the `GS_LOOKUP_LOCATORS` and `GS_LOOKUP_GROUPS` environment variables are set correctly.  
   For example, the `GS_LOOKUP_LOCATORS=localhost` and `GS_LOOKUP_GROUPS=xap-17.2.1`.

   In each of the Intellij run configurations, there will be VM options that will reference these environment variables.

   For example,  
   `-Dcom.gs.jini_lus.locators=${GS_LOOKUP_LOCATORS}` and `-Dcom.gs.jini_lus.groups=${GS_LOOKUP_GROUPS}`.

   Please see the main [README.md](https://github.com/GigaSpaces-ProfessionalServices/gs-core-dev-training/blob/main/README.md) for setting path values (environment variables in File | Settings | Appearance & Behavior | Path Values) used when running within Intellij.
4. Build the project: `mvn package`

## Instructions

1. Start the Service Grid:  
   Go to `$GS_HOME/bin`  
   Run `./gs.sh host run-agent --manager --gsc-4 --restv3 --webui`

   *This starts an agent, manager, 4 gscs, restv3, and the webui.*
2. Deploy the Space. For example:
   `./gs.sh pu deploy demo <LAB_HOME>/space/target/space.jar`
3. From the Intellij Run Configurations, run RegisterDurableTask - This registers `EmbeddedPollingDurableTask` (with `isAutoStart=true`), which causes it to automatically start an embedded event container in each partition.
4. Use the REST API at http://localhost:9090/api/v3/swagger-ui/index.html#/Spaces/listDurableTasks to see the list of Durable Tasks.
5. Restart the GSC for both of the primary partitions (can be done using REST API Swagger UI).
6. Validate `"Business Logic Task"` is still in the registered tasks list via REST API.
7. Run DataGen to write data to the Space.
8. See the console log - events are processed.
9. Run CancelDurableTask - This registers and executes `CancelableCountTask`, waits `WAIT_TIME` (default 3 seconds), then unregisters it.
10. Change `WAIT_TIME` to a longer period in `CancelDurableTask.java` and observe the REST API while the task is running and after it ends.
