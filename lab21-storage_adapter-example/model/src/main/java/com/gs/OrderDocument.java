package com.gs;

import com.gigaspaces.annotation.pojo.*;
import com.gs.CompressedXmlPropertiesAdapter;

import java.io.Serializable;
import java.util.Map;

@SpaceClass
public class OrderDocument implements Serializable {

    private String id;

    private XmlProperty orderData;

    private Map<String, Object> orderDataKeyProps;

    private String customerId;

    @SpaceId(autoGenerate = false)
    @SpaceRouting
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /*
       doNothing() was a legacy IDE workaround. Older IntelliJ versions flagged imports used only
       in annotation parameters (e.g. CompressedXmlPropertiesAdapter.class above) as "unused" and
       would remove them during code cleanup. A method-body reference prevented that. Modern IntelliJ
       correctly recognizes annotation class references as usages, so the workaround is no longer needed.

    private void doNothing(){
       CompressedXmlPropertiesAdapter CompressedXmlPropertiesAdapter;
    }
     */

    @SpacePropertyStorageAdapter(CompressedXmlPropertiesAdapter.class)
    public XmlProperty getOrderData() {
        return orderData;
    }

    public void setOrderData(XmlProperty orderData) {
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
       Properties can be added and indexed once identified
     */
    public Map<String, Object> getOrderDataKeyProps() {
        return orderDataKeyProps;
    }

    public void setOrderDataKeyProps(Map<String, Object> orderDataKeyProps) {
        this.orderDataKeyProps = orderDataKeyProps;
    }
}