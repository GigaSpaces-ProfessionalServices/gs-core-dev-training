# gs-core-dev-training - lab24-dynamic filter example

---

## dynamic filter example
###### Goals
1. Understand dynamic filter hint.
2. Learn how to use dynamic filter hint.
3. Understand colocated joins, join condition related to respective routing of the 2 tables.
  

###### Instructions
**Before starting**: Disable query without index protective mode by editing service-overrides.sh or service-overrides.bat
export GS_OPTIONS_EXT="-Dcom.gs.protectiveMode.queryWithoutIndex=false"

1. Start the demo:  
   Go to `$GS_HOME/bin`  
   Run `./gs.sh demo` or `gs.bat demo`

   *This starts an agent, manager, 4 gscs and the webui. It also deploys a space named 'demo' with 2 partitions and 1 backup apiece.*
2. Change the gigaspaces.version in the `pom.xml` and correct the space name in all main() programs, if needed.
3. Copy the entire `runConfigurations` folder and its contents from this project, into the `.idea` directory. You may need to restart Intellij.
4. Make sure the `GS_LOOKUP_LOCATORS` and `GS_LOOKUP_GROUPS` environment variables are set correctly.  
   For example, the `GS_LOOKUP_LOCATORS=localhost` and `GS_LOOKUP_GROUPS=xap-17.1.0`.
   
   In each of the Intellij run configurations, there will VM options that will reference these environment variables.     

   For example,  
   `-Dcom.gs.jini_lus.locators=${GS_LOOKUP_LOCATORS}` and `-Dcom.gs.jini_lus.groups=${GS_LOOKUP_GROUPS}`.

   Please see the main [README.md](https://github.com/GigaSpaces-ProfessionalServices/gs-core-dev-training/blob/main/README.md) for setting path values (environment variables in File | Settings | Appearance & Behavior | Path Values) used when running within Intellij.  
5. Build the project: `mvn package`
6. Run DataGen to write data to the space.
7. Run DataGen2 to write data to the space
8. Go over TestDynamicHint code
9. Run TestDynamicHint

See that run without the hint with no routing it takes much longer in this scenario and protective warning is logged.
While running the same with the hint, no protective warning is logged as conditions are implicitly added. 
If model is changed so routing of StudentCourses and Courses is courseId as in model2 
and condition for studentId is explicitly added in StudentCourses results are much better than the ones we get without hint in jdbcquery.
Please also note that since plan is cached second execution will be better than first one, an usualy runing services same partterns of queries repeat themselves
