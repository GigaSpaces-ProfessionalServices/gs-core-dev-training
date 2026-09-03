# gs-core-dev-training - lab18-dynamic_filter-example

---

This lab explores the GigaSpaces JDBC Dynamic Filter hint and how colocated joins affect query performance.

## Goals
1. Understand the Dynamic Filter hint.
2. Learn how to use the Dynamic Filter hint.
3. Understand how routing alignment between joined types determines whether a join is colocated.

## What is a Dynamic Filter Hint

In a partitioned space, a SQL join across two types that route by different keys requires a scatter-gather: the engine queries all partitions for each leg of the join and assembles the result on the coordinator. This is expensive when the data set is large.

The `DYNAMIC_FILTER` hint tells the GigaSpaces JDBC engine to use the result of the leading table scan as a filter on subsequent joins. Instead of scanning all partitions for the joined table, the engine uses the routing keys extracted from the first result set to target only the relevant partitions. This turns a cross-partition scatter-gather into a targeted lookup and can dramatically reduce query time.

The hint is added inline in the SQL `SELECT` statement:

```sql
SELECT /*+ DYNAMIC_FILTER */ s.id, s.firstName, sc.sem, c.name
FROM (SELECT * FROM "com.gs.noncolocated.Student" WHERE id = ?) AS s
JOIN "com.gs.noncolocated.StudentCourses" AS sc ON s.id = sc.studentId
JOIN "com.gs.noncolocated.Courses" AS c ON c.id = sc.courseId
```

The hint has no effect on correctness - it is purely an optimization directive. It also suppresses the protective mode warning that XAP logs when a join condition lacks an explicit routing key filter.

## Build and IntelliJ Setup
1. Change the `gigaspaces.version` in the `pom.xml` and correct the space name in all `main()` programs, if needed.
2. Copy the entire `runConfigurations` folder and its contents from this project, into the `.idea` directory. You will need to restart Intellij.
3. Make sure the `GS_LOOKUP_LOCATORS` and `GS_LOOKUP_GROUPS` environment variables are set correctly.  
   For example, the `GS_LOOKUP_LOCATORS=localhost` and `GS_LOOKUP_GROUPS=xap-17.2.2`.

   In each of the Intellij run configurations, there will be VM options that will reference these environment variables.

   For example,  
   `-Dcom.gs.jini_lus.locators=${GS_LOOKUP_LOCATORS}` and `-Dcom.gs.jini_lus.groups=${GS_LOOKUP_GROUPS}`.

   Please see the main [README.md](https://github.com/GigaSpaces-ProfessionalServices/gs-core-dev-training/blob/main/README.md) for setting path values (environment variables in File | Settings | Appearance & Behavior | Path Values) used when running within Intellij.
4. Build the project: `mvn package`

## Instructions
**Before starting**: The model intentionally omits indexes on join fields so the performance difference between query strategies is clearly visible in the benchmark. XAP's query-without-index protective mode blocks such queries by default, so it must be disabled. Edit `service-overrides.sh` or `service-overrides.bat`:
```
export GS_OPTIONS_EXT="-Dcom.gs.protectiveMode.queryWithoutIndex=false"
```

1. Start the demo:  
   Go to `$GS_HOME/bin`  
   Run `./gs.sh demo` or `gs.bat demo`

   *This starts an agent, manager, 4 gscs, restv3, and the webui. It also deploys a space named 'demo' with 2 partitions and 1 backup apiece.*
2. Run NonColocatedDataGen to write data to the Space.
3. Run ColocatedDataGen to write data to the Space.
4. Go over `DynamicFilterBenchmark` code.
5. Run `DynamicFilterBenchmark`.

### Expected Results

1. Running without the hint and with no routing takes much longer than the other strategies - `Courses` and `StudentCourses` both require full scans across all partitions with no filtering.
2. The protective mode warning (`The query contains unIndexed fields`) appears in the GSC log for all three JDBC strategies, not just the no-hint case. This is because `Courses` has no index on the join field and always requires a full scan regardless of the hint. The `DYNAMIC_FILTER` hint reduces the scan on `StudentCourses` (replacing a full scan with a targeted dynamic filter), but does not eliminate the `Courses` full scan.
3. `joinWithDynamicFilter` is significantly faster than the other JDBC strategies. The hint causes XAP to use the routing keys derived from the `Student` result to filter `StudentCourses` partition-by-partition, rather than scatter-gathering the full table. The final `Courses` scan is still full but operates on a smaller intermediate result set.
4. `com.gs.colocated.StudentCourses` routes by `courseId`, aligning it with `Courses` (which routes by `id`). However, in this three-way join, the optimizer cannot exploit that colocation because the first join (Student → StudentCourses) produces an intermediate result rather than a base table access, preventing a partition-local join for the second leg.
5. Since the query plan is cached, the second run is faster than the first across all strategies. Running services typically encounter the same query patterns repeatedly, so the cached plan benefit compounds over time.
