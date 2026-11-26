package com.gs.grpc.examples;

import com.gigaspaces.rest.grpc.proto.ChangeOperation;
import com.gigaspaces.rest.grpc.proto.ChangeRequest;
import com.gigaspaces.rest.grpc.proto.ChangeResponse;
import com.gigaspaces.rest.grpc.proto.ClearRequest;
import com.gigaspaces.rest.grpc.proto.QueryExpressionRequest;
import com.gigaspaces.rest.grpc.proto.QueryExpressionResponse;
import com.gigaspaces.rest.grpc.proto.ReadByIdRequest;
import com.gigaspaces.rest.grpc.proto.ReadMultipleRequest;
import com.gigaspaces.rest.grpc.proto.ReadMultipleResponse;
import com.gigaspaces.rest.grpc.proto.SpaceDocument;
import com.gigaspaces.rest.grpc.proto.SpaceServiceGrpc.SpaceServiceBlockingStub;
import com.gigaspaces.rest.grpc.proto.TakeMultipleRequest;
import com.gigaspaces.rest.grpc.proto.TakeMultipleResponse;
import com.gigaspaces.rest.grpc.proto.WriteMultipleRequest;
import com.gs.grpc.common.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

import static com.gs.grpc.common.Values.doc;
import static com.gs.grpc.common.Values.format;
import static com.gs.grpc.common.Values.of;

/**
 * Step 2 - finding entries: by template ({@code readMultiple}), by SQL WHERE clause
 * ({@code queryExpression}), and changing them in place ({@code change}) without reading them first.
 * Run {@link BasicCrud} once before this, to register the Product type.
 */
public class QueriesAndChange {

    public static void main(String[] args) throws Exception {
        ManagedChannel channel = Grpc.channel();
        try {
            SpaceServiceBlockingStub stub = Grpc.stub(channel);
            String space = Grpc.SPACE;
            String type = BasicCrud.PRODUCT;

            // Ten products: categories alternate books/games, price 10..100, stock 100.
            Grpc.withDeadline(stub).clear(ClearRequest.newBuilder().setSpaceName(space)
                    .setTemplate(SpaceDocument.newBuilder().setTypeName(type)).build());
            WriteMultipleRequest.Builder batch = WriteMultipleRequest.newBuilder().setSpaceName(space);
            for (int i = 1; i <= 10; i++) {
                batch.addDocuments(BasicCrud.product("p" + i, "Product " + i, i % 2 == 0 ? "games" : "books", 10.0 * i, 100));
            }
            Grpc.withDeadline(stub).writeMultiple(batch.build());
            System.out.println("Wrote 10 products (books: p1,p3,p5,p7,p9 - games: p2,p4,p6,p8,p10)");

            // 1. readMultiple by template: the properties that are set must match exactly, the rest are
            //    wildcards. Equality only - no ranges, no OR.
            ReadMultipleResponse books = Grpc.withDeadline(stub).readMultiple(ReadMultipleRequest.newBuilder()
                    .setSpaceName(space).setTemplate(doc(type, "category", "books")).setMaxEntries(100).build());
            System.out.println("\n1. readMultiple template {category=books} -> " + books.getDocumentsCount() + " documents");

            // 2. queryExpression: a SQL WHERE clause, as with SQLQuery. Ranges, AND/OR, IN, LIKE...
            QueryExpressionResponse q = Grpc.withDeadline(stub).queryExpression(QueryExpressionRequest.newBuilder()
                    .setSpaceName(space).setTypeName(type)
                    .setExpression("category = 'games' AND price >= 60").setMaxEntries(100).build());
            System.out.println("\n2. queryExpression \"category = 'games' AND price >= 60\" -> " + q.getDocumentsCount() + " documents");
            q.getDocumentsList().forEach(d -> System.out.println("   " + format(d)));

            // 3. maxEntries limits the result. 0 (the protobuf default when you forget to set it) means
            //    NO limit - the whole type comes back in one response message.
            int limited = Grpc.withDeadline(stub).queryExpression(QueryExpressionRequest.newBuilder()
                    .setSpaceName(space).setTypeName(type).setExpression("price > 0").setMaxEntries(3).build()).getDocumentsCount();
            int unset = Grpc.withDeadline(stub).queryExpression(QueryExpressionRequest.newBuilder()
                    .setSpaceName(space).setTypeName(type).setExpression("price > 0").build()).getDocumentsCount();
            System.out.println("\n3. queryExpression \"price > 0\": maxEntries=3 -> " + limited + ", maxEntries not set -> " + unset);

            // 4. change by id: the space applies the operations to the entry - no read-modify-write
            //    round trip, no lost update between two clients.
            ChangeResponse byId = Grpc.withDeadline(stub).change(ChangeRequest.newBuilder()
                    .setSpaceName(space).setTypeName(type)
                    .setCriteriaType("id").setCriteria(of("p1"))
                    .addOperations(ChangeOperation.newBuilder().setOp("increment").setPath("stock").setValue(of(5)))
                    .addOperations(ChangeOperation.newBuilder().setOp("set").setPath("price").setValue(of(9.99)))
                    .build());
            System.out.println("\n4. change id p1: increment stock by 5, set price 9.99 -> changed " + byId.getNumberOfChangedEntries());
            System.out.println("   " + format(Grpc.withDeadline(stub).readById(ReadByIdRequest.newBuilder()
                    .setSpaceName(space).setTypeName(type).setId(of("p1")).build()).getDocument()));

            // 5. change by SQL: every entry that matches.
            ChangeResponse bySql = Grpc.withDeadline(stub).change(ChangeRequest.newBuilder()
                    .setSpaceName(space).setTypeName(type)
                    .setCriteriaType("sql").setCriteria(of("category = 'games'"))
                    .addOperations(ChangeOperation.newBuilder().setOp("decrement").setPath("stock").setValue(of(1)))
                    .build());
            System.out.println("\n5. change sql \"category = 'games'\": decrement stock by 1 -> changed " + bySql.getNumberOfChangedEntries());

            // 6. A change that matches nothing is not an error.
            ChangeResponse none = Grpc.withDeadline(stub).change(ChangeRequest.newBuilder()
                    .setSpaceName(space).setTypeName(type)
                    .setCriteriaType("id").setCriteria(of("nope"))
                    .addOperations(ChangeOperation.newBuilder().setOp("increment").setPath("stock").setValue(of(1)))
                    .build());
            System.out.println("\n6. change id nope -> changed " + none.getNumberOfChangedEntries());

            // 7. takeMultiple by template: read and remove, up to maxEntries.
            TakeMultipleResponse taken = Grpc.withDeadline(stub).takeMultiple(TakeMultipleRequest.newBuilder()
                    .setSpaceName(space).setTemplate(doc(type, "category", "books")).setMaxEntries(2).build());
            System.out.println("\n7. takeMultiple template {category=books}, maxEntries=2 -> took " + taken.getDocumentsCount());

            // 8. A bad expression is reported, not silently ignored.
            try {
                Grpc.withDeadline(stub).queryExpression(QueryExpressionRequest.newBuilder()
                        .setSpaceName(space).setTypeName(type).setExpression("price >>> 1").build());
            } catch (StatusRuntimeException e) {
                System.out.println("\n8. queryExpression \"price >>> 1\": " + Grpc.describe(e));
            }
        } finally {
            Grpc.close(channel);
        }
    }
}
