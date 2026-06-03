package com.gs;

import com.gigaspaces.annotation.pojo.*;
import com.gigaspaces.metadata.index.SpaceIndexType;
import com.gs.CompressedXmlPropertiesAdapter;

import java.util.Map;

@SpaceClass
public  class OrderDocument {

    private String id;

    private XMLProperty orderData;

    private Map<String, Object> orderDataKeyProps;

    private String customerId;

    @SpaceId
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private void doNothing(){
        CompressedXmlPropertiesAdapter CompressedXmlPropertiesAdapter;
    }

    @SpacePropertyStorageAdapter(CompressedXmlPropertiesAdapter.class)
    public XMLProperty getOrderData() {
        return orderData;
    }

    public void setOrderData(XMLProperty orderData) {
        this.orderData = orderData;
    }

    @SpaceIndex
    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /*
    Those properties can be added and indexed once identified
     */
    public Map<String, Object> getOrderDataKeyProps() {
        return orderDataKeyProps;
    }

    public void setOrderDataKeyProps(Map<String, Object> orderDataKeyProps) {
        this.orderDataKeyProps = orderDataKeyProps;
    }
}