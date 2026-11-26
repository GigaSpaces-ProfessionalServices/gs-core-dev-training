package com.gs.grpc.examples;

import com.gigaspaces.rest.grpc.proto.ChangeOperation;
import com.gigaspaces.rest.grpc.proto.ChangeRequest;
import com.gigaspaces.rest.grpc.proto.ReadByIdRequest;
import com.gigaspaces.rest.grpc.proto.SpaceDocument;
import com.gigaspaces.rest.grpc.proto.SpaceServiceGrpc.SpaceServiceBlockingStub;
import com.gigaspaces.rest.grpc.proto.WriteRequest;
import com.gs.grpc.common.Grpc;
import com.gs.grpc.common.NativeSpace;
import com.gs.grpc.model.Address;
import com.gs.grpc.model.Order;
import com.gs.grpc.model.OrderStatus;
import com.gs.grpc.common.Values;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import org.openspaces.core.GigaSpace;

import java.util.Date;

import static com.gs.grpc.common.Values.format;
import static com.gs.grpc.common.Values.of;

/**
 * Step 3 - a Java service and a gRPC client sharing one type. The Java service owns the
 * {@link Order} POJO and writes it with the native client; the gRPC client reads and writes the same
 * entries as documents. Watch what happens to each property type on the way through.
 */
public class PojoInterop {

    /** A gRPC client names a POJO type by its full class name. */
    static final String ORDER = Order.class.getName();

    public static void main(String[] args) throws Exception {
        GigaSpace gigaSpace = NativeSpace.connect();
        ManagedChannel channel = Grpc.channel();
        try {
            SpaceServiceBlockingStub stub = Grpc.stub(channel);
            String space = Grpc.SPACE;

            // The Java service writes two orders. Writing a POJO registers its type - no registerType.
            gigaSpace.clear(new Order());
            gigaSpace.write(new Order("o1", "c1", OrderStatus.PAID, 2, 59.90, new Date(),
                    new Address("Haifa", "IL")));
            gigaSpace.write(new Order("o2", "c2", OrderStatus.NEW, 1, 15.00, new Date(), null));
            System.out.println("Java service wrote o1 (customer c1) and o2 (customer c2)");

            // 1. The same entry seen from the gRPC side.
            SpaceDocument o1 = readOrder(stub, "o1", "c1");
            System.out.println("\n1. gRPC readById o1 -> " + format(o1));
            System.out.println("   status:          enum    -> string (the constant name)");
            System.out.println("   quantity:        Integer -> int64");
            System.out.println("   createdAt:       Date    -> string (ISO-8601, UTC)");
            System.out.println("   shippingAddress: Address -> string (a description of a nested document - not data you can use or send back)");

            // 2. Routing. Order is routed by customerId, not by its id. readById takes an optional
            //    routing value: with it the call goes to one partition, without it to all of them.
            SpaceDocument noRouting = readOrder(stub, "o1", null);
            System.out.println("\n2. readById o1 without routing -> " + (noRouting == null ? "no document" : "found (sent to every partition)"));

            // 3. Writing from gRPC: the server converts each value to the property's Java type -
            //    a string to the enum, an ISO-8601 string to the Date, an int64 to the Integer.
            Grpc.withDeadline(stub).write(WriteRequest.newBuilder().setSpaceName(space)
                    .setDocument(Values.doc(ORDER, "orderId", "o3", "customerId", "c1", "status", "SHIPPED",
                            "quantity", 4, "amount", 120.0, "createdAt", "2026-11-01T10:15:30Z"))
                    .build());
            Order o3 = gigaSpace.readById(Order.class, "o3", "c1");
            System.out.println("\n3. gRPC wrote o3, the Java service reads -> " + o3);
            System.out.println("   status is " + o3.getStatus().getClass().getSimpleName()
                    + ", quantity is " + o3.getQuantity().getClass().getSimpleName()
                    + ", createdAt is " + o3.getCreatedAt().getClass().getSimpleName());

            // 4. A value that does not fit the property type is rejected.
            tryWrite(stub, "4. quantity = 3,000,000,000 (does not fit an Integer)",
                    Values.doc(ORDER, "orderId", "o4", "customerId", "c1", "quantity", 3_000_000_000L));
            tryWrite(stub, "   status = \"LOST\" (not an OrderStatus constant)",
                    Values.doc(ORDER, "orderId", "o4", "customerId", "c1", "status", "LOST"));
            tryWrite(stub, "   shippingAddress = the string read in step 1",
                    o1.toBuilder().setTypeName(ORDER).build());

            // 5. Read-modify-write from gRPC loses data. A write replaces the whole entry, so a
            //    property the gRPC client cannot send back - here the nested Address - is lost.
            SpaceDocument withoutAddress = o1.toBuilder()
                    .removeProperties("shippingAddress")
                    .putProperties("quantity", of(3))
                    .build();
            Grpc.withDeadline(stub).write(WriteRequest.newBuilder().setSpaceName(space).setDocument(withoutAddress).build());
            System.out.println("\n5. gRPC read o1, set quantity 3, wrote it back without the address it cannot represent");
            System.out.println("   Java service reads -> " + gigaSpace.readById(Order.class, "o1", "c1"));

            // 6. The fix: change only what you mean to change. change touches the named paths and
            //    leaves every other property - including ones gRPC cannot represent - as it is.
            gigaSpace.write(new Order("o2", "c2", OrderStatus.NEW, 1, 15.00, new Date(), new Address("Lyon", "FR")));
            Grpc.withDeadline(stub).change(ChangeRequest.newBuilder()
                    .setSpaceName(space).setTypeName(ORDER)
                    .setCriteriaType("id").setCriteria(of("o2")).setCriteriaRouting(of("c2"))
                    .addOperations(ChangeOperation.newBuilder().setOp("set").setPath("quantity").setValue(of(3)))
                    .addOperations(ChangeOperation.newBuilder().setOp("set").setPath("status").setValue(of("PAID")))
                    .build());
            System.out.println("\n6. gRPC change o2: set quantity 3, set status PAID");
            System.out.println("   Java service reads -> " + gigaSpace.readById(Order.class, "o2", "c2"));
        } finally {
            Grpc.close(channel);
            NativeSpace.exit();
        }
    }

    static SpaceDocument readOrder(SpaceServiceBlockingStub stub, String id, String routing) {
        ReadByIdRequest.Builder request = ReadByIdRequest.newBuilder()
                .setSpaceName(Grpc.SPACE).setTypeName(ORDER).setId(of(id));
        if (routing != null) {
            request.setRouting(of(routing));
        }
        var response = Grpc.withDeadline(stub).readById(request.build());
        return response.hasDocument() ? response.getDocument() : null;
    }

    static void tryWrite(SpaceServiceBlockingStub stub, String label, SpaceDocument doc) {
        try {
            Grpc.withDeadline(stub).write(WriteRequest.newBuilder().setSpaceName(Grpc.SPACE).setDocument(doc).build());
            System.out.println((label.startsWith(" ") ? "" : "\n") + label + ": written");
        } catch (StatusRuntimeException e) {
            System.out.println((label.startsWith(" ") ? "" : "\n") + label + ":\n     " + Grpc.describe(e));
        }
    }
}
