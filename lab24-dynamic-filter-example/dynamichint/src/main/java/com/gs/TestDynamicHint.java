package com.gs;

import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.sql.*;
import java.util.*;

public class TestDynamicHint {
    public static void main(String[] args) throws Exception {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).gigaSpace();
        TestDynamicHint test = new TestDynamicHint();
        test.runWithJdbcQuery(1,gs);
        test.runWithJdbcQuery2(1,gs);

        // test.runWithJDynamicHintQuery(1, gs);
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
        Connection connection= getConnection(gs.getSpaceName());

        PreparedStatement preparedStatement = connection.prepareStatement(queryBuffer.toString());
        long start=System.currentTimeMillis();
        preparedStatement.setInt(1,1);
        ResultSet resultSet = preparedStatement.executeQuery();

        long end=System.currentTimeMillis();
        System.out.println("runWithJdbcQuery got :" +resultSet + " courses, took " + (end-start));
        dumpResult(resultSet);

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
        Connection connection= getConnection(gs.getSpaceName());

        PreparedStatement preparedStatement = connection.prepareStatement(queryBuffer.toString());
        long start=System.currentTimeMillis();
        preparedStatement.setInt(1,1);
        preparedStatement.setInt(2,1);
        ResultSet resultSet = preparedStatement.executeQuery();

        long end=System.currentTimeMillis();
        System.out.println("runWithJdbcQuery2 got :" +resultSet + " courses, took " + (end-start));
        dumpResult(resultSet);


    }

    /*
    Run with dynamic hint - wait till m14
     */
    protected void runWithJDynamicHintQuery(int studentId, GigaSpace gs) throws Exception{
        SQLQuery<Courses> studentCourses = new SQLQuery<Courses>(Courses.class, "id in (?)");
        studentCourses.setParameter(1, getStudentCoursesIds(1, gs));

        StringBuffer queryBuffer = new StringBuffer(6000);
        queryBuffer.append("select /*+ DYNAMIC_FILTER(maxResponseSize='100') */  s.id, s.firstName, s.lastName,sc.sem, c.id, c.name ");
        queryBuffer.append("from (select * from\"com.gs.Student2\" where id=?) as s ");
        queryBuffer.append("join \"com.gs.StudentCourses2\"  as sc ON s.id=sc.studentId ");
        queryBuffer.append("join \"com.gs.Courses2\"  as c ON c.id=sc.courseId ");
        Connection connection= getConnection(gs.getSpaceName());

        PreparedStatement preparedStatement = connection.prepareStatement(queryBuffer.toString());
        long start=System.currentTimeMillis();
        preparedStatement.setInt(1,1);
        ResultSet resultSet = preparedStatement.executeQuery();

        long end=System.currentTimeMillis();
        System.out.println("runWithJDynamicHintQuery got :" +resultSet + " courses, took " + (end-start));

    }

    protected Collection<Object> getStudentCoursesIds(int studentId, GigaSpace gs){
        HashSet<Object> ids = new HashSet<>(DataGen.N_CURSE_PER_STUDENT);
        SQLQuery<StudentCourses> studentCoursesSQLQuery = new SQLQuery<>(StudentCourses.class, "studentId=?");
        studentCoursesSQLQuery.setParameter(1, studentId);
        studentCoursesSQLQuery.setProjections("courseId");
        StudentCourses[] results = gs.readMultiple(studentCoursesSQLQuery);
        Arrays.stream(results).forEach(sc-> ids.add(sc.getCourseId()));
        System.out.println("getStudentCoursesIds: "+ ids.size());
        return ids;
    }

    private void runWith2Queries(GigaSpace gs, int studentId) throws Exception {

        SQLQuery<Courses> studentCourses = new SQLQuery<Courses>(Courses.class, "id in (?)");
        studentCourses.setParameter(1, getStudentCoursesIds(studentId, gs));
        long start=System.currentTimeMillis();
        Courses[] results = gs.readMultiple(studentCourses);
        long end=System.currentTimeMillis();
        System.out.println("runWith2Queries got :" +results.length + " courses, took " + (end-start));

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
