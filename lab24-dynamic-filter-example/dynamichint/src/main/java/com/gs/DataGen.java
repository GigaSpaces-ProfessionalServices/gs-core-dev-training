package com.gs;


import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.openspaces.extensions.QueryExtension.sum;


public class DataGen {
    final static int N_STUDENTS=100;
    final static int N_CURSES= 100000;
    final static int N_CURSE_PER_STUDENT = 50;
    final static int BATCH_SIZE = 5000;

    public static void main(String[] args) throws SQLException {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).gigaSpace();
        DataGen dataGenerator = new DataGen();
        dataGenerator.writeData(gs);
    }


    public void writeData(GigaSpace gs){
        ArrayList<Department> departments = new ArrayList<>(4);
        departments.add(new Department(1, "law"));
        departments.add(new Department(2, "accounting"));
        departments.add(new Department(3, "science"));
        departments.add(new Department(4, "art"));
        departments.forEach(d->gs.write(d));
        ArrayList<Courses> courses = new ArrayList<>(BATCH_SIZE);
        for (int k =1; k <= N_CURSES; k++) {
            courses.add(new Courses(k, "Course_" + k, k + 1500.0, 1 + k % 4));
            if (k % BATCH_SIZE == 0){
              gs.writeMultiple(courses.toArray());
              courses = new ArrayList<>(BATCH_SIZE);
            }
        }
        if (courses.size() > 0)  gs.writeMultiple(courses.toArray());
        ArrayList<Student> students = new ArrayList<>(N_STUDENTS);
        for (int k =1; k <= N_STUDENTS; k++)
            students.add(new  Student("Cohen_"+k,"Avi_"+k, Date.valueOf("1980-02-01"),k));
        students.forEach(c-> gs.write(c));
        ArrayList<StudentCourses> studentCourses = new ArrayList<>(N_STUDENTS*N_CURSE_PER_STUDENT);
        for (int k=1; k<= N_STUDENTS; k++){
            for (int c=1; c<=N_CURSE_PER_STUDENT; c++){
                studentCourses.add(new StudentCourses(k,c, 202501 + k%2));
            }
        }
        gs.writeMultiple(studentCourses.toArray());
    }


}
