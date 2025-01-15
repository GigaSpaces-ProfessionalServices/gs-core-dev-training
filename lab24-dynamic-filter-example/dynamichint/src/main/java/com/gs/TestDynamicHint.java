package com.gs;

import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.sql.*;
import java.util.*;

public class TestDynamicHint {
    private final String EXPLAIN_PLAN_PREFIX = "EXPLAIN ANALYZE FOR ";
    public static void main(String[] args) throws Exception {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).gigaSpace();
        TestDynamicHint test = new TestDynamicHint();
        test.runWithJdbcQuery(1,gs);
        test.runWithJdbcQuery2(1,gs);

        test.runWithJDynamicHintQuery(1, gs);
        test.runWith2Queries(gs,1);
    }


    /*
    Run with no Hint no routing condition
     */
    protected void runWithJdbcQuery(int studentId, GigaSpace gs) throws Exception{
        SQLQuery<Courses> studentCourses = new SQLQuery<Courses>(Courses.class, "id in (?)");
        studentCourses.setParameter(1, getStudentCoursesIds(1, gs));

        StringBuffer queryBuffer = new StringBuffer(6000);
        queryBuffer.append("select s.id, s.firsName, s.lastName,sc.sem, c.id, c.name ");
        queryBuffer.append("from (select * from\"com.gs.Student2\" where id=?) as s ");
        queryBuffer.append("join \"com.gs.StudentCourses2\"  as sc ON s.id=sc.studentId ");
        queryBuffer.append("join \"com.gs.Courses2\"  as c ON c.id=sc.courseId ");


        Object[] params = new Object[1]; params[0]=studentId;
        long time = read(getConnection(gs.getSpaceName()), queryBuffer.toString(), params,false, true);
        System.out.println("runWithJdbcQuery No Hint No Routing took: " + time);

    }

    /*
   Run with no Hint but with proper routing abd with explicit condition on studentid
    */
    protected void runWithJdbcQuery2(int studentId, GigaSpace gs) throws Exception{
        SQLQuery<Courses> studentCourses = new SQLQuery<Courses>(Courses.class, "id in (?)");
        studentCourses.setParameter(1, getStudentCoursesIds(1, gs));

        StringBuffer queryBuffer = new StringBuffer(6000);
        queryBuffer.append("select s.id, s.firsName, s.lastName,sc.sem, c.id, c.name ");
        queryBuffer.append("from (select * from\"com.gs.Student2\" where id=?) as s ");
        queryBuffer.append("join (select * from \"com.gs.StudentCourses2\" where studentId=?) as sc ON s.id=sc.studentId ");
        queryBuffer.append("join \"com.gs.Courses2\"  as c ON c.id=sc.courseId ");

        Object[] params = new Object[2]; params[0]=studentId; params[1]=studentId;

        long time = read(getConnection(gs.getSpaceName()), queryBuffer.toString(), params,false, true);
        System.out.println("runWithJdbcQuery2 - No Hint But Routing conditions took: " + time);

    }



    /*
    Run with dynamic hint - wait till m14
     */
    protected void runWithJDynamicHintQuery(int studentId, GigaSpace gs) throws Exception{
        SQLQuery<Courses> studentCourses = new SQLQuery<Courses>(Courses.class, "id in (?)");
        studentCourses.setParameter(1, getStudentCoursesIds(1, gs));

        StringBuffer queryBuffer = new StringBuffer(6000);
        queryBuffer.append("select /*+ DYNAMIC_FILTER*/  s.id, s.firsName, s.lastName,sc.sem, c.id, c.name ");
        queryBuffer.append("from (select * from\"com.gs.Student\" where id=?) as s ");
        queryBuffer.append("join \"com.gs.StudentCourses\"  as sc ON s.id=sc.studentId ");
        queryBuffer.append("join \"com.gs.Courses\"  as c ON c.id=sc.courseId ");

        Object[] params = new Object[1]; params[0]=studentId;
        long time = read(getConnection(gs.getSpaceName()), queryBuffer.toString(), params,false,true);
        System.out.println("runWithJDynamicHintQuery - Dynamic hint ,No Routing condition took :"  + time);

    }

    protected Collection<Object> getStudentCoursesIds(int studentId, GigaSpace gs){
        HashSet<Object> ids = new HashSet<>(DataGen.N_CURSE_PER_STUDENT);
        SQLQuery<StudentCourses> studentCoursesSQLQuery = new SQLQuery<>(StudentCourses.class, "studentId=?");
        studentCoursesSQLQuery.setParameter(1, studentId);
        studentCoursesSQLQuery.setProjections("courseId");
        StudentCourses[] results = gs.readMultiple(studentCoursesSQLQuery);
        Arrays.stream(results).forEach(sc-> ids.add(sc.getCourseId()));
        return ids;
    }

    private void runWith2Queries(GigaSpace gs, int studentId) throws Exception {

        SQLQuery<Courses> studentCourses = new SQLQuery<Courses>(Courses.class, "id in (?)");
        long start=System.currentTimeMillis();
        studentCourses.setParameter(1, getStudentCoursesIds(studentId, gs));
        // We actually need data regarding courses the student take and student details
        Courses[] results = gs.readMultiple(studentCourses);
        Student template = new Student();
        template.setId(studentId);
        Student student = gs.read(template);
        long end=System.currentTimeMillis();
        System.out.println("runWith2Queries GS API break query took: " + (end-start));

    }

    protected long read(Connection connection, String query, Object[] parameters, boolean shouldPrintResults, boolean shouldPrintExplain){

        if (shouldPrintExplain) {
            System.out.println("");
            System.out.println("About to run explain for Query : " + query);
            try {
                String explan = EXPLAIN_PLAN_PREFIX + " " + query;
                PreparedStatement preparedStatement = connection.prepareStatement(explan);
                for (int k = 0; k < parameters.length; k++)
                    preparedStatement.setObject(k + 1, parameters[k]);

                dumpResult(preparedStatement.executeQuery());
                System.out.println("");
                preparedStatement.close();
            } catch (Throwable e) {
                System.out.println("Fail to run explain plan:" + e);
                e.printStackTrace();
            }
        }
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            for (int k=0; k< parameters.length; k++)
                preparedStatement.setObject(k+1, parameters[k]);
            long start = System.currentTimeMillis();

            ResultSet resultSet = preparedStatement.executeQuery();

            long end = System.currentTimeMillis();
            if (shouldPrintResults)  {
                System.out.println("Results for query: " + query);
                dumpResult(resultSet);
            }

            preparedStatement.close();
            return (end-start);
        }
        catch (Throwable t){

            System.out.println("Fail to run query:" + t);
            t.printStackTrace();
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
        for (int k=1; k<= columnsNumber; k++) {
            if (k > 1) System.out.print(",  ");
            System.out.print(rsmd.getColumnName(k));
        }

        System.out.println();
        while (resultSet.next()) {
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) System.out.print(",  ");
                String columnValue = resultSet.getString(i);
                System.out.print(columnValue);
                rows.add(columnValue);
            }
            System.out.println();
        }
        return rows;
    }



}
