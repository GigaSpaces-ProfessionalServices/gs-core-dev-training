package com.gs.grpc.model;

import com.gigaspaces.annotation.pojo.SpaceClass;
import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceIndex;
import com.gigaspaces.annotation.pojo.SpaceRouting;
import com.gigaspaces.metadata.index.SpaceIndexType;

import java.util.Date;

/**
 * A space class owned by a Java service. Step 3 reads and writes it from a gRPC client, which sees it
 * as a document named {@code com.gs.grpc.model.Order}. The property types are chosen to show how each
 * one crosses the gRPC boundary: Integer, Double, an enum, a Date and a nested object.
 *
 * The routing property is {@code customerId}, not the id - so a gRPC client that reads by id needs to
 * send the routing value too, or the read goes to every partition.
 */
@SpaceClass
public class Order {

    private String orderId;
    private String customerId;
    private OrderStatus status;
    private Integer quantity;
    private Double amount;
    private Date createdAt;
    private Address shippingAddress;

    public Order() {
    }

    public Order(String orderId, String customerId, OrderStatus status, Integer quantity, Double amount,
                 Date createdAt, Address shippingAddress) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.quantity = quantity;
        this.amount = amount;
        this.createdAt = createdAt;
        this.shippingAddress = shippingAddress;
    }

    @SpaceId
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @SpaceRouting
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @SpaceIndex(type = SpaceIndexType.EQUAL)
    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    @Override
    public String toString() {
        return "Order{orderId=" + orderId + ", customerId=" + customerId + ", status=" + status
                + ", quantity=" + quantity + ", amount=" + amount + ", createdAt=" + createdAt
                + ", shippingAddress=" + shippingAddress + "}";
    }
}
