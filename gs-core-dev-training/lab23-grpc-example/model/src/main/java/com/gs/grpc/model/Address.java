package com.gs.grpc.model;

import java.io.Serializable;

/** A nested object inside {@link Order}. The gRPC Value type has no kind for it. */
public class Address implements Serializable {
    private static final long serialVersionUID = 1L;

    private String city;
    private String country;

    public Address() {
    }

    public Address(String city, String country) {
        this.city = city;
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return "Address{city=" + city + ", country=" + country + "}";
    }
}
