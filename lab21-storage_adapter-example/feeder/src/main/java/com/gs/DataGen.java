package com.gs;

import com.j_spaces.core.client.SQLQuery;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.SpaceProxyConfigurer;

import java.util.ArrayList;
import java.util.List;

public class DataGen {

    public static void main(String[] args) throws Exception {
        GigaSpace gs = new GigaSpaceConfigurer(new SpaceProxyConfigurer("demo")).
                gigaSpace();
        DataGen dataGenerator = new DataGen();
        //dataGenerator.writeData(gs);
        dataGenerator.readData(gs);

    }

    private void readData(GigaSpace gigaSpace) throws Exception{
            // Query 1: Equality query on indexed nested property



            SQLQuery<OrderDocument> queryByName = new SQLQuery<>(
                    OrderDocument.class,
                    ""/*"orderDataKeyProps.order.customer.firstName = 'John'"*/
            );
            OrderDocument results1 = gigaSpace.read(queryByName);
            System.out.println("Orders for John: " + results1);
            //TODO
            // Query 2: Range query on indexed numeric property
            /*SQLQuery<OrderDocument> queryByTotal = new SQLQuery<>(
                    OrderDocument.class,
                    "orderDataKeyProps.order.total >= 50.0 AND orderData.order.total <= 500.0 ORDER BY orderData.order.total"
            );
            OrderDocument[] results2 = gigaSpace.readMultiple(queryByTotal);
            System.out.println("Orders between 50 and 500: " + results2.length);

            // Query 3: Pattern matching on indexed string property
            SQLQuery<OrderDocument> queryByLastName = new SQLQuery<>(
                    OrderDocument.class,
                    "orderDataKeyProps.order.customer.lastName LIKE '%Doe%'"
            );
            OrderDocument[] results3 = gigaSpace.readMultiple(queryByLastName);
            System.out.println("Orders with Doe in lastname: " + results3.length);

            // Query 4: Combined conditions using indexed properties
            SQLQuery<OrderDocument> queryComplex = new SQLQuery<>(
                    OrderDocument.class,
                    "orderDataKeyProps.order.customer.firstName = 'John' " +
                            "AND orderData.properties.order.total > 100.0 " +
                            "AND orderData.properties.order.status = 'COMPLETED' " +
                            "ORDER BY orderData.properties.order.total DESC"
            );
            OrderDocument[] results4 = gigaSpace.readMultiple(queryComplex);
            System.out.println("Complex query results: " + results4.length);

            // Query 5: NULL checks on indexed properties
            SQLQuery<OrderDocument> queryNullCheck = new SQLQuery<>(
                    OrderDocument.class,
                    "orderDataKeyProps.customer.email IS NOT NULL"
            );
            OrderDocument[] results5 = gigaSpace.readMultiple(queryNullCheck);
            System.out.println("Orders with email: " + results5.length);*/
        }




    public void writeData(GigaSpace gs) {

        ArrayList<OrderDocument> orders = new ArrayList<>(1000);
        for (int i = 1; i <= 1000; i++) {
            OrderDocument order = new OrderDocument();
            order.setId("order-" + String.format("%04d", i));
            order.setCustomerId("cust-" + (i % 100));

            String xmlContent = "<order>" +
                    "<customer>" +
                    "<firstName>Customer" + i + "</firstName>" +
                    "<lastName>Order" + i + "</lastName>" +
                    "<email>customer" + i + "@example.com</email>" +
                    "</customer>" +
                    "<items>" +
                    "<item>Item " + (i * 2) + " - $" + (i * 10.50) + "</item>" +
                    "<item>Item " + (i * 2 + 1) + " - $" + (i * 5.25) + "</item>" +
                    "</items>" +
                    "<total>" + (i * 15.75) + "</total>" +
                    "<status>PENDING</status>" +
                    "</order>";

           // Map<String, Object> orderData = new HashMap<>();
           // orderData.put("xmlContent", xmlContent);
            order.setOrderData(new XmlProperty(xmlContent));

            orders.add(order);
        }
        // TODO
        int batchSize = 100;
        for (int i = 0; i < orders.size(); i += batchSize) {
            List<OrderDocument> batch = orders.subList(i, Math.min(i + batchSize, orders.size()));
            gs.writeMultiple(batch.toArray(new OrderDocument[0]));
        }
    }
}
