# gs-core-dev-training - lab22-offheap_blobstore-example

## Off-heap blobstore: reading values in place
An off-heap blobstore keeps every entry as a block of serialized bytes outside the Java heap. Until 18.0.1, a query, an aggregation or a space iterator that touched an entry first decoded the whole block into a full entry object - even to read a single property, even when the entry did not match.

From 18.0.1 the space reads the values it needs straight from the block:

- **a query condition** is tested against the block, and an entry that does not match is dropped without being built;
- **an aggregation** made of aggregators that declare the paths they read is fed those values from the block, and builds no entry at all - built-in aggregators already declare their paths, custom ones can;
- **a space iterator with a projection** assembles what it returns from the block, for both iterator types.

There is also one change in behaviour: **a space iterator now applies the query projection**, the way `read` and `readMultiple` always did.

---
###### Lab Goals
1. Deploy a space with an off-heap blobstore, and know which setting decides whether its entries are read from the off-heap blocks.
2. See, for each kind of operation, whether the space built the entries or read the values in place.
3. Write a custom aggregator that gets the speed-up, and recognise the error raised by one that breaks the rule.
4. Find out whether existing read-modify-write code is affected by the iterator projection change, and fix it.
5. Know where the speed-up does not apply.

###### Lab Description
Every example writes 1000 `Payment` entries (`id`, `amount` 1..1000, `currency` USD/EUR/ILS) and runs a few operations. For each operation it prints the result, **how many entries the space built** and how long it took.

The number of built entries is the one to watch. With a thousand entries the time hardly differs, the count does: zero means the values were read in place. The count is taken inside the space by `BuiltEntriesTask`, a distributed task sent with the example - nothing has to be deployed for it. It counts a debug line the space writes for every entry it builds, so it is a diagnostic aid for this lab, not a product API, and two programs counting the same space at the same time would reset each other.

Zero means success only if the counter sees anything at all. So the examples start with a **control**: an operation that must build every entry. If the control shows zero, the numbers after it mean nothing - see [Verifying you get the speed-up](#verifying-you-get-the-speed-up).

## Lab setup
Requires **GigaSpaces 18.0.1 or later** and **JDK 25**. Make sure no other grid is running with the same lookup group.

1. Start a grid with six containers:  
   Go to `$GS_HOME/bin`  
   Run `./gs.sh host run-agent --auto --gsc=6`

   *This starts an agent, a manager and six containers. `gs.sh demo` is not enough: its four containers are taken by its own `demo` space, and one container holds one space instance.*
2. Build the lab: open `lab22-offheap_blobstore-example/pom.xml` with IntelliJ, or run `mvn package` in that folder.
3. Deploy the three spaces the lab uses. The agent from step 1 keeps its terminal, so run these in a second one, from `$GS_HOME/bin`, once the manager and the containers have started (`./gs.sh container list` shows six).

   `offheapDemo`, the space every example uses - an off-heap blobstore with its internal cache switched off:

   ```bash
   ./gs.sh space deploy --partitions=2 --ha \
       -p space.mx.off-heap.enabled=true -p space.mx.off-heap.threshold=200m \
       -p space.mx.persistent=false -p space.mx.cache-entries-percentage=0 offheapDemo
   ```

   `offheapDefaultCache`, the same blobstore with its internal cache left at the default size (step 6 only):

   ```bash
   ./gs.sh space deploy --partitions=1 \
       -p space.mx.off-heap.enabled=true -p space.mx.off-heap.threshold=200m \
       -p space.mx.persistent=false offheapDefaultCache
   ```

   `offheapWithFilter`, the same as `offheapDemo` plus a space filter - a processing unit, see `filter-space/` (step 6 only):

   ```bash
   ./gs.sh pu deploy --partitions=1 offheapWithFilter \
       <lab folder>/filter-space/target/filter-space-1.0-SNAPSHOT.jar
   ```

   No `pu.xml` is needed for the first two: the space template reads the `space.mx.*` properties.

   | property | meaning |
   |---|---|
   | `space.mx.off-heap.enabled=true` | store the entries in an off-heap blobstore |
   | `space.mx.off-heap.threshold` | how much off-heap memory the blobstore may use |
   | `space.mx.persistent=false` | keep the entries in memory only (an off-heap blobstore is never persistent) |
   | `space.mx.cache-entries-percentage=0` | **switch the blobstore internal cache off**, so every read goes to the off-heap block. The default is 20% of the heap |

   A space filter cannot be set through such properties, so `offheapWithFilter` is a processing unit with its own `pu.xml` - the same blobstore settings written out, plus the filter.

   To check what you deployed, look for these lines in the container log of each space instance: `created blob-store handler class=com.gigaspaces.blobstore.offheap.OffHeapBlobStoreHandler persistentBlobStore=false`, `BlobStore space data internal cache size=0` and `Space started [... policy=blob-store ...]`.
4. Copy the entire `runConfigurations` folder and its contents from this project into the `.idea` directory. You may need to restart IntelliJ.
5. Make sure the `GS_LOOKUP_LOCATORS` and `GS_LOOKUP_GROUPS` environment variables are set correctly.  
   For example, `GS_LOOKUP_LOCATORS=localhost` and `GS_LOOKUP_GROUPS=xap-18.0.1`.

   In each of the IntelliJ run configurations, there are VM options that reference these environment variables:  
   `-Dcom.gs.jini_lus.locators=${GS_LOOKUP_LOCATORS}` and `-Dcom.gs.jini_lus.groups=${GS_LOOKUP_GROUPS}`.

   Please see the main [README.md](https://github.com/GigaSpaces-ProfessionalServices/gs-core-dev-training/blob/main/README.md) for setting path values (environment variables in File | Settings | Appearance & Behavior | Path Variables) used when running within IntelliJ.

## Examine the code

| Module | Classes | Purpose |
|--------|---------|---------|
| `model` | `Payment` | Space POJO, no indexes |
| `client` | `common.Spaces`, `common.Payments` | Connecting to the three spaces; the data and the expected answers |
| | `common.BuiltEntries`, `common.BuiltEntriesTask` | Counting the entries a space builds |
| | `aggregators.*` | Three custom aggregators: two correct ones and one that breaks the rule |
| | `examples.*` | One program per step below |
| `filter-space` | `CurrencyAuditFilter`, `META-INF/spring/pu.xml` | A processing unit: an off-heap space with an after-read space filter |

## Step 1 - A query condition is tested against the block: `PredicatePushdown`
A condition on a non-indexed property makes the space look at every entry. It used to build each one to test it; now it reads the properties the condition names from the block and builds only the entries that match - because a read without a projection returns them whole. Nothing changes in the client: every query gets this.

```
control: readMultiple, no condition              result: 1000           built:  1000
readMultiple where amount > 990                  result: 10             built:    10
readMultiple where currency='USD' and amount>990 result: 3              built:     3
count() where amount > 990                       result: 10             built:     0
change where amount > 990                        result: 10             built:    10

As expected: a read or a change builds only the entries it matches, and count() builds none.
```

The gain grows with the selectivity of the query: it is the entries that do **not** match that are no longer built. A query that matches almost everything gains almost nothing.

A `change` gets the same: the entries it does not match are not built, the ones it changes are - a change needs the entry. So keep the selection in the query. A custom `ChangeOperation` that is handed every entry and decides inside which ones to skip makes the space build them all. A condition that is an OR of several pairs (see step 2) is not tested against the block either; several queries joined by AND are.

## Step 2 - Built-in aggregators: `BuiltInAggregators`
Built-in aggregators over one property - `count`, `sum`, `average`, `minValue`, `maxValue` and the like - declare which property they read, so an aggregation made of them builds nothing.

```
count()                                          result: 1000           built:     0
sum(amount)                                      result: 500500.0       built:     0
average(amount)                                  result: 500.5          built:     0
maxValue(amount)                                 result: 1000.0         built:     0
count() where amount > 500                       result: 500            built:     0
count() where (currency, amount) pairs           result: 351            built:  1000
maxEntry(amount) - needs the entry               result: Payment{id=... built:  1000
```

The last two lines show where it stops: `maxEntry` returns an entry, so every entry is built; and a condition of a more complex form - an OR of (currency, amount) pairs - is not decided from the block.

## Step 3 - Custom aggregators: `CustomAggregators`
A custom aggregator opts in by overriding `SpaceEntriesAggregator.getAggregatedPaths()` and returning the paths it reads. The rule:

> **Declared your paths - read only them, and only through `getPathValue`. Need the entry itself - do not declare paths.**

Investigate the three aggregators in `aggregators`:

- **`LargePaymentsAggregator`** - two paths, `currency` and `amount`, declared and read through `context.getPathValue(path)`;
- **`AmountHistogramAggregator`** - one path, written on top of `AbstractPathAggregator`, whose `declaredPath()` turns the path set with `setPath` into the declaration;
- **`LargePaymentsByServerEntryAggregator`** - declares its paths but reads the values off `context.getServerEntry()`.

```
largePayments (custom, two paths)                result: 351            built:     0
  expected: 351
amountHistogram (custom, one path)               result: [250, 250, ... built:     0
  buckets of 250: [250, 250, 250, 250], expected: [250, 250, 250, 250]

largePaymentsByServerEntry (custom, breaks the promise):
  failed, as it should: java.lang.IllegalStateException: An aggregator asked for the entry of uid ...
  after declaring that it reads nothing but its own paths, so the entry was never built.
  See SpaceEntriesAggregator.getAggregatedPaths.
```

The third aggregation fails rather than returning a wrong number: no entry was built, so there is none to hand out. `getRawEntry()` throws the same way. The partition id, the entry uid and the type descriptor are still available. The same exception is logged as `SEVERE` in the container log of each primary instance - that is expected here.

A declaration holds for the class that makes it and is not inherited: an aggregator that extends a built-in one, or `AbstractPathAggregator`, without overriding `getAggregatedPaths()` itself is handed the entry, as before.

## Step 4 - Space iterators: `SpaceIterators`
Both iterator types now apply the projection - `currency`, not projected, comes back `null` - and with a projection of plain properties they build no entry.

```
CURSOR without projection                        read: 1000  currency null:    0  built:  1000
CURSOR with projection id+amount                 read: 1000  currency null: 1000  built:     0
PREFETCH_UIDS with projection id+amount          read: 1000  currency null: 1000  built:     0
  of which built while collecting the ids: 0, while reading by id: 0

As expected: with a projection neither iterator type builds an entry.
```

A `PREFETCH_UIDS` iterator works in two steps: when it is created it collects the ids of all matching entries and receives the first batch of entries with them; then it reads the rest by id. Both steps build nothing now. Without a projection the entries are still built - the client asked for whole entries.

## Step 5 - Read-modify-write: `ReadModifyWrite`
This is the one change that breaks code silently. A loop that iterates with a projection and writes the objects back used to receive whole objects and store them whole; now it receives objects whose non-projected properties are `null`, and a plain `write` stores those `null`s over the values that were there.

```
plain write:   payments that lost their currency: 1000 of 1000
PARTIAL_UPDATE: payments that lost their currency: 0 of 1000, amounts raised: yes
```

If you have such a loop, either remove the projection from the query, or write the object back as a partial update - `WriteModifiers.PARTIAL_UPDATE`, or the `change` API - so that the untouched properties are left alone.

## Step 6 - Where it does not apply: `WhereItDoesNotApply`
The same four operations on all three spaces. Each column is the number of entries built, out of 1000:

```
space                      maxEntry      sum   CURSOR projected  PREFETCH_UIDS projected
offheapDemo                    1000        0                  0                        0
offheapDefaultCache               0        0                  0                        0
offheapWithFilter              1000        0               1000                      900
```

- **`offheapDefaultCache`** - entries held by the blobstore internal cache are already-built objects on the heap and are served from there; the block is not read at all, so even the control (`maxEntry`) shows zero. That is not a problem to tune away: a cached entry needs no optimization, and reading from the block serves the entries that are not in the cache. On a warm cache reading the block would be slower, not faster. The internal cache is switched off in `offheapDemo` only so that every read in this lab goes to the block.
- **`offheapWithFilter`** - a space filter is handed every entry a read returns, whole, before the projection is applied. A batch read with a projection therefore takes the regular path on a space with such a filter, and builds its entries. Aggregation is not affected: aggregators are not passed through read filters. `PREFETCH_UIDS` shows 900, not 1000: its first batch arrives with the ids, as part of an aggregation, and is not passed through read filters - in any version, with or without a projection; the other 900 entries are read by id with a regular, filtered read. The filter writes a line for every entry it sees to the container log (`CurrencyAuditFilter saw payment ...`).

Not shown here, but just as true:

- **tiered storage and a persistent (RocksDB) blobstore** gain nothing: only an off-heap blobstore keeps the entries as blocks the space can read in place;
- an entry that is already built on the heap is served as it is;
- `readByIds` and a read or an iteration **without a projection** build their entries as before;
- a property whose type cannot be read in place, or a projection that names a nested path, takes the regular path.

## Verifying you get the speed-up
1. Run the control first. An operation that must build every entry - an iteration or a `readMultiple` without a projection, or `maxEntry` - has to show `built` equal to the number of entries. If it shows zero, the entries are being served from the internal cache: deploy with `space.mx.cache-entries-percentage=0` to see the in-place reads, as in this lab.
2. Then the operation you care about: zero built entries means the values were read in place.
3. Check the container log for the lines listed in [Lab setup](#lab-setup), step 3.

## Seeing the before
Run the same examples against a grid of **18.0.0** (or earlier) with the same three spaces deployed - the lab, built against 18.0.1, runs there unchanged. Several verdicts then read `UNEXPECTED`, and that is the point:

| step | 18.0.1 | 18.0.0 |
|---|---|---|
| 1 - `readMultiple where amount > 990` | built 10 | built 1000 |
| 2 - `sum(amount)` | built 0 | built 1000 |
| 3 - `largePayments` | built 0 | built 1000 |
| 3 - `largePaymentsByServerEntry` | `IllegalStateException` | returns 351 - the paths are not used, so the entry is there |
| 4 - `CURSOR` with projection | `currency` null 1000, built 0 | `currency` null 0 - projection ignored, built 1000 |
| 4 - `PREFETCH_UIDS` with projection | `currency` null 1000, built 0 | `currency` null 800 - the first batch of each of the two partitions arrives whole, built 1800 |
| 5 - plain write | 1000 currencies lost | 0 lost - the loop was handed whole payments |

## How much faster
Measured separately, outside this lab: an embedded space holding **1,000,000 entries** of a type with 200 properties (one of them a 256-byte `byte[]`), the blobstore internal cache switched off. Each operation goes over all the million entries. The time is for the whole operation; next to it is the time per entry in µs - microseconds, thousandths of a millisecond. The code without and with the change was run in alternating rounds, six of each; the medians are shown.

| operation, over 1,000,000 entries | before | after | change |
|---|---:|---:|---:|
| aggregation `sum` + `average` of one non-indexed property, no condition | 5602 ms (5.60 µs per entry) | 852 ms (0.85 µs) | −84.8% |
| `CURSOR` iteration over all entries, projection of three properties | 5744 ms (5.74 µs per entry) | 1099 ms (1.10 µs) | −80.9% |
| `PREFETCH_UIDS` iteration over all entries, projection of three properties | 6906 ms (6.91 µs per entry) | 2018 ms (2.02 µs) | −70.8% |

The query condition test (step 1) comes from a different measurement - also a million entries in an embedded space, but of another type, and against the off-heap blobstore as it was before all of this release's off-heap work, not before this one change. A `readMultiple` with a condition on a non-indexed property became about 2.4 times faster when the condition matches 100 of the million entries, and about 1.5 times faster when it matches half of them. The gain comes from the entries that do not match, so a condition that matches almost everything gains almost nothing.

The gain depends on the entry: the wider the type and the fewer properties an operation needs, the more there is to save. A narrow type like `Payment` gains less.

## Online documentation
See: https://docs.gigaspaces.com/latest/dev-java/aggregators.html
