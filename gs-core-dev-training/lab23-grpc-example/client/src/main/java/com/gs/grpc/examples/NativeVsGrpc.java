package com.gs.grpc.examples;

import com.gigaspaces.document.SpaceDocument;
import com.gigaspaces.query.IdQuery;
import com.gigaspaces.rest.grpc.proto.ReadByIdRequest;
import com.gigaspaces.rest.grpc.proto.SpaceServiceGrpc.SpaceServiceBlockingStub;
import com.gigaspaces.rest.grpc.proto.WriteRequest;
import com.gs.grpc.common.Grpc;
import com.gs.grpc.common.NativeSpace;
import io.grpc.ManagedChannel;
import org.HdrHistogram.Histogram;
import org.openspaces.core.GigaSpace;

import java.util.function.IntConsumer;

import static com.gs.grpc.common.Values.of;

/**
 * Step 5 - what the extra hop costs. The same two calls, one thread, through the native client and
 * through gRPC: readById and write (an update of an existing id). A small version of the
 * benchmark summarized in REFERENCE.md, which was run outside this lab; on one machine the network
 * trip is almost free, so this shows the cost of the extra process and the conversions rather than
 * of the network.
 *
 * Run {@link BasicCrud} once before this, to register the Product type. Options:
 * {@code -Dcalls=5000} (measured calls per test), {@code -Dwarmup=3000}.
 */
public class NativeVsGrpc {

    static final int ENTRIES = 1000;
    static final int CALLS = Integer.getInteger("calls", 5000);
    static final int WARMUP = Integer.getInteger("warmup", 3000);

    public static void main(String[] args) throws Exception {
        GigaSpace gigaSpace = NativeSpace.connect();
        ManagedChannel channel = Grpc.channel();
        try {
            SpaceServiceBlockingStub stub = Grpc.stub(channel);
            String space = Grpc.SPACE;
            String type = BasicCrud.PRODUCT;

            gigaSpace.clear(new SpaceDocument(type));
            for (int k = 0; k < ENTRIES; k++) {
                gigaSpace.write(nativeProduct(k));
            }
            System.out.printf("%,d products loaded; %,d warm-up + %,d measured calls per test, 1 thread%n%n",
                    ENTRIES, WARMUP, CALLS);
            System.out.printf("%-10s %-7s %10s %10s %10s %12s%n", "operation", "client", "p50 µs", "p99 µs", "mean µs", "calls/sec");

            compare("readById",
                    k -> gigaSpace.readById(new IdQuery<SpaceDocument>(type, "p" + k)),
                    k -> stub.readById(ReadByIdRequest.newBuilder()
                            .setSpaceName(space).setTypeName(type).setId(of("p" + k)).build()));

            compare("write",
                    k -> gigaSpace.write(nativeProduct(k)),
                    k -> stub.write(WriteRequest.newBuilder().setSpaceName(space)
                            .setDocument(BasicCrud.product("p" + k, "Product " + k, "books", k * 1.5, k)).build()));
        } finally {
            Grpc.close(channel);
            NativeSpace.exit();
        }
    }

    static void compare(String name, IntConsumer nativeCall, IntConsumer grpcCall) {
        Histogram n = measure(nativeCall);
        Histogram g = measure(grpcCall);
        print(name, "native", n);
        print(name, "gRPC", g);
        System.out.printf("%-10s gRPC is %.1fx slower at p50%n%n", "", (double) g.getValueAtPercentile(50) / n.getValueAtPercentile(50));
    }

    static Histogram measure(IntConsumer call) {
        for (int i = 0; i < WARMUP; i++) {
            call.accept(i % ENTRIES);
        }
        Histogram h = new Histogram(3);
        for (int i = 0; i < CALLS; i++) {
            long start = System.nanoTime();
            call.accept(i % ENTRIES);
            h.recordValue((System.nanoTime() - start) / 1000);
        }
        return h;
    }

    static void print(String name, String client, Histogram h) {
        System.out.printf("%-10s %-7s %10d %10d %10.0f %,12.0f%n", name, client,
                h.getValueAtPercentile(50), h.getValueAtPercentile(99), h.getMean(), 1_000_000 / h.getMean());
    }

    static SpaceDocument nativeProduct(int k) {
        return new SpaceDocument(BasicCrud.PRODUCT)
                .setProperty("productId", "p" + k).setProperty("name", "Product " + k)
                .setProperty("category", "books").setProperty("price", k * 1.5).setProperty("stock", k);
    }
}
