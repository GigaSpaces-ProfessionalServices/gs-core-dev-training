package com.gs;

import com.gs.noncolocated.Courses;
import com.gs.noncolocated.Student;
import com.gs.noncolocated.StudentCourses;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Benchmarks four query strategies against the same three-way join to demonstrate the performance impact
 * of routing conditions and the DYNAMIC_FILTER hint in a partitioned GigaSpaces space.
 *
 * <p>The colocated model (com.gs.colocated) routes StudentCourses by courseId, aligning it with Courses.
 * The non-colocated model (com.gs.noncolocated) routes StudentCourses by auto-generated id, requiring
 * the DYNAMIC_FILTER hint to avoid a full scatter-gather across all partitions.
 *
 * <p>Each strategy runs twice so warm (cached query plan) vs. cold performance is visible.
 */
public class DynamicFilterBenchmark {
    private static final Logger logger = LoggerFactory.getLogger(DynamicFilterBenchmark.class);
    private final String EXPLAIN_PLAN_PREFIX = "EXPLAIN ANALYZE FOR ";
    public static void main(String[] args) throws Exception {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).gigaSpace();

        DynamicFilterBenchmark benchmark = new DynamicFilterBenchmark();
        for (int k=0; k<2; k++) {
            logger.info("========================================= Run number : {}", k);
            benchmark.joinWithNoHintNoRouting(1, gs);
            benchmark.joinWithRoutingCondition(1, gs);
            benchmark.joinWithDynamicFilter(1, gs);
            benchmark.queryWithNativeApi(gs, 1);
        }
    }


    // Baseline: no hint, no routing condition. XAP scatter-gathers all partitions for every leg of the join.
    protected void joinWithNoHintNoRouting(int studentId, GigaSpace gs) throws Exception{

        StringBuilder queryBuffer = new StringBuilder(6000);
        queryBuffer.append("select s.id, s.firstName, s.lastName,sc.sem, c.id, c.name ");
        queryBuffer.append("from (select * from\"com.gs.colocated.Student\" where id=?) as s ");
        queryBuffer.append("join \"com.gs.colocated.StudentCourses\"  as sc ON s.id=sc.studentId ");
        queryBuffer.append("join \"com.gs.colocated.Courses\"  as c ON c.id=sc.courseId ");


        Object[] params = new Object[1]; params[0]=studentId;
        long time = read(getConnection(gs.getSpaceName()), queryBuffer.toString(), params,false, true);
        logger.info("joinWithNoHintNoRouting took: {} ms", time);

    }

    // No hint, but adds WHERE studentId=? on the StudentCourses subquery, giving XAP a routing key for that leg.
    protected void joinWithRoutingCondition(int studentId, GigaSpace gs) throws Exception{

        StringBuilder queryBuffer = new StringBuilder(6000);
        queryBuffer.append("select s.id, s.firstName, s.lastName,sc.sem, c.id, c.name ");
        queryBuffer.append("from (select * from\"com.gs.colocated.Student\" where id=?) as s ");
        queryBuffer.append("join (select * from \"com.gs.colocated.StudentCourses\" where studentId=?) as sc ON s.id=sc.studentId ");
        queryBuffer.append("join \"com.gs.colocated.Courses\"  as c ON c.id=sc.courseId ");

        Object[] params = new Object[2]; params[0]=studentId; params[1]=studentId;

        long time = read(getConnection(gs.getSpaceName()), queryBuffer.toString(), params,false, true);
        logger.info("joinWithRoutingCondition took: {} ms", time);

    }


    // Uses the DYNAMIC_FILTER hint: XAP derives routing keys from the Student result and targets only the relevant
    // partitions for subsequent joins, avoiding a full scatter-gather. Uses model v1 types (Student/StudentCourses/Courses).
    protected void joinWithDynamicFilter(int studentId, GigaSpace gs) throws Exception{

        StringBuilder queryBuffer = new StringBuilder(6000);
        queryBuffer.append("select /*+ DYNAMIC_FILTER*/  s.id, s.firstName, s.lastName,sc.sem, c.id, c.name ");
        queryBuffer.append("from (select * from\"com.gs.noncolocated.Student\" where id=?) as s ");
        queryBuffer.append("join \"com.gs.noncolocated.StudentCourses\"  as sc ON s.id=sc.studentId ");
        queryBuffer.append("join \"com.gs.noncolocated.Courses\"  as c ON c.id=sc.courseId ");

        Object[] params = new Object[1]; params[0]=studentId;
        long time = read(getConnection(gs.getSpaceName()), queryBuffer.toString(), params,false,true);
        logger.info("joinWithDynamicFilter took: {} ms", time);

    }

    protected Collection<Object> getStudentCoursesIds(int studentId, GigaSpace gs){
        HashSet<Object> ids = new HashSet<>(NonColocatedDataGen.N_COURSE_PER_STUDENT);
        SQLQuery<StudentCourses> studentCoursesSQLQuery = new SQLQuery<>(StudentCourses.class, "studentId=?");
        studentCoursesSQLQuery.setParameter(1, studentId);
        studentCoursesSQLQuery.setProjections("courseId");
        StudentCourses[] results = gs.readMultiple(studentCoursesSQLQuery);
        Arrays.stream(results).forEach(sc-> ids.add(sc.getCourseId()));
        return ids;
    }

    // Bypasses JDBC entirely: two targeted GigaSpace API calls with explicit routing. Fastest approach.
    private void queryWithNativeApi(GigaSpace gs, int studentId) throws Exception {

        long start=System.currentTimeMillis();

        SQLQuery<Courses> studentCourses = new SQLQuery<Courses>(Courses.class, "id in (?)");
        studentCourses.setParameter(1, getStudentCoursesIds(studentId, gs));
        // We actually need data regarding courses the student take and student details
        Courses[] results = gs.readMultiple(studentCourses);
        Student template = new Student();
        template.setId(studentId);
        Student student = gs.read(template);
        long end=System.currentTimeMillis();
        logger.info("queryWithNativeApi took: {} ms", (end - start));

    }

    protected long read(Connection connection, String query, Object[] parameters, boolean shouldPrintResults, boolean shouldPrintExplain){

        if (shouldPrintExplain) {
            logger.info("About to run explain for query: {}", query);
            try {
                String explan = EXPLAIN_PLAN_PREFIX + " " + query;
                PreparedStatement preparedStatement = connection.prepareStatement(explan);
                for (int k = 0; k < parameters.length; k++)
                    preparedStatement.setObject(k + 1, parameters[k]);

                dumpResult(preparedStatement.executeQuery());
                preparedStatement.close();
            } catch (Throwable e) {
                logger.error("Failed to run explain plan", e);
            }
        }
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            for (int k=0; k< parameters.length; k++)
                preparedStatement.setObject(k+1, parameters[k]);
            long start = System.currentTimeMillis();

            ResultSet resultSet = preparedStatement.executeQuery();

            long end = System.currentTimeMillis();
            if (shouldPrintResults) {
                logger.info("Results for query: {}", query);
                dumpResult(resultSet);
            }

            preparedStatement.close();
            return (end-start);
        }
        catch (Throwable t) {
            logger.error("Failed to run query", t);
        }
        return -1;
    }


    protected static Connection getConnection(String spaceName) throws Exception {
        Properties properties = new Properties();
        properties.put("com.gs.embeddedQP.enabled", "true");
        return DriverManager.getConnection("jdbc:gigaspaces:v3://localhost:4174/" + spaceName, properties);

    }

    public List<String> dumpResult(ResultSet resultSet) throws SQLException {
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        List<String> rows = new ArrayList<>();

        StringBuilder header = new StringBuilder();
        for (int k = 1; k <= columnsNumber; k++) {
            if (k > 1) header.append(",  ");
            header.append(rsmd.getColumnName(k));
        }
        logger.info("{}", header);

        while (resultSet.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) row.append(",  ");
                String columnValue = resultSet.getString(i);
                row.append(columnValue);
                rows.add(columnValue);
            }
            logger.info("{}", row);
        }
        return rows;
    }



}
