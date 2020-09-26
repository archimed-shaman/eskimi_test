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
docker run -it --rm --net clickhouse --link eskimi_test_ch_shard1_1:clickhouse-server yandex/clickhouse-client --host eskimi_test_ch_shard1_1 --port 9000
```
Or to the second through the first one:
```
docker run -it --rm --net clickhouse --link eskimi_test_ch_shard1_1:clickhouse-server yandex/clickhouse-client --host eskimi_test_ch_shard2_1 --port 9000
```

## DB configuration
Test ClickHouse database has two shards to demonstrate mechanism of distributed queries. In real life usually there are several shard with two (or more) replicas within. ClickHouse needs ZooKeeper cluster for replication. Yandex is considering Etcd as alternative to ZooKeeper at the moment.
```
Shard_1 Replica_1 - Shard_1 Replica_2 
Shard_2 Replica_1 - Shard_2 Replica_2
...
Shard_N Replica_1 - Shard_N Replica_2
```

# Run

  1. Run ClickHouse & ZooKeper. Just run 

```
$ docker-compose up
``` 
This command will download all necessary containers from DockerHub and launch one ZooKeeper and two ClickHouse servers with all necessary configs.

  2. To simplify proceess, make some aliases: 
```
$ alias clickhouse-client="docker run -it --rm --net clickhouse --link eskimi_test_ch_shard1_1:clickhouse-server yandex/clickhouse-client"
$ alias clickhouse-pipe="docker run -a stdin -a stdout -i --rm --net clickhouse --link eskimi_test_ch_shard1_1:clickhouse-server yandex/clickhouse-client"
$ clickhouse-client --host eskimi_test_ch_shard1_1 --port 9000
ClickHouse client version 20.9.2.20 (official build).
Connecting to eskimi_test_ch_shard1_1:9000 as user default.
Connected to ClickHouse server version 20.9.2 revision 54439.
ch1 :)
```
    
  3. Create databases and tables. Just run sql script: 
```
$ clickhouse-pipe --host eskimi_test_ch_shard1_1 --port 9000 --multiquery < sql/constructors.sql
```
  4. Build data generator
```
$ cd generator && sbt assembly
```
    
  5. Generate data (e.g. full 2020.09), by default - 10000000 events per day.
```
$ for i in {01..30}; do scala generator/target/scala-2.13/generator-assembly-0.1.0-SNAPSHOT.jar 2020-09-$i 10000000; done
2020-09-26 13:04:54.920 [main] INFO  com.github.generator.main$ - generator started with args [day: Tue Sep 01 00:00:00 MSK 2020; rows: 10000000; dir: generated]
2020-09-26 13:05:06.214 [scala-execution-context-global-30] INFO  com.github.generator.main$ - batch 5000000 complete
2020-09-26 13:05:06.273 [scala-execution-context-global-22] INFO  com.github.generator.main$ - batch 6000000 complete
2020-09-26 13:05:06.330 [scala-execution-context-global-23] INFO  com.github.generator.main$ - batch 3000000 complete
2020-09-26 13:05:06.362 [scala-execution-context-global-25] INFO  com.github.generator.main$ - batch 2000000 complete
2020-09-26 13:05:06.363 [scala-execution-context-global-28] INFO  com.github.generator.main$ - batch 9000000 complete
2020-09-26 13:05:06.379 [scala-execution-context-global-26] INFO  com.github.generator.main$ - batch 7000000 complete
2020-09-26 13:05:06.393 [scala-execution-context-global-24] INFO  com.github.generator.main$ - batch 8000000 complete
2020-09-26 13:05:06.395 [scala-execution-context-global-21] INFO  com.github.generator.main$ - batch 1000000 complete
2020-09-26 13:05:06.424 [scala-execution-context-global-29] INFO  com.github.generator.main$ - batch 4000000 complete
2020-09-26 13:05:06.445 [scala-execution-context-global-27] INFO  com.github.generator.main$ - batch 10000000 complete
2020-09-26 13:05:06.454 [main] INFO  com.github.generator.main$ - generator stopped

...

$ du -h --max-depth=1 generated 
795M    generated/2020-09-26
795M    generated/2020-09-09
795M    generated/2020-09-15
795M    generated/2020-09-04
795M    generated/2020-09-20
795M    generated/2020-09-08
795M    generated/2020-09-24
795M    generated/2020-09-22
795M    generated/2020-09-27
795M    generated/2020-09-21
795M    generated/2020-09-06
795M    generated/2020-09-28
795M    generated/2020-09-12
795M    generated/2020-09-25
795M    generated/2020-09-02
795M    generated/2020-09-11
795M    generated/2020-09-23
795M    generated/2020-09-10
795M    generated/2020-09-18
795M    generated/2020-09-29
795M    generated/2020-09-13
795M    generated/2020-09-01
795M    generated/2020-09-03
795M    generated/2020-09-14
795M    generated/2020-09-19
795M    generated/2020-09-07
795M    generated/2020-09-16
795M    generated/2020-09-05
795M    generated/2020-09-17
795M    generated/2020-09-30
24G     generated
```
    
  6. Insert data into ClickHouse:
```
$ for day in {01..30}; do for file in $(ls generator/generated/2020-09-$day/part_*); do cat $file | clickhouse-pipe --host eskimi_test_ch_shard1_1 --port 9000 --query="INSERT INTO logs.visit_log_all FORMAT CSVWithNames"; done; done
```

  7. Now, let's check data
```
ch1 :) select count() from logs.visit_log_all

SELECT count()
FROM logs.visit_log_all

┌───count()─┐
│ 300000000 │
└───────────┘

1 rows in set. Elapsed: 0.013 sec.
```

Check most visited sites:
```
SELECT 
    site_id,
    uniq(dmp_id) AS users,
    count() AS visits
FROM logs.visit_log
PREWHERE toStartOfMonth(event_date) = '2020-09-01'
GROUP BY site_id
ORDER BY users DESC
LIMIT 10
SETTINGS max_threads = 8

┌─site_id─┬──users─┬─visits─┐
│      -1 │ 279432 │ 406639 │
│   12559 │  12139 │  12311 │
│   12905 │  12110 │  12257 │
│   12510 │  12098 │  12256 │
│   12445 │  12087 │  12227 │
│   12469 │  12079 │  12246 │
│   12785 │  12062 │  12210 │
│   12474 │  12062 │  12199 │
│   12268 │  12056 │  12231 │
│   12516 │  12046 │  12196 │
└─────────┴────────┴────────┘

10 rows in set. Elapsed: 9.824 sec. Processed 149.94 million rows, 3.00 GB (15.26 million rows/s., 305.24 MB/s.)
```
