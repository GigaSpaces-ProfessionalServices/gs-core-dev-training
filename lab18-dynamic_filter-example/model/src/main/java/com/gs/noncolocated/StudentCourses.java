package com.gs.noncolocated;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;

import java.io.Serializable;

@SpaceClass
public class StudentCourses implements Serializable {
    private String id;
    private Integer studentId;
    private Integer courseId;
    long sem;


    public StudentCourses() {
    }

    public StudentCourses( Integer studentId, Integer courseId, long sem) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.sem = sem;
    }

    public StudentCourses( Integer studentId, Integer courseId) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.sem = 1;
    }



    public long getSem() {
        return sem;
    }

    public void setSem(long sem) {
        this.sem = sem;
    }

    // Routing defaults to @SpaceId (auto-generated), distributing entries randomly across partitions.
    // This is intentional — the non-colocated model demonstrates the cost of joining types
    // whose routing keys do not align, and the benefit of the DYNAMIC_FILTER hint in that scenario.
    @SpaceId(autoGenerate = true)
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
    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }


}
