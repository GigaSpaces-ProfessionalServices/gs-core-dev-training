# gs-core-dev-training - lab23-custom aggregation example

---

## custom aggregation example
###### Goals
1. Understand custom aggregation.
2. Learn how to use index data in custom aggregator

###### What are aggregations
count, sum, avg, min, max, orderby, groupby etc are example of built in aggrgetaion
Aggregation go over data of specific table with certain condition and gather information 
When possiable index and counters will be used to optimize performance
As Gigaspace is disterbuted in memory data grid, aggregation will be preformed in the server side per each partition
and reduced in the client, several aggrgation can be sent together in aggregationSet

###### GigaSpace API For aggregation
 <T> AggregationResult aggregate(ISpaceQuery<T> query, AggregationSet aggregationSet, ReadModifiers readModifiers)
 
 aggregations will be used implicetly in some SQLQuery as:
 used implicitly via SQLQuery: 
 SqlQuery<Person> query = new SqlQuery<Person>("Age >= ? ORDER BY FirstName = ?")
 
 Or when using JDBC V3 query: 
 Select sum(amount) as total from table1 
 
####### What are custom aggregations
When needed you can define your own custom aggregation by extending SpaceEntriesAggregator
In the example CustomINAggregator we extends AbstractPathAggregator which extends SpaceEntriesAggregator adding common functionality to one field based aggregation
 

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
7. Go over CustomINAggregator code
8. Run TestInAggrgator

#### Note: There is no need to define custom aggrgation for IN as shown in this example by default the IN condition use index
