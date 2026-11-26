package com.gs.grpc.common;

import com.gigaspaces.rest.grpc.proto.SpaceServiceGrpc;
import com.gigaspaces.rest.grpc.proto.SpaceServiceGrpc.SpaceServiceBlockingStub;
import io.grpc.CallCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Opening a channel to the gRPC SpaceService and making a stub - everything a gRPC client needs.
 *
 * <p>Settings, as VM options (all optional):
 * <ul>
 *   <li>{@code -Dgrpc.host} - the host running REST v3 (default {@code localhost})</li>
 *   <li>{@code -Dgrpc.port} - the gRPC port (default {@code 9091}, set on the server by {@code GS_REST_V3_GRPC_PORT})</li>
 *   <li>{@code -Dspace.name} - the space (default {@code grpcDemo})</li>
 *   <li>{@code -Dgrpc.user} / {@code -Dgrpc.password} - only for a secured grid: sent as
 *       {@code authorization: Basic ...} metadata on every call</li>
 * </ul>
 *
 * <p>Note what is not here: no lookup locators, no lookup groups, no GigaSpaces jar. The client knows
 * one host and one port, and never talks to the space itself.
 */
public final class Grpc {

    public static final String HOST = System.getProperty("grpc.host", "localhost");
    public static final int PORT = Integer.getInteger("grpc.port", 9091);
    public static final String SPACE = System.getProperty("space.name", "grpcDemo");

    private Grpc() {
    }

    /**
     * One channel is one HTTP/2 connection, and it is meant to be shared: it is thread-safe, multiplexes
     * calls and reconnects by itself. Create it once per process, not once per call.
     */
    public static ManagedChannel channel() {
        // Plain text. For a server with TLS use TlsChannelCredentials instead.
        return io.grpc.Grpc.newChannelBuilderForAddress(HOST, PORT, InsecureChannelCredentials.create()).build();
    }

    /** A blocking stub on the channel - cheap to create, carries the credentials if any are set. */
    public static SpaceServiceBlockingStub stub(ManagedChannel channel) {
        SpaceServiceBlockingStub stub = SpaceServiceGrpc.newBlockingStub(channel);
        String user = System.getProperty("grpc.user");
        if (user != null) {
            stub = stub.withCallCredentials(basic(user, System.getProperty("grpc.password", "")));
        }
        return stub;
    }

    /** Every call should have a deadline. Without one, a blocking call to a hung server waits forever. */
    public static SpaceServiceBlockingStub withDeadline(SpaceServiceBlockingStub stub) {
        return stub.withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    public static void close(ManagedChannel channel) throws InterruptedException {
        channel.shutdown();
        if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
            channel.shutdownNow();
        }
    }

    /** The status code and description a failed call carries - what a client should branch on. */
    public static String describe(StatusRuntimeException e) {
        String description = e.getStatus().getDescription();
        if (description != null && description.length() > 150) {
            description = description.substring(0, 150) + "...";
        }
        return e.getStatus().getCode() + " - " + description;
    }

    private static CallCredentials basic(String user, String password) {
        String token = Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        Metadata.Key<String> authorization = Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
        return new CallCredentials() {
            @Override
            public void applyRequestMetadata(RequestInfo info, Executor executor, MetadataApplier applier) {
                Metadata headers = new Metadata();
                headers.put(authorization, "Basic " + token);
                applier.apply(headers);
            }
        };
    }
}
