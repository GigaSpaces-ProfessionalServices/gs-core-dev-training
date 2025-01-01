# gs-dev-training - lab22-durable tasks example

---

## cusom aggrgation example
###### Goals
1. Understand custom aggregation
2. Learn thow to use index data in custom aggrgators
  

###### Instructions

1. Start the demo:  
   Go to `$GS_HOME/bin`  
   Run `./gs.sh demo` or `gs.bat demo`

   *This starts an agent, manager, 4 gscs and the webui. It also deploys a space named 'demo' with 2 partitions and 1 backup apiece.*
2. Change the gigaspaces.version in the `pom.xml` and correct the space name in all main() programs, if needed.
3. Copy the entire `runConfigurations` folder and its contents from this project, into the `.idea` directory. You may need to restart Intellij.
4. Make sure the `GS_LOOKUP_LOCATORS` and `GS_LOOKUP_GROUPS` environment variables are set correctly.  
   For example, the `GS_LOOKUP_LOCATORS=localhost` and `GS_LOOKUP_GROUPS=xap-16.4.0`.
   
   In each of the Intellij run configurations, there will VM options that will reference these environment variables.     

   For example,  
   `-Dcom.gs.jini_lus.locators=${GS_LOOKUP_LOCATORS}` and `-Dcom.gs.jini_lus.groups=${GS_LOOKUP_GROUPS}`.

   Please see the main [README.md](https://github.com/GigaSpaces-ProfessionalServices/gs-dev-training/blob/main/README.md) for setting path values (environment variables in File | Settings | Appearance & Behavior | Path Values) used when running within Intellij.  
5. Build the project: `mvn package`
6. Run DataGen to write data to the space.
7. Go over CustomInAggrgator code
8. Run TestInAggrgator
