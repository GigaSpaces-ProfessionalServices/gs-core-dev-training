# gs-core-dev-training - lab16-jdbc_v3-example

---

## Jdbc V3 Example
###### Goals
1. See Jdbc V3 capabilities.
2. Connect with BI tools.
  

###### Instructions

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
7. Go to the Ops-Ui at `http://localhost:8090`.
8. Run queries using the Ops-Ui. 
9. Run JdbcV3Client see queries, explain plan and results.
10. Run MetaDataExplorer (introduced in GigaSpaces v. 16.2).
11. Follow the instructions in the presentation to connect to **Dbeaver** or **Power BI**.

---
For additional information, please see:  
GigaSpaces online documentation regarding [ODBC/JDBC Connection to Tableau and PowerBI Using Data Gateway](https://docs.gigaspaces.com/latest/dev-java/16-2-odbc-jdbc-connection-to-tableau-and-power-bi-using-data-gateway.html).

