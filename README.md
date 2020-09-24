# Solution

# Advantages & disadvantages

## Hadoop & Spark

## ClickHouse

### Advantages
  - 
  
### Disadvantages
  - Weak scalability. CH has no well-working resharding mechanism, so you should strongly plan you data volumes.
  - Data skew. If you have data, sharded by domain, tons of events regarding google.com will be placed to the same shard.
  - No data mutations (no `UPDATE/DELETE`). You have only `DROP PARTITION`. It is not a real problem for analytics instruments.
  - No optimizations for `JOIN` operations. It is not a problem, if you make optiomizations 'by hand' in SQL query (e.g. filter by key).

# Infrastructure

## ClickHouse server
Official Docker images are available at https://hub.docker.com/r/yandex/clickhouse-server/ \
\
Run local clickhouse cluster with docker-compose command:
```
docker-compose up
```

## ClickHouse client
Run official clickhouse client and connect to the first node with command
```
docker run -it --rm --net clickhouse --link eskimi_ch_shard1_1:clickhouse-server yandex/clickhouse-client --host eskimi_ch_shard1_1 --port 9000
```
Or to the second through the first one:
```
docker run -it --rm --net clickhouse --link eskimi_ch_shard1_1:clickhouse-server yandex/clickhouse-client --host eskimi_ch_shard2_1 --port 9000
```

To simplify the command you can do something like
```
$ alias clickhouse-client="docker run -it --rm --net clickhouse --link eskimi_ch_shard1_1:clickhouse-server yandex/clickhouse-client"
$ clickhouse-client --host eskimi_ch_shard1_1 --port 9000
ClickHouse client version 20.9.2.20 (official build).
Connecting to eskimi_ch_shard1_1:9000 as user default.
Connected to ClickHouse server version 20.9.2 revision 54439.

ch1 :)
```

## DB configuration
Test ClickHouse database has two shards to demonstrate mechanism of distributed queries. In real life usually there are several shard with two (or more) replicas within. ClickHouse needs ZooKeeper cluster for replication. Yandex is considering Etcd as alternative to ZooKeeper at the moment.
```
Shard_1 Replica_1 - Shard_1 Replica_2 
Shard_2 Replica_1 - Shard_2 Replica_2
...
Shard_N Replica_1 - Shard_N Replica_2
```
