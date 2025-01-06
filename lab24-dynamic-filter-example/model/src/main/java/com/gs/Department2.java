package com.gs;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;

@SpaceClass(broadcast = true)
public class Department2 {
    Integer id;
    String name;

    public Department2(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Department2() {
    }

    @SpaceId
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
