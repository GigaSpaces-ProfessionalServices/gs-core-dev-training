package com.gs;

import com.gigaspaces.query.aggregators.AggregationResult;
import com.gigaspaces.query.aggregators.AggregationSet;
import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.sql.*;
import java.util.*;

public class TestInAggrgator {
    public static void main(String[] args) throws Exception {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).gigaSpace();
        TestInAggrgator test = new TestInAggrgator();
        test.runAggrgator(gs);
        test.runWithoutAggrgator(gs);
    }

    private void runWithoutAggrgator(GigaSpace gs) throws Exception {

        SQLQuery<Courses> studentCourses = new SQLQuery<Courses>(Courses.class, "id in (?)");
        studentCourses.setParameter(1, getStudentCoursesIds(1, gs));
        long start=System.currentTimeMillis();
        Courses[] results = gs.readMultiple(studentCourses);
        long end=System.currentTimeMillis();
        System.out.println("Run using query condition got :" +results.length + " courses, took " + (end-start));

    }

    private void runAggrgator(GigaSpace gs) throws Exception{
        SQLQuery<Courses> studentCoursesQuery = new SQLQuery<Courses>(Courses.class, "");
        CustomINAggregator customINAggregator  = new CustomINAggregator("id", getStudentCoursesIds(1, gs));
        AggregationSet aggregationSet = new AggregationSet();
        aggregationSet.add(customINAggregator);
        long start=System.currentTimeMillis();
        AggregationResult result = gs.aggregate(studentCoursesQuery, aggregationSet);
        long end=System.currentTimeMillis();

        ArrayList<Object> results = (ArrayList<Object>) result.get(0);

        System.out.println("Run using custom IN Aggrgator got :" +results.size() + " courses, took " + (end-start));

    }

    private Collection<Object> getStudentCoursesIds(int studentId, GigaSpace gs){
        HashSet<Object> ids = new HashSet<>(DataGen.N_CURSE_PER_STUDENT);
        SQLQuery<StudentCourses> studentCoursesSQLQuery = new SQLQuery<>(StudentCourses.class, "studentId=?");
        studentCoursesSQLQuery.setParameter(1, studentId);
        studentCoursesSQLQuery.setProjections("courseId");

        StudentCourses[] results = gs.readMultiple(studentCoursesSQLQuery);
        Arrays.stream(results).forEach(sc-> ids.add(sc.getCourseId()));
        System.out.println("getStudentCoursesIds: "+ ids.size());
        return ids;
    }





}
