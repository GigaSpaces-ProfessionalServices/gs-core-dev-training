# lab23-grpc-example - Reference

Companion to the [lab README](README.md). Read it after the lab, or when designing a real gRPC client.

- [Client comparison](#client-comparison)
- [The .proto files](#the-proto-files)
- [Value mapping](#value-mapping)
- [Error codes](#error-codes)
- [Benchmark](#benchmark) (including [NativeVsGrpc](#measure-it-yourself-nativevsgrpc))
- [Anti-patterns](#anti-patterns)
- [Security](#security)
- [Operations](#operations)

## Client comparison

| | Native `GigaSpace` | JDBC v3 | REST v3 HTTP/JSON | **gRPC SpaceService** |
|---|---|---|---|---|
| Client language | Java | Java, plus BI tools through the JDBC/ODBC gateway | any | any language with gRPC |
| Path to the data | direct to the partitions | direct to the partitions | through REST v3 | through REST v3 |
| GigaSpaces jars on the client | yes | yes | no | no |
| Contract | your Java classes | SQL tables | OpenAPI (Swagger) | `.proto` - typed stubs generated per language |
| Queries | ids, templates, `SQLQuery` with parameters, projections, aggregations, iterators | full SQL: JOIN, GROUP BY, ORDER BY, `EXPLAIN` | | ids, templates, a WHERE clause - **no parameters, no projections** |
| Transactions, event containers, notifications, remoting, colocated tasks | yes | | | **no** (executes an already-registered durable task, nothing more) |
| Values | any Java type | SQL types | JSON | string, int64, double, bool |
| Speed | fastest | | 2 hops | 2 hops - **1.2x to 4.8x slower than native**, 2.2x on average |

**gRPC or REST v3 HTTP?** Both go through the same REST v3 process. gRPC gives you a binary encoding, one long-lived HTTP/2 connection for many concurrent calls, per-call deadlines and generated typed clients. REST v3 HTTP needs nothing but `curl` and has Swagger UI. For a program, gRPC; for a person or a script, HTTP.

## The .proto files
Downloaded from the server with `curl -O http://localhost:9090/api/v3/grpc/proto.zip`.

| file | content |
|---|---|
| `space/common.proto` | `Value` (string / int64 / double / bool) and `SpaceDocument` (a type name and a map of values) |
| `space/space_crud.proto` | write, writeMultiple, readById(s), readMultiple, takeById(s), takeMultiple, clear, count |
| `space/space_admin.proto` | registerType, change, queryExpression |
| `space/space_tasks.proto` | executeDurableTask |
| `space/space_service.proto` | the `SpaceService` with its 14 calls |

They are versioned with the server, not with your client: download them again from each new GigaSpaces version and regenerate. The server also has gRPC reflection switched on, so tools such as `grpcurl` and Postman can list the calls without the files.

`change` supports `set`, `unset`, `increment`, `decrement`, `addToCollection`, `addAllToCollection`, `removeFromCollection`, `putInMap`, `removeFromMap`.

## Value mapping
Going out, the server maps each Java value to one of the four kinds; coming in, it converts the value back to the property's declared type.

| Java property type | gRPC client receives | gRPC client sends | |
|---|---|---|---|
| `String` | string | string | |
| `int`/`Integer`, `long`/`Long`, `short`, `byte` | int64 | int64 | range-checked: out of range is `INVALID_ARGUMENT` |
| `double`/`Double`, `float`/`Float` | double | double | |
| `boolean`/`Boolean` | bool | bool | |
| enum | string - the constant name | string - the constant name | an unknown name is `INVALID_ARGUMENT` |
| `java.util.Date`, `java.sql.*` dates | string - ISO-8601 in UTC | ISO-8601 string | |
| `null` | value not set | value not set | |
| **anything else** - nested objects, collections, maps, `byte[]`, `BigDecimal` | **a string description** | **cannot** | read-only text |

**Routing:** `readById`, `takeById` and `change` by id take an optional routing value; without it the call goes to every partition. `readByIds`/`takeByIds` take one routing value for all ids - per-id routing is not supported.

## Error codes

| status | when |
|---|---|
| `INVALID_ARGUMENT` | a value that does not convert or fit; a type registered twice; negative `maxEntries` |
| `NOT_FOUND` | unknown space |
| `INTERNAL` | unknown type; bad SQL expression; anything else unmapped |
| `DEADLINE_EXCEEDED` | the client deadline passed (raised by the client) |
| `RESOURCE_EXHAUSTED` | a response larger than the client's message limit (raised by the client) |
| `UNAVAILABLE` | REST v3 is down or unreachable - stop REST v3 and run any step to see it |
| `PERMISSION_DENIED` | a security denial (not shown in the lab) |
| `FAILED_PRECONDITION` | a protective-mode rejection (not shown in the lab) |

**Message size:** the 4 MB default limit applies in grpc-java, Python and most other implementations. A query response is one message: an unbounded query that matches too much fails on the client only after REST v3 has read every match into its own heap (512 MB by default) and built the response. Raise `maxInboundMessageSize` on the channel only when you know the size; otherwise set `maxEntries` and page by key.

**Unparameterized queries:** besides the injection risk, each distinct expression string is parsed on its own, so the space cannot reuse a parsed query the way it does for a parameterized `SQLQuery`.

## Benchmark
**A benchmark run outside this lab** (its code is not included): two Linux machines (8-core server, 2-core client, 0.2 ms ping), 500,000 entries, 13 operations, 1 and 50 threads, one gRPC channel per thread, 5 minutes per run, no TLS - the best case for gRPC.

- gRPC was slower in **every one of 104 runs: 1.2x to 4.8x** fewer operations per second than native, **2.2x on average**.
- **One thread** - pure cost per call: 2.4x on average. A single-entry call adds about **110-165 µs** at p50.
- **50 threads**: 1.9x on average. With many calls in flight the network waits overlap; the CPU work per call remains.
- Batches narrow the gap (`writeMultiple` of 50: 1.3x at 50 threads): one hop is paid for many entries.

**What follows for design:**
- Use batch calls (`writeMultiple`, `readByIds`) - the hop is paid per call, not per entry.
- Share one channel; concurrent calls multiplex over it. Under heavy load from one client process a few channels may help.
- The REST v3 process is now in your data path. Size its heap and CPU for the gRPC load (`GS_RESTV3_OPTIONS`), and monitor it like any other service.
- A gRPC client knows one `host:port`. REST v3 runs where an agent was started with `--restv3`; for availability, run it on more than one manager host and put a load balancer, or gRPC client-side load balancing, in front.

### Measure it yourself: `NativeVsGrpc`
Step 5 of the lab, and the part of this section you can run; run it after `BasicCrud`, with the lab's grid. It times the same two calls, `readById` and `write`, through the native client and through gRPC on one thread:

```java
compare("readById",
        k -> gigaSpace.readById(new IdQuery<SpaceDocument>(type, "p" + k)),
        k -> stub.readById(ReadByIdRequest.newBuilder()
                .setSpaceName(space).setTypeName(type).setId(of("p" + k)).build()));
```

```
operation  client      p50 µs     p99 µs    mean µs    calls/sec
readById   native         207        452        230        4,352
readById   gRPC           832       1350        847        1,181
           gRPC is 4.0x slower at p50
write      native         129        303        145        6,916
write      gRPC           569        974        586        1,708
           gRPC is 4.4x slower at p50
```

Your numbers will differ, so run it two or three times. On one machine the network is almost free, so this measures the extra process and the conversions rather than the network. Options: `-Dcalls=5000` (measured calls per test), `-Dwarmup=3000`.

## Anti-patterns

| ❌ Don't | ✅ Do |
|---|---|
| Paste user input into `queryExpression` or a SQL `change` | Quote values (`'` doubled), or allow only known values |
| Leave `maxEntries` unset (0 = unlimited) | Set it on every query; page by key for large results |
| Read a document, change a field, `write` it back | `change` the fields you mean to change |
| Write back a property you received as a description string (nested object, collection, `BigDecimal`) | Keep such properties out of what the gRPC side writes, or model the type flat |
| A channel per call | One channel per process, shared by all threads |
| Calls with no deadline | `withDeadlineAfter(...)` on every call |
| Match `readByIds` results to the request by position | Match by the id property - misses are dropped |
| `ClearRequest`/`CountRequest` with no template | Always set the template's type name |
| Treat `INTERNAL` as "server down, retry" | `INTERNAL` is also an unknown type or bad SQL; retry on `UNAVAILABLE` |
| Retry a timed-out `change` with `increment` | Retry only operations that are safe to repeat |
| gRPC from a latency-critical Java service | The native `GigaSpace` client |
| Keep `.proto` files from an older server | Download `proto.zip` from each new GigaSpaces version and regenerate |

## Security
Read from the 18.0.1 server; not exercised in the lab, which runs an unsecured grid.

- On a secured grid (`com.gs.security.enabled=true`) every call must carry `authorization` metadata: `Basic <base64(user:password)>` or `Bearer <token>`. `common.Grpc` sends Basic credentials when you pass `-Dgrpc.user` and `-Dgrpc.password`. The standard gRPC health check (`grpc.health.v1.Health`) needs none.
- The user's grid privileges apply to each operation as for any other client. `registerType` also needs `PROVISION_PU`. A denial is `PERMISSION_DENIED`. Calls are audited.
- The lab uses plaintext. Basic credentials over plaintext can be read off the network: use TLS outside a lab. The client side is `TlsChannelCredentials` in place of `InsecureChannelCredentials`.

## Operations
| | |
|---|---|
| gRPC port | 9091; set with `GS_REST_V3_GRPC_PORT` (the HTTP port is `GS_REST_V3_PORT`, 9090) |
| Contract | `http://<host>:9090/api/v3/grpc/proto.zip` |
| Service name | `com.gigaspaces.rest.grpc.SpaceService` |
| Reflection | on - `grpcurl`, Postman and similar tools can list and call the service without the `.proto` files (e.g. `grpcurl -plaintext localhost:9091 list`) |
| Health | the standard `grpc.health.v1.Health` service; `Check` answers `SERVING` |
| Tracing | each call is traced as a span (`xap-grpc`), with the gRPC status code and duration |
| JVM | REST v3 needs JDK 25; JVM options in `GS_RESTV3_OPTIONS` |
