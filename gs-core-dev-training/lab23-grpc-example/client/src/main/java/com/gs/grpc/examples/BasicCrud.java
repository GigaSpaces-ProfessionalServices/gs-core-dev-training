package com.gs.grpc.examples;

import com.gigaspaces.rest.grpc.proto.ClearRequest;
import com.gigaspaces.rest.grpc.proto.CountRequest;
import com.gigaspaces.rest.grpc.proto.ReadByIdRequest;
import com.gigaspaces.rest.grpc.proto.ReadByIdResponse;
import com.gigaspaces.rest.grpc.proto.ReadByIdsRequest;
import com.gigaspaces.rest.grpc.proto.ReadByIdsResponse;
import com.gigaspaces.rest.grpc.proto.RegisterTypeRequest;
import com.gigaspaces.rest.grpc.proto.SpaceDocument;
import com.gigaspaces.rest.grpc.proto.SpaceServiceGrpc.SpaceServiceBlockingStub;
import com.gigaspaces.rest.grpc.proto.TakeByIdRequest;
import com.gigaspaces.rest.grpc.proto.TakeByIdResponse;
import com.gigaspaces.rest.grpc.proto.WriteMultipleRequest;
import com.gigaspaces.rest.grpc.proto.WriteRequest;
import com.gs.grpc.common.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import static com.gs.grpc.common.Values.doc;
import static com.gs.grpc.common.Values.format;
import static com.gs.grpc.common.Values.of;

/**
 * Step 1 - the basic operations: register a type, write, read by id, take, count and clear.
 * The client uses nothing but the gRPC stubs generated from the .proto files.
 */
public class BasicCrud {

    static final String PRODUCT = "Product";

    public static void main(String[] args) throws Exception {
        ManagedChannel channel = Grpc.channel();
        try {
            SpaceServiceBlockingStub stub = Grpc.stub(channel);
            String space = Grpc.SPACE;
            System.out.println("gRPC SpaceService at " + Grpc.HOST + ":" + Grpc.PORT + ", space " + space);

            // 1. A gRPC client works with documents, so it registers the type first: property names and
            //    Java type names, the id, indexes. Registering a type that already exists fails, so a
            //    client that runs more than once has to expect that.
            try {
                Grpc.withDeadline(stub).registerType(RegisterTypeRequest.newBuilder()
                        .setSpaceName(space).setTypeName(PRODUCT)
                        .putProperties("productId", "String")
                        .putProperties("name", "String")
                        .putProperties("category", "String")
                        .putProperties("price", "Double")
                        .putProperties("stock", "Integer")
                        .addId("productId")
                        .putIndexes("category", "EQUAL")
                        .build());
                System.out.println("\n1. registerType: registered " + PRODUCT);
            } catch (StatusRuntimeException e) {
                System.out.println("\n1. registerType: " + Grpc.describe(e));
            }
            // Start from an empty type, so the counts below are the same on every run.
            Grpc.withDeadline(stub).clear(ClearRequest.newBuilder().setSpaceName(space)
                    .setTemplate(SpaceDocument.newBuilder().setTypeName(PRODUCT)).build());

            // 2. writeMultiple - one call, five documents.
            WriteMultipleRequest.Builder batch = WriteMultipleRequest.newBuilder().setSpaceName(space);
            String[] categories = {"books", "books", "games", "games", "music"};
            for (int i = 1; i <= 5; i++) {
                batch.addDocuments(product("p" + i, "Product " + i, categories[i - 1], 10.0 * i, 100 + i));
            }
            Grpc.withDeadline(stub).writeMultiple(batch.build());
            System.out.println("\n2. writeMultiple: wrote 5 products, count = " + count(stub));

            // 3. write - one document. A write of an id that exists replaces the entry (insert or update).
            Grpc.withDeadline(stub).write(WriteRequest.newBuilder().setSpaceName(space)
                    .setDocument(product("p1", "Product 1", "books", 12.5, 101)).build());
            System.out.println("\n3. write p1 again with price 12.5: count = " + count(stub) + " (updated, not added)");

            // 4. readById. A miss is not an error: the response simply has no document.
            System.out.println("\n4. readById");
            for (String id : new String[]{"p1", "nope"}) {
                ReadByIdResponse r = Grpc.withDeadline(stub).readById(ReadByIdRequest.newBuilder()
                        .setSpaceName(space).setTypeName(PRODUCT).setId(of(id)).build());
                System.out.println("   " + id + " -> " + (r.hasDocument() ? format(r.getDocument()) : "no document (hasDocument() = false)"));
            }

            // 5. readByIds. Only the entries found come back - no placeholder for a miss, so match the
            //    results to the ids you asked for by their id property, not by position.
            ReadByIdsResponse many = Grpc.withDeadline(stub).readByIds(ReadByIdsRequest.newBuilder()
                    .setSpaceName(space).setTypeName(PRODUCT)
                    .addIds(of("p2")).addIds(of("nope")).addIds(of("p4")).build());
            System.out.println("\n5. readByIds [p2, nope, p4] -> " + many.getDocumentsCount() + " documents");
            many.getDocumentsList().forEach(d -> System.out.println("   " + format(d)));

            // 6. takeById - read and remove.
            TakeByIdResponse taken = Grpc.withDeadline(stub).takeById(TakeByIdRequest.newBuilder()
                    .setSpaceName(space).setTypeName(PRODUCT).setId(of("p5")).build());
            System.out.println("\n6. takeById p5 -> " + format(taken.getDocument()) + ", count = " + count(stub));

            // 7. clear by template: every product whose category is "games".
            Grpc.withDeadline(stub).clear(ClearRequest.newBuilder().setSpaceName(space)
                    .setTemplate(doc(PRODUCT, "category", "games")).build());
            System.out.println("\n7. clear category = games: count = " + count(stub));

            // 8. A failed call throws StatusRuntimeException; the status code is what a client branches on.
            //    Look at which code an unknown type gets - it is not the one you might expect.
            try {
                Grpc.withDeadline(stub).readById(ReadByIdRequest.newBuilder()
                        .setSpaceName(space).setTypeName("NoSuchType").setId(of("x")).build());
            } catch (StatusRuntimeException e) {
                System.out.println("\n8. readById on NoSuchType: " + Grpc.describe(e));
                System.out.println("   status is NOT_FOUND: " + (e.getStatus().getCode() == Status.Code.NOT_FOUND));
            }
        } finally {
            Grpc.close(channel);
        }
    }

    static SpaceDocument product(String id, String name, String category, double price, int stock) {
        return doc(PRODUCT, "productId", id, "name", name, "category", category, "price", price, "stock", stock);
    }

    static int count(SpaceServiceBlockingStub stub) {
        return Grpc.withDeadline(stub).count(CountRequest.newBuilder().setSpaceName(Grpc.SPACE)
                .setTemplate(SpaceDocument.newBuilder().setTypeName(PRODUCT)).build()).getCount();
    }
}
