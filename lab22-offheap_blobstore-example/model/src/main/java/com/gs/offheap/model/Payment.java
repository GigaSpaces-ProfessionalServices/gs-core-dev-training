package com.gs.offheap.model;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

/**
 * A payment: an amount and a currency. None of its properties is indexed, on purpose: every query in
 * this lab has to look at the entries themselves, which is where the off-heap read paths make a
 * difference.
 */
@SpaceClass
public class Payment {

    private Integer id;
    private Double amount;
    private String currency;

    public Payment() {
    }

    public Payment(Integer id, Double amount, String currency) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
    }

    @SpaceId
    @SpaceRouting
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "Payment{id=" + id + ", amount=" + amount + ", currency='" + currency + "'}";
    }
}
