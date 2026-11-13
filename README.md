# gs-core-dev-training

This repository contains the labs for GigaSpaces training:

- [`gs-core-dev-training`](gs-core-dev-training) - Core Developer Training. See [gs-core-dev-training/README.md](gs-core-dev-training/README.md) for prerequisites and development IDE (Maven / IntelliJ) setup.
- [`gs-admin-training`](gs-admin-training) - Admin Training. For more information see the [course syllabus](https://docs.google.com/spreadsheets/d/10Svq6s90eDw-Ger8TJogyVWLy0FcdfSF3EgtilCd2XY/edit#gid=0).

## Getting the labs

Go to the location where you want lab content to be stored and run:

```bash
git clone https://github.com/GigaSpaces-ProfessionalServices/gs-core-dev-training.git
```

Or download the zip from this repository and extract it, preferably to your home directory.

## Core Developer Training labs

| Lab | Topic | Features | Directories |
|-----|-------|----------|-------------|
| - | Core GigaSpaces API (Jupyter notebooks) | `GigaSpace` write/read/take, `SQLQuery`/`IdQuery`, `SpaceDocument`, Change API (`ChangeSet`), colocated `Task` execution, aggregations | [gs-dev-jupyter-lab](gs-core-dev-training/gs-dev-jupyter-lab) |
| 11 | Space-Based Remoting | `@RemotingService`, executor remoting (broadcast and `@Routing`-routed), remote result reducers, BillBuddy sample app | [lab11-space_based_remoting-exercise](gs-core-dev-training/lab11-space_based_remoting-exercise)<br>[lab11-space_based_remoting-solution](gs-core-dev-training/lab11-space_based_remoting-solution) |
| 12 | Event Processing | Event containers: `@Polling` and `@Notify`, event templates, monitoring events | [lab12-event_processing-exercise](gs-core-dev-training/lab12-event_processing-exercise)<br>[lab12-event_processing-solution](gs-core-dev-training/lab12-event_processing-solution) |
| 13 | Transactions | Transactional event processing, Spring `@Transactional` with the Distributed Transaction Manager, `ChangeSet`, monitoring transactions in GS-UI | [lab13-transactions-exercise](gs-core-dev-training/lab13-transactions-exercise)<br>[lab13-transactions-solution](gs-core-dev-training/lab13-transactions-solution) |
| 14 | Persistency | Mirror Service, asynchronous persistence (write-behind) to MySQL via Hibernate | [lab14-persistency-exercise](gs-core-dev-training/lab14-persistency-exercise)<br>[lab14-persistency-solution](gs-core-dev-training/lab14-persistency-solution) |
| 15 | Durable Task | `DurableTask` (extends `DistributedTask`), task survives failover, `@SupportCodeChange` | [lab15-durable_task-example](gs-core-dev-training/lab15-durable_task-example) |
| 16 | JDBC v3 | GigaSpaces JDBC driver: SQL queries, joins, aggregates, dynamic DDL/DML | [lab16-jdbc_v3-example](gs-core-dev-training/lab16-jdbc_v3-example) |
| 17 | Custom Aggregator | Custom `SpaceEntriesAggregator`, `AggregationSet`, compared with an equivalent plain SQL query | [lab17-custom_aggregator-example](gs-core-dev-training/lab17-custom_aggregator-example) |
| 18 | Dynamic Filter | JDBC `DYNAMIC_FILTER` hint, `@SpaceRouting` alignment and colocated joins | [lab18-dynamic_filter-example](gs-core-dev-training/lab18-dynamic_filter-example) |
| 19 | Distributed Tracing | OpenTelemetry-based tracing of space operations, Zipkin as the backend | [lab19-distributed_tracing-example](gs-core-dev-training/lab19-distributed_tracing-example) |
| 20 | Billing App | `gs.sh blueprint generate`, multi-module project: `@RemotingService`, `@Polling` containers, REST service, deploy locally or to Kubernetes (Helm) | [lab20-billing_app-example](gs-core-dev-training/lab20-billing_app-example) |
| 21 | Property Storage Adapter | Custom `PropertyStorageAdapter` with `@SpacePropertyStorageAdapter` to transparently compress an XML property | [lab21-storage_adapter-example](gs-core-dev-training/lab21-storage_adapter-example) |
| 22 | Off-heap Blobstore | Off-heap blobstore read in place: predicate pushdown, built-in and custom aggregators, space iterators, read-modify-write (18.0.1+, JDK 25) | [lab22-offheap_blobstore-example](gs-core-dev-training/lab22-offheap_blobstore-example) |

## Admin Training labs

| Lab | Topic | Features | Directories |
|-----|-------|----------|-------------|
| 01 | GigaSpaces setup and installation | Introduction, start the service grid, one-click install script | [lab01-gs_setup/01-introduction](gs-admin-training/lab01-gs_setup/01-introduction)<br>[lab01-gs_setup/02-one_click_deploy](gs-admin-training/lab01-gs_setup/02-one_click_deploy) |
| 03 | Application Components | Deploy/test/undeploy a space via CLI, REST v3, SpaceDeck, GS-UI; self-healing | [lab03-application_components](gs-admin-training/lab03-application_components) |
| 04 | The Bill Buddy Application | Deploy the BillBuddy space, feeders, distributed executor and web app | [lab04-BillBuddy_training_example](gs-admin-training/lab04-BillBuddy_training_example) |
| 05 | GigaSpaces SpaceDeck | SpaceDeck tour with the ui-demo tool: alerts (CPU, memory, unavailability, no backups), create/remove containers, scale a service | [lab05-spacedeck](gs-admin-training/lab05-spacedeck) |
| 06 | GigaSpaces Metrics | InfluxDB, Grafana dashboards (Docker) | [lab06-metrics](gs-admin-training/lab06-metrics) |
| 07 | GigaSpaces Manager | Multi-manager cluster (Docker Compose; older Vagrant version kept), ZooKeeper client example | [lab07-gigaspaces_manager](gs-admin-training/lab07-gigaspaces_manager) |
| 08 | Hot Deploy Procedure | Update PU code with the custom REST upgrade plugin | [lab08-hot_deploy](gs-admin-training/lab08-hot_deploy) |
| 09 | Zone-Based Deployment with the CLI | Zoned GSCs, `pu deploy --zones`, verify via `gs.sh` and REST v3 | [lab09-zone_deployment](gs-admin-training/lab09-zone_deployment) |
| 10 | GigaSpaces REST Manager API | Why REST v3, SpaceDeck on the same API, curl + jq: create GSCs, deploy, load data, relocate, alerts, then automate the build with one script | [lab10-rest_manager_api](gs-admin-training/lab10-rest_manager_api) |
| 11 | Grid Security | Secured grid, password encryption, roles, secured space | [lab11-security](gs-admin-training/lab11-security) |
| 12 | MemoryXtend | BillBuddy with RocksDB blobstore, restore from disk | [lab12-memoryxtend](gs-admin-training/lab12-memoryxtend) |
| 13 | Kubernetes | minikube, Helm charts, operator, vertical scaling | [lab13-kubernetes](gs-admin-training/lab13-kubernetes) |
| 14 | Storage Optimization | Compare storage types with heap dumps (Eclipse MAT) | [lab14-storage_optimization](gs-admin-training/lab14-storage_optimization) |
| 15 | GigaSpaces ODSX | Setup, tiered storage, data engine | [lab15-odsx](gs-admin-training/lab15-odsx) |
