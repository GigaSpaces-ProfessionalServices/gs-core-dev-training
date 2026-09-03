# gs-dev-training - lab21-storage_adapter-example

## Property Storage Adapter
Because GigaSpaces stores data in memory, large properties such as XML documents, JSON payloads, or binary blobs can consume significant heap space.  
A Property Storage Adapter lets you store a single property in a transformed form (compressed, encrypted, or binary) while the rest of the application continues to work with the original Java type.  
TODO
The adapter is transparent: GigaSpaces calls `toSpace()` on every write and `fromSpace()` on every read, so no changes are needed in the feeder or query code.
---
###### Lab Goals
1. Get familiar with the GigaSpaces Property Storage Adapter feature.
2. Understand how a custom adapter controls serialization and storage of a space entry property.
3. Write and read `OrderDocument` entries whose XML payload is stored in compressed form using `CompressedXmlPropertiesAdapter`.


###### Lab Description

This lab demonstrates the `@SpacePropertyStorageAdapter` annotation, which lets a custom `PropertyStorageAdapter` control how a specific property is serialized before it is written to the space and deserialized when it is read back.  
`CompressedXmlPropertiesAdapter` stores `XmlProperty` objects as DEFLATE-compressed bytes, reducing memory usage for large XML payloads.  
`XmlProperty` holds both the raw XML string and a parsed `Map<String, Object>` of properties to support query access without decompressing the full payload.


## Lab setup
Make sure you restart the service grid and gs-ui (or at least undeploy all Processing Units using gs-ui).

1. Start the demo:  
   Go to `$GS_HOME/bin`  
   Run `./gs.sh demo` or `gs.bat demo`

   *This starts an agent, manager, 4 gscs, restv3 and the webui. It also deploys a space named 'demo' with 2 partitions and 1 backup apiece.*
2. Open gs-dev-training/lab21-storage_adapter-example project with IntelliJ (open pom.xml)
3. Run `mvn compile`

## Examine the code

The lab has three modules:

| Module | Classes | Purpose |
|--------|---------|---------|
| `adapters` | `CompressedXmlPropertiesAdapter`, `XMLProperty`, `XmlParser` | Storage adapter and the property type it manages |
| `model` | `OrderDocument` | Space POJO |
| `feeder` | `DataGen` | Writes and reads `OrderDocument` entries |

Key classes to investigate:

- **`OrderDocument`** - `@SpaceClass` with `@SpacePropertyStorageAdapter(CompressedXmlPropertiesAdapter.class)` on `getOrderData()`. The `orderData` field is transparently compressed on write and decompressed on read.
- **`CompressedXmlPropertiesAdapter`** - extends `PropertyStorageAdapter`; implements `toSpace()` (compress) and `fromSpace()` (decompress).
- **`XmlProperty`** - holds the XML string and a parsed `Map<String, Object>` of properties for indexed query access.
- **`XmlParser`** - converts an XML string into a nested `Map<String, Object>`.
- **`DataGen`** - writes 1000 `OrderDocument` entries and demonstrates read queries.

## Run DataGen

TODO
In `DataGen.main()`, uncomment `writeData` to populate the space, then re-run with it commented out to exercise the read queries.

###### Option 1 - Using Maven CLI:

`cd ~/gs-core-dev-training/lab21-storage_adapter-example/feeder`

`mvn exec:java -D"exec.mainClass"="com.gs.DataGen" -Dexec.classpathScope=compile -Dcom.gs.jini_lus.locators=localhost`

###### Option 2 - Using IntelliJ:

1. Create a run configuration for `com.gs.DataGen` in the `feeder` module.
2. Make sure the `GS_LOOKUP_LOCATORS` and `GS_LOOKUP_GROUPS` environment variables are set correctly.  
   For example, `GS_LOOKUP_LOCATORS=localhost` and `GS_LOOKUP_GROUPS=xap-17.2.1`.

   Add the following VM options to the run configuration:  
   `-Dcom.gs.jini_lus.locators=${GS_LOOKUP_LOCATORS}` and `-Dcom.gs.jini_lus.groups=${GS_LOOKUP_GROUPS}`.

   Please see the main [README.md](https://github.com/GigaSpaces-ProfessionalServices/gs-core-dev-training/blob/main/README.md) for setting path values (environment variables in File | Settings | Appearance & Behavior | Path Values) used when running within IntelliJ.
3. Build the project: `mvn package`
TODO add runConfiguration
## Verify the results

Use the GigaSpaces Web UI or gs-ui to browse the demo space and confirm that `OrderDocument` entries are present.  
The `orderData` field is stored as compressed bytes in the space; the adapter transparently decompresses it on read.

## Online documentation
See: https://docs.gigaspaces.com/latest/dev-java/property-storage-adaptor.html
