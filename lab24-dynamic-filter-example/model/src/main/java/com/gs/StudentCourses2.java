package com.gs;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;


@SpaceClass
public class StudentCourses2 {
    private String id;
    private Integer studentId;
    private Integer courseId;
    long sem;


    public StudentCourses2() {
    }

    public StudentCourses2(Integer studentId, Integer courseId, long sem) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.sem = sem;
    }


    public long getSem() {
        return sem;
    }

    public void setSem(long sem) {
        this.sem = sem;
    }

    @SpaceId (autoGenerate = true)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }




    @SpaceIndex
    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }


    @SpaceIndex
    @SpaceRouting
    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }


}
