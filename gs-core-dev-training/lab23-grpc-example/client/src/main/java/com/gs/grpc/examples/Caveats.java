package com.gs.grpc.examples;

import com.gigaspaces.rest.grpc.proto.ClearRequest;
import com.gigaspaces.rest.grpc.proto.CountRequest;
import com.gigaspaces.rest.grpc.proto.QueryExpressionRequest;
import com.gigaspaces.rest.grpc.proto.SpaceDocument;
import com.gigaspaces.rest.grpc.proto.SpaceServiceGrpc.SpaceServiceBlockingStub;
import com.gigaspaces.rest.grpc.proto.WriteMultipleRequest;
import com.gs.grpc.common.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.gs.grpc.common.Values.doc;

/**
 * Step 4 - the things that bite: no query parameters, partial batches, deadlines, limits.
 * Run {@link BasicCrud} once before this, to register the Product type.
 */
public class Caveats {

    /** After the refused message in item 7, grpc-java logs one INFO line per late data frame. Held so it is not collected. */
    private static final Logger GRPC_LOG = Logger.getLogger("io.grpc");

    public static void main(String[] args) throws Exception {
        GRPC_LOG.setLevel(Level.WARNING);
        ManagedChannel channel = Grpc.channel();
        try {
            SpaceServiceBlockingStub stub = Grpc.stub(channel);
            String space = Grpc.SPACE;
            String type = BasicCrud.PRODUCT;
            reset(stub);
            System.out.println("Wrote 4 products: p1, p2 (books), p3, p4 (games)");

            // 1. queryExpression takes a string and nothing else - there are no '?' parameters. A client
            //    that pastes user input into the expression lets the user rewrite the query.
            String input = "books' OR category <> 'x";
            System.out.println("\n1. user input for the category: " + input);
            System.out.println("   pasted in:  " + countWhere(stub, "category = '" + input + "'") + " products  <- every category");
            System.out.println("   escaped:    " + countWhere(stub, "category = " + quote(input)) + " products  <- no category has that name");
            System.out.println("   honest use: " + countWhere(stub, "category = " + quote("books")) + " products");

            // 2. writeMultiple with one bad document. The server converts every document before it
            //    writes any, so a conversion error rejects the whole call and names the document's
            //    position. That is validation, not a transaction: there is no transaction over gRPC, and
            //    a failure while the space writes (a partition going down, say) is not rolled back.
            WriteMultipleRequest batch = WriteMultipleRequest.newBuilder().setSpaceName(space)
                    .addDocuments(BasicCrud.product("b1", "Batch 1", "batch", 1.0, 1))
                    .addDocuments(BasicCrud.product("b2", "Batch 2", "batch", 2.0, 2))
                    .addDocuments(doc(type, "productId", "b3", "category", "batch", "stock", "many"))   // stock is an Integer
                    .addDocuments(BasicCrud.product("b4", "Batch 4", "batch", 4.0, 4))
                    .build();
            try {
                Grpc.withDeadline(stub).writeMultiple(batch);
                System.out.println("\n2. writeMultiple [b1, b2, b3 (bad), b4]: written");
            } catch (StatusRuntimeException e) {
                System.out.println("\n2. writeMultiple [b1, b2, b3 (bad), b4]:\n     " + Grpc.describe(e));
            }
            System.out.println("   products in category 'batch' after the failed call: " + countWhere(stub, "category = 'batch'"));

            // 3. A deadline turns a slow call into DEADLINE_EXCEEDED instead of a hung thread.
            //    The server may still finish the call - for a write, the client then does not know.
            try {
                stub.withDeadlineAfter(1, TimeUnit.MILLISECONDS).queryExpression(QueryExpressionRequest.newBuilder()
                        .setSpaceName(space).setTypeName(type).setExpression("price > 0").build());
                System.out.println("\n3. queryExpression with a 1 ms deadline: finished in time");
            } catch (StatusRuntimeException e) {
                System.out.println("\n3. queryExpression with a 1 ms deadline: " + Grpc.describe(e));
            }

            // 4. A negative maxEntries is rejected; remember from step 2 that 0 means unlimited.
            try {
                Grpc.withDeadline(stub).queryExpression(QueryExpressionRequest.newBuilder()
                        .setSpaceName(space).setTypeName(type).setExpression("price > 0").setMaxEntries(-1).build());
                System.out.println("\n4. maxEntries = -1: accepted");
            } catch (StatusRuntimeException e) {
                System.out.println("\n4. maxEntries = -1: " + Grpc.describe(e));
            }

            // 5. The space name travels in every request. A wrong one fails per call, not at connect time:
            //    a channel connects to REST v3, not to a space.
            try {
                Grpc.withDeadline(stub).count(CountRequest.newBuilder().setSpaceName("noSuchSpace")
                        .setTemplate(SpaceDocument.newBuilder().setTypeName(type)).build());
                System.out.println("\n5. count in noSuchSpace: answered");
            } catch (StatusRuntimeException e) {
                System.out.println("\n5. count in noSuchSpace: " + Grpc.describe(e));
            }

            // 6. clear and count without a template act on EVERY type in the space, not on one type.
            int typeOnly = Grpc.withDeadline(stub).count(CountRequest.newBuilder().setSpaceName(space)
                    .setTemplate(SpaceDocument.newBuilder().setTypeName(type)).build()).getCount();
            int wholeSpace = Grpc.withDeadline(stub).count(CountRequest.newBuilder().setSpaceName(space).build()).getCount();
            System.out.println("\n6. count with template {Product}: " + typeOnly + ", count with no template: " + wholeSpace
                    + " (every type - the Orders of step 3 too)");
            System.out.println("   a ClearRequest with no template would remove all " + wholeSpace + " - not run here");

            // 7. A response is one message, and a gRPC client accepts at most 4 MB per message by default.
            //    20,000 products with a 500-character name come to about 12 MB. Unbounded (maxEntries 0),
            //    REST v3 reads all of them into its heap, builds the response - and the client refuses it.
            for (int b = 0; b < 20; b++) {
                WriteMultipleRequest.Builder big = WriteMultipleRequest.newBuilder().setSpaceName(space);
                for (int i = 0; i < 1000; i++) {
                    big.addDocuments(doc(type, "productId", "big" + b + "-" + i, "name", "x".repeat(500), "category", "big"));
                }
                stub.withDeadlineAfter(30, TimeUnit.SECONDS).writeMultiple(big.build());
            }
            System.out.println("\n7. wrote 20,000 products in category 'big', ~600 bytes each");
            for (int max : new int[]{0, 1000}) {
                try {
                    int n = stub.withDeadlineAfter(30, TimeUnit.SECONDS).queryExpression(QueryExpressionRequest.newBuilder()
                            .setSpaceName(space).setTypeName(type).setExpression("category = 'big'")
                            .setMaxEntries(max).build()).getDocumentsCount();
                    System.out.println("   maxEntries = " + max + ": " + n + " documents");
                } catch (StatusRuntimeException e) {
                    System.out.println("   maxEntries = " + max + ": " + Grpc.describe(e));
                }
            }
            stub.withDeadlineAfter(30, TimeUnit.SECONDS).clear(ClearRequest.newBuilder().setSpaceName(space)
                    .setTemplate(doc(type, "category", "big")).build());
        } finally {
            Grpc.close(channel);
        }
    }

    /** A SQL string literal: single quotes around it, a quote inside doubled. */
    static String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    static int countWhere(SpaceServiceBlockingStub stub, String expression) {
        return Grpc.withDeadline(stub).queryExpression(QueryExpressionRequest.newBuilder()
                .setSpaceName(Grpc.SPACE).setTypeName(BasicCrud.PRODUCT)
                .setExpression(expression).setMaxEntries(1000).build()).getDocumentsCount();
    }

    static void reset(SpaceServiceBlockingStub stub) {
        Grpc.withDeadline(stub).clear(ClearRequest.newBuilder().setSpaceName(Grpc.SPACE)
                .setTemplate(SpaceDocument.newBuilder().setTypeName(BasicCrud.PRODUCT)).build());
        WriteMultipleRequest.Builder batch = WriteMultipleRequest.newBuilder().setSpaceName(Grpc.SPACE);
        batch.addDocuments(BasicCrud.product("p1", "Product 1", "books", 10.0, 100));
        batch.addDocuments(BasicCrud.product("p2", "Product 2", "books", 20.0, 100));
        batch.addDocuments(BasicCrud.product("p3", "Product 3", "games", 30.0, 100));
        batch.addDocuments(BasicCrud.product("p4", "Product 4", "games", 40.0, 100));
        Grpc.withDeadline(stub).writeMultiple(batch.build());
    }
}
