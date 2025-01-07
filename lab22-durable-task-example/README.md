# gs-core-dev-training - lab22-durable tasks example

---

## Durable tasks example
###### Goals
1. Understand 2 use cases of durable tasks.
2. Learn the related Java and REST API.
  

###### Instructions

1. Start the demo:  
   Go to `$GS_HOME/bin`  
   Run `./gs.sh demo` or `gs.bat demo`

   *This starts an agent, manager, 4 gscs and the webui. It also deploys a space named 'demo' with 2 partitions and 1 backup apiece.*
2. Change the gigaspaces.version in the `pom.xml` and correct the space name in all main() programs, if needed.
3. Copy the entire `runConfigurations` folder and its contents from this project, into the `.idea` directory. You may need to restart Intellij.
4. Make sure the `GS_LOOKUP_LOCATORS` and `GS_LOOKUP_GROUPS` environment variables are set correctly.  
   For example, the `GS_LOOKUP_LOCATORS=localhost` and `GS_LOOKUP_GROUPS=xap-16.4.0`.
   
   In each of the Intellij run configurations, there will be VM options that will reference these environment variables.     

   For example,  
   `-Dcom.gs.jini_lus.locators=${GS_LOOKUP_LOCATORS}` and `-Dcom.gs.jini_lus.groups=${GS_LOOKUP_GROUPS}`.

   Please see the main [README.md](https://github.com/GigaSpaces-ProfessionalServices/gs-dev-training/blob/main/README.md) for setting path values (environment variables in File | Settings | Appearance & Behavior | Path Values) used when running within Intellij.  
5. Build the project: `mvn package`
6. Run TestEventTask - This will trigger embedded logic to run in each partition
7. Use Rest API at `http://localhost:8090/v2`.to see the list of durableTasks
8. Restart both primary (can be done using gs-ui copied from previous version) ** ms14
9. Validate EvenTask is still in register tasks list via Rest API.
8. Run DataGen to write data to the space.
9. See console log - events are processed
10. Run TestLongTask
11. Change wait time to longer period at TestLongTask and see rest api while and after test ends.

