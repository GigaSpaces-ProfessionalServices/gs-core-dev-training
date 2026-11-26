# gs-core-dev-training - lab23-grpc-example

## gRPC SpaceService: the space from any language
From 18.0.1 the REST v3 process hosts a **gRPC server** (port 9091) next to its HTTP server (port 9090). Its one service, `SpaceService`, offers the most frequently used space operations - write, read, take, count, clear, change, template and SQL queries, type registration - to any program that can speak gRPC. The client needs no GigaSpaces jar and no lookup settings: only the `.proto` files and a host and port.

```
 native client                               gRPC client
 ─────────────                               ───────────
 your JVM                                    your process (any language)
   GigaSpace proxy                             generated stub
        │                                          │  1. gRPC (HTTP/2 + protobuf), port 9091
        │                                          ▼
        │                                    REST v3 process (on a manager host)
        │                                      protobuf → SpaceDocument
        │                                      its own GigaSpace proxy
        │  1. space protocol                       │  2. space protocol
        ▼                                          ▼
   ┌───────────┐ ┌───────────┐               ┌───────────┐ ┌───────────┐
   │partition 1│ │partition 2│               │partition 1│ │partition 2│
   └───────────┘ └───────────┘               └───────────┘ └───────────┘
```

Every gRPC call makes two network trips and two conversions. That is the cost of supporting language-agnostic clients; this lab helps you weigh that cost against the benefit.

---
###### Lab Goals
1. Generate a gRPC client from the server's `.proto` files and use every kind of operation.
2. Know what a value becomes on the way through, and how a gRPC client and a Java service can share one type safely.
3. Recognize the caveats: no query parameters, unlimited results by default, message size limits, error codes, deadlines.
4. Decide when gRPC is the right client, given the cost of the extra hop.

## Lab setup
Requires **GigaSpaces 18.0.1 or later** and **JDK 25**. 

1. Start a grid with REST v3:  
   Go to `$GS_HOME/bin`  
   Run `./gs.sh host run-agent --auto --restv3 --gsc=2`

   *Without `--restv3` there is no gRPC server.* Look for the log line `gRPC Server started, listening on address: [/[0:0:0:0:0:0:0:0]:9091], port: 9091`.
2. Deploy the space, from `$GS_HOME/bin` in a second terminal:

   ```bash
   ./gs.sh space deploy --partitions=2 grpcDemo
   ```
3. Set `gigaspaces.version` in `pom.xml` to your GigaSpaces version, open `lab23-grpc-example/pom.xml` with IntelliJ, and run `mvn package` once before the first run: it generates the gRPC client from `client/src/main/proto`.
4. Copy `runConfigurations` into `.idea` and set the path variables, as described in Note 2 of the main [README.md](https://github.com/GigaSpaces-ProfessionalServices/gs-core-dev-training/blob/main/README.md). Set `GS_LOOKUP_LOCATORS` and `GS_LOOKUP_GROUPS` (for example `localhost` and `xap-18.0.1`); only steps 3 and 5 use them, for their native client. The gRPC client needs only `-Dgrpc.host` and `-Dgrpc.port`.

Run the steps in order: step 1 registers the `Product` type the others use. Every program can be run again.

From the command line instead of IntelliJ:

```bash
mvn package dependency:copy-dependencies -DoutputDirectory=target/lib -DincludeScope=runtime
java -Dcom.gs.jini_lus.locators=localhost -cp "client/target/classes:client/target/lib/*:model/target/classes" \
     com.gs.grpc.examples.BasicCrud
```

## Examine the code

### The contract: `client/src/main/proto/space`
The `.proto` files are the server's contract, downloaded from it with `curl -O http://localhost:9090/api/v3/grpc/proto.zip`. Download them again with each new GigaSpaces version and regenerate (file list in [REFERENCE.md](REFERENCE.md#the-proto-files)).

Every call exchanges documents, and every value is one of four kinds (`common.proto`):

```protobuf
message Value {
  oneof kind {
    string string_value = 1;
    int64 int64_value = 2;
    double double_value = 3;
    bool bool_value = 4;
  }
}
message SpaceDocument {
  string type_name = 1;
  map<string, Value> properties = 2;
}
```

`space_service.proto` lists the 14 calls: `Write`, `WriteMultiple`, `RegisterType`, `ReadById`,
`ReadByIds`, `ReadMultiple`, `TakeById`, `TakeByIds`, `TakeMultiple`, `Clear`, `Count`,
`Change`, `QueryExpression`, `ExecuteDurableTask`.

A request carries everything the call needs. For example, `QueryExpressionRequest` (`space_admin.proto`):

```protobuf
message QueryExpressionRequest {
  string space_name = 1;
  string type_name = 2;
  // SQL WHERE clause, e.g. "status = 'PENDING'"; empty string matches every entry of the type.
  string expression = 3;
  // 0 (or unset) means unbounded (GigaSpace.readMultiple(query) without an explicit limit).
  int32 max_entries = 4;
}
```

### The lab modules

| Module | Classes | Purpose |
|--------|---------|---------|
| `model` | `Order`, `OrderStatus`, `Address` | A POJO owned by a Java service, used in step 3 |
| `client` | `src/main/proto/space/*.proto` | The contract, as served by REST v3 |
| | `common.Grpc` | The channel, the stub, deadlines, credentials |
| | `common.Values` | Java values to and from the gRPC `Value`; printing documents |
| | `common.NativeSpace` | The native client, for steps 3 and 5 |
| | `examples.*` | One program per step |
| `python-client` | `space_client.py` | Step 6 |

`common.Grpc` shows the three things every gRPC client should get right:
- **one channel per process** - a `ManagedChannel` is a thread-safe, multiplexed HTTP/2 connection; one per call costs a handshake every time;
- **a deadline on every call** - a blocking call with no deadline waits forever on a server that does not answer;
- **credentials as call metadata**, for a secured grid ([Security](REFERENCE.md#security)).

## Step 1 - Working with documents: `BasicCrud`
A new inventory service needs to keep a product catalog in the space. There is no shared
Java class for it, so the service registers a `Product` type itself and works with documents: a
type name and a map of property values.

```java
Grpc.withDeadline(stub).registerType(RegisterTypeRequest.newBuilder()
        .setSpaceName(space).setTypeName("Product")
        .putProperties("productId", "String")
        .putProperties("price", "Double")
        .putProperties("stock", "Integer")          // ...and name, category
        .addId("productId")
        .putIndexes("category", "EQUAL")
        .build());
```

| Output line | Action | You see | What it means |
|---|---|---|---|
| 1 | `registerType` (second run) | `INVALID_ARGUMENT - Type 'Product' is already registered` | Registration is not idempotent; expect this on restart |
| 3 | `write` p1 again | `count = 5 (updated, not added)` | `write` is insert-or-update and replaces the whole entry |
| 4 | `readById` p1 | `stock=101(int64)` | Every integral type comes back as `int64` |
| 4 | `readById` nope | `hasDocument() = false` | A miss is not an error |
| 5 | `readByIds [p2, nope, p4]` | `2 documents` | Misses are dropped, not `null`: match results by id, never by position |
| 8 | `readById` on NoSuchType | `INTERNAL - Failed to load space type descriptor` | An unknown type is `INTERNAL`, not `NOT_FOUND`; don't treat `INTERNAL` as "server broken, retry" |

**Takeaway:** check `hasDocument()`, match results by id, and expect every integer back as `int64`.

## Step 2 - Query with a WHERE clause, update with change: `QueriesAndChange`
The inventory service needs to find every game priced at 60 or more, and to take stock
down when items sell, without racing other clients that update the same products.

```java
Grpc.withDeadline(stub).queryExpression(QueryExpressionRequest.newBuilder()
        .setSpaceName(space).setTypeName(type)
        .setExpression("category = 'games' AND price >= 60").setMaxEntries(100).build());
```

| Output line | Action | You see | What it means |
|---|---|---|---|
| 1 | `readMultiple` template `{category=books}` | `5 documents` | A template (`readMultiple`, `takeMultiple`, `clear`, `count`) matches exact values only |
| 2 | `queryExpression` `category = 'games' AND price >= 60` | `3 documents` | For ranges, `OR`, `IN`, `LIKE`: a SQL WHERE clause, the same syntax as `SQLQuery` |
| 3 | `queryExpression` with `maxEntries` not set | `10` instead of `3` | **Unset is 0, and 0 means unlimited** |
| 5 | `change` by SQL: decrement stock by 1 | `changed 5` | Updated inside the space, with no read-modify-write race between clients |
| 8 | `queryExpression` `price >>> 1` | `INTERNAL` with a server stack trace | Log it; don't show it to users |

There are no projections, aggregations, ORDER BY, JOINs or iterators: a query returns whole documents, in one response.

**Takeaway:** set `maxEntries` on every query, and update with `change` rather than read-then-write.

## Step 3 - Read-modify-write loses data: `PojoInterop`
Your Java order service owns the `Order` POJO and writes it natively. A Python team now
needs to update order quantities over gRPC. The order's nested `Address` reaches them only
as a description string (output line 1), so they can't send it back.

❌ **Read, modify, write back:** the address is lost

```java
SpaceDocument withoutAddress = o1.toBuilder()
        .removeProperties("shippingAddress")
        .putProperties("quantity", of(3))
        .build();
Grpc.withDeadline(stub).write(WriteRequest.newBuilder().setSpaceName(space).setDocument(withoutAddress).build());
```
```
5. Java service reads -> Order{orderId=o1, ..., quantity=3, ..., shippingAddress=null}
```

✅ **`change` only what you mean to change:** the address survives

```java
Grpc.withDeadline(stub).change(ChangeRequest.newBuilder()
        .setSpaceName(space).setTypeName(ORDER)
        .setCriteriaType("id").setCriteria(of("o2")).setCriteriaRouting(of("c2"))
        .addOperations(ChangeOperation.newBuilder().setOp("set").setPath("quantity").setValue(of(3)))
        .build());
```
```
6. Java service reads -> Order{orderId=o2, ..., quantity=3, ..., shippingAddress=Address{city=Lyon, country=FR}}
```

`write` replaces the whole entry; the Java service, which knows nothing about gRPC, silently lost data. Also in the output:
- **Types** (lines 3–4): enums and dates travel as strings and are checked on the way back in; an out-of-range number or unknown enum name is `INVALID_ARGUMENT`. See [Value mapping](REFERENCE.md#value-mapping).
- **Routing** (line 2): `Order` is routed by `customerId`. Without a routing value, a call by id goes to every partition.

**Takeaway:** give types shared with gRPC clients a flat model, and update them with `change`, not `write`.

## Step 4 - Ways to get it wrong: `Caveats`
The inventory service is going to production. It gets a search box, a nightly batch
import, and categories with tens of thousands of products. Each of these meets a limit
of the gRPC API.

| Output line | Action | You see | What it means |
|---|---|---|---|
| 1 | query with user input `books' OR category <> 'x` | pasted in: `4 products`; escaped: `0 products` | **SQL injection.** There are no `?` parameters: quote every value, or never let free text reach the expression |
| 2 | `writeMultiple` with one bad document | `INVALID_ARGUMENT - Document [2] (id=b3)`, then `0` products written | **Validated, not transactional.** A bad document rejects the whole call, but a failure during the write is not rolled back |
| 3 | `queryExpression` with a 1 ms deadline | `DEADLINE_EXCEEDED` | **Deadlines.** A write may still have happened: retry only what is safe to repeat |
| 4 | `queryExpression` with `maxEntries = -1` | `INVALID_ARGUMENT - max_entries must be >= 0` | Negative is rejected; 0 is unlimited |
| 5 | `count` in noSuchSpace | `NOT_FOUND - Did not find space` | **The space name is in every request.** A wrong name fails per call, not at start-up |
| 6 | `count` with no template | `7` instead of `4` (every type) | **No template means the whole space.** A `ClearRequest` without one empties it |
| 7 | `queryExpression` with `maxEntries = 0` on 20,000 products | `RESOURCE_EXHAUSTED - gRPC message exceeds maximum size 4194304` | **4 MB message limit.** Set `maxEntries` and page by key (`productId > 'last-seen'`) rather than raising the limit |

The search box quotes what it pastes into an expression with `Caveats.quote`:

```java
static String quote(String value) {
    return "'" + value.replace("'", "''") + "'";
}
```

See [Error codes](REFERENCE.md#error-codes).

**Takeaway:** quote values, set deadlines and `maxEntries`, and always set a template.

## Step 5 - The price of the extra hop: `NativeVsGrpc`
A Java team asks whether to move their pricing service to gRPC so that every client uses
the same API. `NativeVsGrpc` times the same `readById` and `write` calls through both
clients. How to run it and what it shows: [Measure it yourself](REFERENCE.md#measure-it-yourself-nativevsgrpc).

**Takeaway:** keep latency-critical Java on the native client. For gRPC, use batch calls and share one channel.

## Step 6 - No JVM required: `python-client/space_client.py`
A data science team wants to add products from a Python notebook. They need only the
`.proto` files and `pip`. From `python-client`:

1. Install the gRPC tools:
   ```bash
   pip install grpcio grpcio-tools
   ```
2. Generate the stubs from the same `.proto` files the Java client uses:
   ```bash
   mkdir generated
   python -m grpc_tools.protoc -I ../client/src/main/proto --python_out=generated --grpc_python_out=generated \
          ../client/src/main/proto/space/*.proto
   ```
3. Run the client:
   ```bash
   python space_client.py localhost:9091 grpcDemo
   ```

**Note:** in Python `True` is an `int`, so check for `bool` first when building a `Value`, or booleans become `int64`:

```python
def value(v):
    if isinstance(v, bool):
        return common_pb2.Value(bool_value=v)
    if isinstance(v, int):
        return common_pb2.Value(int64_value=v)
    ...
```

```
wrote py1
readById py1 -> {'category': 'books', 'name': 'From Python', 'price': 7.5, 'productId': 'py1', 'stock': 3}
queryExpression productId LIKE 'py%' -> ['py1']
readById in noSuchSpace -> NOT_FOUND - Did not find space [noSuchSpace]
```

The entry is an ordinary `Product`, readable by every other client.

**Takeaway:** any language works the same way: generate from the same `.proto` files and call the same 14 operations.

## When to use gRPC
**Use gRPC when**
- The client is **not Java** and needs to read and write entries, not run reports.
- You want a **typed, versioned contract** across teams and languages.
- The client must not carry GigaSpaces jars or join the lookup service.
- The calls are request/response on entries by id, by template or by a simple condition.

**Use something else when**
- The client is Java and latency-critical - the native client (gRPC was 1.2x to 4.8x slower, 2.2x on average, in a [benchmark](REFERENCE.md#benchmark) run outside this lab).
- You need JOINs, GROUP BY, or SQL tools - JDBC v3.
- You need transactions, notifications, event containers, iterators, aggregations, remoting or colocated tasks - native only.
- Entries have nested, collection, binary or `BigDecimal` properties the client must write back (step 3).
- You are in a shell script or a browser - REST v3 HTTP needs only `curl`.

Side-by-side comparison of all four clients, and a checklist of anti-patterns: [REFERENCE.md](REFERENCE.md).

## Further reading
- [REFERENCE.md](REFERENCE.md) - client comparison, value mapping, error codes, benchmark, anti-patterns, security, operations
- Online documentation: https://docs.gigaspaces.com/latest/
